package github.kasuminova.mmce.compat.fluids;

import net.minecraftforge.fluids.FluidStack;

/**
 * 1.7.10 移植适配：对应 Forge 1.12.2 net.minecraftforge.fluids.capability.IFluidTankProperties。
 */
public interface IFluidTankProperties {

    FluidStack getContents();

    int getCapacity();

    boolean canFill();

    boolean canDrain();

    boolean canFillFluidType(FluidStack fluidStack);

    boolean canDrainFluidType(FluidStack fluidStack);
}
