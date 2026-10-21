/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.tiles;

import cpw.mods.fml.common.Optional;
import github.kasuminova.mmce.compat.EnumFacing;
import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.block.prop.EnergyHatchData;
import hellfirepvp.modularmachinery.common.integration.GTEnergyHelper;
import hellfirepvp.modularmachinery.common.integration.IntegrationIC2EventHandlerHelper;
import hellfirepvp.modularmachinery.common.integration.RFEnergyTransfer;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.tiles.base.TileEnergyHatch;
import hellfirepvp.modularmachinery.common.util.IEnergyHandlerAsync;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import javax.annotation.Nullable;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: TileEnergyOutputHatch
 * Created by HellFirePvP
 * Date: 08.07.2017 / 12:43
 * <p>
 * 1.7.10 移植：
 * - 每 tick 主动向相邻的 RF 接收者推送能量（与 1.12.2 的 FE/RF 推送逻辑相同，单 tick 总量不超过传输上限）。
 * - 同时作为 RF 提供者（IEnergyProvider）供导管主动抽取，并作为 IC2 能源（IEnergySource，1 EU = 4 RF）。
 * - 上游的 GTCEu 直连在 1.7.10 改为 GregTech 5 的 IEnergyConnected（软依赖 gregtech，见 {@link GTEnergyHelper}）：每 tick 先按本仓的 GT 电压 / 电流
 *   向相邻的 GT 导线 / 机器输出 EU，剩余部分再按 RF 推送。Flux Networks、Draconic Evolution 的直连没有移植（这两个模组 1.7.10 版本的接口不同）。
 */
@Optional.InterfaceList({
    @Optional.Interface(iface = "cofh.api.energy.IEnergyProvider", modid = "CoFHAPI|energy"),
    @Optional.Interface(iface = "ic2.api.energy.tile.IEnergySource", modid = "IC2"),
    @Optional.Interface(iface = "gregtech.api.interfaces.tileentity.IEnergyConnected", modid = "gregtech")
})
public class TileEnergyOutputHatch extends TileEnergyHatch
    implements cofh.api.energy.IEnergyProvider, ic2.api.energy.tile.IEnergySource, gregtech.api.interfaces.tileentity.IEnergyConnected {

    public TileEnergyOutputHatch() {
    }

    public TileEnergyOutputHatch(EnergyHatchData size) {
        super(size, IOType.OUTPUT);
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    @Override
    public boolean canReceive() {
        return false;
    }

    @Override
    public void update() {
        if (worldObj.isRemote) {
            return;
        }

        if (!tickedOnce) {
            if (Mods.IC2.isPresent()) {
                IntegrationIC2EventHandlerHelper.onEnergyTileLoaded(this);
            }
            tickedOnce = true;
        }

        long prevEnergy = this.energy.get();
        long maxCanExtract = Math.min(this.size.transferLimit, this.energy.get());
        if (maxCanExtract <= 0) {
            return;
        }

        if (Mods.GREGTECH.isPresent()) {
            long transferred = GTEnergyHelper.emit(this, maxCanExtract);
            if (transferred > 0) {
                maxCanExtract -= transferred;
                this.energy.addAndGet(-transferred);
            }
        }

        if (Mods.REDSTONEFLUXAPI.isPresent() && maxCanExtract > 0) {
            for (EnumFacing face : EnumFacing.VALUES) {
                TileEntity te = worldObj.getTileEntity(xCoord + face.getXOffset(), yCoord + face.getYOffset(), zCoord + face.getZOffset());
                if (te == null || te instanceof TileEnergyHatch) {
                    continue;
                }
                int transferred = RFEnergyTransfer.push(te, face.getOpposite().toForgeDirection(), convertDownEnergy(maxCanExtract));
                if (transferred > 0) {
                    maxCanExtract -= transferred;
                    this.energy.addAndGet(-transferred);
                }
                if (maxCanExtract <= 0) {
                    break;
                }
            }
        }

        if (prevEnergy != this.energy.get()) {
            markNoUpdateSync();
        }
    }

    // ---------------- RF ----------------

    @Override
    public int extractEnergy(ForgeDirection from, int maxExtract, boolean simulate) {
        return extractEnergy(maxExtract, simulate);
    }

    // ---------------- IC2 ----------------

    @Override
    public void invalidate() {
        super.invalidate();
        unloadFromIC2();
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        unloadFromIC2();
    }

    private void unloadFromIC2() {
        if (Mods.IC2.isPresent() && tickedOnce && worldObj != null && !worldObj.isRemote) {
            IntegrationIC2EventHandlerHelper.onEnergyTileUnLoaded(this);
            tickedOnce = false;
        }
    }

    @Override
    @Optional.Method(modid = "IC2")
    public double getOfferedEnergy() {
        return Math.min(this.size.getIC2EnergyTransmission(), this.energy.get() / 4L);
    }

    @Override
    @Optional.Method(modid = "IC2")
    public void drawEnergy(double amount) {
        this.energy.set(Math.max(0, this.energy.get() - (long) Math.floor(amount * 4)));
        markNoUpdateSync();
    }

    @Override
    @Optional.Method(modid = "IC2")
    public int getSourceTier() {
        return size.ic2EnergyTier;
    }

    @Override
    @Optional.Method(modid = "IC2")
    public boolean emitsEnergyTo(TileEntity receiver, ForgeDirection direction) {
        return true;
    }

    // ---------------- GregTech 5 ----------------

    @Override
    @Optional.Method(modid = "gregtech")
    public long injectEnergyUnits(ForgeDirection side, long voltage, long amperage) {
        return 0;
    }

    @Override
    @Optional.Method(modid = "gregtech")
    public boolean inputEnergyFrom(ForgeDirection side) {
        return false;
    }

    @Override
    @Optional.Method(modid = "gregtech")
    public boolean outputsEnergyTo(ForgeDirection side) {
        return true;
    }

    @Override
    @Optional.Method(modid = "gregtech")
    public byte getColorization() {
        return -1;
    }

    @Override
    @Optional.Method(modid = "gregtech")
    public byte setColorization(byte color) {
        return -1;
    }

    @Nullable
    @Override
    public MachineComponent.EnergyHatch provideComponent() {
        return new MachineComponent.EnergyHatch(IOType.OUTPUT) {
            @Override
            public IEnergyHandlerAsync getContainerProvider() {
                return TileEnergyOutputHatch.this;
            }
        };
    }
}
