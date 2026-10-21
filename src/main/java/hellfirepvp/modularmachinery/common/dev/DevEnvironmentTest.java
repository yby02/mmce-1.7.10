package hellfirepvp.modularmachinery.common.dev;

import com.warmthdawn.mod.gugu_utils.GuGuUtils;
import com.warmthdawn.mod.gugu_utils.modularmachenary.environment.TileEnvironmentHatch;
import com.warmthdawn.mod.gugu_utils.modularmachenary.environment.envtypes.EnvBiome;
import com.warmthdawn.mod.gugu_utils.modularmachenary.requirements.RequirementEnvironment;
import github.kasuminova.mmce.common.concurrent.RecipeCraftingContextPool;
import github.kasuminova.mmce.compat.BlockPos;
import github.kasuminova.mmce.compat.ItemStacks;
import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.RecipeRegistry;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.tiles.TileItemInputBus;
import hellfirepvp.modularmachinery.common.tiles.TileItemOutputBus;
import hellfirepvp.modularmachinery.common.tiles.TileMachineController;
import hellfirepvp.modularmachinery.common.util.IOInventory;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 仅开发环境：DevSelfTest 的 environment 阶段——gugu-utils 环境传感器（gugu-utils:environmenthatch）与需求 gugu-utils:environment。
 * <p>
 * 测试机 env_test（testdata）：控制器两侧为物品输入 / 输出总线，再外侧（x = ±2）各一个环境传感器，机器颜色 C83C3C。
 * 每一步只放一种原料，设置时间 / 天气 / 生物群系后核对是否产出，以及控制器显示的失败原因（错误信息键同原模组）。
 * 两个传感器相隔 4 格，各自所在的列可设为不同生物群系，用于“两个生物群系交界处才工作”的写法（两条生物群系需求各由一个传感器满足）。
 */
final class DevEnvironmentTest {

    private static final String MACHINE      = "env_test";
    private static final String NO_SENSOR    = "env_nosensor";
    private static final int    MACHINE_COLOR = 0xC83C3C;

    private final DevSelfTest t;
    private BlockPos ctrl;
    private BlockPos ctrlNoSensor;
    private long     since;

    private DevEnvironmentTest(DevSelfTest t) {
        this.t = t;
    }

    static void build(DevSelfTest t) {
        new DevEnvironmentTest(t).steps();
    }

    private void info(String line) {
        CommandMMCETest.report(t.sender(), "INFO " + line);
    }

    private World world() {
        return t.world();
    }

    private IOInventory input() {
        return ((TileItemInputBus) DevSelfTest.tileAt(world(), ctrl.add(-1, 0, 0))).getInventory();
    }

    private IOInventory output() {
        return ((TileItemOutputBus) DevSelfTest.tileAt(world(), ctrl.add(1, 0, 0))).getInventory();
    }

    private static int count(IOInventory inv, Item item, int meta) {
        int n = 0;
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack s = inv.getStackInSlot(i);
            if (!ItemStacks.isEmpty(s) && s.getItem() == item && (meta < 0 || s.getItemDamage() == meta)) {
                n += s.stackSize;
            }
        }
        return n;
    }

    private int out(Item item) {
        return count(output(), item, -1);
    }

    private static void clear(IOInventory inv) {
        for (int i = 0; i < inv.getSlots(); i++) {
            inv.setStackInSlot(i, null);
        }
    }

    private void feed(Block block) {
        feed(new ItemStack(block, 1));
    }

    private void feed(Item item) {
        feed(new ItemStack(item, 1));
    }

    private void feed(ItemStack stack) {
        clear(input());
        clear(output());
        input().setStackInSlot(0, stack);
        since = world().getTotalWorldTime();
    }

    private String status(BlockPos pos) {
        TileEntity te = DevSelfTest.tileAt(world(), pos);
        return te instanceof TileMachineController c ? c.getControllerStatus().getUnlocMessage() : "?";
    }

    private boolean waited(int ticks) {
        return world().getTotalWorldTime() - since >= ticks;
    }

    private void setTime(long time) {
        world().setWorldTime(time);
        since = world().getTotalWorldTime();
    }

    /**
     * 设置天气并保持（剩余时间设得很长）。1.7.10 的降雨 / 雷暴强度每 tick 只变化 0.01，这里直接把强度设到目标值，
     * 使 isRaining / isThundering 立即反映新天气（setRainStrength 是客户端方法，专用服务器上没有，故直接写字段）。
     */
    private void setWeather(boolean rain, boolean thunder) {
        World w = world();
        w.getWorldInfo().setRaining(rain);
        w.getWorldInfo().setRainTime(1_000_000);
        w.getWorldInfo().setThundering(thunder);
        w.getWorldInfo().setThunderTime(1_000_000);
        w.prevRainingStrength = w.rainingStrength = rain ? 1.0F : 0.0F;
        w.prevThunderingStrength = w.thunderingStrength = thunder ? 1.0F : 0.0F;
        since = w.getTotalWorldTime();
    }

    /** 把 (x, z) 这一列的生物群系改为 biome（直接改区块的生物群系数组）。 */
    private void setBiome(BlockPos pos, BiomeGenBase biome) {
        Chunk chunk = world().getChunkFromBlockCoords(pos.getX(), pos.getZ());
        byte[] biomes = chunk.getBiomeArray();
        biomes[(pos.getZ() & 15) << 4 | (pos.getX() & 15)] = (byte) biome.biomeID;
        chunk.setBiomeArray(biomes);
        since = world().getTotalWorldTime();
    }

    /**
     * 该机器上名为 id 的配方（JSON 配方的注册名带 _sub_N 后缀）此刻能否开始，返回其错误信息键。
     * 与控制器搜索配方时相同（createContext + onCheck）。不看控制器显示的状态：所有配方都不能运行时，控制器显示的是
     * “满足条件比例最高”的那条配方的第一个错误，比例相同时取先检查到的一条，不一定是这里关心的配方。
     */
    private List<String> recipeErrors(String id) {
        return recipeErrors(ctrl, MACHINE, id);
    }

    private List<String> recipeErrors(BlockPos at, String machine, String id) {
        TileMachineController c = (TileMachineController) DevSelfTest.tileAt(world(), at);
        for (MachineRecipe recipe : RecipeRegistry.getRecipesFor(DevSelfTest.machine(machine))) {
            String path = recipe.getRegistryName().getPath();
            if (path.equals(id) || path.startsWith(id + "_sub_")) {
                RecipeCraftingContext context = c.createContext(new ActiveMachineRecipe(recipe, 1));
                RecipeCraftingContext.CraftingCheckResult result = c.onCheck(context);
                List<String> errors = result.isSuccess() ? new ArrayList<>() : new ArrayList<>(result.getUnlocalizedErrorMessages());
                RecipeCraftingContextPool.returnCtx(context);
                return errors;
            }
        }
        throw new IllegalStateException("recipe " + id + " not registered for " + machine);
    }

    /** 放入原料后等 80 tick 不产出，且该配方的错误正是 failure（环境条件不满足）。 */
    private void expectNoOutput(String recipe, String name, Item product, String failure) {
        t.waitUntil("environment: " + name + " (no output for 80 ticks)", 200, () -> out(product) > 0 || waited(80));
        t.step("environment: " + name + " -> nothing produced, " + recipe + " fails with " + failure, 0, () -> {
            List<String> errors = recipeErrors(recipe);
            info(name + ": output=" + out(product) + " " + recipe + " errors=" + errors + " (controller status " + status(ctrl) + ")");
            t.expect("environment: " + name + ": no output", out(product) == 0);
            t.expect("environment: " + name + ": " + recipe + " fails only because of " + failure, errors.equals(Collections.singletonList(failure)));
        });
    }

    private void expectOutput(String name, Item product) {
        t.waitUntil("environment: " + name, 400, () -> out(product) >= 1);
    }

    private void steps() {
        t.step("environment: build env_test and env_nosensor, freeze time, clear weather", 40, () -> {
            DynamicMachine machine = DevSelfTest.machine(MACHINE);
            if (machine == null) {
                throw new IllegalStateException("testdata machine env_test not loaded");
            }
            world().getGameRules().setOrCreateGameRule("doDaylightCycle", "false");
            setTime(6000);
            setWeather(false, false);
            ChunkCoordinates spawn = world().getSpawnPoint();
            ctrl = DevSelfTest.groundAbove(world(), spawn.posX + 40, spawn.posZ - 30);
            CommandMMCETest.buildAt(t.sender(), world(), machine, ctrl);
            ctrlNoSensor = DevSelfTest.groundAbove(world(), spawn.posX + 40, spawn.posZ - 40);
            CommandMMCETest.buildAt(t.sender(), world(), DevSelfTest.machine(NO_SENSOR), ctrlNoSensor);
            info("block " + Block.blockRegistry.getNameForObject(world().getBlock(ctrl.getX() - 2, ctrl.getY(), ctrl.getZ()))
                + " tile " + DevSelfTest.tileAt(world(), ctrl.add(-2, 0, 0)));
        });
        t.waitUntil("environment: env_test formed", 200, () -> DevSelfTest.tileAt(world(), ctrl) instanceof TileMachineController c && c.isStructureFormed());
        t.step("environment: registry names and machine colour on both sensors", 0, () -> {
            t.expect("environment: block registered as gugu-utils:environmenthatch",
                "gugu-utils:environmenthatch".equals(Block.blockRegistry.getNameForObject(GuGuUtils.blockEnvironmentHatch)));
            t.expect("environment: item registered as gugu-utils:constructionranged",
                "gugu-utils:constructionranged".equals(Item.itemRegistry.getNameForObject(GuGuUtils.constructionTool)));
            for (int dx : new int[]{-2, 2}) {
                TileEntity te = DevSelfTest.tileAt(world(), ctrl.add(dx, 0, 0));
                int color = te instanceof TileEnvironmentHatch hatch ? hatch.getMachineColor() : -1;
                info("sensor at dx=" + dx + ": " + (te == null ? "none" : te.getClass().getSimpleName()) + " colour=" + Integer.toHexString(color));
                t.expect("environment: sensor at dx=" + dx + " took the machine colour C83C3C when the machine formed", color == MACHINE_COLOR);
            }
            List<String> envRecipes = new ArrayList<>();
            for (MachineRecipe recipe : RecipeRegistry.getRecipesFor(DevSelfTest.machine(MACHINE))) {
                int env = 0;
                for (ComponentRequirement<?, ?> req : recipe.getCraftingRequirements()) {
                    if (req instanceof RequirementEnvironment) {
                        env++;
                    }
                }
                if (env > 0) {
                    envRecipes.add(recipe.getRegistryName().getPath() + " x" + env);
                }
            }
            info("env_test recipes with environment requirements: " + envRecipes);
            t.expect("environment: 11 JSON + 4 ZenScript recipes carry environment requirements (the border recipe has 2)",
                envRecipes.size() == 15 && envRecipes.stream().anyMatch(s -> s.contains("env_biome_border") && s.endsWith(" x2")));
        });

        // 维度
        t.step("environment: dimension [0] (cobblestone)", 0, () -> feed(Blocks.cobblestone));
        expectOutput("dimension [0] matches the overworld -> stone", Item.getItemFromBlock(Blocks.stone));
        t.step("environment: dimension [-1] (dirt)", 0, () -> feed(Blocks.dirt));
        expectNoOutput("env_dim_nether", "dimension [-1] in the overworld", Item.getItemFromBlock(Blocks.grass), "craftcheck.failure.gugu-utils:environment.dimensions");

        // 天气
        t.step("environment: weather raining (sand) while sunny", 0, () -> feed(Blocks.sand));
        expectNoOutput("env_rain", "weather raining while sunny", Item.getItemFromBlock(Blocks.glass), "craftcheck.failure.gugu-utils:environment.weather");
        t.step("environment: start rain", 0, () -> setWeather(true, false));
        expectOutput("weather raining while raining -> glass", Item.getItemFromBlock(Blocks.glass));
        t.step("environment: clear weather", 0, () -> setWeather(false, false));

        // 时间（1000–2000，以及跨午夜的 23000–1000）
        t.step("environment: time 1000-2000 (gravel) at 500", 0, () -> {
            setTime(500);
            feed(Blocks.gravel);
        });
        expectNoOutput("env_time", "time 1000-2000 at 500", Items.flint, "craftcheck.failure.gugu-utils:environment.time");
        t.step("environment: time 1500", 0, () -> setTime(1500));
        expectOutput("time 1000-2000 at 1500 -> flint", Items.flint);
        t.step("environment: time 23000-1000 (seeds) at 12000", 0, () -> {
            setTime(12000);
            feed(Items.wheat_seeds);
        });
        expectNoOutput("env_time_wrap", "time 23000-1000 at 12000", Items.wheat, "craftcheck.failure.gugu-utils:environment.time");
        t.step("environment: time 23500", 0, () -> setTime(23500));
        expectOutput("time 23000-1000 at 23500 (wraps past midnight) -> wheat", Items.wheat);

        // 月相（只在夜间）：第 0 天夜里是满月（0），第 4 天夜里是新月（4）
        t.step("environment: moonphase [4] (clay) on a full-moon night", 0, () -> {
            setTime(18000);
            feed(Items.clay_ball);
        });
        expectNoOutput("env_moon", "moonphase [4] on a full-moon night", Items.brick, "craftcheck.failure.gugu-utils:environment.moonphase");
        t.step("environment: new-moon day", 0, () -> setTime(4 * 24000L + 6000));
        expectNoOutput("env_moon", "moonphase [4] in daytime", Items.brick, "craftcheck.failure.gugu-utils:environment.moonphase");
        t.step("environment: new-moon night", 0, () -> setTime(4 * 24000L + 18000));
        expectOutput("moonphase [4] on a new-moon night -> brick", Items.brick);
        t.step("environment: back to noon", 0, () -> setTime(6000));

        // 海拔
        t.step("environment: altitude 0-255 (bone)", 0, () -> feed(Items.bone));
        expectOutput("altitude 0-255 -> bone meal", Items.dye);
        t.step("environment: altitude 250-255 (feather)", 0, () -> feed(Items.feather));
        expectNoOutput("env_alt_high", "altitude 250-255 at ground level", Items.arrow, "craftcheck.failure.gugu-utils:environment.altitude");

        // 生物群系：两条需求（沙漠、冰原）分别由两个传感器满足；两个传感器都在沙漠时冰原需求不满足
        t.step("environment: sensor columns desert (dx=-2) / ice plains (dx=+2); apple", 0, () -> {
            setBiome(ctrl.add(-2, 0, 0), BiomeGenBase.desert);
            setBiome(ctrl.add(2, 0, 0), BiomeGenBase.icePlains);
            info("biomes at the sensors: " + world().getBiomeGenForCoords(ctrl.getX() - 2, ctrl.getZ()).biomeName + " / "
                + world().getBiomeGenForCoords(ctrl.getX() + 2, ctrl.getZ()).biomeName);
            // 生物群系的写法：1.12.2 原版注册名、1.7.10 名称（忽略大小写 / 空格 / 下划线）、数字 ID
            Object[][] lookups = {
                {"minecraft:ice_flats", BiomeGenBase.icePlains}, {"minecraft:beaches", BiomeGenBase.beach},
                {"minecraft:mutated_plains", BiomeGenBase.getBiome(129)}, {"Ice Plains", BiomeGenBase.icePlains},
                {"minecraft:ice_plains", BiomeGenBase.icePlains}, {"extreme_hills", BiomeGenBase.extremeHills}, {"12", BiomeGenBase.icePlains},
                {"no_such_biome", null}
            };
            for (Object[] l : lookups) {
                BiomeGenBase found = EnvBiome.findBiome((String) l[0]);
                info("findBiome(" + l[0] + ") = " + (found == null ? "null" : found.biomeName));
                t.expect("environment: biome name " + l[0] + " -> " + (l[1] == null ? "none" : ((BiomeGenBase) l[1]).biomeName), found == l[1]);
            }
            feed(Items.apple);
        });
        expectOutput("biome border: one sensor in Desert, the other in Ice Plains -> golden apple", Items.golden_apple);
        t.step("environment: both sensor columns desert; apple", 0, () -> {
            setBiome(ctrl.add(2, 0, 0), BiomeGenBase.desert);
            feed(Items.apple);
        });
        expectNoOutput("env_biome_border", "biome border with both sensors in Desert", Items.golden_apple, "craftcheck.failure.gugu-utils:environment.biomes");
        t.step("environment: biomeType desert (pumpkin)", 0, () -> feed(Blocks.pumpkin));
        expectOutput("biomeType desert with a sensor in Desert -> pumpkin pie", Items.pumpkin_pie);

        // 每 tick 检查：200 tick 的晴天配方，运行中下雨则停住，放晴后完成
        t.step("environment: weather sunny, 200-tick recipe (iron ore)", 0, () -> feed(Blocks.iron_ore));
        t.waitUntil("environment: per-tick: recipe running", 200, () -> DevSelfTest.tileAt(world(), ctrl) instanceof TileMachineController c
            && c.getActiveRecipe() != null && c.getActiveRecipe().getTick() > 20);
        t.step("environment: per-tick: rain starts mid-recipe", 0, () -> {
            TileMachineController c = (TileMachineController) DevSelfTest.tileAt(world(), ctrl);
            info("per-tick: recipe tick before rain " + c.getActiveRecipe().getTick() + "/" + c.getActiveRecipe().getTotalTick());
            setWeather(true, false);
        });
        t.waitUntil("environment: per-tick: wait 300 ticks in the rain", 400, () -> out(Items.iron_ingot) > 0 || waited(300));
        t.step("environment: per-tick: no ingot while raining (the recipe would have finished by now)", 0, () -> {
            String msg = status(ctrl);
            TileMachineController c = (TileMachineController) DevSelfTest.tileAt(world(), ctrl);
            info("per-tick: after 300 rainy ticks output=" + out(Items.iron_ingot) + " status=" + msg
                + " tick=" + (c.getActiveRecipe() == null ? "-" : c.getActiveRecipe().getTick()));
            t.expect("environment: per-tick: no iron ingot while raining", out(Items.iron_ingot) == 0);
            t.expect("environment: per-tick: controller says the weather is wrong", "craftcheck.failure.gugu-utils:environment.weather".equals(msg));
            setWeather(false, false);
        });
        expectOutput("per-tick: sunny again -> the recipe finishes (iron ingot)", Items.iron_ingot);

        // ZenScript 方法生成的配方
        t.step("environment: ZenScript setDimension([0]) (redstone)", 0, () -> feed(Items.redstone));
        expectOutput("ZenScript setDimension([0]) -> glowstone dust", Items.glowstone_dust);
        t.step("environment: ZenScript setWeather(\"thundering\") (coal) while sunny", 0, () -> feed(Items.coal));
        expectNoOutput("zs_env_weather", "ZenScript setWeather(thundering) while sunny", Items.diamond, "craftcheck.failure.gugu-utils:environment.weather");
        t.step("environment: thunderstorm", 0, () -> setWeather(true, true));
        expectOutput("ZenScript setWeather(thundering) in a thunderstorm -> diamond", Items.diamond);
        t.step("environment: clear weather; ZenScript setBiome([\"Desert\", \"12\"]) (sugar)", 0, () -> {
            setWeather(false, false);
            feed(Items.sugar);
        });
        expectOutput("ZenScript setBiome with a sensor in Desert -> cake", Items.cake);

        // 没有环境传感器的机器
        t.step("environment: env_nosensor (cobblestone)", 0, () -> {
            IOInventory in = ((TileItemInputBus) DevSelfTest.tileAt(world(), ctrlNoSensor.add(-1, 0, 0))).getInventory();
            in.setStackInSlot(0, new ItemStack(Blocks.cobblestone));
            since = world().getTotalWorldTime();
        });
        t.waitUntil("environment: env_nosensor waits 80 ticks", 200, () -> waited(80));
        t.step("environment: env_nosensor: missing sensor message", 0, () -> {
            IOInventory in = ((TileItemInputBus) DevSelfTest.tileAt(world(), ctrlNoSensor.add(-1, 0, 0))).getInventory();
            List<String> errors = recipeErrors(ctrlNoSensor, NO_SENSOR, "env_nosensor_dim");
            info("env_nosensor input=" + count(in, Item.getItemFromBlock(Blocks.cobblestone), -1) + " cobblestone, env_nosensor_dim errors="
                + errors + " (controller status " + status(ctrlNoSensor) + ")");
            t.expect("environment: a machine without a sensor: the recipe reports component.missing.gugu-utils:environment.input",
                errors.equals(Collections.singletonList("component.missing.gugu-utils:environment.input")));
            world().getGameRules().setOrCreateGameRule("doDaylightCycle", "true");
            CommandMMCETest.statusAt(t.sender(), world(), DevSelfTest.machine(MACHINE), ctrl);
        });
    }
}
