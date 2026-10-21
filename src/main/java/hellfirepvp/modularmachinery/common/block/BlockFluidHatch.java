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
import github.kasuminova.mmce.compat.MathHelper;
import github.kasuminova.mmce.compat.fluids.FluidContainerInteraction;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.block.prop.FluidHatchSize;
import hellfirepvp.modularmachinery.common.tiles.base.TileFluidTank;
import hellfirepvp.modularmachinery.common.util.HybridTank;
import hellfirepvp.modularmachinery.common.util.RedstoneHelper;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: BlockFluidHatch
 * Created by HellFirePvP
 * Date: 07.07.2017 / 17:59
 * <p>
 * 1.7.10 移植：尺寸存于 meta；手持流体容器右键时直接与储罐交互（{@link FluidContainerInteraction}），否则打开 GUI。
 */
public abstract class BlockFluidHatch extends BlockMachineComponent implements BlockCustomName, BlockVariants {

    @SideOnly(Side.CLIENT)
    private IIcon[] overlays;

    public BlockFluidHatch() {
        super(Material.iron);
        setHardness(2F);
        setResistance(10F);
        setStepSound(soundTypeMetal);
        setHarvestLevel("pickaxe", 1);
        setCreativeTab(CommonProxy.creativeTabModularMachinery);
    }

    protected static FluidHatchSize sizeOf(int meta) {
        return FluidHatchSize.values()[MathHelper.clamp(meta, 0, FluidHatchSize.values().length - 1)];
    }

    /** 覆盖层贴图前缀，如 fluidinputhatch / fluidoutputhatch。 */
    protected abstract String getOverlayPrefix();

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        for (FluidHatchSize size : FluidHatchSize.values()) {
            list.add(new ItemStack(item, 1, size.ordinal()));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable EntityPlayer player, List<String> tooltip, boolean advanced) {
        FluidHatchSize size = sizeOf(stack.getItemDamage());
        tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocalFormatted("tooltip.fluidhatch.tank.info", size.getSize()));
        if (Mods.MEKANISM.isPresent()) {
            tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("tooltip.fluidhatch.tank.mek"));
        }
    }

    @Override
    public boolean onBlockActivated(World worldIn, int x, int y, int z, EntityPlayer playerIn, int side, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            TileEntity te = worldIn.getTileEntity(x, y, z);
            if (te instanceof TileFluidTank) {
                ItemStack activeHand = playerIn.getCurrentEquippedItem();
                if (FluidContainerInteraction.isFluidContainer(activeHand)) {
                    HybridTank ft = ((TileFluidTank) te).getTank();
                    boolean canFillPrev = ft.canFill();
                    boolean canDrainPrev = ft.canDrain();
                    ft.setCanFill(true);
                    ft.setCanDrain(true);
                    try {
                        FluidContainerInteraction.interact(playerIn, ft);
                    } finally {
                        ft.setCanFill(canFillPrev);
                        ft.setCanDrain(canDrainPrev);
                    }
                    return true;
                }
                openGui(playerIn, CommonProxy.GuiType.TANK_INVENTORY.ordinal(), worldIn, x, y, z);
            }
        }
        return true;
    }

    @Override
    public int damageDropped(int meta) {
        return sizeOf(meta).ordinal();
    }

    @Override
    public Iterable<IBlockState> getValidStates() {
        List<IBlockState> ret = new LinkedList<>();
        for (FluidHatchSize type : FluidHatchSize.values()) {
            ret.add(IBlockState.of(this, type.ordinal()));
        }
        return ret;
    }

    @Override
    public String getBlockStateName(IBlockState state) {
        return sizeOf(state.getMetadata()).getName();
    }

    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(World worldIn, int x, int y, int z, int side) {
        return RedstoneHelper.getRedstoneLevel(worldIn.getTileEntity(x, y, z));
    }

    @Nullable
    @Override
    public abstract TileEntity createTileEntity(World world, IBlockState state);

    @Override
    public String getIdentifierForMeta(int meta) {
        return sizeOf(meta).getName();
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected void registerOverlayIcons(IIconRegister register) {
        overlays = new IIcon[FluidHatchSize.values().length];
        for (FluidHatchSize size : FluidHatchSize.values()) {
            overlays[size.ordinal()] = register.registerIcon(ModularMachinery.MODID + ":overlay_" + getOverlayPrefix() + "_" + size.getName());
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getOverlayIcon(int side, int meta) {
        return overlays == null ? null : overlays[sizeOf(meta).ordinal()];
    }
}
