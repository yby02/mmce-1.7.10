package github.kasuminova.mmce.common.util;

import github.kasuminova.mmce.compat.fluids.FluidTankProperties;
import github.kasuminova.mmce.compat.fluids.IFluidHandler;
import github.kasuminova.mmce.compat.fluids.IFluidTankProperties;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.IntConsumer;

/**
 * 固定格数、每格同一容量的流体储罐组（ME 流体输入 / 输出总线的内部储罐）。
 * <p>
 * 1.7.10 移植：上游使用 AE2 1.12 的 AEFluidInventory / IAEFluidTank，GTNH 的 AE2 rv3 中没有这些类，这里按同样的行为实现
 * 移植版的 {@link IFluidHandler}：注入时优先放进已有同种流体的格，其次是空格；抽取时按格顺序取。
 */
public class FluidTankArray implements IFluidHandler {

    private final FluidStack[]   tanks;
    private final ReadWriteLock  rwLock = new ReentrantReadWriteLock();
    private       int            capacity;
    private       IntConsumer    listener;

    public FluidTankArray(int slots, int capacity) {
        this.tanks = new FluidStack[slots];
        this.capacity = capacity;
    }

    public void setListener(IntConsumer listener) {
        this.listener = listener;
    }

    private void changed(int slot) {
        IntConsumer l = listener;
        if (l != null) {
            l.accept(slot);
        }
    }

    public ReadWriteLock getRWLock() {
        return rwLock;
    }

    public int getSlots() {
        return tanks.length;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public FluidStack getFluidInSlot(int slot) {
        return tanks[slot];
    }

    public void setFluidInSlot(int slot, FluidStack stack) {
        tanks[slot] = stack == null || stack.amount <= 0 ? null : stack;
        changed(slot);
    }

    public boolean isEmpty() {
        for (FluidStack stack : tanks) {
            if (stack != null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public IFluidTankProperties[] getTankProperties() {
        IFluidTankProperties[] props = new IFluidTankProperties[tanks.length];
        for (int i = 0; i < tanks.length; i++) {
            props[i] = new FluidTankProperties(tanks[i], capacity);
        }
        return props;
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        if (resource == null || resource.amount <= 0) {
            return 0;
        }
        int filled = 0;
        // 先放进已有同种流体的格，再放进空格
        for (int pass = 0; pass < 2 && filled < resource.amount; pass++) {
            for (int i = 0; i < tanks.length && filled < resource.amount; i++) {
                FluidStack inSlot = tanks[i];
                boolean match = pass == 0 ? inSlot != null && inSlot.isFluidEqual(resource) : inSlot == null;
                if (!match) {
                    continue;
                }
                int current = inSlot == null ? 0 : inSlot.amount;
                int toFill = Math.min(resource.amount - filled, capacity - current);
                if (toFill <= 0) {
                    continue;
                }
                if (doFill) {
                    tanks[i] = new FluidStack(resource, current + toFill);
                    changed(i);
                }
                filled += toFill;
            }
        }
        return filled;
    }

    @Override
    public FluidStack drain(FluidStack resource, boolean doDrain) {
        if (resource == null || resource.amount <= 0) {
            return null;
        }
        int drained = 0;
        for (int i = 0; i < tanks.length && drained < resource.amount; i++) {
            FluidStack inSlot = tanks[i];
            if (inSlot == null || !inSlot.isFluidEqual(resource)) {
                continue;
            }
            int toDrain = Math.min(resource.amount - drained, inSlot.amount);
            if (doDrain) {
                int left = inSlot.amount - toDrain;
                tanks[i] = left <= 0 ? null : new FluidStack(inSlot, left);
                changed(i);
            }
            drained += toDrain;
        }
        return drained <= 0 ? null : new FluidStack(resource, drained);
    }

    @Override
    public FluidStack drain(int maxDrain, boolean doDrain) {
        for (FluidStack inSlot : tanks) {
            if (inSlot != null) {
                return drain(new FluidStack(inSlot, maxDrain), doDrain);
            }
        }
        return null;
    }

    public void writeToNBT(NBTTagCompound tag, String name) {
        NBTTagCompound sub = new NBTTagCompound();
        for (int i = 0; i < tanks.length; i++) {
            if (tanks[i] != null) {
                sub.setTag("#" + i, tanks[i].writeToNBT(new NBTTagCompound()));
            }
        }
        tag.setTag(name, sub);
    }

    public void readFromNBT(NBTTagCompound tag, String name) {
        NBTTagCompound sub = tag.getCompoundTag(name);
        for (int i = 0; i < tanks.length; i++) {
            tanks[i] = sub.hasKey("#" + i) ? FluidStack.loadFluidStackFromNBT(sub.getCompoundTag("#" + i)) : null;
        }
    }
}
