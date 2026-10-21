/*
 * 移植自 gugu-utils 的 RendererEnvironment（https://github.com/ParaParty/gugu-utils，MIT License，Copyright (c) 2020 派瑞派对）。
 * 1.7.10 移植：GlStateManager 改为 GL11，物品渲染改用 RenderItem，传送门动画改用 1.7.10 的 textures/blocks/portal.png（16×512，32 帧）。
 */
package com.warmthdawn.mod.gugu_utils.jei;

import com.warmthdawn.mod.gugu_utils.GuGuUtils;
import com.warmthdawn.mod.gugu_utils.modularmachenary.environment.envtypes.*;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/** 在 NEI 配方页的环境格中绘制 16×16 的图标（左上角 x, y）。 */
@SideOnly(Side.CLIENT)
public final class EnvironmentIconRenderer {

    public static final ResourceLocation RECIPES_UI = new ResourceLocation(GuGuUtils.MODID, "textures/gui/recipes_ui.png");
    private static final ResourceLocation PORTAL    = new ResourceLocation("minecraft", "textures/blocks/portal.png");
    private static final ItemStack        CLOCK     = new ItemStack(Items.clock);
    private static final RenderItem       ITEMS     = new RenderItem();

    private EnvironmentIconRenderer() {
    }

    /** 当前显示的图标在 recipes_ui.png 中的 (u, v)；不用图集（时间、维度）时返回 null。 */
    public static int[] atlasUV(EnvironmentType type, long worldTime) {
        if (type instanceof EnvMoonPhase moon) {
            int[] phases = moon.getPhases();
            int phase = phases.length == 0 ? 0 : phases[(int) (worldTime / 30 % phases.length)];
            return new int[]{106 + phase * 16, 0};
        }
        if (type instanceof EnvWeather weather) {
            return new int[]{106 + 16 * weather.getWeather().ordinal(), 16};
        }
        if (type instanceof EnvAltitude) {
            return new int[]{170, 16};
        }
        if (type instanceof EnvBiome) {
            return new int[]{186, 16};
        }
        return null;
    }

    public static void draw(EnvironmentType type, int x, int y) {
        Minecraft mc = Minecraft.getMinecraft();
        long worldTime = mc.theWorld == null ? 0 : mc.theWorld.getWorldTime();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_LIGHTING_BIT);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        int[] uv = atlasUV(type, worldTime);
        if (uv != null) {
            mc.getTextureManager().bindTexture(RECIPES_UI);
            GL11.glColor4f(1F, 1F, 1F, 0.8F);
            Gui.func_146110_a(x, y, uv[0], uv[1], 16, 16, 256, 257);
        } else if (type instanceof EnvDimension) {
            mc.getTextureManager().bindTexture(PORTAL);
            GL11.glColor4f(1F, 1F, 1F, 0.8F);
            int frame = (int) (worldTime % 32);
            Gui.func_146110_a(x, y, 0, frame * 16, 16, 16, 16, 512);
        } else if (type instanceof EnvTime) {
            GL11.glColor4f(1F, 1F, 1F, 1F);
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            RenderHelper.enableGUIStandardItemLighting();
            ITEMS.renderItemAndEffectIntoGUI(mc.fontRenderer, mc.getTextureManager(), CLOCK, x, y);
            RenderHelper.disableStandardItemLighting();
        }
        GL11.glColor4f(1F, 1F, 1F, 1F);
        GL11.glPopAttrib();
    }
}
