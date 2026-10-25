package hellfirepvp.modularmachinery.client.util;

import github.kasuminova.mmce.compat.BlockPos;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * 结构预览的方块拾取：把画布内的鼠标坐标还原成一条射线，返回射线上<b>第一个</b>属于结构的方块。
 * <p>
 * 变换与 {@link BlockArrayRenderHelper#render3DGUI} 完全一致：
 *
 * <pre>
 * glTranslatef(x + width / 2F, y + height / 2F, 400F);
 * glScalef(blockScale, -blockScale, blockScale);
 * glRotatef(pitch, 1F, 0F, 0F);
 * glRotatef(yaw, 0F, 1F, 0F);
 * glTranslatef(-cx, -cy, -cz);
 * </pre>
 *
 * 组成矩阵 {@code M = T · S · Rx · Ry · T2}，其中 {@code S = diag(s, -s, s)}。
 * <p>
 * 方向约定（容易搞反，单独说明）：<b>q 空间 z 越大越靠近相机</b>。因为 GUI 的正交投影下眼空间 z 越大越近，
 * 而 {@code glTranslatef(..., 400F)} 使模型整体向 +z 移动；q.z 增大等价于向相机移动。
 * 所以射线必须从 <b>az=1（近）</b> 一侧出发朝 az=0（远）推进，第一个命中的就是“看得见”的方块。
 * 反过来（从远端推进）会取到被遮挡的最深处方块 —— 三层实心结构从上往下点击时会选到中间层，正是这个错误的特征。
 * <p>
 * 本类不依赖任何客户端 / NEI 类型，便于单元测试。
 */
public final class PreviewRayPicker {

    /** 沿射线推进的步长（格）。必须 &lt; 1，保证不会跳过整个方块。 */
    private static final double STEP = 0.1D;

    private PreviewRayPicker() {
    }

    /**
     * @param pattern     结构方块表（坐标 → 任意值）
     * @param min         结构最小坐标（含）
     * @param max         结构最大坐标（含）
     * @param canvasX     预览画布左上角 x（页面坐标）
     * @param canvasY     预览画布左上角 y（页面坐标）
     * @param canvasWidth 画布宽
     * @param canvasHeight画布高
     * @param yaw         绕 Y 轴角度
     * @param pitch       绕 X 轴角度
     * @param zoom        缩放倍数
     * @param mouseX      鼠标 x（页面坐标）
     * @param mouseY      鼠标 y（页面坐标）
     * @param sliceY      只拾取该层（为 null 表示全部层）
     * @return 射线上第一个属于结构的方块；没有命中返回 null
     */
    public static BlockPos pick(Map<BlockPos, ?> pattern, BlockPos min, BlockPos max,
                                int canvasX, int canvasY, int canvasWidth, int canvasHeight,
                                float yaw, float pitch, float zoom,
                                int mouseX, int mouseY, @Nullable Integer sliceY) {
        if (pattern == null || pattern.isEmpty()) {
            return null;
        }
        float sizeX = max.getX() - min.getX() + 1;
        float sizeY = max.getY() - min.getY() + 1;
        float sizeZ = max.getZ() - min.getZ() + 1;
        float maxDim = Math.max(sizeX, Math.max(sizeY, sizeZ));
        float blockScale = Math.min(canvasWidth, canvasHeight) / (maxDim * 1.75F) * zoom;
        if (blockScale <= 0F) {
            return null;
        }
        float cx = min.getX() + sizeX / 2F;
        float cy = min.getY() + sizeY / 2F;
        float cz = min.getZ() + sizeZ / 2F;

        // 画布内坐标 → q 空间（撤销缩放；y 被 -s 缩放，所以这里已经取了负号）
        double qx = (mouseX - (canvasX + canvasWidth / 2F)) / blockScale;
        double qy = -(mouseY - (canvasY + canvasHeight / 2F)) / blockScale;

        double radPitch = Math.toRadians(pitch);
        double radYaw = Math.toRadians(yaw);
        double cosP = Math.cos(radPitch);
        double sinP = Math.sin(radPitch);
        double cosY = Math.cos(radYaw);
        double sinY = Math.sin(radYaw);

        // 相机端由实测确定：默认取 az=1 一侧（游戏内验证：这样点击才会选中玩家看得见的方块）。
        // 如需回退到另一侧，可加 JVM 参数 -Dmmce.preview.pickFlip=false。
        boolean flip = !"false".equals(System.getProperty("mmce.preview.pickFlip", "true"));
        double[] nearPoint = unproject(qx, qy, flip ? 1D : 0D, cosP, sinP, cosY, sinY, cx, cy, cz);
        double[] farPoint = unproject(qx, qy, flip ? 0D : 1D, cosP, sinP, cosY, sinY, cx, cy, cz);
        double dx = farPoint[0] - nearPoint[0];
        double dy = farPoint[1] - nearPoint[1];
        double dz = farPoint[2] - nearPoint[2];
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0E-9) {
            return null;
        }
        dx /= len;
        dy /= len;
        dz /= len;

        // 起点退到包围盒之外，保证从相机一侧开始推进
        double diag = Math.sqrt(sizeX * sizeX + sizeY * sizeY + sizeZ * sizeZ) + 2D;
        double sx = nearPoint[0] - dx * diag;
        double sy = nearPoint[1] - dy * diag;
        double sz = nearPoint[2] - dz * diag;

        BlockPos last = null;
        double limit = diag * 2D;
        for (double t = 0D; t <= limit; t += STEP) {
            BlockPos pos = new BlockPos((int) Math.floor(sx + dx * t),
                (int) Math.floor(sy + dy * t),
                (int) Math.floor(sz + dz * t));
            if (pos.equals(last)) {
                continue;
            }
            last = pos;
            if (sliceY != null && pos.getY() != sliceY) {
                continue;
            }
            if (pattern.containsKey(pos)) {
                return pos;
            }
        }
        return null;
    }

    /**
     * 沿一条已知射线拾取，方向由「表面点更靠近哪一端」自动判定，因此不依赖 az→相机的符号约定。
     * <p>
     * {@code e0} / {@code e1} 是同一像素射线上的两个端点（任意顺序），{@code surface} 是该像素深度值
     * 反投影出的可见表面点。可见表面必然位于靠近相机的一侧，所以离 surface 更近的那个端点就是相机端；
     * 从它外侧出发朝另一端推进，第一个命中的方块就是玩家看得见的方块。
     *
     * @param pattern 结构方块表
     * @param e0      射线端点之一
     * @param e1      另一端点
     * @param surface 可见表面点
     * @param sliceY  限定层，null 表示全部
     * @return 第一个命中的方块；未命中返回 null
     */
    public static BlockPos pickAlong(Map<BlockPos, ?> pattern, double[] e0, double[] e1, double[] surface,
                                     @Nullable Integer sliceY) {
        if (pattern == null || pattern.isEmpty() || e0 == null || e1 == null || surface == null) {
            return null;
        }
        double dx = e1[0] - e0[0];
        double dy = e1[1] - e0[1];
        double dz = e1[2] - e0[2];
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0E-9) {
            return null;
        }
        dx /= len;
        dy /= len;
        dz /= len;

        double dist0 = distance(e0, surface);
        double dist1 = distance(e1, surface);
        boolean e0IsCamera = dist0 <= dist1;
        double[] cameraEnd = e0IsCamera ? e0 : e1;
        double[] farEnd = e0IsCamera ? e1 : e0;

        // 推进方向必须是从相机端指向另一端（不能固定用 e0→e1，否则端点顺序反了就会朝反方向走）
        dx = farEnd[0] - cameraEnd[0];
        dy = farEnd[1] - cameraEnd[1];
        dz = farEnd[2] - cameraEnd[2];
        len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0E-9) {
            return null;
        }
        dx /= len;
        dy /= len;
        dz /= len;

        // 结构包围盒对角线，用于把起点退到模型之外、并给出足够的推进长度
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : pattern.keySet()) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        double diag = Math.sqrt((maxX - minX + 1D) * (maxX - minX + 1D)
            + (maxY - minY + 1D) * (maxY - minY + 1D)
            + (maxZ - minZ + 1D) * (maxZ - minZ + 1D)) + 2D;

        double sx = cameraEnd[0] - dx * diag;
        double sy = cameraEnd[1] - dy * diag;
        double sz = cameraEnd[2] - dz * diag;

        BlockPos last = null;
        // 起点在相机端外 diag 处，还要再走过两端的全部距离才保证穿过整个结构
        double limit = diag * 2D + distance(e0, e1);
        for (double t = 0D; t <= limit; t += STEP) {
            BlockPos pos = new BlockPos((int) Math.floor(sx + dx * t),
                (int) Math.floor(sy + dy * t),
                (int) Math.floor(sz + dz * t));
            if (pos.equals(last)) {
                continue;
            }
            last = pos;
            if (sliceY != null && pos.getY() != sliceY) {
                continue;
            }
            if (pattern.containsKey(pos)) {
                return pos;
            }
        }
        return null;
    }

    private static double distance(double[] a, double[] b) {
        double dx = a[0] - b[0];
        double dy = a[1] - b[1];
        double dz = a[2] - b[2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * 已知哪一端是相机端时使用：从该端外侧朝另一端推进，返回第一个命中的方块。
     * <p>
     * 相机端由调用方用真实 GL 矩阵（{@code gluProject} 比较 NDC z）判定 —— 本模组之前尝试过用
     * {@code glReadPixels} 读深度来判断，但在 OptiFine / Angelica 的 GUI 帧缓冲里读到的深度恒为 0.5，
     * 完全不可用，因此改为不依赖深度缓冲。
     */
    public static BlockPos pickFromCamera(Map<BlockPos, ?> pattern, double[] cameraEnd, double[] otherEnd,
                                          @Nullable Integer sliceY) {
        if (pattern == null || pattern.isEmpty() || cameraEnd == null || otherEnd == null) {
            return null;
        }
        double dx = otherEnd[0] - cameraEnd[0];
        double dy = otherEnd[1] - cameraEnd[1];
        double dz = otherEnd[2] - cameraEnd[2];
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0E-9) {
            return null;
        }
        dx /= len;
        dy /= len;
        dz /= len;

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : pattern.keySet()) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        double diag = Math.sqrt((maxX - minX + 1D) * (maxX - minX + 1D)
            + (maxY - minY + 1D) * (maxY - minY + 1D)
            + (maxZ - minZ + 1D) * (maxZ - minZ + 1D)) + 2D;

        // 起点：从相机端再往外退 diag，确保在结构之外
        double sx = cameraEnd[0] - dx * diag;
        double sy = cameraEnd[1] - dy * diag;
        double sz = cameraEnd[2] - dz * diag;

        BlockPos last = null;
        double limit = diag * 2D + len;
        for (double t = 0D; t <= limit; t += STEP) {
            BlockPos pos = new BlockPos((int) Math.floor(sx + dx * t),
                (int) Math.floor(sy + dy * t),
                (int) Math.floor(sz + dz * t));
            if (pos.equals(last)) {
                continue;
            }
            last = pos;
            if (sliceY != null && pos.getY() != sliceY) {
                continue;
            }
            if (pattern.containsKey(pos)) {
                return pos;
            }
        }
        return null;
    }

    /** 正向投影：结构坐标 → 页面坐标（与 render3DGUI 的变换一致）。 */
    public static double[] project(BlockPos min, BlockPos max,
                                   int canvasX, int canvasY, int canvasWidth, int canvasHeight,
                                   float yaw, float pitch, float zoom,
                                   double x, double y, double z) {
        float sizeX = max.getX() - min.getX() + 1;
        float sizeY = max.getY() - min.getY() + 1;
        float sizeZ = max.getZ() - min.getZ() + 1;
        float maxDim = Math.max(sizeX, Math.max(sizeY, sizeZ));
        float blockScale = Math.min(canvasWidth, canvasHeight) / (maxDim * 1.75F) * zoom;
        float cx = min.getX() + sizeX / 2F;
        float cy = min.getY() + sizeY / 2F;
        float cz = min.getZ() + sizeZ / 2F;

        double radPitch = Math.toRadians(pitch);
        double radYaw = Math.toRadians(yaw);
        double cosP = Math.cos(radPitch);
        double sinP = Math.sin(radPitch);
        double cosY = Math.cos(radYaw);
        double sinY = Math.sin(radYaw);

        double px = x - cx;
        double py = y - cy;
        double pz = z - cz;
        // Ry(yaw)
        double bx = px * cosY + pz * sinY;
        double bz = -px * sinY + pz * cosY;
        double by = py;
        // Rx(pitch)
        double ax = bx;
        double ay = by * cosP - bz * sinP;
        return new double[] { canvasX + canvasWidth / 2.0 + ax * blockScale,
            canvasY + canvasHeight / 2.0 - ay * blockScale };
    }

    /** q → 结构坐标。 */
    private static double[] unproject(double qx, double qy, double az, double cosP, double sinP,
                                      double cosY, double sinY, float cx, float cy, float cz) {
        double ax = qx;
        double ay = qy;
        // 撤销 Rx(pitch)
        double bx = ax;
        double by = ay * cosP + az * sinP;
        double bz = -ay * sinP + az * cosP;
        // 撤销 Ry(yaw)
        double px = bx * cosY - bz * sinY;
        double py = by;
        double pz = bx * sinY + bz * cosY;
        return new double[] { cx + px, cy + py, cz + pz };
    }
}
