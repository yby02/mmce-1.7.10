package github.kasuminova.mmce.compat;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.IBlockAccess;

/**
 * 1.7.10 移植适配：1.12.2 的 IBlockState 在 1.7.10 中对应“方块 + meta(0~15)”。
 * 保留类名以便上游代码中的类型声明无需改动；方块属性（PropertyEnum 等）由各方块自行编码到 meta。
 * 实例不可变，可用作哈希键。
 */
public final class IBlockState {

    public static final IBlockState AIR = new IBlockState(Blocks.air, 0);

    private final Block block;
    private final int meta;

    private IBlockState(Block block, int meta) {
        this.block = block;
        this.meta = meta & 15;
    }

    public static IBlockState of(Block block, int meta) {
        if (block == null || block == Blocks.air) {
            return AIR;
        }
        return new IBlockState(block, meta);
    }

    /** 默认状态（meta 0），对应 1.12.2 的 Block#getDefaultState。 */
    public static IBlockState of(Block block) {
        return of(block, 0);
    }

    public static IBlockState fromWorld(IBlockAccess world, int x, int y, int z) {
        return of(world.getBlock(x, y, z), world.getBlockMetadata(x, y, z));
    }

    public static IBlockState fromWorld(IBlockAccess world, BlockPos pos) {
        return fromWorld(world, pos.getX(), pos.getY(), pos.getZ());
    }

    public Block getBlock() {
        return block;
    }

    public int getMetadata() {
        return meta;
    }

    public Material getMaterial() {
        return block.getMaterial();
    }

    public IBlockState withMeta(int newMeta) {
        return newMeta == meta ? this : of(block, newMeta);
    }

    /** 绕 Y 轴旋转。仅对已知朝向编码的方块生效，见 {@link BlockStateRotation}。 */
    public IBlockState withRotation(Rotation rotation) {
        return BlockStateRotation.rotate(this, rotation);
    }

    public String getRegistryNameString() {
        GameRegistry.UniqueIdentifier id = GameRegistry.findUniqueIdentifierFor(block);
        return id == null ? String.valueOf(Block.blockRegistry.getNameForObject(block)) : id.modId + ":" + id.name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof IBlockState)) {
            return false;
        }
        IBlockState other = (IBlockState) obj;
        return block == other.block && meta == other.meta;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(block) * 31 + meta;
    }

    @Override
    public String toString() {
        return getRegistryNameString() + "@" + meta;
    }
}
