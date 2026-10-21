/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.block;

import github.kasuminova.mmce.compat.IBlockState;
import hellfirepvp.modularmachinery.common.tiles.TileItemOutputBus;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: BlockOutputBus
 * Created by HellFirePvP
 * Date: 07.07.2017 / 18:31
 */
public class BlockOutputBus extends BlockBus {

    @Override
    protected String getOverlayPrefix() {
        return "outputbus";
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileItemOutputBus(sizeOf(state.getMetadata()));
    }
}
