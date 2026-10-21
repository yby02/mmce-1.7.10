/*
 * 移植自 gugu-utils 的 ClientEventHandler 与 RenderTools（https://github.com/ParaParty/gugu-utils，MIT License，Copyright (c) 2020 派瑞派对）。
 * 手持范围建造选择工具时：已选起点则描出起点方块的边框，否则描出当前选区（距玩家 32 格内的方块）的外包盒；
 * 边框颜色随时间循环变化，潜行时另画一层细线穿透方块显示。
 * 1.7.10 移植：GlStateManager / BufferBuilder 改为 GL11 状态设置 + Tessellator 线段。
 */
package com.warmthdawn.mod.gugu_utils.handler;

import com.warmthdawn.mod.gugu_utils.modularmachenary.tools.ItemRangedConstructTool;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.kasuminova.mmce.compat.BlockPos;
import hellfirepvp.modularmachinery.common.selection.PlayerStructureSelectionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
public class ClientEventHandler {

    public static int elapsedTicks = 0;

    /** 最近一帧描出的外包盒（minX, minY, minZ, maxX, maxY, maxZ，max 不含），没有时为 null；供开发期自动化核对。 */
    public static volatile int[] lastOutline;

    /** FML 总线：客户端 tick 计数（边框变色用）。 */
    @SubscribeEvent
    public void tickEnd(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            elapsedTicks++;
        }
    }

    /** Forge 总线：世界渲染结束后描边。 */
    @SubscribeEvent
    public void onRenderLast(RenderWorldLastEvent event) {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player == null) {
            return;
        }
        ItemStack held = player.getCurrentEquippedItem();
        if (held == null || !(held.getItem() instanceof ItemRangedConstructTool)) {
            lastOutline = null;
            return;
        }
        BlockPos first = ItemRangedConstructTool.getNbtPos(held);
        if (first.getY() >= 0) {
            renderOutline(first.getX(), first.getY(), first.getZ(), first.getX() + 1, first.getY() + 1, first.getZ() + 1, event.partialTicks, false);
            return;
        }
        PlayerStructureSelectionHelper.StructureSelection sel = PlayerStructureSelectionHelper.clientSelection;
        if (sel == null) {
            lastOutline = null;
            return;
        }
        BlockPos playerPos = new BlockPos(player.posX, player.posY, player.posZ);
        List<BlockPos> toRender = new ArrayList<>();
        for (BlockPos pos : sel.getSelectedPositions()) {
            if (pos.distanceSq(playerPos) <= 1024) {
                toRender.add(pos);
            }
        }
        if (toRender.isEmpty()) {
            lastOutline = null;
            return;
        }
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : toRender) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        renderOutline(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1, event.partialTicks, player.isSneaking());
    }

    private static double timeGradient(double max, double period, double partialTicks) {
        return (elapsedTicks + partialTicks) % period / period * max;
    }

    private static void renderOutline(double x1, double y1, double z1, double x2, double y2, double z2, float partialTicks, boolean renderMasked) {
        lastOutline = new int[]{(int) x1, (int) y1, (int) z1, (int) x2, (int) y2, (int) z2};
        Entity view = Minecraft.getMinecraft().renderViewEntity;
        if (view == null) {
            view = Minecraft.getMinecraft().thePlayer;
        }
        double dx = view.lastTickPosX + (view.posX - view.lastTickPosX) * partialTicks;
        double dy = view.lastTickPosY + (view.posY - view.lastTickPosY) * partialTicks;
        double dz = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * partialTicks;

        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_LINE_BIT | GL11.GL_LIGHTING_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        int rgb = Color.HSBtoRGB((float) timeGradient(1, 200, partialTicks), 0.7F, 1.0F);
        GL11.glColor4f((rgb >> 16 & 0xFF) / 255F, (rgb >> 8 & 0xFF) / 255F, (rgb & 0xFF) / 255F, 1F);
        GL11.glTranslated(-dx, -dy, -dz);
        // 被挡住的部分另画一层细线（潜行时）
        if (renderMasked) {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glLineWidth(2F);
            drawBox(x1, y1, z1, x2, y2, z2);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        }
        GL11.glLineWidth(5F);
        drawBox(x1, y1, z1, x2, y2, z2);
        GL11.glColor4f(1F, 1F, 1F, 1F);
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    /** 长方体的 12 条棱（Tessellator 的 GL_LINES，Angelica 下同样可用）。 */
    private static void drawBox(double x1, double y1, double z1, double x2, double y2, double z2) {
        Tessellator tes = Tessellator.instance;
        tes.startDrawing(GL11.GL_LINES);
        double[][] corners = {{x1, y1, z1}, {x2, y1, z1}, {x2, y1, z2}, {x1, y1, z2}, {x1, y2, z1}, {x2, y2, z1}, {x2, y2, z2}, {x1, y2, z2}};
        int[][] edges = {{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
        for (int[] e : edges) {
            tes.addVertex(corners[e[0]][0], corners[e[0]][1], corners[e[0]][2]);
            tes.addVertex(corners[e[1]][0], corners[e[1]][1], corners[e[1]][2]);
        }
        tes.draw();
    }
}
