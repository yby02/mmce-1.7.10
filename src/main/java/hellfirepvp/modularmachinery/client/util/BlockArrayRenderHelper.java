/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.client.util;

import github.kasuminova.mmce.compat.BlockPos;
import github.kasuminova.mmce.compat.IBlockState;
import hellfirepvp.modularmachinery.common.block.BlockMachineComponent;
import hellfirepvp.modularmachinery.common.tiles.base.ColorableMachineTile;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.common.util.ForgeDirection;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import javax.annotation.Nullable;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.LongSupplier;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: BlockArrayRenderHelper
 * Created by HellFirePvP
 * Date: 09.07.2017 / 20:16
 * <p>
 * 1.7.10 移植：
 * <ul>
 *   <li>BlockRendererDispatcher / BufferBuilder → RenderBlocks + Tessellator（两个渲染通道，与 NEI 结构页共用）；</li>
 *   <li>只读方块世界 {@link WorldBlockArrayRenderAccess}：替选方块按时间（或固定的 sampleSnap）轮换，光照恒为最亮；
 *       MMCE 机器组件的外壳颜色由只提供颜色的占位 TileEntity 给出，其他方块不提供 TileEntity（TESR 不渲染）；</li>
 *   <li>旋转 / 缩放 / 分层由调用方传入（蓝图预览与 NEI 结构页各自保存视角）。</li>
 * </ul>
 */
public class BlockArrayRenderHelper {

    public static final float DEFAULT_YAW   = 225F;
    public static final float DEFAULT_PITCH = 30F;

    private final BlockArray                  blocks;
    private final WorldBlockArrayRenderAccess renderAccess;
    long sampleSnap = -1;
    /** 最近一次带拾取参数的渲染所命中的方块。 */
    @Nullable
    private BlockPos pickedBlock;
    /** 只打印一次深度实测值，便于定位“深度不可读”。 */
    private static final java.util.concurrent.atomic.AtomicBoolean pickingDiagnosed =
        new java.util.concurrent.atomic.AtomicBoolean(false);
    private static final java.util.concurrent.atomic.AtomicBoolean depthDiagnosed =
        new java.util.concurrent.atomic.AtomicBoolean(false);

    BlockArrayRenderHelper(BlockArray blocks, int machineColor) {
        this.blocks = blocks;
        this.renderAccess = new WorldBlockArrayRenderAccess(blocks, () -> sampleSnap, machineColor);
    }

    public BlockArray getBlocks() {
        return blocks;
    }

    public WorldBlockArrayRenderAccess getRenderAccess() {
        return renderAccess;
    }

    /**
     * 在当前 GUI 坐标系中的矩形 (x, y, width, height) 内渲染整个结构（或只渲染 slice 层），超出部分被裁掉。
     * yaw / pitch 为角度；zoom 为相对“刚好放下整个结构”的缩放倍数。
     */
    public void render3DGUI(int x, int y, int width, int height, float yaw, float pitch, float zoom, @Nullable Integer slice) {
        render3DGUI(x, y, width, height, yaw, pitch, zoom, slice, -1, -1);
    }

    /**
     * 同 {@link #render3DGUI(int, int, int, int, float, float, float, Integer)}，并在画完后用
     * <b>深度缓冲 + 真实 GL 矩阵反投影</b> 拾取 (pickX, pickY) 处的方块（与上游 WorldSceneRenderer 的做法一致）。
     * 结果可通过 {@link #getPickedBlock()} 取得；pickX &lt; 0 表示不拾取。
     */
    public void render3DGUI(int x, int y, int width, int height, float yaw, float pitch, float zoom,
                            @Nullable Integer slice, int pickX, int pickY) {
        pickedBlock = null;
        Minecraft mc = Minecraft.getMinecraft();
        FloatBuffer matrix = BufferUtils.createFloatBuffer(16);
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, matrix);
        float originX = matrix.get(12);
        float originY = matrix.get(13);
        ScaledResolution res = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int scale = res.getScaleFactor();

        BlockPos min = blocks.getMin();
        BlockPos max = blocks.getMax();
        float sizeX = max.getX() - min.getX() + 1;
        float sizeY = max.getY() - min.getY() + 1;
        float sizeZ = max.getZ() - min.getZ() + 1;
        float maxDim = Math.max(sizeX, Math.max(sizeY, sizeZ));
        float blockScale = Math.min(width, height) / (maxDim * 1.75F) * zoom;
        float cx = min.getX() + sizeX / 2F;
        float cy = min.getY() + sizeY / 2F;
        float cz = min.getZ() + sizeZ / 2F;

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_SCISSOR_BIT);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int) ((originX + x) * scale), (int) (mc.displayHeight - (originY + y + height) * scale),
            width * scale, height * scale);
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glPushMatrix();
        GL11.glTranslatef(x + width / 2F, y + height / 2F, 400F);
        GL11.glScalef(blockScale, -blockScale, blockScale);
        GL11.glRotatef(pitch, 1F, 0F, 0F);
        GL11.glRotatef(yaw, 0F, 1F, 0F);
        GL11.glTranslatef(-cx, -cy, -cz);

        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
        GL11.glColor4f(1F, 1F, 1F, 1F);
        mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);

        renderAccess.slice = slice;
        try {
            RenderBlocks renderBlocks = new RenderBlocks(renderAccess);
            Set<BlockPos> failed = new HashSet<>();
            Tessellator tessellator = Tessellator.instance;
            for (int pass = 0; pass < 2; pass++) {
                if (pass == 1) {
                    GL11.glEnable(GL11.GL_BLEND);
                    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                    GL11.glDepthMask(false);
                }
                ForgeHooksClient.setRenderPass(pass);
                tessellator.startDrawingQuads();
                for (BlockPos pos : blocks.getPattern().keySet()) {
                    if (slice != null && pos.getY() != slice || failed.contains(pos)) {
                        continue;
                    }
                    Block block = renderAccess.getBlock(pos.getX(), pos.getY(), pos.getZ());
                    if (block == Blocks.air || !block.canRenderInPass(pass)) {
                        continue;
                    }
                    try {
                        renderBlocks.renderBlockByRenderType(block, pos.getX(), pos.getY(), pos.getZ());
                    } catch (Throwable t) {
                        failed.add(pos);
                    }
                }
                tessellator.draw();
            }
        } finally {
            renderAccess.slice = null;
            ForgeHooksClient.setRenderPass(-1);
        }
        GL11.glDepthMask(true);
        if (pickX >= 0 && pickY >= 0) {
            // 此刻深度缓冲仍是本次绘制的内容、矩阵也仍是本次的变换，正是反投影的时机
            pickedBlock = pickAt(pickX, pickY, slice);
        }
        GL11.glPopMatrix();
        GL11.glPopAttrib();
        GL11.glColor4f(1F, 1F, 1F, 1F);
    }

    /** 最近一次带拾取参数的渲染所命中的方块；未命中为 null。 */
    public BlockPos getPickedBlock() {
        return pickedBlock;
    }

    /**
     * 读取鼠标像素处的深度值，用当前的 MODELVIEW / PROJECTION / VIEWPORT 反投影成结构坐标，
     * 再取整得到方块坐标。与上游 {@code WorldSceneRenderer#unProject} 是同一思路，
     * 因此不依赖对相机变换的手工推导，正交 / 透视都成立。
     */
    private BlockPos pickAt(int pickX, int pickY, @Nullable Integer slice) {
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution res = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int scale = res.getScaleFactor();
        int px = pickX * scale;
        int py = mc.displayHeight - pickY * scale - 1;
        if (px < 0 || py < 0 || px >= mc.displayWidth || py >= mc.displayHeight) {
            return null;
        }

        FloatBuffer modelView = BufferUtils.createFloatBuffer(16);
        FloatBuffer projection = BufferUtils.createFloatBuffer(16);
        IntBuffer viewport = BufferUtils.createIntBuffer(16);
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelView);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projection);
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);

        // 不读深度缓冲：OptiFine / Angelica 的 GUI 帧缓冲里 glReadPixels(GL_DEPTH_COMPONENT) 恒为 0.5
        //（实测日志：preview picking depth read = 0.50000006），完全不可用。
        // 改为取射线两个深度端点，用 gluProject 投回屏幕比较 NDC z —— 越小越靠近相机。
        double[] depthZero = unproject(px, py, 0F, modelView, projection, viewport);
        double[] depthOne = unproject(px, py, 1F, modelView, projection, viewport);
        if (depthZero == null || depthOne == null) {
            return null;
        }

        FloatBuffer projected = BufferUtils.createFloatBuffer(3);
        double ndcZ0 = projectNdcZ(depthZero, modelView, projection, viewport, projected);
        double ndcZ1 = projectNdcZ(depthOne, modelView, projection, viewport, projected);

        // 实测：按 NDC z 选出的一端会落在结构背面（总是选到最后一个方块），因此这里取反。
        // 同时打一条日志，把两端的世界坐标与最终命中块记录下来，便于用实测值确认。
        double[] cameraEnd = depthZero;
        double[] otherEnd = depthOne;
        BlockPos picked = PreviewRayPicker.pickFromCamera(blocks.getPattern(), cameraEnd, otherEnd, slice);
        if (pickingDiagnosed.compareAndSet(false, true)) {
            hellfirepvp.modularmachinery.ModularMachinery.log.info(
                "[ModularMachinery] preview picking: ndcZ0={} ndcZ1={} e0={},{},{} e1={},{},{} -> {}",
                ndcZ0, ndcZ1,
                depthZero[0], depthZero[1], depthZero[2],
                depthOne[0], depthOne[1], depthOne[2],
                picked);
        }
        return picked;
    }

    /** gluProject 后取 NDC z；失败返回 NaN。 */
    private static double projectNdcZ(double[] point, FloatBuffer modelView, FloatBuffer projection,
                                      IntBuffer viewport, FloatBuffer out) {
        modelView.rewind();
        projection.rewind();
        viewport.rewind();
        if (!GLU.gluProject((float) point[0], (float) point[1], (float) point[2],
            modelView, projection, viewport, out)) {
            return Double.NaN;
        }
        out.rewind();
        out.get();
        out.get();
        return out.get();
    }

    /** gluUnProject 包装；失败返回 null。 */
    private static double[] unproject(int px, int py, float depth, FloatBuffer modelView,
                                      FloatBuffer projection, IntBuffer viewport) {
        FloatBuffer out = BufferUtils.createFloatBuffer(3);
        modelView.rewind();
        projection.rewind();
        viewport.rewind();
        if (!GLU.gluUnProject(px, py, depth, modelView, projection, viewport, out)) {
            return null;
        }
        out.rewind();
        return new double[] { out.get(), out.get(), out.get() };
    }

    /**
     * 结构的只读方块世界（坐标即 BlockArray 中的坐标）。替选方块按 sampleSnap 取样（-1 表示随客户端 tick 轮换）。
     * 设置 slice 后，其他层的方块视为空气（分层显示时各面都能画出来）。
     */
    public static class WorldBlockArrayRenderAccess implements IBlockAccess {
        private final Map<BlockPos, BlockArray.BlockInformation> pattern;
        private final LongSupplier                               sampleSnap;
        private final int                                        machineColor;
        private final Map<BlockPos, TileEntity>                  colorTiles = new HashMap<>();
        @Nullable
        Integer slice = null;

        public WorldBlockArrayRenderAccess(BlockArray array, LongSupplier sampleSnap, int machineColor) {
            this.pattern = array.getPattern();
            this.sampleSnap = sampleSnap;
            this.machineColor = machineColor;
        }

        @Nullable
        public IBlockState getState(int x, int y, int z) {
            if (slice != null && y != slice) {
                return null;
            }
            BlockArray.BlockInformation info = pattern.get(new BlockPos(x, y, z));
            if (info == null) {
                return null;
            }
            IBlockState state = info.getSampleState(sampleSnap.getAsLong());
            return state == null || state.getBlock() == Blocks.air ? null : state;
        }

        @Override
        public Block getBlock(int x, int y, int z) {
            IBlockState state = getState(x, y, z);
            return state == null ? Blocks.air : state.getBlock();
        }

        @Override
        public TileEntity getTileEntity(int x, int y, int z) {
            IBlockState state = getState(x, y, z);
            if (state == null || !(state.getBlock() instanceof BlockMachineComponent)) {
                return null;
            }
            return colorTiles.computeIfAbsent(new BlockPos(x, y, z), p -> {
                ColorTile tile = new ColorTile(machineColor);
                tile.xCoord = p.getX();
                tile.yCoord = p.getY();
                tile.zCoord = p.getZ();
                return tile;
            });
        }

        @Override
        public int getLightBrightnessForSkyBlocks(int x, int y, int z, int minLight) {
            return 15 << 20 | 15 << 4;
        }

        @Override
        public int getBlockMetadata(int x, int y, int z) {
            IBlockState state = getState(x, y, z);
            return state == null ? 0 : state.getMetadata();
        }

        @Override
        public int isBlockProvidingPowerTo(int x, int y, int z, int side) {
            return 0;
        }

        @Override
        public boolean isAirBlock(int x, int y, int z) {
            return getState(x, y, z) == null;
        }

        @Override
        public BiomeGenBase getBiomeGenForCoords(int x, int z) {
            return BiomeGenBase.plains;
        }

        @Override
        public int getHeight() {
            return 256;
        }

        @Override
        public boolean extendedLevelsInChunkCache() {
            return false;
        }

        @Override
        public boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean _default) {
            IBlockState state = getState(x, y, z);
            if (state == null) {
                return _default;
            }
            return state.getBlock().isSideSolid(this, x, y, z, side);
        }
    }

    /** 只用来提供外壳颜色的占位 TileEntity，不进入任何真实世界。 */
    private static final class ColorTile extends TileEntity implements ColorableMachineTile {
        private final int color;

        ColorTile(int color) {
            this.color = color;
        }

        @Override
        public int getMachineColor() {
            return color;
        }

        @Override
        public void setMachineColor(int newColor) {
        }

        @Override
        public boolean canUpdate() {
            return false;
        }
    }

}
