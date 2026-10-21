package hellfirepvp.modularmachinery.common.dev;

import cpw.mods.fml.common.registry.GameRegistry;
import github.kasuminova.mmce.compat.BlockPos;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

import java.lang.reflect.Method;

/**
 * 仅开发环境：与其他模组的管道 / 能量设备实际对接的自测（DevSelfTest 的 pipes 等阶段）。其他模组的类一律经反射或 GameRegistry 访问，
 * 本类在未装这些模组的环境中也能加载。
 * <ul>
 *   <li>pipes：testdata 中的 pipe_test（流体输入仓 + 流体输出仓，1000 水 → 1000 熔岩）。两台：一台用 Mekanism 基础机械管道，
 *       一台用 EnderIO 流体导管——水从 EnderIO 储罐经管道 / 导管进入输入仓（靠储罐一端设为抽取），产出的熔岩从输出仓经管道 / 导管
 *       （靠输出仓一端设为抽取）送进另一个 EnderIO 储罐。</li>
 * </ul>
 */
final class DevInteropTest {

    private static final String PIPE_TEST = "pipe_test";

    private final DevSelfTest t;

    private DevInteropTest(DevSelfTest t) {
        this.t = t;
    }

    private void info(String line) {
        CommandMMCETest.report(t.sender(), "INFO " + line);
    }

    // ---------------- 流体储罐（EnderIO blockTank） ----------------

    private static Block block(String modId, String name) {
        Block b = GameRegistry.findBlock(modId, name);
        if (b == null) {
            throw new IllegalStateException(modId + ":" + name + " not found");
        }
        return b;
    }

    private static IFluidHandler fluidHandler(World world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
        if (!(te instanceof IFluidHandler handler)) {
            throw new IllegalStateException("no fluid handler at " + pos + ": " + te);
        }
        return handler;
    }

    private static int amount(World world, BlockPos pos, Fluid fluid) {
        int total = 0;
        for (FluidTankInfo info : fluidHandler(world, pos).getTankInfo(ForgeDirection.UNKNOWN)) {
            if (info.fluid != null && info.fluid.getFluid() == fluid) {
                total += info.fluid.amount;
            }
        }
        return total;
    }

    // ---------------- EnderIO 导管 ----------------

    /** 把 pos 处 EnderIO 流体导管朝 side 的连接方式设为 INPUT（从相邻方块抽取）、抽取不受红石控制，与在导管界面中设置的效果相同。 */
    private static void setConduitInput(World world, BlockPos pos, ForgeDirection side) {
        setConduitMode(world, pos, side, "INPUT");
    }

    /** EnderIO 流体导管连接外部储罐时默认是 INPUT（抽取）；送入目标的一面要设为 OUTPUT，与玩家用扳手切换的效果相同。 */
    private static void setConduitOutput(World world, BlockPos pos, ForgeDirection side) {
        setConduitMode(world, pos, side, "OUTPUT");
    }

    private static final String LIQUID_CONDUIT = "crazypants.enderio.conduit.liquid.ILiquidConduit";
    private static final String ITEM_CONDUIT   = "crazypants.enderio.conduit.item.IItemConduit";

    private static void setConduitMode(World world, BlockPos pos, ForgeDirection side, String mode) {
        setConduitMode(world, pos, side, mode, LIQUID_CONDUIT);
    }

    /** conduitType：导管束中要设置的导管接口（流体 / 物品导管）。 */
    private static void setConduitMode(World world, BlockPos pos, ForgeDirection side, String mode, String conduitType) {
        TileEntity te = world.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
        try {
            Class<?> type = Class.forName(conduitType);
            Object conduit = te.getClass().getMethod("getConduit", Class.class).invoke(te, type);
            if (conduit == null) {
                throw new IllegalStateException("no " + type.getSimpleName() + " at " + pos);
            }
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object input = Enum.valueOf((Class) Class.forName("crazypants.enderio.conduit.ConnectionMode"), mode);
            Method set = null;
            for (Method m : conduit.getClass().getMethods()) {
                if (m.getName().equals("setConnectionMode") && m.getParameterTypes().length == 2) {
                    set = m;
                }
            }
            if (set == null) {
                throw new IllegalStateException("setConnectionMode not found on " + conduit.getClass());
            }
            set.invoke(conduit, side, input);
            // EnderIO 导管的抽取默认要有红石信号（RedstoneControlMode.ON），这里设为“始终工作”（导管界面中的同名选项）
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object ignore = Enum.valueOf((Class) Class.forName("crazypants.enderio.machine.RedstoneControlMode"), "IGNORE");
            conduit.getClass().getMethod("setExtractionRedstoneMode", ignore.getClass(), ForgeDirection.class).invoke(conduit, ignore, side);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot configure conduit at " + pos, e);
        }
    }

    private static String conduitModes(World world, BlockPos pos) {
        return conduitModes(world, pos, LIQUID_CONDUIT);
    }

    private static String conduitModes(World world, BlockPos pos, String conduitType) {
        TileEntity te = world.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
        try {
            Object conduit = te.getClass().getMethod("getConduit", Class.class).invoke(te, Class.forName(conduitType));
            Method get = conduit.getClass().getMethod("getConnectionMode", ForgeDirection.class);
            return "W=" + get.invoke(conduit, ForgeDirection.WEST) + " E=" + get.invoke(conduit, ForgeDirection.EAST);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return "? " + e;
        }
    }

    // ---------------- pipes ----------------

    static void buildPipes(DevSelfTest t) {
        new DevInteropTest(t).pipes();
    }

    private BlockPos mekCtrl;
    private BlockPos eioCtrl;

    private void pipes() {
        t.step("pipes: build two pipe_test machines with EnderIO tanks", 40, () -> {
            DynamicMachine machine = DevSelfTest.machine(PIPE_TEST);
            if (machine == null) {
                throw new IllegalStateException("testdata machine pipe_test not loaded");
            }
            World world = t.world();
            ChunkCoordinates spawn = world.getSpawnPoint();
            mekCtrl = DevSelfTest.groundAbove(world, spawn.posX - 120, spawn.posZ + 3);
            eioCtrl = DevSelfTest.groundAbove(world, spawn.posX - 120, spawn.posZ + 10);
            for (BlockPos ctrl : new BlockPos[]{mekCtrl, eioCtrl}) {
                CommandMMCETest.buildAt(t.sender(), world, machine, ctrl);
                // 管道 / 导管放在下方方块的顶面上：地形起伏时先铺一排石头，并清空管道所在的格
                for (int dx = -4; dx <= 4; dx++) {
                    if (dx >= -1 && dx <= 1) {
                        continue;
                    }
                    world.setBlock(ctrl.getX() + dx, ctrl.getY() - 1, ctrl.getZ(), net.minecraft.init.Blocks.stone);
                    world.setBlockToAir(ctrl.getX() + dx, ctrl.getY(), ctrl.getZ());
                    world.setBlockToAir(ctrl.getX() + dx, ctrl.getY() + 1, ctrl.getZ());
                }
                Block tank = block("EnderIO", "blockTank");
                for (int dx : new int[]{-4, 4}) {
                    BlockPos p = ctrl.add(dx, 0, 0);
                    world.setBlock(p.getX(), p.getY(), p.getZ(), tank, 0, 3);
                }
                int filled = fluidHandler(world, ctrl.add(-4, 0, 0)).fill(ForgeDirection.EAST, new FluidStack(FluidRegistry.WATER, 16000), true);
                info("EnderIO tank filled with " + filled + " mB water at " + ctrl.add(-4, 0, 0));
            }
        });
        t.step("pipes: Mekanism mechanical pipes", 10, () -> {
            World world = t.world();
            // Mekanism 基础机械管道：PartTransmitter@4（TransmitterType.MECHANICAL_PIPE_BASIC）
            for (int dx : new int[]{-3, -2, 2, 3}) {
                t.placeItemAt(world, mekCtrl.add(dx, 0, 0), "Mekanism", "PartTransmitter", 4);
            }
            // 靠储罐一端从储罐抽水；靠输出仓一端从输出仓抽熔岩
            DevGasSupport.setTubePull(world, mekCtrl.add(-3, 0, 0), ForgeDirection.WEST);
            DevGasSupport.setTubePull(world, mekCtrl.add(2, 0, 0), ForgeDirection.WEST);
        });
        t.step("pipes: EnderIO fluid conduits", 10, () -> {
            World world = t.world();
            for (int dx : new int[]{-3, -2, 2, 3}) {
                t.placeItemAt(world, eioCtrl.add(dx, 0, 0), "EnderIO", "itemLiquidConduit", 0);
            }
            setConduitInput(world, eioCtrl.add(-3, 0, 0), ForgeDirection.WEST);
            setConduitOutput(world, eioCtrl.add(-2, 0, 0), ForgeDirection.EAST);
            setConduitInput(world, eioCtrl.add(2, 0, 0), ForgeDirection.WEST);
            setConduitOutput(world, eioCtrl.add(3, 0, 0), ForgeDirection.EAST);
            info("EnderIO conduit modes: " + conduitModes(world, eioCtrl.add(-3, 0, 0)) + " | " + conduitModes(world, eioCtrl.add(-2, 0, 0))
                + " | " + conduitModes(world, eioCtrl.add(2, 0, 0)) + " | " + conduitModes(world, eioCtrl.add(3, 0, 0)));
        });
        t.waitUntil("pipes: Mekanism pipes deliver 2000 mB lava from the output hatch to the sink tank", 1200,
            () -> amount(t.world(), mekCtrl.add(4, 0, 0), FluidRegistry.LAVA) >= 2000);
        t.waitUntil("pipes: EnderIO conduits deliver 2000 mB lava from the output hatch to the sink tank", 1200,
            () -> amount(t.world(), eioCtrl.add(4, 0, 0), FluidRegistry.LAVA) >= 2000);
        t.step("pipes: totals", 0, () -> {
            World world = t.world();
            for (BlockPos ctrl : new BlockPos[]{mekCtrl, eioCtrl}) {
                String label = ctrl == mekCtrl ? "Mekanism" : "EnderIO";
                int water = amount(world, ctrl.add(-4, 0, 0), FluidRegistry.WATER);
                int lava = amount(world, ctrl.add(4, 0, 0), FluidRegistry.LAVA);
                info(label + ": source tank water=" + water + " sink tank lava=" + lava);
                CommandMMCETest.statusAt(t.sender(), world, DevSelfTest.machine(PIPE_TEST), ctrl);
                t.expect("pipes: " + label + ": water left the source tank through the pipes", water < 16000);
                t.expect("pipes: " + label + ": lava reached the sink tank in 1000 mB recipe units (plus at most what is still in transit)", lava >= 2000);
            }
        });
    }

    // ---------------- te-rf：热力膨胀 RF ----------------

    static void buildThermalRF(DevSelfTest t) {
        new DevInteropTest(t).thermalRF();
    }

    private static final String ENERGY_TEST = "energy_test";
    private BlockPos energyCtrl;

    private static Object invoke(Object target, String name, Class<?>[] types, Object... args) {
        try {
            return target.getClass().getMethod(name, types).invoke(target, args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot call " + name + " on " + target.getClass().getName(), e);
        }
    }

    private static int rfStored(World world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
        return (Integer) invoke(te, "getEnergyStored", new Class<?>[]{ForgeDirection.class}, ForgeDirection.UNKNOWN);
    }

    private int countIn(BlockPos busPos, net.minecraft.item.Item item) {
        TileEntity te = DevSelfTest.tileAt(t.world(), busPos);
        int n = 0;
        if (te instanceof hellfirepvp.modularmachinery.common.tiles.base.TileItemBus bus) {
            for (int i = 0; i < bus.getInventory().getSlots(); i++) {
                net.minecraft.item.ItemStack s = bus.getInventory().getStackInSlot(i);
                if (s != null && s.getItem() == item) {
                    n += s.stackSize;
                }
            }
        }
        return n;
    }

    /**
     * 测试机 energy_test（能量输入仓 + 能量输出仓 + 物品输入 / 输出总线）：热力膨胀岩浆动力机（装 4000 mB 熔岩，朝向输入仓）→ 能量输入仓，
     * 机器耗电把圆石烧成石头；机器把泥土“烧”成 RF → 能量输出仓 → 热力膨胀基础能量元件。
     * 动力机 / 元件用 setBlock 放置，不经过物品放置逻辑，所以像玩家用扳手那样设置朝向，并补上元件的接收速率（物品放置时由 NBT 设定）。
     */
    private void thermalRF() {
        t.step("te-rf: build energy_test with a magmatic dynamo and an energy cell", 20, () -> {
            DynamicMachine machine = DevSelfTest.machine(ENERGY_TEST);
            if (machine == null) {
                throw new IllegalStateException("testdata machine energy_test not loaded");
            }
            World world = t.world();
            ChunkCoordinates spawn = world.getSpawnPoint();
            // 专用服务器上没有玩家时，出生点 128 格以外的区块会被卸载，测试机器都放在这个范围内
            energyCtrl = DevSelfTest.groundAbove(world, spawn.posX - 30, spawn.posZ + 40);
            CommandMMCETest.buildAt(t.sender(), world, machine, energyCtrl);
            BlockPos dynamo = energyCtrl.add(-2, 0, 0);
            BlockPos cell = energyCtrl.add(2, 0, 0);
            world.setBlock(dynamo.getX(), dynamo.getY(), dynamo.getZ(), block("ThermalExpansion", "Dynamo"), 1, 3);
            world.setBlock(cell.getX(), cell.getY(), cell.getZ(), block("ThermalExpansion", "Cell"), 1, 3);
            TileEntity dyn = DevSelfTest.tileAt(world, dynamo);
            // 玩家放置动力机时 BlockDynamo 调用 blockPlaced()，朝向相邻的 RF 接收方（这里唯一的接收方是东侧的能量输入仓）
            invoke(dyn, "blockPlaced", new Class<?>[0]);
            boolean faced = (Integer) invoke(dyn, "getFacing", new Class<?>[0]) == ForgeDirection.EAST.ordinal();
            int lava = ((IFluidHandler) dyn).fill(ForgeDirection.UNKNOWN, new FluidStack(FluidRegistry.LAVA, 4000), true);
            TileEntity cel = DevSelfTest.tileAt(world, cell);
            boolean sided = (Boolean) invoke(cel, "setSide", new Class<?>[]{int.class, int.class}, ForgeDirection.WEST.ordinal(), 2);
            try {
                int[] maxReceive = (int[]) cel.getClass().getField("MAX_RECEIVE").get(null);
                cel.getClass().getField("energyReceive").setInt(cel, maxReceive[1]);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
            info("dynamo " + dyn.getClass().getSimpleName() + " facing east=" + faced + " lava=" + lava + ", cell " + cel.getClass().getSimpleName() + " west side input=" + sided);
            TileEntity in = DevSelfTest.tileAt(world, energyCtrl.add(0, 0, 1));
            ((hellfirepvp.modularmachinery.common.tiles.base.TileItemBus) in).getInventory()
                .setStackInSlot(0, new net.minecraft.item.ItemStack(net.minecraft.init.Blocks.cobblestone, 8));
        });
        t.waitUntil("te-rf: magmatic dynamo powers the energy input hatch (2 stone made with 100 RF/t)", 1200,
            () -> countIn(energyCtrl.add(0, 1, 0), net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.stone)) >= 2);
        t.step("te-rf: feed dirt for the RF-producing recipe", 0, () -> {
            CommandMMCETest.statusAt(t.sender(), t.world(), DevSelfTest.machine(ENERGY_TEST), energyCtrl);
            TileEntity in = DevSelfTest.tileAt(t.world(), energyCtrl.add(0, 0, 1));
            ((hellfirepvp.modularmachinery.common.tiles.base.TileItemBus) in).getInventory()
                .setStackInSlot(1, new net.minecraft.item.ItemStack(net.minecraft.init.Blocks.dirt, 4));
        });
        t.waitUntil("te-rf: energy output hatch pushes RF into the TE energy cell (>= 2000 RF, one recipe)", 1200,
            () -> rfStored(t.world(), energyCtrl.add(2, 0, 0)) >= 2000);
        t.step("te-rf: totals", 0, () -> {
            int stone = countIn(energyCtrl.add(0, 1, 0), net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.stone));
            int cellRf = rfStored(t.world(), energyCtrl.add(2, 0, 0));
            TileEntity ctrlTe = DevSelfTest.tileAt(t.world(), energyCtrl);
            info("controller ticksExisted=" + ((hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController) ctrlTe).getTicksExisted()
                + " stone=" + stone + " TE cell=" + cellRf + " RF");
            CommandMMCETest.statusAt(t.sender(), t.world(), DevSelfTest.machine(ENERGY_TEST), energyCtrl);
            t.expect("te-rf: RF arrived in both directions", stone >= 2 && cellRf >= 2000);
        });
    }

    // ---------------- bc：BuildCraft 7.1 ----------------

    static void buildBuildCraft(DevSelfTest t) {
        new DevInteropTest(t).buildCraft();
    }

    private BlockPos bcEnergyCtrl;
    private BlockPos bcPipeCtrl;

    private static Object staticField(String className, String field) {
        try {
            return Class.forName(className).getField(field).get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(className + "." + field, e);
        }
    }

    /** BuildCraft 管道物品：BlockGenericPipe.pipes 中管道类名为 pipeClass 的那个物品。 */
    private static net.minecraft.item.Item bcPipe(String pipeClass) {
        java.util.Map<?, ?> pipes = (java.util.Map<?, ?>) staticField("buildcraft.transport.BlockGenericPipe", "pipes");
        for (java.util.Map.Entry<?, ?> e : pipes.entrySet()) {
            if (((Class<?>) e.getValue()).getSimpleName().equals(pipeClass)) {
                return (net.minecraft.item.Item) e.getKey();
            }
        }
        throw new IllegalStateException("BuildCraft pipe " + pipeClass + " not registered");
    }

    /** 放一台创造引擎（engineBlock@3），上方放红石块供电，转向相邻的唯一接收方（同玩家用扳手转向）。powerMode 为 null 时保持默认 20 RF/t。 */
    private void bcCreativeEngine(World world, BlockPos pos, String powerMode) {
        Block engine = (Block) staticField("buildcraft.BuildCraftCore", "engineBlock");
        world.setBlock(pos.getX(), pos.getY(), pos.getZ(), engine, 3, 3);
        TileEntity te = world.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
        try {
            if (powerMode != null) {
                java.lang.reflect.Field mode = te.getClass().getDeclaredField("powerMode");
                mode.setAccessible(true);
                @SuppressWarnings({"unchecked", "rawtypes"})
                Object value = Enum.valueOf((Class) mode.getType(), powerMode);
                mode.set(te, value);
            }
            boolean switched = (Boolean) te.getClass().getMethod("switchOrientation", boolean.class).invoke(te, false);
            world.setBlock(pos.getX(), pos.getY() + 1, pos.getZ(), net.minecraft.init.Blocks.redstone_block);
            te.getClass().getMethod("checkRedstonePower").invoke(te);
            info("BC creative engine at " + pos + " orientation=" + te.getClass().getField("orientation").get(te) + " switched=" + switched
                + " redstone=" + te.getClass().getField("isRedstonePowered").get(te) + (powerMode == null ? "" : " powerMode=" + powerMode));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot set up BuildCraft engine at " + pos, e);
        }
    }

    /**
     * BuildCraft 7.1（GTNH 7.1.64）：
     * 能量：创造引擎（1280 RF/t）到木质能量管道、金质能量管道，再到 energy_test 的能量输入仓，机器耗电烧石头；
     * 流体：BC 储罐（水）到木质流体管道（上方创造引擎供能才会抽取）、金质流体管道，再到 pipe_test 的流体输入仓；
     * 流体输出仓到木质流体管道（同样由引擎供能抽取）、金质流体管道，再到另一个 BC 储罐。
     */
    private void buildCraft() {
        t.step("bc: build energy_test and pipe_test", 20, () -> {
            World world = t.world();
            ChunkCoordinates spawn = world.getSpawnPoint();
            bcEnergyCtrl = DevSelfTest.groundAbove(world, spawn.posX + 30, spawn.posZ + 40);
            bcPipeCtrl = DevSelfTest.groundAbove(world, spawn.posX + 30, spawn.posZ + 50);
            CommandMMCETest.buildAt(t.sender(), world, DevSelfTest.machine(ENERGY_TEST), bcEnergyCtrl);
            CommandMMCETest.buildAt(t.sender(), world, DevSelfTest.machine(PIPE_TEST), bcPipeCtrl);
            for (BlockPos ctrl : new BlockPos[]{bcEnergyCtrl, bcPipeCtrl}) {
                for (int dx = -4; dx <= 4; dx++) {
                    if (dx >= -1 && dx <= 1) {
                        continue;
                    }
                    world.setBlock(ctrl.getX() + dx, ctrl.getY() - 1, ctrl.getZ(), net.minecraft.init.Blocks.stone);
                    for (int dy = 0; dy <= 2; dy++) {
                        world.setBlockToAir(ctrl.getX() + dx, ctrl.getY() + dy, ctrl.getZ());
                    }
                }
            }
            Block tank = (Block) staticField("buildcraft.BuildCraftFactory", "tankBlock");
            for (int dx : new int[]{-4, 4}) {
                world.setBlock(bcPipeCtrl.getX() + dx, bcPipeCtrl.getY(), bcPipeCtrl.getZ(), tank, 0, 3);
            }
            int filled = fluidHandler(world, bcPipeCtrl.add(-4, 0, 0)).fill(ForgeDirection.EAST, new FluidStack(FluidRegistry.WATER, 16000), true);
            info("BC tank filled with " + filled + " mB water");
            ((hellfirepvp.modularmachinery.common.tiles.base.TileItemBus) DevSelfTest.tileAt(world, bcEnergyCtrl.add(0, 0, 1))).getInventory()
                .setStackInSlot(0, new net.minecraft.item.ItemStack(net.minecraft.init.Blocks.cobblestone, 8));
        });
        t.step("bc: pipes", 10, () -> {
            World world = t.world();
            t.placeItemAt(world, bcEnergyCtrl.add(-3, 0, 0), bcPipe("PipePowerWood"), 0, "BuildCraft PipePowerWood");
            t.placeItemAt(world, bcEnergyCtrl.add(-2, 0, 0), bcPipe("PipePowerGold"), 0, "BuildCraft PipePowerGold");
            t.placeItemAt(world, bcPipeCtrl.add(-3, 0, 0), bcPipe("PipeFluidsWood"), 0, "BuildCraft PipeFluidsWood");
            t.placeItemAt(world, bcPipeCtrl.add(-2, 0, 0), bcPipe("PipeFluidsGold"), 0, "BuildCraft PipeFluidsGold");
            t.placeItemAt(world, bcPipeCtrl.add(2, 0, 0), bcPipe("PipeFluidsWood"), 0, "BuildCraft PipeFluidsWood");
            t.placeItemAt(world, bcPipeCtrl.add(3, 0, 0), bcPipe("PipeFluidsGold"), 0, "BuildCraft PipeFluidsGold");
        });
        t.step("bc: creative engines", 10, () -> {
            World world = t.world();
            // 能量：引擎在木质能量管道西侧；流体：引擎在木质流体管道正上方（红石块再在引擎上方）
            bcCreativeEngine(world, bcEnergyCtrl.add(-4, 0, 0), "M128");
            bcCreativeEngine(world, bcPipeCtrl.add(-3, 1, 0), null);
            bcCreativeEngine(world, bcPipeCtrl.add(2, 1, 0), null);
        });
        t.waitUntil("bc: creative engine to kinesis pipes to energy input hatch (2 stone made with 100 RF/t)", 1200,
            () -> countIn(bcEnergyCtrl.add(0, 1, 0), net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.stone)) >= 2);
        t.waitUntil("bc: tank to wooden/gold fluid pipes to machine to wooden/gold fluid pipes to tank (2000 mB lava)", 2400,
            () -> amount(t.world(), bcPipeCtrl.add(4, 0, 0), FluidRegistry.LAVA) >= 2000);
        t.step("bc: totals", 0, () -> {
            World world = t.world();
            int stone = countIn(bcEnergyCtrl.add(0, 1, 0), net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.stone));
            int water = amount(world, bcPipeCtrl.add(-4, 0, 0), FluidRegistry.WATER);
            int lava = amount(world, bcPipeCtrl.add(4, 0, 0), FluidRegistry.LAVA);
            info("BuildCraft: stone=" + stone + ", source tank water=" + water + ", sink tank lava=" + lava);
            CommandMMCETest.statusAt(t.sender(), world, DevSelfTest.machine(ENERGY_TEST), bcEnergyCtrl);
            CommandMMCETest.statusAt(t.sender(), world, DevSelfTest.machine(PIPE_TEST), bcPipeCtrl);
            t.expect("bc: RF and fluids moved through BuildCraft pipes", stone >= 2 && water < 16000 && lava >= 2000);
        });
    }

    // ---------------- exu：Extra Utilities 1.2.12 ----------------

    static void buildExtraUtilities(DevSelfTest t) {
        new DevInteropTest(t).extraUtilities();
    }

    private BlockPos exuEnergyCtrl;
    private BlockPos exuPipeCtrl;

    /** ExU 发电机方块 block 中 tileClass 对应的 meta（BlockGenerator.tiles 的下标）。 */
    private static int exuGeneratorMeta(String tileClass) {
        Object[] tiles = (Object[]) staticField("com.rwtema.extrautils.tileentity.generators.BlockGenerator", "tiles");
        for (int i = 0; i < tiles.length; i++) {
            if (tiles[i] != null && ((Class<?>) tiles[i]).getSimpleName().equals(tileClass)) {
                return i;
            }
        }
        throw new IllegalStateException("ExU generator " + tileClass + " not found");
    }

    /**
     * Extra Utilities 1.2.12：
     * 能量：8 倍岩浆发电机（generator2，装熔岩）紧贴 energy_test 的能量输入仓（ExU 发电机向六面输出 RF）；
     * 流体：ExU 桶（drum，装水）到流体传输节点（meta 6 + 方向，贴在桶上）、传输管道，再到 pipe_test 的流体输入仓；
     * 流体输出仓到流体传输节点（贴在输出仓上）、传输管道，再到另一个 ExU 桶。
     */
    private void extraUtilities() {
        t.step("exu: build energy_test and pipe_test", 20, () -> {
            World world = t.world();
            ChunkCoordinates spawn = world.getSpawnPoint();
            exuEnergyCtrl = DevSelfTest.groundAbove(world, spawn.posX + 50, spawn.posZ + 40);
            exuPipeCtrl = DevSelfTest.groundAbove(world, spawn.posX + 50, spawn.posZ + 50);
            CommandMMCETest.buildAt(t.sender(), world, DevSelfTest.machine(ENERGY_TEST), exuEnergyCtrl);
            CommandMMCETest.buildAt(t.sender(), world, DevSelfTest.machine(PIPE_TEST), exuPipeCtrl);
            for (BlockPos ctrl : new BlockPos[]{exuEnergyCtrl, exuPipeCtrl}) {
                for (int dx = -4; dx <= 4; dx++) {
                    if (dx >= -1 && dx <= 1) {
                        continue;
                    }
                    world.setBlock(ctrl.getX() + dx, ctrl.getY() - 1, ctrl.getZ(), net.minecraft.init.Blocks.stone);
                    for (int dy = 0; dy <= 2; dy++) {
                        world.setBlockToAir(ctrl.getX() + dx, ctrl.getY() + dy, ctrl.getZ());
                    }
                }
            }
            // 能量
            Block generator2 = (Block) staticField("com.rwtema.extrautils.ExtraUtils", "generator2");
            int magma = exuGeneratorMeta("TileEntityGeneratorMagma");
            BlockPos gen = exuEnergyCtrl.add(-2, 0, 0);
            world.setBlock(gen.getX(), gen.getY(), gen.getZ(), generator2, magma, 3);
            int lava = fluidHandler(world, gen).fill(ForgeDirection.EAST, new FluidStack(FluidRegistry.LAVA, 4000), true);
            info("ExU 8x lava generator (meta " + magma + ") " + DevSelfTest.tileAt(world, gen).getClass().getSimpleName() + " lava=" + lava);
            ((hellfirepvp.modularmachinery.common.tiles.base.TileItemBus) DevSelfTest.tileAt(world, exuEnergyCtrl.add(0, 0, 1))).getInventory()
                .setStackInSlot(0, new net.minecraft.item.ItemStack(net.minecraft.init.Blocks.cobblestone, 8));
            // 流体
            Block drum = (Block) staticField("com.rwtema.extrautils.ExtraUtils", "drum");
            Block node = (Block) staticField("com.rwtema.extrautils.ExtraUtils", "transferNode");
            Block pipe = (Block) staticField("com.rwtema.extrautils.ExtraUtils", "transferPipe");
            for (int dx : new int[]{-4, 4}) {
                world.setBlock(exuPipeCtrl.getX() + dx, exuPipeCtrl.getY(), exuPipeCtrl.getZ(), drum, 0, 3);
            }
            int water = fluidHandler(world, exuPipeCtrl.add(-4, 0, 0)).fill(ForgeDirection.EAST, new FluidStack(FluidRegistry.WATER, 16000), true);
            // 流体节点 meta = 6 + 所贴方块的方向（两处都贴在西侧：桶 / 输出仓）
            int nodeMeta = 6 + ForgeDirection.WEST.ordinal();
            world.setBlock(exuPipeCtrl.getX() - 3, exuPipeCtrl.getY(), exuPipeCtrl.getZ(), node, nodeMeta, 3);
            world.setBlock(exuPipeCtrl.getX() - 2, exuPipeCtrl.getY(), exuPipeCtrl.getZ(), pipe, 0, 3);
            world.setBlock(exuPipeCtrl.getX() + 2, exuPipeCtrl.getY(), exuPipeCtrl.getZ(), node, nodeMeta, 3);
            world.setBlock(exuPipeCtrl.getX() + 3, exuPipeCtrl.getY(), exuPipeCtrl.getZ(), pipe, 0, 3);
            info("ExU drum filled with " + water + " mB water; liquid nodes " + DevSelfTest.tileAt(world, exuPipeCtrl.add(-3, 0, 0)).getClass().getSimpleName());
        });
        t.waitUntil("exu: lava generator powers the energy input hatch (2 stone made with 100 RF/t)", 1200,
            () -> countIn(exuEnergyCtrl.add(0, 1, 0), net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.stone)) >= 2);
        t.waitUntil("exu: drum to liquid node and pipe to machine to liquid node and pipe to drum (2000 mB lava)", 2400,
            () -> amount(t.world(), exuPipeCtrl.add(4, 0, 0), FluidRegistry.LAVA) >= 2000);
        t.step("exu: totals", 0, () -> {
            World world = t.world();
            int stone = countIn(exuEnergyCtrl.add(0, 1, 0), net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.stone));
            int water = amount(world, exuPipeCtrl.add(-4, 0, 0), FluidRegistry.WATER);
            int lava = amount(world, exuPipeCtrl.add(4, 0, 0), FluidRegistry.LAVA);
            info("Extra Utilities: stone=" + stone + ", source drum water=" + water + ", sink drum lava=" + lava);
            CommandMMCETest.statusAt(t.sender(), world, DevSelfTest.machine(ENERGY_TEST), exuEnergyCtrl);
            CommandMMCETest.statusAt(t.sender(), world, DevSelfTest.machine(PIPE_TEST), exuPipeCtrl);
            t.expect("exu: RF and fluids moved through Extra Utilities generators / transfer nodes", stone >= 2 && water < 16000 && lava >= 2000);
        });
    }

    // ---------------- railcraft：Railcraft 9.17（GTNH） ----------------

    static void buildRailcraft(DevSelfTest t) {
        new DevInteropTest(t).railcraft();
    }

    private BlockPos rcSteamCtrl;
    private BlockPos rcEnergyCtrl;

    /** Railcraft 的 EnumMachineBeta 中某项对应的方块物品（方块 + meta）。 */
    private static net.minecraft.item.ItemStack railcraftBeta(String name) {
        try {
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object machine = Enum.valueOf((Class) Class.forName("mods.railcraft.common.blocks.machine.beta.EnumMachineBeta"), name);
            return (net.minecraft.item.ItemStack) machine.getClass().getMethod("getItem").invoke(machine);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Railcraft EnumMachineBeta." + name, e);
        }
    }

    /**
     * Railcraft 9.17：测试机 steam_test 烧煤产出 4000 mB 蒸汽（Railcraft 的 steam 流体）到流体输出仓，
     * Mekanism 机械管道从输出仓抽出送进 Railcraft 商用蒸汽引擎（上方红石块启动，转向东侧唯一的 RF 接收方），
     * 引擎的 RF 进入 energy_test 的能量输入仓，机器耗电（20 RF/t）把沙子烧成玻璃。
     */
    private void railcraft() {
        t.step("railcraft: build steam_test and energy_test", 20, () -> {
            World world = t.world();
            ChunkCoordinates spawn = world.getSpawnPoint();
            rcSteamCtrl = DevSelfTest.groundAbove(world, spawn.posX + 70, spawn.posZ + 40);
            rcEnergyCtrl = rcSteamCtrl.add(5, 0, 0);
            CommandMMCETest.buildAt(t.sender(), world, DevSelfTest.machine("steam_test"), rcSteamCtrl);
            CommandMMCETest.buildAt(t.sender(), world, DevSelfTest.machine(ENERGY_TEST), rcEnergyCtrl);
            for (int dx = 2; dx <= 3; dx++) {
                world.setBlock(rcSteamCtrl.getX() + dx, rcSteamCtrl.getY() - 1, rcSteamCtrl.getZ(), net.minecraft.init.Blocks.stone);
                for (int dy = 0; dy <= 2; dy++) {
                    world.setBlockToAir(rcSteamCtrl.getX() + dx, rcSteamCtrl.getY() + dy, rcSteamCtrl.getZ());
                }
            }
            ((hellfirepvp.modularmachinery.common.tiles.base.TileItemBus) DevSelfTest.tileAt(world, rcSteamCtrl.add(0, 0, 1))).getInventory()
                .setStackInSlot(0, new net.minecraft.item.ItemStack(net.minecraft.init.Items.coal, 8));
            ((hellfirepvp.modularmachinery.common.tiles.base.TileItemBus) DevSelfTest.tileAt(world, rcEnergyCtrl.add(0, 0, 1))).getInventory()
                .setStackInSlot(0, new net.minecraft.item.ItemStack(net.minecraft.init.Blocks.sand, 8));
        });
        t.step("railcraft: pipe and commercial steam engine", 10, () -> {
            World world = t.world();
            t.placeItemAt(world, rcSteamCtrl.add(2, 0, 0), "Mekanism", "PartTransmitter", 4);
            DevGasSupport.setTubePull(world, rcSteamCtrl.add(2, 0, 0), ForgeDirection.WEST);
            net.minecraft.item.ItemStack engineStack = railcraftBeta("ENGINE_STEAM_LOW");
            BlockPos engine = rcSteamCtrl.add(3, 0, 0);
            world.setBlock(engine.getX(), engine.getY(), engine.getZ(), Block.getBlockFromItem(engineStack.getItem()), engineStack.getItemDamage(), 3);
            TileEntity te = DevSelfTest.tileAt(world, engine);
            boolean switched = (Boolean) invoke(te, "switchOrientation", new Class<?>[0]);
            world.setBlock(engine.getX(), engine.getY() + 1, engine.getZ(), net.minecraft.init.Blocks.redstone_block);
            te.getBlockType().onNeighborBlockChange(world, engine.getX(), engine.getY(), engine.getZ(), net.minecraft.init.Blocks.redstone_block);
            info("Railcraft engine " + te.getClass().getSimpleName() + " orientation=" + invoke(te, "getOrientation", new Class<?>[0])
                + " switched=" + switched + " powered=" + invoke(te, "isPowered", new Class<?>[0]));
        });
        t.waitUntil("railcraft: steam from the output hatch reaches the engine through the pipe", 1200,
            () -> amount(t.world(), rcSteamCtrl.add(3, 0, 0), FluidRegistry.getFluid("steam")) > 0
                || (Integer) invoke(DevSelfTest.tileAt(t.world(), rcSteamCtrl.add(3, 0, 0)), "getEnergy", new Class<?>[0]) > 0);
        t.waitUntil("railcraft: engine RF powers energy_test (2 glass made with 20 RF/t)", 2400,
            () -> countIn(rcEnergyCtrl.add(0, 1, 0), net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.glass)) >= 2);
        t.step("railcraft: totals", 0, () -> {
            World world = t.world();
            int glass = countIn(rcEnergyCtrl.add(0, 1, 0), net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.glass));
            TileEntity te = DevSelfTest.tileAt(world, rcSteamCtrl.add(3, 0, 0));
            info("Railcraft: glass=" + glass + ", engine steam=" + amount(world, rcSteamCtrl.add(3, 0, 0), FluidRegistry.getFluid("steam"))
                + " mB, engine energy=" + invoke(te, "getEnergy", new Class<?>[0]) + ", output=" + invoke(te, "getCurrentOutput", new Class<?>[0]));
            CommandMMCETest.statusAt(t.sender(), world, DevSelfTest.machine("steam_test"), rcSteamCtrl);
            CommandMMCETest.statusAt(t.sender(), world, DevSelfTest.machine(ENERGY_TEST), rcEnergyCtrl);
            t.expect("railcraft: steam out of MMCE drove the Railcraft engine, whose RF powered MMCE", glass >= 2);
        });
    }

    // ---------------- gt：GregTech 5（GTNH GT5-Unofficial） ----------------

    static void buildGregTech(DevSelfTest t) {
        new DevInteropTest(t).gregTech();
    }

    private static final String GT_TEST = "gt_test";
    private BlockPos gtSourceCtrl;
    private BlockPos gtSinkCtrl;

    /** GT5 的锡导线（1x，LV）物品：GTOreDictUnificator.get(OrePrefixes.cableGt01, Materials.Tin, 1)。 */
    private static net.minecraft.item.ItemStack gtTinCable() {
        try {
            Class<?> prefixes = Class.forName("gregtech.api.enums.OrePrefixes");
            Object cable = prefixes.getField("cableGt01").get(null);
            Object tin = Class.forName("gregtech.api.enums.Materials").getField("Tin").get(null);
            return (net.minecraft.item.ItemStack) Class.forName("gregtech.api.util.GTOreDictUnificator")
                .getMethod("get", prefixes, Object.class, long.class).invoke(null, cable, tin, 1L);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("GT tin cable", e);
        }
    }

    /** 让 pos 处的 GT 导线连接 sides 中的各面（GTNH 默认 GT6 式导线只连接放置时所贴的面，其余面要用扳手连接）。 */
    private static String gtConnect(World world, BlockPos pos, ForgeDirection... sides) {
        TileEntity te = world.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
        Object mte = invoke(te, "getMetaTileEntity", new Class<?>[0]);
        StringBuilder sb = new StringBuilder(mte.getClass().getSimpleName());
        for (ForgeDirection side : sides) {
            Object r = invoke(mte, "connect", new Class<?>[]{ForgeDirection.class}, side);
            sb.append(' ').append(side).append('=').append(r);
        }
        return sb.toString();
    }

    /**
     * GregTech 5：两台 gt_test（LV 能量仓：32 V、2 A）。源机器把泥土“烧”成 128 RF/t，能量输出仓以 GT EU（32 V × 1 A）
     * 经两段 GT 锡导线送到另一台机器的能量输入仓，后者耗电（64 RF/t）把圆石烧成石头；
     * 另外对一个单独的 LV 能量输入仓注入 MV（128 V）电压，确认与上游一样超压爆炸（enable-GT-Explodes 默认开启）。
     */
    private void gregTech() {
        t.step("gt: build two gt_test machines", 20, () -> {
            World world = t.world();
            ChunkCoordinates spawn = world.getSpawnPoint();
            gtSourceCtrl = DevSelfTest.groundAbove(world, spawn.posX + 90, spawn.posZ + 40);
            gtSinkCtrl = gtSourceCtrl.add(5, 0, 0);
            CommandMMCETest.buildAt(t.sender(), world, DevSelfTest.machine(GT_TEST), gtSourceCtrl);
            CommandMMCETest.buildAt(t.sender(), world, DevSelfTest.machine(GT_TEST), gtSinkCtrl);
            for (int dx = 2; dx <= 3; dx++) {
                world.setBlock(gtSourceCtrl.getX() + dx, gtSourceCtrl.getY() - 1, gtSourceCtrl.getZ(), net.minecraft.init.Blocks.stone);
                for (int dy = 0; dy <= 2; dy++) {
                    world.setBlockToAir(gtSourceCtrl.getX() + dx, gtSourceCtrl.getY() + dy, gtSourceCtrl.getZ());
                }
            }
            ((hellfirepvp.modularmachinery.common.tiles.base.TileItemBus) DevSelfTest.tileAt(world, gtSourceCtrl.add(0, 0, 1))).getInventory()
                .setStackInSlot(0, new net.minecraft.item.ItemStack(net.minecraft.init.Blocks.dirt, 16));
            ((hellfirepvp.modularmachinery.common.tiles.base.TileItemBus) DevSelfTest.tileAt(world, gtSinkCtrl.add(0, 0, 1))).getInventory()
                .setStackInSlot(0, new net.minecraft.item.ItemStack(net.minecraft.init.Blocks.cobblestone, 16));
        });
        t.step("gt: tin cables between the output hatch and the input hatch", 10, () -> {
            World world = t.world();
            net.minecraft.item.ItemStack cable = gtTinCable();
            for (int dx = 2; dx <= 3; dx++) {
                t.placeItemAt(world, gtSourceCtrl.add(dx, 0, 0), cable.getItem(), cable.getItemDamage(), "GT tin cable (" + cable.getDisplayName() + ")");
            }
            info("cable at +2: " + gtConnect(world, gtSourceCtrl.add(2, 0, 0), ForgeDirection.WEST, ForgeDirection.EAST));
            info("cable at +3: " + gtConnect(world, gtSourceCtrl.add(3, 0, 0), ForgeDirection.WEST, ForgeDirection.EAST));
        });
        t.waitUntil("gt: GT EU through tin cables powers the second machine (2 stone made with 64 RF/t)", 1200,
            () -> countIn(gtSinkCtrl.add(0, 1, 0), net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.stone)) >= 2);
        t.step("gt: overvoltage", 0, () -> {
            World world = t.world();
            int stone = countIn(gtSinkCtrl.add(0, 1, 0), net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.stone));
            info("GregTech: stone=" + stone);
            CommandMMCETest.statusAt(t.sender(), world, DevSelfTest.machine(GT_TEST), gtSourceCtrl);
            CommandMMCETest.statusAt(t.sender(), world, DevSelfTest.machine(GT_TEST), gtSinkCtrl);
            BlockPos lone = gtSourceCtrl.add(0, 0, 6);
            world.setBlock(lone.getX(), lone.getY(), lone.getZ(), hellfirepvp.modularmachinery.common.lib.BlocksMM.energyInputHatch, 0, 3);
            TileEntity hatch = DevSelfTest.tileAt(world, lone);
            long lv = (Long) invoke(hatch, "injectEnergyUnits", new Class<?>[]{ForgeDirection.class, long.class, long.class}, ForgeDirection.UNKNOWN, 32L, 1L);
            boolean stillThere = world.getTileEntity(lone.getX(), lone.getY(), lone.getZ()) instanceof hellfirepvp.modularmachinery.common.tiles.TileEnergyInputHatch;
            long mv = (Long) invoke(hatch, "injectEnergyUnits", new Class<?>[]{ForgeDirection.class, long.class, long.class}, ForgeDirection.UNKNOWN, 128L, 1L);
            boolean exploded = !(world.getTileEntity(lone.getX(), lone.getY(), lone.getZ()) instanceof hellfirepvp.modularmachinery.common.tiles.TileEnergyInputHatch);
            info("lone LV hatch: 32 V accepted " + lv + " A (still there=" + stillThere + "), 128 V -> " + mv + " A, exploded=" + exploded);
            t.expect("gt: EU moved through GT cables (direct IEnergyConnected)", stone >= 2);
            t.expect("gt: LV hatch accepts 32 V and explodes at 128 V", lv == 1 && stillThere && exploded);
        });
    }

    // ---------------- zs-machine：用 ZenScript 创建的机器 ----------------

    static void buildScriptMachine(DevSelfTest t) {
        new DevInteropTest(t).scriptMachine();
    }

    private BlockPos zsMachineCtrl;

    /**
     * testdata/scripts/mmce_machine_test.zs 用 MachineBuilder 创建 zs_machine（物品输入 / 输出总线 + 外壳）与一条配方（铁矿 → 2 铁锭）。
     * 脚本在服务器启动时运行，方块早已注册，机器没有自己的控制器：放通用控制器并放入 zs_machine 的蓝图，结构形成后运行配方。
     */
    private void scriptMachine() {
        t.step("zs-machine: machine registered by the script", 0, () -> {
            DynamicMachine machine = DevSelfTest.machine("zs_machine");
            t.expect("zs-machine: MachineBuilder machine zs_machine is in the MachineRegistry", machine != null);
            t.expect("zs-machine: it has no dedicated controller block (registered after block registration)",
                machine != null && hellfirepvp.modularmachinery.common.block.BlockController.getControllerWithMachine(machine) == null);
            boolean recipe = false;
            if (machine != null) {
                for (hellfirepvp.modularmachinery.common.crafting.MachineRecipe r : hellfirepvp.modularmachinery.common.crafting.RecipeRegistry.getRecipesFor(machine)) {
                    recipe |= r.getRegistryName().getPath().startsWith("zs_machine_iron");
                }
            }
            t.expect("zs-machine: script recipe zs_machine_iron registered", recipe);
        });
        t.step("zs-machine: build with the universal controller and a blueprint", 40, () -> {
            DynamicMachine machine = DevSelfTest.machine("zs_machine");
            World world = t.world();
            ChunkCoordinates spawn = world.getSpawnPoint();
            zsMachineCtrl = DevSelfTest.groundAbove(world, spawn.posX - 30, spawn.posZ + 60);
            BlockPos c = zsMachineCtrl;
            world.setBlock(c.getX() - 1, c.getY(), c.getZ(), hellfirepvp.modularmachinery.common.lib.BlocksMM.itemInputBus, 0, 3);
            world.setBlock(c.getX() + 1, c.getY(), c.getZ(), hellfirepvp.modularmachinery.common.lib.BlocksMM.itemOutputBus, 0, 3);
            world.setBlock(c.getX(), c.getY() - 1, c.getZ(), hellfirepvp.modularmachinery.common.lib.BlocksMM.blockCasing, 0, 3);
            world.setBlock(c.getX(), c.getY() + 1, c.getZ(), net.minecraft.init.Blocks.air);
            world.setBlock(c.getX(), c.getY(), c.getZ(), hellfirepvp.modularmachinery.common.lib.BlocksMM.blockController,
                github.kasuminova.mmce.compat.EnumFacing.NORTH.getHorizontalIndex(), 3);
            TileEntity te = DevSelfTest.tileAt(world, c);
            net.minecraft.item.ItemStack blueprint = new net.minecraft.item.ItemStack(hellfirepvp.modularmachinery.common.lib.ItemsMM.blueprint);
            hellfirepvp.modularmachinery.common.item.ItemBlueprint.setAssociatedMachine(blueprint, machine);
            ((hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController) te).getInventory()
                .setStackInSlot(hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController.BLUEPRINT_SLOT, blueprint);
            ((hellfirepvp.modularmachinery.common.tiles.base.TileItemBus) DevSelfTest.tileAt(world, c.add(-1, 0, 0))).getInventory()
                .setStackInSlot(0, new net.minecraft.item.ItemStack(net.minecraft.init.Blocks.iron_ore, 2));
            info("universal controller " + te.getClass().getSimpleName() + " at " + c + " with a zs_machine blueprint");
        });
        t.waitUntil("zs-machine: universal controller forms zs_machine and runs the script recipe (4 iron ingots)", 600,
            () -> countIn(zsMachineCtrl.add(1, 0, 0), net.minecraft.init.Items.iron_ingot) >= 4);
        t.step("zs-machine: status", 0, () -> CommandMMCETest.statusAt(t.sender(), t.world(), DevSelfTest.machine("zs_machine"), zsMachineCtrl));
    }

    // ---------------- eio-items：EnderIO 物品导管进出物品总线 ----------------

    static void buildEnderIOItems(DevSelfTest t) {
        new DevInteropTest(t).enderIOItems();
    }

    private BlockPos eioItemCtrl;

    private void chargeAlloyFurnace() {
        TileEntity te = DevSelfTest.tileAt(t.world(), eioItemCtrl.add(0, 0, 2));
        if (te instanceof hellfirepvp.modularmachinery.common.tiles.base.TileEnergyHatch hatch) {
            hatch.setCurrentEnergy(hatch.getMaxEnergy());
        }
    }

    /**
     * 默认机器 Alloy Smelter（alloy_furnace，熔炉配方 x3）：箱子（6 个铁矿）→ EnderIO 物品导管 → 物品输入总线；
     * 物品输出总线 → EnderIO 物品导管 → 箱子。导管靠箱子 / 输出总线的一端设为抽取（INPUT），靠输入总线 / 目标箱子的一端设为送入（OUTPUT），
     * 抽取不受红石控制——与玩家用扳手和导管界面设置的效果相同。
     */
    private void enderIOItems() {
        t.step("eio-items: build alloy_furnace with chests on both sides", 40, () -> {
            World world = t.world();
            ChunkCoordinates spawn = world.getSpawnPoint();
            eioItemCtrl = DevSelfTest.groundAbove(world, spawn.posX - 90, spawn.posZ + 40);
            BlockPos c = eioItemCtrl;
            CommandMMCETest.buildAt(t.sender(), world, DevSelfTest.machine("alloy_furnace"), c);
            for (int dx : new int[]{-4, -3, -2, 2, 3, 4}) {
                world.setBlock(c.getX() + dx, c.getY() - 1, c.getZ() + 1, net.minecraft.init.Blocks.stone);
                world.setBlockToAir(c.getX() + dx, c.getY(), c.getZ() + 1);
                world.setBlockToAir(c.getX() + dx, c.getY() + 1, c.getZ() + 1);
            }
            world.setBlock(c.getX() + 4, c.getY(), c.getZ() + 1, net.minecraft.init.Blocks.chest, 0, 3);
            world.setBlock(c.getX() - 4, c.getY(), c.getZ() + 1, net.minecraft.init.Blocks.chest, 0, 3);
            ((net.minecraft.inventory.IInventory) DevSelfTest.tileAt(world, c.add(4, 0, 1)))
                .setInventorySlotContents(0, new net.minecraft.item.ItemStack(net.minecraft.init.Blocks.iron_ore, 6));
            chargeAlloyFurnace();
        });
        t.step("eio-items: EnderIO item conduits", 10, () -> {
            World world = t.world();
            for (int dx : new int[]{-3, -2, 2, 3}) {
                t.placeItemAt(world, eioItemCtrl.add(dx, 0, 1), "EnderIO", "itemItemConduit", 0);
            }
            // 输入侧：(3) 从箱子抽取，(2) 送入输入总线
            setConduitMode(world, eioItemCtrl.add(3, 0, 1), ForgeDirection.EAST, "INPUT", ITEM_CONDUIT);
            setConduitMode(world, eioItemCtrl.add(2, 0, 1), ForgeDirection.WEST, "OUTPUT", ITEM_CONDUIT);
            // 输出侧：(-2) 从输出总线抽取，(-3) 送入箱子
            setConduitMode(world, eioItemCtrl.add(-2, 0, 1), ForgeDirection.EAST, "INPUT", ITEM_CONDUIT);
            setConduitMode(world, eioItemCtrl.add(-3, 0, 1), ForgeDirection.WEST, "OUTPUT", ITEM_CONDUIT);
            info("EnderIO item conduit modes: in-side " + conduitModes(world, eioItemCtrl.add(3, 0, 1), ITEM_CONDUIT) + " | "
                + conduitModes(world, eioItemCtrl.add(2, 0, 1), ITEM_CONDUIT) + "; out-side " + conduitModes(world, eioItemCtrl.add(-2, 0, 1), ITEM_CONDUIT)
                + " | " + conduitModes(world, eioItemCtrl.add(-3, 0, 1), ITEM_CONDUIT));
        });
        t.waitUntil("eio-items: iron ore leaves the source chest through the conduits", 1200, () -> {
            chargeAlloyFurnace();
            return countChest(eioItemCtrl.add(4, 0, 1), null) == 0;
        });
        t.waitUntil("eio-items: 6 iron ingots reach the destination chest through the conduits", 2400, () -> {
            chargeAlloyFurnace();
            return countChest(eioItemCtrl.add(-4, 0, 1), net.minecraft.init.Items.iron_ingot) >= 6;
        });
        t.step("eio-items: totals", 0, () -> {
            CommandMMCETest.statusAt(t.sender(), t.world(), DevSelfTest.machine("alloy_furnace"), eioItemCtrl);
            int src = countChest(eioItemCtrl.add(4, 0, 1), null);
            int dst = countChest(eioItemCtrl.add(-4, 0, 1), net.minecraft.init.Items.iron_ingot);
            int inBus = countIn(eioItemCtrl.add(1, 0, 1), net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.iron_ore));
            int outBus = countIn(eioItemCtrl.add(-1, 0, 1), net.minecraft.init.Items.iron_ingot);
            info("eio-items: source chest=" + src + ", input bus ore=" + inBus + ", output bus ingots=" + outBus + ", destination chest ingots=" + dst);
            t.expect("eio-items: conduits fed the input bus and pulled the products out of the output bus (6 ore -> 6 ingots in the chest)",
                src == 0 && inBus == 0 && outBus == 0 && dst == 6);
        });
    }

    /** 箱子中某种物品的数量；item 为 null 时统计全部物品。 */
    private int countChest(BlockPos pos, net.minecraft.item.Item item) {
        net.minecraft.inventory.IInventory inv = (net.minecraft.inventory.IInventory) DevSelfTest.tileAt(t.world(), pos);
        int n = 0;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            net.minecraft.item.ItemStack s = inv.getStackInSlot(i);
            if (s != null && (item == null || s.getItem() == item)) {
                n += s.stackSize;
            }
        }
        return n;
    }
}
