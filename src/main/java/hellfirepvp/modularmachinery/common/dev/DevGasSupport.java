package hellfirepvp.modularmachinery.common.dev;

import github.kasuminova.mmce.compat.BlockPos;
import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementGas;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementGasPerTick;
import hellfirepvp.modularmachinery.common.integration.mekanism.MekanismGasHelper;
import hellfirepvp.modularmachinery.common.tiles.base.TileFluidTank;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasRegistry;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.IGasHandler;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.lang.reflect.Field;
import java.util.List;

/**
 * 仅开发环境：服务端气体自测（{@link DevSelfTest} 的 gas 阶段）中用到 Mekanism 类型的部分，单独成类，只在装有 Mekanism 时加载。
 * Mekanism 的加压管道是 FMP 部件，其内部类型不在编译期依赖中，用反射访问。
 */
final class DevGasSupport {

    private DevGasSupport() {
    }

    private static Gas gas(String name) {
        Gas gas = GasRegistry.getGas(name);
        if (gas == null) {
            throw new IllegalStateException("gas not registered: " + name);
        }
        return gas;
    }

    /** 通过 TileEntity 的 IGasHandler 接口（与 Mekanism 管道相同的入口）送入气体，返回接收量。 */
    static int receive(TileEntity te, String gasName, int amount) {
        if (!(te instanceof IGasHandler handler)) {
            throw new IllegalStateException(te + " is not an IGasHandler");
        }
        return handler.receiveGas(ForgeDirection.UP, new GasStack(gas(gasName), amount), true);
    }

    static boolean canReceive(TileEntity te, String gasName) {
        return te instanceof IGasHandler handler && handler.canReceiveGas(ForgeDirection.UP, gas(gasName));
    }

    /** 通过 IGasHandler 抽取（模拟），返回可抽取量。 */
    static int simulateDraw(TileEntity te, int amount) {
        if (!(te instanceof IGasHandler handler)) {
            return -1;
        }
        GasStack drawn = handler.drawGas(ForgeDirection.UP, amount, false);
        return drawn == null ? 0 : drawn.amount;
    }

    /** 流体仓中储存的气体量（不经过 IGasHandler：输入仓不允许外部抽取）。 */
    static int storedGas(TileEntity te) {
        if (!(te instanceof TileFluidTank tank) || MekanismGasHelper.gasName(tank.getTank()) == null) {
            return 0;
        }
        return MekanismGasHelper.gasAmount(tank.getTank());
    }

    static String describe(TileEntity te) {
        if (!(te instanceof TileFluidTank tank)) {
            return "not a fluid hatch";
        }
        String name = MekanismGasHelper.gasName(tank.getTank());
        return name == null ? "none" : MekanismGasHelper.gasAmount(tank.getTank()) + "mB " + name;
    }

    /** 把流体仓写入 NBT 再读成新的 TileEntity，返回 NBT 中的气体标签与读回后的气体（验证存档格式）。 */
    static String nbtRoundTrip(TileEntity te) {
        NBTTagCompound tag = new NBTTagCompound();
        te.writeToNBT(tag);
        NBTTagCompound gasTag = tag.getCompoundTag("tank").getCompoundTag("gasTag");
        TileEntity reloaded = TileEntity.createAndLoadEntity(tag);
        return "nbt[gasName=" + gasTag.getString("gasName") + " amount=" + gasTag.getInteger("amount") + "] reloaded=" + describe(reloaded);
    }

    /** 配方中各需求的简述（需求类型 io，气体需求附带“气体 x数量”），用于核对脚本方法生成的需求。 */
    static String describeRequirements(MachineRecipe recipe) {
        StringBuilder sb = new StringBuilder();
        for (ComponentRequirement<?, ?> req : recipe.getCraftingRequirements()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(req.getRequirementType().getRegistryName().getPath()).append(' ').append(req.getActionType().name().toLowerCase());
            GasStack gas = req instanceof RequirementGas g ? g.required : req instanceof RequirementGasPerTick t ? t.required : null;
            if (gas != null) {
                sb.append(' ').append(gas.getGas().getName()).append(" x").append(gas.amount);
            }
        }
        return sb.toString();
    }

    /** 把 pos 处 Mekanism 管道（FMP 部件）朝 side 的连接方式设为 PULL，与配置器潜行右键该面的效果相同。 */
    static void setTubePull(World world, BlockPos pos, ForgeDirection side) {
        TileEntity te = world.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
        if (te == null) {
            throw new IllegalStateException("no multipart tile at " + pos);
        }
        try {
            List<?> parts = (List<?>) te.getClass().getMethod("jPartList").invoke(te);
            for (Object part : parts) {
                Class<?> pipe = part.getClass();
                while (pipe != null && !pipe.getName().equals("mekanism.common.multipart.PartSidedPipe")) {
                    pipe = pipe.getSuperclass();
                }
                if (pipe == null) {
                    continue;
                }
                Field types = pipe.getField("connectionTypes");
                Object[] array = (Object[]) types.get(part);
                @SuppressWarnings({"unchecked", "rawtypes"})
                Object pull = Enum.valueOf((Class) array.getClass().getComponentType(), "PULL");
                // 配置器还会调用 onModeChange（只标记该面的接收方需要刷新）；这里不反射调用方法：
                // 在专用服务器上 getDeclaredMethod 会解析部件中仅客户端存在的参数类型（EffectRenderer）而抛 NoClassDefFoundError。
                // 管道每 tick 按 connectionTypes 中的 PULL 面抽取，只写字段即可生效。
                array[side.ordinal()] = pull;
                pipe.getField("sendDesc").setBoolean(part, true);
                return;
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot configure tube at " + pos, e);
        }
        throw new IllegalStateException("no Mekanism transmitter part at " + pos);
    }
}
