package github.kasuminova.mmce.compat;

import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * 1.7.10 移植适配：复刻 1.12.2 net.minecraft.util.math.BlockPos（不可变坐标值对象）及 MutableBlockPos。
 * equals/hashCode/compareTo/toLong 的算法与 1.12.2 保持一致，便于直接沿用上游的集合与序列化逻辑。
 */
public class BlockPos implements Comparable<BlockPos> {

    public static final BlockPos ORIGIN = new BlockPos(0, 0, 0);

    private static final int NUM_X_BITS = 26;
    private static final int NUM_Z_BITS = NUM_X_BITS;
    private static final int NUM_Y_BITS = 64 - NUM_X_BITS - NUM_Z_BITS;
    private static final int Y_SHIFT = NUM_Z_BITS;
    private static final int X_SHIFT = Y_SHIFT + NUM_Y_BITS;
    private static final long X_MASK = (1L << NUM_X_BITS) - 1L;
    private static final long Y_MASK = (1L << NUM_Y_BITS) - 1L;
    private static final long Z_MASK = (1L << NUM_Z_BITS) - 1L;

    protected int x;
    protected int y;
    protected int z;

    public BlockPos(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public BlockPos(double x, double y, double z) {
        this(MathHelper.floor_double(x), MathHelper.floor_double(y), MathHelper.floor_double(z));
    }

    public BlockPos(Entity entity) {
        this(entity.posX, entity.posY, entity.posZ);
    }

    public BlockPos(BlockPos source) {
        this(source.getX(), source.getY(), source.getZ());
    }

    public BlockPos(ChunkCoordinates coordinates) {
        this(coordinates.posX, coordinates.posY, coordinates.posZ);
    }

    public static BlockPos of(TileEntity te) {
        return new BlockPos(te.xCoord, te.yCoord, te.zCoord);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public BlockPos add(double x, double y, double z) {
        return x == 0.0D && y == 0.0D && z == 0.0D ? this : new BlockPos(getX() + x, getY() + y, getZ() + z);
    }

    public BlockPos add(int x, int y, int z) {
        return x == 0 && y == 0 && z == 0 ? this : new BlockPos(getX() + x, getY() + y, getZ() + z);
    }

    public BlockPos add(BlockPos vec) {
        return add(vec.getX(), vec.getY(), vec.getZ());
    }

    public BlockPos subtract(BlockPos vec) {
        return add(-vec.getX(), -vec.getY(), -vec.getZ());
    }

    public BlockPos up() {
        return up(1);
    }

    public BlockPos up(int n) {
        return offset(EnumFacing.UP, n);
    }

    public BlockPos down() {
        return down(1);
    }

    public BlockPos down(int n) {
        return offset(EnumFacing.DOWN, n);
    }

    public BlockPos north() {
        return north(1);
    }

    public BlockPos north(int n) {
        return offset(EnumFacing.NORTH, n);
    }

    public BlockPos south() {
        return south(1);
    }

    public BlockPos south(int n) {
        return offset(EnumFacing.SOUTH, n);
    }

    public BlockPos west() {
        return west(1);
    }

    public BlockPos west(int n) {
        return offset(EnumFacing.WEST, n);
    }

    public BlockPos east() {
        return east(1);
    }

    public BlockPos east(int n) {
        return offset(EnumFacing.EAST, n);
    }

    public BlockPos offset(EnumFacing facing) {
        return offset(facing, 1);
    }

    public BlockPos offset(EnumFacing facing, int n) {
        return n == 0 ? this : new BlockPos(getX() + facing.getXOffset() * n, getY() + facing.getYOffset() * n, getZ() + facing.getZOffset() * n);
    }

    public BlockPos rotate(Rotation rotation) {
        switch (rotation) {
            case CLOCKWISE_90:
                return new BlockPos(-getZ(), getY(), getX());
            case CLOCKWISE_180:
                return new BlockPos(-getX(), getY(), -getZ());
            case COUNTERCLOCKWISE_90:
                return new BlockPos(getZ(), getY(), -getX());
            case NONE:
            default:
                return this;
        }
    }

    public BlockPos crossProduct(BlockPos vec) {
        return new BlockPos(getY() * vec.getZ() - getZ() * vec.getY(), getZ() * vec.getX() - getX() * vec.getZ(), getX() * vec.getY() - getY() * vec.getX());
    }

    public double getDistance(int xIn, int yIn, int zIn) {
        double dx = getX() - xIn;
        double dy = getY() - yIn;
        double dz = getZ() - zIn;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public double distanceSq(double toX, double toY, double toZ) {
        double dx = getX() - toX;
        double dy = getY() - toY;
        double dz = getZ() - toZ;
        return dx * dx + dy * dy + dz * dz;
    }

    public double distanceSq(BlockPos to) {
        return distanceSq(to.getX(), to.getY(), to.getZ());
    }

    public double distanceSqToCenter(double xIn, double yIn, double zIn) {
        double dx = getX() + 0.5D - xIn;
        double dy = getY() + 0.5D - yIn;
        double dz = getZ() + 0.5D - zIn;
        return dx * dx + dy * dy + dz * dz;
    }

    public long toLong() {
        return ((long) getX() & X_MASK) << X_SHIFT | ((long) getY() & Y_MASK) << Y_SHIFT | ((long) getZ() & Z_MASK);
    }

    public static BlockPos fromLong(long serialized) {
        int i = (int) (serialized << 64 - X_SHIFT - NUM_X_BITS >> 64 - NUM_X_BITS);
        int j = (int) (serialized << 64 - Y_SHIFT - NUM_Y_BITS >> 64 - NUM_Y_BITS);
        int k = (int) (serialized << 64 - NUM_Z_BITS >> 64 - NUM_Z_BITS);
        return new BlockPos(i, j, k);
    }

    public BlockPos toImmutable() {
        return this;
    }

    public ChunkCoordinates toChunkCoordinates() {
        return new ChunkCoordinates(getX(), getY(), getZ());
    }

    public static Iterable<BlockPos> getAllInBox(BlockPos from, BlockPos to) {
        return getAllInBox(Math.min(from.getX(), to.getX()), Math.min(from.getY(), to.getY()), Math.min(from.getZ(), to.getZ()),
            Math.max(from.getX(), to.getX()), Math.max(from.getY(), to.getY()), Math.max(from.getZ(), to.getZ()));
    }

    public static Iterable<BlockPos> getAllInBox(final int x1, final int y1, final int z1, final int x2, final int y2, final int z2) {
        return () -> new Iterator<BlockPos>() {
            private boolean first = true;
            private int lastX, lastY, lastZ;

            @Override
            public boolean hasNext() {
                return first || !(lastX == x2 && lastY == y2 && lastZ == z2);
            }

            @Override
            public BlockPos next() {
                if (first) {
                    first = false;
                    lastX = x1;
                    lastY = y1;
                    lastZ = z1;
                    return new BlockPos(x1, y1, z1);
                }
                if (lastX == x2 && lastY == y2 && lastZ == z2) {
                    throw new NoSuchElementException();
                }
                if (lastX < x2) {
                    ++lastX;
                } else if (lastY < y2) {
                    lastX = x1;
                    ++lastY;
                } else if (lastZ < z2) {
                    lastX = x1;
                    lastY = y1;
                    ++lastZ;
                }
                return new BlockPos(lastX, lastY, lastZ);
            }
        };
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BlockPos)) {
            return false;
        }
        BlockPos pos = (BlockPos) other;
        return getX() == pos.getX() && getY() == pos.getY() && getZ() == pos.getZ();
    }

    @Override
    public int hashCode() {
        return (getY() + getZ() * 31) * 31 + getX();
    }

    @Override
    public int compareTo(BlockPos other) {
        if (getY() == other.getY()) {
            return getZ() == other.getZ() ? getX() - other.getX() : getZ() - other.getZ();
        }
        return getY() - other.getY();
    }

    @Override
    public String toString() {
        return "BlockPos{x=" + getX() + ", y=" + getY() + ", z=" + getZ() + "}";
    }

    /**
     * 可变版本，对应 1.12.2 BlockPos.MutableBlockPos。注意：放入哈希集合前须 toImmutable()。
     */
    public static class MutableBlockPos extends BlockPos {

        public MutableBlockPos() {
            this(0, 0, 0);
        }

        public MutableBlockPos(BlockPos pos) {
            this(pos.getX(), pos.getY(), pos.getZ());
        }

        public MutableBlockPos(int x, int y, int z) {
            super(x, y, z);
        }

        @Override
        public BlockPos add(double x, double y, double z) {
            return super.add(x, y, z).toImmutable();
        }

        @Override
        public BlockPos add(int x, int y, int z) {
            return new BlockPos(getX() + x, getY() + y, getZ() + z);
        }

        @Override
        public BlockPos offset(EnumFacing facing, int n) {
            return new BlockPos(getX() + facing.getXOffset() * n, getY() + facing.getYOffset() * n, getZ() + facing.getZOffset() * n);
        }

        @Override
        public BlockPos rotate(Rotation rotation) {
            return new BlockPos(this).rotate(rotation);
        }

        public MutableBlockPos setPos(int xIn, int yIn, int zIn) {
            this.x = xIn;
            this.y = yIn;
            this.z = zIn;
            return this;
        }

        public MutableBlockPos setPos(double xIn, double yIn, double zIn) {
            return setPos(MathHelper.floor_double(xIn), MathHelper.floor_double(yIn), MathHelper.floor_double(zIn));
        }

        public MutableBlockPos setPos(Entity entity) {
            return setPos(entity.posX, entity.posY, entity.posZ);
        }

        public MutableBlockPos setPos(BlockPos vec) {
            return setPos(vec.getX(), vec.getY(), vec.getZ());
        }

        public MutableBlockPos move(EnumFacing facing) {
            return move(facing, 1);
        }

        public MutableBlockPos move(EnumFacing facing, int n) {
            return setPos(x + facing.getXOffset() * n, y + facing.getYOffset() * n, z + facing.getZOffset() * n);
        }

        public void setY(int yIn) {
            this.y = yIn;
        }

        @Override
        public BlockPos toImmutable() {
            return new BlockPos(this);
        }
    }
}
