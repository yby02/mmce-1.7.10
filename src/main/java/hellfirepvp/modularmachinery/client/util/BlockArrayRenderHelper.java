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
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.common.util.ForgeDirection;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.nio.FloatBuffer;
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
        GL11.glPopMatrix();
        GL11.glPopAttrib();
        GL11.glColor4f(1F, 1F, 1F, 1F);
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
