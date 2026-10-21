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
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.block.prop.EnergyHatchData;
import hellfirepvp.modularmachinery.common.tiles.base.TileEnergyHatch;
import hellfirepvp.modularmachinery.common.util.RedstoneHelper;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: BlockEnergyHatch
 * Created by HellFirePvP
 * Date: 08.07.2017 / 10:50
 * <p>
 * 1.7.10 移植：等级存于 meta（与 1.12.2 的 EnergyHatchData 序号相同）。
 * GTCEu 专用的电压/电流提示已移除，GT5 EU 兼容在 P5 实现。
 */
public abstract class BlockEnergyHatch extends BlockMachineComponent implements BlockCustomName, BlockVariants {

    @SideOnly(Side.CLIENT)
    private IIcon[] overlays;

    public BlockEnergyHatch() {
        super(Material.iron);
        setHardness(2F);
        setResistance(10F);
        setStepSound(soundTypeMetal);
        setHarvestLevel("pickaxe", 1);
        setCreativeTab(CommonProxy.creativeTabModularMachinery);
    }

    protected static EnergyHatchData sizeOf(int meta) {
        return EnergyHatchData.values()[MathHelper.clamp(meta, 0, EnergyHatchData.values().length - 1)];
    }

    /** 覆盖层贴图前缀，如 energyinputhatch / energyoutputhatch。 */
    protected abstract String getOverlayPrefix();

    @Override
    @SideOnly(Side.CLIENT)
    public abstract void addInformation(ItemStack stack, @Nullable EntityPlayer player, List<String> tooltip, boolean advanced);

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        for (EnergyHatchData size : EnergyHatchData.values()) {
            list.add(new ItemStack(item, 1, size.ordinal()));
        }
    }

    @Override
    public int damageDropped(int meta) {
        return sizeOf(meta).ordinal();
    }

    @Override
    public Iterable<IBlockState> getValidStates() {
        List<IBlockState> ret = new LinkedList<>();
        for (EnergyHatchData type : EnergyHatchData.values()) {
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
    public boolean onBlockActivated(World worldIn, int x, int y, int z, EntityPlayer playerIn, int side, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            TileEntity te = worldIn.getTileEntity(x, y, z);
            if (te instanceof TileEnergyHatch) {
                openGui(playerIn, CommonProxy.GuiType.ENERGY_INVENTORY.ordinal(), worldIn, x, y, z);
            }
        }
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected void registerOverlayIcons(IIconRegister register) {
        overlays = new IIcon[EnergyHatchData.values().length];
        for (EnergyHatchData size : EnergyHatchData.values()) {
            overlays[size.ordinal()] = register.registerIcon(ModularMachinery.MODID + ":overlay_" + getOverlayPrefix() + "_" + size.getName());
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getOverlayIcon(int side, int meta) {
        return overlays == null ? null : overlays[sizeOf(meta).ordinal()];
    }
}
