package github.kasuminova.mmce.client.gui.ae2;

import github.kasuminova.mmce.common.container.ContainerMEBase;
import github.kasuminova.mmce.common.network.PktMEGuiAction;
import github.kasuminova.mmce.compat.ItemStacks;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.client.gui.GuiContainerBase;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraftforge.fluids.FluidStack;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.util.ArrayList;
import java.util.List;

/**
 * ME 部件界面的公共绘制：数量不受 64 限制的物品格与流体槽由界面直接绘制（见 {@link ContainerMEBase}），这里提供绘制、
 * 悬停判断、数量缩写与发送操作。
 */
public abstract class GuiMEBase<C extends ContainerMEBase<?>> extends GuiContainerBase<C> {

    protected GuiMEBase(C container) {
        super(container);
    }

    protected void sendAction(int action, int slot, int value) {
        ModularMachinery.NET_CHANNEL.sendToServer(new PktMEGuiAction(action, slot, value));
    }

    protected void sendAction(int action, int slot, int value, net.minecraft.nbt.NBTTagCompound payload) {
        ModularMachinery.NET_CHANNEL.sendToServer(new PktMEGuiAction(action, slot, value, payload));
    }

    /**
     * 从 NEI 拖入物品（对应上游设定格的 JEI 拖放，由 NEI 集成中的 MEGuiNeiDragHandler 调用）：鼠标处的格子接收时发送操作并返回 true。
     * fluid 为拖入物品对应的流体（流体容器或 NEI 的流体显示物品），没有时为 null。
     */
    public boolean acceptNeiDrop(int mouseX, int mouseY, ItemStack stack, FluidStack fluid) {
        return false;
    }

    /** 鼠标是否在界面内坐标 (x, y) 起的 w×h 区域中（mouseX / mouseY 为屏幕坐标）。 */
    protected boolean isHovering(int mouseX, int mouseY, int x, int y, int w, int h) {
        int rx = mouseX - guiLeft;
        int ry = mouseY - guiTop;
        return rx >= x && rx < x + w && ry >= y && ry < y + h;
    }

    /** 数量缩写：9999 以内原样，其后 K / M / G（两位整数以内保留一位小数）。 */
    public static String formatCount(long n) {
        if (n < 10_000) {
            return String.valueOf(n);
        }
        String[] units = {"K", "M", "G", "T"};
        double v = n;
        int unit = -1;
        while (v >= 1000 && unit < units.length - 1) {
            v /= 1000;
            unit++;
        }
        return (v < 100 ? String.format("%.1f", v) : String.valueOf((long) v)) + units[unit];
    }

    /** 在界面坐标 (x, y) 绘制物品，右下角写 text（null 时不写）。需在前景层（已平移到界面左上角）调用。 */
    protected void drawStack(ItemStack stack, int x, int y, String text) {
        if (ItemStacks.isEmpty(stack)) {
            return;
        }
        ItemStack display = ItemStacks.copyWithSize(stack, 1);
        GL11.glPushMatrix();
        GL11.glColor4f(1F, 1F, 1F, 1F);
        RenderHelper.enableGUIStandardItemLighting();
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        itemRender.zLevel = 100F;
        itemRender.renderItemAndEffectIntoGUI(fontRendererObj, mc.getTextureManager(), display, x, y);
        itemRender.renderItemOverlayIntoGUI(fontRendererObj, mc.getTextureManager(), display, x, y, null);
        itemRender.zLevel = 0F;
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glPopMatrix();
        if (text != null) {
            drawAmount(text, x, y);
        }
    }

    /** 格右下角的数量，半尺寸字体（同 AE2 的 StackSizeRenderer），数量较大时不会挤出格子。 */
    protected void drawAmount(String text, int x, int y) {
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glPushMatrix();
        GL11.glTranslatef(0F, 0F, 200F);
        GL11.glScalef(0.5F, 0.5F, 0.5F);
        int width = fontRendererObj.getStringWidth(text);
        fontRendererObj.drawStringWithShadow(text, (x + 16) * 2 - width, (y + 16) * 2 - 7, 0xFFFFFF);
        GL11.glPopMatrix();
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    /** 半透明白色高亮（同原版鼠标悬停的格）。 */
    protected void drawHighlight(int x, int y, int w, int h) {
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        drawGradientRect(x, y, x + w, y + h, 0x80FFFFFF, 0x80FFFFFF);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    /** 在界面坐标绘制流体：从底部向上按 filled（0–1）比例平铺流体贴图。 */
    protected void drawFluid(FluidStack fluid, int x, int y, int w, int h, float filled) {
        if (fluid == null || fluid.getFluid() == null || filled <= 0) {
            return;
        }
        IIcon icon = fluid.getFluid().getIcon(fluid);
        if (icon == null) {
            icon = ((TextureMap) mc.getTextureManager().getTexture(TextureMap.locationBlocksTexture)).getAtlasSprite("missingno");
        }
        int color = fluid.getFluid().getColor(fluid);
        GL11.glColor4f((color >> 16 & 0xFF) / 255F, (color >> 8 & 0xFF) / 255F, (color & 0xFF) / 255F, 1F);
        GL11.glDisable(GL11.GL_LIGHTING);
        mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
        int px = Math.max(1, Math.min(h, (int) Math.ceil(h * Math.min(filled, 1F))));
        int drawn = 0;
        while (drawn < px) {
            int part = Math.min(16, px - drawn);
            for (int dx = 0; dx < w; dx += 16) {
                int pw = Math.min(16, w - dx);
                drawIconPart(icon, x + dx, y + h - drawn - part, pw, part);
            }
            drawn += part;
        }
        GL11.glColor4f(1F, 1F, 1F, 1F);
    }

    private void drawIconPart(IIcon icon, int x, int y, int w, int h) {
        double u0 = icon.getMinU();
        double v0 = icon.getMinV();
        double u1 = icon.getInterpolatedU(w);
        double v1 = icon.getInterpolatedV(h);
        net.minecraft.client.renderer.Tessellator t = net.minecraft.client.renderer.Tessellator.instance;
        t.startDrawingQuads();
        t.addVertexWithUV(x, y + h, zLevel, u0, v1);
        t.addVertexWithUV(x + w, y + h, zLevel, u1, v1);
        t.addVertexWithUV(x + w, y, zLevel, u1, v0);
        t.addVertexWithUV(x, y, zLevel, u0, v0);
        t.draw();
    }

    /** 物品的悬停提示（物品自身的提示 + 额外几行）。 */
    @SuppressWarnings("unchecked")
    protected void drawStackTooltip(ItemStack stack, int mouseX, int mouseY, List<String> extra) {
        List<String> lines = new ArrayList<>();
        List<String> itemLines = stack.getTooltip(mc.thePlayer, mc.gameSettings.advancedItemTooltips);
        for (int i = 0; i < itemLines.size(); i++) {
            lines.add(i == 0 ? stack.getRarity().rarityColor + itemLines.get(i) : EnumChatFormatting.GRAY + itemLines.get(i));
        }
        lines.addAll(extra);
        drawHoveringText(lines, mouseX, mouseY, fontRendererObj);
    }

    protected void drawTooltip(List<String> lines, int mouseX, int mouseY) {
        drawHoveringText(lines, mouseX, mouseY, fontRendererObj);
    }

    @Override
    protected void setWidthHeight() {
    }
}
