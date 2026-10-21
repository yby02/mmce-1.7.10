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
import hellfirepvp.modularmachinery.common.util.ItemUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.concurrent.locks.ReadWriteLock;

/**
 * ME 物品输入总线：16 个设定格（物品与数量），每格从 ME 网络取到设定数量放进对应的内部格，供机器作为物品输入；
 * 设定改变或清空后，多出 / 不再需要的物品放回 ME 网络。
 * 1.7.10 移植：逻辑同上游 MEItemInputBus，ME 接口换成 AE2 rv3（getItemInventory、createItemStack、getItemStack）。
 */
public class MEItemInputBus extends MEItemBus implements SettingsTransfer {

    public static final  int    SLOTS          = 16;
    private static final String CONFIG_TAG_KEY = "configInventory";

    private IOInventory configInventory = buildConfigInventory();

    @Override
    public IOInventory buildInventory() {
        IOInventory inv = new IOInventory(this, allSlots(SLOTS), new int[0]);
        inv.setStackLimit(Integer.MAX_VALUE, allSlots(SLOTS));
        listenTo(inv);
        return inv;
    }

    @Override
    public ItemStack getVisualItemStack() {
        return new ItemStack(BlocksMM.meItemInputBus);
    }

    private IOInventory buildConfigInventory() {
        IOInventory inv = new IOInventory(this, new int[0], new int[0]);
        inv.setStackLimit(Integer.MAX_VALUE, allSlots(SLOTS));
        inv.setMiscSlots(allSlots(SLOTS));
        listenTo(inv);
        return inv;
    }

    public IOInventory getConfigInventory() {
        return configInventory;
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        if (compound.hasKey(CONFIG_TAG_KEY)) {
            readConfigInventoryNBT(compound.getCompoundTag(CONFIG_TAG_KEY));
        }
    }

    public void readConfigInventoryNBT(NBTTagCompound tag) {
        configInventory = IOInventory.deserialize(this, tag);
        configInventory.setStackLimit(Integer.MAX_VALUE, allSlots(configInventory.getSlots()));
        listenTo(configInventory);
    }

    public boolean configInvHasItem() {
        for (int i = 0; i < configInventory.getSlots(); i++) {
            if (!ItemStacks.isEmpty(configInventory.getStackInSlot(i))) {
                return true;
            }
        }
        return false;
    }

    /** 界面中修改设定格：设为给定物品与数量（null 或数量 ≤ 0 清空）。 */
    public void setConfig(int slot, ItemStack stack) {
        if (slot < 0 || slot >= configInventory.getSlots()) {
            return;
        }
        configInventory.setStackInSlot(slot, ItemStacks.isEmpty(stack) ? ItemStacks.EMPTY : stack.copy());
        onConfigChanged();
        markForUpdateSync();
    }

    @Override
    public NBTTagCompound downloadSettings() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag(CONFIG_TAG_KEY, configInventory.writeNBT());
        return tag;
    }

    @Override
    public void uploadSettings(NBTTagCompound settings) {
        readConfigInventoryNBT(settings.getCompoundTag(CONFIG_TAG_KEY));
        onConfigChanged();
        markForUpdateSync();
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        compound.setTag(CONFIG_TAG_KEY, configInventory.writeNBT());
    }

    @Override
    public MachineComponent.ItemBus provideComponent() {
        return new MachineComponent.ItemBus(IOType.INPUT) {
            @Override
            public IOInventory getContainerProvider() {
                return inventory;
            }

            @Override
            public long getGroupID() {
                return getGroupId();
            }
        };
    }

    @Override
    public boolean canGroupInput() {
        return true;
    }

    // ---------------- AE2 tick ----------------

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(10, 120, !needsUpdate(), true);
    }

    private boolean needsUpdate() {
        for (int slot = 0; slot < configInventory.getSlots(); slot++) {
            ItemStack cfg = configInventory.getStackInSlot(slot);
            ItemStack inv = inventory.getStackInSlot(slot);
            if (ItemStacks.isEmpty(cfg)) {
                if (!ItemStacks.isEmpty(inv)) {
                    return true;
                }
                continue;
            }
            if (ItemStacks.isEmpty(inv) || !ItemUtils.matchStacks(cfg, inv) || inv.stackSize != cfg.stackSize) {
                return true;
            }
        }
        return false;
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
                ItemStack cfg = configInventory.getStackInSlot(slot);
                ItemStack inv = inventory.getStackInSlot(slot);

                if (ItemStacks.isEmpty(cfg)) {
                    if (!ItemStacks.isEmpty(inv)) {
                        inventory.setStackInSlot(slot, insert(me, inv));
                    }
                    continue;
                }
                if (ItemStacks.isEmpty(inv) || !ItemUtils.matchStacks(cfg, inv)) {
                    // 换成新设定的物品：先把旧物品放回 ME，再按设定数量取
                    if (ItemStacks.isEmpty(inv) || ItemStacks.isEmpty(insert(me, inv))) {
                        ItemStack got = extract(me, cfg);
                        inventory.setStackInSlot(slot, got);
                        success |= !ItemStacks.isEmpty(got);
                    }
                    continue;
                }
                if (cfg.stackSize == inv.stackSize) {
                    continue;
                }
                if (cfg.stackSize > inv.stackSize) {
                    ItemStack got = extract(me, ItemUtils.copyStackWithSize(inv, cfg.stackSize - inv.stackSize));
                    if (!ItemStacks.isEmpty(got)) {
                        inventory.setStackInSlot(slot, ItemUtils.copyStackWithSize(inv, inv.stackSize + got.stackSize));
                        success = true;
                        failureCounter[slot] = 0;
                    } else {
                        // ME 中物品不足：降低该格的检查频率
                        failureCounter[slot]++;
                    }
                } else {
                    int extra = inv.stackSize - cfg.stackSize;
                    ItemStack left = insert(me, ItemUtils.copyStackWithSize(inv, extra));
                    inventory.setStackInSlot(slot, ItemUtils.copyStackWithSize(inv, cfg.stackSize + ItemStacks.getCount(left)));
                    success = true;
                }
            }
            for (int i = 0; i < failureCounter.length; i++) {
                if (failureCounter[i] > 0) {
                    failureCounter[i]--;
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

    private ItemStack extract(IMEMonitor<IAEItemStack> me, ItemStack stack) throws GridAccessException {
        IAEItemStack ae = AEApi.instance().storage().createItemStack(stack);
        if (ae == null) {
            return null;
        }
        IAEItemStack got = Platform.poweredExtraction(proxy.getEnergy(), me, ae, source);
        return got == null ? null : got.getItemStack();
    }

    private ItemStack insert(IMEMonitor<IAEItemStack> me, ItemStack stack) throws GridAccessException {
        IAEItemStack ae = AEApi.instance().storage().createItemStack(stack);
        if (ae == null) {
            return stack;
        }
        IAEItemStack left = Platform.poweredInsert(proxy.getEnergy(), me, ae, source);
        return left == null ? null : left.getItemStack();
    }

    /** 设定格变化（界面中修改）后立即唤醒同步。 */
    public void onConfigChanged() {
        synchronized (this) {
            for (int i = 0; i < changedSlots.length; i++) {
                changedSlots[i] = true;
            }
        }
        markNoUpdateSync();
        alertDevice();
    }
}
