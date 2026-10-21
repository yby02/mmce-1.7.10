package github.kasuminova.mmce.common.world;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import github.kasuminova.mmce.compat.BlockPos;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * 记录每个 tick 内发生方块变化的区域，供已成型的机器判断是否需要重新检查结构（减少无谓的结构检查）。
 * <p>
 * 1.7.10 移植：1.12.2 的 IWorldEventListener#notifyBlockUpdate 对应 1.7.10 的 IWorldAccess#markBlockForUpdate。
 * 1.7.10 中 TileEntity 同步数据包同样会走 markBlockForUpdate，因此 MMCE 自身的 TileEntity 同步在调用时会通过
 * {@link #suppress(Runnable)} 屏蔽记录，避免把“数据同步”误判为“结构变化”。
 */
public class MMWorldEventListener {

    public static final MMWorldEventListener INSTANCE = new MMWorldEventListener();

    private static final ThreadLocal<Boolean> SUPPRESSED = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private final Map<World, Long2ObjectMap<StructureBoundingBox>> worldChangedChunks = new HashMap<>();

    private MMWorldEventListener() {
    }

    /** 在不记录方块变化的情况下执行（用于 MMCE 自身的 TileEntity 数据同步）。 */
    public static void suppress(Runnable action) {
        boolean prev = SUPPRESSED.get();
        SUPPRESSED.set(Boolean.TRUE);
        try {
            action.run();
        } finally {
            SUPPRESSED.set(prev);
        }
    }

    @SubscribeEvent
    public void onWorldLoaded(WorldEvent.Load event) {
        World world = event.world;
        if (world.isRemote) {
            return;
        }
        world.addWorldAccess(new WorldAccessProxy(world));
        MachineComponentManager.INSTANCE.addWorld(world);
    }

    @SubscribeEvent
    public void onWorldUnloaded(WorldEvent.Unload event) {
        World world = event.world;
        if (world.isRemote) {
            return;
        }
        worldChangedChunks.remove(world);
        MachineComponentManager.INSTANCE.removeWorld(world);
    }

    @SubscribeEvent
    public void onServerTickStart(TickEvent.ServerTickEvent event) {
        if (event.side != Side.SERVER || event.phase != TickEvent.Phase.START) {
            return;
        }

        worldChangedChunks.values().forEach(Map::clear);
    }

    @SubscribeEvent
    public void onChunkUnload(final ChunkEvent.Unload event) {
        World world = event.world;
        if (world.isRemote) {
            return;
        }
        Chunk chunk = event.getChunk();
        int xStart = chunk.xPosition << 4;
        int zStart = chunk.zPosition << 4;

        StructureBoundingBox structureArea = new StructureBoundingBox(xStart, zStart, xStart + 15, zStart + 15);
        Long2ObjectMap<StructureBoundingBox> changedChunks = worldChangedChunks.get(world);
        if (changedChunks == null) {
            return;
        }
        changedChunks.put(ChunkCoordIntPair.chunkXZ2Int(chunk.xPosition, chunk.zPosition), structureArea);
    }

    public boolean isAreaChanged(@Nonnull final World worldIn,
                                 @Nonnull final BlockPos min,
                                 @Nonnull final BlockPos max) {
        int minChunkX = min.getX() >> 4;
        int maxChunkX = max.getX() >> 4;
        int minChunkZ = min.getZ() >> 4;
        int maxChunkZ = max.getZ() >> 4;

        StructureBoundingBox structureArea = new StructureBoundingBox(
            Math.min(min.getX(), max.getX()), Math.min(min.getY(), max.getY()), Math.min(min.getZ(), max.getZ()),
            Math.max(min.getX(), max.getX()), Math.max(min.getY(), max.getY()), Math.max(min.getZ(), max.getZ()));

        Long2ObjectMap<StructureBoundingBox> changedChunks = worldChangedChunks.get(worldIn);
        if (changedChunks == null) {
            return false;
        }
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                StructureBoundingBox changedArea = changedChunks.get(ChunkCoordIntPair.chunkXZ2Int(chunkX, chunkZ));
                if (changedArea != null && changedArea.intersectsWith(structureArea)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void onBlockChanged(World worldIn, int x, int y, int z) {
        if (SUPPRESSED.get()) {
            return;
        }
        Long2ObjectMap<StructureBoundingBox> changedChunks = worldChangedChunks.computeIfAbsent(worldIn, v -> new Long2ObjectOpenHashMap<>());
        long longChunkPos = ChunkCoordIntPair.chunkXZ2Int(x >> 4, z >> 4);
        StructureBoundingBox changedArea = changedChunks.get(longChunkPos);

        StructureBoundingBox at = new StructureBoundingBox(x, y, z, x, y, z);
        if (changedArea == null) {
            changedChunks.put(longChunkPos, at);
        } else {
            changedArea.expandTo(at);
        }
    }

    /** 1.7.10 的 IWorldAccess 回调不带世界参数，因此每个世界注册一个代理。 */
    private final class WorldAccessProxy extends MMWorldEventListenerDelegate {
        private final World world;

        private WorldAccessProxy(World world) {
            this.world = world;
        }

        @Override
        public void markBlockForUpdate(int x, int y, int z) {
            onBlockChanged(world, x, y, z);
        }
    }
}
