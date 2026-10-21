package github.kasuminova.mmce.common.block.appeng;

import appeng.api.implementations.items.IMemoryCard;
import appeng.api.implementations.items.MemoryCardMessages;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.kasuminova.mmce.common.tile.SettingsTransfer;
import github.kasuminova.mmce.common.tile.base.MEMachineComponent;
import github.kasuminova.mmce.compat.IBlockState;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.block.BlockMachineComponent;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;

/**
 * ME 部件方块的公共基类：四面同一张覆盖层贴图（上游的 blockmodel_overlay_all），放置者记为 AE2 网络节点的所有者（安全终端），
 * AE2 内存卡复制 / 粘贴设置。
 * <p>
 * 1.7.10 移植：上游的方块模型（MODEL + CUTOUT）改为本模组的机器方块 ISBRH（底层着色 + 覆盖层），与其他机器组件外观一致；
 * 带内容破坏时保留到物品 NBT 的方块，掉落由 {@link #breakBlock} 负责，{@link #getDrops} 返回空列表。
 */
public abstract class BlockMEMachineComponent extends BlockMachineComponent {

    private final String overlayName;

    @SideOnly(Side.CLIENT)
    private IIcon overlay;

    protected BlockMEMachineComponent(String overlayName) {
        super(Material.iron);
        this.overlayName = overlayName;
        setHardness(2F);
        setResistance(10F);
        setStepSound(soundTypeMetal);
        setHarvestLevel("pickaxe", 1);
        setCreativeTab(CommonProxy.creativeTabModularMachinery);
    }

    @Nullable
    @Override
    public abstract TileEntity createTileEntity(World world, IBlockState state);

    @Override
    @SideOnly(Side.CLIENT)
    protected void registerOverlayIcons(IIconRegister register) {
        overlay = register.registerIcon(ModularMachinery.MODID + ":overlay_" + overlayName);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getOverlayIcon(int side, int meta) {
        return overlay;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, x, y, z, placer, stack);
        TileEntity te = world.getTileEntity(x, y, z);
        if (!world.isRemote && te instanceof MEMachineComponent component && placer instanceof EntityPlayer player) {
            component.setOwner(player);
        }
    }

    /** 方块破坏时自行掉落（见 {@link #breakBlock}）的方块覆盖为 true。 */
    protected boolean dropsHandledInBreakBlock() {
        return false;
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        if (dropsHandledInBreakBlock()) {
            return new ArrayList<>();
        }
        return super.getDrops(world, x, y, z, metadata, fortune);
    }

    protected static void spawnAsEntity(World world, int x, int y, int z, ItemStack stack) {
        if (world.isRemote || stack == null || stack.stackSize <= 0) {
            return;
        }
        float f = 0.5F;
        double dx = world.rand.nextFloat() * f + (1.0F - f) * 0.5D;
        double dy = world.rand.nextFloat() * f + (1.0F - f) * 0.5D;
        double dz = world.rand.nextFloat() * f + (1.0F - f) * 0.5D;
        EntityItem entity = new EntityItem(world, x + dx, y + dy, z + dz, stack);
        entity.delayBeforeCanPickup = 10;
        world.spawnEntityInWorld(entity);
    }

    protected boolean handleSettingsTransfer(SettingsTransfer settingsProvider, IMemoryCard memoryCard, EntityPlayer player, ItemStack heldItem) {
        if (player.isSneaking()) {
            NBTTagCompound tag = settingsProvider.downloadSettings();
            if (tag != null) {
                memoryCard.setMemoryCardContents(heldItem, getUnlocalizedName(), tag);
                memoryCard.notifyUser(player, MemoryCardMessages.SETTINGS_SAVED);
                return true;
            }
        } else {
            String savedName = memoryCard.getSettingsName(heldItem);
            NBTTagCompound tag = memoryCard.getData(heldItem);
            if (getUnlocalizedName().equals(savedName)) {
                settingsProvider.uploadSettings(tag);
                memoryCard.notifyUser(player, MemoryCardMessages.SETTINGS_LOADED);
                return true;
            } else {
                memoryCard.notifyUser(player, MemoryCardMessages.INVALID_MACHINE);
            }
        }
        return false;
    }
}
