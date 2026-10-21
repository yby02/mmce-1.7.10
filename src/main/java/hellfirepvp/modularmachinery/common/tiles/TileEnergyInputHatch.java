/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.tiles;

import cpw.mods.fml.common.Optional;
import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.block.prop.EnergyHatchData;
import hellfirepvp.modularmachinery.common.integration.GTEnergyHelper;
import hellfirepvp.modularmachinery.common.integration.IntegrationIC2EventHandlerHelper;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.tiles.base.TileEnergyHatch;
import hellfirepvp.modularmachinery.common.util.IEnergyHandlerAsync;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import javax.annotation.Nullable;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: TileEnergyInputHatch
 * Created by HellFirePvP
 * Date: 08.07.2017 / 12:47
 * <p>
 * 1.7.10 移植：接收 RF（IEnergyReceiver，软依赖 CoFHAPI|energy）、IC2 EU（IEnergySink，软依赖 IC2，1 EU = 4 RF）
 * 与 GregTech 5 的 EU（IEnergyConnected，软依赖 gregtech，见 {@link GTEnergyHelper}）。
 */
@Optional.InterfaceList({
    @Optional.Interface(iface = "cofh.api.energy.IEnergyReceiver", modid = "CoFHAPI|energy"),
    @Optional.Interface(iface = "ic2.api.energy.tile.IEnergySink", modid = "IC2"),
    @Optional.Interface(iface = "gregtech.api.interfaces.tileentity.IEnergyConnected", modid = "gregtech")
})
public class TileEnergyInputHatch extends TileEnergyHatch
    implements cofh.api.energy.IEnergyReceiver, ic2.api.energy.tile.IEnergySink, gregtech.api.interfaces.tileentity.IEnergyConnected {

    public TileEnergyInputHatch() {
    }

    public TileEnergyInputHatch(EnergyHatchData size) {
        super(size, IOType.INPUT);
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return true;
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
    }

    // ---------------- RF ----------------

    @Override
    public int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate) {
        return receiveEnergy(maxReceive, simulate);
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
    public double getDemandedEnergy() {
        return Math.min((this.size.maxEnergy - this.energy.get()) / 4, this.size.getIC2EnergyTransmission());
    }

    @Override
    @Optional.Method(modid = "IC2")
    public int getSinkTier() {
        return this.size.ic2EnergyTier;
    }

    @Override
    @Optional.Method(modid = "IC2")
    public double injectEnergy(ForgeDirection directionFrom, double amount, double voltage) {
        long addable = Math.min((this.size.maxEnergy - this.energy.get()) / 4L, (long) Math.floor(amount));
        amount -= addable;
        this.energy.set(MiscUtils.clamp(this.energy.get() + addable * 4L, 0, this.size.maxEnergy));
        markNoUpdateSync();
        return amount;
    }

    @Override
    @Optional.Method(modid = "IC2")
    public boolean acceptsEnergyFrom(TileEntity emitter, ForgeDirection direction) {
        return true;
    }

    // ---------------- GregTech 5 ----------------

    @Override
    @Optional.Method(modid = "gregtech")
    public long injectEnergyUnits(ForgeDirection side, long voltage, long amperage) {
        return GTEnergyHelper.accept(this, voltage, amperage);
    }

    @Override
    @Optional.Method(modid = "gregtech")
    public boolean inputEnergyFrom(ForgeDirection side) {
        return true;
    }

    @Override
    @Optional.Method(modid = "gregtech")
    public boolean outputsEnergyTo(ForgeDirection side) {
        return false;
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
        return new MachineComponent.EnergyHatch(IOType.INPUT) {
            @Override
            public IEnergyHandlerAsync getContainerProvider() {
                return TileEnergyInputHatch.this;
            }
        };
    }
}
