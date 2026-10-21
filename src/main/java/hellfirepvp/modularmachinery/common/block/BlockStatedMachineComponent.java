package hellfirepvp.modularmachinery.common.block;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.kasuminova.mmce.compat.IBlockState;
import github.kasuminova.mmce.compat.MathHelper;
import github.kasuminova.mmce.compat.ResourceLocation;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.block.prop.WorkingState;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 带工作状态（idle/working）的机器组件，例如示例的 crushing_wheels；也可由 CrT 动态注册。
 * <p>
 * 1.7.10 移植：工作状态存于 meta（0=idle，1=working）；贴图为 &lt;注册名&gt;_&lt;状态&gt;，不使用覆盖层。
 */
public class BlockStatedMachineComponent extends BlockMachineComponent implements BlockVariants {

    public static final List<BlockStatedMachineComponent> WAIT_FOR_REGISTRY = new ArrayList<>();

    private boolean isColoured = false;

    @SideOnly(Side.CLIENT)
    private IIcon[] stateIcons;

    public BlockStatedMachineComponent() {
        super(Material.iron);
        setHardness(2F);
        setResistance(10F);
        setStepSound(soundTypeMetal);
        setHarvestLevel("pickaxe", 1);
        setCreativeTab(CommonProxy.creativeTabModularMachinery);
    }

    private static WorkingState stateOf(int meta) {
        return WorkingState.values()[MathHelper.clamp(meta, 0, WorkingState.values().length - 1)];
    }

    @Override
    public boolean isOpaqueCube() {
        return !Objects.equals(this.getRegistryName(), new ResourceLocation(ModularMachinery.MODID, "crushing_wheels"));
    }

    @Override
    public boolean renderAsNormalBlock() {
        return isOpaqueCube();
    }

    public boolean isColoured() {
        return isColoured;
    }

    public BlockStatedMachineComponent setColoured(final boolean coloured) {
        isColoured = coloured;
        return this;
    }

    @Override
    public String getLocalizedName() {
        String prefix = isColoured ? "tile" : "block";
        return StatCollector.translateToLocal(prefix + ".modularmachinery." + getRegistryName().getPath() + ".name");
    }

    @Override
    public int getRenderType() {
        // 整块贴图，使用原版标准方块渲染
        return 0;
    }

    @Override
    public Iterable<IBlockState> getValidStates() {
        ArrayList<IBlockState> states = new ArrayList<>();
        for (WorkingState type : WorkingState.values()) {
            states.add(IBlockState.of(this, type.ordinal()));
        }
        return states;
    }

    @Override
    public String getBlockStateName(final IBlockState state) {
        return stateOf(state.getMetadata()).getName();
    }

    @Override
    public boolean hasTileEntity(int metadata) {
        return isColoured;
    }

    @Override
    public TileEntity createTileEntity(final World world, final IBlockState state) {
        return isColoured ? super.createTileEntity(world, state) : null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        super.registerBlockIcons(register);
        stateIcons = new IIcon[WorkingState.values().length];
        String path = getRegistryName() == null ? "crushing_wheels" : getRegistryName().getPath();
        for (WorkingState state : WorkingState.values()) {
            stateIcons[state.ordinal()] = register.registerIcon(ModularMachinery.MODID + ":" + path + "_" + state.getName());
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getBaseIcon(int side, int meta) {
        return stateIcons == null ? baseIcon : stateIcons[stateOf(meta).ordinal()];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int colorMultiplier(net.minecraft.world.IBlockAccess world, int x, int y, int z) {
        return isColoured ? super.colorMultiplier(world, x, y, z) : 0xFFFFFF;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderColor(int meta) {
        return isColoured ? super.getRenderColor(meta) : 0xFFFFFF;
    }
}
