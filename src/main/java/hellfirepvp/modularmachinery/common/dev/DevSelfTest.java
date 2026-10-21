package hellfirepvp.modularmachinery.common.dev;

import com.github.bsideup.jabel.Desugar;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import github.kasuminova.mmce.compat.BlockPos;
import github.kasuminova.mmce.compat.ResourceLocation;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.lib.BlocksMM;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import github.kasuminova.mmce.common.upgrade.MachineUpgrade;
import github.kasuminova.mmce.compat.EnumFacing;
import github.kasuminova.mmce.compat.ItemStacks;
import github.kasuminova.mmce.compat.Worlds;
import github.kasuminova.mmce.compat.items.ItemHandlerHelper;
import hellfirepvp.modularmachinery.common.block.BlockFactoryController;
import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.machine.factory.FactoryRecipeThread;
import hellfirepvp.modularmachinery.common.tiles.TileFactoryController;
import hellfirepvp.modularmachinery.common.tiles.TileMachineController;
import hellfirepvp.modularmachinery.common.tiles.TileParallelController;
import hellfirepvp.modularmachinery.common.tiles.TileSmartInterface;
import hellfirepvp.modularmachinery.common.tiles.TileUpgradeBus;
import hellfirepvp.modularmachinery.common.tiles.base.TileItemBus;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.util.SmartInterfaceData;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.RecipeRegistry;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementEnergy;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementFluid;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementItem;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.tiles.base.TileEnergyHatch;
import hellfirepvp.modularmachinery.common.tiles.base.TileFluidTank;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementIngredientArray;
import github.kasuminova.mmce.common.itemtype.ChancedIngredientStack;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 仅开发环境：在服务端按 tick 推进的自测序列，可在专用服务器控制台执行（不依赖客户端）。
 * <ul>
 *   <li>persist-a：在出生点附近搭建两台测试机器、投料、运行 60 tick、记录状态并保存；控制器坐标写入 mmce-selftest-state.txt。</li>
 *   <li>persist-b：（重启服务器后）读回坐标，确认结构仍形成、配方进度 / 能量 / 流体 / 蓝图未丢失，再运行至装配机出产物。</li>
 *   <li>energy：能量链路（计划 7.2 节 E1–E4）。发电机 → Mekanism 通用电缆 ×2 → 装配机（装配机不预充能）；
 *       发电机 → EnderIO 能量导管 ×2 → 装配机；发电机 → 单独的 MMCE 能量输入仓（与上游一致，不应传输）；发电机 → Mekanism 能量立方。
 *       电缆 / 导管用假玩家调用物品本身的放置逻辑放下。</li>
 *   <li>zs：testdata/scripts/mmce_test.zs 中用 RecipeBuilder 定义的配方已注册，并在机器中实际运行出产物。</li>
 *   <li>io：物品总线与相邻容器——输入总线从相邻箱子拉取、输出总线向相邻箱子推送、漏斗向输入总线放入、漏斗从输出总线取出。</li>
 *   <li>gas：Mekanism 气体（计划 7.2 节 B3）。G1：经流体仓的 IGasHandler 直接注入氢气，发电机按 B3 产电，核对产电量与耗气量；
 *       流体 / 气体互斥与存档 NBT 往返。G2：电解槽（testdata 中的 gas_electrolyzer，水 + 电 → 氢气输出到流体输出仓）
 *       → Mekanism 加压管道 ×2（靠电解槽一端设为 PULL 抽取）→ 发电机流体输入仓 → B3 产电。</li>
 *   <li>zs-gas：testdata/scripts_mekanism/mmce_gas_test.zs 中 ZenScript 气体方法生成的配方——addGasInput（氧气输入产电）、
 *       addGasOutput（氧气进入流体输出仓）、addGasPerTickInput / addGasPerTickOutput（每 tick 消耗氯气、产出氧气，两次采样核对）。</li>
 *   <li>zs-obf：发布 jar 在重混淆专用服务器上（scripts/obf-server-test.sh，未装 Mekanism / EnderIO）运行 testdata/scripts/mmce_test.zs——
 *       MineTweaker 的原版类注册表已加载、原版尖括号引用能解析；引用 Mekanism / EnderIO 物品的 zs_energetic_alloy 明确报错且不注册，
 *       同一文件中的 zs_blaze_power 照常注册并在燃料发电机中实际运行。</li>
 *   <li>logic：逻辑测试机（testdata 中的 logic_test 与 scripts/mmce_logic_test.zs）——并行控制器（并行 1 / 4）、
 *       机器修改器（顶上放金块时配方时间减半）、机械升级（钻石：0.5^n 倍时间；多次重新统计后数量不变）、
 *       智能接口（mode 不在 5–10 时泥土配方不运行，设为 7 后运行）、工厂控制器（2 个线程同时运行）。</li>
 *   <li>zs-gas-bracket：testdata/scripts_mekanism/mmce_gas_bracket_test.zs——上游写法 {@code <gas:x> * n}、复数方法、addInputs / addOutput 自动识别气体；
 *       未注册的气体名报 Could not resolve；气体需求的修改器（机器事件添加“气体输入 × 2”，实际消耗 200 mB）与几率 0（不消耗）实际运行。</li>
 *   <li>ae2：AE2 ME 部件（testdata 中的 ae2_test，见 {@link DevAE2Test}）——物品 / 流体输入输出总线经 ME 网络运行配方、容量卡、
 *       样板供应器（终极样板带流体原料，经合成 CPU 下单完成）、阻挡 / 合成锁定 / 输入隔离模式、AE2FC 流体滴换成流体、样板镜像、
 *       内存卡设置复制、带内容破坏时保留 NBT。</li>
 *   <li>adapters：配方适配器（testdata 中的 adapter_ic2 / adapter_tic / adapter_te 及其 .adapter.json）——
 *       每个适配器生成的配方数与来源模组的配方表一致；每个适配器挑一条配方放入原料、实际运行并核对产物。</li>
 * </ul>
 * 每一步结果以 SELFTEST OK / SELFTEST FAIL 写入 mmce-test-report.txt；加 exit 参数时结束后关闭服务器。
 */
public final class DevSelfTest {

    private static final String ASSEMBLER = "mek_eio_assembler";
    private static final String GENERATOR = "fuel_generator";
    private static final Pattern ENERGY = Pattern.compile("energy\\[(in|out)]=(\\d+)/");
    private static final Pattern FLUID = Pattern.compile("fluid=(\\d+)mB");
    private static final Pattern TICK = Pattern.compile("tick=(\\d+)/");

    private final ICommandSender sender;
    private final boolean exit;
    private final Deque<Step> steps = new ArrayDeque<>();
    private int wait;
    private int failures;

    ICommandSender sender() {
        return sender;
    }

    private DevSelfTest(ICommandSender sender, boolean exit) {
        this.sender = sender;
        this.exit = exit;
    }

    static void start(ICommandSender sender, String phase, boolean exit) {
        DevSelfTest test = new DevSelfTest(sender, exit);
        switch (phase) {
            case "persist-a" -> test.buildPersistA();
            case "persist-b" -> test.buildPersistB();
            case "energy" -> test.buildEnergy();
            case "io" -> test.buildItemIo();
            case "zs" -> test.buildZenScript();
            case "gas" -> test.buildGas();
            case "zs-gas" -> test.buildZenScriptGas();
            case "zs-obf" -> test.buildZenScriptObf();
            case "logic" -> test.buildLogic();
            case "adapters" -> test.buildAdapters();
            case "zs-gas-bracket" -> test.buildZenScriptGasBracket();
            case "ae2" -> DevAE2Test.build(test);
            case "ae2-gui" -> DevAE2Test.buildForGui(test);
            case "pipes" -> DevInteropTest.buildPipes(test);
            case "te-rf" -> DevInteropTest.buildThermalRF(test);
            case "zs-machine" -> DevInteropTest.buildScriptMachine(test);
            case "magic" -> DevMagicTest.build(test);
            case "alloy" -> DevAlloyTest.build(test);
            case "eio-items" -> DevInteropTest.buildEnderIOItems(test);
            case "environment" -> DevEnvironmentTest.build(test);
            case "gt" -> DevInteropTest.buildGregTech(test);
            case "railcraft" -> DevInteropTest.buildRailcraft(test);
            case "exu" -> DevInteropTest.buildExtraUtilities(test);
            case "bc" -> DevInteropTest.buildBuildCraft(test);
            default -> {
                CommandMMCETest.report(sender, "SELFTEST FAIL unknown phase " + phase);
                return;
            }
        }
        CommandMMCETest.report(sender, "SELFTEST start " + phase);
        FMLCommonHandler.instance().bus().register(test);
    }

    static DynamicMachine machine(String name) {
        return MachineRegistry.getRegistry().getMachine(new ResourceLocation(ModularMachinery.MODID, name));
    }

    private static File stateFile() {
        return new File(Loader.instance().getConfigDir().getParentFile(), "mmce-selftest-state.txt");
    }

    // ---------------- 阶段 A ----------------

    private void buildPersistA() {
        step("build", 40, () -> {
            World world = sender.getEntityWorld();
            ChunkCoordinates spawn = world.getSpawnPoint();
            BlockPos generator = groundAbove(world, spawn.posX, spawn.posZ + 3);
            BlockPos assembler = groundAbove(world, spawn.posX + 8, spawn.posZ + 3);
            CommandMMCETest.buildAt(sender, world, machine(GENERATOR), generator);
            CommandMMCETest.buildAt(sender, world, machine(ASSEMBLER), assembler);
            try {
                Files.write(stateFile().toPath(), (GENERATOR + " " + toText(generator) + "\n" + ASSEMBLER + " " + toText(assembler) + "\n")
                    .getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
        step("fill", 60, () -> {
            CommandMMCETest.fill(sender, machine(GENERATOR));
            CommandMMCETest.fill(sender, machine(ASSEMBLER));
        });
        step("status before save", 0, () -> {
            String gen = CommandMMCETest.status(sender, machine(GENERATOR));
            String asm = CommandMMCETest.status(sender, machine(ASSEMBLER));
            expect("generator formed", gen != null && gen.contains("formed=true"));
            expect("generator crafting", gen != null && gen.contains("state=CRAFTING"));
            expect("assembler crafting", asm != null && asm.contains("state=CRAFTING") && asm.contains("recipe=modularmachinery:mek_eio_basic_capacitor"));
        });
        step("save worlds", 0, () -> {
            MinecraftServer server = MinecraftServer.getServer();
            server.getConfigurationManager().saveAllPlayerData();
            for (World world : server.worldServers) {
                try {
                    ((net.minecraft.world.WorldServer) world).saveAllChunks(true, null);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        });
    }

    // ---------------- 阶段 B ----------------

    private void buildPersistB() {
        step("load positions", 40, () -> {
            List<String> lines;
            try {
                lines = Files.readAllLines(stateFile().toPath(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException("run persist-a first", e);
            }
            for (String line : lines) {
                String[] parts = line.trim().split(" ");
                if (parts.length == 4) {
                    CommandMMCETest.rememberBuilt(machine(parts[0]),
                        new BlockPos(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3])));
                }
            }
        });
        step("status after reload", 0, () -> {
            String gen = CommandMMCETest.status(sender, machine(GENERATOR));
            String asm = CommandMMCETest.status(sender, machine(ASSEMBLER));
            expect("generator still formed (blueprint kept)", gen != null && gen.contains("formed=true"));
            expect("generator energy kept", number(ENERGY, gen, 2) > 0);
            expect("generator fluid kept", number(FLUID, gen, 1) > 0 && number(FLUID, gen, 1) < 1000);
            expect("assembler still formed", asm != null && asm.contains("formed=true"));
            expect("assembler energy kept", number(ENERGY, asm, 2) > 2_000_000);
            boolean progressKept = asm != null && (number(TICK, asm, 1) > 0 || asm.contains("out=2xEnderIO:itemBasicCapacitor@0"));
            expect("assembler progress or output kept", progressKept);
        });
        step("run to completion", 200, () -> {
        });
        step("status after run", 0, () -> {
            String gen = CommandMMCETest.status(sender, machine(GENERATOR));
            String asm = CommandMMCETest.status(sender, machine(ASSEMBLER));
            expect("assembler produced capacitors", asm != null && asm.contains("out=2xEnderIO:itemBasicCapacitor@0"));
            expect("generator still producing", number(ENERGY, gen, 2) > 0);
        });
    }

    // ---------------- ZenScript ----------------

    private BlockPos zsAssembler;

    private static int countRecipes(DynamicMachine machine, String prefix) {
        int count = 0;
        for (hellfirepvp.modularmachinery.common.crafting.MachineRecipe recipe
            : hellfirepvp.modularmachinery.common.crafting.RecipeRegistry.getRecipesFor(machine)) {
            if (recipe.getRegistryName().toString().startsWith(ModularMachinery.MODID + ":" + prefix)) {
                count++;
            }
        }
        return count;
    }

    private static boolean hasScriptRecipe(DynamicMachine machine, String prefix) {
        return countRecipes(machine, prefix) > 0;
    }

    private void buildZenScript() {
        step("recipes registered", 0, () -> {
            expect("script recipe zs_energetic_alloy registered for mek_eio_assembler", hasScriptRecipe(machine(ASSEMBLER), "zs_energetic_alloy"));
            expect("script recipe zs_blaze_power registered for fuel_generator", hasScriptRecipe(machine(GENERATOR), "zs_blaze_power"));
        });
        step("build and fill", 120, () -> {
            World world = sender.getEntityWorld();
            ChunkCoordinates spawn = world.getSpawnPoint();
            zsAssembler = groundAbove(world, spawn.posX - 60, spawn.posZ + 3);
            CommandMMCETest.buildAt(sender, world, machine(ASSEMBLER), zsAssembler);
            for (BlockPos offset : machine(ASSEMBLER).getPattern().getPattern().keySet()) {
                TileEntity te = world.getTileEntity(zsAssembler.getX() + offset.getX(), zsAssembler.getY() + offset.getY(), zsAssembler.getZ() + offset.getZ());
                if (te instanceof hellfirepvp.modularmachinery.common.tiles.TileItemInputBus bus) {
                    net.minecraft.item.Item ingot = cpw.mods.fml.common.registry.GameRegistry.findItem("Mekanism", "Ingot");
                    bus.getInventory().setStackInSlot(0, new net.minecraft.item.ItemStack(ingot, 1, 4));
                    bus.getInventory().setStackInSlot(1, new net.minecraft.item.ItemStack(net.minecraft.init.Items.redstone, 2));
                } else if (te instanceof hellfirepvp.modularmachinery.common.tiles.base.TileEnergyHatch hatch && hatch.canReceive()) {
                    hatch.setCurrentEnergy(hatch.getMaxEnergy());
                }
            }
        });
        step("check output", 0, () -> {
            String line = CommandMMCETest.statusAt(sender, sender.getEntityWorld(), machine(ASSEMBLER), zsAssembler);
            expect("script recipe produced EnderIO energetic alloy", line != null && line.contains("out=1xEnderIO:itemAlloy@1"));
            File mtLog = new File(Loader.instance().getConfigDir().getParentFile(), "logs/minetweaker.log");
            boolean eventSeen = false;
            try {
                eventSeen = mtLog.isFile() && new String(Files.readAllBytes(mtLog.toPath()), StandardCharsets.UTF_8).contains("MMCE_ZS_EVENT structure formed");
            } catch (IOException ignored) {
            }
            expect("script MMEvents.onStructureFormed handler ran (logs/minetweaker.log)", eventSeen);
        });
        int[] before = new int[3];
        step("minetweaker reload", 40, () -> {
            before[0] = countRecipes(machine(ASSEMBLER), "zs_energetic_alloy");
            before[1] = countRecipes(machine(ASSEMBLER), "mek_eio_basic_capacitor");
            before[2] = countRecipes(machine(GENERATOR), "zs_blaze_power");
            MinecraftServer.getServer().getCommandManager().executeCommand(MinecraftServer.getServer(), "minetweaker reload");
        });
        step("after reload", 0, () -> {
            int zsAlloy = countRecipes(machine(ASSEMBLER), "zs_energetic_alloy");
            int jsonCapacitor = countRecipes(machine(ASSEMBLER), "mek_eio_basic_capacitor");
            int zsBlaze = countRecipes(machine(GENERATOR), "zs_blaze_power");
            CommandMMCETest.report(sender, "INFO recipes before/after reload: zs_energetic_alloy " + before[0] + "/" + zsAlloy
                + ", mek_eio_basic_capacitor " + before[1] + "/" + jsonCapacitor + ", zs_blaze_power " + before[2] + "/" + zsBlaze);
            expect("reload keeps script recipes without duplicates", zsAlloy == before[0] && zsBlaze == before[2] && zsAlloy > 0);
            expect("reload keeps JSON recipes", jsonCapacitor == before[1] && jsonCapacitor > 0);
        });
    }

    // ---------------- 发布 jar 中的 ZenScript（重混淆环境） ----------------

    private BlockPos zsObfGenerator;

    private static String mineTweakerLog() {
        File mtLog = new File(Loader.instance().getConfigDir().getParentFile(), "logs/minetweaker.log");
        try {
            return mtLog.isFile() ? new String(Files.readAllBytes(mtLog.toPath()), StandardCharsets.UTF_8) : "";
        } catch (IOException e) {
            return "";
        }
    }

    private void buildZenScriptObf() {
        step("script results", 0, () -> {
            String log = mineTweakerLog();
            // CraftTweaker 3.4.8 把 IC2 支持类（SyntheticCrop extends ic2.api.crops.CropCard）编进了原版注册表 MineTweakerRegistry，
            // 未装 IC2 时整张表加载失败并被跳过，所有原版尖括号引用都解析不到（见 docs/TEST_LOG.md「重混淆环境中脚本解析不到原版引用」）
            expect("MineTweaker vanilla class registry loaded (no \"Classes for registry class minetweaker.mc1710.MineTweakerRegistry not found\")",
                !log.isEmpty() && !log.contains("Classes for registry class minetweaker.mc1710.MineTweakerRegistry not found"));
            expect("vanilla item / liquid brackets resolve (no \"Could not resolve <minecraft\" / \"<liquid\")",
                !log.isEmpty() && !log.contains("Could not resolve <minecraft") && !log.contains("Could not resolve <liquid"));
            expect("mmce_test.zs ran without an exception", !log.isEmpty() && !log.contains("Error executing mmce_test.zs"));
            expect("recipe with unresolved Mekanism / EnderIO items is reported (Recipe modularmachinery:zs_energetic_alloy was not registered)",
                log.contains("Recipe modularmachinery:zs_energetic_alloy: addItemInput(null)")
                    && log.contains("Recipe modularmachinery:zs_energetic_alloy was not registered"));
            expect("zs_energetic_alloy is not registered",
                hellfirepvp.modularmachinery.common.crafting.RecipeRegistry.getRecipe(new ResourceLocation(ModularMachinery.MODID, "zs_energetic_alloy")) == null);
            expect("script recipe zs_blaze_power registered for fuel_generator", hasScriptRecipe(machine(GENERATOR), "zs_blaze_power"));
        });
        step("build and fill", 60, () -> {
            World world = sender.getEntityWorld();
            ChunkCoordinates spawn = world.getSpawnPoint();
            zsObfGenerator = groundAbove(world, spawn.posX - 30, spawn.posZ + 3);
            CommandMMCETest.buildAt(sender, world, machine(GENERATOR), zsObfGenerator);
            int buses = 0;
            for (BlockPos offset : machine(GENERATOR).getPattern().getPattern().keySet()) {
                if (tileAt(world, zsObfGenerator.add(offset)) instanceof hellfirepvp.modularmachinery.common.tiles.TileItemInputBus bus) {
                    // 烈焰粉不是燃料，只有脚本配方 zs_blaze_power 用它
                    bus.getInventory().setStackInSlot(0, new net.minecraft.item.ItemStack(net.minecraft.init.Items.blaze_powder, 1));
                    buses++;
                }
            }
            expect("blaze powder put into the generator's item input bus", buses == 1);
        });
        step("check run", 0, () -> {
            String line = CommandMMCETest.statusAt(sender, sender.getEntityWorld(), machine(GENERATOR), zsObfGenerator);
            long tick = number(TICK, line, 1);
            long energy = number(ENERGY, line, 2);
            CommandMMCETest.report(sender, "INFO zs_blaze_power tick=" + tick + " energy=" + energy + " RF, expected 60 x tick = " + 60 * tick + " RF");
            expect("fuel_generator runs script recipe zs_blaze_power", line != null && line.contains("recipe=modularmachinery:zs_blaze_power"));
            // 工作线程推进配方，状态行中的进度与能量可能相差 1 tick（同 zs-gas）
            expect("zs_blaze_power produces 60 RF/t (within 1 tick)", tick > 0 && energy >= 60 * tick && energy <= 60 * (tick + 1));
        });
    }

    // ---------------- Mekanism 气体 ----------------

    private static final String ELECTROLYZER = "gas_electrolyzer";
    private static final Pattern GAS_IN = Pattern.compile("gas\\[in]=(\\d+)mB hydrogen");
    private BlockPos gasGen1;
    private BlockPos gasGen2;
    private BlockPos electrolyzer;
    private BlockPos lavaHatch;

    static TileEntity tileAt(World world, BlockPos pos) {
        return world.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
    }

    private void buildGas() {
        if (!hellfirepvp.modularmachinery.common.base.Mods.MEKANISM.isPresent()) {
            step("mekanism", 0, () -> expect("Mekanism is loaded", false));
            return;
        }
        step("recipes registered", 0, () -> {
            expect("JSON recipe fuel_generator_hydrogen (B3, modularmachinery:gas input) registered", hasScriptRecipe(machine(GENERATOR), "fuel_generator_hydrogen"));
            expect("JSON recipe gas_electrolyzer_hydrogen (modularmachinery:gas output) registered", hasScriptRecipe(machine(ELECTROLYZER), "gas_electrolyzer_hydrogen"));
        });
        step("build", 40, () -> {
            World world = sender.getEntityWorld();
            ChunkCoordinates spawn = world.getSpawnPoint();
            gasGen1 = groundAbove(world, spawn.posX, spawn.posZ + 24);
            gasGen2 = groundAbove(world, spawn.posX + 12, spawn.posZ + 24);
            // 电解槽的输出仓在其 (-1,0,1)，与发电机输入仓 (1,0,1) 之间隔两格，放两段管道
            electrolyzer = gasGen2.add(5, 0, 0);
            CommandMMCETest.buildAt(sender, world, machine(GENERATOR), gasGen1);
            CommandMMCETest.buildAt(sender, world, machine(GENERATOR), gasGen2);
            CommandMMCETest.buildAt(sender, world, machine(ELECTROLYZER), electrolyzer);
            for (int dx = 2; dx <= 3; dx++) {
                BlockPos tube = gasGen2.add(dx, 0, 1);
                world.setBlockToAir(tube.getX(), tube.getY(), tube.getZ());
                world.setBlockToAir(tube.getX(), tube.getY() + 1, tube.getZ());
                world.setBlock(tube.getX(), tube.getY() - 1, tube.getZ(), net.minecraft.init.Blocks.stone, 0, 3);
            }
            // 单独的流体输入仓（NORMAL，1000 mB），用于流体 / 气体互斥检查
            lavaHatch = gasGen1.add(-4, 0, 0);
            world.setBlock(lavaHatch.getX(), lavaHatch.getY(), lavaHatch.getZ(), hellfirepvp.modularmachinery.common.lib.BlocksMM.fluidInputHatch, 2, 3);
        });
        step("inject and fill", 0, () -> {
            World world = sender.getEntityWorld();
            TileEntity hatch1 = tileAt(world, gasGen1.add(1, 0, 1));
            int accepted = DevGasSupport.receive(hatch1, "hydrogen", 1000);
            CommandMMCETest.report(sender, "INFO G1 IGasHandler.receiveGas(hydrogen, 1000) -> " + accepted + "; hatch " + DevGasSupport.describe(hatch1));
            expect("fluid input hatch accepts hydrogen through IGasHandler", accepted == 1000);
            // 装着气体时不能再放流体（与上游 HybridGasTank 相同）
            net.minecraftforge.fluids.IFluidHandler fluidSide = (net.minecraftforge.fluids.IFluidHandler) hatch1;
            int lavaIntoGas = fluidSide.fill(net.minecraftforge.common.util.ForgeDirection.UP,
                new net.minecraftforge.fluids.FluidStack(net.minecraftforge.fluids.FluidRegistry.LAVA, 100), true);
            boolean canFillLava = fluidSide.canFill(net.minecraftforge.common.util.ForgeDirection.UP, net.minecraftforge.fluids.FluidRegistry.LAVA);
            expect("hatch holding gas rejects fluid (fill=" + lavaIntoGas + ", canFill=" + canFillLava + ")", lavaIntoGas == 0 && !canFillLava);
            // 装着流体时不能再放气体
            TileEntity lava = tileAt(world, lavaHatch);
            int lavaIn = ((net.minecraftforge.fluids.IFluidHandler) lava).fill(net.minecraftforge.common.util.ForgeDirection.UP,
                new net.minecraftforge.fluids.FluidStack(net.minecraftforge.fluids.FluidRegistry.LAVA, 500), true);
            int gasIntoFluid = DevGasSupport.receive(lava, "hydrogen", 100);
            boolean canReceive = DevGasSupport.canReceive(lava, "hydrogen");
            expect("hatch holding fluid (lava " + lavaIn + "mB) rejects gas (received=" + gasIntoFluid + ", canReceive=" + canReceive + ")",
                lavaIn == 500 && gasIntoFluid == 0 && !canReceive);
            // 存档格式：气体写在 tank 标签下的 gasTag 中，读回后不变
            String nbt = DevGasSupport.nbtRoundTrip(hatch1);
            CommandMMCETest.report(sender, "INFO G1 hatch " + nbt);
            expect("gas survives NBT write / read", nbt.contains("gasName=hydrogen amount=1000") && nbt.endsWith("reloaded=1000mB hydrogen"));
            // 电解槽：输入仓放水，能量输入仓充满
            TileEntity waterHatch = tileAt(world, electrolyzer.add(1, 0, 1));
            int water = ((hellfirepvp.modularmachinery.common.tiles.base.TileFluidTank) waterHatch).getTank()
                .fill(new net.minecraftforge.fluids.FluidStack(net.minecraftforge.fluids.FluidRegistry.WATER, 1000), true);
            TileEntity energy = tileAt(world, electrolyzer.add(0, 0, 1));
            if (energy instanceof hellfirepvp.modularmachinery.common.tiles.base.TileEnergyHatch hatch) {
                hatch.setCurrentEnergy(hatch.getMaxEnergy());
            }
            CommandMMCETest.report(sender, "INFO electrolyzer water=" + water + "mB");
        });
        step("place tubes", 0, () -> {
            World world = sender.getEntityWorld();
            // Mekanism 基础加压管道：PartTransmitter@8（TransmitterType.PRESSURIZED_TUBE_BASIC）
            placeItemAt(world, gasGen2.add(2, 0, 1), "Mekanism", "PartTransmitter", 8);
            placeItemAt(world, gasGen2.add(3, 0, 1), "Mekanism", "PartTransmitter", 8);
            // 靠电解槽输出仓的一端设为 PULL：Mekanism 1.7.10 的管道只在 PULL 面主动抽取（drawGas）
            DevGasSupport.setTubePull(world, gasGen2.add(3, 0, 1), net.minecraftforge.common.util.ForgeDirection.EAST);
        });
        step("run", 300, () -> {
        });
        step("check", 0, () -> {
            World world = sender.getEntityWorld();
            String g1 = CommandMMCETest.statusAt(sender, world, machine(GENERATOR), gasGen1);
            long hydrogen = number(GAS_IN, g1, 1);
            if (hydrogen < 0) {
                hydrogen = 0;
            }
            long energy = number(ENERGY, g1, 2);
            long tick = number(TICK, g1, 1);
            long started = (1000 - hydrogen) / 100;
            // 输入在配方开始时消耗；输出能量按 tick 产生：已完成 (started-1) 次 × 100 t + 当前进度
            // 检查时恰好处于两次配方之间（没有进行中的配方）则全部已开始的配方都已完成
            long expectedEnergy = tick >= 0 ? 30L * ((started - 1) * 100 + tick) : 30L * started * 100;
            CommandMMCETest.report(sender, "INFO G1 hydrogen left=" + hydrogen + "mB, runs started=" + started + ", tick=" + tick + ", energy=" + energy + " RF, expected=" + expectedEnergy + " RF");
            expect("B3 generator runs fuel_generator_hydrogen", g1 != null && (tick < 0 || g1.contains("recipe=modularmachinery:fuel_generator_hydrogen")));
            expect("B3 consumes hydrogen in 100 mB steps", (1000 - hydrogen) % 100 == 0 && started >= 3);
            // MMCE 在工作线程中推进配方，产电与进度计数的更新先后不固定，同一状态行里两者可能相差 1 tick（30 RF）
            expect("B3 produces 30 RF/t (energy = 30 x processed ticks, within 1 tick)", Math.abs(energy - expectedEnergy) <= 30 && energy > 0);
            String el = CommandMMCETest.statusAt(sender, world, machine(ELECTROLYZER), electrolyzer);
            long waterLeft = number(FLUID, el, 1);
            CommandMMCETest.report(sender, "INFO electrolyzer output hatch " + DevGasSupport.describe(tileAt(world, electrolyzer.add(-1, 0, 1)))
                + ", tubes " + world.getBlock(gasGen2.getX() + 2, gasGen2.getY(), gasGen2.getZ() + 1).getUnlocalizedName());
            expect("electrolyzer consumed water (gas output recipe ran)", el != null && waterLeft >= 0 && waterLeft < 1000);
            String g2 = CommandMMCETest.statusAt(sender, world, machine(GENERATOR), gasGen2);
            long g2Energy = number(ENERGY, g2, 2);
            expect("electrolyzer -> output hatch -> Mekanism pressurized tubes (PULL) -> generator: B3 produced energy", g2Energy > 0);
        });
    }

    // ---------------- ZenScript 气体方法（testdata/scripts_mekanism/mmce_gas_test.zs） ----------------

    private static final Pattern OXYGEN_IN = Pattern.compile("gas\\[in]=(\\d+)mB oxygen");
    private static final Pattern OXYGEN_OUT = Pattern.compile("gas\\[out]=(\\d+)mB oxygen");
    private static final Pattern CHLORINE_IN = Pattern.compile("gas\\[in]=(\\d+)mB chlorine");
    private BlockPos zsGasGen;
    private BlockPos zsGasOut;
    private BlockPos zsGasTick;
    /** 每 tick 配方的两次采样：tick、剩余氯气、产出氧气、电解槽能量 */
    private final long[][] perTickSamples = new long[2][];

    private static hellfirepvp.modularmachinery.common.crafting.MachineRecipe findRecipe(DynamicMachine machine, String prefix) {
        for (hellfirepvp.modularmachinery.common.crafting.MachineRecipe recipe
            : hellfirepvp.modularmachinery.common.crafting.RecipeRegistry.getRecipesFor(machine)) {
            if (recipe.getRegistryName().toString().startsWith(ModularMachinery.MODID + ":" + prefix)) {
                return recipe;
            }
        }
        return null;
    }

    private void expectScriptRecipe(DynamicMachine machine, String name, String... requirements) {
        hellfirepvp.modularmachinery.common.crafting.MachineRecipe recipe = findRecipe(machine, name);
        String described = recipe == null ? "not registered" : DevGasSupport.describeRequirements(recipe);
        CommandMMCETest.report(sender, "INFO script recipe " + name + ": " + described);
        boolean ok = recipe != null;
        for (String requirement : requirements) {
            ok &= described.contains(requirement);
        }
        expect("script recipe " + name + " registered for " + machine.getRegistryName().getPath() + " with " + String.join(", ", requirements), ok);
    }

    private long[] samplePerTick(World world) {
        String line = CommandMMCETest.statusAt(sender, world, machine(ELECTROLYZER), zsGasTick);
        expect("per-tick recipe zs_gas_pertick is running", line != null && line.contains("recipe=modularmachinery:zs_gas_pertick"));
        return new long[]{number(TICK, line, 1), Math.max(0, number(CHLORINE_IN, line, 1)), Math.max(0, number(OXYGEN_OUT, line, 1)), number(ENERGY, line, 2)};
    }

    private void buildZenScriptGas() {
        if (!hellfirepvp.modularmachinery.common.base.Mods.MEKANISM.isPresent()) {
            step("mekanism", 0, () -> expect("Mekanism is loaded", false));
            return;
        }
        step("script recipes registered", 0, () -> {
            File mtLog = new File(Loader.instance().getConfigDir().getParentFile(), "logs/minetweaker.log");
            String log = "";
            try {
                log = mtLog.isFile() ? new String(Files.readAllBytes(mtLog.toPath()), StandardCharsets.UTF_8) : "";
            } catch (IOException ignored) {
            }
            expect("mmce_gas_test.zs compiled and ran (logs/minetweaker.log)", log.contains("MMCE_ZS_GAS script loaded"));
            expectScriptRecipe(machine(GENERATOR), "zs_gas_oxygen_power", "gas input oxygen x100", "energy output");
            expectScriptRecipe(machine(ELECTROLYZER), "zs_gas_oxygen_out", "fluid input", "energy input", "gas output oxygen x100");
            expectScriptRecipe(machine(ELECTROLYZER), "zs_gas_pertick", "gas_pertick input chlorine x4", "energy input", "gas_pertick output oxygen x2");
            // 写错的气体名：报错并忽略这一项，配方其余部分照常注册
            expectScriptRecipe(machine(GENERATOR), "zs_gas_unknown", "item input", "energy output");
            hellfirepvp.modularmachinery.common.crafting.MachineRecipe unknown = findRecipe(machine(GENERATOR), "zs_gas_unknown");
            expect("unknown gas name is logged and ignored (GasStack not found/unknown gas: mmce_no_such_gas)",
                log.contains("GasStack not found/unknown gas: mmce_no_such_gas") && unknown != null && !DevGasSupport.describeRequirements(unknown).contains("gas"));
        });
        step("build", 40, () -> {
            World world = sender.getEntityWorld();
            ChunkCoordinates spawn = world.getSpawnPoint();
            zsGasGen = groundAbove(world, spawn.posX, spawn.posZ + 40);
            zsGasOut = groundAbove(world, spawn.posX + 10, spawn.posZ + 40);
            zsGasTick = groundAbove(world, spawn.posX + 20, spawn.posZ + 40);
            CommandMMCETest.buildAt(sender, world, machine(GENERATOR), zsGasGen);
            CommandMMCETest.buildAt(sender, world, machine(ELECTROLYZER), zsGasOut);
            CommandMMCETest.buildAt(sender, world, machine(ELECTROLYZER), zsGasTick);
        });
        step("inject and fill", 0, () -> {
            World world = sender.getEntityWorld();
            // 发电机：流体输入仓 (1,0,1) 经 IGasHandler 注入 1000 mB 氧气
            int oxygen = DevGasSupport.receive(tileAt(world, zsGasGen.add(1, 0, 1)), "oxygen", 1000);
            expect("generator fluid input hatch accepts 1000 mB oxygen", oxygen == 1000);
            // 电解槽（气体输出）：输入仓 1000 mB 熔岩，能量输入仓充满
            int lava = ((hellfirepvp.modularmachinery.common.tiles.base.TileFluidTank) tileAt(world, zsGasOut.add(1, 0, 1))).getTank()
                .fill(new net.minecraftforge.fluids.FluidStack(net.minecraftforge.fluids.FluidRegistry.LAVA, 1000), true);
            expect("electrolyzer input hatch accepts 1000 mB lava", lava == 1000);
            // 电解槽（每 tick）：输入仓经 IGasHandler 注入 1000 mB 氯气，能量输入仓充满
            int chlorine = DevGasSupport.receive(tileAt(world, zsGasTick.add(1, 0, 1)), "chlorine", 1000);
            expect("electrolyzer input hatch accepts 1000 mB chlorine", chlorine == 1000);
            for (BlockPos el : new BlockPos[]{zsGasOut, zsGasTick}) {
                if (tileAt(world, el.add(0, 0, 1)) instanceof hellfirepvp.modularmachinery.common.tiles.base.TileEnergyHatch hatch) {
                    hatch.setCurrentEnergy(hatch.getMaxEnergy());
                }
            }
        });
        step("run", 60, () -> {
        });
        step("per-tick sample 1", 30, () -> perTickSamples[0] = samplePerTick(sender.getEntityWorld()));
        step("per-tick sample 2", 0, () -> perTickSamples[1] = samplePerTick(sender.getEntityWorld()));
        step("check", 0, () -> {
            World world = sender.getEntityWorld();
            // addGasPerTickInput / addGasPerTickOutput：100 t 的配方，每 tick 消耗 4 mB 氯气、产出 2 mB 氧气（两次采样都在第一次配方中）
            long[] a = perTickSamples[0];
            long[] b = perTickSamples[1];
            long dt = b[0] - a[0];
            CommandMMCETest.report(sender, "INFO per-tick sample1 tick=" + a[0] + " chlorine=" + a[1] + "mB oxygen=" + a[2] + "mB energy=" + a[3]
                + "; sample2 tick=" + b[0] + " chlorine=" + b[1] + "mB oxygen=" + b[2] + "mB energy=" + b[3]);
            CommandMMCETest.report(sender, "INFO per-tick consumed chlorine " + (1000 - a[1]) + " / " + (1000 - b[1]) + " mB (4 x tick = " + 4 * a[0] + " / " + 4 * b[0]
                + "), between samples " + (a[1] - b[1]) + " mB in " + dt + " t; oxygen produced between samples " + (b[2] - a[2]) + " mB");
            // MMCE 在工作线程中推进配方，同一状态行中的进度与储罐可能相差 1 tick，因此各允许 1 tick 的量
            expect("per-tick samples are inside the first run (0 < tick1 < tick2 < 100)", a[0] > 0 && dt > 0 && b[0] < 100);
            expect("addGasPerTickInput consumes 4 mB chlorine every tick (total and between samples, within 1 tick)",
                Math.abs((1000 - a[1]) - 4 * a[0]) <= 4 && Math.abs((1000 - b[1]) - 4 * b[0]) <= 4 && Math.abs((a[1] - b[1]) - 4 * dt) <= 4);
            expect("addGasPerTickOutput puts 2 mB oxygen into the fluid output hatch every tick (within 1 tick)",
                Math.abs(a[2] - 2 * a[0]) <= 2 && Math.abs(b[2] - 2 * b[0]) <= 2 && Math.abs((b[2] - a[2]) - 2 * dt) <= 2);
            // addGasInput：配方开始时一次取走 100 mB 氧气，产电 25 RF/t
            String gen = CommandMMCETest.statusAt(sender, world, machine(GENERATOR), zsGasGen);
            long left = Math.max(0, number(OXYGEN_IN, gen, 1));
            long tick = number(TICK, gen, 1);
            long energy = number(ENERGY, gen, 2);
            long started = (1000 - left) / 100;
            long expectedEnergy = tick >= 0 ? 25L * ((started - 1) * 50 + tick) : 25L * started * 50;
            CommandMMCETest.report(sender, "INFO generator oxygen left=" + left + "mB, runs started=" + started + ", tick=" + tick + ", energy=" + energy + " RF, expected=" + expectedEnergy + " RF");
            expect("addGasInput recipe zs_gas_oxygen_power runs", gen != null && (tick < 0 || gen.contains("recipe=modularmachinery:zs_gas_oxygen_power")));
            expect("addGasInput consumes oxygen in 100 mB steps at recipe start", (1000 - left) % 100 == 0 && started >= 2);
            expect("addGasInput recipe produces 25 RF/t (within 1 tick)", energy > 0 && Math.abs(energy - expectedEnergy) <= 25);
            // addGasOutput：配方结束时把 100 mB 氧气放入流体输出仓
            String out = CommandMMCETest.statusAt(sender, world, machine(ELECTROLYZER), zsGasOut);
            long lavaLeft = number(FLUID, out, 1);
            long runs = (1000 - Math.max(0, lavaLeft)) / 100;
            long outTick = number(TICK, out, 1);
            long produced = Math.max(0, number(OXYGEN_OUT, out, 1));
            long completed = outTick >= 0 ? runs - 1 : runs;
            CommandMMCETest.report(sender, "INFO electrolyzer (addGasOutput) lava left=" + lavaLeft + "mB, runs started=" + runs + ", tick=" + outTick
                + ", output hatch " + DevGasSupport.describe(tileAt(world, zsGasOut.add(-1, 0, 1))) + ", expected " + 100 * completed + "mB");
            expect("addGasOutput recipe zs_gas_oxygen_out runs", out != null && runs >= 2 && (outTick < 0 || out.contains("recipe=modularmachinery:zs_gas_oxygen_out")));
            // 恰在一次配方结束 / 下一次开始的那个 tick 采样时，完成次数可能差 1（同上，进度与储罐可能相差 1 tick）
            expect("addGasOutput puts 100 mB oxygen per finished run into the fluid output hatch",
                produced >= 100 && produced % 100 == 0 && Math.abs(produced - 100 * completed) <= 100);
        });
    }

    // ---------------- 物品进出 ----------------

    private final BlockPos[] io = new BlockPos[4];

    private static net.minecraft.inventory.IInventory inventoryAt(World world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
        return te instanceof net.minecraft.inventory.IInventory inv ? inv : null;
    }

    private static int countItem(net.minecraft.inventory.IInventory inv, net.minecraft.item.Item item) {
        if (inv == null) {
            return -1;
        }
        int count = 0;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            net.minecraft.item.ItemStack s = inv.getStackInSlot(i);
            if (s != null && s.getItem() == item) {
                count += s.stackSize;
            }
        }
        return count;
    }

    private void buildItemIo() {
        step("build", 0, () -> {
            World world = sender.getEntityWorld();
            ChunkCoordinates spawn = world.getSpawnPoint();
            net.minecraft.item.ItemStack coal = new net.minecraft.item.ItemStack(net.minecraft.init.Items.coal, 8);
            for (int i = 0; i < io.length; i++) {
                io[i] = groundAbove(world, spawn.posX - 24 - i * 6, spawn.posZ + 3);
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 2; dy++) {
                        world.setBlockToAir(io[i].getX() + dx, io[i].getY() + dy, io[i].getZ());
                    }
                }
            }
            net.minecraft.block.Block inputBus = hellfirepvp.modularmachinery.common.lib.BlocksMM.itemInputBus;
            net.minecraft.block.Block outputBus = hellfirepvp.modularmachinery.common.lib.BlocksMM.itemOutputBus;
            // A：输入总线 + 东侧箱子（总线自动从相邻容器拉取）
            world.setBlock(io[0].getX(), io[0].getY(), io[0].getZ(), inputBus, 2, 3);
            world.setBlock(io[0].getX() + 1, io[0].getY(), io[0].getZ(), net.minecraft.init.Blocks.chest, 0, 3);
            inventoryAt(world, io[0].east()).setInventorySlotContents(0, coal.copy());
            // B：输出总线（预放 8 个煤）+ 东侧箱子（总线自动向相邻容器推送）
            world.setBlock(io[1].getX(), io[1].getY(), io[1].getZ(), outputBus, 2, 3);
            world.setBlock(io[1].getX() + 1, io[1].getY(), io[1].getZ(), net.minecraft.init.Blocks.chest, 0, 3);
            ((hellfirepvp.modularmachinery.common.tiles.base.TileItemBus) world.getTileEntity(io[1].getX(), io[1].getY(), io[1].getZ()))
                .getInventory().setStackInSlot(0, coal.copy());
            // C：漏斗（朝下）在输入总线上方，向总线放入
            world.setBlock(io[2].getX(), io[2].getY(), io[2].getZ(), inputBus, 2, 3);
            world.setBlock(io[2].getX(), io[2].getY() + 1, io[2].getZ(), net.minecraft.init.Blocks.hopper, 0, 3);
            inventoryAt(world, io[2].up()).setInventorySlotContents(0, coal.copy());
            // D：漏斗在输出总线下方，从总线取出（总线也会主动推送给漏斗）
            world.setBlock(io[3].getX(), io[3].getY() + 1, io[3].getZ(), outputBus, 2, 3);
            world.setBlock(io[3].getX(), io[3].getY(), io[3].getZ(), net.minecraft.init.Blocks.hopper, 0, 3);
            ((hellfirepvp.modularmachinery.common.tiles.base.TileItemBus) world.getTileEntity(io[3].getX(), io[3].getY() + 1, io[3].getZ()))
                .getInventory().setStackInSlot(0, coal.copy());
            CommandMMCETest.report(sender, "INFO io setups at " + io[0] + " " + io[1] + " " + io[2] + " " + io[3]);
        });
        step("run", 200, () -> {
        });
        step("check", 0, () -> {
            World world = sender.getEntityWorld();
            net.minecraft.item.Item coal = net.minecraft.init.Items.coal;
            int a = countItem(inventoryAt(world, io[0]), coal);
            int b = countItem(inventoryAt(world, io[1].east()), coal);
            int c = countItem(inventoryAt(world, io[2]), coal);
            int d = countItem(inventoryAt(world, io[3]), coal);
            CommandMMCETest.report(sender, "INFO io counts: inputBusFromChest=" + a + " chestFromOutputBus=" + b + " inputBusFromHopper=" + c + " hopperFromOutputBus=" + d);
            expect("input bus pulls items from an adjacent chest", a == 8);
            expect("output bus pushes items into an adjacent chest", b == 8);
            expect("hopper inserts into an input bus", c == 8);
            expect("hopper extracts from an output bus", d == 8);
        });
    }

    // ---------------- 能量链路 ----------------

    private final BlockPos[] gens = new BlockPos[4];
    private final BlockPos[] asms = new BlockPos[2];
    private BlockPos unpowered;
    private BlockPos ic2Gen;
    private BlockPos ic2Sink;
    private BlockPos ic2BatBoxGen;

    private void buildEnergy() {
        step("build", 40, () -> {
            World world = sender.getEntityWorld();
            ChunkCoordinates spawn = world.getSpawnPoint();
            for (int i = 0; i < gens.length; i++) {
                gens[i] = groundAbove(world, spawn.posX + 24 + i * 12, spawn.posZ + 3);
                CommandMMCETest.buildAt(sender, world, machine(GENERATOR), gens[i]);
            }
            // E6：没有供能的装配机
            unpowered = groundAbove(world, spawn.posX + 24 + gens.length * 12, spawn.posZ + 3);
            CommandMMCETest.buildAt(sender, world, machine(ASSEMBLER), unpowered);
            if (Loader.isModLoaded("IC2")) {
                // E5b：MMCE 发电机 -> IC2 BatBox
                ic2BatBoxGen = groundAbove(world, spawn.posX + 24 + (gens.length + 1) * 12, spawn.posZ + 3);
                CommandMMCETest.buildAt(sender, world, machine(GENERATOR), ic2BatBoxGen);
                // E5a：IC2 火力发电机 -> 单独的 MMCE 能量输入仓
                ic2Gen = groundAbove(world, spawn.posX + 24 + (gens.length + 2) * 12, spawn.posZ + 3);
                ic2Sink = ic2Gen.east();
            }
            for (int i = 0; i < asms.length; i++) {
                // 装配机的能量输入仓在顶层中心 (0,1,1)，放在发电机背后下方，使其正上方正好是第二段电缆
                asms[i] = gens[i].add(0, -2, 3);
                CommandMMCETest.buildAt(sender, world, machine(ASSEMBLER), asms[i]);
            }
        });
        step("fill", 20, () -> {
            World world = sender.getEntityWorld();
            for (BlockPos gen : gens) {
                CommandMMCETest.fillAt(sender, world, machine(GENERATOR), gen, true);
            }
            for (BlockPos asm : asms) {
                CommandMMCETest.fillAt(sender, world, machine(ASSEMBLER), asm, false);
            }
            CommandMMCETest.fillAt(sender, world, machine(ASSEMBLER), unpowered, false);
            if (ic2BatBoxGen != null) {
                CommandMMCETest.fillAt(sender, world, machine(GENERATOR), ic2BatBoxGen, true);
            }
        });
        step("place connectors", 0, () -> {
            World world = sender.getEntityWorld();
            // E3：Mekanism 通用电缆（基础，FMP 部件）
            placeItemAt(world, gens[0].add(0, 0, 3), "Mekanism", "PartTransmitter", 0);
            placeItemAt(world, gens[0].add(0, 0, 4), "Mekanism", "PartTransmitter", 0);
            // E2：EnderIO 能量导管（基础）
            placeItemAt(world, gens[1].add(0, 0, 3), "EnderIO", "itemPowerConduit", 0);
            placeItemAt(world, gens[1].add(0, 0, 4), "EnderIO", "itemPowerConduit", 0);
            // E1：单独的 MMCE 能量输入仓
            BlockPos sink = gens[2].add(0, 0, 3);
            world.setBlock(sink.getX(), sink.getY(), sink.getZ(), hellfirepvp.modularmachinery.common.lib.BlocksMM.energyInputHatch, 0, 3);
            // E4：Mekanism 能量立方（基础）
            net.minecraft.block.Block cube = cpw.mods.fml.common.registry.GameRegistry.findBlock("Mekanism", "EnergyCube");
            if (cube == null) {
                throw new IllegalStateException("Mekanism:EnergyCube not found");
            }
            BlockPos cubePos = gens[3].add(0, 0, 3);
            world.setBlock(cubePos.getX(), cubePos.getY(), cubePos.getZ(), cube, 0, 3);
            if (ic2Gen != null) {
                placeIc2(world, ic2BatBoxGen.add(0, 0, 3), "batBox");
                placeIc2(world, ic2Gen, "generator");
                world.setBlock(ic2Sink.getX(), ic2Sink.getY(), ic2Sink.getZ(), hellfirepvp.modularmachinery.common.lib.BlocksMM.energyInputHatch, 0, 3);
                // 往 IC2 发电机的燃料槽放煤
                try {
                    TileEntity gen = world.getTileEntity(ic2Gen.getX(), ic2Gen.getY(), ic2Gen.getZ());
                    Object fuelSlot = gen.getClass().getField("fuelSlot").get(gen);
                    fuelSlot.getClass().getMethod("put", net.minecraft.item.ItemStack.class)
                        .invoke(fuelSlot, new net.minecraft.item.ItemStack(net.minecraft.init.Items.coal, 8));
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("cannot fuel IC2 generator", e);
                }
            }
        });
        step("run", 600, () -> {
        });
        step("check", 0, () -> {
            World world = sender.getEntityWorld();
            for (int i = 0; i < gens.length; i++) {
                CommandMMCETest.statusAt(sender, world, machine(GENERATOR), gens[i]);
            }
            String viaMek = CommandMMCETest.statusAt(sender, world, machine(ASSEMBLER), asms[0]);
            String viaEio = CommandMMCETest.statusAt(sender, world, machine(ASSEMBLER), asms[1]);
            expect("E3 generator -> Mekanism universal cable -> assembler produced capacitors",
                viaMek != null && viaMek.contains("out=2xEnderIO:itemBasicCapacitor@0"));
            expect("E2 generator -> EnderIO power conduit -> assembler produced capacitors",
                viaEio != null && viaEio.contains("out=2xEnderIO:itemBasicCapacitor@0"));
            BlockPos sink = gens[2].add(0, 0, 3);
            TileEntity sinkTile = world.getTileEntity(sink.getX(), sink.getY(), sink.getZ());
            long sinkEnergy = sinkTile instanceof hellfirepvp.modularmachinery.common.tiles.base.TileEnergyHatch hatch ? hatch.getCurrentEnergy() : -1;
            CommandMMCETest.report(sender, "INFO standalone input hatch energy=" + sinkEnergy);
            // 与上游一致：输出仓不向其他 MMCE 能量仓推送（避免同一台机器的输出仓喂给自己的输入仓），两台机器之间需用电缆 / 导管连接
            // 装了 IC2 时，IC2 能量网会直接在相邻的 EU 源 / 汇之间传输（上游 emitsEnergyTo / acceptsEnergyFrom 同样恒为 true）
            if (Loader.isModLoaded("IC2")) {
                expect("E1 (same as upstream, IC2 present) adjacent MMCE hatches exchange EU through the IC2 energy net", sinkEnergy > 0);
            } else {
                expect("E1 (same as upstream) output hatch does not push RF into another MMCE energy hatch", sinkEnergy == 0);
            }
            BlockPos cubePos = gens[3].add(0, 0, 3);
            double cubeEnergy = reflectEnergy(world.getTileEntity(cubePos.getX(), cubePos.getY(), cubePos.getZ()));
            CommandMMCETest.report(sender, "INFO Mekanism energy cube energy(J)=" + cubeEnergy);
            expect("E4 output hatch charges a Mekanism energy cube", cubeEnergy > 0);
            if (ic2Gen != null) {
                TileEntity sinkTe = world.getTileEntity(ic2Sink.getX(), ic2Sink.getY(), ic2Sink.getZ());
                long euIn = sinkTe instanceof hellfirepvp.modularmachinery.common.tiles.base.TileEnergyHatch h ? h.getCurrentEnergy() : -1;
                CommandMMCETest.report(sender, "INFO IC2 generator -> MMCE input hatch energy(RF)=" + euIn);
                expect("E5a IC2 generator feeds EU into an MMCE energy input hatch", euIn > 0);
                BlockPos box = ic2BatBoxGen.add(0, 0, 3);
                double stored = reflectField(world.getTileEntity(box.getX(), box.getY(), box.getZ()), "energy");
                CommandMMCETest.statusAt(sender, world, machine(GENERATOR), ic2BatBoxGen);
                CommandMMCETest.report(sender, "INFO MMCE output hatch -> IC2 BatBox energy(EU)=" + stored);
                expect("E5b MMCE energy output hatch charges an IC2 BatBox", stored > 0);
            } else {
                CommandMMCETest.report(sender, "SELFTEST SKIP E5 (IC2 not loaded)");
            }
            String idle = CommandMMCETest.statusAt(sender, world, machine(ASSEMBLER), unpowered);
            expect("E6 unpowered assembler waits and reports missing energy",
                idle != null && idle.contains("craftcheck.failure.energy.input") && !idle.contains("out="));
        });
    }

    /** 用假玩家对 pos 下方方块的顶面使用物品，走物品自身的放置逻辑（FMP 部件、EnderIO 导管均如此放置）。 */
    void placeItemAt(World world, BlockPos pos, String modId, String name, int meta) {
        net.minecraft.item.Item item = cpw.mods.fml.common.registry.GameRegistry.findItem(modId, name);
        if (item == null) {
            throw new IllegalStateException(modId + ":" + name + " not found");
        }
        placeItemAt(world, pos, item, meta, modId + ":" + name);
    }

    void placeItemAt(World world, BlockPos pos, net.minecraft.item.Item item, int meta, String label) {
        String modId = label;
        String name = "";
        net.minecraftforge.common.util.FakePlayer fake = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft((net.minecraft.world.WorldServer) world);
        net.minecraft.item.ItemStack stack = new net.minecraft.item.ItemStack(item, 1, meta);
        fake.setCurrentItemOrArmor(0, stack);
        boolean placed = stack.tryPlaceItemIntoWorld(fake, world, pos.getX(), pos.getY() - 1, pos.getZ(), 1, 0.5F, 1.0F, 0.5F);
        CommandMMCETest.report(sender, "INFO place " + modId + name + " at " + pos + " -> " + placed
            + " (" + world.getBlock(pos.getX(), pos.getY(), pos.getZ()).getUnlocalizedName() + ")");
        if (!placed) {
            throw new IllegalStateException("could not place " + modId + name);
        }
    }

    /** 放置 Ic2Items 中名为 field 的方块物品（如 generator、batBox）。 */
    private static void placeIc2(World world, BlockPos pos, String field) {
        try {
            net.minecraft.item.ItemStack stack = (net.minecraft.item.ItemStack) Class.forName("ic2.core.Ic2Items").getField(field).get(null);
            net.minecraft.block.Block block = net.minecraft.block.Block.getBlockFromItem(stack.getItem());
            world.setBlock(pos.getX(), pos.getY(), pos.getZ(), block, stack.getItemDamage(), 3);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot place IC2 " + field, e);
        }
    }

    private static double reflectField(TileEntity te, String name) {
        if (te == null) {
            return -1;
        }
        Class<?> c = te.getClass();
        while (c != null) {
            try {
                java.lang.reflect.Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return ((Number) f.get(te)).doubleValue();
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            } catch (IllegalAccessException e) {
                return -1;
            }
        }
        return -1;
    }

    private static double reflectEnergy(TileEntity te) {
        if (te == null) {
            return -1;
        }
        try {
            return ((Number) te.getClass().getMethod("getEnergy").invoke(te)).doubleValue();
        } catch (ReflectiveOperationException e) {
            return -1;
        }
    }

    // ---------------- 工具 ----------------

    // ---------------- zs-gas-bracket：<gas:...>、气体修改器与几率 ----------------

    private BlockPos gasModPos;

    private int storedGas() {
        return DevGasSupport.storedGas(Worlds.getTileEntity(world(), gasModPos.add(1, 0, 0)));
    }

    private long storedEnergy() {
        return tileAt(gasModPos, 0, 0, 1, TileEnergyHatch.class).getCurrentEnergy();
    }

    private void buildZenScriptGasBracket() {
        if (!Mods.MEKANISM.isPresent()) {
            step("mekanism", 0, () -> expect("Mekanism is loaded", false));
            return;
        }
        step("script recipes registered", 0, () -> {
            File mtLog = new File(Loader.instance().getConfigDir().getParentFile(), "logs/minetweaker.log");
            String log = "";
            try {
                log = mtLog.isFile() ? new String(Files.readAllBytes(mtLog.toPath()), StandardCharsets.UTF_8) : "";
            } catch (IOException ignored) {
            }
            expect("mmce_gas_bracket_test.zs ran; <gas:oxygen>.name and (<gas:oxygen> * 3).amount work", log.contains("MMCE_ZS_GASB script loaded oxygen 3"));
            int unknown = log.indexOf("mmce_no_such_gas");
            String around = unknown < 0 ? "" : log.substring(Math.max(0, unknown - 120), Math.min(log.length(), unknown + 40));
            CommandMMCETest.report(sender, "INFO unknown gas in minetweaker.log: " + around.replace('\n', ' ').replace('\r', ' '));
            expect("unknown <gas:mmce_no_such_gas> is reported as unresolved", around.contains("resolve"));
            expectScriptRecipe(machine("gas_modifier_test"), "zs_gasb_input", "item input", "gas input oxygen x100", "energy output");
            expectScriptRecipe(machine("gas_modifier_test"), "zs_gasb_auto", "item input", "fluid input", "gas input chlorine x50", "gas output oxygen x100");
            expectScriptRecipe(machine("gas_modifier_test"), "zs_gasb_plural", "gas_pertick input chlorine x2", "gas_pertick input hydrogen x1",
                "gas output oxygen x10", "gas output hydrogen x5");
            expectScriptRecipe(machine("gas_modifier_test"), "zs_gasb_modifier", "item input", "gas input oxygen x100", "energy output");
        });
        step("build gas_modifier_test, inject 1000 mB oxygen, insert a stick", 0, () -> {
            ChunkCoordinates spawn = world().getSpawnPoint();
            gasModPos = groundAbove(world(), spawn.posX + 30, spawn.posZ + 40);
            CommandMMCETest.buildAt(sender, world(), machine("gas_modifier_test"), gasModPos);
        });
        step("inject oxygen", 40, () -> {
            int injected = DevGasSupport.receive(Worlds.getTileEntity(world(), gasModPos.add(1, 0, 0)), "oxygen", 1000);
            expect("fluid input hatch accepts 1000 mB oxygen", injected == 1000);
            insertInput(gasModPos, new ItemStack(Items.stick, 1));
        });
        waitUntil("modifier recipe finished (stick consumed, energy produced)", 400,
            () -> countIn(gasModPos, -1, Items.stick) == 0 && activeRecipe(gasModPos) == null && storedEnergy() > 0);
        step("gas modifier", 0, () -> {
            int gas = storedGas();
            CommandMMCETest.report(sender, "INFO after zs_gasb_modifier: oxygen " + gas + " mB, energy " + storedEnergy() + " RF");
            expect("gas input modifier x2: 100 mB recipe consumed 200 mB (1000 -> 800)", gas == 800);
        });
        step("insert paper (chance 0 recipe)", 0, () -> insertInput(gasModPos, new ItemStack(Items.paper, 1)));
        waitUntil("chance recipe finished (paper consumed)", 400,
            () -> countIn(gasModPos, -1, Items.paper) == 0 && activeRecipe(gasModPos) == null);
        step("gas chance 0", 0, () -> {
            int gas = storedGas();
            CommandMMCETest.report(sender, "INFO after zs_gasb_chance: oxygen " + gas + " mB, energy " + storedEnergy() + " RF");
            expect("gas input with chance 0 is not consumed (still 800 mB)", gas == 800);
            expect("chance recipe still ran (energy 2 × 200 RF)", storedEnergy() >= 400);
        });
    }

    // ---------------- adapters：配方适配器 ----------------

    private final java.util.Map<String, BlockPos> adapterPos = new java.util.HashMap<>();

    private static int countByPrefix(DynamicMachine machine, String prefix) {
        int n = 0;
        for (MachineRecipe recipe : RecipeRegistry.getRecipesFor(machine)) {
            if (recipe.getRegistryName().toString().startsWith(prefix)) {
                n++;
            }
        }
        return n;
    }

    /** 只用单个物品 / 流体作输入、产物没有几率的配方，便于确定地核对产物。 */
    private static boolean simple(MachineRecipe recipe) {
        boolean hasOutput = false;
        for (ComponentRequirement<?, ?> req : recipe.getCraftingRequirements()) {
            if (req instanceof RequirementItem item) {
                if (item.required == null || item.oreDictName != null || item.chance < 1F) {
                    return false;
                }
                hasOutput |= req.getActionType() == IOType.OUTPUT;
            } else if (req instanceof RequirementFluid fluid) {
                if (fluid.required == null || fluid.chance < 1F) {
                    return false;
                }
                hasOutput |= req.getActionType() == IOType.OUTPUT;
            } else if (req instanceof RequirementIngredientArray array) {
                if (req.getActionType() != IOType.INPUT || array.getIngredients().isEmpty()) {
                    return false;
                }
            } else if (!(req instanceof RequirementEnergy)) {
                return false;
            }
        }
        return hasOutput;
    }

    private static MachineRecipe pick(DynamicMachine machine, String prefix, java.util.function.Predicate<MachineRecipe> preferred) {
        MachineRecipe first = null;
        for (MachineRecipe recipe : RecipeRegistry.getRecipesFor(machine)) {
            if (!recipe.getRegistryName().toString().startsWith(prefix) || !simple(recipe)) {
                continue;
            }
            if (preferred.test(recipe)) {
                return recipe;
            }
            if (first == null) {
                first = recipe;
            }
        }
        return first;
    }

    private static boolean inputItemIs(MachineRecipe recipe, Item item) {
        for (ComponentRequirement<?, ?> req : recipe.getCraftingRequirements()) {
            if (req instanceof RequirementItem ri && req.getActionType() == IOType.INPUT && ri.required.getItem() == item) {
                return true;
            }
        }
        return false;
    }

    /** 输入是配料组（矿辞等多个候选），且候选中包含该物品。 */
    private static boolean ingredientArrayContains(MachineRecipe recipe, Item item) {
        for (ComponentRequirement<?, ?> req : recipe.getCraftingRequirements()) {
            if (req instanceof RequirementIngredientArray array && req.getActionType() == IOType.INPUT) {
                for (ChancedIngredientStack ingredient : array.getIngredients()) {
                    if (ingredient.itemStack != null && ingredient.itemStack.getItem() == item) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static String describe(MachineRecipe recipe) {
        StringBuilder sb = new StringBuilder(recipe.getRegistryName().toString()).append(" ").append(recipe.getRecipeTotalTickTime()).append("t");
        for (ComponentRequirement<?, ?> req : recipe.getCraftingRequirements()) {
            sb.append(" | ").append(req.getActionType() == IOType.INPUT ? "in " : "out ");
            if (req instanceof RequirementItem ri) {
                sb.append(ItemStacks.getCount(ri.required)).append("x").append(ri.required.getDisplayName()).append("@").append(ri.required.getItemDamage());
            } else if (req instanceof RequirementFluid rf) {
                sb.append(rf.required.amount).append("mB ").append(rf.required.getFluid().getName());
            } else if (req instanceof RequirementEnergy re) {
                sb.append(re.requirementPerTick).append(" RF/t");
            } else if (req instanceof RequirementIngredientArray array) {
                sb.append("one of ").append(array.getIngredients().size()).append(" (");
                for (ChancedIngredientStack ingredient : array.getIngredients()) {
                    sb.append(ItemStacks.getCount(ingredient.itemStack)).append("x").append(ingredient.itemStack.getDisplayName()).append(", ");
                }
                sb.append(")");
            }
        }
        return sb.toString();
    }

    private final java.util.Map<String, MachineRecipe> adapterRecipe = new java.util.HashMap<>();
    /** 配料组输入时优先放入的候选物品。 */
    private Item preferredIngredient;

    private void supply(String machineName, MachineRecipe recipe) {
        BlockPos ctrl = adapterPos.get(machineName);
        clearBus(ctrl, -1);
        clearBus(ctrl, 1);
        for (int[] off : new int[][]{{-1, 0, 1}, {-1, 0, 2}, {1, 0, 1}}) {
            TileFluidTank tank = tileAt(ctrl, off[0], off[1], off[2], TileFluidTank.class);
            tank.getTank().setFluid(null);
        }
        TileEnergyHatch energy = tileAt(ctrl, 0, 0, 1, TileEnergyHatch.class);
        energy.setCurrentEnergy(energy.getMaxEnergy());
        int fluidSlot = 0;
        int[][] fluidInputs = {{-1, 0, 1}, {-1, 0, 2}};
        for (ComponentRequirement<?, ?> req : recipe.getCraftingRequirements()) {
            if (req.getActionType() != IOType.INPUT) {
                continue;
            }
            if (req instanceof RequirementItem ri) {
                insertInput(ctrl, ri.required.copy());
            } else if (req instanceof RequirementIngredientArray array) {
                ChancedIngredientStack chosen = array.getIngredients().get(0);
                for (ChancedIngredientStack ingredient : array.getIngredients()) {
                    if (ingredient.itemStack != null && ingredient.itemStack.getItem() == preferredIngredient) {
                        chosen = ingredient;
                    }
                }
                CommandMMCETest.report(sender, "INFO supplying ingredient " + chosen.itemStack);
                insertInput(ctrl, chosen.itemStack.copy());
            } else if (req instanceof RequirementFluid rf) {
                int[] off = fluidInputs[fluidSlot++];
                tileAt(ctrl, off[0], off[1], off[2], TileFluidTank.class).getTank().fill(rf.required.copy(), true);
            }
        }
    }

    private boolean outputsPresent(String machineName, MachineRecipe recipe) {
        BlockPos ctrl = adapterPos.get(machineName);
        for (ComponentRequirement<?, ?> req : recipe.getCraftingRequirements()) {
            if (req.getActionType() != IOType.OUTPUT) {
                continue;
            }
            if (req instanceof RequirementItem ri) {
                TileItemBus bus = tileAt(ctrl, 1, 0, 0, TileItemBus.class);
                int n = 0;
                for (int i = 0; i < bus.getInventory().getSlots(); i++) {
                    ItemStack s = bus.getInventory().getStackInSlot(i);
                    if (!ItemStacks.isEmpty(s) && s.getItem() == ri.required.getItem() && s.getItemDamage() == ri.required.getItemDamage()) {
                        n += ItemStacks.getCount(s);
                    }
                }
                if (n < ItemStacks.getCount(ri.required)) {
                    return false;
                }
            } else if (req instanceof RequirementFluid rf) {
                net.minecraftforge.fluids.FluidStack in = tileAt(ctrl, 1, 0, 1, TileFluidTank.class).getTank().getFluid();
                if (in == null || !in.isFluidEqual(rf.required) || in.amount < rf.required.amount) {
                    return false;
                }
            }
        }
        return true;
    }

    private void runAdapter(String label, String machineName, String prefix, java.util.function.Predicate<MachineRecipe> preferred) {
        step(label + ": pick a recipe and supply its inputs", 0, () -> {
            MachineRecipe recipe = pick(machine(machineName), prefix, preferred);
            if (recipe == null) {
                throw new IllegalStateException("no simple " + prefix + " recipe");
            }
            adapterRecipe.put(label, recipe);
            CommandMMCETest.report(sender, "INFO " + label + " recipe " + describe(recipe));
            supply(machineName, recipe);
        });
        waitUntil(label + ": outputs produced", 400, () -> outputsPresent(machineName, adapterRecipe.get(label)));
        step(label + ": result", 0, () -> {
            CommandMMCETest.report(sender, "INFO " + label + " after run: "
                + CommandMMCETest.statusAt(sender, world(), machine(machineName), adapterPos.get(machineName)));
            expect(label + ": recipe ran and produced its outputs", outputsPresent(machineName, adapterRecipe.get(label)));
        });
    }

    private void expectCount(String label, DynamicMachine machine, String prefix, int source) {
        int converted = countByPrefix(machine, prefix);
        CommandMMCETest.report(sender, "INFO " + label + ": source " + source + ", converted " + converted);
        expect(label + ": every source recipe converted (" + converted + " of " + source + ")", source > 0 && converted == source);
    }

    private void buildAdapters() {
        step("build adapter test machines", 60, () -> {
            ChunkCoordinates spawn = world().getSpawnPoint();
            int x = spawn.posX - 30;
            for (String name : new String[]{"adapter_ic2", "adapter_tic", "adapter_te"}) {
                if (machine(name) == null) {
                    continue;
                }
                BlockPos pos = groundAbove(world(), x, spawn.posZ + 3);
                CommandMMCETest.buildAt(sender, world(), machine(name), pos);
                adapterPos.put(name, pos);
                x -= 8;
            }
        });
        step("adapter recipe counts", 0, () -> {
            if (Mods.IC2.isPresent()) {
                expectCount("ic2:te_macerator", machine("adapter_ic2"), "ic2:te_macerator_recipe_", DevAdapterSupport.ic2("macerator"));
                expectCount("ic2:te_compressor", machine("adapter_ic2"), "ic2:te_compressor_recipe_", DevAdapterSupport.ic2("compressor"));
                expectCount("ic2:te_extractor", machine("adapter_ic2"), "ic2:te_extractor_recipe_", DevAdapterSupport.ic2("extractor"));
            }
            if (Mods.TCONSTRUCT.isPresent()) {
                expectCount("tconstruct:smeltery_melting", machine("adapter_tic"), "tconstruct:smeltery_melting_", DevAdapterSupport.ticMelting());
                expectCount("tconstruct:smeltery_alloy", machine("adapter_tic"), "tconstruct:smeltery_alloy_", DevAdapterSupport.ticAlloy());
            }
            if (Mods.THERMAL_EXPANSION.isPresent()) {
                expectCount("thermalexpansion:insolator", machine("adapter_te"), "thermalexpansion:insolator_", DevAdapterSupport.teInsolator());
            }
        });
        if (Mods.IC2.isPresent()) {
            step("prefer iron ore for ingredient arrays", 0, () -> preferredIngredient = Item.getItemFromBlock(Blocks.iron_ore));
            runAdapter("IC2 macerator (iron ore, ore dictionary input)", "adapter_ic2", "ic2:te_macerator_recipe_",
                r -> ingredientArrayContains(r, Item.getItemFromBlock(Blocks.iron_ore)));
            runAdapter("IC2 compressor", "adapter_ic2", "ic2:te_compressor_recipe_", r -> true);
        }
        if (Mods.TCONSTRUCT.isPresent()) {
            runAdapter("TConstruct melting (iron ingot)", "adapter_tic", "tconstruct:smeltery_melting_", r -> inputItemIs(r, Items.iron_ingot));
            runAdapter("TConstruct alloy", "adapter_tic", "tconstruct:smeltery_alloy_", r -> true);
        }
        if (Mods.THERMAL_EXPANSION.isPresent()) {
            runAdapter("TE insolator", "adapter_te", "thermalexpansion:insolator_", r -> true);
        }
    }

    // ---------------- logic：并行、修改器、升级、智能接口、工厂 ----------------

    private static final String LOGIC = "logic_test";
    private BlockPos logicPos;
    private BlockPos factoryPos;

    World world() {
        return sender.getEntityWorld();
    }

    private <T> T tileAt(BlockPos ctrl, int dx, int dy, int dz, Class<T> type) {
        TileEntity te = Worlds.getTileEntity(world(), ctrl.add(dx, dy, dz));
        if (!type.isInstance(te)) {
            throw new IllegalStateException("expected " + type.getSimpleName() + " at " + ctrl.add(dx, dy, dz) + ", found " + te);
        }
        return type.cast(te);
    }

    private TileMultiblockMachineController controllerAt(BlockPos pos) {
        return tileAt(pos, 0, 0, 0, TileMultiblockMachineController.class);
    }

    private void insertInput(BlockPos ctrl, ItemStack stack) {
        TileItemBus bus = tileAt(ctrl, -1, 0, 0, TileItemBus.class);
        ItemStack rest = ItemHandlerHelper.insertItem(bus.getInventory(), stack, false);
        if (!ItemStacks.isEmpty(rest)) {
            throw new IllegalStateException("input bus full, " + rest + " left");
        }
    }

    private int countIn(BlockPos ctrl, int dx, Item item) {
        TileItemBus bus = tileAt(ctrl, dx, 0, 0, TileItemBus.class);
        int n = 0;
        for (int i = 0; i < bus.getInventory().getSlots(); i++) {
            ItemStack s = bus.getInventory().getStackInSlot(i);
            if (!ItemStacks.isEmpty(s) && s.getItem() == item) {
                n += ItemStacks.getCount(s);
            }
        }
        return n;
    }

    private void clearBus(BlockPos ctrl, int dx) {
        TileItemBus bus = tileAt(ctrl, dx, 0, 0, TileItemBus.class);
        for (int i = 0; i < bus.getInventory().getSlots(); i++) {
            bus.getInventory().setStackInSlot(i, ItemStacks.EMPTY);
        }
    }

    private ActiveMachineRecipe activeRecipe(BlockPos ctrl) {
        TileMultiblockMachineController c = controllerAt(ctrl);
        return c instanceof TileMachineController mc ? mc.getActiveRecipe() : null;
    }

    private void reportRecipe(String label, BlockPos ctrl) {
        ActiveMachineRecipe r = activeRecipe(ctrl);
        CommandMMCETest.report(sender, "INFO " + label + ": " + (r == null ? "no active recipe"
            : r.getRegistryName() + " tick=" + r.getTick() + "/" + r.getTotalTick() + " parallelism=" + r.getParallelism()));
    }

    private void expectRecipe(String label, BlockPos ctrl, String recipe, int totalTick, int parallelism) {
        ActiveMachineRecipe r = activeRecipe(ctrl);
        reportRecipe(label, ctrl);
        expect(label + ": " + recipe + " total " + totalTick + " t, parallelism " + parallelism,
            r != null && r.getRegistryName().contains(recipe) && r.getTotalTick() == totalTick && r.getParallelism() == parallelism);
    }

    private TileParallelController.ParallelControllerProvider parallel(BlockPos ctrl) {
        return tileAt(ctrl, -1, 0, 1, TileParallelController.class).provideComponent();
    }

    private TileUpgradeBus upgradeBus(BlockPos ctrl) {
        return tileAt(ctrl, 1, 0, 1, TileUpgradeBus.class);
    }

    private int upgradeStackSize(BlockPos ctrl) {
        int total = 0;
        for (List<MachineUpgrade> list : upgradeBus(ctrl).provideComponent().getUpgrades(controllerAt(ctrl)).values()) {
            for (MachineUpgrade upgrade : list) {
                if ("mmce_test_speed".equals(upgrade.getType().getName())) {
                    total += upgrade.getStackSize();
                }
            }
        }
        return total;
    }

    private int controllerUpgradeSize(BlockPos ctrl) {
        int total = 0;
        for (MachineUpgrade upgrade : controllerAt(ctrl).getFoundUpgrades().getOrDefault("mmce_test_speed", java.util.Collections.emptyList())) {
            total += upgrade.getStackSize();
        }
        return total;
    }

    private int busUpgradeSize(TileUpgradeBus bus) {
        int total = 0;
        for (List<MachineUpgrade> list : bus.provideComponent().getUpgrades(null).values()) {
            for (MachineUpgrade upgrade : list) {
                if ("mmce_test_speed".equals(upgrade.getType().getName())) {
                    total += upgrade.getStackSize();
                }
            }
        }
        return total;
    }

    private int factoryWorking() {
        int working = 0;
        for (FactoryRecipeThread thread : tileAt(factoryPos, 0, 0, 0, TileFactoryController.class).getFactoryRecipeThreadList()) {
            if (thread.getActiveRecipe() != null) {
                working++;
            }
        }
        return working;
    }

    /** 等当前配方结束：清空输入，等到没有进行中的配方，再清空输出。 */
    private void drain(java.util.function.Supplier<BlockPos> ctrl, String name) {
        step("drain (" + name + ")", 0, () -> clearBus(ctrl.get(), -1));
        waitUntil("recipe finished (" + name + ")", 200, () -> activeRecipe(ctrl.get()) == null);
        step("clear output (" + name + ")", 5, () -> clearBus(ctrl.get(), 1));
    }

    private void awaitRecipe(String name, java.util.function.Supplier<BlockPos> ctrl) {
        waitUntil("recipe started (" + name + ")", 200, () -> activeRecipe(ctrl.get()) != null);
    }

    private void buildLogic() {
        step("build logic_test (controller) and logic_test (factory controller)", 60, () -> {
            ChunkCoordinates spawn = world().getSpawnPoint();
            logicPos = groundAbove(world(), spawn.posX - 12, spawn.posZ + 3);
            factoryPos = groundAbove(world(), spawn.posX - 20, spawn.posZ + 3);
            CommandMMCETest.buildAt(sender, world(), machine(LOGIC), logicPos);
            CommandMMCETest.buildAt(sender, world(), machine(LOGIC), factoryPos);
            BlockFactoryController factory = BlockFactoryController.FACTORY_CONTROLLERS.get(machine(LOGIC));
            if (factory == null) {
                throw new IllegalStateException("no factory controller block for logic_test");
            }
            world().setBlock(factoryPos.getX(), factoryPos.getY(), factoryPos.getZ(), factory, EnumFacing.NORTH.getHorizontalIndex(), 3);
            CommandMMCETest.report(sender, "INFO logic_test controller=" + logicPos + " factory=" + factoryPos);
        });
        step("structures formed", 0, () -> {
            for (BlockPos pos : new BlockPos[]{logicPos, factoryPos}) {
                TileMultiblockMachineController c = controllerAt(pos);
                expect("logic_test formed at " + pos + " (" + c.getClass().getSimpleName() + ")",
                    c.isStructureFormed() && machine(LOGIC).equals(c.getFoundMachine()));
            }
            TileParallelController.ParallelControllerProvider p = parallel(logicPos);
            CommandMMCETest.report(sender, "INFO parallel controller max=" + p.getMaxParallelism() + ", machine max parallelism="
                + controllerAt(logicPos).getMaxParallelism() + ", upgrade bus slots=" + upgradeBus(logicPos).getInventory().getSlots());
        });

        // 并行控制器：并行数 1 -> 每次 1 个；并行数 4 -> 一次配方 4 个
        step("parallelism 1, insert 8 cobblestone", 0, () -> {
            parallel(logicPos).setParallelism(1);
            insertInput(logicPos, new ItemStack(Blocks.cobblestone, 8));
        });
        awaitRecipe("parallel 1", () -> logicPos);
        step("parallelism 1 recipe", 0, () -> expectRecipe("parallel 1", logicPos, "logic_test_cobble", 40, 1));
        drain(() -> logicPos, "parallel 1");
        step("parallelism 4, insert 8 cobblestone", 0, () -> {
            parallel(logicPos).setParallelism(4);
            insertInput(logicPos, new ItemStack(Blocks.cobblestone, 8));
        });
        awaitRecipe("parallel 4", () -> logicPos);
        step("parallelism 4 recipe", 0, () -> expectRecipe("parallel 4", logicPos, "logic_test_cobble", 40, 4));
        waitUntil("parallel-4 recipe finished", 100, () -> countIn(logicPos, 1, Item.getItemFromBlock(Blocks.stone)) >= 4);
        step("parallelism 4 output", 0, () -> {
            int stone = countIn(logicPos, 1, Item.getItemFromBlock(Blocks.stone));
            CommandMMCETest.report(sender, "INFO stone after one parallel-4 recipe: " + stone + " | "
                + CommandMMCETest.statusAt(sender, world(), machine(LOGIC), logicPos));
            expect("one parallel-4 recipe produced at least 4 stone", stone >= 4);
            parallel(logicPos).setParallelism(1);
        });
        drain(() -> logicPos, "parallel 4");

        // 机器修改器：结构顶上（0, 1, 1）放金块 -> 配方时间 × 0.5
        step("place gold block (modifier)", 60, () ->
            world().setBlock(logicPos.getX(), logicPos.getY() + 1, logicPos.getZ() + 1, Blocks.gold_block, 0, 3));
        step("insert cobblestone (modifier)", 0, () -> insertInput(logicPos, new ItemStack(Blocks.cobblestone, 4)));
        awaitRecipe("modifier", () -> logicPos);
        step("modifier recipe", 0, () -> {
            CommandMMCETest.report(sender, "INFO found modifiers " + controllerAt(logicPos).getFoundModifiers().keySet());
            expectRecipe("gold block modifier", logicPos, "logic_test_cobble", 20, 1);
        });
        drain(() -> logicPos, "modifier");
        step("remove gold block", 60, () -> world().setBlockToAir(logicPos.getX(), logicPos.getY() + 1, logicPos.getZ() + 1));

        // 机械升级：1 个钻石 -> 0.5 倍；共 3 个（两组）-> 0.125 倍；反复重新统计后数量不变
        step("upgrade: 1 diamond", 0, () -> {
            upgradeBus(logicPos).getInventory().setStackInSlot(0, new ItemStack(Items.diamond, 1));
            insertInput(logicPos, new ItemStack(Blocks.cobblestone, 4));
        });
        awaitRecipe("upgrade x1", () -> logicPos);
        step("upgrade x1 recipe", 0, () -> {
            expect("upgrade stack size 1", upgradeStackSize(logicPos) == 1);
            expectRecipe("upgrade x1", logicPos, "logic_test_cobble", 20, 1);
        });
        drain(() -> logicPos, "upgrade x1");
        step("upgrade: 2 + 1 diamonds, recount 5 times", 0, () -> {
            TileUpgradeBus bus = upgradeBus(logicPos);
            bus.getInventory().setStackInSlot(0, new ItemStack(Items.diamond, 2));
            bus.getInventory().setStackInSlot(1, new ItemStack(Items.diamond, 1));
            for (int i = 0; i < 5; i++) {
                bus.onUpgradeInventoryChanged(-1);
            }
            insertInput(logicPos, new ItemStack(Blocks.cobblestone, 4));
        });
        awaitRecipe("upgrade x3", () -> logicPos);
        step("upgrade x3 recipe", 0, () -> {
            int size = upgradeStackSize(logicPos);
            CommandMMCETest.report(sender, "INFO upgrade stack size after 5 recounts: " + size);
            expect("upgrade stack size stays 3 after repeated recounts", size == 3);
            expectRecipe("upgrade x3", logicPos, "logic_test_cobble", 5, 1);
        });
        drain(() -> logicPos, "upgrade x3");
        // 两个升级总线：第二个放在 (1, 0, 2)（结构允许外壳或升级总线），2 + 1 个钻石应合计为 3，且多次结构检查后不变
        step("second upgrade bus: 2 diamonds here, 1 there", 0, () -> {
            upgradeBus(logicPos).getInventory().setStackInSlot(0, new ItemStack(Items.diamond, 2));
            upgradeBus(logicPos).getInventory().setStackInSlot(1, ItemStacks.EMPTY);
            world().setBlock(logicPos.getX() + 1, logicPos.getY(), logicPos.getZ() + 2, BlocksMM.upgradeBus, 0, 3);
            tileAt(logicPos, 1, 0, 2, TileUpgradeBus.class).getInventory().setStackInSlot(0, new ItemStack(Items.diamond, 1));
        });
        waitUntil("both upgrade buses found", 200, () -> controllerAt(logicPos).getFoundUpgrades().containsKey("mmce_test_speed")
            && controllerUpgradeSize(logicPos) == 3);
        step("wait several structure checks", 200, () -> {
        });
        step("two upgrade buses", 0, () -> {
            int controller = controllerUpgradeSize(logicPos);
            int first = upgradeStackSize(logicPos);
            int second = busUpgradeSize(tileAt(logicPos, 1, 0, 2, TileUpgradeBus.class));
            CommandMMCETest.report(sender, "INFO two buses: controller=" + controller + " first bus=" + first + " second bus=" + second);
            expect("controller sums both upgrade buses (3) after several structure checks", controller == 3);
            expect("each bus keeps its own count (2 and 1)", first == 2 && second == 1);
        });
        step("insert cobblestone (two buses)", 0, () -> insertInput(logicPos, new ItemStack(Blocks.cobblestone, 4)));
        awaitRecipe("two buses", () -> logicPos);
        step("two buses recipe", 0, () -> expectRecipe("two upgrade buses (x3)", logicPos, "logic_test_cobble", 5, 1));
        drain(() -> logicPos, "two buses");
        step("remove upgrades and second bus", 60, () -> {
            upgradeBus(logicPos).getInventory().setStackInSlot(0, ItemStacks.EMPTY);
            upgradeBus(logicPos).getInventory().setStackInSlot(1, ItemStacks.EMPTY);
            tileAt(logicPos, 1, 0, 2, TileUpgradeBus.class).getInventory().setStackInSlot(0, ItemStacks.EMPTY);
            world().setBlock(logicPos.getX() + 1, logicPos.getY(), logicPos.getZ() + 2, BlocksMM.blockCasing, 0, 3);
        });

        // 智能接口：mode 默认 0，配方要求 5–10
        step("smart interface: insert dirt with mode 0", 120, () -> insertInput(logicPos, new ItemStack(Blocks.dirt, 1)));
        step("smart interface mode 0", 0, () -> {
            TileSmartInterface.SmartInterfaceProvider si = tileAt(logicPos, -1, 0, 2, TileSmartInterface.class).provideComponent();
            SmartInterfaceData data = si.getMachineData(logicPos);
            CommandMMCETest.report(sender, "INFO smart interface data " + (data == null ? "none" : data.getType() + "=" + data.getValue()));
            expect("smart interface bound to logic_test with type mode", data != null && "mode".equals(data.getType()));
            reportRecipe("smart interface mode 0", logicPos);
            expect("dirt recipe does not run while mode is 0", activeRecipe(logicPos) == null
                && countIn(logicPos, -1, Item.getItemFromBlock(Blocks.dirt)) == 1);
        });
        step("smart interface: set mode 7", 0, () -> {
            TileSmartInterface owner = tileAt(logicPos, -1, 0, 2, TileSmartInterface.class);
            SmartInterfaceData data = owner.provideComponent().getMachineData(logicPos);
            data.setValue(7F);
            TileSmartInterface.onDataUpdate(owner, data);
            owner.markForUpdateSync();
        });
        waitUntil("grass produced", 300, () -> countIn(logicPos, 1, Item.getItemFromBlock(Blocks.grass)) >= 1);
        step("smart interface mode 7", 0, () -> {
            int grass = countIn(logicPos, 1, Item.getItemFromBlock(Blocks.grass));
            reportRecipe("smart interface mode 7", logicPos);
            expect("dirt recipe ran with mode 7 (grass in output bus: " + grass + ")", grass == 1
                && countIn(logicPos, -1, Item.getItemFromBlock(Blocks.dirt)) == 0);
        });

        // 工厂控制器：2 个线程同时运行
        step("factory: parallelism 1, insert 4 cobblestone", 0, () -> {
            parallel(factoryPos).setParallelism(1);
            insertInput(factoryPos, new ItemStack(Blocks.cobblestone, 4));
        });
        waitUntil("factory: both threads working", 200, () -> factoryWorking() == 2);
        step("factory threads", 0, () -> {
            TileFactoryController factory = tileAt(factoryPos, 0, 0, 0, TileFactoryController.class);
            int working = 0;
            for (FactoryRecipeThread thread : factory.getFactoryRecipeThreadList()) {
                ActiveMachineRecipe r = thread.getActiveRecipe();
                if (r != null) {
                    working++;
                    CommandMMCETest.report(sender, "INFO factory thread: " + r.getRegistryName() + " tick=" + r.getTick() + "/" + r.getTotalTick());
                }
            }
            CommandMMCETest.report(sender, "INFO factory max threads=" + factory.getMaxThreads() + ", working=" + working);
            expect("factory has 2 threads", factory.getMaxThreads() == 2);
            expect("factory runs 2 recipes at the same time", working == 2);
        });
        waitUntil("factory: 2 stone", 100, () -> countIn(factoryPos, 1, Item.getItemFromBlock(Blocks.stone)) >= 2);
        step("factory output", 0, () -> {
            int stone = countIn(factoryPos, 1, Item.getItemFromBlock(Blocks.stone));
            CommandMMCETest.report(sender, "INFO factory stone after one recipe time: " + stone + " | "
                + CommandMMCETest.statusAt(sender, world(), machine(LOGIC), factoryPos));
            expect("factory produced 2 stone in one recipe time", stone >= 2);
        });
    }

    static BlockPos groundAbove(World world, int x, int z) {
        int top = world.getTopSolidOrLiquidBlock(x, z);
        // 结构最底层（y = -1）放在地面方块的位置上
        return new BlockPos(x, top + 1, z);
    }

    private static String toText(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    private static long number(Pattern pattern, String line, int group) {
        if (line == null) {
            return -1;
        }
        Matcher m = pattern.matcher(line);
        return m.find() ? Long.parseLong(m.group(group)) : -1;
    }

    void expect(String name, boolean ok) {
        if (!ok) {
            failures++;
        }
        CommandMMCETest.report(sender, "SELFTEST " + (ok ? "OK   " : "FAIL ") + name);
    }

    void step(String name, int waitAfter, Runnable action) {
        steps.add(new Step(name, waitAfter, action, null));
    }

    /** 等到条件成立（每 5 tick 检查一次），超过 maxTicks 记为失败。 */
    void waitUntil(String name, int maxTicks, java.util.function.BooleanSupplier condition) {
        steps.add(new Step(name, maxTicks, null, condition));
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            // 原版 WorldServer#updateEntities：没有玩家（也没有强制加载区块）的世界连续 1200 tick 后停止更新实体与方块实体。
            // 专用服务器上的自测没有玩家，测试机器运行超过 1 分钟就会全部停住（实测：能量测试机在 ticksExisted = 1199 时不再 tick）。
            // 自测期间像有玩家在线一样每 tick 重置该计数。
            for (net.minecraft.world.WorldServer ws : MinecraftServer.getServer().worldServers) {
                ws.resetUpdateEntityTick();
            }
            return;
        }
        if (wait > 0) {
            wait--;
            return;
        }
        Step step = steps.poll();
        if (step == null) {
            finish();
            return;
        }
        if (step.condition != null) {
            boolean ok;
            try {
                ok = step.condition.getAsBoolean();
            } catch (Throwable t) {
                ok = false;
            }
            if (!ok && step.waitAfter > 0) {
                // 每 5 tick 再检查一次，剩余时间记在 waitAfter 里
                steps.addFirst(new Step(step.name, step.waitAfter - 5, null, step.condition));
                wait = 5;
                return;
            }
            if (!ok) {
                failures++;
                CommandMMCETest.report(sender, "SELFTEST FAIL " + step.name + ": timed out");
            } else {
                CommandMMCETest.report(sender, "SELFTEST OK   " + step.name);
            }
            return;
        }
        try {
            step.action.run();
        } catch (Throwable t) {
            failures++;
            CommandMMCETest.report(sender, "SELFTEST FAIL " + step.name + ": " + t);
            ModularMachinery.log.error("[mmce-test] selftest step failed: " + step.name, t);
        }
        wait = step.waitAfter;
    }

    private void finish() {
        FMLCommonHandler.instance().bus().unregister(this);
        CommandMMCETest.report(sender, "SELFTEST result " + (failures == 0 ? "PASS" : "FAIL (" + failures + ")"));
        if (exit) {
            MinecraftServer.getServer().initiateShutdown();
        }
    }

    @Desugar
    private record Step(String name, int waitAfter, Runnable action, java.util.function.BooleanSupplier condition) {
    }
}
