package ink.ikx.mmce.common.utils;

import net.minecraftforge.fluids.IFluidContainerItem;

import net.minecraftforge.fluids.FluidContainerRegistry;

import github.kasuminova.mmce.compat.fluids.FluidContainerInteraction;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import github.kasuminova.mmce.compat.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidBlock;

import java.util.Arrays;

public class FluidUtils {

    public static boolean isFluidHandler(ItemStack stack) {
        return StackUtils.isNotEmpty(stack) && FluidContainerInteraction.isFluidContainer(stack);
    }

    public static boolean areFluidHandler(ItemStack... stacks) {
        return Arrays.stream(stacks).allMatch(FluidUtils::isFluidHandler);
    }

    public static FluidStack getFluidFromHandler(ItemStack stack) {
        // 1.7.10 移植：IFluidContainerItem 或 FluidContainerRegistry 登记的容器
        if (!isFluidHandler(stack)) {
            return null;
        }
        if (stack.getItem() instanceof IFluidContainerItem) {
            return ((IFluidContainerItem) stack.getItem()).getFluid(stack);
        }
        return FluidContainerRegistry.getFluidForFilledItem(stack);
    }

    public static boolean equalsFluidFromStack(ItemStack stackA, ItemStack stackB) {
        FluidStack fluidA = getFluidFromHandler(stackA);
        FluidStack fluidB = getFluidFromHandler(stackB);
        if (MiscUtils.areNull(fluidA, fluidB)) {
            return false;
        }
        return fluidA.containsFluid(fluidB);
    }

    public static FluidStack getFluidStackFromBlockState(final IBlockState state) {
        Block block = state.getBlock();
        if (block instanceof BlockFluidBase) {
            return new FluidStack(((IFluidBlock) block).getFluid(), 1000);
        } else if (block instanceof BlockLiquid) {
            Material material = block.getMaterial();
            if (material == Material.water) {
                return new FluidStack(FluidRegistry.WATER, 1000);
            } else if (material == Material.lava) {
                return new FluidStack(FluidRegistry.LAVA, 1000);
            }
        }

        return null;
    }
}
