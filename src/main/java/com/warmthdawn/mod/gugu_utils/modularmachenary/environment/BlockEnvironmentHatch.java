/*
 * 移植自 gugu-utils（https://github.com/ParaParty/gugu-utils，MIT License，Copyright (c) 2020 派瑞派对）。
 * 1.7.10 移植：原模组用方块模型（机器外壳底 + 覆盖层，块色由 IBlockColor 取方块实体颜色）；这里继承 MMCE 的 BlockMachineComponent，
 * 与其他机器组件一样由 ISBRH 绘制着色底层 + 覆盖层（覆盖层贴图同原模组），机器颜色取自方块实体。
 */
package com.warmthdawn.mod.gugu_utils.modularmachenary.environment;

import com.warmthdawn.mod.gugu_utils.GuGuUtils;
import com.warmthdawn.mod.gugu_utils.common.Constants;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.kasuminova.mmce.compat.IBlockState;
import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.block.BlockMachineComponent;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class BlockEnvironmentHatch extends BlockMachineComponent {

    @SideOnly(Side.CLIENT)
    private IIcon overlay;

    public BlockEnvironmentHatch() {
        super(Material.rock);
        setHardness(2.0F);
        setResistance(10.0F);
        setStepSound(soundTypeStone);
        setHarvestLevel("pickaxe", 0);
        setBlockName(GuGuUtils.MODID + "." + Constants.NAME_ENVIRONMENTHATCH);
        setCreativeTab(CommonProxy.creativeTabModularMachinery);
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEnvironmentHatch();
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected void registerOverlayIcons(IIconRegister register) {
        overlay = register.registerIcon(GuGuUtils.MODID + ":overlay_" + Constants.NAME_ENVIRONMENTHATCH);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getOverlayIcon(int side, int meta) {
        return overlay;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable EntityPlayer player, List<String> tooltip, boolean advanced) {
        tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("tooltips.gugu-utils.environmenthatch.desc"));
    }
}
