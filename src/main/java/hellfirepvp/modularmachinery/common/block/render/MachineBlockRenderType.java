package hellfirepvp.modularmachinery.common.block.render;

/**
 * 1.7.10 移植：机器组件方块使用的渲染类型 ID 与覆盖层渲染状态。
 * 客户端在预初始化时注册 ISBRH 并写入 {@link #renderId}；服务端保持 0（标准方块），不会被使用。
 * <p>
 * 1.12.2 用双层方块模型（着色底层 + 不着色覆盖层）。1.7.10 的 ISBRH 先正常画一遍方块，
 * 再在 {@link #beginOverlayPass()} / {@link #endOverlayPass()} 之间画第二遍，期间方块返回覆盖层贴图和白色。
 */
public final class MachineBlockRenderType {

    public static int renderId = 0;

    private static final ThreadLocal<int[]> OVERLAY_DEPTH = ThreadLocal.withInitial(() -> new int[1]);

    private MachineBlockRenderType() {
    }

    public static boolean isOverlayPass() {
        return OVERLAY_DEPTH.get()[0] > 0;
    }

    public static void beginOverlayPass() {
        OVERLAY_DEPTH.get()[0]++;
    }

    public static void endOverlayPass() {
        int[] depth = OVERLAY_DEPTH.get();
        depth[0] = Math.max(0, depth[0] - 1);
    }
}
