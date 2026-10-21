package hellfirepvp.modularmachinery.common.dev;

import github.kasuminova.mmce.compat.BlockPos;
import github.kasuminova.mmce.compat.ItemStacks;
import github.kasuminova.mmce.compat.RegistryNames;
import github.kasuminova.mmce.compat.Worlds;
import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.RecipeRegistry;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementItem;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.tiles.TileItemInputBus;
import hellfirepvp.modularmachinery.common.tiles.TileItemOutputBus;
import hellfirepvp.modularmachinery.common.tiles.base.TileEnergyHatch;
import hellfirepvp.modularmachinery.common.util.IOInventory;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 仅开发环境：DevSelfTest 的 alloy 阶段——默认机器“Alloy Smelter”（alloy_furnace）与熔炉配方适配器。
 * <p>
 * 用户报告：合金炉不放任何原料也会一直产出金锭。熔炉配方表里有输入物品为空的条目（物品为 null 或数量为 0）时，
 * 适配器跳过输入需求、只留下产出，生成一条“无输入”的配方。这里核对配方表、配方内容，并实际运行机器。
 */
final class DevAlloyTest {

    private static final String MACHINE = "alloy_furnace";

    private final DevSelfTest t;
    private BlockPos ctrl;
    private long      idleSince;
    private ItemStack injectedOutput;

    private DevAlloyTest(DevSelfTest t) {
        this.t = t;
    }

    static void build(DevSelfTest t) {
        new DevAlloyTest(t).steps();
    }

    private void info(String line) {
        CommandMMCETest.report(t.sender(), "INFO " + line);
    }

    private static String describe(ItemStack stack) {
        if (stack == null) {
            return "null";
        }
        Item item = stack.getItem();
        return (item == null ? "<null item>" : RegistryNames.getName(item)) + "@" + stack.getItemDamage() + " x" + stack.stackSize;
    }

    private List<TileEntity> parts(Class<?> type) {
        List<TileEntity> list = new ArrayList<>();
        DynamicMachine machine = DevSelfTest.machine(MACHINE);
        for (BlockPos offset : machine.getPattern().getPattern().keySet()) {
            TileEntity te = Worlds.getTileEntity(t.world(), ctrl.add(offset));
            if (type.isInstance(te)) {
                list.add(te);
            }
        }
        return list;
    }

    private void chargeEnergy() {
        for (TileEntity te : parts(TileEnergyHatch.class)) {
            TileEnergyHatch hatch = (TileEnergyHatch) te;
            hatch.setCurrentEnergy(hatch.getMaxEnergy());
        }
    }

    /** 输出总线中物品的总数；item 为 null 时统计所有物品。 */
    private int outputCount(Item item) {
        int n = 0;
        for (TileEntity te : parts(TileItemOutputBus.class)) {
            IOInventory inv = ((TileItemOutputBus) te).getInventory();
            for (int i = 0; i < inv.getSlots(); i++) {
                ItemStack s = inv.getStackInSlot(i);
                if (!ItemStacks.isEmpty(s) && (item == null || s.getItem() == item)) {
                    n += s.stackSize;
                }
            }
        }
        return n;
    }

    private int inputCount() {
        int n = 0;
        for (TileEntity te : parts(TileItemInputBus.class)) {
            IOInventory inv = ((TileItemInputBus) te).getInventory();
            for (int i = 0; i < inv.getSlots(); i++) {
                n += ItemStacks.getCount(inv.getStackInSlot(i));
            }
        }
        return n;
    }

    private void steps() {
        t.step("alloy: furnace smelting list", 0, () -> {
            Map<ItemStack, ItemStack> list = FurnaceRecipes.smelting().getSmeltingList();
            List<String> empty = new ArrayList<>();
            for (Map.Entry<ItemStack, ItemStack> e : list.entrySet()) {
                if (ItemStacks.isEmpty(e.getKey())) {
                    empty.add(describe(e.getKey()) + " -> " + describe(e.getValue()));
                }
            }
            info("furnace smelting entries=" + list.size() + ", with an empty input=" + empty.size() + (empty.isEmpty() ? "" : " " + empty));
        });
        // 复现用户环境：模组用没有物品形式的方块调用 GameRegistry.addSmelting(Block, ...) 时，FurnaceRecipes#func_151393_a
        // 以 Item.getItemFromBlock(block) = null 调用 func_151396_a，配方表中出现物品为 null 的输入。
        //（1.7.10 的原版方块都有物品形式，这里直接走同一个调用。）然后像服务器启动时一样重建适配器配方
        t.step("alloy: a mod registers smelting for a block without an item form (null item -> gold ingot)", 0, () -> {
            injectedOutput = new ItemStack(Items.gold_ingot);
            FurnaceRecipes.smelting().func_151396_a(null, injectedOutput, 0.1F);
            int empty = 0;
            for (ItemStack in : FurnaceRecipes.smelting().getSmeltingList().keySet()) {
                if (ItemStacks.isEmpty(in)) {
                    empty++;
                }
            }
            info("smelting entries with an empty input after registering: " + empty);
            t.expect("alloy: the smelting list now has an entry with an empty input", empty > 0);
            RecipeRegistry.reloadAdapters();
        });
        t.step("alloy: every item-producing alloy_furnace recipe consumes an item", 0, () -> {
            DynamicMachine machine = DevSelfTest.machine(MACHINE);
            int total = 0;
            List<String> free = new ArrayList<>();
            for (MachineRecipe recipe : RecipeRegistry.getRecipesFor(machine)) {
                total++;
                boolean itemIn = false;
                boolean itemOut = false;
                for (ComponentRequirement<?, ?> req : recipe.getCraftingRequirements()) {
                    if (req instanceof RequirementItem item) {
                        if (item.getActionType() == IOType.INPUT) {
                            itemIn = true;
                        } else {
                            itemOut = true;
                        }
                    }
                }
                if (itemOut && !itemIn) {
                    free.add(recipe.getRegistryName().toString());
                }
            }
            info("alloy_furnace recipes=" + total + ", producing items without any item input=" + free.size() + (free.isEmpty() ? "" : " " + free));
            t.expect("alloy: no alloy_furnace recipe produces items out of nothing", total > 0 && free.isEmpty());
        });
        t.step("alloy: build alloy_furnace, power it, leave the input bus empty", 40, () -> {
            World world = t.world();
            ChunkCoordinates spawn = world.getSpawnPoint();
            ctrl = DevSelfTest.groundAbove(world, spawn.posX - 60, spawn.posZ + 60);
            CommandMMCETest.buildAt(t.sender(), world, DevSelfTest.machine(MACHINE), ctrl);
            chargeEnergy();
            startIdle();
        });
        // 每 tick 补满能量（能量仓容量不够跑完一条配方），若有“无输入”的配方就会在这段时间内产出
        t.waitUntil("alloy: machine idles for 400 ticks with power and no input", 900, () -> {
            chargeEnergy();
            return outputCount(null) > 0 || idled(400);
        });
        t.step("alloy: nothing produced from an empty input bus", 0, () -> {
            CommandMMCETest.statusAt(t.sender(), t.world(), DevSelfTest.machine(MACHINE), ctrl);
            int out = outputCount(null);
            info("output bus items after idling=" + out);
            t.expect("alloy: output bus is still empty (no free output)", out == 0);
            IOInventory inv = ((TileItemInputBus) parts(TileItemInputBus.class).get(0)).getInventory();
            inv.setStackInSlot(0, new ItemStack(Blocks.iron_ore, 3));
        });
        t.waitUntil("alloy: 3 iron ore smelted into 3 iron ingots (furnace adapter, input and output x3)", 1200, () -> {
            chargeEnergy();
            return outputCount(Items.iron_ingot) >= 3;
        });
        t.step("alloy: totals", 0, () -> {
            CommandMMCETest.statusAt(t.sender(), t.world(), DevSelfTest.machine(MACHINE), ctrl);
            info("iron ingots=" + outputCount(Items.iron_ingot) + ", all outputs=" + outputCount(null) + ", input bus items left=" + inputCount());
            t.expect("alloy: the iron ore was consumed", inputCount() == 0);
            t.expect("alloy: only the 3 iron ingots were produced", outputCount(null) == 3);
            startIdle();
        });
        // 原料用完后继续供能：不应再有产出（“不消耗原料、持续产出”）
        t.waitUntil("alloy: powered for 400 more ticks after the ore ran out", 900, () -> {
            chargeEnergy();
            return outputCount(null) > 3 || idled(400);
        });
        t.step("alloy: still exactly 3 iron ingots; put 1 gold ore (recipe needs 3) and 4 dirt (no furnace recipe)", 0, () -> {
            info("outputs after the extra 400 ticks=" + outputCount(null));
            t.expect("alloy: no further output after the input ran out", outputCount(null) == 3);
            clearOutput();
            IOInventory inv = ((TileItemInputBus) parts(TileItemInputBus.class).get(0)).getInventory();
            inv.setStackInSlot(0, new ItemStack(Blocks.gold_ore, 1));
            inv.setStackInSlot(1, new ItemStack(Blocks.dirt, 4));
            startIdle();
        });
        t.waitUntil("alloy: powered for 400 ticks with too little gold ore", 900, () -> {
            chargeEnergy();
            return outputCount(null) > 0 || idled(400);
        });
        t.step("alloy: nothing produced from 1 gold ore + dirt; add 2 more gold ore", 0, () -> {
            CommandMMCETest.statusAt(t.sender(), t.world(), DevSelfTest.machine(MACHINE), ctrl);
            info("outputs=" + outputCount(null) + ", input items=" + inputCount());
            t.expect("alloy: 1 gold ore is not enough (input x3), nothing produced", outputCount(null) == 0 && inputCount() == 5);
            IOInventory inv = ((TileItemInputBus) parts(TileItemInputBus.class).get(0)).getInventory();
            inv.setStackInSlot(0, new ItemStack(Blocks.gold_ore, 3));
        });
        t.waitUntil("alloy: 3 gold ore smelted into 3 gold ingots", 1200, () -> {
            chargeEnergy();
            return outputCount(Items.gold_ingot) >= 3;
        });
        t.step("alloy: gold done", 0, this::startIdle);
        t.waitUntil("alloy: powered for 400 more ticks after the gold ore ran out", 900, () -> {
            chargeEnergy();
            return outputCount(null) > 3 || idled(400);
        });
        t.step("alloy: exactly 3 gold ingots, the gold ore consumed, the dirt left alone", 0, () -> {
            CommandMMCETest.statusAt(t.sender(), t.world(), DevSelfTest.machine(MACHINE), ctrl);
            info("gold ingots=" + outputCount(Items.gold_ingot) + ", all outputs=" + outputCount(null) + ", input items=" + inputCount());
            t.expect("alloy: exactly 3 gold ingots and nothing else", outputCount(Items.gold_ingot) == 3 && outputCount(null) == 3);
            t.expect("alloy: only the 4 dirt are left in the input bus", inputCount() == 4);
            FurnaceRecipes.smelting().getSmeltingList().entrySet().removeIf(e -> e.getValue() == injectedOutput);
            RecipeRegistry.reloadAdapters();
        });
    }

    private void startIdle() {
        idleSince = t.world().getTotalWorldTime();
    }

    /** 自 startIdle 起是否已过 ticks（waitUntil 每 6 tick 检查一次条件，不能按调用次数计时）。 */
    private boolean idled(int ticks) {
        return t.world().getTotalWorldTime() - idleSince >= ticks;
    }

    private void clearOutput() {
        for (TileEntity te : parts(TileItemOutputBus.class)) {
            IOInventory inv = ((TileItemOutputBus) te).getInventory();
            for (int i = 0; i < inv.getSlots(); i++) {
                inv.setStackInSlot(i, null);
            }
        }
    }
}
