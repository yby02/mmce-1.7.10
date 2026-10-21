package github.kasuminova.mmce.compat;

import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraftforge.common.MinecraftForge;

/**
 * 1.7.10 移植适配：1.12.2 只有 MinecraftForge.EVENT_BUS 一条总线；1.7.10 中 Tick/玩家登录/网络等 FML 事件发布在
 * FMLCommonHandler.instance().bus()，世界/方块/物品等 Forge 事件发布在 MinecraftForge.EVENT_BUS。
 * 注册错总线时事件会静默失效，因此统一把监听器同时注册到两条总线（每条总线只会分发自己的事件类型）。
 */
public final class EventBuses {

    private EventBuses() {
    }

    public static void register(Object listener) {
        MinecraftForge.EVENT_BUS.register(listener);
        FMLCommonHandler.instance().bus().register(listener);
    }

    public static void unregister(Object listener) {
        MinecraftForge.EVENT_BUS.unregister(listener);
        FMLCommonHandler.instance().bus().unregister(listener);
    }
}
