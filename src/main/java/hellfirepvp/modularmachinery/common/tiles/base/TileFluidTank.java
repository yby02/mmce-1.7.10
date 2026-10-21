/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.tiles.base;

import cpw.mods.fml.common.Optional;
import github.kasuminova.mmce.compat.MathHelper;
import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.block.prop.FluidHatchSize;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.util.HybridGasTank;
import hellfirepvp.modularmachinery.common.util.HybridTank;
import hellfirepvp.modularmachinery.common.util.RedstoneHelper;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.GasTankInfo;
import mekanism.api.gas.IGasHandler;
import mekanism.api.gas.IGasTankInfoProvider;
import mekanism.api.gas.ITubeConnection;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: TileFluidTank
 * Created by HellFirePvP
 * Date: 07.07.2017 / 17:51
 * <p>
 * 1.7.10 移植：
 * - 1.12.2 通过 FLUID_HANDLER_CAPABILITY 暴露 {@link HybridTank}；1.7.10 由本类实现 net.minecraftforge.fluids.IFluidHandler（各面均可访问），
 *   供管道、桶以及其他模组的流体系统使用。
 * - Mekanism 气体：1.12.2 通过 Capability 暴露 IGasHandler / ITubeConnection；1.7.10 的 Mekanism 直接检查 TileEntity 是否实现这些接口，
 *   因此由本类以 {@link Optional.Interface} 软依赖实现（未装 Mekanism 时接口与方法被 FML 剥离）。装有 Mekanism 时储罐为 {@link HybridGasTank}，
 *   同一时刻只能装流体或气体之一，与上游相同。
 */
@Optional.InterfaceList({
    @Optional.Interface(modid = "Mekanism", iface = "mekanism.api.gas.IGasHandler"),
    @Optional.Interface(modid = "Mekanism", iface = "mekanism.api.gas.ITubeConnection"),
    @Optional.Interface(modid = "Mekanism", iface = "mekanism.api.gas.IGasTankInfoProvider")
})
public abstract class TileFluidTank extends TileColorableMachineComponent
    implements MachineComponentTile, IFluidHandler, IGasHandler, ITubeConnection, IGasTankInfoProvider, SelectiveUpdateTileEntity {

    private HybridTank     tank;
    private IOType         ioType;
    private FluidHatchSize hatchSize;
    private int            prevRedstoneLevel = 0;

    public TileFluidTank() {
    }

    public TileFluidTank(FluidHatchSize size, IOType type) {
        this.tank = size.buildTank(this, true, true);
        this.hatchSize = size;
        this.ioType = type;
    }

    public HybridTank getTank() {
        return tank;
    }

    public IOType getIOType() {
        return ioType;
    }

    // ---------------- 1.7.10 IFluidHandler ----------------

    @Override
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
        return tank.fill(resource, doFill);
    }

    @Override
    public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
        return tank.drain(resource, doDrain);
    }

    @Override
    public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
        return tank.drain(maxDrain, doDrain);
    }

    @Override
    public boolean canFill(ForgeDirection from, Fluid fluid) {
        if (!tank.canFill() || tank.isHoldingGas()) {
            return false;
        }
        FluidStack contained = tank.getFluid();
        return contained == null || contained.getFluid() == fluid;
    }

    @Override
    public boolean canDrain(ForgeDirection from, Fluid fluid) {
        if (!tank.canDrain()) {
            return false;
        }
        FluidStack contained = tank.getFluid();
        return contained != null && (fluid == null || contained.getFluid() == fluid);
    }

    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection from) {
        return new FluidTankInfo[]{tank.getInfo()};
    }

    // ---------------- NBT ----------------

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);

        this.ioType = compound.getBoolean("input") ? IOType.INPUT : IOType.OUTPUT;
        this.hatchSize = FluidHatchSize.values()[MathHelper.clamp(compound.getInteger("size"), 0, FluidHatchSize.values().length - 1)];
        HybridTank newTank = hatchSize.buildTank(this, true, true);
        NBTTagCompound tankTag = compound.getCompoundTag("tank");
        newTank.readFromNBT(tankTag);
        this.tank = newTank;
        if (Mods.MEKANISM.isPresent()) {
            this.readMekGasData(tankTag);
        }
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);

        compound.setBoolean("input", ioType == IOType.INPUT);
        compound.setInteger("size", this.hatchSize.ordinal());
        NBTTagCompound tankTag = new NBTTagCompound();
        this.tank.writeToNBT(tankTag);
        if (Mods.MEKANISM.isPresent()) {
            this.writeMekGasData(tankTag);
        }
        compound.setTag("tank", tankTag);
    }

    @Override
    public void markNoUpdate() {
        int redstoneLevel = RedstoneHelper.getRedstoneLevel(this);
        if (prevRedstoneLevel != redstoneLevel) {
            prevRedstoneLevel = redstoneLevel;
            this.requireUpdateComparatorLevel = true;
        }
        super.markNoUpdate();
        this.requireUpdateComparatorLevel = false;
    }

    @Override
    public boolean canGroupInput() {
        return ioType == IOType.INPUT;
    }

    @Nullable
    @Override
    public MachineComponent<?> provideComponent() {
        return new MachineComponent.FluidHatch(ioType) {
            @Override
            public HybridTank getContainerProvider() {
                return TileFluidTank.this.tank;
            }
        };
    }

    // ---------------- Mekanism 气体（软依赖） ----------------

    @Optional.Method(modid = "Mekanism")
    private void writeMekGasData(NBTTagCompound compound) {
        if (this.tank instanceof HybridGasTank gasTank) {
            gasTank.writeGasToNBT(compound);
        }
    }

    @Optional.Method(modid = "Mekanism")
    private void readMekGasData(NBTTagCompound compound) {
        if (this.tank instanceof HybridGasTank gasTank) {
            gasTank.readGasFromNBT(compound);
        }
    }

    @Override
    @Optional.Method(modid = "Mekanism")
    public boolean canTubeConnect(ForgeDirection side) {
        return true;
    }

    @Override
    @Optional.Method(modid = "Mekanism")
    public int receiveGas(ForgeDirection side, GasStack stack, boolean doTransfer) {
        return this.tank instanceof HybridGasTank gasTank ? gasTank.receiveGas(side, stack, doTransfer) : 0;
    }

    @Override
    @Deprecated
    @Optional.Method(modid = "Mekanism")
    public int receiveGas(ForgeDirection side, GasStack stack) {
        return receiveGas(side, stack, true);
    }

    @Override
    @Optional.Method(modid = "Mekanism")
    public GasStack drawGas(ForgeDirection side, int amount, boolean doTransfer) {
        return this.tank instanceof HybridGasTank gasTank ? gasTank.drawGas(side, amount, doTransfer) : null;
    }

    @Override
    @Deprecated
    @Optional.Method(modid = "Mekanism")
    public GasStack drawGas(ForgeDirection side, int amount) {
        return drawGas(side, amount, true);
    }

    @Override
    @Optional.Method(modid = "Mekanism")
    public boolean canReceiveGas(ForgeDirection side, Gas type) {
        return this.tank instanceof HybridGasTank gasTank && gasTank.canReceiveGas(side, type);
    }

    @Override
    @Optional.Method(modid = "Mekanism")
    public boolean canDrawGas(ForgeDirection side, Gas type) {
        return this.tank instanceof HybridGasTank gasTank && gasTank.canDrawGas(side, type);
    }

    @Nonnull
    @Override
    @Optional.Method(modid = "Mekanism")
    public GasTankInfo[] getTankInfo() {
        return this.tank instanceof HybridGasTank gasTank ? gasTank.getTankInfo() : NONE;
    }
}
