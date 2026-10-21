package hellfirepvp.modularmachinery.common.block;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.kasuminova.mmce.compat.IBlockState;
import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.block.prop.UpgradeBusData;
import hellfirepvp.modularmachinery.common.tiles.TileUpgradeBus;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 升级总线。1.7.10 移植：等级存于 meta。
 */
public class BlockUpgradeBus extends BlockMachineVariant<UpgradeBusData> {

    public BlockUpgradeBus() {
        super(UpgradeBusData.values(), "upgrade_bus", 5F);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable EntityPlayer player, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        UpgradeBusData data = variantOf(stack.getItemDamage());
        tooltip.add(StatCollector.translateToLocalFormatted("tile.modularmachinery.blockupgradebus.tip", data.getMaxUpgradeSlot()));
    }

    @Override
    public void breakBlock(World worldIn, int x, int y, int z, Block block, int meta) {
        TileEntity te = worldIn.getTileEntity(x, y, z);
        if (te instanceof TileUpgradeBus) {
            dropInventory(worldIn, x, y, z, ((TileUpgradeBus) te).getInventory());
        }
        super.breakBlock(worldIn, x, y, z, block, meta);
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileUpgradeBus(variantOf(state.getMetadata()).getMaxUpgradeSlot());
    }

    @Override
    public boolean onBlockActivated(World worldIn, int x, int y, int z, EntityPlayer playerIn, int side, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            TileEntity te = worldIn.getTileEntity(x, y, z);
            if (te instanceof TileUpgradeBus) {
                openGui(playerIn, CommonProxy.GuiType.UPGRADE_BUS.ordinal(), worldIn, x, y, z);
            }
        }
        return true;
    }
}
