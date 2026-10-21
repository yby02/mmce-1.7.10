package github.kasuminova.mmce.compat.fluids;

import net.minecraftforge.fluids.FluidStack;

/**
 * 1.7.10 移植适配：与 Forge 1.12.2 net.minecraftforge.fluids.capability.IFluidHandler 相同的接口形状（无方向参数），
 * 供 MMCE 内部流体逻辑使用。对外由 TileEntity 实现 1.7.10 的 net.minecraftforge.fluids.IFluidHandler 并委托到本接口。
 */
public interface IFluidHandler {

    IFluidTankProperties[] getTankProperties();

    int fill(FluidStack resource, boolean doFill);

    FluidStack drain(FluidStack resource, boolean doDrain);

    FluidStack drain(int maxDrain, boolean doDrain);
}
