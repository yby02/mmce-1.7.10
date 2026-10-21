package github.kasuminova.mmce.common.block.appeng;

import appeng.api.implementations.items.IMemoryCard;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.kasuminova.mmce.common.tile.MEFluidInputBus;
import github.kasuminova.mmce.compat.IBlockState;
import hellfirepvp.modularmachinery.common.CommonProxy;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/** ME 机械流体输入总线。 */
public class BlockMEFluidInputBus extends BlockMEFluidBus {

    public BlockMEFluidInputBus() {
        super("mefluidinputbus");
    }

    @Override
    public boolean onBlockActivated(World worldIn, int x, int y, int z, EntityPlayer playerIn, int side, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            TileEntity te = worldIn.getTileEntity(x, y, z);
            if (te instanceof MEFluidInputBus bus) {
                ItemStack heldItem = playerIn.getHeldItem();
                if (heldItem != null && heldItem.getItem() instanceof IMemoryCard memoryCard && handleSettingsTransfer(bus, memoryCard, playerIn, heldItem)) {
                    return true;
                }
                openGui(playerIn, CommonProxy.GuiType.ME_FLUID_INPUT_BUS.ordinal(), worldIn, x, y, z);
            }
        }
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new MEFluidInputBus();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable EntityPlayer player, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add(StatCollector.translateToLocal("tooltip.groupinput.block"));
    }
}
