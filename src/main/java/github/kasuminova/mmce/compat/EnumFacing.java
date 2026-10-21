package github.kasuminova.mmce.compat;

import net.minecraftforge.common.util.ForgeDirection;

import java.util.Iterator;
import java.util.Locale;
import java.util.Random;

/**
 * 1.7.10 移植适配：复刻 1.12.2 net.minecraft.util.EnumFacing 中 MMCE 用到的 API。
 * 索引与 1.7.10 的 side 编号、ForgeDirection 序号一致（D=0, U=1, N=2, S=3, W=4, E=5）。
 */
public enum EnumFacing implements IStringSerializable {
    DOWN(0, 1, -1, "down", AxisDirection.NEGATIVE, Axis.Y, 0, -1, 0),
    UP(1, 0, -1, "up", AxisDirection.POSITIVE, Axis.Y, 0, 1, 0),
    NORTH(2, 3, 2, "north", AxisDirection.NEGATIVE, Axis.Z, 0, 0, -1),
    SOUTH(3, 2, 0, "south", AxisDirection.POSITIVE, Axis.Z, 0, 0, 1),
    WEST(4, 5, 1, "west", AxisDirection.NEGATIVE, Axis.X, -1, 0, 0),
    EAST(5, 4, 3, "east", AxisDirection.POSITIVE, Axis.X, 1, 0, 0);

    public static final EnumFacing[] VALUES = values();
    public static final EnumFacing[] HORIZONTALS = {SOUTH, WEST, NORTH, EAST};

    private final int index;
    private final int opposite;
    private final int horizontalIndex;
    private final String name;
    private final AxisDirection axisDirection;
    private final Axis axis;
    private final int offsetX, offsetY, offsetZ;

    EnumFacing(int index, int opposite, int horizontalIndex, String name, AxisDirection axisDirection, Axis axis,
               int offsetX, int offsetY, int offsetZ) {
        this.index = index;
        this.opposite = opposite;
        this.horizontalIndex = horizontalIndex;
        this.name = name;
        this.axisDirection = axisDirection;
        this.axis = axis;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
    }

    public int getIndex() {
        return index;
    }

    public int getHorizontalIndex() {
        return horizontalIndex;
    }

    public AxisDirection getAxisDirection() {
        return axisDirection;
    }

    public EnumFacing getOpposite() {
        return VALUES[opposite];
    }

    public EnumFacing rotateAround(Axis axis) {
        switch (axis) {
            case X:
                return this == WEST || this == EAST ? this : rotateX();
            case Y:
                return this == UP || this == DOWN ? this : rotateY();
            case Z:
                return this == NORTH || this == SOUTH ? this : rotateZ();
            default:
                throw new IllegalStateException("Unable to get CW facing for axis " + axis);
        }
    }

    public EnumFacing rotateY() {
        switch (this) {
            case NORTH: return EAST;
            case EAST: return SOUTH;
            case SOUTH: return WEST;
            case WEST: return NORTH;
            default: throw new IllegalStateException("Unable to get Y-rotated facing of " + this);
        }
    }

    public EnumFacing rotateYCCW() {
        switch (this) {
            case NORTH: return WEST;
            case EAST: return NORTH;
            case SOUTH: return EAST;
            case WEST: return SOUTH;
            default: throw new IllegalStateException("Unable to get CCW facing of " + this);
        }
    }

    private EnumFacing rotateX() {
        switch (this) {
            case NORTH: return DOWN;
            case SOUTH: return UP;
            case UP: return NORTH;
            case DOWN: return SOUTH;
            default: throw new IllegalStateException("Unable to get X-rotated facing of " + this);
        }
    }

    private EnumFacing rotateZ() {
        switch (this) {
            case EAST: return DOWN;
            case WEST: return UP;
            case UP: return EAST;
            case DOWN: return WEST;
            default: throw new IllegalStateException("Unable to get Z-rotated facing of " + this);
        }
    }

    public int getXOffset() {
        return offsetX;
    }

    public int getYOffset() {
        return offsetY;
    }

    public int getZOffset() {
        return offsetZ;
    }

    public int getFrontOffsetX() {
        return offsetX;
    }

    public int getFrontOffsetY() {
        return offsetY;
    }

    public int getFrontOffsetZ() {
        return offsetZ;
    }

    public String getName2() {
        return name;
    }

    public Axis getAxis() {
        return axis;
    }

    public float getHorizontalAngle() {
        return (float) ((horizontalIndex & 3) * 90);
    }

    public BlockPos getDirectionVec() {
        return new BlockPos(offsetX, offsetY, offsetZ);
    }

    /** 转换为 1.7.10 的 ForgeDirection（序号一致）。 */
    public ForgeDirection toForgeDirection() {
        return ForgeDirection.getOrientation(index);
    }

    public static EnumFacing fromForgeDirection(ForgeDirection direction) {
        return direction == null || direction == ForgeDirection.UNKNOWN ? null : VALUES[direction.ordinal()];
    }

    /** 由 1.7.10 的 side 编号（0~5）获取方向；越界返回 null。 */
    public static EnumFacing fromSide(int side) {
        return side < 0 || side >= VALUES.length ? null : VALUES[side];
    }

    public static EnumFacing byName(String name) {
        if (name == null) {
            return null;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        for (EnumFacing facing : VALUES) {
            if (facing.name.equals(lower)) {
                return facing;
            }
        }
        return null;
    }

    public static EnumFacing byIndex(int index) {
        return VALUES[Math.abs(index % VALUES.length)];
    }

    public static EnumFacing getFront(int index) {
        return byIndex(index);
    }

    public static EnumFacing byHorizontalIndex(int horizontalIndex) {
        return HORIZONTALS[Math.abs(horizontalIndex % HORIZONTALS.length)];
    }

    public static EnumFacing getHorizontal(int horizontalIndex) {
        return byHorizontalIndex(horizontalIndex);
    }

    public static EnumFacing fromAngle(double angle) {
        return byHorizontalIndex((int) Math.floor(angle / 90.0D + 0.5D) & 3);
    }

    public static EnumFacing random(Random rand) {
        return VALUES[rand.nextInt(VALUES.length)];
    }

    public static EnumFacing getFacingFromAxis(AxisDirection axisDirection, Axis axis) {
        for (EnumFacing facing : VALUES) {
            if (facing.getAxisDirection() == axisDirection && facing.getAxis() == axis) {
                return facing;
            }
        }
        throw new IllegalArgumentException("No such direction: " + axisDirection + " " + axis);
    }

    /** 由实体朝向求水平面上的朝向（等价于 1.12 EntityLivingBase#getHorizontalFacing）。 */
    public static EnumFacing fromRotationYaw(float rotationYaw) {
        return byHorizontalIndex((int) Math.floor((double) (rotationYaw * 4.0F / 360.0F) + 0.5D) & 3);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public String getName() {
        return name;
    }

    public enum Axis implements IStringSerializable {
        X("x", Plane.HORIZONTAL),
        Y("y", Plane.VERTICAL),
        Z("z", Plane.HORIZONTAL);

        private final String name;
        private final Plane plane;

        Axis(String name, Plane plane) {
            this.name = name;
            this.plane = plane;
        }

        public boolean isVertical() {
            return plane == Plane.VERTICAL;
        }

        public boolean isHorizontal() {
            return plane == Plane.HORIZONTAL;
        }

        public Plane getPlane() {
            return plane;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum AxisDirection {
        POSITIVE(1, "Towards positive"),
        NEGATIVE(-1, "Towards negative");

        private final int offset;
        private final String description;

        AxisDirection(int offset, String description) {
            this.offset = offset;
            this.description = description;
        }

        public int getOffset() {
            return offset;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    public enum Plane implements Iterable<EnumFacing> {
        HORIZONTAL,
        VERTICAL;

        public EnumFacing[] facings() {
            return this == HORIZONTAL ? new EnumFacing[]{NORTH, EAST, SOUTH, WEST} : new EnumFacing[]{UP, DOWN};
        }

        public EnumFacing random(Random rand) {
            EnumFacing[] facings = facings();
            return facings[rand.nextInt(facings.length)];
        }

        public boolean apply(EnumFacing facing) {
            return facing != null && facing.getAxis().getPlane() == this;
        }

        @Override
        public Iterator<EnumFacing> iterator() {
            return java.util.Arrays.asList(facings()).iterator();
        }
    }
}
