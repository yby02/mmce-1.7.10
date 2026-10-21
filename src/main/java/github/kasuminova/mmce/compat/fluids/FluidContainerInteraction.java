package github.kasuminova.mmce.compat.fluids;

import github.kasuminova.mmce.compat.ItemStacks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

/**
 * 1.7.10 移植适配：替代 1.12.2 的 FluidUtil.interactWithFluidHandler（手持桶/流体容器右键储罐）。
 * 支持 FluidContainerRegistry 登记的容器（桶、瓶等）与 IFluidContainerItem（各模组的流体单元/储罐物品）。
 */
public final class FluidContainerInteraction {

    private FluidContainerInteraction() {
    }

    /** 手持物品是否为流体容器（满或空）。 */
    public static boolean isFluidContainer(ItemStack stack) {
        if (ItemStacks.isEmpty(stack)) {
            return false;
        }
        return FluidContainerRegistry.isContainer(stack) || stack.getItem() instanceof IFluidContainerItem;
    }

    /** 尝试在玩家手持容器与储罐之间转移流体；成功返回 true。 */
    public static boolean interact(EntityPlayer player, IFluidHandler tank) {
        ItemStack held = player.getCurrentEquippedItem();
        if (ItemStacks.isEmpty(held)) {
            return false;
        }
        if (held.getItem() instanceof IFluidContainerItem) {
            return interactContainerItem(player, held, (IFluidContainerItem) held.getItem(), tank);
        }
        FluidStack contained = FluidContainerRegistry.getFluidForFilledItem(held);
        if (contained != null) {
            // 满容器 -> 倒入储罐
            if (tank.fill(contained, false) < contained.amount) {
                return false;
            }
            tank.fill(contained, true);
            if (!player.capabilities.isCreativeMode) {
                ItemStack empty = FluidContainerRegistry.drainFluidContainer(held);
                consumeHeldAndGive(player, held, empty);
            }
            return true;
        }
        if (FluidContainerRegistry.isEmptyContainer(held)) {
            // 空容器 -> 从储罐装出
            FluidStack available = tank.drain(Integer.MAX_VALUE, false);
            if (available == null || available.amount <= 0) {
                return false;
            }
            ItemStack filled = FluidContainerRegistry.fillFluidContainer(available, held);
            FluidStack inFilled = filled == null ? null : FluidContainerRegistry.getFluidForFilledItem(filled);
            if (inFilled == null) {
                return false;
            }
            FluidStack drained = tank.drain(inFilled, false);
            if (drained == null || drained.amount < inFilled.amount) {
                return false;
            }
            tank.drain(inFilled, true);
            if (!player.capabilities.isCreativeMode) {
                consumeHeldAndGive(player, held, filled);
            }
            return true;
        }
        return false;
    }

    /**
     * 对应 1.12.2 FluidUtil.tryEmptyContainer：把 1 个容器中的流体（至多 maxAmount）倒入储罐。
     * 返回 null 表示失败；成功时返回倒空后的单个容器（可能为 null，表示容器被消耗）包装在数组中。
     */
    public static ItemStack[] tryEmptyContainer(ItemStack container, IFluidHandler tank, int maxAmount, boolean doDrain) {
        if (ItemStacks.isEmpty(container) || maxAmount <= 0) {
            return null;
        }
        ItemStack single = ItemStacks.copyWithSize(container, 1);
        if (single.getItem() instanceof IFluidContainerItem) {
            IFluidContainerItem item = (IFluidContainerItem) single.getItem();
            FluidStack drainable = item.drain(single, maxAmount, false);
            if (drainable == null || drainable.amount <= 0) {
                return null;
            }
            int accepted = tank.fill(drainable, false);
            if (accepted <= 0) {
                return null;
            }
            FluidStack drained = item.drain(single, accepted, true);
            if (doDrain && drained != null) {
                tank.fill(drained, true);
            }
            return new ItemStack[]{single};
        }
        FluidStack contained = FluidContainerRegistry.getFluidForFilledItem(single);
        if (contained == null || contained.amount <= 0 || contained.amount > maxAmount) {
            return null;
        }
        if (tank.fill(contained, false) < contained.amount) {
            return null;
        }
        if (doDrain) {
            tank.fill(contained, true);
        }
        return new ItemStack[]{ItemStacks.normalize(FluidContainerRegistry.drainFluidContainer(single))};
    }

    private static boolean interactContainerItem(EntityPlayer player, ItemStack held, IFluidContainerItem container, IFluidHandler tank) {
        ItemStack work = held.stackSize > 1 ? ItemStacks.copyWithSize(held, 1) : held;
        FluidStack inItem = container.getFluid(work);
        if (inItem != null && inItem.amount > 0) {
            FluidStack drainable = container.drain(work, Integer.MAX_VALUE, false);
            if (drainable == null) {
                return false;
            }
            int accepted = tank.fill(drainable, false);
            if (accepted <= 0) {
                return false;
            }
            FluidStack drained = container.drain(work, accepted, true);
            tank.fill(drained, true);
        } else {
            FluidStack available = tank.drain(Integer.MAX_VALUE, false);
            if (available == null || available.amount <= 0) {
                return false;
            }
            int filled = container.fill(work, available, false);
            if (filled <= 0) {
                return false;
            }
            FluidStack toMove = available.copy();
            toMove.amount = filled;
            tank.drain(toMove, true);
            container.fill(work, toMove, true);
        }
        if (work != held) {
            consumeHeldAndGive(player, held, work);
        }
        return true;
    }

    private static void consumeHeldAndGive(EntityPlayer player, ItemStack held, ItemStack result) {
        held.stackSize--;
        if (held.stackSize <= 0) {
            player.inventory.setInventorySlotContents(player.inventory.currentItem, ItemStacks.normalize(result));
        } else if (!ItemStacks.isEmpty(result) && !player.inventory.addItemStackToInventory(result)) {
            player.dropPlayerItemWithRandomChoice(result, false);
        }
        player.inventoryContainer.detectAndSendChanges();
    }
}
