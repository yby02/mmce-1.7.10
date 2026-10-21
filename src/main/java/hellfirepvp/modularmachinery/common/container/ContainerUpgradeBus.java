package hellfirepvp.modularmachinery.common.container;

import github.kasuminova.mmce.compat.ItemStacks;

import hellfirepvp.modularmachinery.common.tiles.TileUpgradeBus;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import github.kasuminova.mmce.compat.items.IItemHandlerModifiable;
import github.kasuminova.mmce.compat.items.SlotItemHandler;

import javax.annotation.Nonnull;

public class ContainerUpgradeBus extends ContainerBase<TileUpgradeBus> {
    public static final int SLOT_START_X = 8;
    public static final int SLOT_START_Y = 17;

    public ContainerUpgradeBus(final TileUpgradeBus owner, final EntityPlayer opening) {
        super(owner, opening);
        addInventorySlots(owner.getInventory().asGUIAccess(), owner.provideComponent().size());
    }

    private void addInventorySlots(IItemHandlerModifiable itemHandler, int size) {
        int x = SLOT_START_X;
        int y = SLOT_START_Y;

        for (int i = 0; i < size; i++) {
            addSlotToContainer(new SlotItemHandler(itemHandler, i, x, y));
            x += 18;

            if ((i + 1) % 3 == 0) {
                x = SLOT_START_X;
                y += 18;
            }
        }
    }

    @Override
    protected void addPlayerSlots(EntityPlayer opening) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlotToContainer(new Slot(opening.inventory, j + i * 9 + 9, 8 + j * 18, 131 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            addSlotToContainer(new Slot(opening.inventory, i, 8 + i * 18, 189));
        }
    }

    @Nonnull
    @Override
    public ItemStack transferStackInSlot(@Nonnull EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStacks.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();

            boolean changed = false;
            if (index < 36) {
                if (this.mergeItemStack(itemstack1, 36, inventorySlots.size(), false)) {
                    changed = true;
                }
            }

            if (!changed) {
                if (index < 27) {
                    if (!this.mergeItemStack(itemstack1, 27, 36, false)) {
                        return ItemStacks.EMPTY;
                    }
                } else if (index < 36) {
                    if (!this.mergeItemStack(itemstack1, 0, 27, false)) {
                        return ItemStacks.EMPTY;
                    }
                } else if (!this.mergeItemStack(itemstack1, 0, 36, false)) {
                    return ItemStacks.EMPTY;
                }
            }

            if (ItemStacks.getCount(itemstack1) == 0) {
                slot.putStack(ItemStacks.EMPTY);
            } else {
                slot.onSlotChanged();
            }

            if (ItemStacks.getCount(itemstack1) == ItemStacks.getCount(itemstack)) {
                return ItemStacks.EMPTY;
            }

            slot.onPickupFromSlot(playerIn, itemstack1);
        }

        return itemstack;
    }
}
