/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.block;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.kasuminova.mmce.compat.IBlockState;
import hellfirepvp.modularmachinery.client.util.EnergyDisplayUtil;
import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.block.prop.EnergyHatchData;
import hellfirepvp.modularmachinery.common.tiles.TileEnergyOutputHatch;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: BlockEnergyOutputHatch
 * Created by HellFirePvP
 * Date: 08.07.2017 / 12:43
 */
public class BlockEnergyOutputHatch extends BlockEnergyHatch {

    @Override
    protected String getOverlayPrefix() {
        return "energyoutputhatch";
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable EntityPlayer player, List<String> tooltip, boolean advanced) {
        EnergyHatchData size = sizeOf(stack.getItemDamage());
        if (EnergyDisplayUtil.displayFETooltip) {
            tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocalFormatted("tooltip.energyhatch.storage", MiscUtils.formatDecimal(size.maxEnergy)));
            tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocalFormatted("tooltip.energyhatch.out.transfer", MiscUtils.formatDecimal(size.transferLimit)));
            tooltip.add("");
        }
        if (Mods.IC2.isPresent() && EnergyDisplayUtil.displayIC2EUTooltip) {
            tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocalFormatted("tooltip.energyhatch.ic2.out.voltage",
                EnumChatFormatting.BLUE + StatCollector.translateToLocal(size.getUnlocalizedEnergyDescriptor())));
            tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocalFormatted("tooltip.energyhatch.ic2.out.transfer",
                EnumChatFormatting.BLUE + MiscUtils.formatDecimal(size.getIC2EnergyTransmission()),
                EnumChatFormatting.BLUE + StatCollector.translateToLocal("tooltip.energyhatch.ic2.powerrate")));
            tooltip.add("");
        }
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEnergyOutputHatch(sizeOf(state.getMetadata()));
    }
}
