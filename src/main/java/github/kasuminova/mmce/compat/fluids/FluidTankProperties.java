package github.kasuminova.mmce.compat.fluids;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;

/**
 * 1.7.10 移植适配：对应 Forge 1.12.2 net.minecraftforge.fluids.capability.FluidTankProperties。
 */
public class FluidTankProperties implements IFluidTankProperties {

    private final FluidStack contents;
    private final int capacity;
    private final boolean canFill;
    private final boolean canDrain;

    public FluidTankProperties(FluidStack contents, int capacity) {
        this(contents, capacity, true, true);
    }

    public FluidTankProperties(FluidStack contents, int capacity, boolean canFill, boolean canDrain) {
        this.contents = contents;
        this.capacity = capacity;
        this.canFill = canFill;
        this.canDrain = canDrain;
    }

    public static FluidTankProperties[] convert(FluidTankInfo[] infos) {
        FluidTankProperties[] properties = new FluidTankProperties[infos.length];
        for (int i = 0; i < infos.length; i++) {
            properties[i] = new FluidTankProperties(infos[i].fluid, infos[i].capacity);
        }
        return properties;
    }

    /** 转换为 1.7.10 的 FluidTankInfo，供实现 net.minecraftforge.fluids.IFluidHandler#getTankInfo 使用。 */
    public static FluidTankInfo[] toTankInfo(IFluidTankProperties[] properties) {
        FluidTankInfo[] infos = new FluidTankInfo[properties.length];
        for (int i = 0; i < properties.length; i++) {
            FluidStack contents = properties[i].getContents();
            infos[i] = new FluidTankInfo(contents == null ? null : contents.copy(), properties[i].getCapacity());
        }
        return infos;
    }

    @Override
    public FluidStack getContents() {
        return contents == null ? null : contents.copy();
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public boolean canFill() {
        return canFill;
    }

    @Override
    public boolean canDrain() {
        return canDrain;
    }

    @Override
    public boolean canFillFluidType(FluidStack fluidStack) {
        return canFill;
    }

    @Override
    public boolean canDrainFluidType(FluidStack fluidStack) {
        return canDrain;
    }
}
