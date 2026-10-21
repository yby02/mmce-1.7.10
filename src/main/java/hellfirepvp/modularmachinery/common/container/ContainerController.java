/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.container;

import github.kasuminova.mmce.compat.ItemStacks;

import hellfirepvp.modularmachinery.common.item.ItemBlueprint;
import hellfirepvp.modularmachinery.common.tiles.TileMachineController;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import github.kasuminova.mmce.compat.items.IItemHandler;
import github.kasuminova.mmce.compat.items.SlotItemHandler;

import javax.annotation.Nonnull;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: ContainerController
 * Created by HellFirePvP
 * Date: 12.07.2017 / 23:30
 */
public class ContainerController extends ContainerBase<TileMachineController> {

    private final Slot slotBlueprint;

    public ContainerController(TileMachineController owner, EntityPlayer opening) {
        super(owner, opening);

        this.slotBlueprint = addSlotToContainer(new SlotBlueprint(
            owner.getInventory().asGUIAccess(),
            TileMultiblockMachineController.BLUEPRINT_SLOT, 151, 8));
    }

    @Nonnull
    @Override
    public ItemStack transferStackInSlot(@Nonnull EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStacks.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();

            if (index < 36) {
                if (!ItemStacks.isEmpty(itemstack1) && itemstack1.getItem() instanceof ItemBlueprint) {
                    Slot sb = this.inventorySlots.get(this.slotBlueprint.slotNumber);
                    if (!sb.getHasStack()) {
                        if (!this.mergeItemStack(itemstack1, sb.slotNumber, sb.slotNumber + 1, false)) {
                            return ItemStacks.EMPTY;
                        }
                    }
                }
            }

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

    public static class SlotBlueprint extends SlotItemHandler {

        public SlotBlueprint(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }

        @Override
        public boolean isItemValid(@Nonnull ItemStack stack) {
            if (!(stack.getItem() instanceof ItemBlueprint)) {
                return false;
            }
            return super.isItemValid(stack);
        }
    }

}
