package ink.ikx.mmce.common.utils;

import net.minecraftforge.fluids.FluidContainerRegistry;

import github.kasuminova.mmce.compat.Worlds;

import github.kasuminova.mmce.compat.ItemStacks;

import github.kasuminova.mmce.common.machine.pattern.SpecialItemBlockProxy;
import github.kasuminova.mmce.common.machine.pattern.SpecialItemBlockProxyRegistry;
import hellfirepvp.modularmachinery.common.block.BlockController;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import github.kasuminova.mmce.compat.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBucket;
import net.minecraft.item.ItemStack;
import github.kasuminova.mmce.compat.EnumFacing;
import github.kasuminova.mmce.compat.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidBlock;

import java.util.List;

public class StackUtils {

    public static boolean isNotEmpty(ItemStack stack) {
        return !ItemStacks.isEmpty(stack) && stack.getItem() != Items.milk_bucket;
    }

    public static boolean isStackFilter(ItemStack stack) {
        return isNotEmpty(stack) && !(Block.getBlockFromItem(stack.getItem()) instanceof BlockController);
    }

    public static ItemStack getStackFromBlockState(IBlockState state) {
        Block block = state.getBlock();
        if (block instanceof BlockFluidBase) {
            return ItemStacks.normalize(FluidContainerRegistry.fillFluidContainer(new FluidStack(((IFluidBlock) block).getFluid(), 1000), new ItemStack(Items.bucket)));
        } else if (block instanceof BlockLiquid) {
            Material m = block.getMaterial();
            if (m == Material.lava) {
                return new ItemStack(Items.lava_bucket);
            } else if (m == Material.water) {
                return new ItemStack(Items.water_bucket);
            }
        }
        Item item = Item.getItemFromBlock(block);
        if (item == null) {
            return ItemStacks.EMPTY;
        }
        return new ItemStack(item, 1, block.damageDropped(state.getMetadata()));
    }

    public static ItemStack getStackFromBlockState(IBlockState state, BlockPos pos, World world) {
        ItemStack rawStack = ItemStacks.EMPTY;
        try {
            rawStack = state.getBlock().getPickBlock(new MovingObjectPosition(pos.getX(), pos.getY(), pos.getZ(), EnumFacing.UP.getIndex(), Vec3.createVectorHelper(0, 0, 0)), world, pos.getX(), pos.getY(), pos.getZ());
        } catch (Exception ignored) {
        }
        if (ItemStacks.isEmpty(rawStack)) {
            rawStack = getStackFromBlockState(state);
        }
        SpecialItemBlockProxy specialItemBlockProxy = SpecialItemBlockProxyRegistry.INSTANCE.getValidProxy(rawStack);
        if (specialItemBlockProxy != null) {
            return specialItemBlockProxy.getTrueStack(Worlds.getBlockState(world, pos), Worlds.getTileEntity(world, pos));
        }
        return rawStack;
    }

    public static ItemStack hasStacks(List<ItemStack> inputStacks, List<ItemStack> outputStacks, boolean isRemove) {
        return outputStacks.stream().filter(stack -> isNotEmpty(hasStack(stack, inputStacks, isRemove))).findFirst().orElse(ItemStacks.EMPTY);
    }

    public static int getIndex(List<ItemStack> stacks, ItemStack stack) {
        for (int i = 0; i < stacks.size(); i++) {
            if (stacks.get(i).isItemEqual(stack)) {
                return i;
            }
        }
        return -1;
    }

    public static ItemStack hasStack(ItemStack stack, List<ItemStack> stacks, boolean isRemove) {
        for (ItemStack stackInSlot : stacks) {
            ItemStack copy = stackInSlot.copy();
            if (ItemStacks.isEmpty(stackInSlot)) {
                continue;
            }
            if (FluidUtils.areFluidHandler(stack, stackInSlot)) {
                if (FluidUtils.equalsFluidFromStack(stackInSlot, stack)) {
                    if (stackInSlot.getItem() instanceof ItemBucket) {
                        if (isRemove) {
                            ItemStacks.shrink(stackInSlot, ItemStacks.getCount(stack));
                        }
                        return copy;
                    }
                    return ItemStacks.EMPTY;
                }
            } else if (stackInSlot.isItemEqual(stack)) {
                if (ItemStacks.getCount(stackInSlot) >= ItemStacks.getCount(stack)) {
                    if (isRemove) {
                        ItemStacks.shrink(stackInSlot, ItemStacks.getCount(stack));
                    }
                    return copy;
                }
            }
        }
        return ItemStacks.EMPTY;
    }

}
