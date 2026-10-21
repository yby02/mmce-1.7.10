/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import github.kasuminova.mmce.compat.client.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import github.kasuminova.mmce.compat.ItemStacks;
import github.kasuminova.mmce.compat.Tuple;
import net.minecraft.util.AxisAlignedBB;
import github.kasuminova.mmce.compat.BlockPos;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.List;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: RenderingUtils
 * Created by HellFirePvP
 * Date: 18.08.2017 / 14:02
 */
public class RenderingUtils {

    static void drawWhiteOutlineCubes(List<BlockPos> positions, float partialTicks) {
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.color(1F, 1F, 1F, 0.4F);
        GlStateManager.disableTexture2D();
        GlStateManager.enableColorMaterial();
        GlStateManager.disableCull();

        Entity player = Minecraft.getMinecraft().renderViewEntity;
        if (player == null) {
            player = Minecraft.getMinecraft().thePlayer;
        }

        double dX = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double) partialTicks;
        double dY = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double) partialTicks;
        double dZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double) partialTicks;

        // 1.7.10 移植：Tessellator 直接提交顶点（1.12.2 的 BufferBuilder 与顶点排序不存在）
        Tessellator tes = Tessellator.instance;
        tes.startDrawingQuads();
        for (BlockPos pos : positions) {
            AxisAlignedBB box = AxisAlignedBB.getBoundingBox(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1)
                .expand(0.002, 0.002, 0.002).getOffsetBoundingBox(-dX, -dY, -dZ);

            tes.addVertex(box.minX, box.minY, box.minZ);
            tes.addVertex(box.maxX, box.minY, box.minZ);
            tes.addVertex(box.maxX, box.minY, box.maxZ);
            tes.addVertex(box.minX, box.minY, box.maxZ);

            tes.addVertex(box.minX, box.maxY, box.maxZ);
            tes.addVertex(box.maxX, box.maxY, box.maxZ);
            tes.addVertex(box.maxX, box.maxY, box.minZ);
            tes.addVertex(box.minX, box.maxY, box.minZ);

            tes.addVertex(box.maxX, box.minY, box.minZ);
            tes.addVertex(box.maxX, box.maxY, box.minZ);
            tes.addVertex(box.maxX, box.maxY, box.maxZ);
            tes.addVertex(box.maxX, box.minY, box.maxZ);

            tes.addVertex(box.minX, box.minY, box.maxZ);
            tes.addVertex(box.minX, box.maxY, box.maxZ);
            tes.addVertex(box.minX, box.maxY, box.minZ);
            tes.addVertex(box.minX, box.minY, box.minZ);

            tes.addVertex(box.minX, box.maxY, box.minZ);
            tes.addVertex(box.maxX, box.maxY, box.minZ);
            tes.addVertex(box.maxX, box.minY, box.minZ);
            tes.addVertex(box.minX, box.minY, box.minZ);

            tes.addVertex(box.minX, box.minY, box.maxZ);
            tes.addVertex(box.maxX, box.minY, box.maxZ);
            tes.addVertex(box.maxX, box.maxY, box.maxZ);
            tes.addVertex(box.minX, box.maxY, box.maxZ);
        }
        tes.draw();

        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.disableColorMaterial();
        GlStateManager.color(1F, 1F, 1F, 1F);
    }


    public static void renderBlueStackTooltip(int x, int y, List<Tuple<ItemStack, String>> tooltipData, FontRenderer fr, RenderItem ri) {
        renderStackTooltip(x, y, tooltipData, new Color(0x000037), new Color(0x000000), Color.WHITE, fr, ri);
    }

    public static void renderStackTooltip(int x, int y, List<Tuple<ItemStack, String>> tooltipData, Color color, Color colorFade, Color strColor, FontRenderer fr, RenderItem ri) {
        Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.locationBlocksTexture);
        if (!tooltipData.isEmpty()) {
            int esWidth = 0;
            for (Tuple<ItemStack, String> toolTip : tooltipData) {
                int width = fr.getStringWidth(toolTip.getSecond()) + 17;
                if (width > esWidth) {
                    esWidth = width;
                }
            }
            ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft(), Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight);
            if (x + 15 + esWidth > sr.getScaledWidth()) {
                x -= esWidth + 24;
            }
            int sumLineHeight = 8;
            int lastAdded = 0;
            if (tooltipData.size() > 1) {
                sumLineHeight += 2;
                for (Tuple<ItemStack, String> tooltipEntry : tooltipData) {
                    int height = ItemStacks.isEmpty(tooltipEntry.getFirst()) ? 10 : 17;
                    sumLineHeight += height;
                    lastAdded = height;
                }
                sumLineHeight -= lastAdded;
            }

            if (y + sumLineHeight > sr.getScaledHeight()) {
                y = (sr.getScaledHeight() - sumLineHeight);
                y = Math.max(25, y);
            }

            int pX = x + 12;
            int pY = y - 12;

            float z = 300F;

            GlStateManager.disableDepth();
            drawGradientRect(pX - 3, pY - 4, z, pX + esWidth + 3, pY - 3, color, colorFade);
            drawGradientRect(pX - 3, pY + sumLineHeight + 3, z, pX + esWidth + 3, pY + sumLineHeight + 4, color, colorFade);
            drawGradientRect(pX - 3, pY - 3, z, pX + esWidth + 3, pY + sumLineHeight + 3, color, colorFade);
            drawGradientRect(pX - 4, pY - 3, z, pX - 3, pY + sumLineHeight + 3, color, colorFade);
            drawGradientRect(pX + esWidth + 3, pY - 3, z, pX + esWidth + 4, pY + sumLineHeight + 3, color, colorFade);

            int rgb = color.getRGB();
            int col = (rgb & 0x00FFFFFF) | rgb & 0xFF000000;
            Color colOp = new Color(col);
            drawGradientRect(pX - 3, pY - 3 + 1, z, pX - 3 + 1, pY + sumLineHeight + 3 - 1, color, colOp);
            drawGradientRect(pX + esWidth + 2, pY - 3 + 1, z, pX + esWidth + 3, pY + sumLineHeight + 3 - 1, color, colOp);
            drawGradientRect(pX - 3, pY - 3, z, pX + esWidth + 3, pY - 3 + 1, colOp, colOp);
            drawGradientRect(pX - 3, pY + sumLineHeight + 2, z, pX + esWidth + 3, pY + sumLineHeight + 3, color, color);

            for (Tuple<ItemStack, String> stackDesc : tooltipData) {
                if (!ItemStacks.isEmpty(stackDesc.getFirst())) {
                    fr.drawStringWithShadow(stackDesc.getSecond(), pX + 17, pY, strColor.getRGB());
                    GlStateManager.color(1F, 1F, 1F, 1F);
                    GlStateManager.pushMatrix();
                    RenderHelper.enableGUIStandardItemLighting();
                    ri.renderItemAndEffectIntoGUI(fr, Minecraft.getMinecraft().getTextureManager(), stackDesc.getFirst(), pX - 1, pY - 5);
                    GlStateManager.popMatrix();
                    pY += 17;
                } else if (stackDesc.getSecond().isEmpty()) {
                    pY += 6;
                } else {
                    fr.drawStringWithShadow(stackDesc.getSecond(), pX, pY, strColor.getRGB());
                    GlStateManager.color(1F, 1F, 1F, 1F);
                    pY += 10;
                }
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            }
            GlStateManager.color(1F, 1F, 1F, 1F);
            GlStateManager.enableDepth();
        }

        GlStateManager.enableAlpha();
        GlStateManager.color(1F, 1F, 1F, 1F);
        Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.locationBlocksTexture);
    }

    public static void renderBlueTooltip(int x, int y, List<String> tooltipData, FontRenderer fontRendererObj) {
        renderTooltip(x, y, tooltipData, new Color(0x000037), new Color(0x000000), Color.WHITE, fontRendererObj);
    }

    public static void renderTooltip(int x, int y, List<String> tooltipData, Color color, Color colorFade, Color strColor, FontRenderer fontRendererObj) {
        Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.locationBlocksTexture);
        boolean lighting = GL11.glGetBoolean(GL11.GL_LIGHTING);
        if (lighting) {
            RenderHelper.disableStandardItemLighting();
        }

        if (!tooltipData.isEmpty()) {
            int esWidth = 0;
            for (String toolTip : tooltipData) {
                int width = fontRendererObj.getStringWidth(toolTip);
                if (width > esWidth) {
                    esWidth = width;
                }
            }
            ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft(), Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight);
            if (x + 15 + esWidth > sr.getScaledWidth()) {
                x -= esWidth + 24;
            }

            int sumLineHeight = 8;
            if (tooltipData.size() > 1) {
                sumLineHeight += 2 + (tooltipData.size() - 1) * 10;
            }


            if (y + sumLineHeight > sr.getScaledHeight()) {
                y = (sr.getScaledHeight() - sumLineHeight);
                y = Math.max(25, y);
            }

            int pX = x + 12;
            int pY = y - 12;

            float z = 300F;

            drawGradientRect(pX - 3, pY - 4, z, pX + esWidth + 3, pY - 3, color, colorFade);
            drawGradientRect(pX - 3, pY + sumLineHeight + 3, z, pX + esWidth + 3, pY + sumLineHeight + 4, color, colorFade);
            drawGradientRect(pX - 3, pY - 3, z, pX + esWidth + 3, pY + sumLineHeight + 3, color, colorFade);
            drawGradientRect(pX - 4, pY - 3, z, pX - 3, pY + sumLineHeight + 3, color, colorFade);
            drawGradientRect(pX + esWidth + 3, pY - 3, z, pX + esWidth + 4, pY + sumLineHeight + 3, color, colorFade);

            int rgb = color.getRGB();
            int col = (rgb & 0x00FFFFFF) | rgb & 0xFF000000;
            Color colOp = new Color(col);
            drawGradientRect(pX - 3, pY - 3 + 1, z, pX - 3 + 1, pY + sumLineHeight + 3 - 1, color, colOp);
            drawGradientRect(pX + esWidth + 2, pY - 3 + 1, z, pX + esWidth + 3, pY + sumLineHeight + 3 - 1, color, colOp);
            drawGradientRect(pX - 3, pY - 3, z, pX + esWidth + 3, pY - 3 + 1, colOp, colOp);
            drawGradientRect(pX - 3, pY + sumLineHeight + 2, z, pX + esWidth + 3, pY + sumLineHeight + 3, color, color);

            GlStateManager.disableDepth();
            for (int i = 0; i < tooltipData.size(); ++i) {
                String str = tooltipData.get(i);
                fontRendererObj.drawStringWithShadow(str, pX, pY, strColor.getRGB());
                if (i == 0) {
                    pY += 2;
                }
                pY += 10;
            }
            GlStateManager.color(1F, 1F, 1F, 1F);
            GlStateManager.enableDepth();
        }

        if (lighting) {
            RenderHelper.enableStandardItemLighting();
        }
        GlStateManager.color(1F, 1F, 1F, 1F);
        Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.locationBlocksTexture);
    }

    public static void drawGradientRect(int x, int y, float z, int toX, int toY, Color color, Color colorFade) {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        Tessellator tes = Tessellator.instance;
        tes.startDrawingQuads();
        tes.setColorRGBA(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        tes.addVertex(toX, y, z);
        tes.addVertex(x, y, z);
        tes.setColorRGBA(colorFade.getRed(), colorFade.getGreen(), colorFade.getBlue(), colorFade.getAlpha());
        tes.addVertex(x, toY, z);
        tes.addVertex(toX, toY, z);
        tes.draw();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

}
