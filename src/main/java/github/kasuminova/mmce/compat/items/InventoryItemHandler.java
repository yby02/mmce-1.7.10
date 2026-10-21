package github.kasuminova.mmce.compat.items;

import github.kasuminova.mmce.compat.EnumFacing;
import github.kasuminova.mmce.compat.ItemStacks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;

import javax.annotation.Nullable;

/**
 * 把 1.7.10 的 IInventory / ISidedInventory 包装成 {@link IItemHandlerModifiable}，
 * 行为对应 Forge 1.12.2 的 InvWrapper / SidedInvWrapper。用于与相邻的外部容器交换物品。
 */
public class InventoryItemHandler implements IItemHandlerModifiable {

    private final IInventory inv;
    /** null 表示不区分面（等同 InvWrapper）。 */
    @Nullable
    private final EnumFacing side;
    @Nullable
    private final int[] slots;

    public InventoryItemHandler(IInventory inv, @Nullable EnumFacing side) {
        this.inv = inv;
        if (side != null && inv instanceof ISidedInventory) {
            this.side = side;
            int[] accessible = ((ISidedInventory) inv).getAccessibleSlotsFromSide(side.getIndex());
            this.slots = accessible == null ? new int[0] : accessible;
        } else {
            this.side = null;
            this.slots = null;
        }
    }

    /**
     * 返回相邻 TileEntity 在给定面上的物品处理器；不是容器时返回 null。
     * 箱子按原版漏斗的方式合并为大箱子。
     */
    @Nullable
    public static IItemHandlerModifiable of(@Nullable TileEntity te, @Nullable EnumFacing side) {
        if (te == null || te.isInvalid()) {
            return null;
        }
        if (te instanceof IItemHandlerModifiable) {
            return (IItemHandlerModifiable) te;
        }
        IInventory inventory = null;
        if (te instanceof TileEntityChest && te.getWorldObj() != null) {
            Block block = te.getWorldObj().getBlock(te.xCoord, te.yCoord, te.zCoord);
            if (block instanceof BlockChest) {
                inventory = ((BlockChest) block).func_149951_m(te.getWorldObj(), te.xCoord, te.yCoord, te.zCoord);
            }
        } else if (te instanceof IInventory) {
            inventory = (IInventory) te;
        }
        return inventory == null ? null : new InventoryItemHandler(inventory, side);
    }

    private int toInvSlot(int slot) {
        return slots == null ? slot : slots[slot];
    }

    @Override
    public int getSlots() {
        return slots == null ? inv.getSizeInventory() : slots.length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot < 0 || slot >= getSlots()) {
            return ItemStacks.EMPTY;
        }
        return ItemStacks.normalize(inv.getStackInSlot(toInvSlot(slot)));
    }

    private boolean canInsert(int invSlot, ItemStack stack) {
        if (!inv.isItemValidForSlot(invSlot, stack)) {
            return false;
        }
        return side == null || ((ISidedInventory) inv).canInsertItem(invSlot, stack, side.getIndex());
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (ItemStacks.isEmpty(stack) || slot < 0 || slot >= getSlots()) {
            return stack;
        }
        int invSlot = toInvSlot(slot);
        if (!canInsert(invSlot, stack)) {
            return stack;
        }
        ItemStack inSlot = ItemStacks.normalize(inv.getStackInSlot(invSlot));
        int limit = Math.min(stack.getMaxStackSize(), getSlotLimit(slot));
        int moved;
        if (!ItemStacks.isEmpty(inSlot)) {
            if (!ItemHandlerHelper.canItemStacksStack(stack, inSlot)) {
                return stack;
            }
            moved = Math.min(limit - inSlot.stackSize, stack.stackSize);
            if (moved <= 0) {
                return stack;
            }
            if (!simulate) {
                inv.setInventorySlotContents(invSlot, ItemStacks.copyWithSize(stack, inSlot.stackSize + moved));
                inv.markDirty();
            }
        } else {
            moved = Math.min(limit, stack.stackSize);
            if (moved <= 0) {
                return stack;
            }
            if (!simulate) {
                inv.setInventorySlotContents(invSlot, ItemStacks.copyWithSize(stack, moved));
                inv.markDirty();
            }
        }
        return moved >= stack.stackSize ? ItemStacks.EMPTY : ItemStacks.copyWithSize(stack, stack.stackSize - moved);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0 || slot < 0 || slot >= getSlots()) {
            return ItemStacks.EMPTY;
        }
        int invSlot = toInvSlot(slot);
        ItemStack inSlot = ItemStacks.normalize(inv.getStackInSlot(invSlot));
        if (ItemStacks.isEmpty(inSlot)) {
            return ItemStacks.EMPTY;
        }
        if (side != null && !((ISidedInventory) inv).canExtractItem(invSlot, inSlot, side.getIndex())) {
            return ItemStacks.EMPTY;
        }
        int count = Math.min(inSlot.stackSize, amount);
        if (simulate) {
            return ItemStacks.copyWithSize(inSlot, count);
        }
        ItemStack extracted = inv.decrStackSize(invSlot, count);
        inv.markDirty();
        return ItemStacks.normalize(extracted);
    }

    @Override
    public int getSlotLimit(int slot) {
        return inv.getInventoryStackLimit();
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot >= 0 && slot < getSlots() && canInsert(toInvSlot(slot), stack);
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        if (slot < 0 || slot >= getSlots()) {
            return;
        }
        inv.setInventorySlotContents(toInvSlot(slot), ItemStacks.normalize(stack));
        inv.markDirty();
    }
}
