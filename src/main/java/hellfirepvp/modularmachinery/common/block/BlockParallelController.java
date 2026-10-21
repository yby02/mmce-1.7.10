package hellfirepvp.modularmachinery.common.block;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.kasuminova.mmce.compat.IBlockState;
import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.block.prop.ParallelControllerData;
import hellfirepvp.modularmachinery.common.tiles.TileParallelController;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 并行控制器。1.7.10 移植：等级存于 meta。
 */
public class BlockParallelController extends BlockMachineVariant<ParallelControllerData> {

    public BlockParallelController() {
        super(ParallelControllerData.values(), "parallel_controller", 5F);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable EntityPlayer player, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        ParallelControllerData data = variantOf(stack.getItemDamage());
        tooltip.add(StatCollector.translateToLocalFormatted("tile.modularmachinery.blockparallelcontroller.tip", data.getMaxParallelism()));
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileParallelController(variantOf(state.getMetadata()).getMaxParallelism());
    }

    @Override
    public boolean onBlockActivated(World worldIn, int x, int y, int z, EntityPlayer playerIn, int side, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            TileEntity te = worldIn.getTileEntity(x, y, z);
            if (te instanceof TileParallelController) {
                openGui(playerIn, CommonProxy.GuiType.PARALLEL_CONTROLLER.ordinal(), worldIn, x, y, z);
            }
        }
        return true;
    }
}
