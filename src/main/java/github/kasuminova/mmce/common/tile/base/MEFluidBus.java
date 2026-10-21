package github.kasuminova.mmce.common.tile.base;

import appeng.api.AEApi;
import appeng.api.networking.ticking.IGridTickable;
import github.kasuminova.mmce.common.util.FluidTankArray;
import github.kasuminova.mmce.compat.ItemStacks;
import hellfirepvp.modularmachinery.common.util.IOInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * ME 流体总线基类：9 格储罐（默认每格 8000 mB），5 格升级槽只接受 AE2 容量卡，每张使容量 × 4（同上游：容量 = 2000 × 4^(卡数 + 1)）。
 * 1.7.10 移植：上游的 AEFluidInventoryUpgradeable / StackUpgradeInventory 属于 AE2 1.12，这里用 {@link FluidTankArray} 与 {@link IOInventory} 实现。
 */
public abstract class MEFluidBus extends MEMachineComponent implements IGridTickable {

    public static final int TANK_SLOT_AMOUNT      = 9;
    public static final int TANK_DEFAULT_CAPACITY = 8000;
    public static final int UPGRADE_SLOTS         = 5;

    protected final FluidTankArray tanks    = new FluidTankArray(TANK_SLOT_AMOUNT, TANK_DEFAULT_CAPACITY);
    protected       IOInventory    upgrades = buildUpgradeInventory();
    protected       boolean[]      changedSlots = new boolean[TANK_SLOT_AMOUNT];
    protected       long           lastFullCheckTick = 0;

    public MEFluidBus() {
        tanks.setListener(slot -> {
            synchronized (this) {
                changedSlots[slot] = true;
            }
            markNoUpdateSync();
        });
    }

    private IOInventory buildUpgradeInventory() {
        int[] slots = new int[UPGRADE_SLOTS];
        for (int i = 0; i < UPGRADE_SLOTS; i++) {
            slots[i] = i;
        }
        IOInventory inv = new IOInventory(this, new int[0], new int[0]);
        inv.setMiscSlots(slots);
        inv.setStackLimit(1, slots);
        inv.setListener(slot -> updateTankCapacity());
        return inv;
    }

    public static boolean isCapacityCard(ItemStack stack) {
        return !ItemStacks.isEmpty(stack) && AEApi.instance().definitions().materials().cardCapacity().isSameAs(stack);
    }

    public int getInstalledCapacityCards() {
        int n = 0;
        for (int i = 0; i < upgrades.getSlots(); i++) {
            if (isCapacityCard(upgrades.getStackInSlot(i))) {
                n++;
            }
        }
        return n;
    }

    protected void updateTankCapacity() {
        tanks.setCapacity((int) (Math.pow(4, getInstalledCapacityCards() + 1) * (TANK_DEFAULT_CAPACITY / 4)));
        markNoUpdateSync();
    }

    protected synchronized int[] getNeedUpdateSlots() {
        long current = worldObj.getTotalWorldTime();
        if (lastFullCheckTick + 100 < current) {
            lastFullCheckTick = current;
            int[] all = new int[TANK_SLOT_AMOUNT];
            for (int i = 0; i < all.length; i++) {
                all[i] = i;
            }
            return all;
        }
        int count = 0;
        for (boolean c : changedSlots) {
            if (c) {
                count++;
            }
        }
        int[] out = new int[count];
        int j = 0;
        for (int i = 0; i < changedSlots.length; i++) {
            if (changedSlots[i]) {
                out[j++] = i;
            }
        }
        return out;
    }

    public FluidTankArray getTanks() {
        return tanks;
    }

    public IOInventory getUpgrades() {
        return upgrades;
    }

    public synchronized boolean hasChangedSlots() {
        for (boolean c : changedSlots) {
            if (c) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void markNoUpdate() {
        if (hasChangedSlots()) {
            alertDevice();
        }
        super.markNoUpdate();
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        if (compound.hasKey("upgrades")) {
            upgrades = IOInventory.deserialize(this, compound.getCompoundTag("upgrades"));
            int[] slots = new int[upgrades.getSlots()];
            for (int i = 0; i < slots.length; i++) {
                slots[i] = i;
            }
            upgrades.setStackLimit(1, slots);
            upgrades.setListener(slot -> updateTankCapacity());
        }
        tanks.readFromNBT(compound, "tanks");
        tanks.setCapacity((int) (Math.pow(4, getInstalledCapacityCards() + 1) * (TANK_DEFAULT_CAPACITY / 4)));
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        compound.setTag("upgrades", upgrades.writeNBT());
        tanks.writeToNBT(compound, "tanks");
    }
}
