/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.tiles.base;

import github.kasuminova.mmce.compat.ITickable;
import github.kasuminova.mmce.compat.energy.IEnergyStorage;
import hellfirepvp.modularmachinery.common.block.prop.EnergyHatchData;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.util.IEnergyHandlerAsync;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import hellfirepvp.modularmachinery.common.util.RedstoneHelper;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.concurrent.atomic.AtomicLong;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: TileEnergyHatch
 * Created by HellFirePvP
 * Date: 08.07.2017 / 10:14
 * <p>
 * 1.7.10 移植：
 * - 内部能量单位为 RF（与 1.12.2 的 FE 等价），存储与传输上限沿用 {@link EnergyHatchData}。
 * - 1.12.2 的 Forge Energy 能力在 1.7.10 不存在；对外接口由子类以软依赖实现：
 *   RF（CoFHAPI|energy 的 IEnergyReceiver/IEnergyProvider）与 IC2 EU（IEnergySink/IEnergySource，1 EU = 4 RF）。
 * - GTCEu 能量容器、TheOneProbe IBigPower、Draconic Evolution 能量核心直连已移除（GT5 与 DE 1.7.10 版本在 P5 评估）。
 */
public abstract class TileEnergyHatch extends TileColorableMachineComponent implements
    ITickable,
    IEnergyStorage,
    IEnergyHandlerAsync,
    MachineComponentTile,
    SelectiveUpdateTileEntity {

    protected final AtomicLong      energy            = new AtomicLong();
    protected       EnergyHatchData size;
    protected       boolean         tickedOnce        = false;
    private         int             prevRedstoneLevel = 0;

    public TileEnergyHatch() {
    }

    public TileEnergyHatch(EnergyHatchData size, IOType ioType) {
        this.size = size;
    }

    protected static int convertDownEnergy(long energy) {
        return energy >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) energy;
    }

    // ---------------- FE 形状的内部接口 ----------------

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (!canReceive()) {
            return 0;
        }
        int insertable = this.energy.get() + maxReceive > this.size.maxEnergy ? convertDownEnergy(this.size.maxEnergy - this.energy.get()) : maxReceive;
        insertable = Math.min(insertable, convertDownEnergy(size.transferLimit));
        if (!simulate && insertable > 0) {
            this.energy.set(MiscUtils.clamp(this.energy.get() + insertable, 0, this.size.maxEnergy));
            markNoUpdateSync();
        }
        return insertable;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (!canExtract()) {
            return 0;
        }
        int extractable = this.energy.get() - maxExtract < 0 ? convertDownEnergy(this.energy.get()) : maxExtract;
        extractable = Math.min(extractable, convertDownEnergy(size.transferLimit));
        if (!simulate && extractable > 0) {
            this.energy.set(MiscUtils.clamp(this.energy.get() - extractable, 0, this.size.maxEnergy));
            markNoUpdateSync();
        }
        return extractable;
    }

    @Override
    public int getEnergyStored() {
        return convertDownEnergy(this.energy.get());
    }

    @Override
    public int getMaxEnergyStored() {
        return convertDownEnergy(this.size.maxEnergy);
    }

    @Override
    public abstract boolean canExtract();

    @Override
    public abstract boolean canReceive();

    // ---------------- RF API 公共部分（方法签名不引用 CoFH 类型，未安装 RF API 时无影响） ----------------

    public int getEnergyStored(ForgeDirection from) {
        return getEnergyStored();
    }

    public int getMaxEnergyStored(ForgeDirection from) {
        return getMaxEnergyStored();
    }

    public boolean canConnectEnergy(ForgeDirection from) {
        return true;
    }

    // ---------------- NBT ----------------

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);

        NBTBase energyTag = compound.getTag("energy");
        if (energyTag instanceof NBTBase.NBTPrimitive) {
            this.energy.set(((NBTBase.NBTPrimitive) energyTag).func_150291_c());
        }
        this.size = EnergyHatchData.values()[compound.getInteger("hatchSize")];
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);

        compound.setLong("energy", this.energy.get());
        compound.setInteger("hatchSize", this.size.ordinal());
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

    //MM stuff

    public EnergyHatchData getTier() {
        return size;
    }

    @Override
    public long getCurrentEnergy() {
        return this.energy.get();
    }

    @Override
    public void setCurrentEnergy(long energy) {
        synchronized (this) {
            this.energy.set(MiscUtils.clamp(energy, 0, getMaxEnergy()));
        }
        markNoUpdateSync();
    }

    @Override
    public boolean extractEnergy(long extract) {
        boolean success = false;
        synchronized (this) {
            if (this.energy.get() >= extract) {
                this.energy.addAndGet(-extract);
                success = true;
            }
        }
        if (success) {
            markNoUpdateSync();
        }
        return success;
    }

    @Override
    public boolean receiveEnergy(long receive) {
        boolean success = false;
        synchronized (this) {
            if (getRemainingCapacity() >= receive) {
                this.energy.addAndGet(receive);
                success = true;
            }
        }
        if (success) {
            markNoUpdateSync();
        }
        return success;
    }

    @Override
    public long getMaxEnergy() {
        return this.size.maxEnergy;
    }

}
