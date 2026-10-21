package github.kasuminova.mmce.common.block.appeng;

import github.kasuminova.mmce.common.tile.base.MEFluidBus;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/** ME 流体总线：破坏时掉落容量卡（储罐中的流体随方块消失，同上游）。 */
public abstract class BlockMEFluidBus extends BlockMEMachineComponent {

    protected BlockMEFluidBus(String overlayName) {
        super(overlayName);
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        TileEntity te = world.getTileEntity(x, y, z);
        if (te instanceof MEFluidBus bus) {
            dropInventory(world, x, y, z, bus.getUpgrades());
        }
        super.breakBlock(world, x, y, z, block, meta);
    }
}
