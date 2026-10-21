/*
 * 移植自 gugu-utils（https://github.com/ParaParty/gugu-utils，MIT License，Copyright (c) 2020 派瑞派对）。
 * 用法同原模组：创造模式且有命令权限（单人开启作弊或服务器 OP）时，右键方块依次选择起点与终点，把长方体内所有非空气方块
 * （包括流体）放进 MMCE 建造选择工具的选区；之后用 MMCE 的建造选择工具微调，最后右键控制器导出机器结构 JSON。
 * 1.7.10 移植：
 * - 不依赖植物魔法（mcmod 记载原版“未安装植物魔法时无法使用”，原模组代码中这个物品与植物魔法无关；本移植在未装植物魔法的环境中实测可用）。
 *   原模组只在创造模式且有命令权限时生效、否则什么也不做，这里在不满足条件时给出聊天提示。
 * - 起点 / 选区完成时给出聊天提示；起点 y = 0 也可用（原模组把 y &gt; 0 当作“已选起点”）。
 * - 选区上限 {@link #MAX_SELECTION} 个方块：MMCE 的选区同步包每个方块 12 字节，1.7.10 单个数据包上限 2 MB；超出时不改变选区并提示。
 */
package com.warmthdawn.mod.gugu_utils.modularmachenary.tools;

import com.warmthdawn.mod.gugu_utils.GuGuUtils;
import com.warmthdawn.mod.gugu_utils.common.Constants;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.kasuminova.mmce.compat.BlockPos;
import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.selection.PlayerStructureSelectionHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class ItemRangedConstructTool extends Item {

    private static final String TAG_X = "startX";
    private static final String TAG_Y = "startY";
    private static final String TAG_Z = "startZ";

    public static final int MAX_SELECTION = 100_000;

    public ItemRangedConstructTool() {
        setUnlocalizedName(GuGuUtils.MODID + "." + Constants.NAME_RANGED_CONSTRUCTION_TOOL);
        setTextureName(GuGuUtils.MODID + ":rangedwrench");
        setCreativeTab(CommonProxy.creativeTabModularMachinery);
        setFull3D();
    }

    /** 已记录的起点；未选起点时 y 为 -1（同原模组的 NBT：startX / startY / startZ）。 */
    public static BlockPos getNbtPos(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey(TAG_Y)) {
            return new BlockPos(0, -1, 0);
        }
        return new BlockPos(tag.getInteger(TAG_X), tag.getInteger(TAG_Y), tag.getInteger(TAG_Z));
    }

    public static void setNbtPos(ItemStack stack, BlockPos pos) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setInteger(TAG_X, pos.getX());
        tag.setInteger(TAG_Y, pos.getY());
        tag.setInteger(TAG_Z, pos.getZ());
    }

    /** 与 MMCE 的建造选择工具相同的权限：单人游戏（开启作弊）或服务器 OP。 */
    private static boolean canSendCommands(EntityPlayer player) {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) {
            return false;
        }
        return server.isSinglePlayer() && server.getServerOwner() != null && server.getServerOwner().equals(player.getCommandSenderName())
            || server.getConfigurationManager().func_152596_g(player.getGameProfile());
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        tooltip.add(StatCollector.translateToLocal("message.gugu-utils.rangedconstructtool.creative"));
        tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("message.gugu-utils.rangedconstructtool.usage"));
        BlockPos first = getNbtPos(stack);
        if (first.getY() >= 0) {
            tooltip.add(EnumChatFormatting.AQUA + StatCollector.translateToLocalFormatted("message.gugu-utils.rangedconstructtool.start",
                first.getX(), first.getY(), first.getZ()));
        }
    }

    @Override
    public boolean onItemUse(ItemStack held, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return true;
        }
        if (!player.capabilities.isCreativeMode || !canSendCommands(player)) {
            player.addChatMessage(new ChatComponentTranslation("message.gugu-utils.rangedconstructtool.denied"));
            return true;
        }
        BlockPos pos = new BlockPos(x, y, z);
        BlockPos first = getNbtPos(held);
        if (first.getY() < 0) {
            setNbtPos(held, pos);
            player.addChatMessage(new ChatComponentTranslation("message.gugu-utils.rangedconstructtool.start", x, y, z));
            return true;
        }
        setNbtPos(held, new BlockPos(0, -1, 0));
        List<BlockPos> selected = new ArrayList<>();
        int minX = Math.min(first.getX(), x), maxX = Math.max(first.getX(), x);
        int minY = Math.max(0, Math.min(first.getY(), y)), maxY = Math.min(world.getHeight() - 1, Math.max(first.getY(), y));
        int minZ = Math.min(first.getZ(), z), maxZ = Math.max(first.getZ(), z);
        for (int bx = minX; bx <= maxX; bx++) {
            for (int by = minY; by <= maxY; by++) {
                for (int bz = minZ; bz <= maxZ; bz++) {
                    // 空气忽略；流体（包括倒出来的流动流体）与其他方块都选入
                    if (!world.isAirBlock(bx, by, bz)) {
                        if (selected.size() >= MAX_SELECTION) {
                            player.addChatMessage(new ChatComponentTranslation("message.gugu-utils.rangedconstructtool.toolarge", MAX_SELECTION));
                            return true;
                        }
                        selected.add(new BlockPos(bx, by, bz));
                    }
                }
            }
        }
        PlayerStructureSelectionHelper.purgeSelection(player);
        for (BlockPos p : selected) {
            PlayerStructureSelectionHelper.toggleInSelection(player, p);
        }
        PlayerStructureSelectionHelper.sendSelection(player);
        player.addChatMessage(new ChatComponentTranslation("message.gugu-utils.rangedconstructtool.selected", selected.size(),
            minX, minY, minZ, maxX, maxY, maxZ));
        return true;
    }
}
