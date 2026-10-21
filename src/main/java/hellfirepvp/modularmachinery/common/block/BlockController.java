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
import github.kasuminova.mmce.compat.EnumFacing;
import github.kasuminova.mmce.compat.IBlockState;
import github.kasuminova.mmce.compat.ItemStacks;
import github.kasuminova.mmce.compat.Rotation;
import github.kasuminova.mmce.compat.registry.IRegistryNamed;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.data.Config;
import hellfirepvp.modularmachinery.common.item.ItemBlockController;
import hellfirepvp.modularmachinery.common.item.ItemDynamicColor;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.tiles.TileMachineController;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import github.kasuminova.mmce.compat.BlockPos;
import github.kasuminova.mmce.compat.BlockStateRotation;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.StatCollector;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: BlockController
 * Created by HellFirePvP
 * Date: 28.06.2017 / 20:48
 * <p>
 * 1.7.10 移植：
 * - 朝向（水平方向索引 0~3，与 1.12.2 的 getMetaFromState 相同）存于 meta；formed 状态由 TileEntity 提供给渲染器。
 * - 1.12.2 的 PropertyEnum/PropertyBool 改为 {@link #getFacing(IBlockState)} / {@link #withFacing(EnumFacing)}。
 * - 正面覆盖层为 overlay_controller，其余面无覆盖层（与 1.12.2 模型 blockmodel_overlay_orientable_all 一致）。
 */
public class BlockController extends BlockMachineComponent implements ItemDynamicColor {

    public static final Map<DynamicMachine, BlockController> MACHINE_CONTROLLERS     = new HashMap<>();
    public static final Map<DynamicMachine, BlockController> MOC_MACHINE_CONTROLLERS = new HashMap<>();

    private static final Random RANDOM = new Random();

    static {
        // 控制器朝向旋转规则：meta 为水平方向索引，顺时针 +1（S->W->N->E）
        BlockStateRotation.register(BlockController.class, meta -> (meta & ~3) | (EnumFacing.byHorizontalIndex(meta & 3).rotateY().getHorizontalIndex()));
    }

    protected DynamicMachine parentMachine = null;

    @SideOnly(Side.CLIENT)
    protected IIcon frontOverlay;

    public BlockController() {
        super(Material.iron);
        setHardness(5F);
        setResistance(10F);
        setStepSound(soundTypeMetal);
        setHarvestLevel("pickaxe", 1);
        setCreativeTab(CommonProxy.creativeTabModularMachinery);
    }

    public BlockController(DynamicMachine parentMachine) {
        this();
        this.parentMachine = parentMachine;
        String path = parentMachine.getRegistryName().getPath() + "_controller";
        IRegistryNamed.setName(this, ModularMachinery.MODID, path);
        setBlockName(ModularMachinery.MODID + '.' + path);
    }

    public BlockController(String namespace, DynamicMachine parentMachine) {
        this();
        this.parentMachine = parentMachine;
        // 1.7.10：方块注册名命名空间固定为本模组，这里用前缀区分 MOC 兼容控制器
        String path = namespace + "_" + parentMachine.getRegistryName().getPath() + "_controller";
        IRegistryNamed.setName(this, ModularMachinery.MODID, path);
        setBlockName(ModularMachinery.MODID + '.' + path);
        this.mocCompatible = true;
    }

    private boolean mocCompatible = false;

    public static BlockController getControllerWithMachine(DynamicMachine machine) {
        return MACHINE_CONTROLLERS.get(machine);
    }

    public static BlockController getMocControllerWithMachine(DynamicMachine machine) {
        return MOC_MACHINE_CONTROLLERS.get(machine);
    }

    /** 由方块状态取控制器朝向（1.12.2 的 state.getValue(FACING)）。 */
    public static EnumFacing getFacing(IBlockState state) {
        return EnumFacing.byHorizontalIndex(state.getMetadata() & 3);
    }

    public static EnumFacing getFacing(int meta) {
        return EnumFacing.byHorizontalIndex(meta & 3);
    }

    /** 对应 1.12.2 的 getDefaultState().withProperty(FACING, facing)。 */
    public IBlockState withFacing(EnumFacing facing) {
        return IBlockState.of(this, facing.getHorizontalIndex());
    }

    public DynamicMachine getParentMachine() {
        return parentMachine;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable EntityPlayer player, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        if (mocCompatible && !Config.disableMocDeprecatedTip) {
            tooltip.add(StatCollector.translateToLocal("tile.modularmachinery.machinecontroller.deprecated.tip.0"));
            tooltip.add(StatCollector.translateToLocal("tile.modularmachinery.machinecontroller.deprecated.tip.1"));
        }
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        ItemStack stack = getRestorableDropItem(world, x, y, z, metadata);
        if (!ItemStacks.isEmpty(stack)) {
            ArrayList<ItemStack> result = new ArrayList<>();
            result.add(stack);
            return result;
        }
        return super.getDrops(world, x, y, z, metadata, fortune);
    }

    @Override
    public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z, EntityPlayer player) {
        ItemStack stack = getRestorableDropItem(world, x, y, z, world.getBlockMetadata(x, y, z));
        if (!ItemStacks.isEmpty(stack)) {
            return stack;
        }
        return super.getPickBlock(target, world, x, y, z, player);
    }

    private ItemStack getRestorableDropItem(IBlockAccess world, int x, int y, int z, int meta) {
        Random rand = world instanceof World ? ((World) world).rand : RANDOM;
        TileEntity tileEntity = world.getTileEntity(x, y, z);
        if (tileEntity instanceof TileMultiblockMachineController ctrl && ctrl.getOwner() != null) {
            UUID ownerUUID = ctrl.getOwner();
            Item dropped = getItemDropped(meta, rand, 0);
            if (dropped instanceof ItemBlockController) {
                ItemStack stackCtrl = new ItemStack(dropped, 1, damageDropped(meta));
                NBTTagCompound tag = new NBTTagCompound();
                tag.setString("owner", ownerUUID.toString());
                stackCtrl.setTagCompound(tag);
                return stackCtrl;
            } else {
                ModularMachinery.log.warn("Cannot get controller drops at World: " + world + ", Pos: " + MiscUtils.posToString(new BlockPos(x, y, z)));
            }
        }
        return null;
    }

    @Override
    public int damageDropped(int meta) {
        // 朝向不影响掉落物
        return 0;
    }

    /** 延迟到 harvestBlock 后再移除方块，以便掉落物仍能读取 TileEntity 中的所有者信息（与 1.12.2 相同的技巧）。 */
    @Override
    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willHarvest) {
        return willHarvest || super.removedByPlayer(world, player, x, y, z, willHarvest);
    }

    @Override
    public void harvestBlock(World world, EntityPlayer player, int x, int y, int z, int meta) {
        super.harvestBlock(world, player, x, y, z, meta);
        world.setBlockToAir(x, y, z);
    }

    @Override
    public void breakBlock(World worldIn, int x, int y, int z, Block block, int meta) {
        TileEntity te = worldIn.getTileEntity(x, y, z);
        if (te instanceof TileMultiblockMachineController ctrl) {
            dropInventory(worldIn, x, y, z, ctrl.getInventory());
        }
        super.breakBlock(worldIn, x, y, z, block, meta);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (world.getTileEntity(x, y, z) instanceof TileMultiblockMachineController ctrl) {
            ctrl.onNeighborChange();
        }
    }

    @Override
    public boolean canConnectRedstone(IBlockAccess world, int x, int y, int z, int side) {
        return true;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return isFullBlock();
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        EnumFacing facing = EnumFacing.fromRotationYaw(placer.rotationYaw).getOpposite();
        world.setBlockMetadataWithNotify(x, y, z, facing.getHorizontalIndex(), 2);
    }

    @Override
    public boolean onBlockActivated(World worldIn, int x, int y, int z, EntityPlayer playerIn, int side, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            TileEntity te = worldIn.getTileEntity(x, y, z);
            if (te instanceof TileMachineController) {
                openGui(playerIn, CommonProxy.GuiType.CONTROLLER.ordinal(), worldIn, x, y, z);
            }
        }
        return true;
    }

    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(World worldIn, int x, int y, int z, int side) {
        TileEntity te = worldIn.getTileEntity(x, y, z);
        if (te instanceof final TileMultiblockMachineController ctrl) {
            return ctrl.isWorking() ? 15 : ctrl.getFoundMachine() != null ? 1 : 0;
        }
        return 0;
    }

    /** 1.12.2 的 withRotation。 */
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        return withFacing(rot.rotate(getFacing(state)));
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileMachineController(state);
    }

    @Override
    public String getLocalizedName() {
        if (parentMachine != null) {
            return StatCollector.translateToLocalFormatted("tile.modularmachinery.machinecontroller.name", parentMachine.getLocalizedName());
        }
        return StatCollector.translateToLocal("tile.modularmachinery.blockcontroller.name");
    }

    public boolean isFullBlock() {
        return parentMachine == null || isFullCube(parentMachine.getControllerBoundingBox());
    }

    private static boolean isFullCube(AxisAlignedBB box) {
        return box == null || (box.minX == 0 && box.minY == 0 && box.minZ == 0 && box.maxX == 1 && box.maxY == 1 && box.maxZ == 1);
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        applyControllerBounds();
    }

    @Override
    public void setBlockBoundsForItemRender() {
        applyControllerBounds();
    }

    private void applyControllerBounds() {
        AxisAlignedBB box = parentMachine != null ? parentMachine.getControllerBoundingBox() : null;
        if (box == null) {
            setBlockBounds(0F, 0F, 0F, 1F, 1F, 1F);
        } else {
            setBlockBounds((float) box.minX, (float) box.minY, (float) box.minZ, (float) box.maxX, (float) box.maxY, (float) box.maxZ);
        }
    }

    @Override
    public int getColorFromItemstack(ItemStack stack, int tintIndex) {
        if (parentMachine == null) {
            return Config.machineColor;
        }
        return parentMachine.getMachineColor();
    }

    @Override
    public int getColorMultiplier(IBlockState state, @Nullable IBlockAccess worldIn, @Nullable BlockPos pos, int tintIndex) {
        if (parentMachine == null) {
            return Config.machineColor;
        }
        return parentMachine.getMachineColor();
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected void registerOverlayIcons(IIconRegister register) {
        frontOverlay = register.registerIcon(ModularMachinery.MODID + ":" + getFrontOverlayTexture());
    }

    protected String getFrontOverlayTexture() {
        return "overlay_controller";
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getOverlayIcon(int side, int meta) {
        return side == getFacing(meta).getIndex() ? frontOverlay : null;
    }
}
