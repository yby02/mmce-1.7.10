package github.kasuminova.mmce.common.tile;

import appeng.api.AEApi;
import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEFluidStack;
import appeng.me.GridAccessException;
import appeng.util.Platform;
import github.kasuminova.mmce.common.tile.base.MEFluidBus;
import github.kasuminova.mmce.compat.fluids.IFluidHandler;
import hellfirepvp.modularmachinery.common.lib.BlocksMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.concurrent.locks.ReadWriteLock;

/** ME 流体输出总线：机器产出的流体放进 9 格储罐，随后尽快送入 ME 网络。逻辑同上游 MEFluidOutputBus。 */
public class MEFluidOutputBus extends MEFluidBus {

    @Override
    public ItemStack getVisualItemStack() {
        return new ItemStack(BlocksMM.meFluidOutputBus);
    }

    @Override
    public MachineComponent.FluidHatch provideComponent() {
        return new MachineComponent.FluidHatch(IOType.OUTPUT) {
            @Override
            public IFluidHandler getContainerProvider() {
                return tanks;
            }
        };
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(5, 60, tanks.isEmpty(), true);
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
        ReadWriteLock rwLock = tanks.getRWLock();
        rwLock.writeLock().lock();
        try {
            boolean success = false;
            IMEMonitor<IAEFluidStack> me = proxy.getStorage().getFluidInventory();
            for (int slot : needUpdate) {
                synchronized (this) {
                    changedSlots[slot] = false;
                }
                FluidStack stack = tanks.getFluidInSlot(slot);
                if (stack == null) {
                    continue;
                }
                IAEFluidStack ae = AEApi.instance().storage().createFluidStack(stack);
                if (ae == null) {
                    continue;
                }
                IAEFluidStack left = Platform.poweredInsert(proxy.getEnergy(), me, ae, source);
                if (left == null) {
                    tanks.setFluidInSlot(slot, null);
                    success = true;
                } else if (left.getStackSize() != stack.amount) {
                    tanks.setFluidInSlot(slot, left.getFluidStack());
                    success = true;
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
