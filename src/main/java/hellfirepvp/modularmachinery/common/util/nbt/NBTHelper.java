/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.util.nbt;

import github.kasuminova.mmce.compat.IBlockState;
import github.kasuminova.mmce.compat.RegistryNames;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: NBTHelper
 * Created by HellFirePvP
 * Date: 28.11.2018 / 16:53
 * <p>
 * 1.7.10 移植：1.12.2 以“注册名 + 属性列表”序列化方块状态；1.7.10 的方块状态即“方块 + meta”，
 * 改为写入 registryName 与 meta。读取时兼容缺少 meta 的旧数据（按 0 处理）。
 */
public class NBTHelper {

    public static void setBlockState(NBTTagCompound cmp, String key, IBlockState state) {
        NBTTagCompound serialized = getBlockStateNBTTag(state);
        if (serialized != null) {
            cmp.setTag(key, serialized);
        }
    }

    @Nullable
    public static IBlockState getBlockState(NBTTagCompound cmp, String key) {
        return getBlockStateFromTag(cmp.getCompoundTag(key));
    }

    @Nullable
    public static NBTTagCompound getBlockStateNBTTag(IBlockState state) {
        if (state == null || state.getBlock() == null || state.getBlock() == Blocks.air) {
            return null;
        }
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("registryName", RegistryNames.getName(state.getBlock()));
        tag.setInteger("meta", state.getMetadata());
        return tag;
    }

    @Nullable
    public static IBlockState getBlockStateFromTag(NBTTagCompound cmp) {
        if (!cmp.hasKey("registryName")) {
            return null;
        }
        Block block = RegistryNames.findBlock(cmp.getString("registryName"));
        if (block == null || block == Blocks.air) {
            return null;
        }
        return IBlockState.of(block, cmp.getInteger("meta"));
    }

}
