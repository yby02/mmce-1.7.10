package hellfirepvp.modularmachinery.common.block;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.kasuminova.mmce.compat.IBlockState;
import github.kasuminova.mmce.compat.IStringSerializable;
import github.kasuminova.mmce.compat.MathHelper;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.CommonProxy;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import java.util.LinkedList;
import java.util.List;

/**
 * 1.7.10 移植：1.12.2 中“单个 PropertyEnum 变体”的机器组件方块的公共实现。
 * 变体序号即 meta；覆盖层贴图为 overlay_&lt;前缀&gt;_&lt;变体名&gt;。
 */
public abstract class BlockMachineVariant<E extends Enum<E> & IStringSerializable> extends BlockMachineComponent
    implements BlockCustomName, BlockVariants {

    private final E[] variants;
    private final String overlayPrefix;

    @SideOnly(Side.CLIENT)
    private IIcon[] overlays;

    protected BlockMachineVariant(E[] variants, String overlayPrefix, float hardness) {
        super(Material.iron);
        this.variants = variants;
        this.overlayPrefix = overlayPrefix;
        setHardness(hardness);
        setResistance(10F);
        setStepSound(soundTypeMetal);
        setHarvestLevel("pickaxe", 1);
        setCreativeTab(CommonProxy.creativeTabModularMachinery);
    }

    public E variantOf(int meta) {
        return variants[MathHelper.clamp(meta, 0, variants.length - 1)];
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        for (E variant : variants) {
            list.add(new ItemStack(item, 1, variant.ordinal()));
        }
    }

    @Override
    public int damageDropped(int meta) {
        return variantOf(meta).ordinal();
    }

    @Override
    public String getIdentifierForMeta(int meta) {
        return variantOf(meta).getName();
    }

    @Override
    public Iterable<IBlockState> getValidStates() {
        List<IBlockState> states = new LinkedList<>();
        for (E variant : variants) {
            states.add(IBlockState.of(this, variant.ordinal()));
        }
        return states;
    }

    @Override
    public String getBlockStateName(IBlockState state) {
        return variantOf(state.getMetadata()).getName();
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected void registerOverlayIcons(IIconRegister register) {
        overlays = new IIcon[variants.length];
        for (E variant : variants) {
            overlays[variant.ordinal()] = register.registerIcon(ModularMachinery.MODID + ":overlay_" + overlayPrefix + "_" + variant.getName());
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getOverlayIcon(int side, int meta) {
        return overlays == null ? null : overlays[variantOf(meta).ordinal()];
    }
}
