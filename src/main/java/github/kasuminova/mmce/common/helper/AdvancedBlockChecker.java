package github.kasuminova.mmce.common.helper;

import github.kasuminova.mmce.compat.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import github.kasuminova.mmce.compat.BlockPos;
import net.minecraft.world.World;

@FunctionalInterface
public interface AdvancedBlockChecker {
    boolean isMatch(World world, BlockPos pos, IBlockState blockState, NBTTagCompound nbt);
}
