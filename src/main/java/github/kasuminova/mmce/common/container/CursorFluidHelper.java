package github.kasuminova.mmce.common.container;

import github.kasuminova.mmce.compat.ItemStacks;
import github.kasuminova.mmce.compat.fluids.FluidContainerInteraction;
import github.kasuminova.mmce.compat.fluids.IFluidHandler;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

import javax.annotation.Nullable;

/** 界面中用鼠标上拿着的流体容器（而不是手持物品）与储罐交互：满容器倒入，空容器装出，一次处理一个容器。 */
public final class CursorFluidHelper {

    private CursorFluidHelper() {
    }

    /** 鼠标上的容器中装的流体（不是流体容器或为空时返回 null）。 */
    @Nullable
    public static FluidStack fluidIn(ItemStack stack) {
        if (ItemStacks.isEmpty(stack)) {
            return null;
        }
        if (stack.getItem() instanceof IFluidContainerItem container) {
            FluidStack fluid = container.getFluid(stack);
            return fluid != null && fluid.amount > 0 ? fluid : null;
        }
        return FluidContainerRegistry.getFluidForFilledItem(stack);
    }

    public static boolean interact(EntityPlayerMP player, IFluidHandler tank) {
        ItemStack held = player.inventory.getItemStack();
        if (ItemStacks.isEmpty(held)) {
            return false;
        }
        ItemStack result = null;
        boolean done = false;

        ItemStack[] emptied = FluidContainerInteraction.tryEmptyContainer(held, tank, Integer.MAX_VALUE, true);
        if (emptied != null) {
            result = emptied[0];
            done = true;
        } else {
            FluidStack available = tank.drain(Integer.MAX_VALUE, false);
            if (available != null && available.amount > 0) {
                ItemStack single = ItemStacks.copyWithSize(held, 1);
                if (single.getItem() instanceof IFluidContainerItem container) {
                    int filled = container.fill(single, available, false);
                    if (filled > 0) {
                        FluidStack drained = tank.drain(new FluidStack(available, filled), true);
                        if (drained != null) {
                            container.fill(single, drained, true);
                            result = single;
                            done = true;
                        }
                    }
                } else {
                    ItemStack filledItem = FluidContainerRegistry.fillFluidContainer(available, single);
                    FluidStack inFilled = filledItem == null ? null : FluidContainerRegistry.getFluidForFilledItem(filledItem);
                    if (inFilled != null) {
                        FluidStack drainable = tank.drain(inFilled, false);
                        if (drainable != null && drainable.amount >= inFilled.amount) {
                            tank.drain(inFilled, true);
                            result = filledItem;
                            done = true;
                        }
                    }
                }
            }
        }
        if (!done) {
            return false;
        }
        if (held.stackSize > 1) {
            held.stackSize--;
            if (!ItemStacks.isEmpty(result) && !player.inventory.addItemStackToInventory(result)) {
                player.dropPlayerItemWithRandomChoice(result, false);
            }
            player.inventoryContainer.detectAndSendChanges();
        } else {
            player.inventory.setItemStack(ItemStacks.normalize(result));
        }
        player.updateHeldItem();
        return true;
    }
}
