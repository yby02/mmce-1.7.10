package github.kasuminova.mmce.compat;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFurnace;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockPumpkin;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.material.Material;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 结构旋转时方块 meta 的旋转规则（替代 1.12.2 IBlockState#withRotation）。
 * 朝向编码取自 1.7.10 原版：楼梯 0 东 1 西 2 南 3 北（bit2 倒置），熔炉为朝向索引 2~5，南瓜等 0 南 1 西 2 北 3 东。
 */
class BlockStateRotationTest {

    private static Block stairs;
    private static Block furnace;
    private static Block pumpkin;
    private static Block log;
    private static Block plain;

    @BeforeAll
    static void createBlocks() {
        Block model = new Block(Material.rock) {
        };
        stairs = new BlockStairs(model, 0) {
        };
        furnace = new BlockFurnace(false) {
        };
        pumpkin = new BlockPumpkin(false) {
        };
        log = new BlockLog() {
        };
        plain = new Block(Material.iron) {
        };
    }

    private static int rotate(Block block, int meta, Rotation rotation) {
        return IBlockState.of(block, meta).withRotation(rotation).getMetadata();
    }

    @Test
    void stairsRotateClockwiseAndKeepUpsideDownBit() {
        assertEquals(0, rotate(stairs, 3, Rotation.CLOCKWISE_90));   // 北 -> 东
        assertEquals(2, rotate(stairs, 0, Rotation.CLOCKWISE_90));   // 东 -> 南
        assertEquals(1, rotate(stairs, 2, Rotation.CLOCKWISE_90));   // 南 -> 西
        assertEquals(3, rotate(stairs, 1, Rotation.CLOCKWISE_90));   // 西 -> 北
        assertEquals(4, rotate(stairs, 7, Rotation.CLOCKWISE_90));   // 倒置北 -> 倒置东
        assertEquals(1, rotate(stairs, 3, Rotation.COUNTERCLOCKWISE_90)); // 北 -> 西
        assertEquals(2, rotate(stairs, 3, Rotation.CLOCKWISE_180));  // 北 -> 南
    }

    @Test
    void furnaceFacingIndexRotates() {
        assertEquals(5, rotate(furnace, 2, Rotation.CLOCKWISE_90)); // 北 -> 东
        assertEquals(3, rotate(furnace, 5, Rotation.CLOCKWISE_90)); // 东 -> 南
        assertEquals(4, rotate(furnace, 3, Rotation.CLOCKWISE_90)); // 南 -> 西
        assertEquals(2, rotate(furnace, 4, Rotation.CLOCKWISE_90)); // 西 -> 北
    }

    @Test
    void directionalBlocksRotate() {
        assertEquals(1, rotate(pumpkin, 0, Rotation.CLOCKWISE_90)); // 南 -> 西
        assertEquals(0, rotate(pumpkin, 3, Rotation.CLOCKWISE_90)); // 东 -> 南
    }

    @Test
    void pillarAxisSwapsHorizontally() {
        assertEquals(8 | 1, rotate(log, 4 | 1, Rotation.CLOCKWISE_90)); // X 轴 -> Z 轴，保留木种
        assertEquals(4, rotate(log, 8, Rotation.COUNTERCLOCKWISE_90));
        assertEquals(2, rotate(log, 2, Rotation.CLOCKWISE_90));         // 竖直不变
    }

    @Test
    void unregisteredBlocksKeepMeta() {
        for (Rotation rotation : Rotation.values()) {
            assertEquals(5, rotate(plain, 5, rotation));
        }
    }

    @Test
    void fourQuarterTurnsAreIdentity() {
        for (int meta = 0; meta < 8; meta++) {
            int m = meta;
            for (int i = 0; i < 4; i++) {
                m = rotate(stairs, m, Rotation.CLOCKWISE_90);
            }
            assertEquals(meta, m);
        }
    }
}
