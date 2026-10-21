package github.kasuminova.mmce.common.container;

import github.kasuminova.mmce.compat.ItemStacks;
import hellfirepvp.modularmachinery.common.container.ContainerBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

/**
 * ME 部件界面的容器基类：玩家物品栏位置由子类给出（与上游贴图一致）；Shift 点击只放进接受该物品的格（1.7.10 的 mergeItemStack 不检查
 * isItemValid，样板格 / 升级卡格需要自行判断）。
 * <p>
 * 数量可能超过 64 的格（ME 总线的设定格与缓存、样板供应器的缓存）不作为容器槽位：1.7.10 的物品同步包只用一个字节传数量，
 * 这些内容随方块实体更新同步，由界面直接绘制。
 */
public abstract class ContainerMEBase<T extends TileEntity> extends ContainerBase<T> implements MEGuiActionHandler {

    protected ContainerMEBase(T owner, EntityPlayer opening) {
        super(owner, opening);
    }

    /** 玩家物品栏第一行的 y 坐标（快捷栏在其下 58 像素）。 */
    protected abstract int playerInventoryY();

    @Override
    protected void addPlayerSlots(EntityPlayer opening) {
        int y = playerInventoryY();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlotToContainer(new Slot(opening.inventory, j + i * 9 + 9, 8 + j * 18, y + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            addSlotToContainer(new Slot(opening.inventory, i, 8 + i * 18, y + 58));
        }
    }

    @Override
    public void onGuiAction(EntityPlayerMP player, int action, int slot, int value) {
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
            boolean moved = mergeIntoValidSlots(stack, 36, inventorySlots.size());
            if (!moved) {
                if (index < 27) {
                    if (!this.mergeItemStack(stack, 27, 36, false)) {
                        return ItemStacks.EMPTY;
                    }
                } else if (!this.mergeItemStack(stack, 0, 27, false)) {
                    return ItemStacks.EMPTY;
                }
            }
        } else if (!this.mergeItemStack(stack, 0, 36, false)) {
            return ItemStacks.EMPTY;
        }

        if (stack.stackSize <= 0) {
            slot.putStack(ItemStacks.EMPTY);
        } else {
            slot.putStack(stack);
        }
        if (stack.stackSize == original.stackSize) {
            return ItemStacks.EMPTY;
        }
        slot.onPickupFromSlot(playerIn, stack);
        return original;
    }

    /** 依次放进 [from, to) 中接受该物品的格（遵守格的数量上限），返回是否放进了至少一个。 */
    protected boolean mergeIntoValidSlots(ItemStack stack, int from, int to) {
        boolean moved = false;
        for (int i = from; i < to && stack.stackSize > 0; i++) {
            Slot target = this.inventorySlots.get(i);
            if (!target.isItemValid(stack)) {
                continue;
            }
            ItemStack inSlot = target.getStack();
            int limit = Math.min(target.getSlotStackLimit(), stack.getMaxStackSize());
            if (ItemStacks.isEmpty(inSlot)) {
                int toMove = Math.min(limit, stack.stackSize);
                target.putStack(ItemStacks.copyWithSize(stack, toMove));
                stack.stackSize -= toMove;
                moved = true;
            } else if (inSlot.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(inSlot, stack) && inSlot.stackSize < limit) {
                int toMove = Math.min(limit - inSlot.stackSize, stack.stackSize);
                target.putStack(ItemStacks.copyWithSize(inSlot, inSlot.stackSize + toMove));
                stack.stackSize -= toMove;
                moved = true;
            }
        }
        return moved;
    }
}
