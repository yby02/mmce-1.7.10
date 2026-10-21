package hellfirepvp.modularmachinery.common.util;

import github.kasuminova.mmce.compat.IBlockState;
import net.minecraft.block.Block;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.material.Material;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 描述符按状态内容（方块 + meta）匹配：1.7.10 的 IBlockState 不是唯一实例，从世界读出的状态总是新对象。
 */
class IBlockStateDescriptorTest {

    private static Block stairs;
    private static Block plain;

    @BeforeAll
    static void createBlocks() {
        Block model = new Block(Material.rock) {
        };
        stairs = new BlockStairs(model, 0) {
        };
        plain = new Block(Material.iron) {
        };
    }

    @Test
    void rotatedDescriptorAcceptsEqualStateFromWorld() {
        IBlockStateDescriptor eastStairs = new IBlockStateDescriptor(IBlockState.of(stairs, 0));
        AtomicBoolean rotated = new AtomicBoolean(false);
        IBlockStateDescriptor northStairs = eastStairs.copyRotateYCCW(rotated);
        assertTrue(rotated.get());
        // 楼梯 0 东 -> 逆时针 90° -> 3 北；新建的状态对象必须同样被接受
        assertTrue(northStairs.accepts(IBlockState.of(stairs, 3)));
        assertFalse(northStairs.accepts(IBlockState.of(stairs, 0)));
    }

    @Test
    void copiedMultiStateDescriptorAcceptsEqualStates() {
        IBlockStateDescriptor two = new IBlockStateDescriptor(Arrays.asList(IBlockState.of(plain, 1), IBlockState.of(plain, 2)), null);
        IBlockStateDescriptor copy = two.copy();
        assertTrue(copy.accepts(IBlockState.of(plain, 2)));
        assertFalse(copy.accepts(IBlockState.of(plain, 3)));
    }

    @Test
    void wildcardWithSampleAcceptsAnyMetaAndShowsSample() {
        IBlockStateDescriptor wildcard = IBlockStateDescriptor.wildcardWithSample(IBlockState.of(plain, 2));
        assertTrue(wildcard.isWildcard());
        assertTrue(wildcard.accepts(IBlockState.of(plain, 7)));
        assertTrue(wildcard.getApplicable().contains(IBlockState.of(plain, 2)));
    }
}
