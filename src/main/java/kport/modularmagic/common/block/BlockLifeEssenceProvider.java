package kport.modularmagic.common.block;

import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.CommonProxy;
import kport.modularmagic.common.tile.TileLifeEssenceProvider;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import java.util.function.Supplier;

/** 生命精华仓（血魔法 1）：右键打开放血之宝珠的界面；破坏时掉落宝珠。 */
public abstract class BlockLifeEssenceProvider extends BlockMagicProvider {

    protected BlockLifeEssenceProvider(String overlayName, Supplier<TileEntity> tileFactory) {
        super(overlayName, tileFactory);
    }

    @Override
    protected void describe(TileEntity te, EntityPlayer player) {
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        if (player.isSneaking() && player.getHeldItem() == null) {
            return false;
        }
        if (!world.isRemote && world.getTileEntity(x, y, z) instanceof TileLifeEssenceProvider) {
            player.openGui(ModularMachinery.instance, CommonProxy.GuiType.GUI_ESSENCE_PROVIDER.ordinal(), world, x, y, z);
        }
        return true;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        if (world.getTileEntity(x, y, z) instanceof TileLifeEssenceProvider te) {
            dropInventory(world, x, y, z, te.getInventory());
        }
        super.breakBlock(world, x, y, z, block, meta);
    }
}
