package kport.modularmagic.common.container;

import github.kasuminova.mmce.common.container.SlotFiltered;
import github.kasuminova.mmce.compat.ItemStacks;
import hellfirepvp.modularmachinery.common.container.ContainerBase;
import kport.modularmagic.common.tile.TileLifeEssenceProvider;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/** 生命精华仓界面：一格血之宝珠（位置同上游 ContainerLifeEssence）。 */
public class ContainerLifeEssence extends ContainerBase<TileLifeEssenceProvider> {

    public ContainerLifeEssence(TileLifeEssenceProvider owner, EntityPlayer opening) {
        super(owner, opening);
        addSlotToContainer(new SlotFiltered(owner.getInventory().asGUIAccess(), 0, 81, 30, TileLifeEssenceProvider::isOrb));
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        Slot slot = this.inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) {
            return ItemStacks.EMPTY;
        }
        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();
        if (index < 36) {
            Slot orb = this.inventorySlots.get(36);
            if (!TileLifeEssenceProvider.isOrb(stack) || orb.getHasStack()) {
                return super.transferStackInSlot(playerIn, index);
            }
            orb.putStack(ItemStacks.copyWithSize(stack, 1));
            stack.stackSize--;
        } else if (!this.mergeItemStack(stack, 0, 36, false)) {
            return ItemStacks.EMPTY;
        }
        slot.putStack(stack.stackSize <= 0 ? ItemStacks.EMPTY : stack);
        return original;
    }
}
