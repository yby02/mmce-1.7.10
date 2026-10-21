package github.kasuminova.mmce.common.handler;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.tiles.base.MachineGroupInput;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/**
 * 空手潜行右键可分组的输入仓（物品输入总线、流体输入仓、ME 输入总线、ME 样板供应器）时打开“组 ID”配置界面。
 * <p>
 * 1.7.10 移植：取自上游 github.kasuminova.mmce.common.handler.EventHandler#onBlockPlaced。此前移植了配置界面
 * （ContainerGroupInputConfig / GuiContainerGroupInputConfig）却漏了打开它的入口，方块提示中的操作无效；移植 ME 输入总线时补上。
 */
public class GroupInputConfigHandler {

    public static final GroupInputConfigHandler INSTANCE = new GroupInputConfigHandler();

    private GroupInputConfigHandler() {
    }

    @SubscribeEvent
    public void onPlayerRightClickBlock(PlayerInteractEvent event) {
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK || event.world == null) {
            return;
        }
        EntityPlayer player = event.entityPlayer;
        if (!player.isSneaking() || player.getHeldItem() != null) {
            return;
        }
        TileEntity te = event.world.getTileEntity(event.x, event.y, event.z);
        if (!(te instanceof MachineGroupInput m) || !m.canGroupInput()) {
            return;
        }
        if (!event.world.isRemote) {
            player.openGui(ModularMachinery.instance, CommonProxy.GuiType.GUI_GROUP_INPUT_CONFIG.ordinal(), event.world, event.x, event.y, event.z);
        }
        event.setCanceled(true);
    }
}
