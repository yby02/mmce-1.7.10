package hellfirepvp.modularmachinery.common.block;

import github.kasuminova.mmce.compat.IBlockState;
import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.block.prop.SmartInterfaceTypeEnum;
import hellfirepvp.modularmachinery.common.tiles.TileSmartInterface;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * 智能数据接口。1.7.10 移植：类型存于 meta。
 */
public class BlockSmartInterface extends BlockMachineVariant<SmartInterfaceTypeEnum> {

    public BlockSmartInterface() {
        super(SmartInterfaceTypeEnum.values(), "smartinterface", 5F);
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileSmartInterface();
    }

    @Override
    public boolean onBlockActivated(World worldIn, int x, int y, int z, EntityPlayer playerIn, int side, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            TileEntity te = worldIn.getTileEntity(x, y, z);
            if (te instanceof TileSmartInterface) {
                openGui(playerIn, CommonProxy.GuiType.SMART_INTERFACE.ordinal(), worldIn, x, y, z);
            }
        }
        return true;
    }
}
