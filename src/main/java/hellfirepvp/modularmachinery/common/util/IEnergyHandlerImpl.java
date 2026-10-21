package hellfirepvp.modularmachinery.common.util;

public class IEnergyHandlerImpl implements IEnergyHandler {
    protected final long capacity;

    protected long energy = 0;

    public IEnergyHandlerImpl(final long capacity) {
        this.capacity = capacity;
    }

    public IEnergyHandlerImpl(final IEnergyHandler handler) {
        this.capacity = handler.getMaxEnergy();
        this.energy = handler.getCurrentEnergy();
    }

    @Override
    public long getCurrentEnergy() {
        return this.energy;
    }

    @Override
    public void setCurrentEnergy(long energy) {
        this.energy = MiscUtils.clamp(energy, 0, capacity);
    }

    @Override
    public long getMaxEnergy() {
        return capacity;
    }
}
