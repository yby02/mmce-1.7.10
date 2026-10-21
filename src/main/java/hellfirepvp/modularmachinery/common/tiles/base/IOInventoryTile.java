package hellfirepvp.modularmachinery.common.tiles.base;

import github.kasuminova.mmce.compat.ItemStacks;
import hellfirepvp.modularmachinery.common.util.IOInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

/**
 * 1.7.10 移植：1.12.2 通过 ITEM_HANDLER_CAPABILITY 暴露 {@link IOInventory}；1.7.10 中由 TileEntity 实现本接口（ISidedInventory），
 * 默认方法委托到 IOInventory（输入槽/输出槽限制由 IOInventory 的 insertItem/extractItem 判定），供漏斗、管道和其他模组访问。
 * 只能由 {@link TileEntity} 的子类实现。
 */
public interface IOInventoryTile extends ISidedInventory {

    IOInventory getInventory();

    @Override
    default int[] getAccessibleSlotsFromSide(int side) {
        int[] slots = new int[getInventory().getSlots()];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = i;
        }
        return slots;
    }

    @Override
    default boolean canInsertItem(int slot, ItemStack stack, int side) {
        if (ItemStacks.isEmpty(stack)) {
            return false;
        }
        ItemStack remain = getInventory().insertItem(slot, stack, true);
        return ItemStacks.getCount(remain) < stack.stackSize;
    }

    @Override
    default boolean canExtractItem(int slot, ItemStack stack, int side) {
        return !ItemStacks.isEmpty(getInventory().extractItem(slot, Math.max(1, ItemStacks.getCount(stack)), true));
    }

    @Override
    default int getSizeInventory() {
        return getInventory().getSlots();
    }

    @Override
    default ItemStack getStackInSlot(int slot) {
        return ItemStacks.normalize(getInventory().getStackInSlot(slot));
    }

    @Override
    default ItemStack decrStackSize(int slot, int amount) {
        IOInventory inventory = getInventory();
        ItemStack stack = inventory.getStackInSlot(slot);
        if (ItemStacks.isEmpty(stack)) {
            return null;
        }
        ItemStack taken = ItemStacks.copyWithSize(stack, Math.min(amount, stack.stackSize));
        inventory.setStackInSlot(slot, ItemStacks.copyWithSize(stack, stack.stackSize - ItemStacks.getCount(taken)));
        return taken;
    }

    @Override
    default ItemStack getStackInSlotOnClosing(int slot) {
        return null;
    }

    @Override
    default void setInventorySlotContents(int slot, ItemStack stack) {
        getInventory().setStackInSlot(slot, ItemStacks.normalize(stack));
    }

    @Override
    default String getInventoryName() {
        TileEntity te = (TileEntity) this;
        return te.getBlockType() == null ? "container.modularmachinery" : te.getBlockType().getUnlocalizedName() + ".name";
    }

    @Override
    default boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    default int getInventoryStackLimit() {
        IOInventory inventory = getInventory();
        int max = 0;
        for (int i = 0; i < inventory.getSlots(); i++) {
            max = Math.max(max, inventory.getSlotLimit(i));
        }
        return max <= 0 ? 64 : max;
    }

    @Override
    default boolean isUseableByPlayer(EntityPlayer player) {
        TileEntity te = (TileEntity) this;
        return te.getWorldObj() != null && te.getWorldObj().getTileEntity(te.xCoord, te.yCoord, te.zCoord) == te
            && player.getDistanceSq(te.xCoord + 0.5D, te.yCoord + 0.5D, te.zCoord + 0.5D) <= 64.0D;
    }

    @Override
    default void openInventory() {
    }

    @Override
    default void closeInventory() {
    }

    @Override
    default boolean isItemValidForSlot(int slot, ItemStack stack) {
        return canInsertItem(slot, stack, -1);
    }
}
