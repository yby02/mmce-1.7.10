package kport.modularmagic.common.tile;

import github.kasuminova.mmce.compat.ITickable;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.tiles.base.MachineComponentTile;
import hellfirepvp.modularmachinery.common.tiles.base.TileColorableMachineComponent;
import kport.modularmagic.common.crafting.helper.ManaProviderCopy;
import kport.modularmagic.common.tile.machinecomponent.MachineComponentManaProvider;
import net.minecraft.nbt.NBTTagCompound;
import vazkii.botania.api.mana.IManaPool;
import vazkii.botania.api.mana.IManaReceiver;
import vazkii.botania.api.mana.ManaNetworkEvent;

/**
 * 魔力仓（植物魔法）。输入仓接收魔力脉冲（魔力发射器对准它发射）；输出仓登记为魔力池，相邻的魔力发射器、功能花等从中取用魔力。
 * 1.7.10 移植：取自上游 kport.modularmagic.common.tile.TileManaProvider，接口换成植物魔法 1.7.10（GTNH）的 IManaReceiver / IManaPool。
 */
public abstract class TileManaProvider extends TileColorableMachineComponent implements ITickable, IManaReceiver, MachineComponentTile {

    /** 每个仓的魔力上限（同上游默认值）。 */
    public static int manaCapacity = 100000;

    private volatile int mana = 0;

    @Override
    public void update() {
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        mana = compound.getInteger("mana");
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        compound.setInteger("mana", mana);
    }

    @Override
    public int getCurrentMana() {
        return mana;
    }

    @Override
    public boolean isFull() {
        return mana >= getManaCapacity();
    }

    /** 植物魔法的约定：负数表示取出（魔力发射器从相邻的魔力池抽取时即调用 recieveMana(-n)）。 */
    @Override
    public synchronized void recieveMana(int amount) {
        mana = Math.max(0, Math.min(mana + amount, getManaCapacity()));
        markNoUpdateSync();
    }

    public synchronized int reduceMana(int amount) {
        int n = Math.max(0, Math.min(amount, mana));
        mana -= n;
        markNoUpdateSync();
        return n;
    }

    public synchronized int addMana(int amount) {
        int n = Math.max(0, Math.min(amount, getManaCapacity() - mana));
        mana += n;
        markNoUpdateSync();
        return n;
    }

    public int getManaCapacity() {
        return manaCapacity;
    }

    @Override
    public boolean canRecieveManaFromBursts() {
        return false;
    }

    public static class Input extends TileManaProvider {

        @Override
        public MachineComponentManaProvider provideComponent() {
            return new MachineComponentManaProvider(IOType.INPUT, new ManaProviderCopy(this));
        }

        @Override
        public boolean canRecieveManaFromBursts() {
            return true;
        }

        @Override
        public boolean canGroupInput() {
            return true;
        }
    }

    public static class Output extends TileManaProvider implements IManaPool {

        private boolean inNetwork = false;

        @Override
        public void update() {
            if (!inNetwork && worldObj != null && !worldObj.isRemote && !isInvalid()) {
                ManaNetworkEvent.addPool(this);
                inNetwork = true;
            }
        }

        private void leaveNetwork() {
            if (inNetwork) {
                ManaNetworkEvent.removePool(this);
                inNetwork = false;
            }
        }

        @Override
        public void invalidate() {
            super.invalidate();
            leaveNetwork();
        }

        @Override
        public void onChunkUnload() {
            super.onChunkUnload();
            leaveNetwork();
        }

        @Override
        public boolean isOutputtingPower() {
            return true;
        }

        @Override
        public MachineComponentManaProvider provideComponent() {
            return new MachineComponentManaProvider(IOType.OUTPUT, new ManaProviderCopy(this));
        }
    }
}
