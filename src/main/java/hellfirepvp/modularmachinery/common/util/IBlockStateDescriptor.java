package hellfirepvp.modularmachinery.common.util;

import hellfirepvp.modularmachinery.common.block.BlockStatedMachineComponent;
import hellfirepvp.modularmachinery.common.block.BlockVariants;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import github.kasuminova.mmce.compat.IBlockState;
import github.kasuminova.mmce.compat.Rotation;
import net.minecraftforge.fluids.BlockFluidBase;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 结构中某个位置可接受的方块状态集合。
 * <p>
 * 1.7.10 移植：
 * - 1.12.2 中 "modid:block"（不写 @meta）会枚举 block.getBlockState().getValidStates()；1.7.10 无法在服务端可靠地枚举方块的
 *   合法 meta，因此改为“通配”描述符（{@link #isWildcard()}）：匹配该方块的任意 meta。
 *   展示用的样例状态：MMCE 自身方块（{@link BlockVariants}）使用其全部变体，其余方块使用 meta 0。
 * - 流体方块与 1.12.2 相同，只接受源方块（meta 0）。
 * - 1.12.2 的 IBlockState 是唯一实例，上游用按引用比较的 ReferenceArrayList；1.7.10 的 {@link IBlockState} 每次读取世界都新建，
 *   必须用按 equals 比较的列表，否则旋转后 meta 改变的方块（楼梯、原木等）与复制出的多状态描述符永远匹配不上。
 */
public class IBlockStateDescriptor {

    // TODO It may not be necessary to use.
    private static final ObjectOpenHashSet<IBlockStateDescriptor> POOL = new ObjectOpenHashSet<>();

    private final List<IBlockState> applicable;
    /** 非 null 时表示匹配该方块的任意 meta。 */
    private final Block wildcardBlock;

    public IBlockStateDescriptor(IBlockState state) {
        this.applicable = Collections.singletonList(state);
        this.wildcardBlock = null;
    }

    public IBlockStateDescriptor(Block block) {
        if (block instanceof BlockLiquid || block instanceof BlockFluidBase) {
            this.applicable = Collections.singletonList(IBlockState.of(block));
            this.wildcardBlock = null;
            return;
        }

        List<IBlockState> applicable = new ObjectArrayList<>();
        if (block instanceof BlockVariants) {
            for (IBlockState state : ((BlockVariants) block).getValidStates()) {
                if (!applicable.contains(state)) {
                    applicable.add(state);
                }
            }
        }
        this.applicable = applicable.isEmpty() ? Collections.singletonList(IBlockState.of(block)) : applicable;
        this.wildcardBlock = block;
    }

    protected IBlockStateDescriptor(List<IBlockState> applicable, Block wildcardBlock) {
        this.applicable = applicable;
        this.wildcardBlock = wildcardBlock;
    }

    public static IBlockStateDescriptor of(IBlockState state) {
        return new IBlockStateDescriptor(state).canonicalize();
    }

    public static IBlockStateDescriptor of(Block block) {
        return new IBlockStateDescriptor(block).canonicalize();
    }

    /** 接受该方块的任意 meta，预览 / 投影时以 sample 显示（例如朝北的控制器）。 */
    public static IBlockStateDescriptor wildcardWithSample(IBlockState sample) {
        return new IBlockStateDescriptor(Collections.singletonList(sample), sample.getBlock()).canonicalize();
    }

    public static void clearPool() {
        POOL.clear();
    }

    public boolean isWildcard() {
        return wildcardBlock != null;
    }

    /** 判断给定状态是否被本描述符接受。 */
    public boolean accepts(IBlockState state) {
        if (wildcardBlock != null) {
            return state.getBlock() == wildcardBlock;
        }
        return applicable.contains(state);
    }

    public IBlockStateDescriptor copy() {
        return applicable.size() == 1
            ? new IBlockStateDescriptor(Collections.singletonList(applicable.get(0)), wildcardBlock)
            : new IBlockStateDescriptor(new ObjectArrayList<>(applicable), wildcardBlock);
    }

    public IBlockStateDescriptor copyRotateYCCW(final AtomicBoolean rotated) {
        if (wildcardBlock != null) {
            // 通配描述符不关心 meta，旋转后保持不变
            return this;
        }
        List<IBlockState> applicable = new ObjectArrayList<>();
        for (IBlockState state : this.applicable) {
            IBlockState rotatedState = state.withRotation(Rotation.COUNTERCLOCKWISE_90);
            if (!state.equals(rotatedState)) {
                rotated.set(true);
            }
            applicable.add(rotatedState);
        }
        return new IBlockStateDescriptor(applicable, null).canonicalize();
    }

    public boolean hasTileEntity() {
        for (IBlockState state : applicable) {
            if (state.getBlock().hasTileEntity(state.getMetadata())) {
                return true;
            }
        }
        return false;
    }

    public boolean hasStatedMachineComponent() {
        for (final IBlockState state : applicable) {
            if (state.getBlock() instanceof BlockStatedMachineComponent) {
                return true;
            }
        }
        return false;
    }

    public List<IBlockState> getApplicable() {
        return applicable;
    }

    public IBlockStateDescriptor canonicalize() {
        synchronized (POOL) {
            return POOL.addOrGet(this);
        }
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof IBlockStateDescriptor)) {
            return false;
        }
        IBlockStateDescriptor other = (IBlockStateDescriptor) obj;
        return wildcardBlock == other.wildcardBlock && applicable.equals(other.applicable);
    }

    @Override
    public int hashCode() {
        return applicable.hashCode() * 31 + (wildcardBlock == null ? 0 : System.identityHashCode(wildcardBlock));
    }
}
