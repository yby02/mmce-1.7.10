package github.kasuminova.mmce.compat;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * 1.7.10 移植适配：把 1.12.2 以 BlockPos/IBlockState 为参数的世界访问 API 映射到 1.7.10 的 x/y/z + Block/meta API。
 */
public final class Worlds {

    public static final int FLAG_BLOCK_UPDATE = 1;
    public static final int FLAG_SEND_TO_CLIENT = 2;
    public static final int FLAG_DEFAULT = FLAG_BLOCK_UPDATE | FLAG_SEND_TO_CLIENT;

    private Worlds() {
    }

    public static IBlockState getBlockState(IBlockAccess world, BlockPos pos) {
        return IBlockState.fromWorld(world, pos);
    }

    public static Block getBlock(IBlockAccess world, BlockPos pos) {
        return world.getBlock(pos.getX(), pos.getY(), pos.getZ());
    }

    public static int getMeta(IBlockAccess world, BlockPos pos) {
        return world.getBlockMetadata(pos.getX(), pos.getY(), pos.getZ());
    }

    public static TileEntity getTileEntity(IBlockAccess world, BlockPos pos) {
        return world.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
    }

    /** 对应 1.12.2 World#isBlockLoaded：只检查区块是否已加载，不会触发加载。 */
    public static boolean isBlockLoaded(World world, BlockPos pos) {
        return world.blockExists(pos.getX(), pos.getY(), pos.getZ());
    }

    public static boolean setBlockState(World world, BlockPos pos, IBlockState state, int flags) {
        return world.setBlock(pos.getX(), pos.getY(), pos.getZ(), state.getBlock(), state.getMetadata(), flags);
    }

    public static boolean setBlockState(World world, BlockPos pos, IBlockState state) {
        return setBlockState(world, pos, state, FLAG_DEFAULT);
    }

    public static boolean setBlockToAir(World world, BlockPos pos) {
        return world.setBlockToAir(pos.getX(), pos.getY(), pos.getZ());
    }

    public static boolean isAirBlock(IBlockAccess world, BlockPos pos) {
        return world.isAirBlock(pos.getX(), pos.getY(), pos.getZ());
    }

    /** 对应 1.12.2 World#notifyBlockUpdate：把方块/TE 变化同步给客户端。 */
    public static void notifyBlockUpdate(World world, BlockPos pos) {
        world.markBlockForUpdate(pos.getX(), pos.getY(), pos.getZ());
    }

    /** 对应 1.12.2 World#notifyNeighborsOfStateChange。 */
    public static void notifyNeighbors(World world, BlockPos pos, Block block) {
        world.notifyBlocksOfNeighborChange(pos.getX(), pos.getY(), pos.getZ(), block == null ? Blocks.air : block);
    }

    public static void markChunkDirty(World world, BlockPos pos, TileEntity te) {
        world.markTileEntityChunkModified(pos.getX(), pos.getY(), pos.getZ(), te);
    }

    public static void markBlockRangeForRenderUpdate(World world, BlockPos pos) {
        world.markBlockRangeForRenderUpdate(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
    }

    public static void markBlockRangeForRenderUpdate(World world, BlockPos from, BlockPos to) {
        world.markBlockRangeForRenderUpdate(from.getX(), from.getY(), from.getZ(), to.getX(), to.getY(), to.getZ());
    }

    /** 对应 1.12.2 World#getStrongPower(BlockPos)：周围方块提供的最大强充能等级。 */
    public static int getStrongPower(World world, BlockPos pos) {
        return world.getBlockPowerInput(pos.getX(), pos.getY(), pos.getZ());
    }

    /** 对应 1.12.2 World#isAreaLoaded(StructureBoundingBox)。 */
    public static boolean isAreaLoaded(World world, net.minecraft.world.gen.structure.StructureBoundingBox box) {
        return world.checkChunksExist(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

    public static boolean isAreaLoaded(World world, BlockPos from, BlockPos to) {
        return world.checkChunksExist(from.getX(), from.getY(), from.getZ(), to.getX(), to.getY(), to.getZ());
    }

    /** 对应 1.12.2 World#getRedstonePowerFromNeighbors。 */
    public static int getRedstonePowerFromNeighbors(World world, BlockPos pos) {
        return world.getStrongestIndirectPower(pos.getX(), pos.getY(), pos.getZ());
    }

    public static boolean isBlockPowered(World world, BlockPos pos) {
        return world.isBlockIndirectlyGettingPowered(pos.getX(), pos.getY(), pos.getZ());
    }

    public static BlockPos getPos(TileEntity te) {
        return new BlockPos(te.xCoord, te.yCoord, te.zCoord);
    }

    public static int getDimension(World world) {
        return world.provider.dimensionId;
    }
}
