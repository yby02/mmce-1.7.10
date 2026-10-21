package kport.modularmagic.common.block;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.kasuminova.mmce.compat.IBlockState;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.block.BlockMachineComponent;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Modular Magic 仓方块的公共实现：与其他机器组件一样用着色底层 + 覆盖层渲染（ISBRH）。
 * 1.7.10 移植：上游的源质仓 / 生命精华仓是自定义模型（罐子、宝珠底座），1.7.10 改为完整方块 + 覆盖层；覆盖层贴图由上游魔力仓的覆盖层改色得到。
 */
public abstract class BlockMagicProvider extends BlockMachineComponent {

    private final String                overlayName;
    private final Supplier<TileEntity>  tileFactory;

    @SideOnly(Side.CLIENT)
    private IIcon overlay;

    protected BlockMagicProvider(String overlayName, Supplier<TileEntity> tileFactory) {
        super(Material.iron);
        this.overlayName = overlayName;
        this.tileFactory = tileFactory;
        setHardness(2F);
        setResistance(10F);
        setStepSound(soundTypeMetal);
        setHarvestLevel("pickaxe", 1);
        setCreativeTab(CommonProxy.creativeTabModularMachinery);
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return tileFactory.get();
    }

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

    /** 右键时在聊天栏显示仓中的内容（子类实现）；返回 false 表示交给子类的其他处理。 */
    protected abstract void describe(TileEntity te, EntityPlayer player);

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        if (!world.isRemote && player.getHeldItem() == null && !player.isSneaking()) {
            TileEntity te = world.getTileEntity(x, y, z);
            if (te != null) {
                describe(te, player);
            }
        }
        return player.getHeldItem() == null && !player.isSneaking();
    }
}
