package github.kasuminova.mmce.common.container;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

/**
 * ME 部件界面的操作（设定格、流体槽、工作模式按钮等）。界面通过 {@link github.kasuminova.mmce.common.network.PktMEGuiAction}
 * 发送，服务端交给玩家当前打开的容器处理。
 */
public interface MEGuiActionHandler {

    /** 物品设定格：左 / 右键点击，value 为鼠标键。 */
    int ACTION_CONFIG_CLICK  = 0;
    /** 物品设定格：滚轮改数量，value 为新的数量。 */
    int ACTION_CONFIG_AMOUNT = 1;
    /** 流体设定格：value 为鼠标键（左键设为手持容器中的流体，空手左键或右键清空）。 */
    int ACTION_FLUID_CONFIG  = 2;
    /** 样板供应器：value 为工作模式序号。 */
    int ACTION_WORK_MODE     = 3;
    /** 样板供应器：把缓存送回 ME 网络。 */
    int ACTION_RETURN_ITEMS  = 4;
    /** 样板供应器：手持流体容器点击独立存储的流体槽（倒入或装出）。 */
    int ACTION_SUB_TANK      = 5;
    /** 物品设定格：从 NEI 拖入物品（对应上游的 JEI 拖放），载荷为物品 NBT。 */
    int ACTION_CONFIG_DROP   = 6;
    /** 流体设定格：从 NEI 拖入流体容器或流体显示物品，载荷为流体 NBT。 */
    int ACTION_FLUID_DROP    = 7;

    void onGuiAction(EntityPlayerMP player, int action, int slot, int value);

    /** 带载荷的操作（NEI 拖放）；不处理载荷的容器忽略它。payload 可能为 null。 */
    default void onGuiAction(EntityPlayerMP player, int action, int slot, int value, NBTTagCompound payload) {
        onGuiAction(player, action, slot, value);
    }
}
