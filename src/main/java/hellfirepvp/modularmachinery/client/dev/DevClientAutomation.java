package hellfirepvp.modularmachinery.client.dev;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import hellfirepvp.modularmachinery.ModularMachinery;
import github.kasuminova.mmce.compat.BlockPos;
import github.kasuminova.mmce.compat.EnumFacing;
import github.kasuminova.mmce.compat.ResourceLocation;
import hellfirepvp.modularmachinery.client.ClientProxy;
import hellfirepvp.modularmachinery.client.gui.GuiScreenBlueprint;
import hellfirepvp.modularmachinery.client.render.MachineComponentRenderer;
import hellfirepvp.modularmachinery.client.util.DynamicMachineRenderContext;
import hellfirepvp.modularmachinery.common.item.ItemBlueprint;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import hellfirepvp.modularmachinery.common.selection.PlayerStructureSelectionHelper;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import ink.ikx.mmce.core.AssemblyEventHandler;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayerMP;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import hellfirepvp.modularmachinery.common.lib.ItemsMM;
import net.minecraft.init.Items;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.StatCollector;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import youyihj.mmce.common.item.MachineProjector;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * 开发期客户端自动化：不注入任何系统键鼠输入，直接在游戏内驱动“建档 → 进世界 → 发放物品 → 打开背包/NEI 搜索 → 截图”。
 * 仅在开发环境且设置了 -Dmmce.dev.autotest=<场景名> 时启用；-Dmmce.dev.autotest.exit=true 时跑完自动退出。
 * 截图保存在 run/client/screenshots/，报告保存在 run/client/mmce-autotest/<场景名>-report.txt。
 */
public final class DevClientAutomation {

    private static final String WORLD_FOLDER = "mmce-autotest";

    private final String scenario;
    private final boolean exitWhenDone;
    private final Deque<Step> steps = new ArrayDeque<>();
    private final List<String> report = new ArrayList<>();

    private int waitTicks = 0;
    private String pendingScreenshot = null;
    private boolean worldLaunched = false;

    private DevClientAutomation(String scenario, boolean exitWhenDone) {
        this.scenario = scenario;
        this.exitWhenDone = exitWhenDone;
    }

    public static void registerIfRequested() {
        if (!hellfirepvp.modularmachinery.common.dev.DevMode.isEnabled()) {
            return;
        }
        String scenario = System.getProperty("mmce.dev.autotest");
        if (scenario == null || scenario.isEmpty()) {
            return;
        }
        DevClientAutomation automation = new DevClientAutomation(scenario, Boolean.getBoolean("mmce.dev.autotest.exit"));
        automation.buildScenario();
        FMLCommonHandler.instance().bus().register(automation);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new TooltipOverlay());
        ModularMachinery.log.info("[AutoTest] Client automation enabled, scenario={}", scenario);
    }

    private void buildScenario() {
        if ("p2".equals(scenario)) {
            buildP2Scenario();
            return;
        }
        if ("p4".equals(scenario)) {
            buildP4Scenario();
            return;
        }
        if ("p4b".equals(scenario)) {
            buildP4bScenario();
            return;
        }
        if ("p3".equals(scenario)) {
            buildP3Scenario();
            return;
        }
        if ("obf".equals(scenario)) {
            buildObfScenario();
            return;
        }
        if ("b3".equals(scenario)) {
            buildGasScenario();
            return;
        }
        if ("projector".equals(scenario)) {
            buildProjectorScenario();
            return;
        }
        if ("ae2".equals(scenario)) {
            buildAE2Scenario();
            return;
        }
        if ("magic".equals(scenario)) {
            buildMagicScenario();
            return;
        }
        if ("gugu".equals(scenario)) {
            buildGuguScenario();
            return;
        }
        // P0：验证依赖模组的物品能注册、贴图能渲染、NEI 能检索到。
        steps.add(new Step("give test items", 40, mc -> {
            String p = mc.thePlayer.getCommandSenderName();
            String[] gives = {
                "Mekanism:Ingot 2 4", "Mekanism:EnrichedAlloy 1", "Mekanism:ControlCircuit 1 0",
                "Mekanism:BasicBlock 9 8", "EnderIO:itemBasicCapacitor 2 0", "EnderIO:itemAlloy 1 0",
                "minecraft:coal 4"
            };
            for (String g : gives) {
                mc.thePlayer.sendChatMessage("/give " + p + " " + g);
            }
        }));
        // 创造模式下 GuiInventory 会被替换成创造物品栏，这里切到生存模式以使用普通背包界面
        steps.add(new Step("gamemode survival", 20, mc -> mc.thePlayer.sendChatMessage("/gamemode 0")));
        steps.add(new Step("open inventory", 30, mc -> mc.displayGuiScreen(new GuiInventory(mc.thePlayer))));
        // NEI 在首次进入世界后异步构建物品列表，必须等它完成后再搜索/截图
        steps.add(Step.waitUntil("wait nei item list", 20 * 60, mc -> !Loader.isModLoaded("NotEnoughItems") || NeiHooks.itemsLoaded()));
        steps.add(new Step("nei search @Mekanism", 30, mc -> neiSearch("@Mekanism")));
        steps.add(Step.waitUntil("wait nei filter (Mekanism)", 20 * 20, mc -> neiPanelHasItems()));
        steps.add(new Step("nei diagnostics", 0, mc -> neiDiagnostics()));
        steps.add(new Step("screenshot nei-mekanism", 10, mc -> screenshot("nei-mekanism")));
        steps.add(new Step("nei search capacitor", 30, mc -> neiSearch("capacitor")));
        steps.add(Step.waitUntil("wait nei filter (capacitor)", 20 * 20, mc -> neiPanelHasItems()));
        steps.add(new Step("nei diagnostics", 0, mc -> neiDiagnostics()));
        steps.add(new Step("screenshot nei-capacitor", 10, mc -> screenshot("nei-capacitor")));
        if ("p1".equals(scenario)) {
            // P1：MMCE 自身的方块/物品已注册，NEI 能检索到（贴图与渲染在 P2 验收）
            steps.add(new Step("nei search @Modular", 30, mc -> neiSearch("@Modular")));
            steps.add(Step.waitUntil("wait nei filter (Modular Machinery)", 20 * 20, mc -> neiPanelHasItems()));
            steps.add(new Step("nei diagnostics", 0, mc -> neiDiagnostics()));
            steps.add(new Step("screenshot nei-mmce", 10, mc -> screenshot("nei-mmce")));
        }
        steps.add(new Step("close gui", 20, mc -> mc.displayGuiScreen(null)));
        steps.add(new Step("screenshot world-hotbar", 10, mc -> screenshot("world-hotbar")));
    }

    // ---------------- 发布 jar（重混淆环境）：只用 MMCE 自身方块 ----------------

    private void buildObfScenario() {
        steps.add(new Step("setup world", 20, mc -> {
            new File(mc.mcDataDir, "mmce-test-report.txt").delete();
            for (String c : new String[]{"/time set 6000", "/gamerule doDaylightCycle false", "/gamerule doMobSpawning false"}) {
                mc.thePlayer.sendChatMessage(c);
            }
        }));
        steps.add(new Step("give mmce items", 40, mc -> {
            for (String g : new String[]{"modularmachinery:fuel_generator_controller 1", "modularmachinery:blockcasing 64 0",
                "modularmachinery:blockcasing 64 1", "modularmachinery:blockcasing 64 2", "modularmachinery:blockinputbus 4 0",
                "modularmachinery:blockenergyoutputhatch 4 0", "modularmachinery:itemblueprint 1", "modularmachinery:itemmodularium 16"}) {
                mc.thePlayer.sendChatMessage("/give " + mc.thePlayer.getCommandSenderName() + " " + g);
            }
        }));
        steps.add(new Step("screenshot hotbar", 10, mc -> {
            mc.thePlayer.inventory.currentItem = 8;
            look(mc, 0F, 10F);
            screenshot("hotbar");
        }));
        steps.add(new Step("face south", 5, mc -> look(mc, 0F, 25F)));
        steps.add(new Step("build fuel_generator", 40, mc -> mc.thePlayer.sendChatMessage("/mmce-test build fuel_generator")));
        steps.add(Step.waitUntil("fuel_generator formed", 20 * 20, mc -> formed(mc, "fuel_generator") == Boolean.TRUE));
        steps.add(new Step("screenshot formed", 10, mc -> screenshot("fuel_generator-formed")));
        steps.add(new Step("open controller", 10, mc -> rightClick(mc, "fuel_generator", 0, 0, 0, 2)));
        steps.add(Step.waitUntil("controller gui open", 20 * 5, mc -> screenIs(mc, "GuiMachineController")));
        steps.add(new Step("screenshot controller", 10, mc -> screenshot("fuel_generator-controller")));
        steps.add(new Step("close gui", 10, mc -> mc.thePlayer.closeScreen()));
        steps.add(new Step("open energy hatch", 10, mc -> rightClick(mc, "fuel_generator", 0, 0, 2, 2)));
        steps.add(Step.waitUntil("energy hatch gui open", 20 * 5, mc -> screenIs(mc, "GuiContainerEnergyHatch")));
        steps.add(new Step("close gui", 10, mc -> mc.thePlayer.closeScreen()));
        steps.add(new Step("fill", 200, mc -> mc.thePlayer.sendChatMessage("/mmce-test fill fuel_generator")));
        steps.add(new Step("status", 10, mc -> mc.thePlayer.sendChatMessage("/mmce-test status fuel_generator")));
        steps.add(new Step("generator produced energy", 0, mc -> expectStatus(mc, "fuel_generator", "state=CRAFTING")));
        // 流体仓 GUI 含 Mekanism 气体的软依赖分支：未装 Mekanism 时也必须能打开
        steps.add(new Step("open fluid hatch", 10, mc -> rightClick(mc, "fuel_generator", 1, 0, 1, 2)));
        steps.add(Step.waitUntil("fluid hatch gui open", 20 * 5, mc -> screenIs(mc, "GuiContainerFluidHatch")));
        steps.add(new Step("screenshot fluid hatch", 10, mc -> screenshot("fuel_generator-fluidhatch")));
        steps.add(new Step("close gui", 10, mc -> mc.thePlayer.closeScreen()));
        steps.add(new Step("look at controller", 20, mc -> look(mc, 0F, 0F)));
        steps.add(new Step("screenshot running", 10, mc -> screenshot("fuel_generator-running")));
        // gugu-utils 子集随发布 jar 提供：ID 与原模组相同，重混淆环境中照常注册与渲染
        steps.add(new Step("give gugu-utils items", 30, mc -> {
            String p = mc.thePlayer.getCommandSenderName();
            mc.thePlayer.sendChatMessage("/clear " + p);
            mc.thePlayer.sendChatMessage("/give " + p + " gugu-utils:environmenthatch 4");
            mc.thePlayer.sendChatMessage("/give " + p + " gugu-utils:constructionranged 1");
        }));
        steps.add(Step.waitUntil("gugu-utils items in the hotbar", 20 * 5, mc -> findHotbar(mc, GameRegistry.findItem("gugu-utils", "environmenthatch")) >= 0
            && findHotbar(mc, GameRegistry.findItem("gugu-utils", "constructionranged")) >= 0));
        steps.add(new Step("screenshot gugu-utils hotbar", 10, mc -> {
            mc.thePlayer.inventory.currentItem = 1;
            look(mc, 0F, 10F);
            screenshot("gugu-hotbar");
        }));
        steps.add(new Step("collect server report", 0, mc -> {
            for (String line : serverReport(mc)) {
                report.add("SRV  " + line);
            }
        }));
    }

    // ---------------- AE2：ME 部件的方块外观与界面 ----------------

    /** ae2_test 中相对控制器的位置。 */
    private static int[] ae2Pos(Minecraft mc, int dx, int dy, int dz) {
        int[] c = controllerPos(mc, "ae2_test");
        if (c == null) {
            throw new IllegalStateException("ae2_test not built");
        }
        return new int[]{c[0] + dx, c[1] + dy, c[2] + dz};
    }

    /** 在打开的容器中把背包里的某种物品拿到鼠标上（ME 部件容器的玩家格：0–26 主背包，27–35 快捷栏）。 */
    private static void pickUp(Minecraft mc, Item item) {
        ItemStack[] inv = mc.thePlayer.inventory.mainInventory;
        for (int i = 0; i < inv.length; i++) {
            if (inv[i] != null && inv[i].getItem() == item) {
                int index = i < 9 ? 27 + i : i - 9;
                mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, index, 0, 0, mc.thePlayer);
                return;
            }
        }
        throw new IllegalStateException("no " + item.getUnlocalizedName() + " in the inventory");
    }

    /** 把鼠标上的物品放回主背包的第一个空格。 */
    private static void putDown(Minecraft mc) {
        ItemStack[] inv = mc.thePlayer.inventory.mainInventory;
        for (int i = 9; i < inv.length; i++) {
            if (inv[i] == null) {
                mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, i - 9, 0, 0, mc.thePlayer);
                return;
            }
        }
    }

    private static void sendMEAction(int action, int slot, int value) {
        ModularMachinery.NET_CHANNEL.sendToServer(new github.kasuminova.mmce.common.network.PktMEGuiAction(action, slot, value));
    }

    private static boolean cursorIs(Minecraft mc, Item item) {
        ItemStack held = mc.thePlayer.inventory.getItemStack();
        return held != null && held.getItem() == item;
    }

    /**
     * 服务端自测 ae2-gui 搭好 AE2 网络与 ae2_test 并留下一组状态；客户端站到机器背后截取方块外观，依次打开 5 个 ME 界面截图，
     * 并经界面数据包（PktMEGuiAction）实际操作：物品设定格数量、拿着水桶设定流体、切换样板供应器工作模式、空桶从独立存储装出熔岩，
     * 以及空手潜行右键打开组 ID 配置界面。
     */
    private void buildAE2Scenario() {
        steps.add(new Step("setup world", 20, mc -> {
            new File(mc.mcDataDir, "mmce-test-report.txt").delete();
            for (String c : new String[]{"/time set 6000", "/gamerule doDaylightCycle false", "/gamerule doMobSpawning false", "/weather clear"}) {
                mc.thePlayer.sendChatMessage(c);
            }
        }));
        steps.add(new Step("start selftest ae2-gui", 20, mc -> mc.thePlayer.sendChatMessage("/mmce-test selftest ae2-gui")));
        steps.add(Step.waitUntil("selftest ae2-gui finished", 20 * 90, mc -> serverReport(mc).stream().anyMatch(l -> l.startsWith("SELFTEST result"))));
        steps.add(new Step("selftest ae2-gui passed", 0, mc -> {
            if (serverReport(mc).stream().noneMatch(l -> l.equals("SELFTEST result PASS"))) {
                throw new IllegalStateException("selftest ae2-gui did not pass");
            }
        }));
        steps.add(new Step("stand behind the machine", 40, mc -> {
            int[] c = ae2Pos(mc, 0, 0, 0);
            mc.thePlayer.sendChatMessage("/tp " + mc.thePlayer.getCommandSenderName() + " " + (c[0] + 0.5) + " " + c[1] + " " + (c[2] + 5.5));
        }));
        steps.add(new Step("look north", 10, mc -> look(mc, 180F, 20F)));
        // ME 部件是选择性更新的方块实体：内容只在玩家右键 / 打开界面时同步，这里只等方块实体出现
        steps.add(Step.waitUntil("client has the ME part tile entities", 20 * 10, mc -> DevClientAE2.providerMode(mc, ae2Pos(mc, 0, 0, 1)) != null));
        steps.add(new Step("screenshot machine", 10, mc -> screenshot("machine")));

        // ME 物品输入总线
        steps.add(new Step("open item input bus", 10, mc -> rightClick(mc, "ae2_test", -2, 0, 1, 3)));
        steps.add(Step.waitUntil("item input bus gui open", 20 * 5, mc -> screenIs(mc, "GuiMEItemInputBus")));
        steps.add(Step.waitUntil("client config slot 0 = 4", 20 * 5, mc -> DevClientAE2.itemConfigCount(mc, ae2Pos(mc, -2, 0, 1), 0) == 4));
        steps.add(new Step("set config slot 0 to 32 (gui packet)", 10,
            mc -> sendMEAction(github.kasuminova.mmce.common.container.MEGuiActionHandler.ACTION_CONFIG_AMOUNT, 0, 32)));
        steps.add(Step.waitUntil("client config slot 0 = 32", 20 * 5, mc -> DevClientAE2.itemConfigCount(mc, ae2Pos(mc, -2, 0, 1), 0) == 32));
        // NEI 拖放（对应上游的 JEI 拖放）：7 根羽毛拖到设定格 5（界面内 (34, 61)）；拖到界面空白处不被接收。
        // 用测试机配方中没有的物品 / 流体，以免总线从网络取货后触发配方、改变后面样板供应器的状态
        //（原先用金锭时，总线取到金锭后钻石配方消耗了供应器独立存储中的熔岩）
        steps.add(new Step("nei drop 7 feathers on config slot 5", 10, mc -> {
            if (!DevClientAE2.neiDrop(mc, 176, 204, 34, 61, new ItemStack(Items.feather, 7))) {
                throw new IllegalStateException("no NEI gui handler accepted the drop");
            }
            if (DevClientAE2.neiDrop(mc, 176, 204, 150, 12, new ItemStack(Items.feather, 7))) {
                throw new IllegalStateException("a drop outside the config slots was accepted");
            }
        }));
        steps.add(Step.waitUntil("client config slot 5 = feather x7", 20 * 5,
            mc -> "minecraft:feather@0 x7".equals(DevClientAE2.itemConfig(mc, ae2Pos(mc, -2, 0, 1), 5))));
        steps.add(new Step("screenshot item input bus", 10, mc -> screenshot("iteminputbus")));
        steps.add(new Step("close gui", 10, mc -> mc.thePlayer.closeScreen()));
        // 空手潜行右键：组 ID 配置
        addSneak(true);
        steps.add(new Step("sneak right click item input bus", 10, mc -> rightClick(mc, "ae2_test", -2, 0, 1, 3)));
        addSneak(false);
        steps.add(Step.waitUntil("group input config gui open", 20 * 5, mc -> screenIs(mc, "GuiContainerGroupInputConfig")));
        steps.add(new Step("screenshot group input config", 10, mc -> screenshot("groupinputconfig")));
        steps.add(new Step("close gui", 10, mc -> mc.thePlayer.closeScreen()));

        // ME 流体输入总线：拿着水桶点击第 2 个设定格
        steps.add(new Step("give water bucket and bucket", 20, mc -> {
            mc.thePlayer.sendChatMessage("/give " + mc.thePlayer.getCommandSenderName() + " minecraft:water_bucket 1");
            mc.thePlayer.sendChatMessage("/give " + mc.thePlayer.getCommandSenderName() + " minecraft:bucket 1");
        }));
        steps.add(new Step("open fluid input bus", 10, mc -> rightClick(mc, "ae2_test", -1, 0, 1, 3)));
        steps.add(Step.waitUntil("fluid input bus gui open", 20 * 5, mc -> screenIs(mc, "GuiMEFluidBus")));
        steps.add(Step.waitUntil("client: water marked, 1 capacity card (32000 mB)", 20 * 5,
            mc -> "water".equals(DevClientAE2.fluidConfig(mc, ae2Pos(mc, -1, 0, 1), 0)) && DevClientAE2.fluidCapacity(mc, ae2Pos(mc, -1, 0, 1)) == 32000));
        steps.add(new Step("pick up water bucket", 10, mc -> pickUp(mc, Items.water_bucket)));
        steps.add(Step.waitUntil("water bucket on the cursor", 20 * 5, mc -> cursorIs(mc, Items.water_bucket)));
        steps.add(new Step("click config slot 1 with the water bucket (gui packet)", 10,
            mc -> sendMEAction(github.kasuminova.mmce.common.container.MEGuiActionHandler.ACTION_FLUID_CONFIG, 1, 0)));
        steps.add(Step.waitUntil("client config slot 1 = water", 20 * 5, mc -> "water".equals(DevClientAE2.fluidConfig(mc, ae2Pos(mc, -1, 0, 1), 1))));
        steps.add(new Step("put the bucket back", 10, DevClientAutomation::putDown));
        // NEI 拖放：NEI 的氢气流体显示物品拖到流体设定格 2（界面内 (52, 43)）；流体容器（桶等）走同一路径（NEI 的 StackInfo.getFluid）
        steps.add(new Step("nei drop the hydrogen fluid display item on fluid config slot 2", 10, mc -> {
            if (!DevClientAE2.neiDrop(mc, 211, 231, 52, 43, NeiHooks.fluidDisplay("hydrogen"))) {
                throw new IllegalStateException("no NEI gui handler accepted the drop");
            }
        }));
        steps.add(Step.waitUntil("client config slot 2 = hydrogen", 20 * 5, mc -> "hydrogen".equals(DevClientAE2.fluidConfig(mc, ae2Pos(mc, -1, 0, 1), 2))));
        steps.add(new Step("screenshot fluid input bus", 10, mc -> screenshot("fluidinputbus")));
        steps.add(new Step("close gui", 10, mc -> mc.thePlayer.closeScreen()));

        // ME 样板供应器：切换工作模式；空桶点击独立存储的流体槽装出熔岩
        steps.add(new Step("open pattern provider", 10, mc -> rightClick(mc, "ae2_test", 0, 0, 1, 3)));
        steps.add(Step.waitUntil("pattern provider gui open", 20 * 5, mc -> screenIs(mc, "GuiMEPatternProvider")));
        steps.add(Step.waitUntil("client: cache entries and 2000 mB lava in the single inv", 20 * 5,
            mc -> DevClientAE2.providerCacheEntries(mc, ae2Pos(mc, 0, 0, 1)) >= 2 && DevClientAE2.providerSubTank(mc, ae2Pos(mc, 0, 0, 1)) == 2000));
        steps.add(new Step("screenshot pattern provider", 10, mc -> screenshot("patternprovider")));
        steps.add(new Step("work mode -> blocking (gui packet)", 10,
            mc -> sendMEAction(github.kasuminova.mmce.common.container.MEGuiActionHandler.ACTION_WORK_MODE, 0, 1)));
        steps.add(Step.waitUntil("client work mode = BLOCKING_MODE", 20 * 5, mc -> "BLOCKING_MODE".equals(DevClientAE2.providerMode(mc, ae2Pos(mc, 0, 0, 1)))));
        steps.add(new Step("pick up empty bucket", 10, mc -> pickUp(mc, Items.bucket)));
        steps.add(Step.waitUntil("bucket on the cursor", 20 * 5, mc -> cursorIs(mc, Items.bucket)));
        steps.add(new Step("click the single-inv tank with the bucket (gui packet)", 10,
            mc -> sendMEAction(github.kasuminova.mmce.common.container.MEGuiActionHandler.ACTION_SUB_TANK, 0, 0)));
        steps.add(Step.waitUntil("lava bucket on the cursor, 1000 mB left in the tank", 20 * 5,
            mc -> cursorIs(mc, Items.lava_bucket) && DevClientAE2.providerSubTank(mc, ae2Pos(mc, 0, 0, 1)) == 1000));
        steps.add(new Step("put the bucket back", 10, DevClientAutomation::putDown));
        steps.add(new Step("screenshot pattern provider (blocking)", 10, mc -> screenshot("patternprovider-blocking")));
        steps.add(new Step("close gui", 10, mc -> mc.thePlayer.closeScreen()));

        steps.add(new Step("open fluid output bus", 10, mc -> rightClick(mc, "ae2_test", 1, 0, 1, 3)));
        steps.add(Step.waitUntil("fluid output bus gui open", 20 * 5, mc -> screenIs(mc, "GuiMEFluidBus")));
        steps.add(new Step("screenshot fluid output bus", 10, mc -> screenshot("fluidoutputbus")));
        steps.add(new Step("close gui", 10, mc -> mc.thePlayer.closeScreen()));
        steps.add(new Step("open item output bus", 10, mc -> rightClick(mc, "ae2_test", 2, 0, 1, 3)));
        steps.add(Step.waitUntil("item output bus gui open", 20 * 5, mc -> screenIs(mc, "GuiMEItemOutputBus")));
        steps.add(new Step("screenshot item output bus", 10, mc -> screenshot("itemoutputbus")));
        steps.add(new Step("close gui", 10, mc -> mc.thePlayer.closeScreen()));
        steps.add(new Step("collect server report", 0, mc -> {
            for (String line : serverReport(mc)) {
                report.add("SRV  " + line);
            }
        }));
    }

    // ---------------- magic：Modular Magic 1.7.10 子集的方块外观、界面与 NEI ----------------

    /**
     * 需要 WITH_MAGIC=1（神秘时代 4、植物魔法、血魔法 1）。集成服务器先跑自测 magic（搭好 magic_test 与源质罐 / 管道、魔力发射器 / 魔力池、
     * 血之宝珠并跑完全部配方），然后站到机器南侧截取方块外观；右键源质 / 魔力输入仓看聊天栏中的内容说明，打开生命精华仓界面；
     * 最后在 NEI 中查看三类魔法需求的配方页（JSON 配方与 ZenScript 配方）。
     */
    private void buildMagicScenario() {
        steps.add(new Step("setup world", 20, mc -> {
            new File(mc.mcDataDir, "mmce-test-report.txt").delete();
            for (String c : new String[]{"/time set 6000", "/gamerule doDaylightCycle false", "/gamerule doMobSpawning false", "/weather clear"}) {
                mc.thePlayer.sendChatMessage(c);
            }
        }));
        steps.add(new Step("start selftest magic", 20, mc -> mc.thePlayer.sendChatMessage("/mmce-test selftest magic")));
        steps.add(Step.waitUntil("selftest magic finished", 20 * 180, mc -> serverReport(mc).stream().anyMatch(l -> l.startsWith("SELFTEST result"))));
        steps.add(new Step("selftest magic passed", 0, mc -> {
            if (serverReport(mc).stream().noneMatch(l -> l.equals("SELFTEST result PASS"))) {
                throw new IllegalStateException("selftest magic did not pass");
            }
        }));
        steps.add(new Step("stand south of the machine", 40, mc -> {
            int[] c = controllerPos(mc, "magic_test");
            if (c == null) {
                throw new IllegalStateException("magic_test not built");
            }
            mc.thePlayer.sendChatMessage("/tp " + mc.thePlayer.getCommandSenderName() + " " + (c[0] + 0.5) + " " + (c[1] + 2) + " " + (c[2] + 8.5));
        }));
        steps.add(new Step("look north", 40, mc -> look(mc, 180F, 22F)));
        steps.add(new Step("screenshot machine", 10, mc -> screenshot("machine")));
        // 魔力发射器与魔力池在机器北侧（z = -1 … -4）：站到北侧 8 格外朝南看
        steps.add(new Step("stand north of the machine", 20, mc -> {
            int[] c = controllerPos(mc, "magic_test");
            mc.thePlayer.sendChatMessage("/tp " + mc.thePlayer.getCommandSenderName() + " " + (c[0] + 0.5) + " " + (c[1] + 3) + " " + (c[2] - 9.5));
        }));
        steps.add(new Step("look south", 40, mc -> look(mc, 0F, 25F)));
        steps.add(new Step("screenshot devices", 10, mc -> screenshot("devices")));
        steps.add(new Step("back south of the machine", 40, mc -> {
            int[] c = controllerPos(mc, "magic_test");
            mc.thePlayer.sendChatMessage("/tp " + mc.thePlayer.getCommandSenderName() + " " + (c[0] + 0.5) + " " + c[1] + " " + (c[2] + 3.5));
        }));
        steps.add(new Step("look north (close)", 10, mc -> look(mc, 180F, 10F)));
        // 源质输入仓：自测结束时剩 24 点火（64 − 4 × 8 − 2 × 4）
        steps.add(new Step("right click aspect input hatch", 10, mc -> rightClick(mc, "magic_test", -3, 0, 0, 3)));
        steps.add(Step.waitUntil("chat: essentia 24 / 250", 20 * 5, mc -> chatHasPart(mc, "24 / 250")));
        steps.add(new Step("right click mana input hatch", 10, mc -> rightClick(mc, "magic_test", -2, 0, 0, 3)));
        steps.add(Step.waitUntil("chat: mana x / 100000", 20 * 5, mc -> chatHasPart(mc, " / 100000")));
        steps.add(new Step("screenshot hatch messages", 10, mc -> screenshot("hatch-messages")));
        steps.add(new Step("open life essence input hatch", 10, mc -> rightClick(mc, "magic_test", -1, 0, 0, 3)));
        steps.add(Step.waitUntil("life essence gui open", 20 * 5, mc -> screenIs(mc, "GuiContainerLifeEssence")));
        // 输入仓的缓存会从灵魂网络补满到上限（魔术师宝珠 150000 的 1/10），界面打开期间同步到客户端
        steps.add(Step.waitUntil("client LP cache synced (15000 / 15000)", 20 * 5, mc -> {
            int[] c = controllerPos(mc, "magic_test");
            net.minecraft.tileentity.TileEntity te = mc.theWorld.getTileEntity(c[0] - 1, c[1], c[2]);
            return te instanceof kport.modularmagic.common.tile.TileLifeEssenceProvider p && p.getLifeEssenceCache() == 15000;
        }));
        steps.add(new Step("screenshot life essence gui", 20, mc -> screenshot("lifeessence-gui")));
        steps.add(new Step("close gui", 10, mc -> mc.thePlayer.closeScreen()));

        steps.add(new Step("open inventory", 20, mc -> mc.displayGuiScreen(new GuiInventory(mc.thePlayer))));
        steps.add(Step.waitUntil("wait nei item list", 20 * 60, mc -> !Loader.isModLoaded("NotEnoughItems") || NeiHooks.itemsLoaded()));
        // 产物按 R：源质输入（圆石 → 石头）、魔力输入（沙子 → 玻璃）、生命精华输入（黏土 → 砖）；原料按 U：源质输出（泥土 → 水）
        addNeiPage("recipe-stone-aspect-input", true, () -> stack("minecraft", "stone", 0), "modularmachinery.recipe.magic_test", null);
        addNeiPage("recipe-glass-mana-input", true, () -> stack("minecraft", "glass", 0), "modularmachinery.recipe.magic_test", null);
        addNeiPage("recipe-brick-lp-input", true, () -> stack("minecraft", "brick", 0), "modularmachinery.recipe.magic_test", null);
        addNeiPage("usage-dirt-aspect-output", false, () -> stack("minecraft", "dirt", 0), "modularmachinery.recipe.magic_test", null);
        // ZenScript 配方（testdata/scripts_magic）：羽毛 → 箭（addAspectInput）
        addNeiPage("usage-feather-zs-aspect", false, () -> stack("minecraft", "feather", 0), "modularmachinery.recipe.magic_test", null);
        steps.add(new Step("close nei", 10, mc -> mc.displayGuiScreen(null)));
        steps.add(new Step("collect server report", 0, mc -> {
            for (String line : serverReport(mc)) {
                report.add("SRV  " + line);
            }
        }));
    }

    // ---------------- gugu-utils：环境传感器（成型变色、NEI 环境图标）与范围建造选择工具 ----------------

    /** 范围建造选择工具的两个角（相对 env_test 控制器）：两个传感器之间的一排，加上下面一排（外壳与地面）。 */
    private static final int[] RANGED_A = {-2, -1, 0};
    private static final int[] RANGED_B = {2, 0, 0};
    private int rangedExpected = -1;
    private long exportSince;

    private void buildGuguScenario() {
        steps.add(new Step("setup world", 20, mc -> {
            new File(mc.mcDataDir, "mmce-test-report.txt").delete();
            for (String c : new String[]{"/time set 6000", "/gamerule doDaylightCycle false", "/gamerule doMobSpawning false", "/weather clear"}) {
                mc.thePlayer.sendChatMessage(c);
            }
        }));
        // 原介绍说“未安装植物魔法时无法使用该工具”：开发环境未装植物魔法，下面的工具流程照常完成
        steps.add(new Step("Botania not installed", 0, mc -> report.add("INFO Botania loaded=" + Loader.isModLoaded("Botania"))));
        steps.add(new Step("start selftest environment", 20, mc -> mc.thePlayer.sendChatMessage("/mmce-test selftest environment")));
        steps.add(Step.waitUntil("selftest environment finished", 20 * 400, mc -> serverReport(mc).stream().anyMatch(l -> l.startsWith("SELFTEST result"))));
        steps.add(new Step("selftest environment passed", 0, mc -> {
            if (serverReport(mc).stream().noneMatch(l -> l.equals("SELFTEST result PASS"))) {
                throw new IllegalStateException("selftest environment did not pass");
            }
        }));
        steps.add(new Step("noon, clear, freeze time", 20, mc -> {
            for (String c : new String[]{"/time set 6000", "/gamerule doDaylightCycle false", "/weather clear"}) {
                mc.thePlayer.sendChatMessage(c);
            }
        }));
        steps.add(new Step("stand north of env_test", 40, mc -> {
            int[] c = guguCtrl(mc);
            mc.thePlayer.sendChatMessage("/tp " + mc.thePlayer.getCommandSenderName() + " " + (c[0] + 0.5) + " " + (c[1] + 1) + " " + (c[2] - 5.5));
        }));
        steps.add(new Step("look at env_test", 10, mc -> {
            int[] c = guguCtrl(mc);
            lookAt(mc, c[0] + 0.5, c[1] + 0.2, c[2] + 0.5);
        }));
        steps.add(Step.waitUntil("client: both sensors have the machine colour C83C3C", 20 * 10, mc -> sensorColor(mc, -2) == 0xC83C3C && sensorColor(mc, 2) == 0xC83C3C));
        steps.add(new Step("client: sensor block tint is the machine colour", 0, mc -> {
            int[] c = guguCtrl(mc);
            int tint = com.warmthdawn.mod.gugu_utils.GuGuUtils.blockEnvironmentHatch.colorMultiplier(mc.theWorld, c[0] - 2, c[1], c[2]) & 0xFFFFFF;
            report.add("INFO client sensor tint " + Integer.toHexString(tint));
            if (tint != 0xC83C3C) {
                throw new IllegalStateException("sensor tint " + Integer.toHexString(tint));
            }
        }));
        steps.add(new Step("screenshot env_test formed", 10, mc -> screenshot("env-machine")));

        // 成型时变色：拆一块外壳（解除成型），把西侧传感器换成新放的（默认颜色），再补回外壳 -> 重新成型后新传感器变成机器颜色
        steps.add(new Step("break env_test casing", 10, mc -> mc.thePlayer.sendChatMessage("/mmce-test break env_test")));
        steps.add(Step.waitUntil("env_test no longer formed", 20 * 20, mc -> formed(mc, "env_test") == Boolean.FALSE));
        steps.add(new Step("replace the west sensor with a new one", 20, mc -> {
            int[] c = guguCtrl(mc);
            String at = (c[0] - 2) + " " + c[1] + " " + c[2];
            mc.thePlayer.sendChatMessage("/setblock " + at + " air");
            mc.thePlayer.sendChatMessage("/setblock " + at + " gugu-utils:environmenthatch");
        }));
        steps.add(Step.waitUntil("client: new sensor has the default colour", 20 * 10, mc -> sensorColor(mc, -2) == hellfirepvp.modularmachinery.common.data.Config.machineColor));
        steps.add(new Step("new sensor colour before forming", 0, mc -> report.add("INFO new sensor before forming: "
            + Integer.toHexString(sensorColor(mc, -2)) + " (default " + Integer.toHexString(hellfirepvp.modularmachinery.common.data.Config.machineColor) + ")")));
        steps.add(new Step("screenshot new sensor before forming", 10, mc -> screenshot("env-sensor-before-forming")));
        steps.add(new Step("repair env_test casing", 10, mc -> mc.thePlayer.sendChatMessage("/mmce-test repair env_test")));
        steps.add(Step.waitUntil("env_test formed again", 20 * 20, mc -> formed(mc, "env_test") == Boolean.TRUE));
        steps.add(Step.waitUntil("client: new sensor took the machine colour C83C3C", 20 * 10, mc -> sensorColor(mc, -2) == 0xC83C3C));
        steps.add(new Step("screenshot new sensor after forming", 10, mc -> screenshot("env-sensor-after-forming")));

        // NEI：环境需求的图标与悬停提示
        steps.add(new Step("open inventory", 20, mc -> mc.displayGuiScreen(new GuiInventory(mc.thePlayer))));
        steps.add(Step.waitUntil("wait nei item list", 20 * 60, mc -> !Loader.isModLoaded("NotEnoughItems") || NeiHooks.itemsLoaded()));
        addEnvNeiPage("golden_apple", "biome-border", "biomes", 2);
        addEnvNeiPage("book", "zs-time-altitude-moon", "time", 3);
        addEnvNeiPage("diamond", "zs-thundering", "weather", 1);
        addEnvNeiPage("glass", "rain", "weather", 1);
        addEnvNeiPage("stone", "dimension", "dimensions", 1);
        addEnvNeiPage("pumpkin_pie", "biome-type", "biomes", 1);
        steps.add(new Step("close nei", 10, mc -> {
            TooltipOverlay.lines = null;
            mc.displayGuiScreen(null);
        }));

        // 范围建造选择工具：右键两个角 -> 选中框内所有非空气方块；手持时彩色描边；再用 MMCE 的建造选择工具右键控制器导出结构。
        // 服务端只处理 6 格以内的方块右键（触及距离 5 + 1），先走到机器北侧 3.5 格处
        steps.add(new Step("stand 3.5 blocks north of env_test", 40, mc -> {
            int[] c = guguCtrl(mc);
            mc.thePlayer.sendChatMessage("/tp " + mc.thePlayer.getCommandSenderName() + " " + (c[0] + 0.5) + " " + c[1] + " " + (c[2] - 3.5));
        }));
        steps.add(new Step("look at env_test (close)", 10, mc -> {
            int[] c = guguCtrl(mc);
            lookAt(mc, c[0] + 0.5, c[1], c[2] + 0.5);
        }));
        steps.add(new Step("give tools", 20, mc -> {
            String p = mc.thePlayer.getCommandSenderName();
            mc.thePlayer.sendChatMessage("/give " + p + " gugu-utils:constructionranged 1");
            mc.thePlayer.sendChatMessage("/give " + p + " modularmachinery:itemconstructtool 1");
        }));
        steps.add(new Step("tools into hotbar", 10, mc -> {
            moveToHotbar(mc, com.warmthdawn.mod.gugu_utils.GuGuUtils.constructionTool, 7);
            moveToHotbar(mc, ItemsMM.constructTool, 6);
        }));
        steps.add(Step.waitUntil("tools in hotbar", 20 * 5, mc -> findHotbar(mc, com.warmthdawn.mod.gugu_utils.GuGuUtils.constructionTool) >= 0
            && findHotbar(mc, ItemsMM.constructTool) >= 0));
        steps.add(new Step("ranged tool tooltip", 0, mc -> {
            ItemStack s = mc.thePlayer.inventory.mainInventory[findHotbar(mc, com.warmthdawn.mod.gugu_utils.GuGuUtils.constructionTool)];
            List<String> tip = s.getTooltip(mc.thePlayer, false);
            report.add("INFO ranged tool tooltip: " + tip);
            String creative = net.minecraft.client.resources.I18n.format("message.gugu-utils.rangedconstructtool.creative");
            if (!tip.contains(creative)) {
                throw new IllegalStateException("tooltip lacks '" + creative + "'");
            }
        }));
        addSneak(true);
        steps.add(new Step("ranged tool: first corner", 10, mc -> rangedClick(mc, RANGED_A)));
        steps.add(Step.waitUntil("ranged tool: start point stored on the held tool", 20 * 5, mc -> {
            int[] c = guguCtrl(mc);
            github.kasuminova.mmce.compat.BlockPos p = com.warmthdawn.mod.gugu_utils.modularmachenary.tools.ItemRangedConstructTool.getNbtPos(mc.thePlayer.getCurrentEquippedItem());
            return p.getX() == c[0] + RANGED_A[0] && p.getY() == c[1] + RANGED_A[1] && p.getZ() == c[2] + RANGED_A[2];
        }));
        addSneak(false);
        steps.add(Step.waitUntil("chat: start point", 20 * 5, mc -> {
            int[] c = guguCtrl(mc);
            return chatHasPart(mc, net.minecraft.client.resources.I18n.format("message.gugu-utils.rangedconstructtool.start",
                String.valueOf(c[0] + RANGED_A[0]), String.valueOf(c[1] + RANGED_A[1]), String.valueOf(c[2] + RANGED_A[2])));
        }));
        steps.add(Step.waitUntil("start point outlined", 20 * 5, mc -> {
            int[] c = guguCtrl(mc);
            int x = c[0] + RANGED_A[0], y = c[1] + RANGED_A[1], z = c[2] + RANGED_A[2];
            return java.util.Arrays.equals(com.warmthdawn.mod.gugu_utils.handler.ClientEventHandler.lastOutline, new int[]{x, y, z, x + 1, y + 1, z + 1});
        }));
        steps.add(new Step("screenshot ranged start point", 10, mc -> screenshot("ranged-start")));
        steps.add(new Step("count non-air blocks in the box", 0, mc -> {
            int[] c = guguCtrl(mc);
            int n = 0;
            for (int x = c[0] + RANGED_A[0]; x <= c[0] + RANGED_B[0]; x++) {
                for (int y = c[1] + RANGED_A[1]; y <= c[1] + RANGED_B[1]; y++) {
                    for (int z = c[2] + RANGED_A[2]; z <= c[2] + RANGED_B[2]; z++) {
                        if (!mc.theWorld.isAirBlock(x, y, z)) {
                            n++;
                        }
                    }
                }
            }
            rangedExpected = n;
            report.add("INFO non-air blocks in the box: " + n);
        }));
        addSneak(true);
        steps.add(new Step("ranged tool: second corner", 10, mc -> rangedClick(mc, RANGED_B)));
        addSneak(false);
        steps.add(Step.waitUntil("client selection synced (every non-air block in the box)", 20 * 5, mc -> constructSelectionSize() == rangedExpected));
        steps.add(Step.waitUntil("chat: selected N blocks", 20 * 5, mc -> {
            int[] c = guguCtrl(mc);
            return chatHasPart(mc, net.minecraft.client.resources.I18n.format("message.gugu-utils.rangedconstructtool.selected", String.valueOf(rangedExpected),
                String.valueOf(c[0] + RANGED_A[0]), String.valueOf(c[1] + RANGED_A[1]), String.valueOf(c[2] + RANGED_A[2]),
                String.valueOf(c[0] + RANGED_B[0]), String.valueOf(c[1] + RANGED_B[1]), String.valueOf(c[2] + RANGED_B[2])));
        }));
        steps.add(new Step("start point cleared after the second corner", 0, mc -> {
            if (com.warmthdawn.mod.gugu_utils.modularmachenary.tools.ItemRangedConstructTool.getNbtPos(mc.thePlayer.getCurrentEquippedItem()).getY() >= 0) {
                throw new IllegalStateException("start point still set");
            }
        }));
        steps.add(Step.waitUntil("selection bounding box outlined", 20 * 5, mc -> {
            int[] c = guguCtrl(mc);
            return java.util.Arrays.equals(com.warmthdawn.mod.gugu_utils.handler.ClientEventHandler.lastOutline, new int[]{
                c[0] + RANGED_A[0], c[1] + RANGED_A[1], c[2] + RANGED_A[2], c[0] + RANGED_B[0] + 1, c[1] + RANGED_B[1] + 1, c[2] + RANGED_B[2] + 1});
        }));
        steps.add(new Step("screenshot ranged selection", 10, mc -> screenshot("ranged-selection")));
        // MMCE 建造选择工具：潜行右键控制器 -> 把选区导出为机器结构 JSON
        addSneak(true);
        steps.add(new Step("export with the construct tool (sneak right-click the controller)", 10, mc -> {
            exportSince = System.currentTimeMillis() - 2000;
            int[] c = guguCtrl(mc);
            select(mc, findHotbar(mc, ItemsMM.constructTool));
            mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getCurrentEquippedItem(), c[0], c[1], c[2], 2,
                Vec3.createVectorHelper(c[0] + 0.5, c[1] + 0.5, c[2]));
        }));
        addSneak(false);
        steps.add(Step.waitUntil("exported machine json written", 20 * 5, mc -> !exportedMachines(mc).isEmpty()));
        steps.add(new Step("exported json contains the selection", 0, mc -> {
            List<File> files = exportedMachines(mc);
            String json = new String(Files.readAllBytes(files.get(0).toPath()), StandardCharsets.UTF_8);
            // 严格解析（不合法的 JSON 抛异常），再像加载机器时一样把每个部件的 nbt 读回
            com.google.gson.JsonArray parts = new com.google.gson.Gson().getAdapter(com.google.gson.JsonElement.class).fromJson(json)
                .getAsJsonObject().getAsJsonArray("parts");
            int withNbt = 0;
            for (com.google.gson.JsonElement part : parts) {
                if (part.getAsJsonObject().has("nbt")) {
                    hellfirepvp.modularmachinery.common.util.nbt.NBTJsonDeserializer.deserialize(part.getAsJsonObject().get("nbt").toString());
                    withNbt++;
                }
            }
            report.add("INFO exported " + files.get(0).getName() + ": " + parts.size() + " parts (" + withNbt + " with nbt, all parsed back), contains sensor="
                + json.contains("gugu-utils:environmenthatch"));
            if (parts.size() != rangedExpected || !json.contains("gugu-utils:environmenthatch")) {
                throw new IllegalStateException("exported structure has " + parts.size() + " parts (expected " + rangedExpected + ") or no environment sensor");
            }
            // 导出文件会在下次启动时作为机器加载，删除本次测试生成的文件
            for (File f : files) {
                Files.delete(f.toPath());
            }
        }));
        steps.add(Step.waitUntil("client selection cleared after export", 20 * 5, mc -> constructSelectionSize() == 0));

        // 生存模式：提示需要创造模式与指令权限，不记录起点
        steps.add(new Step("survival", 20, mc -> mc.thePlayer.sendChatMessage("/gamemode 0")));
        addSneak(true);
        steps.add(new Step("ranged tool in survival", 10, mc -> rangedClick(mc, RANGED_A)));
        addSneak(false);
        steps.add(Step.waitUntil("chat: denied", 20 * 5, mc -> chatHasPart(mc, net.minecraft.client.resources.I18n.format("message.gugu-utils.rangedconstructtool.denied"))));
        steps.add(new Step("no start point in survival", 0, mc -> {
            ItemStack s = mc.thePlayer.inventory.mainInventory[findHotbar(mc, com.warmthdawn.mod.gugu_utils.GuGuUtils.constructionTool)];
            if (com.warmthdawn.mod.gugu_utils.modularmachenary.tools.ItemRangedConstructTool.getNbtPos(s).getY() >= 0) {
                throw new IllegalStateException("start point stored in survival");
            }
        }));
        steps.add(new Step("screenshot denied message", 10, mc -> screenshot("ranged-denied")));
        steps.add(new Step("open inventory (survival)", 20, mc -> mc.displayGuiScreen(new GuiInventory(mc.thePlayer))));
        steps.add(new Step("nei search @GuGu", 30, mc -> neiSearch("@GuGu")));
        steps.add(Step.waitUntil("nei shows the two gugu-utils items", 20 * 20, mc -> !Loader.isModLoaded("NotEnoughItems") || NeiHooks.visibleItemCount() == 2));
        steps.add(new Step("screenshot nei gugu items", 10, mc -> screenshot("nei-gugu-items")));
        steps.add(new Step("close inventory, creative", 10, mc -> {
            mc.displayGuiScreen(null);
            mc.thePlayer.sendChatMessage("/gamemode 1");
        }));
        steps.add(new Step("collect server report", 0, mc -> {
            for (String line : serverReport(mc)) {
                report.add("SRV  " + line);
            }
        }));
    }

    private static int[] guguCtrl(Minecraft mc) {
        int[] c = controllerPos(mc, "env_test");
        if (c == null) {
            throw new IllegalStateException("env_test not built");
        }
        return c;
    }

    /** 客户端世界中控制器 dx 处传感器的机器颜色；不是传感器时返回 -1。 */
    private static int sensorColor(Minecraft mc, int dx) {
        int[] c = guguCtrl(mc);
        TileEntity te = mc.theWorld.getTileEntity(c[0] + dx, c[1], c[2]);
        return te instanceof com.warmthdawn.mod.gugu_utils.modularmachenary.environment.TileEnvironmentHatch h ? h.getMachineColor() : -1;
    }

    /** 拿着范围建造选择工具右键控制器偏移 o 处的方块（顶面）。 */
    private static void rangedClick(Minecraft mc, int[] o) {
        int[] c = guguCtrl(mc);
        select(mc, findHotbar(mc, com.warmthdawn.mod.gugu_utils.GuGuUtils.constructionTool));
        int x = c[0] + o[0];
        int y = c[1] + o[1];
        int z = c[2] + o[2];
        mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getCurrentEquippedItem(), x, y, z, 1,
            Vec3.createVectorHelper(x + 0.5, y + 1, z + 0.5));
    }

    /** 本次导出的结构文件（MMCE 建造选择工具写到 machinery 目录的 machine-<玩家>-<时间>.json）。 */
    private List<File> exportedMachines(Minecraft mc) {
        File dir = hellfirepvp.modularmachinery.common.CommonProxy.dataHolder.getMachineryDirectory();
        File[] files = dir.listFiles((d, n) -> n.startsWith("machine-" + mc.thePlayer.getCommandSenderName() + "-") && n.endsWith(".json"));
        List<File> out = new ArrayList<>();
        if (files != null) {
            for (File f : files) {
                if (f.lastModified() >= exportSince) {
                    out.add(f);
                }
            }
        }
        return out;
    }

    /** NEI：对产物按 R，切到 env_test 页，核对含环境需求的配方的图标数与悬停提示，并截图（悬停提示用 TooltipOverlay 画出）。 */
    private void addEnvNeiPage(String product, String name, String envName, int envSlots) {
        steps.add(new Step("nei R " + product + " (env_test)", 20, mc -> {
            TooltipOverlay.lines = null;
            ItemStack s = stack("minecraft", product, 0);
            int recipes = NeiHooks.openAndSelect(true, s, "modularmachinery.recipe.env_test");
            if (recipes <= 0) {
                throw new IllegalStateException("env_test has no recipe for " + product);
            }
            List<String> slots = NeiHooks.envSlots(0);
            report.add("INFO nei env slots for " + product + ": " + slots);
            if (slots.size() != envSlots || slots.stream().noneMatch(l -> l.startsWith(envName + ": "))) {
                throw new IllegalStateException("expected " + envSlots + " environment slots incl. " + envName + ", got " + slots);
            }
            List<String> tip = NeiHooks.envSlotTooltip(0, 0);
            TooltipOverlay.lines = tip;
        }));
        steps.add(new Step("screenshot nei " + name, 10, mc -> screenshot("nei-env-" + name)));
    }

    // ---------------- B3：Mekanism 气体在流体仓 GUI 与 Waila 中的显示 ----------------

    private void buildGasScenario() {
        steps.add(new Step("setup world", 20, mc -> {
            new File(mc.mcDataDir, "mmce-test-report.txt").delete();
            for (String c : new String[]{"/time set 6000", "/gamerule doDaylightCycle false", "/gamerule doMobSpawning false", "/weather clear"}) {
                mc.thePlayer.sendChatMessage(c);
            }
        }));
        steps.add(new Step("face south", 5, mc -> look(mc, 0F, 25F)));
        steps.add(new Step("build fuel_generator", 40, mc -> mc.thePlayer.sendChatMessage("/mmce-test build fuel_generator")));
        steps.add(Step.waitUntil("fuel_generator formed", 20 * 20, mc -> formed(mc, "fuel_generator") == Boolean.TRUE));
        steps.add(new Step("inject hydrogen", 40, mc -> mc.thePlayer.sendChatMessage("/mmce-test gas fuel_generator hydrogen 16000")));
        steps.add(new Step("open fluid hatch", 10, mc -> rightClick(mc, "fuel_generator", 1, 0, 1, 2)));
        steps.add(Step.waitUntil("fluid hatch gui open", 20 * 5, mc -> screenIs(mc, "GuiContainerFluidHatch")));
        steps.add(new Step("client hatch has gas", 10, mc -> {
            // 流体仓是选择性更新的方块实体：右键 / 打开 GUI 时服务端单独向该玩家同步（SelectiveUpdateHandler）
            String gas = clientHatchGas(mc, "fuel_generator", 1, 0, 1);
            report.add("INFO client fluid hatch gas: " + gas);
            if (gas == null || !gas.endsWith("hydrogen")) {
                throw new IllegalStateException("client fluid hatch has no hydrogen: " + gas);
            }
        }));
        steps.add(new Step("screenshot fluidhatch-gas", 10, mc -> screenshot("fluidhatch-gas")));
        // 与上游相同：打开 GUI 后离开方块实体 6 格，服务端关闭 GUI（SelectiveUpdateHandler）
        steps.add(new Step("walk 8 blocks away with the gui open", 0,
            mc -> mc.thePlayer.sendChatMessage("/tp " + mc.thePlayer.getCommandSenderName() + " ~ ~ ~-8")));
        steps.add(Step.waitUntil("gui closed after leaving 6 blocks", 20 * 5, mc -> mc.currentScreen == null));
        // Waila：站到流体仓东侧两格、看向它（正面视线会被前排外壳挡住）
        steps.add(new Step("move east of hatch", 30, mc -> {
            int[] c = controllerPos(mc, "fuel_generator");
            mc.thePlayer.sendChatMessage("/tp " + mc.thePlayer.getCommandSenderName() + " " + (c[0] + 3.5) + " " + (c[1] - 1) + " " + (c[2] + 1.5));
        }));
        steps.add(new Step("aim at hatch", 30, mc -> {
            int[] c = controllerPos(mc, "fuel_generator");
            aimAt(mc, c[0] + 1.5, c[1] + 0.5, c[2] + 1.5);
        }));
        steps.add(new Step("screenshot waila-gas", 10, mc -> screenshot("waila-gas")));
        steps.add(new Step("status", 10, mc -> mc.thePlayer.sendChatMessage("/mmce-test status fuel_generator")));
        steps.add(new Step("generator runs B3", 0, mc -> expectStatus(mc, "fuel_generator", "recipe=modularmachinery:fuel_generator_hydrogen")));
        steps.add(new Step("collect server report", 0, mc -> {
            for (String line : serverReport(mc)) {
                report.add("SRV  " + line);
            }
        }));
    }

    /** 客户端世界中控制器偏移处流体仓的气体（"<数量>mB <气体名>"），没有气体返回 null。 */
    private static String clientHatchGas(Minecraft mc, String machine, int dx, int dy, int dz) {
        if (!hellfirepvp.modularmachinery.common.base.Mods.MEKANISM.isPresent()) {
            return null;
        }
        int[] c = controllerPos(mc, machine);
        TileEntity te = mc.theWorld.getTileEntity(c[0] + dx, c[1] + dy, c[2] + dz);
        if (!(te instanceof hellfirepvp.modularmachinery.common.tiles.base.TileFluidTank tank)) {
            return null;
        }
        String name = hellfirepvp.modularmachinery.common.integration.mekanism.MekanismGasHelper.gasName(tank.getTank());
        return name == null ? null : hellfirepvp.modularmachinery.common.integration.mekanism.MekanismGasHelper.gasAmount(tank.getTank()) + "mB " + name;
    }

    /** 让玩家视线对准目标点（1.7.10 客户端玩家的 posY 即眼睛高度）。 */
    private static void aimAt(Minecraft mc, double x, double y, double z) {
        double dx = x - mc.thePlayer.posX;
        double dy = y - mc.thePlayer.posY;
        double dz = z - mc.thePlayer.posZ;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        look(mc, yaw, pitch);
    }

    // ---------------- P3：红石控制、区块卸载后恢复 ----------------

    private final long[] savedTick = new long[1];

    private void buildP3Scenario() {
        steps.add(new Step("setup world", 20, mc -> {
            new File(mc.mcDataDir, "mmce-test-report.txt").delete();
            for (String c : new String[]{"/time set 6000", "/gamerule doDaylightCycle false", "/gamerule doMobSpawning false"}) {
                mc.thePlayer.sendChatMessage(c);
            }
        }));
        // 红石：拉杆强充能控制器时配方暂停，断开后继续
        steps.add(new Step("face south", 5, mc -> look(mc, 0F, 25F)));
        steps.add(new Step("build assembler", 40, mc -> mc.thePlayer.sendChatMessage("/mmce-test build mek_eio_assembler")));
        steps.add(Step.waitUntil("assembler formed", 20 * 20, mc -> formed(mc, "mek_eio_assembler") == Boolean.TRUE));
        steps.add(new Step("fill", 30, mc -> mc.thePlayer.sendChatMessage("/mmce-test fill mek_eio_assembler")));
        // Waila：准星对准运行中的控制器
        steps.add(new Step("look at controller", 30, mc -> look(mc, 0F, 0F)));
        steps.add(new Step("screenshot waila-controller", 10, mc -> screenshot("waila-controller")));
        steps.add(new Step("redstone on", 10, mc -> mc.thePlayer.sendChatMessage("/mmce-test redstone mek_eio_assembler on")));
        steps.add(new Step("status powered 1", 60, mc -> mc.thePlayer.sendChatMessage("/mmce-test status mek_eio_assembler")));
        steps.add(new Step("remember tick", 0, mc -> savedTick[0] = lastTick(mc, "mek_eio_assembler")));
        steps.add(new Step("status powered 2", 10, mc -> mc.thePlayer.sendChatMessage("/mmce-test status mek_eio_assembler")));
        steps.add(new Step("screenshot powered", 10, mc -> screenshot("redstone-powered")));
        steps.add(new Step("paused while powered", 0, mc -> {
            long now = lastTick(mc, "mek_eio_assembler");
            if (now != savedTick[0]) {
                throw new IllegalStateException("tick changed while powered: " + savedTick[0] + " -> " + now);
            }
            report.add("INFO redstone paused at tick " + now);
        }));
        steps.add(new Step("redstone off", 140, mc -> mc.thePlayer.sendChatMessage("/mmce-test redstone mek_eio_assembler off")));
        steps.add(new Step("status unpowered", 10, mc -> mc.thePlayer.sendChatMessage("/mmce-test status mek_eio_assembler")));
        steps.add(new Step("resumed after unpowered", 0, mc -> expectStatus(mc, "mek_eio_assembler", "out=2xEnderIO:itemBasicCapacitor@0")));

        // 区块卸载：在出生区块之外搭建 -> 离开 -> 返回，机器继续运行
        steps.add(new Step("go far east", 60, mc -> mc.thePlayer.sendChatMessage("/tp " + mc.thePlayer.getCommandSenderName() + " ~1000 ~ ~")));
        steps.add(new Step("face south", 5, mc -> look(mc, 0F, 25F)));
        steps.add(new Step("build far generator", 40, mc -> mc.thePlayer.sendChatMessage("/mmce-test build fuel_generator")));
        steps.add(Step.waitUntil("far generator formed", 20 * 20, mc -> formed(mc, "fuel_generator") == Boolean.TRUE));
        steps.add(new Step("fill far generator", 40, mc -> mc.thePlayer.sendChatMessage("/mmce-test fill fuel_generator")));
        steps.add(new Step("status before leaving", 10, mc -> mc.thePlayer.sendChatMessage("/mmce-test status fuel_generator")));
        steps.add(new Step("remember generator tick", 0, mc -> {
            savedTick[0] = lastTick(mc, "fuel_generator");
            report.add("INFO generator tick before leaving " + savedTick[0]);
        }));
        steps.add(new Step("leave (chunks unload)", 400, mc -> mc.thePlayer.sendChatMessage("/tp " + mc.thePlayer.getCommandSenderName() + " ~-1000 ~ ~")));
        steps.add(new Step("come back", 100, mc -> mc.thePlayer.sendChatMessage("/tp " + mc.thePlayer.getCommandSenderName() + " ~1000 ~ ~")));
        steps.add(Step.waitUntil("generator formed after reload", 20 * 20, mc -> formed(mc, "fuel_generator") == Boolean.TRUE));
        steps.add(new Step("status after reload", 10, mc -> mc.thePlayer.sendChatMessage("/mmce-test status fuel_generator")));
        steps.add(new Step("screenshot after reload", 10, mc -> screenshot("generator-after-chunk-reload")));
        steps.add(new Step("generator resumed", 0, mc -> {
            expectStatus(mc, "fuel_generator", "formed=true");
            expectStatus(mc, "fuel_generator", "state=CRAFTING");
            report.add("INFO generator tick after reload " + lastTick(mc, "fuel_generator"));
        }));
        // 自动组装：创造模式瞬间完成；生存模式逐个放置并消耗背包；需要蓝图的机器先右键放入蓝图
        steps.add(new Step("move for assembly", 40, mc -> mc.thePlayer.sendChatMessage("/tp " + mc.thePlayer.getCommandSenderName() + " ~ ~ ~12")));
        steps.add(new Step("face south", 5, mc -> look(mc, 0F, 0F)));
        steps.add(new Step("controller only (creative)", 30, mc -> mc.thePlayer.sendChatMessage("/mmce-test controller mek_eio_assembler")));
        steps.add(new Step("give stick", 20, mc -> mc.thePlayer.sendChatMessage("/give " + mc.thePlayer.getCommandSenderName() + " minecraft:stick 1")));
        steps.add(new Step("right click with stick (creative)", 20, mc -> rightClickWith(mc, "mek_eio_assembler", Items.stick)));
        steps.add(Step.waitUntil("creative assembly formed", 20 * 20, mc -> formed(mc, "mek_eio_assembler") == Boolean.TRUE));
        steps.add(new Step("screenshot creative-assembly", 10, mc -> screenshot("assembly-creative")));

        steps.add(new Step("move for survival assembly", 40, mc -> mc.thePlayer.sendChatMessage("/tp " + mc.thePlayer.getCommandSenderName() + " ~ ~ ~12")));
        steps.add(new Step("face south", 5, mc -> look(mc, 0F, 0F)));
        steps.add(new Step("clear inventory", 20, mc -> mc.thePlayer.sendChatMessage("/clear " + mc.thePlayer.getCommandSenderName())));
        steps.add(new Step("survival", 20, mc -> mc.thePlayer.sendChatMessage("/gamemode 0")));
        steps.add(new Step("give generator blocks", 20, mc -> mc.thePlayer.sendChatMessage("/mmce-test give fuel_generator")));
        steps.add(new Step("give blueprint", 20, mc -> mc.thePlayer.sendChatMessage("/mm-get_blueprint fuel_generator")));
        steps.add(new Step("give stick", 20, mc -> mc.thePlayer.sendChatMessage("/give " + mc.thePlayer.getCommandSenderName() + " minecraft:stick 1")));
        steps.add(new Step("controller only (survival)", 30, mc -> mc.thePlayer.sendChatMessage("/mmce-test controller fuel_generator")));
        steps.add(new Step("inventory before", 0, mc -> report.add("INFO inventory before assembly: " + countItems(mc) + " items")));
        steps.add(new Step("right click with blueprint", 30, mc -> rightClickWith(mc, "fuel_generator", ItemsMM.blueprint)));
        steps.add(new Step("blueprint consumed", 0, mc -> {
            if (findHotbar(mc, ItemsMM.blueprint) >= 0 || findInventory(mc, ItemsMM.blueprint)) {
                throw new IllegalStateException("blueprint still in inventory");
            }
        }));
        steps.add(new Step("right click with stick (survival)", 20, mc -> rightClickWith(mc, "fuel_generator", Items.stick)));
        steps.add(Step.waitUntil("survival assembly formed", 20 * 30, mc -> formed(mc, "fuel_generator") == Boolean.TRUE));
        steps.add(new Step("inventory consumed", 0, mc -> {
            int left = countItems(mc);
            report.add("INFO inventory after assembly: " + left + " items");
            if (left > 1) {
                throw new IllegalStateException("structure blocks not consumed, items left: " + left);
            }
        }));
        steps.add(new Step("screenshot survival-assembly", 10, mc -> screenshot("assembly-survival")));
        steps.add(new Step("creative again", 10, mc -> mc.thePlayer.sendChatMessage("/gamemode 1")));
        steps.add(new Step("collect server report", 0, mc -> {
            for (String line : serverReport(mc)) {
                report.add("SRV  " + line);
            }
        }));
    }

    private static int findHotbar(Minecraft mc, Item item) {
        for (int i = 0; i < 9; i++) {
            ItemStack s = mc.thePlayer.inventory.mainInventory[i];
            if (s != null && s.getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    private static boolean findInventory(Minecraft mc, Item item) {
        for (ItemStack s : mc.thePlayer.inventory.mainInventory) {
            if (s != null && s.getItem() == item) {
                return true;
            }
        }
        return false;
    }

    private static int countItems(Minecraft mc) {
        int count = 0;
        for (ItemStack s : mc.thePlayer.inventory.mainInventory) {
            if (s != null) {
                count += s.stackSize;
            }
        }
        return count;
    }

    /** 手持指定物品（必须在快捷栏中）右键控制器正面。 */
    private static void rightClickWith(Minecraft mc, String machine, Item item) {
        int slot = findHotbar(mc, item);
        if (slot < 0) {
            throw new IllegalStateException(item + " not in hotbar");
        }
        int[] pos = controllerPos(mc, machine);
        if (pos == null) {
            throw new IllegalStateException("controller of " + machine + " not placed");
        }
        mc.thePlayer.inventory.currentItem = slot;
        mc.playerController.updateController();
        mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getCurrentEquippedItem(), pos[0], pos[1], pos[2], 2,
            Vec3.createVectorHelper(pos[0] + 0.5, pos[1] + 0.5, pos[2]));
    }

    /** 最近一次状态行中的配方 tick；没有进行中的配方时返回 -1。 */
    private static long lastTick(Minecraft mc, String machine) {
        String last = null;
        for (String line : serverReport(mc)) {
            if (line.startsWith("INFO status " + ModularMachinery.MODID + ":" + machine + " ")) {
                last = line;
            }
        }
        if (last == null) {
            return -1;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("tick=(\\d+)/").matcher(last);
        return m.find() ? Long.parseLong(m.group(1)) : -1;
    }

    // ---------------- P4：NEI 配方页与结构预览 ----------------

    private void buildP4Scenario() {
        steps.add(new Step("gamemode survival", 20, mc -> mc.thePlayer.sendChatMessage("/gamemode 0")));
        steps.add(new Step("open inventory", 20, mc -> mc.displayGuiScreen(new GuiInventory(mc.thePlayer))));
        steps.add(Step.waitUntil("wait nei item list", 20 * 60, mc -> !Loader.isModLoaded("NotEnoughItems") || NeiHooks.itemsLoaded()));
        // N1：对控制器按 R -> 结构预览
        addNeiPage("structure-fuel_generator", true, () -> controller("fuel_generator"), NeiHooks.STRUCTURE_ID, "modularmachinery:fuel_generator");
        steps.add(new Step("rotate view", 20, mc -> NeiHooks.setStructureView(135F, 20F, 1.3F, -1)));
        steps.add(new Step("screenshot structure-rotated", 10, mc -> screenshot("structure-fuel_generator-rotated")));
        steps.add(new Step("show bottom layer", 20, mc -> NeiHooks.setStructureView(225F, 60F, 1.0F, 0)));
        steps.add(new Step("screenshot structure-layer", 10, mc -> screenshot("structure-fuel_generator-layer0")));
        addNeiPage("structure-mek_eio_assembler", true, () -> controller("mek_eio_assembler"), NeiHooks.STRUCTURE_ID, "modularmachinery:mek_eio_assembler");
        // N3：对 Mekanism 钢制机壳按 U -> 列出用到它的结构
        addNeiPage("usage-steel-casing-structure", false, () -> stack("Mekanism", "BasicBlock", 8), NeiHooks.STRUCTURE_ID, "modularmachinery:mek_eio_assembler");
        // N2：对 EIO 基础电容按 R -> 装配机配方页
        addNeiPage("recipe-basic-capacitor", true, () -> stack("EnderIO", "itemBasicCapacitor", 0), "modularmachinery.recipe.mek_eio_assembler", null);
        // N3：对 Mekanism 钢锭按 U -> 用途中出现 A1
        addNeiPage("usage-steel-ingot", false, () -> stack("Mekanism", "Ingot", 4), "modularmachinery.recipe.mek_eio_assembler", null);
        // P6：ZenScript 定义的配方同样出现在 NEI 中（对 EIO 充能合金按 R）
        addNeiPage("recipe-zs-energetic-alloy", true, () -> stack("EnderIO", "itemAlloy", 1), "modularmachinery.recipe.mek_eio_assembler", null);
        // N4：熔岩桶与煤 -> 发电机配方
        addNeiPage("usage-lava-bucket", false, () -> stack("minecraft", "lava_bucket", 0), "modularmachinery.recipe.fuel_generator", null);
        addNeiPage("usage-coal-fuel", false, () -> stack("minecraft", "coal", 0), "modularmachinery.recipe.fuel_generator", null);
        // B3：Mekanism 气体按 Mekanism 的 NEI 约定（"gas" + GasStack）查询：氢气的用途 -> 发电机 B3；氢气的来源 -> 电解槽（测试机器）
        addNeiGasPage("usage-gas-hydrogen", false, "hydrogen", "modularmachinery.recipe.fuel_generator");
        addNeiGasPage("recipe-gas-hydrogen", true, "hydrogen", "modularmachinery.recipe.gas_electrolyzer");
        // Mekanism 把气体同时注册为同名流体：对该流体的 NEI 流体显示物品按 U 也能找到 B3
        addNeiPage("usage-hydrogen-fluid-display", false, () -> NeiHooks.fluidDisplay("hydrogen"), "modularmachinery.recipe.fuel_generator", null);
        // ZenScript 气体方法（testdata/scripts_mekanism/mmce_gas_test.zs）：氧气的用途 -> 发电机（addGasInput）；
        // 氧气的来源 -> 电解槽 2 条（addGasOutput、addGasPerTickOutput）；氯气的用途 -> 电解槽（addGasPerTickInput），并核对每 tick 气体格的悬停提示
        addNeiGasPage("usage-gas-oxygen", false, "oxygen", "modularmachinery.recipe.fuel_generator", 1);
        addNeiGasPage("recipe-gas-oxygen", true, "oxygen", "modularmachinery.recipe.gas_electrolyzer", 2);
        addNeiGasPage("usage-gas-chlorine-pertick", false, "chlorine", "modularmachinery.recipe.gas_electrolyzer", 1);
        steps.add(new Step("nei per-tick gas slot tooltips", 0, mc -> checkPerTickGasTooltips()));
        // 配方页上的气体格：氯气的用途页第一条配方（zs_gas_pertick）中，输入格（氯气）按 U、输出格（氧气）按 R
        addNeiGasSlotLookup("chlorine slot U", 0, true, "modularmachinery.recipe.gas_electrolyzer");
        addNeiGasPage("usage-gas-chlorine-pertick (again)", false, "chlorine", "modularmachinery.recipe.gas_electrolyzer", 1);
        addNeiGasSlotLookup("oxygen slot R", 1, false, "modularmachinery.recipe.gas_electrolyzer");
        // 副产物几率：对小麦种子按 R -> 离心机的草配方（种子 70%、可可豆 5%、南瓜 / 西瓜种子各 10%、黏土 40%、两种蘑菇各 30%、地狱疣 1%），
        // 格子左上角标注几率，悬停提示中有“有 x% 概率产出”
        addNeiPage("recipe-centrifuge-chance", true, () -> stack("minecraft", "wheat_seeds", 0), "modularmachinery.recipe.iron_centrifuge", null);
        steps.add(new Step("nei chance marks and tooltips", 0, mc -> checkChanceMarks()));
        // N5：控制器作为催化剂 -> 该机器全部配方
        addNeiPage("usage-controller-catalyst", false, () -> controller("mek_eio_assembler"), "modularmachinery.recipe.mek_eio_assembler", null);
        steps.add(new Step("close nei", 10, mc -> mc.displayGuiScreen(null)));
    }

    private void addNeiGasPage(String name, boolean crafting, String gas, String handlerId) {
        addNeiGasPage(name, crafting, gas, handlerId, -1);
    }

    /** expected &gt; 0 时要求该处理器的配方数恰好为 expected。 */
    private void addNeiGasPage(String name, boolean crafting, String gas, String handlerId, int expected) {
        steps.add(new Step("nei " + (crafting ? "R " : "U ") + name, 20, mc -> {
            if (!Loader.isModLoaded("Mekanism")) {
                report.add("SKIP nei " + name + " (Mekanism not loaded)");
                return;
            }
            int recipes = NeiHooks.openGasAndSelect(crafting, gas, handlerId);
            report.add("INFO nei " + name + " handler=" + handlerId + " recipes=" + recipes);
            if (recipes <= 0) {
                throw new IllegalStateException("handler " + handlerId + " has no recipes for gas " + gas);
            }
            if (expected > 0 && recipes != expected) {
                throw new IllegalStateException("handler " + handlerId + " has " + recipes + " recipes for gas " + gas + ", expected " + expected);
            }
        }));
        steps.add(new Step("screenshot " + name, 10, mc -> screenshot(name)));
    }

    /** 在当前页第一条配方的第 slot 个气体格上按 R / U，要求打开的新配方页中有 handlerId 的配方。 */
    private void addNeiGasSlotLookup(String name, int slot, boolean usage, String handlerId) {
        steps.add(new Step("nei gas slot " + name, 20, mc -> {
            if (!Loader.isModLoaded("Mekanism")) {
                report.add("SKIP nei gas slot " + name + " (Mekanism not loaded)");
                return;
            }
            String before = NeiHooks.currentPage();
            if (!NeiHooks.lookupGasSlot(0, slot, usage)) {
                throw new IllegalStateException("gas slot " + slot + " lookup did not open a page (from " + before + ")");
            }
            report.add("INFO nei gas slot " + name + ": " + before + " -> " + NeiHooks.currentPage());
        }));
        steps.add(new Step("nei gas slot " + name + " page", 0, mc -> {
            if (!Loader.isModLoaded("Mekanism")) {
                return;
            }
            if (!(mc.currentScreen instanceof codechicken.nei.recipe.GuiRecipe<?> gui)) {
                throw new IllegalStateException("no recipe page open");
            }
            boolean found = false;
            for (codechicken.nei.recipe.IRecipeHandler handler : gui.currenthandlers) {
                found |= handlerId.equals(handler.getHandlerId()) && handler.numRecipes() > 0;
            }
            if (!found || (usage != (gui instanceof codechicken.nei.recipe.GuiUsageRecipe))) {
                throw new IllegalStateException("expected a " + (usage ? "usage" : "recipe") + " page with " + handlerId + ", got " + NeiHooks.currentPage());
            }
        }));
        steps.add(new Step("screenshot nei gas slot " + name, 10, mc -> screenshot("gas-slot-" + name.replace(' ', '-'))));
    }

    /** 当前 NEI 页（氯气的用途 -> 电解槽的 zs_gas_pertick）中每 tick 气体格的悬停提示：与鼠标悬停时相同的内容。 */
    /** 客户端一侧（界面打开期间同步来的）能量仓储能；不是能量仓时为 -1。 */
    private static long clientEnergy(Minecraft mc, String machine, int dx, int dy, int dz) {
        int[] c = controllerPos(mc, machine);
        net.minecraft.tileentity.TileEntity te = c == null ? null : mc.theWorld.getTileEntity(c[0] + dx, c[1] + dy, c[2] + dz);
        return te instanceof hellfirepvp.modularmachinery.common.tiles.base.TileEnergyHatch hatch ? hatch.getCurrentEnergy() : -1;
    }

    /** 集成服务器一侧的能量仓储能；不是能量仓时为 -1。 */
    private static long serverEnergy(Minecraft mc, String machine, int dx, int dy, int dz) {
        int[] c = controllerPos(mc, machine);
        net.minecraft.tileentity.TileEntity te = c == null ? null
            : mc.getIntegratedServer().worldServerForDimension(mc.thePlayer.dimension).getTileEntity(c[0] + dx, c[1] + dy, c[2] + dz);
        return te instanceof hellfirepvp.modularmachinery.common.tiles.base.TileEnergyHatch hatch ? hatch.getCurrentEnergy() : -1;
    }

    /** 燃料发电机能量输出仓的储能提示（与界面中鼠标悬停在储能条上时相同），exact 对应是否按住 Shift。 */
    private static java.util.List<String> energyTooltip(Minecraft mc, boolean exact) {
        int[] c = controllerPos(mc, "fuel_generator");
        hellfirepvp.modularmachinery.common.tiles.base.TileEnergyHatch hatch =
            (hellfirepvp.modularmachinery.common.tiles.base.TileEnergyHatch) mc.theWorld.getTileEntity(c[0], c[1], c[2] + 2);
        return hellfirepvp.modularmachinery.client.util.EnergyDisplayUtil.chargeTooltip(hatch.getCurrentEnergy(), hatch.getMaxEnergy(), exact);
    }

    private void checkEnergyTooltip(Minecraft mc) {
        java.util.List<String> normal = energyTooltip(mc, false);
        java.util.List<String> exact = energyTooltip(mc, true);
        report.add("INFO energy tooltip: " + normal + " | shift: " + exact);
        String unit = hellfirepvp.modularmachinery.client.util.EnergyDisplayUtil.unit();
        String hint = StatCollector.translateToLocal("tooltip.energyhatch.shift");
        // 输出仓上限 2097152 RF：缩写为 2.097M，完整为 2,097,152；当前储能为发电机的实际产出（客户端同步值）
        if (!normal.get(0).endsWith(" " + unit) || !normal.get(0).contains("2.097M") || normal.size() != 2 || !normal.get(1).contains(hint)) {
            throw new IllegalStateException("abbreviated tooltip " + normal);
        }
        if (!exact.get(0).endsWith(" " + unit) || !exact.get(0).contains("2,097,152") || exact.size() != 1) {
            throw new IllegalStateException("shift tooltip " + exact);
        }
    }

    /** 开发期：在当前界面上画一段提示文字（截图用；真实悬停需要鼠标在储能条上）。lines 为 null 时不画。 */
    public static final class TooltipOverlay {

        static volatile java.util.List<String> lines;

        @cpw.mods.fml.common.eventhandler.SubscribeEvent
        public void onDrawScreen(net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent.Post event) {
            java.util.List<String> text = lines;
            if (text == null || event.gui == null) {
                return;
            }
            try {
                Method draw = findMethod(net.minecraft.client.gui.GuiScreen.class, new String[]{"drawHoveringText"},
                    java.util.List.class, int.class, int.class, net.minecraft.client.gui.FontRenderer.class);
                draw.invoke(event.gui, text, event.gui.width / 2 - 70, event.gui.height / 2 - 60, Minecraft.getMinecraft().fontRenderer);
            } catch (ReflectiveOperationException e) {
                lines = null;
            }
        }
    }

    private void checkChanceMarks() {
        // 对小麦种子按 R 的离心机页中可能有多条配方（还有蘑菇等），逐条找出草配方（种子 70%）
        java.util.List<String> info = java.util.Collections.emptyList();
        for (int r = 0; r < 8; r++) {
            java.util.List<String> lines = NeiHooks.chanceInfo(r);
            if (lines.contains("mark 70%")) {
                info = lines;
                report.add("INFO nei chance: grass recipe is #" + r);
                break;
            }
        }
        for (String line : info) {
            report.add("INFO nei chance: " + line);
        }
        java.util.List<String> marks = new java.util.ArrayList<>();
        for (String line : info) {
            if (line.startsWith("mark ")) {
                marks.add(line.substring(5));
            }
        }
        java.util.Collections.sort(marks);
        // 草配方的 8 个几率产出（JSON：0.7 / 0.05 / 0.1 / 0.1 / 0.4 / 0.3 / 0.3 / 0.01）；必定产出的泥土不标注
        java.util.List<String> expected = java.util.Arrays.asList("1%", "10%", "10%", "30%", "30%", "40%", "5%", "70%");
        if (!marks.equals(expected)) {
            throw new IllegalStateException("chance marks " + marks + ", expected " + expected);
        }
        String seeds = net.minecraft.client.resources.I18n.format("tooltip.machinery.chance.out", "70%");
        if (info.stream().noneMatch(l -> l.startsWith("out ") && l.contains(seeds))) {
            throw new IllegalStateException("no output slot tooltip contains '" + seeds + "'");
        }
    }

    private void checkPerTickGasTooltips() {
        if (!Loader.isModLoaded("Mekanism")) {
            report.add("SKIP nei per-tick gas tooltips (Mekanism not loaded)");
            return;
        }
        java.util.List<String> slots = NeiHooks.gasSlotTooltips(0);
        for (String slot : slots) {
            report.add("INFO nei per-tick gas slot: " + slot);
        }
        String in = net.minecraft.client.resources.I18n.format("tooltip.fluid_pertick.in", 4);
        String out = net.minecraft.client.resources.I18n.format("tooltip.fluid_pertick.out", 2);
        boolean inOk = false;
        boolean outOk = false;
        for (String slot : slots) {
            inOk |= slot.startsWith("in ") && slot.contains(in);
            outOk |= slot.startsWith("out ") && slot.contains(out);
        }
        if (slots.size() != 2 || !inOk || !outOk) {
            throw new IllegalStateException("per-tick gas slots " + slots + " do not show '" + in + "' and '" + out + "'");
        }
    }

    // ---------------- P4b：蓝图右键预览界面、投影到世界、动态结构尺寸用于自动组装 ----------------

    private void buildP4bScenario() {
        steps.add(new Step("setup world", 20, mc -> {
            new File(mc.mcDataDir, "mmce-test-report.txt").delete();
            for (String c : new String[]{"/time set 6000", "/gamerule doDaylightCycle false", "/gamerule doMobSpawning false", "/weather clear"}) {
                mc.thePlayer.sendChatMessage(c);
            }
        }));
        steps.add(new Step("give blueprints and stick", 40, mc -> {
            mc.thePlayer.sendChatMessage("/mm-get_blueprint fuel_generator");
            mc.thePlayer.sendChatMessage("/mm-get_blueprint assembly_line");
            mc.thePlayer.sendChatMessage("/give " + mc.thePlayer.getCommandSenderName() + " minecraft:stick 1");
        }));

        // A：手持蓝图右键（真实右键流程）打开预览界面；3D、分层
        addOpenBlueprintPreview("fuel_generator");
        steps.add(new Step("rotate preview", 10, mc -> blueprintGui(mc).getContext().setView(200F, 25F, 1.1F)));
        steps.add(new Step("screenshot blueprint-preview-fuel_generator", 10, mc -> screenshot("blueprint-preview-fuel_generator")));
        steps.add(new Step("click layer mode", 10, mc -> clickBlueprintButton(mc, GuiScreenBlueprint.BTN_LAYER_MODE)));
        steps.add(new Step("click layer up", 10, mc -> {
            clickBlueprintButton(mc, GuiScreenBlueprint.BTN_LAYER_UP);
            DynamicMachineRenderContext ctx = blueprintGui(mc).getContext();
            if (ctx.doesRenderIn3D() || ctx.getRenderSlice() != 1) {
                throw new IllegalStateException("layer mode not applied: 3d=" + ctx.doesRenderIn3D() + " slice=" + ctx.getRenderSlice());
            }
            report.add("INFO preview layer " + (ctx.getRenderSlice() + 1) + "/" + ctx.getLayerCount());
        }));
        steps.add(new Step("screenshot blueprint-preview-layer", 10, mc -> screenshot("blueprint-preview-fuel_generator-layer2")));
        steps.add(new Step("close preview", 10, mc -> mc.displayGuiScreen(null)));

        // B：动态结构（流水线，最小 5）：预览中调到 7 -> 投影 -> 贴到朝南的控制器上 -> 木棍自动组装 -> 结构按 7 组装，投影自动清除
        steps.add(new Step("assembly_line controller (south)", 30, mc -> mc.thePlayer.sendChatMessage("/mmce-test controller assembly_line south")));
        addOpenBlueprintPreview("assembly_line");
        steps.add(new Step("dynamic size +2", 10, mc -> {
            clickBlueprintButton(mc, GuiScreenBlueprint.BTN_DYNAMIC_PLUS);
            clickBlueprintButton(mc, GuiScreenBlueprint.BTN_DYNAMIC_PLUS);
            int size = blueprintGui(mc).getContext().getDynamicPatternSize();
            report.add("INFO preview dynamic size " + size);
            if (size != 7) {
                throw new IllegalStateException("dynamic size " + size + " != 7");
            }
        }));
        steps.add(new Step("rotate preview", 10, mc -> blueprintGui(mc).getContext().setView(240F, 30F, 1.0F)));
        steps.add(new Step("screenshot assembly_line-preview-size7", 10, mc -> screenshot("blueprint-preview-assembly_line-size7")));
        steps.add(new Step("click place world preview", 10, mc -> {
            clickBlueprintButton(mc, GuiScreenBlueprint.BTN_PLACE_WORLD);
            DynamicMachineRenderContext ctx = ClientProxy.renderHelper.getContext();
            if (mc.currentScreen != null || ctx == null || ctx.getDynamicPatternSize() != 7) {
                throw new IllegalStateException("projection not started: screen=" + mc.currentScreen + " ctx=" + ctx);
            }
        }));
        steps.add(new Step("server got projection size", 0, mc -> expectServerProjectionSize(mc, "assembly_line", 7)));
        steps.add(new Step("look at assembly_line controller", 5, mc -> lookAtController(mc, "assembly_line")));
        steps.add(new Step("projection follows cursor", 0, mc -> {
            int missing = ClientProxy.renderHelper.getMissingBlocks().size();
            report.add("INFO projection following, missing blocks " + missing);
            if (missing == 0 || ClientProxy.renderHelper.getAttachedPosition() != null) {
                throw new IllegalStateException("projection not following the cursor, missing=" + missing);
            }
        }));
        steps.add(new Step("screenshot projection-follow", 10, mc -> screenshot("projection-assembly_line-follow")));
        steps.add(new Step("select stick", 5, mc -> select(mc, findHotbar(mc, Items.stick))));
        steps.add(new Step("right click (place projection)", 10, DevClientAutomation::rightClickMouse));
        steps.add(new Step("projection placed on controller", 0, mc -> {
            BlockPos attached = ClientProxy.renderHelper.getAttachedPosition();
            if (attached == null) {
                throw new IllegalStateException("projection not placed");
            }
            report.add("INFO projection placed at " + attached + " layer=" + ClientProxy.renderHelper.getRenderedLayer()
                + " missing=" + ClientProxy.renderHelper.getMissingBlocks().size());
            // 放置投影的这次右键被取消，不应触发自动组装（创造模式组装是瞬间完成的，组装了缺失方块会变为 0）
            if (ClientProxy.renderHelper.getMissingBlocks().isEmpty() || formed(mc, "assembly_line") == Boolean.TRUE) {
                throw new IllegalStateException("placing the projection must not start assembly");
            }
        }));
        steps.add(new Step("screenshot projection-placed", 10, mc -> screenshot("projection-assembly_line-placed")));
        steps.add(new Step("right click with stick (assembly)", 20, DevClientAutomation::rightClickMouse));
        steps.add(Step.waitUntil("assembly_line formed", 20 * 20, mc -> formed(mc, "assembly_line") == Boolean.TRUE));
        steps.add(new Step("status assembly_line", 10, mc -> mc.thePlayer.sendChatMessage("/mmce-test status assembly_line")));
        steps.add(new Step("assembled with dynamic size 7", 0, mc -> {
            expectStatus(mc, "assembly_line", "formed=true");
            expectStatus(mc, "assembly_line", "facing=south");
            expectStatus(mc, "assembly_line", "dyn[line]=7");
        }));
        steps.add(Step.waitUntil("projection cleared after completion", 20 * 5, mc -> ClientProxy.renderHelper.getContext() == null));
        steps.add(new Step("server projection size cleared", 0, mc -> expectServerProjectionSize(mc, "assembly_line", 0)));
        steps.add(new Step("look at assembled line", 10, mc -> {
            int[] pos = controllerPos(mc, "assembly_line");
            lookAt(mc, pos[0] + 0.5, pos[1] + 0.5, pos[2] + 5.5);
        }));
        steps.add(new Step("screenshot assembly_line-assembled", 10, mc -> screenshot("assembly_line-assembled-size7")));

        // C：NEI 结构页“投影”按钮；控制器朝东时投影随之旋转；组装后投影自动清除（说明旋转后的投影与组装结果一致）
        steps.add(new Step("move west", 40, mc -> mc.thePlayer.sendChatMessage("/tp " + mc.thePlayer.getCommandSenderName() + " ~-14 ~ ~")));
        steps.add(new Step("fuel_generator controller (east)", 30, mc -> mc.thePlayer.sendChatMessage("/mmce-test controller fuel_generator east")));
        steps.add(new Step("open inventory", 20, mc -> mc.displayGuiScreen(new GuiInventory(mc.thePlayer))));
        steps.add(Step.waitUntil("wait nei item list", 20 * 60, mc -> !Loader.isModLoaded("NotEnoughItems") || NeiHooks.itemsLoaded()));
        addNeiPage("structure-fuel_generator", true, () -> controller("fuel_generator"), NeiHooks.STRUCTURE_ID, "modularmachinery:fuel_generator");
        steps.add(new Step("nei project button", 10, mc -> {
            if (!NeiHooks.projectStructure(0)) {
                throw new IllegalStateException("NEI projection failed");
            }
            DynamicMachineRenderContext ctx = ClientProxy.renderHelper.getContext();
            if (ctx == null || !"fuel_generator".equals(ctx.getDisplayedMachine().getRegistryName().getPath()) || mc.currentScreen != null) {
                throw new IllegalStateException("NEI projection not active");
            }
        }));
        steps.add(new Step("look at fuel_generator controller", 5, mc -> lookAtController(mc, "fuel_generator")));
        steps.add(new Step("screenshot projection-rotated-follow", 10, mc -> screenshot("projection-fuel_generator-east-follow")));
        steps.add(new Step("select stick", 5, mc -> select(mc, findHotbar(mc, Items.stick))));
        steps.add(new Step("right click (place rotated projection)", 10, DevClientAutomation::rightClickMouse));
        steps.add(new Step("rotated projection placed", 0, mc -> {
            BlockPos attached = ClientProxy.renderHelper.getAttachedPosition();
            int[] ctrl = controllerPos(mc, "fuel_generator");
            if (attached == null) {
                throw new IllegalStateException("projection not placed");
            }
            BlockPos expected = new BlockPos(ctrl[0], ctrl[1], ctrl[2]).subtract(
                MiscUtils.rotateYCCWNorthUntil(ClientProxy.renderHelper.getContext().getMoveOffset(), EnumFacing.EAST));
            report.add("INFO rotated projection at " + attached + " expected " + expected + " missing=" + ClientProxy.renderHelper.getMissingBlocks().size());
            if (!attached.equals(expected)) {
                throw new IllegalStateException("projection not aligned to east-facing controller");
            }
        }));
        steps.add(new Step("screenshot projection-rotated-placed", 10, mc -> screenshot("projection-fuel_generator-east-placed")));
        steps.add(new Step("right click with stick (assembly east)", 20, DevClientAutomation::rightClickMouse));
        steps.add(Step.waitUntil("rotated projection cleared after assembly", 20 * 10, mc -> ClientProxy.renderHelper.getContext() == null));
        steps.add(new Step("status fuel_generator", 10, mc -> mc.thePlayer.sendChatMessage("/mmce-test status fuel_generator")));
        steps.add(new Step("fuel_generator facing east", 0, mc -> expectStatus(mc, "fuel_generator", "facing=east")));
        steps.add(new Step("screenshot fuel_generator-east-assembled", 10, mc -> screenshot("fuel_generator-east-assembled")));

        // D：清除条件——离开 32 格以上；打开另一台机器的蓝图预览（同一台机器不清除）
        addOpenBlueprintPreview("fuel_generator");
        steps.add(new Step("click place world preview", 10, mc -> clickBlueprintButton(mc, GuiScreenBlueprint.BTN_PLACE_WORLD)));
        // 不要正好 45° 且从方块中心出发：射线恰好穿过方块棱角时 1.7.10 原版射线检测（含准星 objectMouseOver）会落空
        steps.add(new Step("look at ground", 5, mc -> lookAt(mc, mc.thePlayer.posX - 2.3, mc.thePlayer.posY - 1.62 - 0.5, mc.thePlayer.posZ - 3.1)));
        steps.add(new Step("right click (place on ground)", 10, DevClientAutomation::rightClickMouse));
        steps.add(new Step("placed on ground", 0, mc -> {
            BlockPos attached = ClientProxy.renderHelper.getAttachedPosition();
            if (attached == null) {
                throw new IllegalStateException("projection not placed on ground");
            }
            report.add("INFO ground projection at " + attached + " missing=" + ClientProxy.renderHelper.getMissingBlocks().size());
        }));
        steps.add(new Step("screenshot projection-ground", 10, mc -> screenshot("projection-fuel_generator-ground")));
        steps.add(new Step("leave 40 blocks", 40, mc -> mc.thePlayer.sendChatMessage("/tp " + mc.thePlayer.getCommandSenderName() + " ~40 ~ ~")));
        steps.add(Step.waitUntil("projection cleared after leaving", 20 * 5, mc -> ClientProxy.renderHelper.getContext() == null));
        steps.add(new Step("server projection size cleared", 0, mc -> expectServerProjectionSize(mc, "fuel_generator", 0)));
        addOpenBlueprintPreview("fuel_generator");
        steps.add(new Step("click place world preview", 10, mc -> clickBlueprintButton(mc, GuiScreenBlueprint.BTN_PLACE_WORLD)));
        addOpenBlueprintPreview("fuel_generator");
        steps.add(new Step("same machine preview keeps projection", 0, mc -> {
            if (ClientProxy.renderHelper.getContext() == null) {
                throw new IllegalStateException("projection cleared by opening the same machine's preview");
            }
        }));
        steps.add(new Step("close preview", 10, mc -> mc.displayGuiScreen(null)));
        addOpenBlueprintPreview("assembly_line");
        steps.add(new Step("other machine preview clears projection", 0, mc -> {
            if (ClientProxy.renderHelper.getContext() != null) {
                throw new IllegalStateException("projection not cleared by opening another machine's preview");
            }
        }));
        steps.add(new Step("close preview", 10, mc -> mc.displayGuiScreen(null)));
        steps.add(new Step("collect server report", 0, mc -> {
            for (String line : serverReport(mc)) {
                report.add("SRV  " + line);
            }
        }));
    }

    // ---------------- 机械投影仪：潜行右键控制器 -> 在该控制器处投影其机器结构 ----------------

    private void buildProjectorScenario() {
        steps.add(new Step("setup world", 20, mc -> {
            new File(mc.mcDataDir, "mmce-test-report.txt").delete();
            for (String c : new String[]{"/time set 6000", "/gamerule doDaylightCycle false", "/gamerule doMobSpawning false", "/weather clear"}) {
                mc.thePlayer.sendChatMessage(c);
            }
        }));
        steps.add(new Step("give projector, blueprint and stick", 40, mc -> {
            mc.thePlayer.sendChatMessage("/give " + mc.thePlayer.getCommandSenderName() + " modularmachinery:machine_projector 1");
            mc.thePlayer.sendChatMessage("/mm-get_blueprint fuel_generator");
            mc.thePlayer.sendChatMessage("/give " + mc.thePlayer.getCommandSenderName() + " minecraft:stick 1");
        }));
        steps.add(new Step("projector registered with name and tooltip", 0, mc -> {
            Item item = GameRegistry.findItem(ModularMachinery.MODID, "machine_projector");
            if (item != MachineProjector.INSTANCE) {
                throw new IllegalStateException("modularmachinery:machine_projector not registered: " + item);
            }
            @SuppressWarnings("unchecked")
            List<String> tooltip = new ItemStack(item).getTooltip(mc.thePlayer, false);
            report.add("INFO projector tooltip " + tooltip + " icon=" + item.getIconFromDamage(0).getIconName());
            if (tooltip.size() < 2 || tooltip.get(0).startsWith("item.") || tooltip.get(1).startsWith("tooltip.")) {
                throw new IllegalStateException("projector name / tooltip not translated: " + tooltip);
            }
        }));

        // A：控制器朝东（只放控制器，玩家在其北侧 3 格）；不潜行右键 -> 打开控制器界面，不投影（与上游一致：需要潜行，否则方块先响应右键）
        steps.add(new Step("fuel_generator controller (east)", 30, mc -> mc.thePlayer.sendChatMessage("/mmce-test controller fuel_generator east")));
        steps.add(new Step("select projector", 5, mc -> select(mc, findHotbar(mc, MachineProjector.INSTANCE))));
        steps.add(new Step("right click controller without sneaking", 10, mc -> aimAndRightClick(mc, "fuel_generator", true)));
        steps.add(Step.waitUntil("controller gui open", 20 * 5, mc -> screenIs(mc, "GuiMachineController")));
        steps.add(new Step("no projection without sneaking", 0, mc -> {
            if (ClientProxy.renderHelper.getContext() != null) {
                throw new IllegalStateException("projection started without sneaking");
            }
        }));
        steps.add(new Step("close gui", 10, mc -> mc.thePlayer.closeScreen()));

        // B：潜行右键控制器 -> 投影按控制器朝向旋转并对齐控制器，不打开界面
        addSneak(true);
        steps.add(new Step("sneak right click controller", 10, mc -> aimAndRightClick(mc, "fuel_generator", true)));
        steps.add(new Step("projection placed on controller", 10, mc -> {
            DynamicMachineRenderContext ctx = ClientProxy.renderHelper.getContext();
            BlockPos attached = ClientProxy.renderHelper.getAttachedPosition();
            if (ctx == null || attached == null) {
                throw new IllegalStateException("projection not started / placed: ctx=" + ctx + " attached=" + attached);
            }
            DynamicMachine machine = ctx.getDisplayedMachine();
            if (!"fuel_generator".equals(machine.getRegistryName().getPath())) {
                throw new IllegalStateException("projecting " + machine.getRegistryName());
            }
            int[] ctrl = controllerPos(mc, "fuel_generator");
            BlockPos expected = new BlockPos(ctrl[0], ctrl[1], ctrl[2]).subtract(MiscUtils.rotateYCCWNorthUntil(ctx.getMoveOffset(), EnumFacing.EAST));
            int missing = ClientProxy.renderHelper.getMissingBlocks().size();
            report.add("INFO projector projection at " + attached + " expected " + expected + " layer=" + ClientProxy.renderHelper.getRenderedLayer() + " missing=" + missing);
            if (!attached.equals(expected) || missing == 0) {
                throw new IllegalStateException("projection not aligned to the east-facing controller");
            }
            String message = StatCollector.translateToLocalFormatted("message.machine_projector.project", machine.getLocalizedName());
            if (!chatContains(mc, message)) {
                throw new IllegalStateException("chat message missing: " + message);
            }
        }));
        steps.add(new Step("no controller gui while sneaking", 0, mc -> {
            if (mc.currentScreen != null) {
                throw new IllegalStateException("screen opened: " + mc.currentScreen);
            }
        }));
        addSneak(false);
        addProjectorOverviewScreenshot("fuel_generator-east-projected");

        // C：站到控制器正面（东侧 3 格）放入蓝图（发电机需要蓝图），木棍自动组装 -> 结构形成，投影随即清除（投影与实际结构一致）
        addMoveNearController(3.5, 0.5);
        steps.add(new Step("select blueprint", 5, mc -> select(mc, findBlueprint(mc, "fuel_generator"))));
        steps.add(new Step("right click with blueprint (insert)", 20, mc -> aimAndRightClick(mc, "fuel_generator", true)));
        steps.add(new Step("select stick", 5, mc -> select(mc, findHotbar(mc, Items.stick))));
        steps.add(new Step("right click with stick (assembly)", 20, mc -> aimAndRightClick(mc, "fuel_generator", true)));
        steps.add(Step.waitUntil("fuel_generator formed", 20 * 20, mc -> formed(mc, "fuel_generator") == Boolean.TRUE));
        steps.add(Step.waitUntil("projection cleared after assembly", 20 * 10, mc -> ClientProxy.renderHelper.getContext() == null));
        addProjectorOverviewScreenshot("fuel_generator-east-assembled");

        // D：对已完整的机器使用投影仪：投影开始后因全部方块已到位而自动清除
        addMoveNearController(3.5, 0.5);
        steps.add(new Step("select projector", 5, mc -> select(mc, findHotbar(mc, MachineProjector.INSTANCE))));
        addSneak(true);
        steps.add(new Step("sneak right click formed controller", 2, mc -> {
            aimAndRightClick(mc, "fuel_generator", true);
            if (ClientProxy.renderHelper.getAttachedPosition() == null) {
                throw new IllegalStateException("projection not placed on the formed machine");
            }
        }));
        steps.add(Step.waitUntil("projection of complete machine cleared", 20 * 5, mc -> ClientProxy.renderHelper.getContext() == null));

        // E：潜行右键普通方块（控制器东北方向的地面）不投影
        steps.add(new Step("sneak right click ground", 10, mc -> aimAndRightClick(mc, "fuel_generator", false)));
        steps.add(new Step("no projection on other blocks", 0, mc -> {
            if (ClientProxy.renderHelper.getContext() != null) {
                throw new IllegalStateException("projection started from a non-controller block");
            }
        }));
        addSneak(false);

        // F：工厂控制器（logic_test 的工厂版，朝东）同样适用：潜行右键 -> 投影 logic_test 并对齐控制器
        steps.add(new Step("logic_test factory controller (east)", 30,
            mc -> mc.thePlayer.sendChatMessage("/mmce-test controller logic_test east factory")));
        steps.add(new Step("select projector", 5, mc -> select(mc, findHotbar(mc, MachineProjector.INSTANCE))));
        addSneak(true);
        steps.add(new Step("sneak right click factory controller", 10, mc -> aimAndRightClick(mc, "logic_test", true)));
        steps.add(new Step("projection placed on factory controller", 10, mc -> {
            DynamicMachineRenderContext ctx = ClientProxy.renderHelper.getContext();
            BlockPos attached = ClientProxy.renderHelper.getAttachedPosition();
            if (ctx == null || attached == null) {
                throw new IllegalStateException("projection not started / placed on the factory controller: ctx=" + ctx + " attached=" + attached);
            }
            if (!"logic_test".equals(ctx.getDisplayedMachine().getRegistryName().getPath())) {
                throw new IllegalStateException("projecting " + ctx.getDisplayedMachine().getRegistryName());
            }
            int[] ctrl = controllerPos(mc, "logic_test");
            BlockPos expected = new BlockPos(ctrl[0], ctrl[1], ctrl[2]).subtract(MiscUtils.rotateYCCWNorthUntil(ctx.getMoveOffset(), EnumFacing.EAST));
            report.add("INFO factory projection at " + attached + " expected " + expected + " missing=" + ClientProxy.renderHelper.getMissingBlocks().size());
            if (!attached.equals(expected) || mc.currentScreen != null) {
                throw new IllegalStateException("factory projection not aligned, or a screen opened: " + mc.currentScreen);
            }
        }));
        steps.add(new Step("screenshot logic_test-factory-east-projected", 10, mc -> {
            lookAtController(mc, "logic_test");
            screenshot("logic_test-factory-east-projected");
        }));
        addSneak(false);
        steps.add(new Step("collect server report", 0, mc -> {
            for (String line : serverReport(mc)) {
                report.add("SRV  " + line);
            }
        }));
    }

    /**
     * 对准目标并在同一 tick 内刷新准星目标（EntityRenderer#getMouseOver）、核对后真实右键：
     * onController 为 true 时对准控制器中心，否则对准玩家东北方的地面。
     * 游戏窗口处于前台时，关闭界面会重新捕获鼠标（Minecraft#setIngameFocus），真实鼠标的位移会改变视角；
     * 因此不依赖前面步骤设置的视角，并把与自动化上次设置的视角不一致（外部输入造成的偏移）写进报告。
     */
    private void aimAndRightClick(Minecraft mc, String machine, boolean onController) throws ReflectiveOperationException {
        float yaw = mc.thePlayer.rotationYaw;
        float pitch = mc.thePlayer.rotationPitch;
        if (!Float.isNaN(lastLookYaw) && (Math.abs(MathHelper.wrapAngleTo180_float(yaw - lastLookYaw)) > 1F || Math.abs(pitch - lastLookPitch) > 1F)) {
            report.add("INFO view drifted since last aim: yaw " + lastLookYaw + " -> " + yaw + ", pitch " + lastLookPitch + " -> " + pitch);
        }
        int[] c = controllerPos(mc, machine);
        if (onController) {
            lookAtController(mc, machine);
        } else {
            lookAt(mc, mc.thePlayer.posX + 2.3, mc.thePlayer.posY - 1.62 - 0.5, mc.thePlayer.posZ - 3.1);
        }
        mc.entityRenderer.getMouseOver(1.0F);
        MovingObjectPosition mop = mc.objectMouseOver;
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            throw new IllegalStateException("crosshair not on a block: " + mop);
        }
        boolean hitController = mop.blockX == c[0] && mop.blockY == c[1] && mop.blockZ == c[2];
        if (hitController != onController) {
            throw new IllegalStateException("crosshair on " + mop.blockX + "," + mop.blockY + "," + mop.blockZ
                + " (" + mc.theWorld.getBlock(mop.blockX, mop.blockY, mop.blockZ).getUnlocalizedName() + "), controller at " + c[0] + "," + c[1] + "," + c[2]);
        }
        rightClickMouse(mc);
    }

    /** 传送到燃料发电机控制器旁（水平偏移 dx / dz，脚下与玩家原来的地面同高）。 */
    private void addMoveNearController(double dx, double dz) {
        steps.add(new Step("move near controller " + dx + "," + dz, 20, mc -> {
            int[] c = controllerPos(mc, "fuel_generator");
            mc.thePlayer.sendChatMessage("/tp " + mc.thePlayer.getCommandSenderName() + " " + (c[0] + dx) + " " + (c[1] - 1) + " " + (c[2] + dz));
        }));
    }

    /** 从东北侧地面看整台机器并截图（不改变投影本身；对准与截图请求在同一步内）。 */
    private void addProjectorOverviewScreenshot(String name) {
        addMoveNearController(4.5, -3.5);
        steps.add(new Step("screenshot " + name, 10, mc -> {
            int[] c = controllerPos(mc, "fuel_generator");
            lookAt(mc, c[0] - 1.0, c[1] - 0.5, c[2] + 0.5);
            screenshot(name);
        }));
    }

    /** 按下 / 松开潜行键（游戏内 KeyBinding 状态，不注入系统键盘），等客户端与集成服务器上的玩家都同步到该潜行状态。 */
    private void addSneak(boolean down) {
        steps.add(new Step(down ? "press sneak key" : "release sneak key", 2,
            mc -> KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), down)));
        steps.add(Step.waitUntil(down ? "sneaking (client and server)" : "not sneaking (client and server)", 20 * 5,
            mc -> mc.thePlayer.isSneaking() == down && serverPlayer(mc).isSneaking() == down));
    }

    private static EntityPlayerMP serverPlayer(Minecraft mc) {
        return mc.getIntegratedServer().getConfigurationManager().func_152612_a(mc.thePlayer.getCommandSenderName());
    }

    /** 聊天栏（GuiNewChat 的完整消息列表）中是否有该文本的消息。 */
    private static boolean chatContains(Minecraft mc, String text) throws ReflectiveOperationException {
        Field f = null;
        for (String name : new String[]{"chatLines", "field_146252_h"}) {
            try {
                f = net.minecraft.client.gui.GuiNewChat.class.getDeclaredField(name);
                break;
            } catch (NoSuchFieldException ignored) {
            }
        }
        if (f == null) {
            throw new NoSuchFieldException("GuiNewChat#chatLines");
        }
        f.setAccessible(true);
        for (Object line : (List<?>) f.get(mc.ingameGUI.getChatGUI())) {
            if (text.equals(((ChatLine) line).func_151461_a().getUnformattedText())) {
                return true;
            }
        }
        return false;
    }

    /** 聊天栏中是否有包含该文本的消息（不区分游戏语言时用数字部分匹配）。 */
    private static boolean chatHasPart(Minecraft mc, String part) {
        try {
            Field f = null;
            for (String name : new String[]{"chatLines", "field_146252_h"}) {
                try {
                    f = net.minecraft.client.gui.GuiNewChat.class.getDeclaredField(name);
                    break;
                } catch (NoSuchFieldException ignored) {
                }
            }
            if (f == null) {
                return false;
            }
            f.setAccessible(true);
            for (Object line : (List<?>) f.get(mc.ingameGUI.getChatGUI())) {
                if (((ChatLine) line).func_151461_a().getUnformattedText().contains(part)) {
                    return true;
                }
            }
            return false;
        } catch (IllegalAccessException e) {
            return false;
        }
    }

    /** 抬头看天，选中对应蓝图，真实右键（RIGHT_CLICK_AIR -> 使用物品）打开预览界面。 */
    private void addOpenBlueprintPreview(String machine) {
        steps.add(new Step("look at sky", 3, mc -> look(mc, mc.thePlayer.rotationYaw, -80F)));
        steps.add(new Step("select blueprint " + machine, 3, mc -> {
            int slot = findBlueprint(mc, machine);
            if (slot < 0) {
                throw new IllegalStateException("blueprint of " + machine + " not in hotbar");
            }
            select(mc, slot);
        }));
        steps.add(new Step("right click blueprint " + machine, 5, DevClientAutomation::rightClickMouse));
        steps.add(Step.waitUntil("blueprint preview " + machine + " open", 20 * 5,
            mc -> mc.currentScreen instanceof GuiScreenBlueprint gui && machine.equals(gui.getMachine().getRegistryName().getPath())));
    }

    private static GuiScreenBlueprint blueprintGui(Minecraft mc) {
        if (!(mc.currentScreen instanceof GuiScreenBlueprint gui)) {
            throw new IllegalStateException("blueprint preview not open: " + mc.currentScreen);
        }
        return gui;
    }

    /** 与鼠标点击按钮相同：GuiScreen#mouseClicked（按钮命中判定 -> actionPerformed）。 */
    private static void clickBlueprintButton(Minecraft mc, int id) throws ReflectiveOperationException {
        GuiScreenBlueprint gui = blueprintGui(mc);
        GuiButton button = gui.getButton(id);
        if (button == null || !button.visible || !button.enabled) {
            throw new IllegalStateException("button " + id + " not clickable");
        }
        Method m = findMethod(GuiScreen.class, new String[]{"mouseClicked", "func_73864_a"}, int.class, int.class, int.class);
        m.invoke(gui, button.xPosition + button.width / 2, button.yPosition + button.height / 2, 0);
    }

    /** 与真实的鼠标右键相同：Minecraft#func_147121_ag（按准星目标触发 RIGHT_CLICK_BLOCK / RIGHT_CLICK_AIR 事件，再交互或使用物品）。 */
    private static void rightClickMouse(Minecraft mc) throws ReflectiveOperationException {
        findMethod(Minecraft.class, new String[]{"func_147121_ag", "rightClickMouse"}).invoke(mc);
    }

    private static Method findMethod(Class<?> owner, String[] names, Class<?>... params) throws NoSuchMethodException {
        for (String name : names) {
            try {
                Method m = owner.getDeclaredMethod(name, params);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {
            }
        }
        throw new NoSuchMethodException(owner.getName() + "#" + names[0]);
    }

    private static int findBlueprint(Minecraft mc, String machine) {
        for (int i = 0; i < 9; i++) {
            ItemStack s = mc.thePlayer.inventory.mainInventory[i];
            if (s != null && s.getItem() == ItemsMM.blueprint) {
                DynamicMachine m = ItemBlueprint.getAssociatedMachine(s);
                if (m != null && machine.equals(m.getRegistryName().getPath())) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static void select(Minecraft mc, int slot) {
        if (slot < 0) {
            throw new IllegalStateException("item not in hotbar");
        }
        mc.thePlayer.inventory.currentItem = slot;
        mc.playerController.updateController();
    }

    /** 让视线（客户端玩家 posY 即眼睛高度）对准某点。 */
    private static void lookAt(Minecraft mc, double x, double y, double z) {
        double dx = x - mc.thePlayer.posX;
        double dy = y - mc.thePlayer.posY;
        double dz = z - mc.thePlayer.posZ;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        look(mc, yaw, pitch);
    }

    private static void lookAtController(Minecraft mc, String machine) {
        int[] pos = controllerPos(mc, machine);
        if (pos == null) {
            throw new IllegalStateException("controller of " + machine + " not placed");
        }
        lookAt(mc, pos[0] + 0.5, pos[1] + 0.5, pos[2] + 0.5);
    }

    /** 集成服务器上该玩家记录的投影动态结构尺寸（自动组装使用的值）。 */
    private void expectServerProjectionSize(Minecraft mc, String machine, int expected) {
        EntityPlayerMP serverPlayer = serverPlayer(mc);
        DynamicMachine m = MachineRegistry.getRegistry().getMachine(new ResourceLocation(ModularMachinery.MODID, machine));
        int size = AssemblyEventHandler.getProjectionPatternSize(serverPlayer, m);
        report.add("INFO server projection size " + machine + " = " + size);
        if (size != expected) {
            throw new IllegalStateException("server projection size " + size + " != " + expected);
        }
    }

    private interface StackSupplier {
        ItemStack get();
    }

    /** 打开 NEI（R = 合成 / U = 用途），切到指定处理器页并截图；expectedMachine 不为 null 时核对结构页中的机器。 */
    private void addNeiPage(String name, boolean crafting, StackSupplier stack, String handlerId, String expectedMachine) {
        steps.add(new Step("nei " + (crafting ? "R " : "U ") + name, 20, mc -> {
            ItemStack s = stack.get();
            if (s == null) {
                throw new IllegalStateException("item not found");
            }
            int recipes = NeiHooks.openAndSelect(crafting, s, handlerId);
            report.add("INFO nei " + name + " handler=" + handlerId + " recipes=" + recipes);
            if (recipes <= 0) {
                throw new IllegalStateException("handler " + handlerId + " has no recipes for " + s);
            }
            if (expectedMachine != null && !NeiHooks.structureContains(expectedMachine)) {
                throw new IllegalStateException("structure page does not contain " + expectedMachine);
            }
        }));
        steps.add(new Step("screenshot " + name, 10, mc -> screenshot(name)));
    }

    private static ItemStack controller(String machine) {
        Item item = GameRegistry.findItem(ModularMachinery.MODID, machine + "_controller");
        return item == null ? null : new ItemStack(item);
    }

    private static ItemStack stack(String modId, String name, int meta) {
        Item item = GameRegistry.findItem(modId, name);
        return item == null ? null : new ItemStack(item, 1, meta);
    }

    // ---------------- P2：方块渲染、结构形成/拆解、GUI、物品/流体/能量进出 ----------------

    /** p2：离开装配机去看发电机前的位置与视角（x、脚下 y、z、yaw、pitch）。 */
    private double[] savedPlayerPos;

    private static final String[] P2_GIVES = {
        "modularmachinery:fuel_generator_controller 1", "modularmachinery:mek_eio_assembler_controller 1",
        "modularmachinery:blockcasing 64 0", "modularmachinery:blockcasing 64 2", "modularmachinery:blockinputbus 4 0",
        "modularmachinery:blockfluidinputhatch 4 0", "modularmachinery:blockenergyinputhatch 4 0", "modularmachinery:itemmodularium 16"
    };

    private void buildP2Scenario() {
        steps.add(new Step("setup world", 20, mc -> {
            new File(mc.mcDataDir, "mmce-test-report.txt").delete();
            for (String c : new String[]{"/time set 6000", "/gamerule doDaylightCycle false", "/gamerule doMobSpawning false", "/weather clear"}) {
                mc.thePlayer.sendChatMessage(c);
            }
        }));
        steps.add(new Step("give mmce items", 40, mc -> {
            for (String g : P2_GIVES) {
                mc.thePlayer.sendChatMessage("/give " + mc.thePlayer.getCommandSenderName() + " " + g);
            }
        }));
        steps.add(new Step("screenshot hotbar", 10, mc -> {
            mc.thePlayer.inventory.currentItem = 8;
            look(mc, 0F, 10F);
            screenshot("hotbar");
        }));
        addMachineChecks("fuel_generator", new int[][]{{-1, 0, 1, 2}, {1, 0, 1, 2}, {0, 0, 2, 2}},
            new String[]{"GuiContainerItemBus", "GuiContainerFluidHatch", "GuiContainerEnergyHatch"});
        steps.add(new Step("move east", 30, mc -> mc.thePlayer.sendChatMessage("/tp " + mc.thePlayer.getCommandSenderName() + " ~8 ~ ~")));
        addMachineChecks("mek_eio_assembler", new int[][]{{-1, 0, 0, 2}, {0, 1, 1, 2}},
            new String[]{"GuiContainerItemBus", "GuiContainerEnergyHatch"});

        // 投料后运行：装配机 100 t，发电机 200/400 t
        steps.add(new Step("fill machines", 300, mc -> {
            mc.thePlayer.sendChatMessage("/mmce-test fill mek_eio_assembler");
            mc.thePlayer.sendChatMessage("/mmce-test fill fuel_generator");
        }));
        steps.add(new Step("status after run", 20, mc -> {
            mc.thePlayer.sendChatMessage("/mmce-test status mek_eio_assembler");
            mc.thePlayer.sendChatMessage("/mmce-test status fuel_generator");
        }));
        steps.add(new Step("assembler produced capacitors", 0, mc -> expectStatus(mc, "mek_eio_assembler", "out=2xEnderIO:itemBasicCapacitor@0")));
        steps.add(new Step("generator produced energy", 0, mc -> expectStatus(mc, "fuel_generator", "energy[out]=")));
        // 能量仓储能提示：带单位；未按 Shift 时缩写并提示按 Shift，按 Shift 时显示完整数值（不注入真实按键，直接取两种提示文字）
        // 此时玩家在东边 8 格的装配机旁，离发电机超过 6 格时界面会被关闭（G-2）：先回到发电机旁，结束后再回去
        steps.add(new Step("move back west to the generator", 30, mc -> {
            savedPlayerPos = new double[]{mc.thePlayer.posX, mc.thePlayer.boundingBox.minY, mc.thePlayer.posZ, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch};
            mc.thePlayer.sendChatMessage("/tp " + mc.thePlayer.getCommandSenderName() + " ~-8 ~ ~");
        }));
        steps.add(new Step("open generator energy output hatch", 10, mc -> rightClick(mc, "fuel_generator", 0, 0, 2, 2)));
        steps.add(Step.waitUntil("energy hatch gui open", 20 * 5, mc -> screenIs(mc, "GuiContainerEnergyHatch")));
        steps.add(new Step("energy hatch: server / client stored energy", 0, mc -> report.add("INFO energy hatch server="
            + serverEnergy(mc, "fuel_generator", 0, 0, 2) + " client=" + clientEnergy(mc, "fuel_generator", 0, 0, 2))));
        steps.add(Step.waitUntil("energy hatch: client shows the server's stored energy (> 0)", 20 * 5, mc -> {
            long server = serverEnergy(mc, "fuel_generator", 0, 0, 2);
            long client = clientEnergy(mc, "fuel_generator", 0, 0, 2);
            return server > 0 && Math.abs(server - client) <= 200;
        }));
        steps.add(new Step("energy tooltip: unit, abbreviation + shift hint, exact value", 0, mc -> checkEnergyTooltip(mc)));
        steps.add(new Step("screenshot energy tooltip (abbreviated)", 10, mc -> {
            TooltipOverlay.lines = energyTooltip(mc, false);
            screenshot("energy-tooltip");
        }));
        steps.add(new Step("screenshot energy tooltip (shift)", 10, mc -> {
            TooltipOverlay.lines = energyTooltip(mc, true);
            screenshot("energy-tooltip-shift");
        }));
        steps.add(new Step("close gui", 10, mc -> {
            TooltipOverlay.lines = null;
            mc.thePlayer.closeScreen();
        }));
        // Waila：站到发电机背后（能量输出仓在控制器后方 2 格），准星对准能量仓，显示“当前 / 上限 单位”与 Shift 提示
        steps.add(new Step("stand behind the generator", 30, mc -> {
            int[] c = controllerPos(mc, "fuel_generator");
            mc.thePlayer.sendChatMessage("/tp " + mc.thePlayer.getCommandSenderName() + " " + (c[0] + 0.5) + " " + c[1] + " " + (c[2] + 4.5));
        }));
        steps.add(new Step("look at the energy output hatch", 30, mc -> {
            int[] c = controllerPos(mc, "fuel_generator");
            lookAt(mc, c[0] + 0.5, c[1] + 0.5, c[2] + 2.5);
        }));
        steps.add(new Step("screenshot waila-energy-hatch", 10, mc -> screenshot("waila-energy-hatch")));
        steps.add(new Step("back to the assembler (saved position)", 30, mc -> mc.thePlayer.sendChatMessage(String.format(java.util.Locale.ROOT,
            "/tp %s %.3f %.3f %.3f", mc.thePlayer.getCommandSenderName(), savedPlayerPos[0], savedPlayerPos[1], savedPlayerPos[2]))));
        steps.add(new Step("restore view", 5, mc -> look(mc, (float) savedPlayerPos[3], (float) savedPlayerPos[4])));
        steps.add(new Step("open assembler output bus", 10, mc -> rightClick(mc, "mek_eio_assembler", 1, 0, 0, 2)));
        steps.add(Step.waitUntil("output bus gui open", 20 * 5, mc -> screenIs(mc, "GuiContainerItemBus")));
        steps.add(new Step("screenshot assembler-output", 10, mc -> screenshot("assembler-output")));
        steps.add(new Step("close gui", 10, mc -> mc.thePlayer.closeScreen()));
        steps.add(new Step("open assembler controller", 10, mc -> rightClick(mc, "mek_eio_assembler", 0, 0, 0, 2)));
        steps.add(Step.waitUntil("controller gui open", 20 * 5, mc -> screenIs(mc, "GuiMachineController")));
        // 打开控制器 GUI 期间服务端每 15 tick 发送性能报告（GUI 中的 Avg μs/t 与工作模式）
        steps.add(Step.waitUntil("performance report received", 20 * 5, mc -> TileMultiblockMachineController.usedTimeCache > 0));
        steps.add(new Step("performance report values", 0, mc -> report.add("INFO performance report: " + TileMultiblockMachineController.usedTimeCache
            + " us/t, search " + TileMultiblockMachineController.searchUsedTimeCache + " us, mode " + TileMultiblockMachineController.workModeCache)));
        steps.add(new Step("screenshot assembler-controller-after-run", 10, mc -> screenshot("assembler-controller-after-run")));
        steps.add(new Step("close gui", 10, mc -> mc.thePlayer.closeScreen()));

        // 拆掉一块外壳 -> 解除形成；放回 -> 恢复
        steps.add(new Step("break casing", 10, mc -> mc.thePlayer.sendChatMessage("/mmce-test break mek_eio_assembler")));
        steps.add(Step.waitUntil("structure broken", 20 * 20, mc -> formed(mc, "mek_eio_assembler") == Boolean.FALSE));
        steps.add(new Step("screenshot broken", 10, mc -> screenshot("assembler-broken")));
        steps.add(new Step("repair casing", 10, mc -> mc.thePlayer.sendChatMessage("/mmce-test repair mek_eio_assembler")));
        steps.add(Step.waitUntil("structure re-formed", 20 * 20, mc -> formed(mc, "mek_eio_assembler") == Boolean.TRUE));
        addConstructToolChecks();
        steps.add(new Step("collect server report", 0, mc -> {
            for (String line : serverReport(mc)) {
                report.add("SRV  " + line);
            }
        }));
    }

    /** 搭建机器 -> 等待形成 -> 截图 -> 打开控制器与各仓 GUI 并截图。parts：{dx, dy, dz, 点击面}，相对控制器。 */
    /** 装配机控制器左右两格与下面一排（相对控制器的偏移）。 */
    private static final int[][] CONSTRUCT_SELECTION = {{-1, 0, 0}, {1, 0, 0}, {-1, -1, 0}, {0, -1, 0}, {1, -1, 0}};

    /**
     * 构造工具（创造模式）选区：潜行右键方块加入选区，客户端拿着工具时在世界中画半透明白框（RenderWorldLastEvent）；
     * 以及 F3 右侧追加的“方块 meta”行（RenderGameOverlayEvent.Text）。最后再次点击把选区清空。
     */
    private void addConstructToolChecks() {
        steps.add(new Step("give construct tool", 20,
            mc -> mc.thePlayer.sendChatMessage("/give " + mc.thePlayer.getCommandSenderName() + " modularmachinery:itemconstructtool 1")));
        steps.add(new Step("construct tool into hotbar", 10, mc -> moveToHotbar(mc, ItemsMM.constructTool)));
        addSneak(true);
        steps.add(new Step("select blocks with construct tool", 10, mc -> toggleConstructSelection(mc, "mek_eio_assembler")));
        addSneak(false);
        steps.add(Step.waitUntil("client selection synced", 20 * 5, mc -> constructSelectionSize() == CONSTRUCT_SELECTION.length));
        steps.add(new Step("screenshot construct-selection", 10, mc -> {
            mc.thePlayer.inventory.currentItem = findHotbar(mc, ItemsMM.constructTool);
            lookAtController(mc, "mek_eio_assembler");
            screenshot("construct-selection");
        }));
        steps.add(new Step("show debug screen", 10, mc -> mc.gameSettings.showDebugInfo = true));
        steps.add(new Step("screenshot f3-meta", 10, mc -> screenshot("f3-meta")));
        steps.add(new Step("hide debug screen", 5, mc -> mc.gameSettings.showDebugInfo = false));
        addSneak(true);
        steps.add(new Step("clear construct selection", 10, mc -> toggleConstructSelection(mc, "mek_eio_assembler")));
        addSneak(false);
        steps.add(Step.waitUntil("client selection cleared", 20 * 5, mc -> constructSelectionSize() == 0));
    }

    /** 把背包中的物品换到快捷栏最后一格（装有匠魂时首次进入会获得手册，可能占用快捷栏空位，/give 的物品会进背包）。 */
    private static void moveToHotbar(Minecraft mc, Item item) {
        moveToHotbar(mc, item, 8);
    }

    private static void moveToHotbar(Minecraft mc, Item item, int hotbarSlot) {
        if (findHotbar(mc, item) >= 0) {
            return;
        }
        ItemStack[] inv = mc.thePlayer.inventory.mainInventory;
        for (int i = 9; i < inv.length; i++) {
            if (inv[i] != null && inv[i].getItem() == item) {
                // 通过容器点击交换（服务端同步）：玩家背包容器中，主背包 9–35 对应槽 9–35，快捷栏 0–8 对应 36–44
                mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, i, hotbarSlot, 2, mc.thePlayer);
                return;
            }
        }
    }

    private static void toggleConstructSelection(Minecraft mc, String machine) {
        int slot = findHotbar(mc, ItemsMM.constructTool);
        int[] c = controllerPos(mc, machine);
        if (slot < 0 || c == null) {
            throw new IllegalStateException("construct tool not in hotbar or " + machine + " not built");
        }
        mc.thePlayer.inventory.currentItem = slot;
        mc.playerController.updateController();
        for (int[] o : CONSTRUCT_SELECTION) {
            int x = c[0] + o[0];
            int y = c[1] + o[1];
            int z = c[2] + o[2];
            mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getCurrentEquippedItem(), x, y, z, 2,
                Vec3.createVectorHelper(x + 0.5, y + 0.5, z));
        }
    }

    private static int constructSelectionSize() {
        PlayerStructureSelectionHelper.StructureSelection sel = PlayerStructureSelectionHelper.clientSelection;
        return sel == null ? 0 : sel.getSelectedPositions().size();
    }

    private void addMachineChecks(String machine, int[][] parts, String[] guiNames) {
        steps.add(new Step("face south", 5, mc -> look(mc, 0F, 25F)));
        steps.add(new Step("build " + machine, 40, mc -> mc.thePlayer.sendChatMessage("/mmce-test build " + machine)));
        steps.add(Step.waitUntil(machine + " formed", 20 * 20, mc -> formed(mc, machine) == Boolean.TRUE));
        steps.add(new Step("screenshot " + machine, 10, mc -> screenshot(machine + "-formed")));
        steps.add(new Step("open " + machine + " controller", 10, mc -> rightClick(mc, machine, 0, 0, 0, 2)));
        steps.add(Step.waitUntil(machine + " controller gui open", 20 * 5, mc -> screenIs(mc, "GuiMachineController")));
        steps.add(new Step("screenshot " + machine + "-controller", 10, mc -> screenshot(machine + "-controller")));
        steps.add(new Step("close gui", 10, mc -> mc.thePlayer.closeScreen()));
        for (int i = 0; i < parts.length; i++) {
            int[] part = parts[i];
            String gui = guiNames[i];
            steps.add(new Step("open " + machine + " " + gui, 10, mc -> rightClick(mc, machine, part[0], part[1], part[2], part[3])));
            steps.add(Step.waitUntil(machine + " " + gui + " open", 20 * 5, mc -> screenIs(mc, gui)));
            steps.add(new Step("screenshot " + machine + "-" + gui, 10, mc -> screenshot(machine + "-" + gui)));
            steps.add(new Step("close gui", 10, mc -> mc.thePlayer.closeScreen()));
        }
    }

    /**
     * 自动化期间不让游戏窗口抓住鼠标（否则桌面上的真实鼠标移动会转动视角，截图对不准），
     * 并把视角保持在最近一次 {@link #look} 设置的方向。
     */
    private static void holdView(Minecraft mc) {
        if (mc.inGameHasFocus) {
            // 不用 setIngameNotInFocus()：它会 KeyBinding.unPressAllKeys()，清掉场景里设置的潜行等按键状态
            mc.inGameHasFocus = false;
            mc.mouseHelper.ungrabMouseCursor();
        }
        if (!Float.isNaN(lastLookYaw)) {
            mc.thePlayer.rotationYaw = lastLookYaw;
            mc.thePlayer.prevRotationYaw = lastLookYaw;
            mc.thePlayer.rotationPitch = lastLookPitch;
            mc.thePlayer.prevRotationPitch = lastLookPitch;
        }
    }

    /** 自动化最近一次设置的视角，用于发现外部输入造成的视角偏移。 */
    private static float lastLookYaw = Float.NaN;
    private static float lastLookPitch = Float.NaN;

    private static void look(Minecraft mc, float yaw, float pitch) {
        mc.thePlayer.rotationYaw = yaw;
        mc.thePlayer.prevRotationYaw = yaw;
        mc.thePlayer.rotationPitch = pitch;
        mc.thePlayer.prevRotationPitch = pitch;
        lastLookYaw = yaw;
        lastLookPitch = pitch;
    }

    private static boolean screenIs(Minecraft mc, String simpleName) {
        return mc.currentScreen != null && mc.currentScreen.getClass().getSimpleName().equals(simpleName);
    }

    private static List<String> serverReport(Minecraft mc) {
        File file = new File(mc.mcDataDir, "mmce-test-report.txt");
        try {
            return file.isFile() ? Files.readAllLines(file.toPath(), StandardCharsets.UTF_8) : new ArrayList<>();
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    /** 从服务端报告中解析最近一次搭建的控制器坐标。 */
    private static int[] controllerPos(Minecraft mc, String machine) {
        int[] pos = null;
        String prefix = "OK   build " + ModularMachinery.MODID + ":" + machine + " controller=BlockPos{";
        String prefix2 = "OK   controller " + ModularMachinery.MODID + ":" + machine + " controller=BlockPos{";
        for (String line : serverReport(mc)) {
            if (line.startsWith(prefix) || line.startsWith(prefix2)) {
                String inner = line.substring(line.indexOf('{') + 1, line.indexOf('}'));
                String[] parts = inner.split(",");
                pos = new int[3];
                for (int i = 0; i < 3; i++) {
                    pos[i] = Integer.parseInt(parts[i].substring(parts[i].indexOf('=') + 1).trim());
                }
            }
        }
        return pos;
    }

    /** 客户端同步到的控制器形成状态；控制器尚不存在时返回 null。 */
    private static Boolean formed(Minecraft mc, String machine) {
        int[] pos = controllerPos(mc, machine);
        if (pos == null) {
            return null;
        }
        TileEntity te = mc.theWorld.getTileEntity(pos[0], pos[1], pos[2]);
        return te instanceof TileMultiblockMachineController ? ((TileMultiblockMachineController) te).isStructureFormed() : null;
    }

    private static void rightClick(Minecraft mc, String machine, int dx, int dy, int dz, int side) {
        int[] pos = controllerPos(mc, machine);
        if (pos == null) {
            throw new IllegalStateException("controller of " + machine + " not built");
        }
        int x = pos[0] + dx;
        int y = pos[1] + dy;
        int z = pos[2] + dz;
        mc.thePlayer.inventory.currentItem = 8;
        mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getCurrentEquippedItem(), x, y, z, side,
            Vec3.createVectorHelper(x + 0.5, y + 0.5, z));
    }

    private static void expectStatus(Minecraft mc, String machine, String expected) {
        String last = null;
        for (String line : serverReport(mc)) {
            if (line.startsWith("INFO status " + ModularMachinery.MODID + ":" + machine + " ")) {
                last = line;
            }
        }
        if (last == null || !last.contains(expected)) {
            throw new IllegalStateException("expected '" + expected + "' in status, got: " + last);
        }
    }

    private void neiSearch(String text) {
        if (!Loader.isModLoaded("NotEnoughItems")) {
            report.add("SKIP nei search (NEI not loaded): " + text);
            return;
        }
        NeiHooks.setSearchText(text);
    }

    private boolean neiPanelHasItems() {
        return !Loader.isModLoaded("NotEnoughItems") || NeiHooks.visibleItemCount() > 0;
    }

    private void neiDiagnostics() {
        if (Loader.isModLoaded("NotEnoughItems")) {
            report.add("INFO nei " + NeiHooks.diagnostics());
        }
    }

    private void screenshot(String name) {
        // 装有 Angelica（及启用光影）时截图名带标记，避免覆盖原版渲染下的同名截图；
        // Java 9+（runClient17 等）再加 -java<主版本>，-Dmmce.dev.shottag=<标记>（client-autotest.sh 的 SHOT_TAG）再加 -<标记>
        String tag = !Loader.isModLoaded("angelica") ? "" : shaderPackInUse() ? "-angelica-shaders" : "-angelica";
        String spec = System.getProperty("java.specification.version", "1.8");
        if (!spec.startsWith("1.")) {
            tag += "-java" + spec;
        }
        String extra = System.getProperty("mmce.dev.shottag", "");
        if (!extra.isEmpty()) {
            tag += "-" + extra;
        }
        pendingScreenshot = scenario + tag + "-" + name + ".png";
    }

    /** Angelica 内置 Iris 的公开接口 IrisApi（v0）：当前是否有光影包在用。未安装 Angelica 或查询失败时返回 false。 */
    private static boolean shaderPackInUse() {
        try {
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object instance = api.getMethod("getInstance").invoke(null);
            return (Boolean) api.getMethod("isShaderPackInUse").invoke(instance);
        } catch (ReflectiveOperationException | LinkageError e) {
            return false;
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();

        if (!worldLaunched) {
            if (mc.currentScreen instanceof GuiMainMenu) {
                launchFreshWorld(mc);
            }
            return;
        }
        if (mc.thePlayer == null || mc.theWorld == null || pendingScreenshot != null) {
            return;
        }
        holdView(mc);
        // 窗口失去焦点时单人游戏会打开暂停菜单并暂停集成服务器，自动化期间直接关闭它
        if (mc.currentScreen instanceof GuiIngameMenu) {
            mc.displayGuiScreen(null);
        }
        if (waitTicks > 0) {
            waitTicks--;
            return;
        }
        Step step = steps.poll();
        if (step == null) {
            finish(mc);
            return;
        }
        try {
            if (step.condition != null) {
                if (!step.condition.test(mc)) {
                    step.remainingTicks -= 5;
                    if (step.remainingTicks > 0) {
                        steps.addFirst(step);
                        waitTicks = 5;
                        return;
                    }
                    report.add("FAIL " + step.name + ": timed out");
                    waitTicks = step.waitAfter;
                    return;
                }
            } else {
                step.action.run(mc);
            }
            report.add("OK   " + step.name);
        } catch (Throwable t) {
            report.add("FAIL " + step.name + ": " + t);
            ModularMachinery.log.error("[AutoTest] Step failed: " + step.name, t);
        }
        waitTicks = step.waitAfter;
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase == TickEvent.Phase.START) {
            if (worldLaunched && mc.thePlayer != null) {
                holdView(mc);
            }
            return;
        }
        if (pendingScreenshot == null) {
            return;
        }
        ScreenShotHelper.saveScreenshot(mc.mcDataDir, pendingScreenshot, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        report.add("SHOT screenshots/" + pendingScreenshot);
        pendingScreenshot = null;
    }

    private void launchFreshWorld(Minecraft mc) {
        worldLaunched = true;
        // 自动化期间不因窗口失去焦点而暂停（只改内存中的设置，不写入 options.txt）
        mc.gameSettings.pauseOnLostFocus = false;
        // 仅删除自动测试专用的存档目录，保证每次运行从同一初始状态开始
        mc.getSaveLoader().flushCache();
        mc.getSaveLoader().deleteWorldDirectory(WORLD_FOLDER);
        WorldSettings settings = new WorldSettings(20261018L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT);
        settings.enableCommands();
        mc.launchIntegratedServer(WORLD_FOLDER, "MMCE AutoTest", settings);
        waitTicks = 60;
        report.add("OK   launch world " + WORLD_FOLDER);
        ModContainer angelica = Loader.instance().getIndexedModList().get("angelica");
        report.add("INFO renderer " + (angelica == null ? "vanilla" : "angelica " + angelica.getVersion()));
        report.add("INFO java " + System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + "), lwjgl " + org.lwjgl.Sys.getVersion()
            + ", GL " + org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_VERSION) + " / " + org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_RENDERER));
    }

    private void finish(Minecraft mc) {
        FMLCommonHandler.instance().bus().unregister(this);
        File dir = new File(mc.mcDataDir, "mmce-autotest");
        dir.mkdirs();
        File out = new File(dir, scenario + "-report.txt");
        if (Loader.isModLoaded("angelica")) {
            report.add("INFO shader pack in use " + shaderPackInUse());
        }
        if (!MachineComponentRenderer.WORLD_RENDER_THREADS.isEmpty()) {
            report.add("INFO machine ISBRH threads " + new java.util.TreeSet<>(MachineComponentRenderer.WORLD_RENDER_THREADS));
        }
        long failed = report.stream().filter(s -> s.startsWith("FAIL")).count();
        report.add("result " + (failed == 0 ? "PASS" : "FAIL (" + failed + " failed steps)"));
        try (Writer w = new OutputStreamWriter(Files.newOutputStream(out.toPath()), StandardCharsets.UTF_8)) {
            for (String line : report) {
                w.write(line);
                w.write(System.lineSeparator());
            }
        } catch (IOException e) {
            ModularMachinery.log.error("[AutoTest] Failed to write report " + out, e);
        }
        ModularMachinery.log.info("[AutoTest] Scenario {} finished, report: {}", scenario, out);
        if (exitWhenDone) {
            mc.shutdown();
        }
    }

    private interface StepAction {
        void run(Minecraft mc) throws Exception;
    }

    private interface StepCondition {
        boolean test(Minecraft mc);
    }

    private static final class Step {
        final String name;
        final int waitAfter;
        final StepAction action;
        final StepCondition condition;
        int remainingTicks;

        Step(String name, int waitAfter, StepAction action) {
            this(name, waitAfter, action, null, 0);
        }

        private Step(String name, int waitAfter, StepAction action, StepCondition condition, int timeoutTicks) {
            this.name = name;
            this.waitAfter = waitAfter;
            this.action = action;
            this.condition = condition;
            this.remainingTicks = timeoutTicks;
        }

        static Step waitUntil(String name, int timeoutTicks, StepCondition condition) {
            return new Step(name, 10, null, condition, timeoutTicks);
        }
    }
}
