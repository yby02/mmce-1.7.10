package hellfirepvp.modularmachinery.common.integration.mekanism;

import hellfirepvp.modularmachinery.common.util.HybridGasTank;
import hellfirepvp.modularmachinery.common.util.HybridTank;
import mekanism.api.gas.GasStack;

import javax.annotation.Nullable;

/**
 * 读取流体仓中 Mekanism 气体的辅助方法。所有 Mekanism 类型都集中在本类中，调用方必须先检查 {@code Mods.MEKANISM.isPresent()}，
 * 这样未装 Mekanism 时本类不会被加载。
 */
public final class MekanismGasHelper {

    private MekanismGasHelper() {
    }

    @Nullable
    public static GasStack gasOf(HybridTank tank) {
        if (tank instanceof HybridGasTank gasTank) {
            GasStack gas = gasTank.getGas();
            return gas == null || gas.amount <= 0 || gas.getGas() == null ? null : gas;
        }
        return null;
    }

    /** 气体名（Mekanism 注册名，如 hydrogen）；储罐没有气体时返回 null。 */
    @Nullable
    public static String gasName(HybridTank tank) {
        GasStack gas = gasOf(tank);
        return gas == null ? null : gas.getGas().getName();
    }

    /** 气体的未本地化名（如 gas.hydrogen），由客户端本地化；储罐没有气体时返回 null。 */
    @Nullable
    public static String gasUnlocalizedName(HybridTank tank) {
        GasStack gas = gasOf(tank);
        return gas == null ? null : gas.getGas().getUnlocalizedName();
    }

    public static int gasAmount(HybridTank tank) {
        GasStack gas = gasOf(tank);
        return gas == null ? 0 : gas.amount;
    }
}
