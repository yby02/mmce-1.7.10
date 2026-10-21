package github.kasuminova.mmce.common.tile;

import appeng.api.AEApi;
import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEFluidStack;
import appeng.me.GridAccessException;
import appeng.util.Platform;
import github.kasuminova.mmce.common.tile.base.MEFluidBus;
import github.kasuminova.mmce.compat.fluids.IFluidHandler;
import hellfirepvp.modularmachinery.common.lib.BlocksMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import java.util.concurrent.locks.ReadWriteLock;

/**
 * ME 流体输入总线：9 个设定格（流体种类），每格从 ME 网络把对应储罐补满到当前容量，供机器作为流体输入；
 * 设定改变或清空后，储罐中的流体放回 ME 网络。逻辑同上游 MEFluidInputBus。
 */
public class MEFluidInputBus extends MEFluidBus implements SettingsTransfer {

    private final FluidStack[] config = new FluidStack[TANK_SLOT_AMOUNT];

    @Override
    public ItemStack getVisualItemStack() {
        return new ItemStack(BlocksMM.meFluidInputBus);
    }

    public FluidStack getConfig(int slot) {
        return config[slot];
    }

    /** 设定某格的流体种类（null 清空），由界面调用。 */
    public void setConfig(int slot, FluidStack fluid) {
        if (slot < 0 || slot >= config.length) {
            return;
        }
        config[slot] = fluid == null ? null : new FluidStack(fluid, 1);
        synchronized (this) {
            changedSlots[slot] = true;
        }
        markForUpdateSync();
        alertDevice();
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        readConfigNBT(compound.getCompoundTag("config"));
    }

    private void readConfigNBT(NBTTagCompound tag) {
        for (int i = 0; i < config.length; i++) {
            config[i] = tag.hasKey("#" + i) ? FluidStack.loadFluidStackFromNBT(tag.getCompoundTag("#" + i)) : null;
        }
    }

    private NBTTagCompound writeConfigNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        for (int i = 0; i < config.length; i++) {
            if (config[i] != null) {
                tag.setTag("#" + i, config[i].writeToNBT(new NBTTagCompound()));
            }
        }
        return tag;
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        compound.setTag("config", writeConfigNBT());
    }

    @Override
    public NBTTagCompound downloadSettings() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("config", writeConfigNBT());
        return tag;
    }

    @Override
    public void uploadSettings(NBTTagCompound settings) {
        readConfigNBT(settings.getCompoundTag("config"));
        synchronized (this) {
            for (int i = 0; i < changedSlots.length; i++) {
                changedSlots[i] = true;
            }
        }
        markForUpdateSync();
        alertDevice();
    }

    @Override
    public MachineComponent.FluidHatch provideComponent() {
        return new MachineComponent.FluidHatch(IOType.INPUT) {
            @Override
            public IFluidHandler getContainerProvider() {
                return tanks;
            }

            @Override
            public long getGroupID() {
                return getGroupId();
            }
        };
    }

    @Override
    public boolean canGroupInput() {
        return true;
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(10, 120, !needsUpdate(), true);
    }

    private boolean needsUpdate() {
        int capacity = tanks.getCapacity();
        for (int slot = 0; slot < TANK_SLOT_AMOUNT; slot++) {
            FluidStack cfg = config[slot];
            FluidStack inv = tanks.getFluidInSlot(slot);
            if (cfg == null) {
                if (inv != null) {
                    return true;
                }
                continue;
            }
            if (inv == null || !cfg.isFluidEqual(inv) || inv.amount != capacity) {
                return true;
            }
        }
        return false;
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (!proxy.isActive()) {
            return TickRateModulation.IDLE;
        }
        int[] needUpdate = getNeedUpdateSlots();
        if (needUpdate.length == 0) {
            return TickRateModulation.SLOWER;
        }
        ReadWriteLock rwLock = tanks.getRWLock();
        rwLock.writeLock().lock();
        try {
            boolean success = false;
            IMEMonitor<IAEFluidStack> me = proxy.getStorage().getFluidInventory();
            int capacity = tanks.getCapacity();
            for (int slot : needUpdate) {
                synchronized (this) {
                    changedSlots[slot] = false;
                }
                FluidStack cfg = config[slot];
                FluidStack inv = tanks.getFluidInSlot(slot);
                if (cfg == null) {
                    if (inv != null) {
                        tanks.setFluidInSlot(slot, insert(me, inv));
                    }
                    continue;
                }
                if (inv == null || !cfg.isFluidEqual(inv)) {
                    if (inv != null) {
                        FluidStack left = insert(me, inv);
                        if (left != null) {
                            tanks.setFluidInSlot(slot, left);
                            continue;
                        }
                    }
                    FluidStack got = extract(me, new FluidStack(cfg, capacity));
                    tanks.setFluidInSlot(slot, got);
                    success |= got != null;
                    continue;
                }
                if (inv.amount == capacity) {
                    continue;
                }
                if (inv.amount < capacity) {
                    FluidStack got = extract(me, new FluidStack(inv, capacity - inv.amount));
                    if (got != null) {
                        tanks.setFluidInSlot(slot, new FluidStack(inv, inv.amount + got.amount));
                        success = true;
                    }
                } else {
                    FluidStack left = insert(me, new FluidStack(inv, inv.amount - capacity));
                    tanks.setFluidInSlot(slot, new FluidStack(inv, capacity + (left == null ? 0 : left.amount)));
                    success = true;
                }
            }
            return success ? TickRateModulation.FASTER : TickRateModulation.SLOWER;
        } catch (GridAccessException e) {
            synchronized (this) {
                changedSlots = new boolean[changedSlots.length];
            }
            return TickRateModulation.IDLE;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    private FluidStack extract(IMEMonitor<IAEFluidStack> me, FluidStack stack) throws GridAccessException {
        IAEFluidStack ae = AEApi.instance().storage().createFluidStack(stack);
        if (ae == null) {
            return null;
        }
        IAEFluidStack got = Platform.poweredExtraction(proxy.getEnergy(), me, ae, source);
        return got == null ? null : got.getFluidStack();
    }

    private FluidStack insert(IMEMonitor<IAEFluidStack> me, FluidStack stack) throws GridAccessException {
        IAEFluidStack ae = AEApi.instance().storage().createFluidStack(stack);
        if (ae == null) {
            return stack;
        }
        IAEFluidStack left = Platform.poweredInsert(proxy.getEnergy(), me, ae, source);
        return left == null ? null : left.getFluidStack();
    }
}
