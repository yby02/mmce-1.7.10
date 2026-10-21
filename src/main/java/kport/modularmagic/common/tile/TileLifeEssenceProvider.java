package kport.modularmagic.common.tile;

import WayofTime.alchemicalWizardry.api.items.interfaces.IBloodOrb;
import WayofTime.alchemicalWizardry.api.soulNetwork.SoulNetworkHandler;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.tiles.base.MachineComponentTile;
import hellfirepvp.modularmachinery.common.tiles.base.TileInventory;
import hellfirepvp.modularmachinery.common.util.IOInventory;
import kport.modularmagic.common.crafting.helper.LifeEssenceProviderCopy;
import kport.modularmagic.common.tile.machinecomponent.MachineComponentLifeEssenceProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;

/**
 * 生命精华仓（血魔法 1）：放入一个已绑定的血之宝珠，仓连到宝珠主人的灵魂网络。
 * 输入仓把网络中的 LP 取到自己的缓存里（缓存上限为宝珠容量的 1/10，同上游），配方从缓存中消耗；
 * 输出仓的缓存由配方填入，再存进网络（不超过宝珠容量）。
 * <p>
 * 1.7.10 移植：上游使用血魔法 2 的 SoulNetwork / Binding；血魔法 1 的网络按主人名访问（SoulNetworkHandler，宝珠 NBT 中的 ownerName）。
 * 血魔法 1 的 syphonFromNetwork 是全有或全无，这里只取网络中现有的量。
 * 界面显示缓存量（上游界面只有宝珠格），因此按选择性更新方块实体处理：玩家右键与打开界面期间向该玩家同步缓存。
 */
public abstract class TileLifeEssenceProvider extends TileInventory implements MachineComponentTile,
    hellfirepvp.modularmachinery.common.tiles.base.SelectiveUpdateTileEntity {

    protected volatile int lifeEssenceCache = 0;
    private            int tick;

    public TileLifeEssenceProvider() {
        super(1);
    }

    @Override
    public IOInventory buildInventory(TileInventory tile, int size) {
        IOInventory inv = new IOInventory(tile, new int[0], new int[0]);
        inv.setMiscSlots(0);
        inv.setStackLimit(1, 0);
        return inv;
    }

    public static boolean isOrb(ItemStack stack) {
        return stack != null && stack.getItem() instanceof IBloodOrb;
    }

    @Nullable
    public String getOwner() {
        ItemStack stack = getInventory().getStackInSlot(0);
        if (!isOrb(stack)) {
            return null;
        }
        String owner = SoulNetworkHandler.getOwnerName(stack);
        return owner == null || owner.isEmpty() ? null : owner;
    }

    public int getOrbCapacity() {
        ItemStack stack = getInventory().getStackInSlot(0);
        return isOrb(stack) ? ((IBloodOrb) stack.getItem()).getMaxEssence() : 0;
    }

    /** 缓存上限：宝珠容量的 1/10（同上游）。 */
    public int getCacheCapacity() {
        return getOrbCapacity() / 10;
    }

    public int getLifeEssenceCache() {
        return lifeEssenceCache;
    }

    public synchronized int addLifeEssenceCache(int amount) {
        int n = Math.max(0, Math.min(amount, getCacheCapacity() - lifeEssenceCache));
        lifeEssenceCache += n;
        markNoUpdateSync();
        return n;
    }

    public synchronized int removeLifeEssenceCache(int amount) {
        int n = Math.max(0, Math.min(amount, lifeEssenceCache));
        lifeEssenceCache -= n;
        markNoUpdateSync();
        return n;
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        inventory.setStackLimit(1, 0);
        lifeEssenceCache = compound.getInteger("lifeEssenceCache");
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        compound.setInteger("lifeEssenceCache", lifeEssenceCache);
    }

    @Override
    public void doRestrictedTick() {
        if (worldObj.isRemote || ++tick % 10 != 0) {
            return;
        }
        String owner = getOwner();
        if (owner != null) {
            exchange(owner);
        }
    }

    protected abstract void exchange(String owner);

    public static class Input extends TileLifeEssenceProvider {

        @Override
        protected synchronized void exchange(String owner) {
            int need = Math.min(getCacheCapacity() - lifeEssenceCache, SoulNetworkHandler.getCurrentEssence(owner));
            if (need <= 0) {
                return;
            }
            int got = SoulNetworkHandler.syphonFromNetwork(owner, need);
            if (got > 0) {
                lifeEssenceCache += got;
                markNoUpdateSync();
            }
        }

        @Override
        public boolean canGroupInput() {
            return true;
        }

        @Nullable
        @Override
        public MachineComponentLifeEssenceProvider provideComponent() {
            return new MachineComponentLifeEssenceProvider(IOType.INPUT, new LifeEssenceProviderCopy(this));
        }
    }

    public static class Output extends TileLifeEssenceProvider {

        @Override
        protected synchronized void exchange(String owner) {
            if (lifeEssenceCache <= 0) {
                return;
            }
            int added = SoulNetworkHandler.addCurrentEssenceToMaximum(owner, lifeEssenceCache, getOrbCapacity());
            if (added > 0) {
                lifeEssenceCache -= added;
                markNoUpdateSync();
            }
        }

        @Nullable
        @Override
        public MachineComponentLifeEssenceProvider provideComponent() {
            return new MachineComponentLifeEssenceProvider(IOType.OUTPUT, new LifeEssenceProviderCopy(this));
        }
    }
}
