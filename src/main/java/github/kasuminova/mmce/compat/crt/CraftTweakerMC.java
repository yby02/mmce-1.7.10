package github.kasuminova.mmce.compat.crt;

import github.kasuminova.mmce.compat.BlockPos;
import github.kasuminova.mmce.compat.EnumFacing;
import minetweaker.api.data.IData;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import minetweaker.api.liquid.ILiquidStack;
import minetweaker.api.minecraft.MineTweakerMC;
import minetweaker.api.player.IPlayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;

/**
 * 1.7.10 移植适配：CraftTweaker 1.12 crafttweaker.api.minecraft.CraftTweakerMC 的门面。
 * 能直接映射的转发给 MineTweakerMC，MT3 缺失的世界相关类型由本包的同名 ZenClass 提供。
 */
public final class CraftTweakerMC {

    private CraftTweakerMC() {
    }

    public static ItemStack getItemStack(IItemStack item) {
        return MineTweakerMC.getItemStack(item);
    }

    public static ItemStack getItemStack(IIngredient ingredient) {
        return MineTweakerMC.getItemStack(ingredient);
    }

    public static IItemStack getIItemStack(ItemStack stack) {
        return MineTweakerMC.getIItemStack(stack);
    }

    /** MT3 的 IItemStack 不区分可变/不可变，等价于 getIItemStack。 */
    public static IItemStack getIItemStackMutable(ItemStack stack) {
        return MineTweakerMC.getIItemStack(stack);
    }

    public static FluidStack getLiquidStack(ILiquidStack stack) {
        return MineTweakerMC.getLiquidStack(stack);
    }

    public static NBTTagCompound getNBTCompound(IData data) {
        return data == null ? null : MineTweakerMC.getNBTCompound(data);
    }

    public static IData getIData(NBTBase nbt) {
        return MineTweakerMC.getIData(nbt);
    }

    public static IData getIDataModifyable(NBTBase nbt) {
        return MineTweakerMC.getIDataModifyable(nbt);
    }

    public static IPlayer getIPlayer(EntityPlayer player) {
        return MineTweakerMC.getIPlayer(player);
    }

    public static IBlockPos getIBlockPos(BlockPos pos) {
        return pos == null ? null : new IBlockPos(pos);
    }

    public static BlockPos getBlockPos(IBlockPos pos) {
        return pos == null ? null : pos.getInternal();
    }

    public static IWorld getIWorld(World world) {
        return world == null ? null : new IWorld(world);
    }

    public static World getWorld(IWorld world) {
        return world == null ? null : world.getInternal();
    }

    public static IFacing getIFacing(EnumFacing facing) {
        return IFacing.of(facing);
    }

    public static github.kasuminova.mmce.compat.IBlockState getBlockState(IBlockState state) {
        return state == null ? null : state.getInternal();
    }

    public static IBlockState getBlockState(github.kasuminova.mmce.compat.IBlockState state) {
        return state == null ? null : new IBlockState(state);
    }

    public static AxisAlignedBB getAxisAlignedBB(IAxisAlignedBB box) {
        return box == null ? null : box.getInternal();
    }
}
