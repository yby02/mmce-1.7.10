package github.kasuminova.mmce.compat;

/**
 * 1.7.10 移植适配：复刻 1.12.2 net.minecraft.util.Rotation（绕 Y 轴旋转）。
 */
public enum Rotation {
    NONE,
    CLOCKWISE_90,
    CLOCKWISE_180,
    COUNTERCLOCKWISE_90;

    private static final Rotation[] VALUES = values();

    public Rotation add(Rotation rotation) {
        return VALUES[(ordinal() + rotation.ordinal()) % VALUES.length];
    }

    public EnumFacing rotate(EnumFacing facing) {
        if (facing.getAxis() == EnumFacing.Axis.Y) {
            return facing;
        }
        switch (this) {
            case CLOCKWISE_90: return facing.rotateY();
            case CLOCKWISE_180: return facing.getOpposite();
            case COUNTERCLOCKWISE_90: return facing.rotateYCCW();
            default: return facing;
        }
    }

    public int rotate(int rotation, int positionCount) {
        switch (this) {
            case CLOCKWISE_90: return (rotation + positionCount / 4) % positionCount;
            case CLOCKWISE_180: return (rotation + positionCount / 2) % positionCount;
            case COUNTERCLOCKWISE_90: return (rotation + positionCount * 3 / 4) % positionCount;
            default: return rotation;
        }
    }
}
