package hellfirepvp.modularmachinery.common.integration.waila;

import cpw.mods.fml.common.event.FMLInterModComms;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.client.util.EnergyDisplayUtil;
import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.block.BlockEnergyHatch;
import hellfirepvp.modularmachinery.common.block.BlockFluidHatch;
import hellfirepvp.modularmachinery.common.block.BlockParallelController;
import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.integration.mekanism.MekanismGasHelper;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.tiles.TileFactoryController;
import hellfirepvp.modularmachinery.common.tiles.TileMachineController;
import hellfirepvp.modularmachinery.common.tiles.TileParallelController;
import hellfirepvp.modularmachinery.common.tiles.base.TileEnergyHatch;
import hellfirepvp.modularmachinery.common.tiles.base.TileFluidTank;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import mcp.mobius.waila.api.IWailaDataProvider;
import mcp.mobius.waila.api.IWailaRegistrar;
import net.minecraft.util.StatCollector;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;

/**
 * 1.7.10 移植：1.12.2 用 TheOneProbe（MMInfoProvider）在准星处显示机器信息，1.7.10 改为 Waila。
 * 服务端在 getNBTData 中写入控制器形成状态 / 运行状态 / 配方进度 / 并行数，能量仓与流体仓的存量；客户端按这些数据显示。
 * 本类只会通过 Waila 的 IMC 回调加载，未安装 Waila 时不会被加载。
 */
public class ModIntegrationWaila implements IWailaDataProvider {

    private static final ModIntegrationWaila INSTANCE = new ModIntegrationWaila();

    /** 在 init 阶段调用（只在 Waila 已加载时）。 */
    public static void sendIMC() {
        FMLInterModComms.sendMessage("Waila", "register", ModIntegrationWaila.class.getName() + ".callbackRegister");
    }

    public static void callbackRegister(IWailaRegistrar registrar) {
        for (Class<?> block : new Class<?>[]{BlockController.class, BlockEnergyHatch.class, BlockFluidHatch.class, BlockParallelController.class}) {
            registrar.registerBodyProvider(INSTANCE, block);
            registrar.registerNBTProvider(INSTANCE, block);
        }
        ModularMachinery.log.info("[ModularMachinery] Waila providers registered.");
    }

    // ---------------- 服务端：同步数据 ----------------

    @Override
    public NBTTagCompound getNBTData(EntityPlayerMP player, TileEntity te, NBTTagCompound tag, World world, int x, int y, int z) {
        if (te instanceof TileMultiblockMachineController ctrl) {
            DynamicMachine found = ctrl.getFoundMachine();
            tag.setBoolean("mmFormed", ctrl.isStructureFormed());
            if (found != null) {
                tag.setString("mmMachine", found.getRegistryName().toString());
                tag.setString("mmMachineName", found.getLocalizedName());
            }
            tag.setString("mmStatusType", ctrl.getControllerStatus().getStatus().name());
            tag.setString("mmStatus", ctrl.getControllerStatus().getUnlocMessage());
            if (te instanceof TileMachineController machine) {
                ActiveMachineRecipe active = machine.getActiveRecipe();
                if (active != null) {
                    tag.setInteger("mmTick", active.getTick());
                    tag.setInteger("mmTotal", active.getTotalTick());
                    tag.setInteger("mmParallel", active.getParallelism());
                }
            } else if (te instanceof TileFactoryController factory) {
                tag.setInteger("mmThreads", factory.getFactoryRecipeThreadList().size());
                tag.setInteger("mmMaxThreads", factory.getMaxThreads());
            }
        } else if (te instanceof TileEnergyHatch hatch) {
            tag.setLong("mmEnergy", hatch.getCurrentEnergy());
            tag.setLong("mmMaxEnergy", hatch.getMaxEnergy());
        } else if (te instanceof TileFluidTank tank) {
            FluidStack fluid = tank.getTank().getFluid();
            if (fluid != null && fluid.getFluid() != null) {
                tag.setString("mmFluid", fluid.getFluid().getLocalizedName(fluid));
                tag.setInteger("mmFluidAmount", fluid.amount);
            } else if (Mods.MEKANISM.isPresent()) {
                // 装有 Mekanism 时流体仓也可装气体；发送未本地化名，由客户端本地化
                String gas = MekanismGasHelper.gasUnlocalizedName(tank.getTank());
                if (gas != null) {
                    tag.setString("mmGas", gas);
                    tag.setInteger("mmGasAmount", MekanismGasHelper.gasAmount(tank.getTank()));
                }
            }
            tag.setInteger("mmFluidCapacity", tank.getTank().getCapacity());
        } else if (te instanceof TileParallelController parallel) {
            tag.setInteger("mmParallelism", parallel.provideComponent().getParallelism());
            tag.setInteger("mmMaxParallelism", parallel.provideComponent().getMaxParallelism());
        }
        return tag;
    }

    // ---------------- 客户端：显示 ----------------

    @Override
    public List<String> getWailaBody(ItemStack itemStack, List<String> tip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
        NBTTagCompound tag = accessor.getNBTData();
        if (tag == null) {
            return tip;
        }
        if (tag.hasKey("mmFormed")) {
            if (tag.getBoolean("mmFormed")) {
                tip.add(EnumChatFormatting.GREEN + StatCollector.translateToLocal("top.machine.structure.found")
                    + (tag.hasKey("mmMachineName") ? EnumChatFormatting.WHITE + ": " + tag.getString("mmMachineName") : ""));
            } else {
                tip.add(EnumChatFormatting.RED + StatCollector.translateToLocal("top.machine.structure.none"));
            }
            String status = tag.getString("mmStatus");
            if (!status.isEmpty()) {
                tip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal(status));
            }
            if (tag.hasKey("mmTotal")) {
                int tick = tag.getInteger("mmTick");
                int total = Math.max(1, tag.getInteger("mmTotal"));
                tip.add(EnumChatFormatting.AQUA + StatCollector.translateToLocal("top.recipe.progress") + ": " + EnumChatFormatting.WHITE
                    + (tick * 100 / total) + "% (" + tick + " / " + total + " t)");
                if (tag.getInteger("mmParallel") > 1) {
                    tip.add(EnumChatFormatting.AQUA + StatCollector.translateToLocal("top.parallelism") + " " + EnumChatFormatting.GREEN + tag.getInteger("mmParallel"));
                }
            }
            if (tag.hasKey("mmMaxThreads")) {
                tip.add(EnumChatFormatting.GREEN + String.valueOf(tag.getInteger("mmThreads")) + EnumChatFormatting.AQUA + " "
                    + StatCollector.translateToLocal("top.factory.thread.running") + EnumChatFormatting.RESET + " / " + EnumChatFormatting.YELLOW
                    + tag.getInteger("mmMaxThreads") + EnumChatFormatting.GOLD + " " + StatCollector.translateToLocal("top.factory.thread.max"));
            }
        } else if (tag.hasKey("mmMaxEnergy")) {
            // 带单位；按住 Shift 显示完整数值（Waila 的提示在客户端绘制）
            long energy = tag.getLong("mmEnergy");
            long max = tag.getLong("mmMaxEnergy");
            tip.add(EnumChatFormatting.GOLD + EnergyDisplayUtil.formatEnergy(energy)
                + EnumChatFormatting.GRAY + " / " + EnergyDisplayUtil.formatEnergy(max) + " " + EnergyDisplayUtil.unit());
            if (EnergyDisplayUtil.needsShiftHint(energy, max)) {
                tip.add(EnergyDisplayUtil.shiftHint());
            }
        } else if (tag.hasKey("mmFluidCapacity")) {
            if (tag.hasKey("mmFluid")) {
                tip.add(EnumChatFormatting.AQUA + tag.getString("mmFluid") + EnumChatFormatting.GRAY + ": "
                    + tag.getInteger("mmFluidAmount") + " / " + tag.getInteger("mmFluidCapacity") + " mB");
            } else if (tag.hasKey("mmGas")) {
                tip.add(EnumChatFormatting.AQUA + StatCollector.translateToLocal("tooltip.fluidhatch.gas") + " "
                    + StatCollector.translateToLocal(tag.getString("mmGas")) + EnumChatFormatting.GRAY + ": "
                    + tag.getInteger("mmGasAmount") + " / " + tag.getInteger("mmFluidCapacity") + " mB");
            } else {
                tip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("tooltip.fluidhatch.empty") + " (0 / " + tag.getInteger("mmFluidCapacity") + " mB)");
            }
        } else if (tag.hasKey("mmMaxParallelism")) {
            tip.add(EnumChatFormatting.AQUA + StatCollector.translateToLocal("top.parallelism") + " " + EnumChatFormatting.GREEN + tag.getInteger("mmParallelism"));
            tip.add(EnumChatFormatting.GOLD + StatCollector.translateToLocal("top.max_parallelism") + " " + EnumChatFormatting.YELLOW + tag.getInteger("mmMaxParallelism"));
        }
        return tip;
    }

    @Override
    public ItemStack getWailaStack(IWailaDataAccessor accessor, IWailaConfigHandler config) {
        return null;
    }

    @Override
    public List<String> getWailaHead(ItemStack itemStack, List<String> tip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
        return tip;
    }

    @Override
    public List<String> getWailaTail(ItemStack itemStack, List<String> tip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
        return tip;
    }
}
