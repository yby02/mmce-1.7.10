package github.kasuminova.mmce.compat.items;

import github.kasuminova.mmce.compat.ItemStacks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * 1.7.10 移植适配：对应 Forge 1.12.2 net.minecraftforge.items.SlotItemHandler（容器槽位直接读写 IItemHandler）。
 */
public class SlotItemHandler extends Slot {

    private static final IInventory EMPTY_INVENTORY = new InventoryBasic("[Null]", true, 0);

    private final IItemHandler itemHandler;
    private final int index;

    public SlotItemHandler(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
        super(EMPTY_INVENTORY, index, xPosition, yPosition);
        this.itemHandler = itemHandler;
        this.index = index;
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    public int getSlotIndex() {
        return index;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        if (ItemStacks.isEmpty(stack) || !itemHandler.isItemValid(index, stack)) {
            return false;
        }
        IItemHandler handler = this.itemHandler;
        if (handler instanceof IItemHandlerModifiable) {
            IItemHandlerModifiable modifiable = (IItemHandlerModifiable) handler;
            ItemStack current = modifiable.getStackInSlot(index);
            modifiable.setStackInSlot(index, null);
            ItemStack remainder = modifiable.insertItem(index, stack, true);
            modifiable.setStackInSlot(index, current);
            return ItemStacks.getCount(remainder) < stack.stackSize;
        }
        ItemStack remainder = handler.insertItem(index, stack, true);
        return ItemStacks.getCount(remainder) < stack.stackSize;
    }

    @Override
    public ItemStack getStack() {
        return ItemStacks.normalize(itemHandler.getStackInSlot(index));
    }

    @Override
    public void putStack(ItemStack stack) {
        ((IItemHandlerModifiable) itemHandler).setStackInSlot(index, ItemStacks.normalize(stack));
        onSlotChanged();
    }

    @Override
    public void onSlotChange(ItemStack oldStack, ItemStack newStack) {
    }

    @Override
    public int getSlotStackLimit() {
        return itemHandler.getSlotLimit(index);
    }

    @Override
    public boolean canTakeStack(EntityPlayer player) {
        return !ItemStacks.isEmpty(itemHandler.extractItem(index, 1, true));
    }

    @Override
    public ItemStack decrStackSize(int amount) {
        return ItemStacks.normalize(itemHandler.extractItem(index, amount, false));
    }

    @Override
    public boolean isSlotInInventory(IInventory inventory, int slot) {
        return false;
    }
}
