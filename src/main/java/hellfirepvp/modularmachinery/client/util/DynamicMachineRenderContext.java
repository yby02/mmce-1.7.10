/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.client.util;

import github.kasuminova.mmce.common.util.DynamicPattern;
import github.kasuminova.mmce.compat.BlockPos;
import github.kasuminova.mmce.compat.EnumFacing;
import hellfirepvp.modularmachinery.client.ClientScheduler;
import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.block.BlockFactoryController;
import hellfirepvp.modularmachinery.common.data.Config;
import hellfirepvp.modularmachinery.common.lib.BlocksMM;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import hellfirepvp.modularmachinery.common.util.IBlockStateDescriptor;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: DynamicMachineRenderContext
 * Created by HellFirePvP
 * Date: 09.07.2017 / 21:18
 * <p>
 * 1.7.10 移植：
 * <ul>
 *   <li>视角（yaw / pitch / 缩放）保存在这里，渲染交给 {@link BlockArrayRenderHelper#render3DGUI}；</li>
 *   <li>控制器位置使用“接受任意朝向、以朝北显示”的描述符（1.12.2 用默认状态，只认一个朝向，投影贴到其他朝向的控制器上时控制器会一直显示为缺失）；</li>
 *   <li>另外保存未平移、不含控制器的结构（含动态结构）{@link #getStructurePattern()}，供方块清单使用。</li>
 * </ul>
 */
public class DynamicMachineRenderContext {

    private final DynamicMachine         machine;
    private final BlockArrayRenderHelper render;
    private final BlockArray             pattern;
    private final BlockArray             structurePattern;
    private final BlockPos               moveOffset;
    private       int                    dynamicPatternSize;

    private boolean render3D    = true;
    private int     renderSlice = 0;
    private float   scale       = 1F;
    private float   yaw         = BlockArrayRenderHelper.DEFAULT_YAW;
    private float   pitch       = BlockArrayRenderHelper.DEFAULT_PITCH;

    private long shiftSnap = -1;

    private DynamicMachineRenderContext(DynamicMachine machine, BlockArray pattern, int dynamicPatternSize) {
        this.machine = machine;
        this.dynamicPatternSize = dynamicPatternSize;

        BlockArray copy = buildDynamicPattern(machine, new BlockArray(pattern));
        this.structurePattern = copy;

        BlockPos min = copy.getMin();
        BlockPos max = copy.getMax();
        this.moveOffset = new BlockPos(
            (min.getX() + (max.getX() - min.getX()) / 2) * -1,
            -min.getY(),
            (min.getZ() + (max.getZ() - min.getZ()) / 2) * -1
        );

        copy = new BlockArray(copy, moveOffset);

        addControllerToBlockArray(machine, copy, this.moveOffset);
        addReplacementToBlockArray(machine.getModifiersAsMatchingReplacements(), copy, this.moveOffset);

        this.pattern = copy;
        this.render = new BlockArrayRenderHelper(copy, machine.getMachineColor());
    }

    private static void addControllerToBlockArray(DynamicMachine machine, BlockArray copy, BlockPos moveOffset) {
        // Factory Only
        if (machine.isHasFactory() && machine.isFactoryOnly()) {
            BlockFactoryController factory = BlockFactoryController.getControllerWithMachine(machine);
            if (factory == null) {
                factory = BlocksMM.blockFactoryController;
            }
            copy.addBlock(moveOffset, new BlockArray.BlockInformation(
                Collections.singletonList(controllerDescriptor(factory))));
            return;
        }

        List<IBlockStateDescriptor> descriptors = new ArrayList<>();

        // Controller
        BlockController ctrl = BlockController.getControllerWithMachine(machine);
        if (ctrl == null) {
            ctrl = BlocksMM.blockController;
        }
        descriptors.add(controllerDescriptor(ctrl));

        // Factory
        if (machine.isHasFactory() || Config.enableFactoryControllerByDefault) {
            BlockFactoryController factory = BlockFactoryController.getControllerWithMachine(machine);
            if (factory == null) {
                factory = BlocksMM.blockFactoryController;
            }
            descriptors.add(controllerDescriptor(factory));
        }

        copy.addBlock(moveOffset, new BlockArray.BlockInformation(descriptors));
    }

    /** 未旋转的结构定义对应控制器朝北。 */
    private static IBlockStateDescriptor controllerDescriptor(BlockController controller) {
        return IBlockStateDescriptor.wildcardWithSample(controller.withFacing(EnumFacing.NORTH));
    }

    public static void addReplacementToBlockArray(
        DynamicMachine.ModifierReplacementMap replacementMap,
        BlockArray blockArray,
        BlockPos moveOffset) {
        for (Map.Entry<BlockPos, List<BlockArray.BlockInformation>> entry : replacementMap.entrySet()) {
            BlockPos pos = entry.getKey().add(moveOffset);

            List<BlockArray.BlockInformation> informationList = entry.getValue();
            for (BlockArray.BlockInformation info : informationList) {
                Map<BlockPos, BlockArray.BlockInformation> pattern = blockArray.getPattern();
                if (pattern.containsKey(pos)) {
                    // Clone the block info, we don't want to modify the canonical instance.
                    BlockArray.BlockInformation newInfo = pattern.get(pos).copy();
                    newInfo.addMatchingStates(info.getMatchingStates());
                    blockArray.addBlock(pos, newInfo);
                } else {
                    blockArray.addBlock(pos, info);
                }
            }
        }
    }

    public static DynamicMachineRenderContext createContext(DynamicMachine machine) {
        return createContext(machine, 0);
    }

    public static DynamicMachineRenderContext createContext(final DynamicMachine machine,
                                                            final int dynamicPatternSize) {
        return new DynamicMachineRenderContext(machine, machine.getPattern(), dynamicPatternSize);
    }

    private BlockArray buildDynamicPattern(final DynamicMachine machine, BlockArray copy) {
        Map<String, DynamicPattern> dynamicPatterns = machine.getDynamicPatterns();

        if (!dynamicPatterns.isEmpty()) {
            for (final DynamicPattern pattern : dynamicPatterns.values()) {
                this.dynamicPatternSize = Math.max(dynamicPatternSize, pattern.getMinSize());
            }

            for (final DynamicPattern pattern : dynamicPatterns.values()) {
                pattern.addPatternToBlockArray(
                    copy,
                    Math.min(Math.max(pattern.getMinSize(), dynamicPatternSize), pattern.getMaxSize()),
                    pattern.getFaces().iterator().next(),
                    EnumFacing.NORTH);
            }
        }

        return copy;
    }

    /** 所有动态结构的最小尺寸中的最大值（没有动态结构时为 0）。 */
    public static int minDynamicPatternSize(DynamicMachine machine) {
        int size = 0;
        for (DynamicPattern pattern : machine.getDynamicPatterns().values()) {
            size = Math.max(size, pattern.getMinSize());
        }
        return size;
    }

    /** 所有动态结构的最大尺寸中的最大值（没有动态结构时为 0）。 */
    public static int maxDynamicPatternSize(DynamicMachine machine) {
        int size = 0;
        for (DynamicPattern pattern : machine.getDynamicPatterns().values()) {
            size = Math.max(size, pattern.getMaxSize());
        }
        return size;
    }

    public BlockArrayRenderHelper getRender() {
        return render;
    }

    /** 渲染 / 投影用的结构：已平移到“最低层 y = 0、水平居中”，含控制器与升级方块的替选。 */
    public BlockArray getPattern() {
        return pattern;
    }

    /** 以控制器为原点、含动态结构、不含控制器的结构（控制器朝北）。 */
    public BlockArray getStructurePattern() {
        return structurePattern;
    }

    public int getDynamicPatternSize() {
        return dynamicPatternSize;
    }

    public BlockPos getMoveOffset() {
        return moveOffset;
    }

    public long getShiftSnap() {
        return shiftSnap;
    }

    public void setShiftSnap(final long shiftSnap) {
        this.shiftSnap = shiftSnap;
        this.render.sampleSnap = shiftSnap;
    }

    public void snapSamples() {
        setShiftSnap(ClientScheduler.getClientTick());
    }

    public void releaseSamples() {
        setShiftSnap(-1);
    }

    public void resetRender() {
        setTo2D();
        setTo3D();
    }

    /** 分层模式：从最低层开始，俯视。 */
    public void setTo2D() {
        if (!render3D) {
            return;
        }
        render3D = false;
        renderSlice = pattern.getMin().getY();
        yaw = 180F;
        pitch = 90F;
        scale = 1F;
    }

    public void setTo3D() {
        if (render3D) {
            return;
        }
        render3D = true;
        renderSlice = 0;
        resetRotation();
    }

    public void resetRotation() {
        if (render3D) {
            yaw = BlockArrayRenderHelper.DEFAULT_YAW;
            pitch = BlockArrayRenderHelper.DEFAULT_PITCH;
        } else {
            yaw = 180F;
            pitch = 90F;
        }
        scale = 1F;
    }

    /** 复制另一个上下文的视角与显示模式（例如调整动态结构尺寸后重建上下文时）。 */
    public void copyViewFrom(DynamicMachineRenderContext other) {
        this.render3D = other.render3D;
        this.renderSlice = Math.max(pattern.getMin().getY(), Math.min(pattern.getMax().getY(), other.renderSlice));
        this.scale = other.scale;
        this.yaw = other.yaw;
        this.pitch = other.pitch;
        setShiftSnap(other.shiftSnap);
    }

    public float getScale() {
        return scale;
    }

    public void zoomOut() {
        scale = Math.max(0.3F, scale * 0.85F);
    }

    public void zoomIn() {
        scale = Math.min(5F, scale * 1.15F);
    }

    public boolean doesRenderIn3D() {
        return render3D;
    }

    /** 当前显示的层相对最低层的序号（从 0 开始）。 */
    public int getRenderSlice() {
        return renderSlice - pattern.getMin().getY();
    }

    public int getLayerCount() {
        return pattern.getMax().getY() - pattern.getMin().getY() + 1;
    }

    public boolean hasSliceDown() {
        return pattern.getMin().getY() < renderSlice;
    }

    public boolean hasSliceUp() {
        return pattern.getMax().getY() > renderSlice;
    }

    public void sliceUp() {
        if (hasSliceUp()) {
            renderSlice++;
        }
    }

    public void sliceDown() {
        if (hasSliceDown()) {
            renderSlice--;
        }
    }

    public DynamicMachine getDisplayedMachine() {
        return machine;
    }

    /** 结构所需方块（按物品合并计数，不含控制器）；替选方块按当前取样显示。 */
    public List<ItemStack> getDescriptiveStacks() {
        return this.structurePattern.getAsDescriptiveStacks(shiftSnap);
    }

    public void renderAt(int x, int y, int width, int height) {
        render.render3DGUI(x, y, width, height, yaw, pitch, scale, render3D ? null : renderSlice);
    }

    public void rotateRender(float dYaw, float dPitch) {
        this.yaw += dYaw;
        this.pitch = Math.max(-90F, Math.min(90F, this.pitch + dPitch));
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setView(float yaw, float pitch, float scale) {
        this.yaw = yaw;
        this.pitch = Math.max(-90F, Math.min(90F, pitch));
        this.scale = Math.max(0.3F, Math.min(5F, scale));
    }

}
