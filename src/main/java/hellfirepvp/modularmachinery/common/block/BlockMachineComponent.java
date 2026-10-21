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
import github.kasuminova.mmce.compat.BlockPos;
import github.kasuminova.mmce.compat.IBlockState;
import github.kasuminova.mmce.compat.ItemStacks;
import github.kasuminova.mmce.compat.items.IItemHandlerModifiable;
import github.kasuminova.mmce.compat.registry.IRegistryNamed;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.block.render.MachineBlockRenderType;
import hellfirepvp.modularmachinery.common.data.Config;
import hellfirepvp.modularmachinery.common.tiles.base.ColorableMachineTile;
import hellfirepvp.modularmachinery.common.tiles.base.TileColorableMachineComponent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import javax.annotation.Nullable;
import java.util.List;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: BlockMachineComponent
 * Created by HellFirePvP
 * Date: 15.08.2017 / 16:26
 * <p>
 * 1.7.10 移植：
 * - 方块变体（尺寸/外壳类型/朝向）直接编码在 meta 中，{@link IBlockState} 为“方块 + meta”。
 * - 保留 1.12.2 风格的 {@link #createTileEntity(World, IBlockState)} 与 {@link #addInformation}，由本类桥接到 1.7.10 的 API，
 *   子类改动最小化。
 * - 外壳着色：底层贴图使用机器颜色着色，覆盖层（overlay）不着色，由客户端的 ISBRH 渲染（见 P2）。
 */
public abstract class BlockMachineComponent extends BlockContainer implements BlockDynamicColor, IRegistryNamed {

    public static final String BASE_TEXTURE = ModularMachinery.MODID + ":blockcasing_plain";

    @SideOnly(Side.CLIENT)
    protected IIcon baseIcon;
    /** 覆盖层渲染时，没有覆盖层的面使用的全透明贴图（防止 renderAllFaces 时把底层再画一遍白色）。 */
    @SideOnly(Side.CLIENT)
    protected static IIcon transparentIcon;

    protected BlockMachineComponent(Material materialIn) {
        super(materialIn);
    }

    // ---------------- TileEntity ----------------

    /** 1.12.2 风格入口，子类按需覆盖。 */
    @Nullable
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileColorableMachineComponent();
    }

    @Override
    public TileEntity createTileEntity(World world, int metadata) {
        return createTileEntity(world, IBlockState.of(this, metadata));
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return createTileEntity(worldIn, IBlockState.of(this, meta));
    }

    @Override
    public boolean hasTileEntity(int metadata) {
        return true;
    }

    // ---------------- 状态（meta） ----------------

    public IBlockState getDefaultState() {
        return IBlockState.of(this, 0);
    }

    public IBlockState getStateFromMeta(int meta) {
        return IBlockState.of(this, meta);
    }

    public int getMetaFromState(IBlockState state) {
        return state.getMetadata();
    }

    // ---------------- 颜色 ----------------

    @Override
    public int getColorMultiplier(IBlockState state, @Nullable IBlockAccess worldIn, @Nullable BlockPos pos, int tintIndex) {
        if (worldIn == null || pos == null) {
            return Config.machineColor;
        }
        TileEntity te = worldIn.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
        if (te instanceof ColorableMachineTile) {
            return ((ColorableMachineTile) te).getMachineColor();
        }
        return Config.machineColor;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int colorMultiplier(IBlockAccess world, int x, int y, int z) {
        if (MachineBlockRenderType.isOverlayPass()) {
            return 0xFFFFFF;
        }
        return getColorMultiplier(IBlockState.fromWorld(world, x, y, z), world, new BlockPos(x, y, z), 0);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderColor(int meta) {
        return getColorMultiplier(IBlockState.of(this, meta), null, null, 0);
    }

    // ---------------- 其他行为 ----------------

    @Override
    public boolean onBlockEventReceived(World worldIn, int x, int y, int z, int id, int param) {
        worldIn.markBlockRangeForRenderUpdate(x, y, z, x, y, z);
        return true;
    }

    /** 1.12.2 中由 Block#addInformation 提供；1.7.10 由对应的 ItemBlock 调用。 */
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable EntityPlayer player, List<String> tooltip, boolean advanced) {
    }

    /** 破坏方块时把库存物品掉落到世界中。 */
    protected void dropInventory(World world, int x, int y, int z, IItemHandlerModifiable inv) {
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!ItemStacks.isEmpty(stack)) {
                dropBlockAsItem(world, x, y, z, stack.copy());
                inv.setStackInSlot(i, ItemStacks.EMPTY);
            }
        }
    }

    protected static void openGui(EntityPlayer player, int guiId, World world, int x, int y, int z) {
        player.openGui(ModularMachinery.instance, guiId, world, x, y, z);
    }

    // ---------------- 贴图 ----------------

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        baseIcon = register.registerIcon(BASE_TEXTURE);
        transparentIcon = register.registerIcon(ModularMachinery.MODID + ":overlay_transparent");
        registerOverlayIcons(register);
    }

    /**
     * 世界渲染：ISBRH 先以普通状态画一遍（着色的底层），再在 {@link MachineBlockRenderType#isOverlayPass()} 期间画一遍覆盖层。
     */
    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
        int meta = world.getBlockMetadata(x, y, z);
        if (MachineBlockRenderType.isOverlayPass()) {
            IIcon overlay = getOverlayIcon(world, x, y, z, side, meta);
            return overlay != null ? overlay : transparentIcon;
        }
        return getBaseIcon(side, meta);
    }

    /** 覆盖层渲染时跳过没有覆盖层的面。传入的坐标是相邻方块的坐标。 */
    @Override
    @SideOnly(Side.CLIENT)
    public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
        if (MachineBlockRenderType.isOverlayPass()) {
            ForgeDirection dir = ForgeDirection.getOrientation(side);
            int ox = x - dir.offsetX;
            int oy = y - dir.offsetY;
            int oz = z - dir.offsetZ;
            if (getOverlayIcon(world, ox, oy, oz, side, world.getBlockMetadata(ox, oy, oz)) == null) {
                return false;
            }
        }
        return super.shouldSideBeRendered(world, x, y, z, side);
    }

    /** 子类注册覆盖层贴图。 */
    @SideOnly(Side.CLIENT)
    protected void registerOverlayIcons(IIconRegister register) {
    }

    /** 着色的底层贴图。 */
    @SideOnly(Side.CLIENT)
    public IIcon getBaseIcon(int side, int meta) {
        return baseIcon;
    }

    /** 不着色的覆盖层贴图；null 表示无覆盖层。 */
    @SideOnly(Side.CLIENT)
    @Nullable
    public IIcon getOverlayIcon(IBlockAccess world, int x, int y, int z, int side, int meta) {
        return getOverlayIcon(side, meta);
    }

    @SideOnly(Side.CLIENT)
    @Nullable
    public IIcon getOverlayIcon(int side, int meta) {
        return null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return getBaseIcon(side, meta);
    }

    @Override
    public int getRenderType() {
        return MachineBlockRenderType.renderId;
    }

    /** 判断方块是否为本模组的机器组件（用于 ISBRH 判断）。 */
    public static boolean isMachineComponent(Block block) {
        return block instanceof BlockMachineComponent;
    }
}
