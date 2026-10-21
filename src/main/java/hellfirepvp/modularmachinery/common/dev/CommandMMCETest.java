package hellfirepvp.modularmachinery.common.dev;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameRegistry;
import github.kasuminova.mmce.common.util.DynamicPattern;
import github.kasuminova.mmce.compat.BlockPos;
import github.kasuminova.mmce.compat.EnumFacing;
import github.kasuminova.mmce.compat.IBlockState;
import github.kasuminova.mmce.compat.ItemStacks;
import github.kasuminova.mmce.compat.ResourceLocation;
import github.kasuminova.mmce.compat.Worlds;
import github.kasuminova.mmce.compat.items.ItemHandlerHelper;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.block.BlockCasing;
import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.block.BlockMachineComponent;
import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.integration.mekanism.MekanismGasHelper;
import hellfirepvp.modularmachinery.common.item.ItemBlueprint;
import hellfirepvp.modularmachinery.common.lib.ItemsMM;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import hellfirepvp.modularmachinery.common.tiles.TileItemInputBus;
import hellfirepvp.modularmachinery.common.tiles.TileItemOutputBus;
import hellfirepvp.modularmachinery.common.tiles.TileMachineController;
import hellfirepvp.modularmachinery.common.tiles.base.TileEnergyHatch;
import hellfirepvp.modularmachinery.common.tiles.base.TileFluidTank;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import hellfirepvp.modularmachinery.common.util.BlockArrayCache;
import hellfirepvp.modularmachinery.common.util.IBlockStateDescriptor;
import net.minecraft.block.Block;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 仅在开发环境或显式开启 -Dmmce.dev.tools=true 时注册（见 ModularMachinery#serverStarting、{@link DevMode}）：按机器定义自动搭建结构、投放测试材料、报告运行状态，
 * 供客户端自动场景与人工测试使用。结果同时写入聊天、日志和运行目录下的 mmce-test-report.txt。
 * <pre>
 * /mmce-test build &lt;machine&gt;      在玩家南侧 3 格处搭建（控制器朝北，结构向南延伸；保证各仓在 Forge 的右键距离内）；
 *                                  通配的仓/总线取容量最大的一档；需要蓝图的机器在控制器中放入对应蓝图
 * /mmce-test fill &lt;machine&gt;       按测试配方投放材料、充满能量输入仓（只支持 testdata 中的测试机器）
 * /mmce-test status &lt;machine&gt;     报告形成状态、控制器朝向、动态结构尺寸、配方进度、能量与输出
 * /mmce-test break &lt;machine&gt;      拆掉结构中第一个外壳方块；/mmce-test repair 再放回
 * /mmce-test controller &lt;machine&gt; [north|east|south|west] 只放控制器（用于测试自动组装与投影旋转，默认朝北）；
 *                                  /mmce-test give &lt;machine&gt; 给玩家整套结构方块
 * /mmce-test redstone &lt;machine&gt; on|off   在控制器正面放 / 拆一个已拉下的拉杆（强充能控制器）
 * /mmce-test gas &lt;machine&gt; [气体] [数量]  经 IGasHandler 向第一个流体输入仓注入 Mekanism 气体（默认 hydrogen 1000，需要 Mekanism）
 * /mmce-test selftest &lt;persist-a|persist-b|energy|io|zs|gas|zs-gas&gt; [exit]   服务端自测（见 {@link DevSelfTest}），可在专用服务器控制台执行
 * </pre>
 */
public class CommandMMCETest extends CommandBase {

    private static final Map<String, BlockPos> BUILT = new HashMap<>();
    private static final Map<String, BlockPos> BROKEN = new HashMap<>();

    @Override
    public String getCommandName() {
        return "mmce-test";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/mmce-test <build|fill|status|break|repair|redstone|controller|give|gas> <machine> | /mmce-test selftest <persist-a|persist-b|energy|io|zs|gas|zs-gas> [exit]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 2) {
            throw new WrongUsageException(getCommandUsage(sender));
        }
        if ("selftest".equals(args[0])) {
            DevSelfTest.start(sender, args[1], args.length > 2 && "exit".equals(args[2]));
            return;
        }
        String name = args[1];
        DynamicMachine machine = MachineRegistry.getRegistry().getMachine(new ResourceLocation(ModularMachinery.MODID, name));
        if (machine == null) {
            report(sender, "FAIL unknown machine " + name);
            return;
        }
        switch (args[0]) {
            case "build" -> build(sender, machine);
            case "fill" -> fill(sender, machine);
            case "status" -> status(sender, machine);
            case "break" -> breakBlock(sender, machine);
            case "repair" -> repairBlock(sender, machine);
            case "redstone" -> redstone(sender, machine, args.length > 2 && "on".equals(args[2]));
            case "controller" -> placeController(sender, machine, args.length > 2 ? EnumFacing.byName(args[2]) : EnumFacing.NORTH,
                args.length > 3 && "factory".equals(args[3]));
            case "give" -> giveIngredients(sender, machine);
            case "gas" -> injectGas(sender, machine, args.length > 2 ? args[2] : "hydrogen", args.length > 3 ? parseInt(sender, args[3]) : 1000);
            default -> throw new WrongUsageException(getCommandUsage(sender));
        }
    }

    private static void build(ICommandSender sender, DynamicMachine machine) {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        buildAt(sender, player.worldObj, machine, new BlockPos(player.posX, player.posY, player.posZ).add(0, 1, 3));
    }

    /** 以 ctrlPos 为控制器位置搭建（控制器朝北，结构向南延伸）。 */
    static void buildAt(ICommandSender sender, World world, DynamicMachine machine, BlockPos ctrlPos) {
        BlockController controller = BlockController.getControllerWithMachine(machine);
        if (controller == null) {
            report(sender, "FAIL no controller block for " + machine.getRegistryName());
            return;
        }
        Map<BlockPos, BlockArray.BlockInformation> pattern = machine.getPattern().getPattern();
        // 先清空结构包围盒（含上方一格），再放方块
        BlockPos min = ctrlPos, max = ctrlPos;
        for (BlockPos offset : pattern.keySet()) {
            BlockPos p = ctrlPos.add(offset);
            min = new BlockPos(Math.min(min.getX(), p.getX()), Math.min(min.getY(), p.getY()), Math.min(min.getZ(), p.getZ()));
            max = new BlockPos(Math.max(max.getX(), p.getX()), Math.max(max.getY(), p.getY()), Math.max(max.getZ(), p.getZ()));
        }
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY() + 1; y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    world.setBlockToAir(x, y, z);
                }
            }
        }
        int placed = 0;
        for (Map.Entry<BlockPos, BlockArray.BlockInformation> entry : pattern.entrySet()) {
            IBlockState state = sampleState(entry.getValue());
            BlockPos p = ctrlPos.add(entry.getKey());
            Worlds.setBlockState(world, p, state);
            placed++;
        }
        // 控制器朝北（未旋转的结构定义即对应朝北）
        world.setBlock(ctrlPos.getX(), ctrlPos.getY(), ctrlPos.getZ(), controller, EnumFacing.NORTH.getHorizontalIndex(), 3);
        if (machine.isRequiresBlueprint()) {
            TileEntity te = Worlds.getTileEntity(world, ctrlPos);
            if (te instanceof TileMultiblockMachineController ctrl) {
                ItemStack blueprint = new ItemStack(ItemsMM.blueprint);
                ItemBlueprint.setAssociatedMachine(blueprint, machine);
                ctrl.getInventory().setStackInSlot(TileMultiblockMachineController.BLUEPRINT_SLOT, blueprint);
            }
        }
        BUILT.put(machine.getRegistryName().getPath(), ctrlPos);
        report(sender, "OK   build " + machine.getRegistryName() + " controller=" + ctrlPos + " blocks=" + placed);
    }

    /** 结构定义中的样例方块；MMCE 仓/总线写成通配（不带 @meta）时取最后一档（容量最大）。 */
    private static IBlockState sampleState(BlockArray.BlockInformation info) {
        List<IBlockStateDescriptor> descriptors = info.getMatchingStates();
        if (!descriptors.isEmpty() && descriptors.get(0).isWildcard()) {
            List<IBlockState> applicable = descriptors.get(0).getApplicable();
            return applicable.get(applicable.size() - 1);
        }
        return info.getSampleState(0);
    }

    private static void placeController(ICommandSender sender, DynamicMachine machine, EnumFacing facing, boolean factory) {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        World world = player.worldObj;
        net.minecraft.block.Block controller = factory
            ? hellfirepvp.modularmachinery.common.block.BlockFactoryController.FACTORY_CONTROLLERS.get(machine)
            : BlockController.getControllerWithMachine(machine);
        if (controller == null) {
            report(sender, "FAIL controller: no " + (factory ? "factory " : "") + "controller block for " + machine.getRegistryName());
            return;
        }
        if (facing == null || facing.getHorizontalIndex() < 0) {
            report(sender, "FAIL controller: bad facing");
            return;
        }
        BlockPos ctrlPos = new BlockPos(player.posX, player.posY, player.posZ).add(0, 1, 3);
        for (BlockPos offset : BlockArrayCache.getBlockArrayCache(machine.getPattern(), facing).getPattern().keySet()) {
            BlockPos p = ctrlPos.add(offset);
            world.setBlockToAir(p.getX(), p.getY(), p.getZ());
        }
        world.setBlock(ctrlPos.getX(), ctrlPos.getY(), ctrlPos.getZ(), controller, facing.getHorizontalIndex(), 3);
        BUILT.put(machine.getRegistryName().getPath(), ctrlPos);
        report(sender, "OK   controller " + machine.getRegistryName() + " controller=" + ctrlPos + " facing=" + facing.getName2()
            + (factory ? " factory" : ""));
    }

    /** 按结构方块清单（每个位置取第一种可选方块）给玩家物品。 */
    private static void giveIngredients(ICommandSender sender, DynamicMachine machine) {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        int stacks = 0;
        for (List<ItemStack> alternatives : machine.getPattern().getIngredientList()) {
            if (alternatives.isEmpty() || ItemStacks.isEmpty(alternatives.get(0))) {
                continue;
            }
            ItemStack stack = alternatives.get(0).copy();
            if (!player.inventory.addItemStackToInventory(stack)) {
                player.dropPlayerItemWithRandomChoice(stack, false);
            }
            stacks++;
        }
        player.inventoryContainer.detectAndSendChanges();
        report(sender, "OK   give " + machine.getRegistryName() + " stacks=" + stacks);
    }

    static BlockPos builtPos(DynamicMachine machine) {
        return BUILT.get(machine.getRegistryName().getPath());
    }

    static void rememberBuilt(DynamicMachine machine, BlockPos ctrlPos) {
        BUILT.put(machine.getRegistryName().getPath(), ctrlPos);
    }

    static void fill(ICommandSender sender, DynamicMachine machine) {
        BlockPos ctrlPos = BUILT.get(machine.getRegistryName().getPath());
        if (ctrlPos == null) {
            report(sender, "FAIL fill: build first");
            return;
        }
        fillAt(sender, sender.getEntityWorld(), machine, ctrlPos, true);
    }

    /** 按测试配方投料；withEnergy 为 false 时不给能量输入仓充能（用于验证外部供能链路）。 */
    static void fillAt(ICommandSender sender, World world, DynamicMachine machine, BlockPos ctrlPos, boolean withEnergy) {
        List<ItemStack> items = new ArrayList<>();
        FluidStack fluid = null;
        boolean fillEnergy = false;
        switch (machine.getRegistryName().getPath()) {
            case "mek_eio_assembler" -> {
                items.add(stack("Mekanism", "Ingot", 2, 4));
                items.add(stack("Mekanism", "EnrichedAlloy", 1, 0));
                items.add(stack("Mekanism", "ControlCircuit", 1, 0));
                fillEnergy = withEnergy;
            }
            case "fuel_generator" -> {
                items.add(stack("minecraft", "coal", 1, 0));
                fluid = new FluidStack(FluidRegistry.LAVA, 1000);
            }
            default -> {
                report(sender, "FAIL fill: no test materials for " + machine.getRegistryName());
                return;
            }
        }
        int inserted = 0;
        int energyHatches = 0;
        int tanks = 0;
        for (BlockPos offset : machine.getPattern().getPattern().keySet()) {
            TileEntity te = Worlds.getTileEntity(world, ctrlPos.add(offset));
            if (te instanceof TileItemInputBus bus) {
                for (ItemStack s : items) {
                    if (s != null && ItemStacks.isEmpty(ItemHandlerHelper.insertItem(bus.getInventory(), s.copy(), false))) {
                        inserted++;
                    }
                }
                items.clear();
            } else if (te instanceof TileEnergyHatch hatch && fillEnergy && hatch.canReceive()) {
                hatch.setCurrentEnergy(hatch.getMaxEnergy());
                energyHatches++;
            } else if (te instanceof TileFluidTank tank && fluid != null) {
                if (tank.getTank().fill(fluid, true) > 0) {
                    tanks++;
                }
                fluid = null;
            }
        }
        report(sender, "OK   fill " + machine.getRegistryName() + " stacks=" + inserted + " energyHatches=" + energyHatches + " tanks=" + tanks);
    }

    private static void injectGas(ICommandSender sender, DynamicMachine machine, String gas, int amount) {
        BlockPos ctrlPos = BUILT.get(machine.getRegistryName().getPath());
        if (ctrlPos == null) {
            report(sender, "FAIL gas: build first");
            return;
        }
        if (!Mods.MEKANISM.isPresent()) {
            report(sender, "FAIL gas: Mekanism is not loaded");
            return;
        }
        World world = sender.getEntityWorld();
        for (BlockPos offset : machine.getPattern().getPattern().keySet()) {
            TileEntity te = Worlds.getTileEntity(world, ctrlPos.add(offset));
            if (te instanceof TileFluidTank tank && tank.getIOType() == IOType.INPUT) {
                int accepted = DevGasSupport.receive(te, gas, amount);
                report(sender, "OK   gas " + machine.getRegistryName() + " " + gas + " accepted=" + accepted + " at offset " + offset);
                return;
            }
        }
        report(sender, "FAIL gas: no fluid input hatch in " + machine.getRegistryName());
    }

    private static ItemStack stack(String modId, String name, int count, int meta) {
        Item item = GameRegistry.findItem(modId, name);
        return item == null ? null : new ItemStack(item, count, meta);
    }

    static String status(ICommandSender sender, DynamicMachine machine) {
        BlockPos ctrlPos = BUILT.get(machine.getRegistryName().getPath());
        if (ctrlPos == null) {
            report(sender, "FAIL status: build first");
            return null;
        }
        return statusAt(sender, sender.getEntityWorld(), machine, ctrlPos);
    }

    static String statusAt(ICommandSender sender, World world, DynamicMachine machine, BlockPos ctrlPos) {
        TileEntity te = Worlds.getTileEntity(world, ctrlPos);
        if (!(te instanceof TileMachineController ctrl)) {
            report(sender, "FAIL status: no controller at " + ctrlPos);
            return null;
        }
        StringBuilder sb = new StringBuilder("INFO status ").append(machine.getRegistryName())
            .append(" formed=").append(ctrl.isStructureFormed())
            .append(" facing=").append(BlockController.getFacing(world.getBlockMetadata(ctrlPos.getX(), ctrlPos.getY(), ctrlPos.getZ())).getName2())
            .append(" state=").append(ctrl.getControllerStatus().getStatus())
            .append(" msg='").append(ctrl.getControllerStatus().getUnlocMessage()).append('\'');
        ActiveMachineRecipe active = ctrl.getActiveRecipe();
        if (active != null) {
            sb.append(" recipe=").append(active.getRecipe().getRegistryName()).append(" tick=").append(active.getTick()).append('/').append(active.getTotalTick());
        }
        for (DynamicPattern.Status dynamic : ctrl.getDynamicPatterns().values()) {
            sb.append(" dyn[").append(dynamic.getPatternName()).append("]=").append(dynamic.getSize());
        }
        if (!ctrl.isStructureFormed()) {
            // 未形成时给出第一个不匹配的位置（相对控制器）与该处实际方块，便于定位
            EnumFacing facing = BlockController.getFacing(world.getBlockMetadata(ctrlPos.getX(), ctrlPos.getY(), ctrlPos.getZ()));
            DynamicMachine.ModifierReplacementMap replacements = machine.getModifiersAsMatchingReplacements();
            for (EnumFacing f = EnumFacing.NORTH; f != facing; f = f.rotateYCCW()) {
                replacements = replacements.rotateYCCW();
            }
            BlockPos mismatch = BlockArrayCache.getBlockArrayCache(machine.getPattern(), facing).getRelativeMismatchPosition(world, ctrlPos, replacements);
            if (mismatch != null) {
                IBlockState found = Worlds.getBlockState(world, ctrlPos.add(mismatch));
                sb.append(" mismatch=").append(mismatch).append(" found=").append(found);
            }
            for (DynamicPattern dynamic : machine.getDynamicPatterns().values()) {
                sb.append(" dynmatch[").append(dynamic.getName()).append("]=").append(dynamic.matches(ctrl, false, facing).size());
                BlockPos start = ctrlPos.add(dynamic.getStructureSizeOffsetStart(facing));
                BlockArray first = BlockArrayCache.getBlockArrayCache(dynamic.getPattern(), facing);
                BlockPos dynMismatch = first.getRelativeMismatchPosition(world, start, null);
                if (dynMismatch != null) {
                    sb.append(" dynmismatch=").append(start.add(dynMismatch))
                        .append(" expected=").append(first.getPattern().get(dynMismatch).getMatchingStates().get(0).getApplicable())
                        .append(" found=").append(Worlds.getBlockState(world, start.add(dynMismatch)));
                }
            }
        }
        for (BlockPos offset : machine.getPattern().getPattern().keySet()) {
            TileEntity part = Worlds.getTileEntity(world, ctrlPos.add(offset));
            if (part instanceof TileEnergyHatch hatch) {
                sb.append(" energy[").append(hatch.canReceive() ? "in" : "out").append("]=").append(hatch.getCurrentEnergy()).append('/').append(hatch.getMaxEnergy());
            } else if (part instanceof TileItemOutputBus bus) {
                for (int i = 0; i < bus.getInventory().getSlots(); i++) {
                    ItemStack s = bus.getInventory().getStackInSlot(i);
                    if (!ItemStacks.isEmpty(s)) {
                        sb.append(" out=").append(s.stackSize).append('x').append(Item.itemRegistry.getNameForObject(s.getItem())).append('@').append(s.getItemDamage());
                    }
                }
            } else if (part instanceof TileItemInputBus bus) {
                int count = 0;
                for (int i = 0; i < bus.getInventory().getSlots(); i++) {
                    count += ItemStacks.getCount(bus.getInventory().getStackInSlot(i));
                }
                sb.append(" inItems=").append(count);
            } else if (part instanceof TileFluidTank tank) {
                FluidStack f = tank.getTank().getFluid();
                sb.append(" fluid=").append(f == null ? "empty" : f.amount + "mB " + f.getFluid().getName());
                // 装有 Mekanism 时流体仓也可装气体：gas[in|out]=<数量>mB <气体名>
                if (Mods.MEKANISM.isPresent()) {
                    String gas = MekanismGasHelper.gasName(tank.getTank());
                    if (gas != null) {
                        sb.append(" gas[").append(tank.getIOType() == IOType.INPUT ? "in" : "out").append("]=")
                            .append(MekanismGasHelper.gasAmount(tank.getTank())).append("mB ").append(gas);
                    }
                }
            }
        }
        report(sender, sb.toString());
        return sb.toString();
    }

    private static void redstone(ICommandSender sender, DynamicMachine machine, boolean on) {
        BlockPos ctrlPos = BUILT.get(machine.getRegistryName().getPath());
        if (ctrlPos == null) {
            report(sender, "FAIL redstone: build first");
            return;
        }
        World world = sender.getEntityWorld();
        BlockPos lever = ctrlPos.north();
        if (on) {
            // 拉杆 meta 4 = 附着在 z+1 的方块（控制器）上，+8 = 已拉下；拉下的拉杆对附着方块提供强充能
            world.setBlock(lever.getX(), lever.getY(), lever.getZ(), Blocks.lever, 4 | 8, 3);
        } else {
            world.setBlockToAir(lever.getX(), lever.getY(), lever.getZ());
        }
        report(sender, "OK   redstone " + machine.getRegistryName() + " " + (on ? "on" : "off") + " power=" + world.getBlockPowerInput(ctrlPos.getX(), ctrlPos.getY(), ctrlPos.getZ()));
    }

    private static void breakBlock(ICommandSender sender, DynamicMachine machine) {
        BlockPos ctrlPos = BUILT.get(machine.getRegistryName().getPath());
        if (ctrlPos == null) {
            report(sender, "FAIL break: build first");
            return;
        }
        World world = sender.getEntityWorld();
        for (BlockPos offset : machine.getPattern().getPattern().keySet()) {
            BlockPos p = ctrlPos.add(offset);
            Block block = world.getBlock(p.getX(), p.getY(), p.getZ());
            if (block instanceof BlockCasing || block != Blocks.air && !(block instanceof BlockMachineComponent)) {
                BROKEN.put(machine.getRegistryName().getPath(), offset);
                world.setBlockToAir(p.getX(), p.getY(), p.getZ());
                report(sender, "OK   break " + machine.getRegistryName() + " at offset " + offset);
                return;
            }
        }
        report(sender, "FAIL break: no casing block found");
    }

    private static void repairBlock(ICommandSender sender, DynamicMachine machine) {
        String key = machine.getRegistryName().getPath();
        BlockPos ctrlPos = BUILT.get(key);
        BlockPos offset = BROKEN.remove(key);
        if (ctrlPos == null || offset == null) {
            report(sender, "FAIL repair: nothing broken");
            return;
        }
        IBlockState state = sampleState(machine.getPattern().getPattern().get(offset));
        Worlds.setBlockState(sender.getEntityWorld(), ctrlPos.add(offset), state);
        report(sender, "OK   repair " + machine.getRegistryName() + " at offset " + offset);
    }

    static void report(ICommandSender sender, String line) {
        sender.addChatMessage(new ChatComponentText(line));
        ModularMachinery.log.info("[mmce-test] {}", line);
        File out = new File(Loader.instance().getConfigDir().getParentFile(), "mmce-test-report.txt");
        try (Writer w = new OutputStreamWriter(Files.newOutputStream(out.toPath(), StandardOpenOption.CREATE, StandardOpenOption.APPEND), StandardCharsets.UTF_8)) {
            w.write(line);
            w.write(System.lineSeparator());
        } catch (IOException e) {
            ModularMachinery.log.error("[mmce-test] Failed to write " + out, e);
        }
    }
}
