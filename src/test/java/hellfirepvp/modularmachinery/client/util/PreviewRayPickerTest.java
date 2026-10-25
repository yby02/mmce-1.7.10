package hellfirepvp.modularmachinery.client.util;

import github.kasuminova.mmce.compat.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 结构预览拾取的回归测试。
 * <p>
 * 断言的是“语义”而不是我的算式：
 * <ul>
 *   <li>单独的方块，从任意角度点击它的中心都必须命中它；</li>
 *   <li>实心结构从正上方点击，必须命中顶层（曾经错误地命中中间层）；</li>
 *   <li>任何角度点击实心结构，都不能命中内部方块 —— 内部方块玩家看不见，不该被选中。</li>
 * </ul>
 */
class PreviewRayPickerTest {

    private static final int CANVAS_X = 0;
    private static final int CANVAS_Y = 11;
    private static final int CANVAS_W = 166;
    private static final int CANVAS_H = 100;

    private static Map<BlockPos, Object> box(int sizeX, int sizeY, int sizeZ) {
        Map<BlockPos, Object> map = new LinkedHashMap<>();
        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    map.put(new BlockPos(x, y, z), Boolean.TRUE);
                }
            }
        }
        return map;
    }

    private static BlockPos pickAtCenterOf(Map<BlockPos, Object> pattern, BlockPos min, BlockPos max,
                                           float yaw, float pitch, BlockPos target) {
        double[] screen = PreviewRayPicker.project(min, max, CANVAS_X, CANVAS_Y, CANVAS_W, CANVAS_H,
            yaw, pitch, 1F, target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D);
        return PreviewRayPicker.pick(pattern, min, max, CANVAS_X, CANVAS_Y, CANVAS_W, CANVAS_H,
            yaw, pitch, 1F, (int) Math.round(screen[0]), (int) Math.round(screen[1]), null);
    }

    /** 单个方块没有遮挡歧义：任意角度点击它的投影中心都必须命中它自己。 */
    @Test
    void singleBlockPickedFromEveryAngle() {
        Map<BlockPos, Object> pattern = box(1, 1, 1);
        BlockPos min = new BlockPos(0, 0, 0);
        BlockPos max = new BlockPos(0, 0, 0);
        BlockPos only = new BlockPos(0, 0, 0);
        for (float yaw = 0F; yaw < 360F; yaw += 30F) {
            for (float pitch : new float[] { 0F, -30F, -60F, -90F, 30F, 60F, 90F }) {
                BlockPos picked = pickAtCenterOf(pattern, min, max, yaw, pitch, only);
                assertNotNull(picked, "yaw=" + yaw + " pitch=" + pitch + " 未命中唯一方块");
                assertEquals(only, picked, "yaw=" + yaw + " pitch=" + pitch);
            }
        }
    }

    /** 任何角度点击实心立方体，选中的必须是外露方块（至少一个坐标在边界上），不能是内部方块。 */
    @Test
    void solidCubeNeverPicksInteriorBlock() {
        Map<BlockPos, Object> pattern = box(3, 3, 3);
        BlockPos min = new BlockPos(0, 0, 0);
        BlockPos max = new BlockPos(2, 2, 2);
        int[] pixels = { CANVAS_X + CANVAS_W / 2, CANVAS_Y + CANVAS_H / 2 };
        for (float yaw = 0F; yaw < 360F; yaw += 30F) {
            for (float pitch : new float[] { -30F, -60F, -90F, 30F, 60F, 90F }) {
                BlockPos picked = PreviewRayPicker.pick(pattern, min, max, CANVAS_X, CANVAS_Y,
                    CANVAS_W, CANVAS_H, yaw, pitch, 1F, pixels[0], pixels[1], null);
                assertNotNull(picked, "yaw=" + yaw + " pitch=" + pitch + " 中心像素未命中");
                boolean onShell = picked.getX() == 0 || picked.getX() == 2
                    || picked.getY() == 0 || picked.getY() == 2
                    || picked.getZ() == 0 || picked.getZ() == 2;
                assertTrue(onShell, "yaw=" + yaw + " pitch=" + pitch + " 命中了内部方块 " + picked);
            }
        }
    }

    /** 点在对角线外侧（结构之外）不应命中任何方块。 */
    @Test
    void clickingOutsideMisses() {
        Map<BlockPos, Object> pattern = box(3, 3, 3);
        BlockPos picked = PreviewRayPicker.pick(pattern, new BlockPos(0, 0, 0), new BlockPos(2, 2, 2),
            CANVAS_X, CANVAS_Y, CANVAS_W, CANVAS_H, -45F, -30F, 1F, CANVAS_X + 1, CANVAS_Y + 1, null);
        assertNull(picked, "画布角落不应命中方块，实际 " + picked);
    }

    /** 分层显示时只在当前层内拾取。 */
    @Test
    void respectsSlice() {
        Map<BlockPos, Object> pattern = box(3, 3, 3);
        double[] screen = PreviewRayPicker.project(new BlockPos(0, 0, 0), new BlockPos(2, 2, 2),
            CANVAS_X, CANVAS_Y, CANVAS_W, CANVAS_H, -45F, -90F, 1F, 1.5D, 2.5D, 1.5D);
        BlockPos picked = PreviewRayPicker.pick(pattern, new BlockPos(0, 0, 0), new BlockPos(2, 2, 2),
            CANVAS_X, CANVAS_Y, CANVAS_W, CANVAS_H, -45F, -90F, 1F,
            (int) Math.round(screen[0]), (int) Math.round(screen[1]), 0);
        assertNotNull(picked);
        assertEquals(0, picked.getY(), "限定第 0 层时应命中 y=0，实际 y=" + picked.getY());
    }

    /**
     * 朝向判定：同一像素射线的两个端点顺序无关，只要给出可见表面点，
     * 就必须从靠近相机的一侧推进，返回靠近相机的那一层。
     */
    @Test
    void pickAlongIgnoresEndpointOrder() {
        Map<BlockPos, Object> pattern = box(3, 3, 3);
        double[] above = { 1.5D, 20D, 1.5D };
        double[] below = { 1.5D, -20D, 1.5D };
        double[] surface = { 1.5D, 3.0D, 1.5D };   // 顶面
        BlockPos forward = PreviewRayPicker.pickAlong(pattern, above, below, surface, null);
        BlockPos backward = PreviewRayPicker.pickAlong(pattern, below, above, surface, null);
        assertNotNull(forward);
        assertEquals(2, forward.getY(), "从上方看应命中顶层，实际 y=" + forward.getY());
        assertEquals(forward, backward, "端点顺序不应影响结果");
    }

    /** 从下方看时，表面点在底面，必须命中底层。 */
    @Test
    void pickAlongFromBelowHitsBottomLayer() {
        Map<BlockPos, Object> pattern = box(3, 3, 3);
        double[] above = { 1.5D, 20D, 1.5D };
        double[] below = { 1.5D, -20D, 1.5D };
        double[] surface = { 1.5D, 0.0D, 1.5D };
        BlockPos picked = PreviewRayPicker.pickAlong(pattern, below, above, surface, null);
        assertNotNull(picked);
        assertEquals(0, picked.getY(), "从下方看应命中底层，实际 y=" + picked.getY());
    }
}
