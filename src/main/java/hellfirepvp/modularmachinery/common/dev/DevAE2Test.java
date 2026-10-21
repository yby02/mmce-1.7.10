package hellfirepvp.modularmachinery.common.dev;

import github.kasuminova.mmce.common.tile.MEFluidInputBus;
import github.kasuminova.mmce.common.tile.MEFluidOutputBus;
import github.kasuminova.mmce.common.tile.MEItemInputBus;
import github.kasuminova.mmce.common.tile.MEItemOutputBus;
import github.kasuminova.mmce.common.tile.MEPatternMirrorImage;
import github.kasuminova.mmce.common.tile.MEPatternProvider;
import github.kasuminova.mmce.common.util.InfItemFluidHandler;
import github.kasuminova.mmce.compat.BlockPos;
import github.kasuminova.mmce.compat.ItemStacks;
import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.lib.BlocksMM;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;
import java.util.concurrent.Future;

/**
 * 仅开发环境：DevSelfTest 的 ae2 阶段（只在装有 AE2 时调用；AE2 的类都经 {@link DevAE2Support} 访问）。
 * <p>
 * 机器 ae2_test（testdata）：控制器后一排依次为 ME 物品输入总线、ME 流体输入总线、ME 样板供应器、ME 流体输出总线、ME 物品输出总线；
 * 上方放创造能源元件、ME 驱动器、1k 合成存储器，组成无控制器的 AE2 网络。配方：4 圆石 → 4 石头；1000 水 → 1000 熔岩；
 * 2 金锭 + 1000 熔岩 → 1 钻石（样板供应器）。
 */
final class DevAE2Test {

    private static final ItemStack COBBLE  = new ItemStack(Blocks.cobblestone);
    private static final ItemStack STONE   = new ItemStack(Blocks.stone);
    private static final ItemStack GOLD    = new ItemStack(Items.gold_ingot);
    private static final ItemStack DIAMOND = new ItemStack(Items.diamond);
    private static final ItemStack DIRT    = new ItemStack(Blocks.dirt);

    private final DevSelfTest t;

    private BlockPos          ctrl;
    private MEItemInputBus    itemIn;
    private MEItemOutputBus   itemOut;
    private MEFluidInputBus   fluidIn;
    private MEFluidOutputBus  fluidOut;
    private MEPatternProvider provider;
    private long              lavaBeforeCraft;
    @SuppressWarnings("rawtypes")
    private Future            job;

    private DevAE2Test(DevSelfTest t) {
        this.t = t;
    }

    static void build(DevSelfTest t) {
        DevAE2Test test = new DevAE2Test(t);
        test.setupSteps();
        test.steps();
    }

    /**
     * ae2-gui：搭好网络后留下一组便于截图的状态（客户端场景 ae2 用来打开各个 ME 界面）——物品输入总线标记 4 圆石、
     * 流体输入总线标记水并装 1 张容量卡、样板供应器装入样板，缓存中放 5 泥土与 500 mB 熔岩，独立存储放 3 铁锭与 2000 mB 熔岩。
     */
    static void buildForGui(DevSelfTest t) {
        DevAE2Test test = new DevAE2Test(t);
        test.setupSteps();
        test.guiSteps();
    }

    private static boolean isTestBlock(net.minecraft.block.Block block) {
        cpw.mods.fml.common.registry.GameRegistry.UniqueIdentifier id = cpw.mods.fml.common.registry.GameRegistry.findUniqueIdentifierFor(block);
        return id != null && ("modularmachinery".equals(id.modId) || "appliedenergistics2".equals(id.modId) || "ae2fc".equals(id.modId));
    }

    /**
     * 服务端自测每次复用同一个存档。groundAbove 取该列最高的方块，上次留下的机器与 AE2 网络还在原处时，新的会叠在上面；
     * 新旧两套网络相邻就连成一个，无控制器网络超过 8 个频道后全部离线（全量回归中 ae2 因此失败）。
     * 这里清掉本测试范围内（x ± 4、z −5 … +3）地面以上的 MMCE / AE2 / AE2FC 方块：先移除方块实体，避免掉落物品；
     * 地面按控制器所在列往下越过空气与这几类方块后的第一个方块计。返回清理后的控制器位置。
     */
    private BlockPos clearPreviousRun(World world, int x, int z) {
        int ground = world.getTopSolidOrLiquidBlock(x, z) - 1;
        while (ground > 1 && (world.isAirBlock(x, ground, z) || isTestBlock(world.getBlock(x, ground, z)))) {
            ground--;
        }
        int removed = 0;
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -5; dz <= 3; dz++) {
                int top = world.getTopSolidOrLiquidBlock(x + dx, z + dz);
                for (int y = ground + 1; y <= top; y++) {
                    if (isTestBlock(world.getBlock(x + dx, y, z + dz))) {
                        world.removeTileEntity(x + dx, y, z + dz);
                        world.setBlock(x + dx, y, z + dz, Blocks.air, 0, 3);
                        removed++;
                    }
                }
            }
        }
        if (removed > 0) {
            info("ae2: removed " + removed + " blocks left by a previous run");
        }
        return DevSelfTest.groundAbove(world, x, z);
    }

    private static FluidStack water(int amount) {
        return new FluidStack(FluidRegistry.WATER, amount);
    }

    private static FluidStack lava(int amount) {
        return new FluidStack(FluidRegistry.LAVA, amount);
    }

    private void info(String line) {
        CommandMMCETest.report(t.sender(), "INFO " + line);
    }

    private TileEntity at(int dx, int dy, int dz) {
        return DevSelfTest.tileAt(t.world(), ctrl.add(dx, dy, dz));
    }

    private long items(ItemStack stack) {
        try {
            return DevAE2Support.itemAmount(itemIn, stack);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private long fluids(FluidStack stack) {
        try {
            return DevAE2Support.fluidAmount(itemIn, stack);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String status() {
        return CommandMMCETest.statusAt(t.sender(), t.world(), DevSelfTest.machine("ae2_test"), ctrl);
    }

    private boolean push(Object... stacks) {
        return DevAE2Support.push(provider, 0, stacks);
    }

    private void setupSteps() {
        t.step("ae2: build machine and network", 20, () -> {
            DynamicMachine machine = DevSelfTest.machine("ae2_test");
            if (!Mods.AE2.isPresent() || machine == null) {
                throw new IllegalStateException("AE2 missing or testdata machine ae2_test not loaded");
            }
            World world = t.world();
            ChunkCoordinates spawn = world.getSpawnPoint();
            ctrl = clearPreviousRun(world, spawn.posX - 90, spawn.posZ + 3);
            CommandMMCETest.buildAt(t.sender(), world, machine, ctrl);
            itemIn = (MEItemInputBus) at(-2, 0, 1);
            fluidIn = (MEFluidInputBus) at(-1, 0, 1);
            provider = (MEPatternProvider) at(0, 0, 1);
            fluidOut = (MEFluidOutputBus) at(1, 0, 1);
            itemOut = (MEItemOutputBus) at(2, 0, 1);
            DevAE2Support.placeInfrastructure(world, ctrl.add(-2, 1, 1), ctrl.add(-1, 1, 1), ctrl.add(0, 1, 1));
        });
        t.waitUntil("ae2: all 5 ME parts online (channel + power)", 400, () ->
            DevAE2Support.isActive(itemIn) && DevAE2Support.isActive(itemOut) && DevAE2Support.isActive(fluidIn)
                && DevAE2Support.isActive(fluidOut) && DevAE2Support.isActive(provider));
        t.waitUntil("ae2: crafting CPU formed", 200, () -> {
            try {
                return DevAE2Support.cpuCount(itemIn) >= 1;
            } catch (Exception e) {
                return false;
            }
        });
        t.step("ae2: stock the network", 5, () -> {
            try {
                t.expect("ae2: 64 cobblestone stored in the ME item cell", DevAE2Support.injectItem(itemIn, COBBLE, 64) == 0);
                t.expect("ae2: 16 gold ingots stored", DevAE2Support.injectItem(itemIn, GOLD, 16) == 0);
                t.expect("ae2: 16000 mB water stored in the AE2FC fluid cell", DevAE2Support.injectFluid(itemIn, water(16000)) == 0);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            status();
        });
    }

    private void guiSteps() {
        t.step("ae2-gui: leave the parts in a state worth looking at", 0, () -> {
            itemIn.setConfig(0, new ItemStack(Blocks.cobblestone, 4));
            fluidIn.setConfig(0, water(1));
            fluidIn.getUpgrades().setStackInSlot(0, DevAE2Support.capacityCard());
            provider.getPatterns().setStackInSlot(0, DevAE2Support.diamondPattern(new ItemStack(Items.gold_ingot, 2), DIAMOND, lava(1000)));
            provider.getSubItemHandler().setStackInSlot(0, new ItemStack(Items.iron_ingot, 3));
            provider.getSubFluidHandler().fill(lava(2000), true);
        });
        t.step("ae2-gui: provider cache", 20, () -> {
            boolean pushed = push(DevAE2Support.aeItem(new ItemStack(Blocks.dirt, 5)), DevAE2Support.aeFluid(lava(500)));
            t.expect("ae2-gui: dirt and lava pushed into the provider cache", pushed && !provider.getInfHandler().isEmpty());
            status();
        });
    }

    private void steps() {

        // ---- ME 物品输入 / 输出总线 ----
        t.step("ae2: mark 4 cobblestone in the ME item input bus", 0, () -> itemIn.setConfig(0, new ItemStack(Blocks.cobblestone, 4)));
        t.waitUntil("ae2: stone reaches the ME network (item input bus -> recipe -> item output bus)", 800, () -> items(STONE) >= 4);
        t.step("ae2: unmark the item input bus", 0, () -> {
            info("after item recipe: ME cobblestone=" + items(COBBLE) + " stone=" + items(STONE));
            status();
            itemIn.setConfig(0, null);
        });
        t.waitUntil("ae2: unmarked item input bus returns its cobblestone to ME, machine idle", 400,
            () -> !itemIn.hasItem() && !itemOut.hasItem() && !status().contains("state=CRAFTING"));
        t.step("ae2: item bus totals", 0, () -> {
            long cobble = items(COBBLE);
            long stone = items(STONE);
            info("ME cobblestone=" + cobble + " stone=" + stone);
            t.expect("ae2: cobblestone + stone in ME == 64 (nothing lost or duplicated)", cobble + stone == 64);
            t.expect("ae2: stone produced in multiples of 4", stone > 0 && stone % 4 == 0);
        });

        // ---- ME 流体输入 / 输出总线 ----
        t.step("ae2: mark water in the ME fluid input bus", 0, () -> fluidIn.setConfig(0, water(1)));
        t.waitUntil("ae2: lava reaches the ME network (fluid input bus -> recipe -> fluid output bus)", 800, () -> fluids(lava(1)) >= 1000);
        t.step("ae2: unmark the fluid input bus", 0, () -> {
            info("after fluid recipe: ME water=" + fluids(water(1)) + " lava=" + fluids(lava(1)));
            status();
            fluidIn.setConfig(0, null);
        });
        t.waitUntil("ae2: unmarked fluid input bus returns its water to ME, machine idle", 400,
            () -> fluidIn.getTanks().isEmpty() && fluidOut.getTanks().isEmpty() && !status().contains("state=CRAFTING"));
        t.step("ae2: fluid bus totals", 0, () -> {
            long w = fluids(water(1));
            long l = fluids(lava(1));
            info("ME water=" + w + " lava=" + l);
            t.expect("ae2: water + lava in ME == 16000 (1:1 recipe, nothing lost)", w + l == 16000);
            t.expect("ae2: lava produced in multiples of 1000", l > 0 && l % 1000 == 0);
        });
        t.step("ae2: capacity cards", 0, () -> {
            int before = fluidIn.getTanks().getCapacity();
            fluidIn.getUpgrades().setStackInSlot(0, DevAE2Support.capacityCard());
            fluidIn.getUpgrades().setStackInSlot(1, DevAE2Support.capacityCard());
            int with2 = fluidIn.getTanks().getCapacity();
            fluidIn.getUpgrades().setStackInSlot(0, ItemStacks.EMPTY);
            fluidIn.getUpgrades().setStackInSlot(1, ItemStacks.EMPTY);
            int after = fluidIn.getTanks().getCapacity();
            info("fluid bus capacity: " + before + " -> 2 cards " + with2 + " -> removed " + after);
            t.expect("ae2: tank capacity 8000 -> 128000 with 2 capacity cards (2000 x 4^(n+1)) -> 8000", before == 8000 && with2 == 128000 && after == 8000);
            t.expect("ae2: a non-card item is rejected by the upgrade slot filter", !github.kasuminova.mmce.common.tile.base.MEFluidBus.isCapacityCard(DIRT));
        });

        // ---- 样板供应器：终极样板（带流体原料）经合成 CPU 下单 ----
        t.step("ae2: insert pattern 2 gold + 1000 lava -> 1 diamond", 0, () ->
            provider.getPatterns().setStackInSlot(0, DevAE2Support.diamondPattern(new ItemStack(Items.gold_ingot, 2), DIAMOND, lava(1000))));
        t.waitUntil("ae2: diamond becomes craftable in the ME network", 100, () -> {
            try {
                return DevAE2Support.craftable(itemIn, DIAMOND);
            } catch (Exception e) {
                return false;
            }
        });
        t.step("ae2: request 1 diamond", 0, () -> {
            info("pattern inputs: " + DevAE2Support.describeInputs(provider.getPatternDetails(0)));
            t.expect("ae2: pattern carries the fluid ingredient (1000 mB lava)", DevAE2Support.hasFluidInput(provider.getPatternDetails(0)));
            lavaBeforeCraft = fluids(lava(1));
            try {
                job = DevAE2Support.beginJob(itemIn, DIAMOND);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
        t.waitUntil("ae2: crafting job calculated", 200, () -> job != null && job.isDone());
        t.step("ae2: submit crafting job", 0, () -> {
            String result;
            try {
                result = DevAE2Support.submitJob(itemIn, job);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            info("crafting job: " + result);
            t.expect("ae2: crafting job submitted to the CPU", !result.startsWith("FAIL"));
        });
        t.waitUntil("ae2: diamond crafted (CPU -> pattern provider -> machine -> item output bus -> CPU)", 800, () -> items(DIAMOND) >= 1);
        t.waitUntil("ae2: crafting CPU idle again", 200, () -> {
            try {
                return !DevAE2Support.anyCpuBusy(itemIn);
            } catch (Exception e) {
                return false;
            }
        });
        t.step("ae2: crafting totals", 0, () -> {
            long gold = items(GOLD);
            long lava = fluids(lava(1));
            info("after crafting: ME gold=" + gold + " lava=" + lava + " (before " + lavaBeforeCraft + ") diamond=" + items(DIAMOND)
                + " provider cache empty=" + provider.getInfHandler().isEmpty());
            t.expect("ae2: crafting took 2 gold and 1000 mB lava from ME", gold == 14 && lava == lavaBeforeCraft - 1000);
            t.expect("ae2: pattern provider cache empty after the recipe", provider.getInfHandler().isEmpty());
        });

        // ---- 工作模式 ----
        t.step("ae2: blocking mode", 0, () -> {
            provider.setWorkMode(MEPatternProvider.WorkModeSetting.BLOCKING_MODE);
            // AE2 的合成 CPU 在推送前检查 isBusy；直接调用 pushPattern 不经过这一步，所以这里只检查 isBusy
            boolean pushed = push(DevAE2Support.aeItem(DIRT));
            t.expect("ae2: blocking mode: provider busy while its cache holds items", pushed && provider.isBusy());
            provider.returnItemsScheduled();
        });
        t.waitUntil("ae2: return items sends the dirt back to ME", 100, () -> provider.getInfHandler().isEmpty() && items(DIRT) == 1);
        t.step("ae2: crafting lock mode", 0, () -> {
            t.expect("ae2: not busy after returning items", !provider.isBusy());
            provider.setWorkMode(MEPatternProvider.WorkModeSetting.CRAFTING_LOCK_MODE);
            boolean pushed = push(DevAE2Support.aeItem(new ItemStack(Items.gold_ingot, 2)), DevAE2Support.aeFluid(lava(1000)));
            t.expect("ae2: crafting lock mode: push accepted and provider busy until the machine finishes a recipe", pushed && provider.isBusy());
        });
        t.waitUntil("ae2: crafting lock released by the machine's RecipeFinishEvent", 600, () -> !provider.isBusy());
        t.waitUntil("ae2: second diamond reaches ME", 200, () -> items(DIAMOND) >= 2);

        // 切换到 / 离开输入隔离模式时供应器会安排一次“清空缓存”（下一个同步任务），推送放在下一步
        t.step("ae2: switch to isolation input mode", 10, () -> provider.setWorkMode(MEPatternProvider.WorkModeSetting.ISOLATION_INPUT));
        t.step("ae2: isolation input mode", 0, () -> {
            boolean pushed = push(DevAE2Support.aeItem(new ItemStack(Items.gold_ingot, 2)), DevAE2Support.aeFluid(lava(1000)));
            InfItemFluidHandler isolated = (InfItemFluidHandler) provider.getCombinationComponents().get(0).getContainerProvider();
            t.expect("ae2: isolation mode: ingredients go to pattern slot 0's own cache, the shared cache stays empty",
                pushed && !isolated.isEmpty() && provider.getInfHandler().isEmpty());
            t.expect("ae2: isolation mode: provider offers 36 separate components and no shared one",
                provider.provideComponent() == null && provider.provideComponents().size() == MEPatternProvider.PATTERNS);
        });
        t.waitUntil("ae2: machine picks up the isolated cache (third diamond)", 800, () -> items(DIAMOND) >= 3);

        t.step("ae2: back to default mode", 10, () -> provider.setWorkMode(MEPatternProvider.WorkModeSetting.DEFAULT));
        t.step("ae2: AE2FC fluid drop item becomes fluid", 0, () -> {
            boolean pushed = push(DevAE2Support.aeItem(new ItemStack(Items.gold_ingot, 2)), DevAE2Support.aeItem(DevAE2Support.fluidDrop(lava(1000))));
            InfItemFluidHandler h = provider.getInfHandler();
            boolean lavaIn = false;
            for (FluidStack f : h.getFluidStackList()) {
                lavaIn |= f != null && f.getFluid() == FluidRegistry.LAVA && f.amount == 1000;
            }
            boolean dropAsItem = false;
            for (ItemStack s : h.getItemStackList()) {
                dropAsItem |= s != null && s.getItem() != Items.gold_ingot;
            }
            t.expect("ae2: fluid drop pushed as 1000 mB lava, not as an item", pushed && lavaIn && !dropAsItem);
        });
        t.waitUntil("ae2: fourth diamond", 800, () -> items(DIAMOND) >= 4);

        // ---- 样板镜像 ----
        t.step("ae2: pattern mirror image", 0, () -> {
            World world = t.world();
            BlockPos mirrorPos = ctrl.add(0, 0, -3);
            world.setBlock(mirrorPos.getX(), mirrorPos.getY(), mirrorPos.getZ(), BlocksMM.mePatternMirrorImage, 0, 3);
            MEPatternMirrorImage mirror = (MEPatternMirrorImage) DevSelfTest.tileAt(world, mirrorPos);
            t.expect("ae2: unbound mirror provides nothing", mirror.provideComponent() == null);
            mirror.providerPos = ctrl.add(0, 0, 1);
            MachineComponent<?> c = mirror.provideComponent();
            t.expect("ae2: bound mirror provides the provider's shared cache", c != null && c.getContainerProvider() == provider.getInfHandler());
            NBTTagCompound tag = new NBTTagCompound();
            mirror.writeCustomNBT(tag);
            MEPatternMirrorImage copy = new MEPatternMirrorImage();
            copy.readCustomNBT(tag);
            t.expect("ae2: mirror binding survives NBT", ctrl.add(0, 0, 1).equals(copy.providerPos));
        });

        // ---- 内存卡设置、带内容破坏 ----
        t.step("ae2: settings transfer (memory card data)", 0, () -> {
            itemIn.setConfig(3, new ItemStack(Blocks.cobblestone, 7));
            NBTTagCompound settings = itemIn.downloadSettings();
            itemIn.setConfig(3, null);
            itemIn.uploadSettings(settings);
            ItemStack restored = itemIn.getConfigInventory().getStackInSlot(3);
            t.expect("ae2: item input bus settings round trip (7 cobblestone in config slot 3)",
                restored != null && restored.getItem() == Item.getItemFromBlock(Blocks.cobblestone) && restored.stackSize == 7);
            NBTTagCompound fluidSettings;
            fluidIn.setConfig(2, lava(1));
            fluidSettings = fluidIn.downloadSettings();
            fluidIn.setConfig(2, null);
            fluidIn.uploadSettings(fluidSettings);
            FluidStack restoredFluid = fluidIn.getConfig(2);
            t.expect("ae2: fluid input bus settings round trip (lava in config slot 2)", restoredFluid != null && restoredFluid.getFluid() == FluidRegistry.LAVA);
        });
        t.step("ae2: break item input bus with a config", 5, () -> {
            World world = t.world();
            BlockPos p = ctrl.add(-2, 0, 1);
            world.func_147480_a(p.getX(), p.getY(), p.getZ(), true);
        });
        t.step("ae2: dropped bus keeps its config", 0, () -> {
            World world = t.world();
            BlockPos p = ctrl.add(-2, 0, 1);
            @SuppressWarnings("unchecked")
            List<EntityItem> drops = world.getEntitiesWithinAABB(EntityItem.class,
                AxisAlignedBB.getBoundingBox(p.getX() - 2, p.getY() - 2, p.getZ() - 2, p.getX() + 3, p.getY() + 3, p.getZ() + 3));
            int buses = 0;
            boolean withConfig = false;
            for (EntityItem e : drops) {
                ItemStack s = e.getEntityItem();
                if (s.getItem() == Item.getItemFromBlock(BlocksMM.meItemInputBus)) {
                    buses += s.stackSize;
                    withConfig |= s.hasTagCompound() && s.getTagCompound().hasKey("configInventory");
                }
                e.setDead();
            }
            t.expect("ae2: exactly one ME item input bus dropped, with its config in NBT", buses == 1 && withConfig);
        });
    }
}
