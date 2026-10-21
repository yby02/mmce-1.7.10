/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.util;

import github.kasuminova.mmce.compat.IBlockState;
import github.kasuminova.mmce.compat.Tuple;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: BlockCompatHelper
 * Created by HellFirePvP
 * Date: 27.11.2017 / 20:49
 * <p>
 * 1.7.10 移植：上游针对 IC2 1.12 统一 TE 方块（ic2:te）的状态恢复逻辑在 IC2 1.7.10 中没有对应结构（1.7.10 的 IC2 机器以 meta 区分），
 * 因此只保留按 block + meta 创建 TileEntity 的通用路径，供结构预览渲染使用。
 */
public class BlockCompatHelper {

    @Nonnull
    public static Tuple<IBlockState, TileEntity> transformState(IBlockState state, @Nullable NBTTagCompound matchTag, BlockArray.TileInstantiateContext context) {
        Block block = state.getBlock();
        int meta = state.getMetadata();
        TileEntity te = block.hasTileEntity(meta) ? block.createTileEntity(context.world(), meta) : null;
        if (te != null) {
            context.apply(te);
        }
        return new Tuple<>(state, te);
    }
}
