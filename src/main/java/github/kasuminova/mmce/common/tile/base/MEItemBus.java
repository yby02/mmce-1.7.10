package github.kasuminova.mmce.common.tile.base;

import appeng.api.networking.ticking.IGridTickable;
import github.kasuminova.mmce.compat.ItemStacks;
import hellfirepvp.modularmachinery.common.tiles.base.IOInventoryTile;
import hellfirepvp.modularmachinery.common.util.IOInventory;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.List;

/**
 * ME 物品总线基类：内部物品栏（数量不受 64 限制）与“哪些格需要和 ME 网络同步”的标记。
 * 1.7.10 移植：上游以物品能力对外提供内部物品栏，这里实现 {@link IOInventoryTile}（漏斗等按输入 / 输出格访问）。
 */
public abstract class MEItemBus extends MEMachineComponent implements IGridTickable, IOInventoryTile {

    protected IOInventory inventory = buildInventory();
    protected boolean[]   changedSlots   = new boolean[inventory.getSlots()];
    protected int[]       failureCounter = new int[inventory.getSlots()];
    protected long        lastFullCheckTick = 0;

    public abstract IOInventory buildInventory();

    /** 上次全量检查 100 tick 后返回全部格，否则只返回有变化且最近没有连续失败的格。 */
    protected synchronized int[] getNeedUpdateSlots() {
        long current = worldObj.getTotalWorldTime();
        if (lastFullCheckTick + 100 < current) {
            lastFullCheckTick = current;
            int[] all = new int[inventory.getSlots()];
            for (int i = 0; i < all.length; i++) {
                all[i] = i;
            }
            return all;
        }
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < changedSlots.length; i++) {
            if (changedSlots[i] && failureCounter[i] <= 0) {
                slots.add(i);
            }
        }
        int[] out = new int[slots.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = slots.get(i);
        }
        return out;
    }

    protected void listenTo(IOInventory inv) {
        inv.setListener(slot -> {
            synchronized (this) {
                if (slot >= 0 && slot < changedSlots.length) {
                    changedSlots[slot] = true;
                }
            }
        });
    }

    protected static int[] allSlots(int size) {
        int[] slots = new int[size];
        for (int i = 0; i < size; i++) {
            slots[i] = i;
        }
        return slots;
    }

    @Override
    public IOInventory getInventory() {
        return inventory;
    }

    public IOInventory getInternalInventory() {
        return inventory;
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        if (compound.hasKey("inventory")) {
            readInventoryNBT(compound.getCompoundTag("inventory"));
        }
    }

    /** 读入内部物品栏（存档，或放下保存了内容的方块物品时）。 */
    public void readInventoryNBT(NBTTagCompound tag) {
        inventory = IOInventory.deserialize(this, tag);
        inventory.setStackLimit(Integer.MAX_VALUE, allSlots(inventory.getSlots()));
        listenTo(inventory);
        synchronized (this) {
            if (changedSlots.length != inventory.getSlots()) {
                changedSlots = new boolean[inventory.getSlots()];
                failureCounter = new int[inventory.getSlots()];
            }
            for (int i = 0; i < changedSlots.length; i++) {
                changedSlots[i] = true;
            }
        }
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        compound.setTag("inventory", inventory.writeNBT());
    }

    public boolean hasItem() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            if (!ItemStacks.isEmpty(inventory.getStackInSlot(i))) {
                return true;
            }
        }
        return false;
    }

    public synchronized boolean hasChangedSlots() {
        for (boolean changed : changedSlots) {
            if (changed) {
                return true;
            }
        }
        return false;
    }

    /** 内部物品栏变化后唤醒 AE2 的 tick 调度（上游在 markNoUpdate 中做同样的事）。 */
    @Override
    public void markNoUpdate() {
        if (hasChangedSlots()) {
            alertDevice();
        }
        super.markNoUpdate();
    }
}
