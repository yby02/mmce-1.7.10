/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.client.util;

import github.kasuminova.mmce.compat.BlockPos;
import github.kasuminova.mmce.compat.EnumFacing;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.client.ClientScheduler;
import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.network.PktAssemblyPatternSize;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GLContext;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: BlockArrayPreviewRenderHelper
 * Created by HellFirePvP
 * Date: 30.12.2017 / 14:38
 * <p>
 * 把结构预览投影到世界中，半透明显示缺少的方块：
 * <ul>
 *   <li>开始投影后，投影跟随准星（射线 20 格）；准星对着控制器时按控制器朝向旋转并对齐控制器，否则以结构底面中心贴在所看方块的那一面上；</li>
 *   <li>右键放置后只显示自下而上第一层尚未搭好的方块；全部搭好、离开 32 格以上、断开连接、开始另一个投影、
 *       打开另一台机器的蓝图预览时清除；</li>
 *   <li>开始 / 清除时把机器与动态结构尺寸同步给服务端，投影期间自动组装使用该尺寸（{@link PktAssemblyPatternSize}）。</li>
 * </ul>
 * 1.7.10 移植：射线检测用 EntityLivingBase#rayTrace；方块用 RenderBlocks（renderAllFaces）编进显示列表，每块缩小到 0.75 并以
 * 常量 alpha 半透明混合（缺少 OpenGL 1.4 时退回上游的 ONE_MINUS_DST_COLOR / DST_COLOR 混合）；缺失方块集合每 tick 计算一次。
 */
public class BlockArrayPreviewRenderHelper {

    private static final double MAX_DISTANCE_SQ = 1024D;
    private static final double LOOK_RANGE      = 20D;
    private static final float  GHOST_ALPHA     = 0.55F;

    private BlockArrayRenderHelper      renderHelper       = null;
    private BlockArray                  matchArray         = null;
    private BlockPos                    renderHelperOffset = null;
    private DynamicMachine              machine            = null;
    private BlockPos                    attachedPosition   = null;
    private int                         renderedLayer      = -1;
    private DynamicMachineRenderContext context            = null;
    private long                        placedTick         = -1;

    private final Map<EnumFacing, BlockArray>                                         rotatedArrays = new EnumMap<>(EnumFacing.class);
    private final Map<BlockArray, BlockArrayRenderHelper.WorldBlockArrayRenderAccess> accesses      = new IdentityHashMap<>();

    /** 本 tick 要显示的结构、原点与缺失方块（相对原点）。 */
    private       BlockArray     renderArray  = null;
    private       BlockPos       renderOrigin = null;
    private final List<BlockPos> missing      = new ArrayList<>();

    private int displayList = -1;
    private int displayHash = 0;

    @Nullable
    private static MovingObjectPosition getLookBlock(EntityPlayer player) {
        MovingObjectPosition mop = player.rayTrace(LOOK_RANGE, 1F);
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            return null;
        }
        return mop;
    }

    public boolean startPreview(DynamicMachineRenderContext currentContext) {
        clearSelection(false);
        if (currentContext.getShiftSnap() == -1) {
            currentContext.snapSamples();
        }
        this.context = currentContext;
        this.renderHelper = currentContext.getRender();
        this.matchArray = this.renderHelper.getBlocks();
        this.renderHelperOffset = currentContext.getMoveOffset();
        this.machine = currentContext.getDisplayedMachine();
        this.attachedPosition = null;
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player != null) {
            player.addChatMessage(new ChatComponentTranslation("gui.blueprint.popout.place"));
        }
        syncPatternSize(machine.getRegistryName().toString(), currentContext.getDynamicPatternSize());
        updateRenderTarget();
        return true;
    }

    public boolean placePreview() {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        if (player == null || mc.theWorld == null || this.renderHelper == null || this.attachedPosition != null) {
            return false;
        }
        MovingObjectPosition lookBlock = getLookBlock(player);
        if (lookBlock == null) {
            return false;
        }
        BlockPos hit = new BlockPos(lookBlock.blockX, lookBlock.blockY, lookBlock.blockZ);
        Block block = mc.theWorld.getBlock(hit.getX(), hit.getY(), hit.getZ());
        BlockPos attachPos;
        if (block instanceof BlockController) {
            EnumFacing rotate = BlockController.getFacing(mc.theWorld.getBlockMetadata(hit.getX(), hit.getY(), hit.getZ()));
            attachPos = hit.subtract(MiscUtils.rotateYCCWNorthUntil(this.renderHelperOffset, rotate));
            this.matchArray = rotatedArray(rotate);
        } else {
            attachPos = hit.offset(EnumFacing.byIndex(lookBlock.sideHit));
        }
        this.attachedPosition = attachPos;
        this.placedTick = ClientScheduler.getClientTick();
        updateLayers();
        if (context.getDynamicPatternSize() > 0) {
            player.addChatMessage(new ChatComponentTranslation("gui.blueprint.popout.placed.dynamic_pattern"));
        }
        updateRenderTarget();
        return true;
    }

    /** 本 tick 刚刚放置了投影（同一次右键随后触发的 RIGHT_CLICK_AIR 也应被取消）。 */
    public boolean wasPlacedThisTick() {
        return placedTick == ClientScheduler.getClientTick();
    }

    public void tick() {
        Minecraft mc = Minecraft.getMinecraft();
        if (renderHelper == null || mc.theWorld == null || mc.thePlayer == null) {
            return;
        }
        if (attachedPosition != null) {
            if (mc.thePlayer.getDistanceSq(attachedPosition.getX() + 0.5, attachedPosition.getY() + 0.5, attachedPosition.getZ() + 0.5) >= MAX_DISTANCE_SQ) {
                clearSelection(true);
                return;
            }
            if (hasLowerLayer() && !doesPlacedLayerMatch(this.renderedLayer - 1)) {
                updateLayers();
            } else if (doesPlacedLayerMatch(this.renderedLayer)) {
                if (!this.matchArray.matches(mc.theWorld, this.attachedPosition, true, null)) {
                    updateLayers();
                } else {
                    clearSelection(true);
                    return;
                }
            }
        }
        updateRenderTarget();
    }

    private BlockArray rotatedArray(EnumFacing facing) {
        return rotatedArrays.computeIfAbsent(facing, f -> MiscUtils.rotateYCCWNorthUntil(this.renderHelper.getBlocks(), f));
    }

    /** 计算本 tick 投影的原点（放置后固定，放置前跟随准星）与缺失方块。 */
    private void updateRenderTarget() {
        missing.clear();
        renderArray = null;
        renderOrigin = null;
        Minecraft mc = Minecraft.getMinecraft();
        World world = mc.theWorld;
        if (renderHelper == null || world == null || mc.thePlayer == null) {
            return;
        }
        if (attachedPosition != null) {
            renderOrigin = attachedPosition;
            renderArray = matchArray;
        } else {
            MovingObjectPosition res = getLookBlock(mc.thePlayer);
            if (res == null) {
                return;
            }
            BlockPos hit = new BlockPos(res.blockX, res.blockY, res.blockZ);
            if (world.getBlock(hit.getX(), hit.getY(), hit.getZ()) instanceof BlockController) {
                EnumFacing rotate = BlockController.getFacing(world.getBlockMetadata(hit.getX(), hit.getY(), hit.getZ()));
                renderOrigin = hit.subtract(MiscUtils.rotateYCCWNorthUntil(this.renderHelperOffset, rotate));
                renderArray = rotatedArray(rotate);
            } else {
                renderOrigin = hit.offset(EnumFacing.byIndex(res.sideHit));
                renderArray = renderHelper.getBlocks();
            }
        }
        for (Map.Entry<BlockPos, BlockArray.BlockInformation> entry : renderArray.getPattern().entrySet()) {
            BlockPos offset = entry.getKey();
            if (attachedPosition != null && offset.getY() != renderedLayer) {
                continue;
            }
            if (entry.getValue().matches(world, renderOrigin.add(offset), false)) {
                continue;
            }
            missing.add(offset);
        }
    }

    public void renderTranslucentBlocks(float partialTicks) {
        if (renderOrigin == null || renderArray == null || missing.isEmpty()) {
            return;
        }
        int hash = System.identityHashCode(renderArray) * 31 + missing.hashCode();
        if (displayList == -1 || hash != displayHash) {
            if (displayList == -1) {
                displayList = GLAllocation.generateDisplayLists(1);
            }
            GL11.glNewList(displayList, GL11.GL_COMPILE);
            batchBlocks(renderArray, missing);
            GL11.glEndList();
            displayHash = hash;
        }

        boolean constantAlpha = GLContext.getCapabilities().OpenGL14;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_CURRENT_BIT | GL11.GL_LIGHTING_BIT);
        GL11.glPushMatrix();
        GL11.glTranslated(renderOrigin.getX() - RenderManager.renderPosX, renderOrigin.getY() - RenderManager.renderPosY, renderOrigin.getZ() - RenderManager.renderPosZ);
        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        if (constantAlpha) {
            GL14.glBlendColor(1F, 1F, 1F, GHOST_ALPHA);
            GL11.glBlendFunc(GL11.GL_CONSTANT_ALPHA, GL11.GL_ONE_MINUS_CONSTANT_ALPHA);
        } else {
            GL11.glBlendFunc(GL11.GL_ONE_MINUS_DST_COLOR, GL11.GL_DST_COLOR);
        }
        GL11.glColor4f(1F, 1F, 1F, 1F);
        GL11.glCallList(displayList);
        GL11.glPopMatrix();
        GL11.glPopAttrib();
        GL11.glDepthMask(true);
        //Color desync on block rendering - prevent that, resync
        GL11.glColor4f(1F, 1F, 1F, 1F);
    }

    private void batchBlocks(BlockArray array, List<BlockPos> positions) {
        BlockArrayRenderHelper.WorldBlockArrayRenderAccess access = accesses.computeIfAbsent(array,
            a -> new BlockArrayRenderHelper.WorldBlockArrayRenderAccess(a, () -> context == null ? -1 : context.getShiftSnap(), machine.getMachineColor()));
        RenderBlocks renderBlocks = new RenderBlocks(access);
        renderBlocks.renderAllFaces = true;
        Tessellator tes = Tessellator.instance;
        try {
            for (BlockPos pos : positions) {
                Block block = access.getBlock(pos.getX(), pos.getY(), pos.getZ());
                if (block == Blocks.air) {
                    continue;
                }
                GL11.glPushMatrix();
                GL11.glTranslated(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                GL11.glScaled(0.75, 0.75, 0.75);
                GL11.glTranslated(-pos.getX() - 0.5, -pos.getY() - 0.5, -pos.getZ() - 0.5);
                for (int pass = 0; pass < 2; pass++) {
                    if (!block.canRenderInPass(pass)) {
                        continue;
                    }
                    ForgeHooksClient.setRenderPass(pass);
                    tes.startDrawingQuads();
                    try {
                        renderBlocks.renderBlockByRenderType(block, pos.getX(), pos.getY(), pos.getZ());
                    } catch (Throwable ignored) {
                        // 个别方块渲染需要真实世界中的 TileEntity，跳过
                    }
                    tes.draw();
                }
                GL11.glPopMatrix();
            }
        } finally {
            ForgeHooksClient.setRenderPass(-1);
        }
    }

    private boolean doesPlacedLayerMatch(int slice) {
        if (this.attachedPosition != null) {
            World world = Minecraft.getMinecraft().theWorld;
            if (world != null) {
                for (Map.Entry<BlockPos, BlockArray.BlockInformation> data : this.matchArray.getPatternSlice(slice).entrySet()) {
                    if (!data.getValue().matches(world, data.getKey().add(this.attachedPosition), false)) {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

    private boolean hasLowerLayer() {
        if (this.attachedPosition != null) {
            return (this.matchArray.getMin().getY()) <= this.renderedLayer - 1;
        }
        return false;
    }

    private void updateLayers() {
        this.renderedLayer = -1;
        if (this.attachedPosition != null) {
            int lowestSlice = matchArray.getMin().getY();
            int maxSlice = matchArray.getMax().getY();
            for (int y = lowestSlice; y <= maxSlice; y++) {
                if (!doesPlacedLayerMatch(y)) {
                    this.renderedLayer = y;
                    return;
                }
            }
        }
    }

    private void clearSelection(boolean notifyServer) {
        boolean wasActive = this.context != null;
        this.renderHelper = null;
        this.matchArray = null;
        this.renderHelperOffset = null;
        this.attachedPosition = null;
        this.machine = null;
        this.context = null;
        this.renderedLayer = -1;
        this.placedTick = -1;
        this.rotatedArrays.clear();
        this.accesses.clear();
        this.renderArray = null;
        this.renderOrigin = null;
        this.missing.clear();
        if (wasActive && notifyServer) {
            syncPatternSize("", 0);
        }
    }

    /** 手动清除投影（例如打开另一台机器的蓝图预览）。 */
    public void clearPreview() {
        clearSelection(true);
    }

    /** 当前投影的不是该机器时清除投影。 */
    public void clearIfOtherMachine(DynamicMachine other) {
        if (this.machine != null && this.machine != other) {
            clearSelection(true);
        }
    }

    public void unloadWorld() {
        clearSelection(false);
    }

    private static void syncPatternSize(String machine, int size) {
        if (Minecraft.getMinecraft().getNetHandler() != null) {
            ModularMachinery.NET_CHANNEL.sendToServer(new PktAssemblyPatternSize(machine, size));
        }
    }

    @Nullable
    public DynamicMachineRenderContext getContext() {
        return context;
    }

    @Nullable
    public BlockPos getAttachedPosition() {
        return attachedPosition;
    }

    /** 当前投影的方块（已旋转，坐标相对 {@link #getAttachedPosition()}）；未放置或未投影时为 null。 */
    @Nullable
    public BlockArray getPlacedPattern() {
        return attachedPosition == null ? null : matchArray;
    }

    /** 放置后当前显示的层（结构坐标中的 y），全部搭好时为 -1。 */
    public int getRenderedLayer() {
        return renderedLayer;
    }

    /** 本 tick 显示的缺失方块（只读）。 */
    public List<BlockPos> getMissingBlocks() {
        return Collections.unmodifiableList(missing);
    }

}
