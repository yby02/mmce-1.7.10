package github.kasuminova.mmce.common.tile.base;

import appeng.api.implementations.IPowerChannelState;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.MachineSource;
import appeng.api.util.AECableType;
import appeng.api.util.DimensionalCoord;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import appeng.util.Platform;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.tiles.base.MachineComponentTile;
import hellfirepvp.modularmachinery.common.tiles.base.SelectiveUpdateTileEntity;
import hellfirepvp.modularmachinery.common.tiles.base.TileColorableMachineComponent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * ME 部件的公共基类：AE2 网络节点（{@link AENetworkProxy}，需要频道，空闲耗能 1 AE/t）。
 * <p>
 * 1.7.10 移植：按 GTNH 的 AE2 rv3 接口重写上游 github.kasuminova.mmce.common.tile.base.MEMachineComponent——
 * IGridHost 的方向参数为 ForgeDirection，操作来源为 {@link BaseActionSource}；网络节点在下一个服务端同步任务中就绪
 * （与上游一样，不在 validate 中直接调用 onReady）。只在装有 AE2 时注册。
 */
public abstract class MEMachineComponent extends TileColorableMachineComponent
    implements SelectiveUpdateTileEntity, MachineComponentTile, IActionHost, IGridProxyable, IPowerChannelState {

    protected final AENetworkProxy   proxy;
    protected final BaseActionSource source;

    public MEMachineComponent() {
        this.proxy = new AENetworkProxy(this, "aeProxy", getVisualItemStack(), true);
        this.proxy.setIdlePowerUsage(1.0D);
        this.proxy.setFlags(GridFlags.REQUIRE_CHANNEL);
        this.source = new MachineSource(this);
    }

    public abstract ItemStack getVisualItemStack();

    public void setOwner(EntityPlayer player) {
        proxy.setOwner(player);
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        if (worldObj == null || !worldObj.isRemote) {
            try {
                proxy.readFromNBT(compound);
            } catch (IllegalStateException e) {
                // 已加入网络后不再读取网络数据（与上游相同）
                ModularMachinery.log.warn(e);
            }
        }
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        proxy.writeToNBT(compound);
    }

    protected void notifyNeighbors() {
        if (proxy.isActive()) {
            try {
                proxy.getTick().wakeDevice(proxy.getNode());
            } catch (GridAccessException ignored) {
            }
            Platform.notifyBlocksOfNeighbors(worldObj, xCoord, yCoord, zCoord);
        }
    }

    protected void alertDevice() {
        try {
            proxy.getTick().alertDevice(proxy.getNode());
        } catch (GridAccessException ignored) {
        }
    }

    // ---------------- AE2 ----------------

    @Override
    public void gridChanged() {
    }

    @Override
    public IGridNode getActionableNode() {
        return proxy.getNode();
    }

    @Override
    public AENetworkProxy getProxy() {
        return proxy;
    }

    @Override
    public DimensionalCoord getLocation() {
        return new DimensionalCoord(this);
    }

    @Override
    public IGridNode getGridNode(ForgeDirection dir) {
        return proxy.getNode();
    }

    @Override
    public AECableType getCableConnectionType(ForgeDirection dir) {
        return AECableType.SMART;
    }

    @Override
    public void securityBreak() {
        worldObj.func_147480_a(xCoord, yCoord, zCoord, true);
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        proxy.onChunkUnload();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        proxy.invalidate();
    }

    @Override
    public void validate() {
        super.validate();
        if (worldObj != null && !worldObj.isRemote) {
            ModularMachinery.EXECUTE_MANAGER.addSyncTask(proxy::onReady);
        }
    }

    @Override
    public boolean isPowered() {
        return proxy.isPowered();
    }

    @Override
    public boolean isActive() {
        return proxy.isActive();
    }
}
