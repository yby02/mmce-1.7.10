package github.kasuminova.mmce.common.tile;

import appeng.api.AEApi;
import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEItemStack;
import appeng.me.GridAccessException;
import appeng.util.Platform;
import github.kasuminova.mmce.common.tile.base.MEItemBus;
import github.kasuminova.mmce.compat.ItemStacks;
import hellfirepvp.modularmachinery.common.lib.BlocksMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.util.IOInventory;
import net.minecraft.item.ItemStack;

import java.util.concurrent.locks.ReadWriteLock;

/**
 * ME 物品输出总线：机器的产物放进 36 格内部缓存（数量不受 64 限制），随后尽快送入 ME 网络；ME 存不下时留在缓存中。
 * 1.7.10 移植：逻辑同上游 MEItemOutputBus。上游的“每格堆叠上限”设置界面（stack size）未移植，缓存每格上限为 Integer.MAX_VALUE。
 */
public class MEItemOutputBus extends MEItemBus {

    public static final int SLOTS = 36;

    @Override
    public IOInventory buildInventory() {
        IOInventory inv = new IOInventory(this, new int[0], allSlots(SLOTS));
        inv.setStackLimit(Integer.MAX_VALUE, allSlots(SLOTS));
        listenTo(inv);
        return inv;
    }

    @Override
    public ItemStack getVisualItemStack() {
        return new ItemStack(BlocksMM.meItemOutputBus);
    }

    @Override
    public MachineComponent.ItemBus provideComponent() {
        return new MachineComponent.ItemBus(IOType.OUTPUT) {
            @Override
            public IOInventory getContainerProvider() {
                return inventory;
            }
        };
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(5, 60, !hasItem(), true);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (!proxy.isActive()) {
            return TickRateModulation.IDLE;
        }
        int[] needUpdate = getNeedUpdateSlots();
        if (needUpdate.length == 0) {
            return TickRateModulation.SLOWER;
        }
        ReadWriteLock rwLock = inventory.getRWLock();
        rwLock.writeLock().lock();
        try {
            boolean success = false;
            IMEMonitor<IAEItemStack> me = proxy.getStorage().getItemInventory();
            for (int slot : needUpdate) {
                synchronized (this) {
                    changedSlots[slot] = false;
                }
                ItemStack stack = inventory.getStackInSlot(slot);
                if (ItemStacks.isEmpty(stack)) {
                    continue;
                }
                ItemStack extracted = inventory.extractItem(slot, stack.stackSize, false);
                IAEItemStack ae = AEApi.instance().storage().createItemStack(extracted);
                if (ae == null) {
                    continue;
                }
                IAEItemStack left = Platform.poweredInsert(proxy.getEnergy(), me, ae, source);
                if (left != null) {
                    inventory.setStackInSlot(slot, left.getItemStack());
                    if (ae.getStackSize() != left.getStackSize()) {
                        success = true;
                        failureCounter[slot] = 0;
                    } else {
                        failureCounter[slot] = Math.min(failureCounter[slot] + 1, 10);
                    }
                } else {
                    success = true;
                    failureCounter[slot] = 0;
                }
            }
            return success ? TickRateModulation.FASTER : TickRateModulation.SLOWER;
        } catch (GridAccessException e) {
            synchronized (this) {
                changedSlots = new boolean[changedSlots.length];
            }
            return TickRateModulation.IDLE;
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
