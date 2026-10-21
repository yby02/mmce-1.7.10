package github.kasuminova.mmce.compat.items;

import github.kasuminova.mmce.compat.ItemStacks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;

/**
 * 1.7.10 移植适配：把 MMCE 内部的 {@link IItemHandlerModifiable} 以 1.7.10 的 ISidedInventory 形式暴露给漏斗、管道和其他模组。
 * TileEntity 实现 ISidedInventory 时可直接继承本类的逻辑（组合方式见 TileInventory）。
 */
public class ItemHandlerInventory implements ISidedInventory {

    private final IItemHandlerModifiable handler;
    private final String name;
    private final Runnable onChanged;
    private final boolean canInsert;
    private final boolean canExtract;

    public ItemHandlerInventory(IItemHandlerModifiable handler, String name, Runnable onChanged, boolean canInsert, boolean canExtract) {
        this.handler = handler;
        this.name = name;
        this.onChanged = onChanged;
        this.canInsert = canInsert;
        this.canExtract = canExtract;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        int[] slots = new int[handler.getSlots()];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = i;
        }
        return slots;
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, int side) {
        if (!canInsert || ItemStacks.isEmpty(stack)) {
            return false;
        }
        ItemStack remain = handler.insertItem(slot, stack, true);
        return ItemStacks.getCount(remain) < stack.stackSize;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int side) {
        return canExtract && !ItemStacks.isEmpty(handler.extractItem(slot, ItemStacks.getCount(stack), true));
    }

    @Override
    public int getSizeInventory() {
        return handler.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return ItemStacks.normalize(handler.getStackInSlot(slot));
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        ItemStack extracted = handler.extractItem(slot, amount, false);
        if (!ItemStacks.isEmpty(extracted)) {
            markDirty();
        }
        return ItemStacks.normalize(extracted);
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        return null;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        handler.setStackInSlot(slot, ItemStacks.normalize(stack));
        markDirty();
    }

    @Override
    public String getInventoryName() {
        return name;
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        int max = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            max = Math.max(max, handler.getSlotLimit(i));
        }
        return max <= 0 ? 64 : max;
    }

    @Override
    public void markDirty() {
        if (onChanged != null) {
            onChanged.run();
        }
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return true;
    }

    @Override
    public void openInventory() {
    }

    @Override
    public void closeInventory() {
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return canInsert && handler.isItemValid(slot, stack);
    }
}
