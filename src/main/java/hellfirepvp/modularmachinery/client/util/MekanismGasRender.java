package hellfirepvp.modularmachinery.client.util;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.kasuminova.mmce.compat.MathHelper;
import github.kasuminova.mmce.compat.client.GlStateManager;
import hellfirepvp.modularmachinery.common.integration.mekanism.MekanismGasHelper;
import hellfirepvp.modularmachinery.common.util.HybridTank;
import mekanism.api.gas.GasStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.IIcon;
import org.lwjgl.opengl.GL11;

import java.util.List;

/**
 * 客户端绘制 Mekanism 气体（流体仓 GUI、NEI 配方页）。调用方必须先检查 {@code Mods.MEKANISM.isPresent()}。
 * <p>
 * 1.7.10 移植：1.12.2 的气体有 Sprite 与 Tint；1.7.10 的气体只有方块图集中的 IIcon（由 Mekanism 在贴图拼接时设置），不再着色。
 */
@SideOnly(Side.CLIENT)
public final class MekanismGasRender {

    private MekanismGasRender() {
    }

    private static IIcon iconOf(GasStack gas) {
        IIcon icon = gas.getGas().getIcon();
        if (icon == null) {
            icon = ((TextureMap) Minecraft.getMinecraft().getTextureManager().getTexture(TextureMap.locationBlocksTexture)).getAtlasSprite("missingno");
        }
        return icon;
    }

    /** 在储罐区域内按装填比例自下而上绘制气体；储罐没有气体时不绘制并返回 false。 */
    public static boolean drawTankGas(Gui gui, HybridTank tank, int x, int y, int width, int height) {
        GasStack gas = MekanismGasHelper.gasOf(tank);
        if (gas == null) {
            return false;
        }
        float filled = MathHelper.clamp(((float) gas.amount) / ((float) tank.getCapacity()), 0F, 1F);
        int px = MathHelper.ceil(filled * height);
        GlStateManager.color(1F, 1F, 1F, 1F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
        drawBlended(gui, iconOf(gas), x, y + height - px, width, px);
        return true;
    }

    /** 气体贴图带透明度（例如氢气是半透明白色），开启 alpha 混合绘制，与 Mekanism 自己的 GUI 一致。 */
    private static void drawBlended(Gui gui, IIcon icon, int x, int y, int width, int height) {
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        gui.drawTexturedModelRectFromIcon(x, y, icon, width, height);
        if (!blend) {
            GL11.glDisable(GL11.GL_BLEND);
        }
    }

    /** 储罐中有气体时追加 [气体]、气体名、数量 / 容量，并返回 true。 */
    public static boolean addTankTooltip(HybridTank tank, List<String> text) {
        GasStack gas = MekanismGasHelper.gasOf(tank);
        if (gas == null) {
            return false;
        }
        text.add(I18n.format("tooltip.fluidhatch.gas"));
        text.add(gas.getGas().getLocalizedName());
        text.add(I18n.format("tooltip.fluidhatch.tank.gas", String.valueOf(gas.amount), String.valueOf(tank.getCapacity())));
        return true;
    }

    /** 绘制 16×16 的气体图标（NEI 配方页的气体格），gasStack 必须是 {@link GasStack}。 */
    public static void drawGasIcon(Gui gui, Object gasStack, int x, int y) {
        GlStateManager.color(1F, 1F, 1F, 1F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
        drawBlended(gui, iconOf((GasStack) gasStack), x, y, 16, 16);
    }
}
