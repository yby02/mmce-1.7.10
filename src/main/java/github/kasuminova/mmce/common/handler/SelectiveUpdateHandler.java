package github.kasuminova.mmce.common.handler;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.container.ContainerBase;
import hellfirepvp.modularmachinery.common.tiles.base.SelectiveUpdateTileEntity;
import hellfirepvp.modularmachinery.common.tiles.base.TileEntitySynchronized;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import github.kasuminova.mmce.common.network.PktPerformanceReport;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/**
 * 选择性更新：流体仓、能量仓等 {@link SelectiveUpdateTileEntity} 的内容变化时只标记区块（不广播），
 * 由玩家右键它、以及打开它的 GUI 期间单独向该玩家发送方块实体更新，以减少网络流量。
 * <p>
 * 1.7.10 移植：取自上游 github.kasuminova.mmce.common.handler.EventHandler 的 onPlayerRightClickBlock / onPlayerTick 中的同步部分
 * （P5 气体实测时发现此前未移植，流体仓 / 能量仓 GUI 不显示实时内容），以及同一处的两项上游行为：
 * 玩家离开方块实体 6 格时关闭 GUI；打开控制器 GUI 期间每 15 tick 向该玩家发送性能报告（GUI 中的 Avg μs/t、搜索耗时与工作模式）。
 */
public class SelectiveUpdateHandler {

    public static final SelectiveUpdateHandler INSTANCE = new SelectiveUpdateHandler();

    private SelectiveUpdateHandler() {
    }

    @SubscribeEvent
    public void onPlayerRightClickBlock(PlayerInteractEvent event) {
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        World world = event.world;
        if (world == null || world.isRemote) {
            return;
        }
        TileEntity te = world.getTileEntity(event.x, event.y, event.z);
        if (te instanceof SelectiveUpdateTileEntity && te instanceof final TileEntitySynchronized teSync) {
            teSync.notifyUpdate();
        }
    }

    /** 与上游相同：玩家所在方块坐标与方块实体坐标的距离不小于 6 时视为离开。 */
    private static boolean outOfRange(EntityPlayerMP player, TileEntity te) {
        double dx = te.xCoord - MathHelper.floor_double(player.posX);
        double dy = te.yCoord - MathHelper.floor_double(player.posY);
        double dz = te.zCoord - MathHelper.floor_double(player.posZ);
        return Math.sqrt(dx * dx + dy * dy + dz * dz) >= 6.0D;
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || event.side != Side.SERVER) {
            return;
        }
        if (!(event.player instanceof EntityPlayerMP playerMP)) {
            return;
        }
        Container container = playerMP.openContainer;
        if (!(container instanceof ContainerBase<?> base)) {
            return;
        }
        TileEntity te = base.getOwner();
        if (!(te instanceof SelectiveUpdateTileEntity) || !(te instanceof final TileEntitySynchronized teSync)) {
            return;
        }
        if (outOfRange(playerMP, te)) {
            playerMP.closeScreen();
            return;
        }
        ModularMachinery.EXECUTE_MANAGER.addSyncTask(() -> {
            // 内容在上一 tick 内变化过（markNoUpdate 记录的时间）才发送
            if (teSync.getLastUpdateTick() + 1 >= playerMP.worldObj.getTotalWorldTime()) {
                playerMP.playerNetServerHandler.sendPacket(teSync.getDescriptionPacket());
            }
            if (te instanceof final TileMultiblockMachineController ctrl && playerMP.worldObj.getWorldTime() % 15 == 0) {
                ModularMachinery.NET_CHANNEL.sendTo(new PktPerformanceReport(ctrl.usedTimeAvg(), ctrl.recipeSearchUsedTimeAvg(), ctrl.getWorkMode()), playerMP);
            }
        });
    }
}
