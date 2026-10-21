/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.tiles.base;

import github.kasuminova.mmce.common.world.MMWorldEventListener;
import github.kasuminova.mmce.compat.BlockPos;
import github.kasuminova.mmce.compat.ITickable;
import hellfirepvp.modularmachinery.ModularMachinery;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: TileEntitySynchronized
 * Created by HellFirePvP
 * Date: 28.06.2017 / 17:15
 * <p>
 * 1.7.10 移植：
 * - readFromNBT/writeToNBT 在 1.7.10 中返回 void；描述包使用 getDescriptionPacket。
 * - 提供 1.12.2 风格的 getPos()/getWorld()，供上游代码直接沿用。
 * - updateEntity 转发到 {@link ITickable#update()}，非 ITickable 的 TileEntity 不参与 tick。
 */
public class TileEntitySynchronized extends TileEntity {
    protected boolean requireUpdateComparatorLevel = false;

    private boolean inUpdateTask = false;
    private boolean inMarkTask   = false;

    private long lastUpdateTick = 0;

    private BlockPos cachedPos = null;

    @Override
    public final void readFromNBT(@Nonnull NBTTagCompound compound) {
        super.readFromNBT(compound);
        cachedPos = null;
        readCustomNBT(compound);
    }

    public void readCustomNBT(NBTTagCompound compound) {
    }

    public void readNetNBT(NBTTagCompound compound) {
    }

    @Override
    public final void writeToNBT(@Nonnull NBTTagCompound compound) {
        super.writeToNBT(compound);
        writeCustomNBT(compound);
    }

    public void writeCustomNBT(NBTTagCompound compound) {
    }

    public void writeNetNBT(NBTTagCompound compound) {
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound compound = new NBTTagCompound();
        super.writeToNBT(compound);
        writeCustomNBT(compound);
        writeNetNBT(compound);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 255, compound);
    }

    @Override
    public final void onDataPacket(@Nonnull NetworkManager manager, @Nonnull S35PacketUpdateTileEntity packet) {
        NBTTagCompound compound = packet.func_148857_g();
        super.readFromNBT(compound);
        readCustomNBT(compound);
        readNetNBT(compound);
    }

    @Override
    public boolean canUpdate() {
        return this instanceof ITickable;
    }

    @Override
    public void updateEntity() {
        if (this instanceof ITickable) {
            ((ITickable) this).update();
        }
    }

    /** 1.12.2 风格坐标访问。 */
    public BlockPos getPos() {
        BlockPos pos = cachedPos;
        if (pos == null || pos.getX() != xCoord || pos.getY() != yCoord || pos.getZ() != zCoord) {
            pos = cachedPos = new BlockPos(xCoord, yCoord, zCoord);
        }
        return pos;
    }

    /** 1.12.2 风格世界访问。 */
    public World getWorld() {
        return worldObj;
    }

    public void markNoUpdate() {
        World world = getWorld();
        if (world == null) {
            return;
        }

        if (requireUpdateComparatorLevel) {
            world.func_147453_f(xCoord, yCoord, zCoord, this.getBlockType());
        }
        markChunkDirty();

        inMarkTask = false;
        lastUpdateTick = world.getTotalWorldTime();
    }

    public void markForUpdate() {
        markNoUpdate();
        notifyUpdate();
    }

    public void notifyUpdate() {
        World world = getWorld();
        if (world == null) {
            return;
        }
        // 数据同步不是结构变化，不计入 MMWorldEventListener 的方块变化记录
        MMWorldEventListener.suppress(() -> world.markBlockForUpdate(xCoord, yCoord, zCoord));
        inUpdateTask = false;
    }

    public void markNoUpdateSync() {
        if (inMarkTask) {
            return;
        }
        ModularMachinery.EXECUTE_MANAGER.addTEMarkNoUpdateTask(this);
        inMarkTask = true;
    }

    public void markChunkDirty() {
        World world = worldObj;
        if (world == null) {
            return;
        }
        // Prevent extra chunk loading and checks.
        if (world.getChunkProvider().chunkExists(xCoord >> 4, zCoord >> 4)) {
            world.getChunkFromChunkCoords(xCoord >> 4, zCoord >> 4).setChunkModified();
        }
    }

    /**
     * <p>markForUpdate 的同步实现，防止意料之外的 CME。</p>
     * <p>*** 只能保证 MMCE 自身对世界的线程安全 ***</p>
     */
    public void markForUpdateSync() {
        if (inUpdateTask) {
            return;
        }
        ModularMachinery.EXECUTE_MANAGER.addTEUpdateTask(this);
        inUpdateTask = true;
        inMarkTask = true;
    }

    public boolean isInUpdateTask() {
        return inUpdateTask;
    }

    public long getLastUpdateTick() {
        return lastUpdateTick;
    }
}
