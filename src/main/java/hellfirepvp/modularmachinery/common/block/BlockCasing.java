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
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: BlockCasing
 * Created by HellFirePvP
 * Date: 29.06.2017 / 17:32
 * <p>
 * 1.7.10 移植：外壳类型直接存于 meta（0=plain ... 5=circuitry，与 1.12.2 相同），结构 JSON 中的 blockcasing@N 写法保持不变。
 */
public class BlockCasing extends BlockMachineComponent implements BlockCustomName, BlockVariants {

    @SideOnly(Side.CLIENT)
    private IIcon[] overlays;

    public BlockCasing() {
        super(Material.iron);
        setHardness(2F);
        setResistance(10F);
        setStepSound(soundTypeMetal);
        setHarvestLevel("pickaxe", 1);
        setCreativeTab(CommonProxy.creativeTabModularMachinery);
    }

    private static CasingType typeOf(int meta) {
        return CasingType.values()[MathHelper.clamp(meta, 0, CasingType.values().length - 1)];
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        for (CasingType type : CasingType.values()) {
            list.add(new ItemStack(item, 1, type.ordinal()));
        }
    }

    @Override
    public int damageDropped(int meta) {
        return typeOf(meta).ordinal();
    }

    @Override
    public String getIdentifierForMeta(int meta) {
        return typeOf(meta).getName();
    }

    @Override
    public Iterable<IBlockState> getValidStates() {
        List<IBlockState> ret = new LinkedList<>();
        for (CasingType type : CasingType.values()) {
            ret.add(IBlockState.of(this, type.ordinal()));
        }
        return ret;
    }

    @Override
    public String getBlockStateName(IBlockState state) {
        return typeOf(state.getMetadata()).getName();
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected void registerOverlayIcons(IIconRegister register) {
        overlays = new IIcon[CasingType.values().length];
        for (CasingType type : CasingType.values()) {
            if (type != CasingType.PLAIN) {
                overlays[type.ordinal()] = register.registerIcon(ModularMachinery.MODID + ":overlay_" + type.getName());
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getOverlayIcon(int side, int meta) {
        return overlays == null ? null : overlays[typeOf(meta).ordinal()];
    }

    public enum CasingType implements IStringSerializable {

        PLAIN,
        VENT,
        FIREBOX,
        GEARBOX,
        REINFORCED,
        CIRCUITRY;

        @Override
        public String getName() {
            return name().toLowerCase();
        }

    }

}
