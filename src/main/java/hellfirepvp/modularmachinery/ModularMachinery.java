/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery;

import hellfirepvp.modularmachinery.common.network.PktAssemblyReport;
import hellfirepvp.modularmachinery.common.network.PktAssemblyPatternSize;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.eventhandler.EventBus;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import github.kasuminova.mmce.common.concurrent.TaskExecutor;
import github.kasuminova.mmce.common.network.PktPerformanceReport;
import github.kasuminova.mmce.compat.crt.ZenRegistration;
import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.command.CommandGetBluePrint;
import hellfirepvp.modularmachinery.common.command.CommandHand;
import hellfirepvp.modularmachinery.common.command.CommandPerformanceReport;
import hellfirepvp.modularmachinery.common.command.CommandSyntax;
import hellfirepvp.modularmachinery.common.network.PktCopyToClipboard;
import hellfirepvp.modularmachinery.common.network.PktGroupInputConfig;
import hellfirepvp.modularmachinery.common.network.PktInteractFluidTankGui;
import hellfirepvp.modularmachinery.common.network.PktParallelControllerUpdate;
import hellfirepvp.modularmachinery.common.network.PktSmartInterfaceUpdate;
import hellfirepvp.modularmachinery.common.network.PktSyncSelection;
import net.minecraft.launchwrapper.Launch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on Github.
 * Class: ModularMachinery
 * Created by HellFirePvP
 * Date: 26.06.2017 / 20:26
 * <p>
 * 1.7.10 移植：依赖改为 GTNHLib + MineTweaker3（必需），其余模组均为软依赖。
 * 网络包 ID 与 1.12.2 保持一致；AE2 / Astral Sorcery 相关包随对应功能暂未移植。
 */
@Mod(modid = ModularMachinery.MODID, name = ModularMachinery.NAME, version = ModularMachinery.VERSION,
    dependencies = "required-after:Forge@[10.13.4.1614,);" +
        "required-after:gtnhlib@[0.11.54,);" +
        "required-after:MineTweaker3;" +
        "after:NotEnoughItems;" +
        "after:Waila;" +
        "after:IC2;" +
        "after:Mekanism;" +
        "after:EnderIO;" +
        "after:appliedenergistics2;" +
        "after:gregtech;" +
        "after:TConstruct;" +
        "after:ThermalExpansion;",
    acceptedMinecraftVersions = "[1.7.10]"
)
public class ModularMachinery {

    public static final String               MODID           = "modularmachinery";
    public static final String               NAME            = "Modular Machinery: Community Edition";
    public static final String               VERSION         = Tags.VERSION;
    public static final String               CLIENT_PROXY    = "hellfirepvp.modularmachinery.client.ClientProxy";
    public static final String               COMMON_PROXY    = "hellfirepvp.modularmachinery.common.CommonProxy";
    public static final SimpleNetworkWrapper NET_CHANNEL     = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);
    public static final TaskExecutor         EXECUTE_MANAGER = new TaskExecutor();
    public static final EventBus             EVENT_BUS       = new EventBus();

    @Mod.Instance(MODID)
    public static  ModularMachinery instance;
    public static  Logger           log         = LogManager.getLogger(NAME);
    @SidedProxy(clientSide = CLIENT_PROXY, serverSide = COMMON_PROXY)
    public static  CommonProxy      proxy;
    private static boolean          devEnvCache = false;

    public static boolean isRunningInDevEnvironment() {
        return devEnvCache;
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        event.getModMetadata().version = VERSION;
        devEnvCache = Boolean.TRUE.equals(Launch.blackboard.get("fml.deobfuscatedEnvironment"));

        NET_CHANNEL.registerMessage(PktCopyToClipboard.class, PktCopyToClipboard.class, 0, Side.CLIENT);
        NET_CHANNEL.registerMessage(PktSyncSelection.class, PktSyncSelection.class, 1, Side.CLIENT);
        NET_CHANNEL.registerMessage(PktPerformanceReport.class, PktPerformanceReport.class, 2, Side.CLIENT);
        NET_CHANNEL.registerMessage(PktAssemblyReport.class, PktAssemblyReport.class, 3, Side.CLIENT);

        NET_CHANNEL.registerMessage(PktGroupInputConfig.class, PktGroupInputConfig.class, 99, Side.SERVER);
        NET_CHANNEL.registerMessage(PktInteractFluidTankGui.class, PktInteractFluidTankGui.class, 100, Side.SERVER);
        NET_CHANNEL.registerMessage(PktSmartInterfaceUpdate.class, PktSmartInterfaceUpdate.class, 101, Side.SERVER);
        NET_CHANNEL.registerMessage(PktParallelControllerUpdate.class, PktParallelControllerUpdate.class, 102, Side.SERVER);
        // ID 104：1.12.2 的 PktAutoAssemblyRequest。1.7.10 的右键事件在服务端直接处理自动组装，客户端只需在开始 / 清除世界投影时同步动态结构尺寸
        NET_CHANNEL.registerMessage(PktAssemblyPatternSize.class, PktAssemblyPatternSize.class, 104, Side.SERVER);
        // ID 105：ME 部件界面操作（1.12.2 中为多个 AE2 专用数据包，1.7.10 合并为一个；不引用 AE2 类，未装 AE2 时同样注册以保持 ID 一致）
        NET_CHANNEL.registerMessage(github.kasuminova.mmce.common.network.PktMEGuiAction.class, github.kasuminova.mmce.common.network.PktMEGuiAction.class, 105, Side.SERVER);

        CommonProxy.loadModData(event.getModConfigurationDirectory());
        ZenRegistration.registerAll(event.getAsmData());
        proxy.preInit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit();
    }

    @Mod.EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) {
        proxy.loadComplete();
    }

    @Mod.EventHandler
    public void onServerStart(FMLServerStartingEvent event) {
        //Cmd registration
        event.registerServerCommand(new CommandSyntax());
        event.registerServerCommand(new CommandHand());
        event.registerServerCommand(new CommandGetBluePrint());
        event.registerServerCommand(new CommandPerformanceReport());
        if (hellfirepvp.modularmachinery.common.dev.DevMode.isEnabled()) {
            // 开发环境（或发布版加 -Dmmce.dev.tools=true）：自动搭建/投料/状态报告，供自动化测试使用
            event.registerServerCommand(new hellfirepvp.modularmachinery.common.dev.CommandMMCETest());
        }
        proxy.serverStarting(event);
    }

}
