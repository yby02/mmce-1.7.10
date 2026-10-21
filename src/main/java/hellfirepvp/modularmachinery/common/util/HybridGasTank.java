/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.util;

import github.kasuminova.mmce.common.util.IExtendedGasHandler;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.GasTank;
import mekanism.api.gas.GasTankInfo;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: HybridGasTank
 * Created by HellFirePvP
 * Date: 26.08.2017 / 19:03
 * <p>
 * 流体 / 气体二选一的储罐：装有 Mekanism 时流体仓使用它（见 FluidHatchSize#buildTank），只在装有 Mekanism 时被加载。
 * <p>
 * 1.7.10 移植：
 * - 方向参数改为 ForgeDirection；补上 Mekanism 1.7.10 IGasHandler 中已弃用但仍需实现的两参数 receiveGas / drawGas。
 * - 修正上游 drawGas 只在模拟（doTransfer=false）时触发 onContentsChanged 的错误：实际抽取后才需要同步与保存。
 * - canReceiveGas 在储罐装有流体时返回 false（上游返回 true 但 receiveGas 恒为 0，会让管道网络把它算作接收方）。
 */
public class HybridGasTank extends HybridTank implements IExtendedGasHandler {
    protected final GasTank gasTank;

    public HybridGasTank(int capacity) {
        super(capacity);
        gasTank = new GasTank(capacity);
    }

    @Override
    public void setFluid(@Nullable FluidStack fluid) {
        try {
            rwLock.writeLock().lock();
            super.setFluid(fluid);
            if (getFluid() != null) {
                setGas(null);
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public boolean isHoldingGas() {
        GasStack gas = getGas();
        return gas != null && gas.amount > 0;
    }

    @Override
    public int fillInternal(FluidStack resource, boolean doFill) {
        if (isHoldingGas()) {
            return 0;
        }
        return super.fillInternal(resource, doFill);
    }

    @Nullable
    @Override
    public FluidStack drainInternal(int maxDrain, boolean doDrain) {
        if (isHoldingGas()) {
            return null;
        }
        return super.drainInternal(maxDrain, doDrain);
    }

    @Nullable
    @Override
    public FluidStack drainInternal(FluidStack resource, boolean doDrain) {
        if (isHoldingGas()) {
            return null;
        }
        return super.drainInternal(resource, doDrain);
    }

    @Nullable
    public GasStack getGas() {
        try {
            rwLock.readLock().lock();
            return this.gasTank.getGas();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void setGas(@Nullable GasStack stack) {
        try {
            rwLock.writeLock().lock();
            if (stack != null) {
                this.gasTank.setGas(stack.copy());
                setFluid(null);
            } else {
                this.gasTank.setGas(null);
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public int receiveGas(ForgeDirection side, GasStack stack, boolean doTransfer) {
        if (stack == null || stack.amount <= 0) {
            return 0;
        }
        try {
            (doTransfer ? rwLock.writeLock() : rwLock.readLock()).lock();

            if (fluid != null && fluid.amount > 0) {
                return 0; //We don't collide with the internal fluids
            }

            int receive = gasTank.receive(stack, doTransfer);
            if (receive != 0 && doTransfer) {
                onContentsChanged();
            }
            return receive;
        } finally {
            (doTransfer ? rwLock.writeLock() : rwLock.readLock()).unlock();
        }
    }

    @Override
    @Deprecated
    public int receiveGas(ForgeDirection side, GasStack stack) {
        return receiveGas(side, stack, true);
    }

    @Override
    public GasStack drawGas(ForgeDirection side, int amount, boolean doTransfer) {
        try {
            (doTransfer ? rwLock.writeLock() : rwLock.readLock()).lock();

            if (getGas() == null || amount <= 0) {
                return null;
            }
            if (getFluid() != null && getFluid().amount > 0) {
                return null; //We don't collide with the internal fluids
            }
            if (!this.canDrawGas(side, null)) {
                return null;
            }

            GasStack drawn = this.gasTank.draw(amount, doTransfer);
            if (drawn != null && doTransfer) {
                onContentsChanged();
            }
            return drawn;
        } finally {
            (doTransfer ? rwLock.writeLock() : rwLock.readLock()).unlock();
        }
    }

    @Override
    @Deprecated
    public GasStack drawGas(ForgeDirection side, int amount) {
        return drawGas(side, amount, true);
    }

    @Override
    public GasStack drawGas(final GasStack toDraw, final boolean doTransfer) {
        try {
            (doTransfer ? rwLock.writeLock() : rwLock.readLock()).lock();

            return canDrawGas(null, toDraw.getGas()) ? drawGas(null, toDraw.amount, doTransfer) : null;
        } finally {
            (doTransfer ? rwLock.writeLock() : rwLock.readLock()).unlock();
        }
    }

    @Override
    public boolean canReceiveGas(ForgeDirection side, Gas type) {
        FluidStack contained = getFluid();
        return canFill() && (contained == null || contained.amount <= 0) && gasTank.canReceive(type);
    }

    @Override
    public boolean canDrawGas(ForgeDirection side, Gas type) {
        return canDrain() && gasTank.canDraw(type);
    }

    @Nonnull
    @Override
    public GasTankInfo[] getTankInfo() {
        return new GasTank[]{gasTank};
    }

    public void readGasFromNBT(NBTTagCompound nbt) {
        NBTTagCompound subGas = nbt.getCompoundTag("gasTag");
        if (subGas.hasNoTags()) {
            setGas(null);
            return;
        }

        // Old version tag support.
        if (!subGas.hasKey("Empty")) {
            setGas(GasStack.readFromNBT(subGas));
        } else {
            setGas(null);
        }
    }

    public void writeGasToNBT(NBTTagCompound nbt) {
        NBTTagCompound subGas = new NBTTagCompound();
        if (getGas() != null) {
            getGas().write(subGas);
        }
        nbt.setTag("gasTag", subGas);
    }

}
