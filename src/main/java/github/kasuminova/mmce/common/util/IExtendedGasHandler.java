package github.kasuminova.mmce.common.util;

import mekanism.api.gas.GasStack;
import mekanism.api.gas.IGasHandler;
import mekanism.api.gas.IGasTankInfoProvider;

/**
 * 1.7.10 移植：Mekanism 1.7.10 的 IGasHandler 不含 getTankInfo（拆到了 IGasTankInfoProvider），因此这里一并继承。
 * 只在装有 Mekanism 时被加载。
 */
public interface IExtendedGasHandler extends IGasHandler, IGasTankInfoProvider {

    GasStack drawGas(GasStack toDraw, boolean doTransfer);

}
