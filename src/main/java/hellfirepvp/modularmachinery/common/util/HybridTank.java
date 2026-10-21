/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.util;

import github.kasuminova.mmce.common.util.concurrent.ReadWriteLockProvider;
import github.kasuminova.mmce.compat.fluids.FluidTankProperties;
import github.kasuminova.mmce.compat.fluids.IFluidHandler;
import github.kasuminova.mmce.compat.fluids.IFluidTankProperties;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidTank;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * MMCE 的线程安全流体储罐。
 * <p>
 * 1.7.10 移植：1.12.2 版本继承 Forge 1.12.2 的 FluidTank 并依赖其 canFill/canDrain、fillInternal/drainInternal、
 * drain(FluidStack)、onContentsChanged 等行为；1.7.10 的 FluidTank 不具备这些，因此在此完整实现同等语义：
 * - fill/drain 遵守 canFill/canDrain 开关；fillInternal/drainInternal 忽略开关（供机器内部使用）。
 * - 实现 1.7.10 的 IFluidTank（对外储罐信息）与适配层的 {@link IFluidHandler}（MMCE 内部逻辑）。
 * - NBT 格式与 Forge FluidTank 相同（FluidStack 写入根标签，空罐写 "Empty"），容量不写入 NBT。
 */
public class HybridTank implements IFluidTank, IFluidHandler, ReadWriteLockProvider {
    protected final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    @Nullable
    protected FluidStack fluid;
    protected int capacity;
    protected boolean canFill = true;
    protected boolean canDrain = true;

    public HybridTank(int capacity) {
        this.capacity = capacity;
    }

    // ---------------- 状态 ----------------

    @Nullable
    @Override
    public FluidStack getFluid() {
        try {
            rwLock.readLock().lock();
            return fluid;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void setFluid(@Nullable FluidStack fluid) {
        try {
            rwLock.writeLock().lock();
            this.fluid = fluid;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public int getFluidAmount() {
        try {
            rwLock.readLock().lock();
            return fluid == null ? 0 : fluid.amount;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public FluidTankInfo getInfo() {
        FluidStack contents = getFluid();
        return new FluidTankInfo(contents == null ? null : contents.copy(), capacity);
    }

    public boolean canFill() {
        return canFill;
    }

    public boolean canDrain() {
        return canDrain;
    }

    public void setCanFill(boolean canFill) {
        this.canFill = canFill;
    }

    public void setCanDrain(boolean canDrain) {
        this.canDrain = canDrain;
    }

    /** 储罐当前是否装着 Mekanism 气体（见 HybridGasTank）；装着气体时不能再放入流体。 */
    public boolean isHoldingGas() {
        return false;
    }

    public boolean canFillFluidType(FluidStack fluid) {
        return canFill();
    }

    public boolean canDrainFluidType(@Nullable FluidStack fluid) {
        return fluid != null && canDrain();
    }

    @Override
    public IFluidTankProperties[] getTankProperties() {
        FluidStack contents = getFluid();
        return new IFluidTankProperties[]{new FluidTankProperties(contents, capacity, canFill, canDrain)};
    }

    /** 内容变化回调（对应 1.12.2 FluidTank#onContentsChanged）。 */
    protected void onContentsChanged() {
    }

    // ---------------- 填充 ----------------

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        if (!canFillFluidType(resource)) {
            return 0;
        }
        return fillInternal(resource, doFill);
    }

    public int fillInternal(FluidStack resource, boolean doFill) {
        if (resource == null || resource.amount <= 0) {
            return 0;
        }
        try {
            (doFill ? rwLock.writeLock() : rwLock.readLock()).lock();
            if (!doFill) {
                if (fluid == null) {
                    return Math.min(capacity, resource.amount);
                }
                if (!fluid.isFluidEqual(resource)) {
                    return 0;
                }
                return Math.min(capacity - fluid.amount, resource.amount);
            }

            if (fluid == null) {
                fluid = new FluidStack(resource, Math.min(capacity, resource.amount));
                onContentsChanged();
                return fluid.amount;
            }
            if (!fluid.isFluidEqual(resource)) {
                return 0;
            }
            int filled = capacity - fluid.amount;
            if (resource.amount < filled) {
                fluid.amount += resource.amount;
                filled = resource.amount;
            } else {
                fluid.amount = capacity;
            }
            if (filled > 0) {
                onContentsChanged();
            }
            return filled;
        } finally {
            (doFill ? rwLock.writeLock() : rwLock.readLock()).unlock();
        }
    }

    // ---------------- 抽取 ----------------

    @Override
    @Nullable
    public FluidStack drain(FluidStack resource, boolean doDrain) {
        if (!canDrainFluidType(getFluid())) {
            return null;
        }
        return drainInternal(resource, doDrain);
    }

    @Override
    @Nullable
    public FluidStack drain(int maxDrain, boolean doDrain) {
        if (!canDrainFluidType(getFluid())) {
            return null;
        }
        return drainInternal(maxDrain, doDrain);
    }

    @Nullable
    public FluidStack drainInternal(FluidStack resource, boolean doDrain) {
        if (resource == null) {
            return null;
        }
        FluidStack current = getFluid();
        if (current == null || !resource.isFluidEqual(current)) {
            return null;
        }
        return drainInternal(resource.amount, doDrain);
    }

    @Nullable
    public FluidStack drainInternal(int maxDrain, boolean doDrain) {
        try {
            (doDrain ? rwLock.writeLock() : rwLock.readLock()).lock();
            if (fluid == null || maxDrain <= 0) {
                return null;
            }
            int drained = Math.min(maxDrain, fluid.amount);
            FluidStack stack = new FluidStack(fluid, drained);
            if (doDrain) {
                fluid.amount -= drained;
                if (fluid.amount <= 0) {
                    fluid = null;
                }
                if (drained > 0) {
                    onContentsChanged();
                }
            }
            return stack;
        } finally {
            (doDrain ? rwLock.writeLock() : rwLock.readLock()).unlock();
        }
    }

    // ---------------- NBT ----------------

    public HybridTank readFromNBT(NBTTagCompound nbt) {
        if (!nbt.hasKey("Empty")) {
            setFluid(FluidStack.loadFluidStackFromNBT(nbt));
        } else {
            setFluid(null);
        }
        return this;
    }

    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        FluidStack current = getFluid();
        if (current != null) {
            current.writeToNBT(nbt);
        } else {
            nbt.setString("Empty", "");
        }
        return nbt;
    }

    @Nonnull
    @Override
    public ReadWriteLock getRWLock() {
        return rwLock;
    }
}
