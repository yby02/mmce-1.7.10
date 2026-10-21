package github.kasuminova.mmce.common.block.appeng;

import github.kasuminova.mmce.common.tile.MEFluidOutputBus;
import github.kasuminova.mmce.compat.IBlockState;
import hellfirepvp.modularmachinery.common.CommonProxy;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/** ME 机械流体输出总线。 */
public class BlockMEFluidOutputBus extends BlockMEFluidBus {

    public BlockMEFluidOutputBus() {
        super("mefluidoutputbus");
    }

    @Override
    public boolean onBlockActivated(World worldIn, int x, int y, int z, EntityPlayer playerIn, int side, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote && worldIn.getTileEntity(x, y, z) instanceof MEFluidOutputBus) {
            openGui(playerIn, CommonProxy.GuiType.ME_FLUID_OUTPUT_BUS.ordinal(), worldIn, x, y, z);
        }
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new MEFluidOutputBus();
    }
}
