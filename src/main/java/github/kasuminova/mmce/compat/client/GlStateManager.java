package github.kasuminova.mmce.compat.client;

import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.GL11;

/**
 * 1.7.10 移植适配：1.12.2 的 GlStateManager 在 1.7.10 不存在，这里直接转发到 GL11（不做状态缓存）。
 * 仅客户端使用。
 */
public final class GlStateManager {

    private GlStateManager() {
    }

    public static void color(float red, float green, float blue, float alpha) {
        GL11.glColor4f(red, green, blue, alpha);
    }

    public static void color(float red, float green, float blue) {
        GL11.glColor4f(red, green, blue, 1.0F);
    }

    public static void resetColor() {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void pushMatrix() {
        GL11.glPushMatrix();
    }

    public static void popMatrix() {
        GL11.glPopMatrix();
    }

    public static void translate(float x, float y, float z) {
        GL11.glTranslatef(x, y, z);
    }

    public static void translate(double x, double y, double z) {
        GL11.glTranslated(x, y, z);
    }

    public static void scale(float x, float y, float z) {
        GL11.glScalef(x, y, z);
    }

    public static void scale(double x, double y, double z) {
        GL11.glScaled(x, y, z);
    }

    public static void rotate(float angle, float x, float y, float z) {
        GL11.glRotatef(angle, x, y, z);
    }

    public static void enableBlend() {
        GL11.glEnable(GL11.GL_BLEND);
    }

    public static void disableBlend() {
        GL11.glDisable(GL11.GL_BLEND);
    }

    public static void blendFunc(SourceFactor src, DestFactor dst) {
        GL11.glBlendFunc(src.factor, dst.factor);
    }

    public static void blendFunc(int src, int dst) {
        GL11.glBlendFunc(src, dst);
    }

    public static void tryBlendFuncSeparate(SourceFactor srcColor, DestFactor dstColor, SourceFactor srcAlpha, DestFactor dstAlpha) {
        OpenGlHelper.glBlendFunc(srcColor.factor, dstColor.factor, srcAlpha.factor, dstAlpha.factor);
    }

    public static void tryBlendFuncSeparate(int srcColor, int dstColor, int srcAlpha, int dstAlpha) {
        OpenGlHelper.glBlendFunc(srcColor, dstColor, srcAlpha, dstAlpha);
    }

    public static void shadeModel(int mode) {
        GL11.glShadeModel(mode);
    }

    public static void enableTexture2D() {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    public static void disableTexture2D() {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

    public static void enableTexture() {
        enableTexture2D();
    }

    public static void disableTexture() {
        disableTexture2D();
    }

    public static void enableDepth() {
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    public static void disableDepth() {
        GL11.glDisable(GL11.GL_DEPTH_TEST);
    }

    public static void depthMask(boolean flag) {
        GL11.glDepthMask(flag);
    }

    public static void enableAlpha() {
        GL11.glEnable(GL11.GL_ALPHA_TEST);
    }

    public static void disableAlpha() {
        GL11.glDisable(GL11.GL_ALPHA_TEST);
    }

    public static void alphaFunc(int func, float ref) {
        GL11.glAlphaFunc(func, ref);
    }

    public static void enableCull() {
        GL11.glEnable(GL11.GL_CULL_FACE);
    }

    public static void disableCull() {
        GL11.glDisable(GL11.GL_CULL_FACE);
    }

    public static void enableLighting() {
        GL11.glEnable(GL11.GL_LIGHTING);
    }

    public static void disableLighting() {
        GL11.glDisable(GL11.GL_LIGHTING);
    }

    public static void enableColorMaterial() {
        GL11.glEnable(GL11.GL_COLOR_MATERIAL);
    }

    public static void disableColorMaterial() {
        GL11.glDisable(GL11.GL_COLOR_MATERIAL);
    }

    public static void enableRescaleNormal() {
        GL11.glEnable(0x803A); // GL12.GL_RESCALE_NORMAL
    }

    public static void disableRescaleNormal() {
        GL11.glDisable(0x803A);
    }

    public enum SourceFactor {
        CONSTANT_ALPHA(32771),
        CONSTANT_COLOR(32769),
        DST_ALPHA(772),
        DST_COLOR(774),
        ONE(1),
        ONE_MINUS_CONSTANT_ALPHA(32772),
        ONE_MINUS_CONSTANT_COLOR(32770),
        ONE_MINUS_DST_ALPHA(773),
        ONE_MINUS_DST_COLOR(775),
        ONE_MINUS_SRC_ALPHA(771),
        ONE_MINUS_SRC_COLOR(769),
        SRC_ALPHA(770),
        SRC_ALPHA_SATURATE(776),
        SRC_COLOR(768),
        ZERO(0);

        public final int factor;

        SourceFactor(int factor) {
            this.factor = factor;
        }
    }

    public enum DestFactor {
        CONSTANT_ALPHA(32771),
        CONSTANT_COLOR(32769),
        DST_ALPHA(772),
        DST_COLOR(774),
        ONE(1),
        ONE_MINUS_CONSTANT_ALPHA(32772),
        ONE_MINUS_CONSTANT_COLOR(32770),
        ONE_MINUS_DST_ALPHA(773),
        ONE_MINUS_DST_COLOR(775),
        ONE_MINUS_SRC_ALPHA(771),
        ONE_MINUS_SRC_COLOR(769),
        SRC_ALPHA(770),
        SRC_COLOR(768),
        ZERO(0);

        public final int factor;

        DestFactor(int factor) {
            this.factor = factor;
        }
    }
}
