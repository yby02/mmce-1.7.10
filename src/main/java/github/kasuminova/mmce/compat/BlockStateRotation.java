package github.kasuminova.mmce.compat;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAnvil;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockEnderChest;
import net.minecraft.block.BlockFurnace;
import net.minecraft.block.BlockHopper;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.BlockQuartz;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.BlockStairs;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * 1.7.10 移植适配：1.12.2 中结构旋转依赖 IBlockState#withRotation，1.7.10 没有纯数据的旋转 API。
 * 这里按方块类型维护 meta 旋转规则（绕 Y 轴顺时针 90°），覆盖原版常见的有朝向方块；
 * 模组方块可通过 {@link #register(Block, MetaRotator)} / {@link #register(Class, MetaRotator)} 补充。
 * 未登记的方块视为与朝向无关（旋转后 meta 不变）——这是相对 1.12.2 的已知限制，记录于 MIGRATION_PLAN 4.3。
 */
public final class BlockStateRotation {

    /** 把 meta 顺时针旋转 90°。 */
    @FunctionalInterface
    public interface MetaRotator {
        int rotateClockwise(int meta);
    }

    private static final Map<Block, MetaRotator> BY_BLOCK = new IdentityHashMap<>();
    private static final List<Map.Entry<Class<? extends Block>, MetaRotator>> BY_CLASS = new ArrayList<>();

    static {
        // 楼梯：低 2 位 0=东 1=西 2=南 3=北，bit2 为倒置
        register(BlockStairs.class, meta -> (meta & ~3) | facingToStairs(Rotation.CLOCKWISE_90.rotate(stairsToFacing(meta & 3))));
        // 原木、干草块等：bit2~3 为轴向（0=Y, 4=X, 8=Z）
        register(BlockRotatedPillar.class, meta -> {
            int axis = meta & 12;
            return axis == 4 ? (meta & ~12) | 8 : axis == 8 ? (meta & ~12) | 4 : meta;
        });
        // 石英柱：3 / 4 为两种水平轴向
        register(BlockQuartz.class, meta -> meta == 3 ? 4 : meta == 4 ? 3 : meta);
        // 整个 meta 即朝向索引（2~5）
        MetaRotator fullFacing = meta -> meta >= 2 && meta <= 5 ? Rotation.CLOCKWISE_90.rotate(EnumFacing.byIndex(meta)).getIndex() : meta;
        register(BlockFurnace.class, fullFacing);
        register(BlockChest.class, fullFacing);
        register(BlockEnderChest.class, fullFacing);
        register(BlockLadder.class, fullFacing);
        // 低 3 位为朝向索引，高位为状态位
        MetaRotator lowFacing = meta -> {
            int facing = meta & 7;
            return facing >= 2 && facing <= 5 ? (meta & ~7) | Rotation.CLOCKWISE_90.rotate(EnumFacing.byIndex(facing)).getIndex() : meta;
        };
        register(BlockDispenser.class, lowFacing);
        register(BlockPistonBase.class, lowFacing);
        register(BlockHopper.class, lowFacing);
        // 低 2 位为 Direction（0=南 1=西 2=北 3=东），顺时针 +1：南瓜、床、栅栏门、红石中继器/比较器、可可豆、绊线钩等
        MetaRotator direction = meta -> (meta & ~3) | ((meta + 1) & 3);
        register(BlockDirectional.class, direction);
        register(BlockAnvil.class, direction);
    }

    private BlockStateRotation() {
    }

    public static void register(Block block, MetaRotator rotator) {
        BY_BLOCK.put(block, rotator);
    }

    public static void register(Class<? extends Block> blockClass, MetaRotator rotator) {
        BY_CLASS.add(new java.util.AbstractMap.SimpleImmutableEntry<>(blockClass, rotator));
    }

    public static IBlockState rotate(IBlockState state, Rotation rotation) {
        if (rotation == Rotation.NONE || state == IBlockState.AIR) {
            return state;
        }
        MetaRotator rotator = find(state.getBlock());
        if (rotator == null) {
            return state;
        }
        int steps = rotation.ordinal(); // NONE=0, CW90=1, CW180=2, CCW90=3
        int meta = state.getMetadata();
        for (int i = 0; i < steps; i++) {
            meta = rotator.rotateClockwise(meta) & 15;
        }
        return state.withMeta(meta);
    }

    private static MetaRotator find(Block block) {
        MetaRotator rotator = BY_BLOCK.get(block);
        if (rotator != null) {
            return rotator;
        }
        for (Map.Entry<Class<? extends Block>, MetaRotator> entry : BY_CLASS) {
            if (entry.getKey().isInstance(block)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static EnumFacing stairsToFacing(int bits) {
        switch (bits) {
            case 0: return EnumFacing.EAST;
            case 1: return EnumFacing.WEST;
            case 2: return EnumFacing.SOUTH;
            default: return EnumFacing.NORTH;
        }
    }

    private static int facingToStairs(EnumFacing facing) {
        switch (facing) {
            case EAST: return 0;
            case WEST: return 1;
            case SOUTH: return 2;
            default: return 3;
        }
    }
}
