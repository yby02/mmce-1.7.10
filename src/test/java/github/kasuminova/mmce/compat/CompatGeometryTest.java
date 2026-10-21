package github.kasuminova.mmce.compat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * compat 层的 BlockPos / EnumFacing / Rotation 必须与 1.12.2 原版行为一致，否则结构旋转会错位。
 */
class CompatGeometryTest {

    @Test
    void facingIndicesMatchForgeDirection() {
        // 1.7.10 的 side 编号：0 下 1 上 2 北 3 南 4 西 5 东
        assertEquals(0, EnumFacing.DOWN.getIndex());
        assertEquals(1, EnumFacing.UP.getIndex());
        assertEquals(2, EnumFacing.NORTH.getIndex());
        assertEquals(3, EnumFacing.SOUTH.getIndex());
        assertEquals(4, EnumFacing.WEST.getIndex());
        assertEquals(5, EnumFacing.EAST.getIndex());
        for (EnumFacing facing : EnumFacing.VALUES) {
            assertEquals(facing, EnumFacing.fromForgeDirection(facing.toForgeDirection()));
            assertEquals(facing.getIndex(), facing.toForgeDirection().ordinal());
            assertEquals(facing, facing.getOpposite().getOpposite());
        }
    }

    @Test
    void horizontalRotationCycles() {
        assertEquals(EnumFacing.EAST, EnumFacing.NORTH.rotateY());
        assertEquals(EnumFacing.SOUTH, EnumFacing.EAST.rotateY());
        assertEquals(EnumFacing.WEST, EnumFacing.SOUTH.rotateY());
        assertEquals(EnumFacing.NORTH, EnumFacing.WEST.rotateY());
        for (EnumFacing facing : EnumFacing.HORIZONTALS) {
            assertEquals(facing, facing.rotateY().rotateYCCW());
            assertEquals(facing.getOpposite(), Rotation.CLOCKWISE_180.rotate(facing));
            assertEquals(facing, Rotation.COUNTERCLOCKWISE_90.rotate(Rotation.CLOCKWISE_90.rotate(facing)));
        }
        assertEquals(EnumFacing.UP, Rotation.CLOCKWISE_90.rotate(EnumFacing.UP));
    }

    @Test
    void rotationAddWrapsAround() {
        assertEquals(Rotation.CLOCKWISE_180, Rotation.CLOCKWISE_90.add(Rotation.CLOCKWISE_90));
        assertEquals(Rotation.NONE, Rotation.CLOCKWISE_90.add(Rotation.COUNTERCLOCKWISE_90));
        // 180° + (270° + 180°) = 630° = 270°
        assertEquals(Rotation.COUNTERCLOCKWISE_90, Rotation.CLOCKWISE_180.add(Rotation.COUNTERCLOCKWISE_90.add(Rotation.CLOCKWISE_180)));
    }

    @Test
    void blockPosRotationMatchesFacingRotation() {
        // 1.12.2 BlockPos#rotate(CLOCKWISE_90) = (-z, y, x)
        assertEquals(new BlockPos(-3, 2, 1), new BlockPos(1, 2, 3).rotate(Rotation.CLOCKWISE_90));
        assertEquals(new BlockPos(-1, 2, -3), new BlockPos(1, 2, 3).rotate(Rotation.CLOCKWISE_180));
        assertEquals(new BlockPos(3, 2, -1), new BlockPos(1, 2, 3).rotate(Rotation.COUNTERCLOCKWISE_90));
        // 位置旋转与朝向旋转一致：朝向 f 的单位偏移旋转后等于旋转后朝向的单位偏移
        for (Rotation rotation : Rotation.values()) {
            for (EnumFacing facing : EnumFacing.HORIZONTALS) {
                BlockPos rotatedOffset = BlockPos.ORIGIN.offset(facing).rotate(rotation);
                assertEquals(BlockPos.ORIGIN.offset(rotation.rotate(facing)), rotatedOffset, rotation + " " + facing);
            }
        }
    }

    @Test
    void blockPosArithmetic() {
        BlockPos pos = new BlockPos(10, 64, -5);
        assertEquals(new BlockPos(10, 65, -5), pos.up());
        assertEquals(new BlockPos(10, 64, -6), pos.north());
        assertEquals(new BlockPos(11, 64, -5), pos.east());
        assertEquals(new BlockPos(13, 62, -1), pos.add(3, -2, 4));
        assertEquals(new BlockPos(7, 66, -9), pos.subtract(new BlockPos(3, -2, 4)));
        assertEquals(pos, BlockPos.fromLong(pos.toLong()));
        assertEquals(new BlockPos(-30000000, 0, 29999999), BlockPos.fromLong(new BlockPos(-30000000, 0, 29999999).toLong()));
    }
}
