package hellfirepvp.modularmachinery.common.dev;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModAPIManager;
import cpw.mods.fml.common.registry.GameRegistry;
import github.kasuminova.mmce.compat.ResourceLocation;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.RecipeRegistry;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.launchwrapper.Launch;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * 仅在开发环境（deobf）运行：确认测试用依赖已加载，以及测试机器/配方引用的物品 ID 在当前版本中存在。
 * 结果写入日志和运行目录下的 mmce-dev-probe.txt。发布环境下不做任何事。
 */
public final class DevEnvironmentProbe {

    private static final String[] MODS = {
        "gtnhlib", "MineTweaker3", "NotEnoughItems", "Waila",
        "ForgeMultipart", "Mekanism", "MekanismGenerators", "MekanismTools",
        "EnderIO", "endercore", "CoFHLib"
    };

    private static final String[] ITEMS = {
        "Mekanism:Ingot", "Mekanism:EnrichedAlloy", "Mekanism:ControlCircuit",
        "EnderIO:itemBasicCapacitor", "EnderIO:itemAlloy",
        "minecraft:coal", "minecraft:redstone"
    };

    private static final String[] BLOCKS = {
        "Mekanism:BasicBlock", "minecraft:stonebrick",
        // MMCE 自身方块（与 1.12.2 相同的注册名）
        "modularmachinery:blockcontroller", "modularmachinery:blockfactorycontroller", "modularmachinery:blockcasing",
        "modularmachinery:blockinputbus", "modularmachinery:blockoutputbus",
        "modularmachinery:blockfluidinputhatch", "modularmachinery:blockfluidoutputhatch",
        "modularmachinery:blockenergyinputhatch", "modularmachinery:blockenergyoutputhatch",
        "modularmachinery:blocksmartinterface", "modularmachinery:blockparallelcontroller", "modularmachinery:blockupgradebus"
    };
    private static final String[] MMCE_ITEMS = {
        "modularmachinery:itemblueprint", "modularmachinery:itemmodularium", "modularmachinery:itemconstructtool",
        "modularmachinery:blockcasing", "modularmachinery:blockcontroller"
    };
    /** 默认机器（assets/modularmachinery/default_machinery）在首次运行时复制到配置目录。 */
    private static final String[] EXPECTED_MACHINES = {
        "modularmachinery:alloy_furnace", "modularmachinery:iron_centrifuge"
    };
    /**
     * 测试机器（仓库 testdata/modularmachinery，由测试脚本复制到运行目录）：注册名、结构方块数（不含控制器）、配方数。
     * 对应 docs/MIGRATION_PLAN.md 第 7.2 节。
     */
    private static final Object[][] TEST_MACHINES = {
        {"mek_eio_assembler", 26, 2},
        // B3（氢气）与电解槽的配方使用 modularmachinery:gas，只在装有 Mekanism 时加载
        {"fuel_generator", 26, hellfirepvp.modularmachinery.common.base.Mods.MEKANISM.isPresent() ? 3 : 2},
        {"gas_electrolyzer", 5, hellfirepvp.modularmachinery.common.base.Mods.MEKANISM.isPresent() ? 1 : 0}
    };

    private DevEnvironmentProbe() {
    }

    /** 记录已加载机器的结构方块数与配方数；缺少期望的默认机器或结构为空时计为缺失。 */
    private static int probeMachines(List<String> lines) {
        int missing = 0;
        List<DynamicMachine> machines = MachineRegistry.getLoadedMachines();
        lines.add("machines loaded " + machines.size());
        for (DynamicMachine machine : machines) {
            int parts = machine.getPattern().getPattern().size();
            int recipes = 0;
            for (MachineRecipe ignored : RecipeRegistry.getRecipesFor(machine)) {
                recipes++;
            }
            if (parts == 0) missing++;
            lines.add("machine " + (parts > 0 ? "OK     " : "EMPTY  ") + " " + machine.getRegistryName() + " parts=" + parts + " recipes=" + recipes);
        }
        File machineryDir = CommonProxy.dataHolder.getMachineryDirectory();
        for (Object[] test : TEST_MACHINES) {
            String name = (String) test[0];
            if (machineryDir == null || !new File(machineryDir, name + ".json").isFile()) {
                lines.add("test   SKIP    " + name + " (testdata not copied)");
                continue;
            }
            DynamicMachine machine = MachineRegistry.getRegistry().getMachine(new ResourceLocation(ModularMachinery.MODID, name));
            if (machine == null) {
                missing++;
                lines.add("test   MISSING " + name);
                continue;
            }
            int parts = machine.getPattern().getPattern().size();
            int recipes = 0;
            for (MachineRecipe ignored : RecipeRegistry.getRecipesFor(machine)) {
                recipes++;
            }
            boolean ok = parts == (Integer) test[1] && recipes == (Integer) test[2];
            if (!ok) missing++;
            lines.add("test   " + (ok ? "OK     " : "WRONG  ") + " " + name + " parts=" + parts + "/" + test[1] + " recipes=" + recipes + "/" + test[2]);
        }
        for (String expected : EXPECTED_MACHINES) {
            boolean found = false;
            for (DynamicMachine machine : machines) {
                if (machine.getRegistryName().toString().equals(expected)) {
                    found = true;
                    break;
                }
            }
            if (!found) missing++;
            lines.add("expect " + (found ? "OK     " : "MISSING") + " " + expected);
        }
        return missing;
    }

    public static void runIfDevEnvironment() {
        Object deobf = Launch.blackboard.get("fml.deobfuscatedEnvironment");
        if (!Boolean.TRUE.equals(deobf)) {
            return;
        }

        List<String> lines = new ArrayList<>();
        int missing = 0;
        for (String modId : MODS) {
            // CoFHLib 等库以 @API 形式注册，不是 ModContainer，需要同时查 API 管理器
            boolean loaded = Loader.isModLoaded(modId) || ModAPIManager.INSTANCE.hasAPI(modId);
            if (!loaded) missing++;
            lines.add("mod   " + (loaded ? "OK     " : "MISSING") + " " + modId);
        }
        for (String id : ITEMS) {
            String[] split = id.split(":", 2);
            Item item = GameRegistry.findItem(split[0], split[1]);
            if (item == null) missing++;
            lines.add("item  " + (item != null ? "OK     " : "MISSING") + " " + id);
        }
        for (String id : BLOCKS) {
            String[] split = id.split(":", 2);
            Block block = GameRegistry.findBlock(split[0], split[1]);
            if (block == null) missing++;
            lines.add("block " + (block != null ? "OK     " : "MISSING") + " " + id);
        }
        for (String id : MMCE_ITEMS) {
            String[] split = id.split(":", 2);
            Item item = GameRegistry.findItem(split[0], split[1]);
            if (item == null) missing++;
            lines.add("item  " + (item != null ? "OK     " : "MISSING") + " " + id);
        }
        missing += probeMachines(lines);
        lines.add("result " + (missing == 0 ? "PASS" : "FAIL (" + missing + " missing)"));

        for (String line : lines) {
            ModularMachinery.log.info("[DevProbe] {}", line);
        }
        File out = new File(Loader.instance().getConfigDir().getParentFile(), "mmce-dev-probe.txt");
        try (Writer w = new OutputStreamWriter(Files.newOutputStream(out.toPath()), StandardCharsets.UTF_8)) {
            for (String line : lines) {
                w.write(line);
                w.write(System.lineSeparator());
            }
        } catch (IOException e) {
            ModularMachinery.log.error("[DevProbe] Failed to write " + out, e);
        }
    }
}
