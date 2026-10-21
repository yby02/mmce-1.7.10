package kport.modularmagic.common.tile;

import github.kasuminova.mmce.compat.ITickable;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.tiles.base.MachineComponentTile;
import hellfirepvp.modularmachinery.common.tiles.base.TileColorableMachineComponent;
import kport.modularmagic.common.crafting.helper.AspectProviderCopy;
import kport.modularmagic.common.tile.machinecomponent.MachineComponentAspectProvider;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;

/**
 * 源质仓（神秘时代 4）：与源质罐一样只装一种源质，容量 250。输入仓以吸力 128 经源质管道从相邻的罐 / 管道吸入源质；
 * 输出仓吸力为 0，源质管道从中抽走源质。
 * <p>
 * 1.7.10 移植：上游继承神秘时代 6 的 TileJarFillable；神秘时代 4 的罐不在 API 中，这里按 TC4 的 IAspectContainer / IEssentiaTransport
 * 自行实现同样的行为（罐的吸入逻辑见 TileJarFillable#fillJar）。
 */
public abstract class TileAspectProvider extends TileColorableMachineComponent implements ITickable, IAspectContainer, IEssentiaTransport, MachineComponentTile {

    public static int capacity = 250;

    protected Aspect aspect;
    protected int    amount;
    private   int    tick;

    public synchronized Aspect getAspect() {
        return amount > 0 ? aspect : null;
    }

    public synchronized int getAmount() {
        return amount;
    }

    public int getCapacity() {
        return capacity;
    }

    // ---------------- 存取 ----------------

    @Override
    public synchronized AspectList getAspects() {
        AspectList list = new AspectList();
        if (aspect != null && amount > 0) {
            list.add(aspect, amount);
        }
        return list;
    }

    @Override
    public synchronized void setAspects(AspectList aspects) {
        if (aspects == null || aspects.size() == 0) {
            aspect = null;
            amount = 0;
        } else {
            aspect = aspects.getAspects()[0];
            amount = Math.min(aspects.getAmount(aspect), getCapacity());
        }
        markNoUpdateSync();
    }

    @Override
    public synchronized boolean doesContainerAccept(Aspect tag) {
        return tag != null && (aspect == null || amount == 0 || aspect == tag);
    }

    /** 放入，返回放不下的数量（与神秘时代的约定相同）。 */
    @Override
    public synchronized int addToContainer(Aspect tag, int am) {
        if (am <= 0 || !doesContainerAccept(tag)) {
            return am;
        }
        int added = Math.min(am, getCapacity() - amount);
        if (added > 0) {
            aspect = tag;
            amount += added;
            markNoUpdateSync();
        }
        return am - added;
    }

    @Override
    public synchronized boolean takeFromContainer(Aspect tag, int am) {
        if (aspect == null || aspect != tag || amount < am || am < 0) {
            return false;
        }
        amount -= am;
        if (amount <= 0) {
            amount = 0;
            aspect = null;
        }
        markNoUpdateSync();
        return true;
    }

    @Override
    public boolean takeFromContainer(AspectList ot) {
        return false;
    }

    @Override
    public synchronized boolean doesContainerContainAmount(Aspect tag, int am) {
        return aspect == tag && amount >= am;
    }

    @Override
    public synchronized boolean doesContainerContain(AspectList ot) {
        for (Aspect a : ot.getAspects()) {
            if (a != aspect || ot.getAmount(a) > amount) {
                return false;
            }
        }
        return true;
    }

    @Override
    public synchronized int containerContains(Aspect tag) {
        return aspect == tag ? amount : 0;
    }

    // ---------------- 源质管道 ----------------

    @Override
    public boolean isConnectable(ForgeDirection face) {
        return true;
    }

    @Override
    public boolean canInputFrom(ForgeDirection face) {
        return true;
    }

    @Override
    public boolean canOutputTo(ForgeDirection face) {
        return true;
    }

    @Override
    public void setSuction(Aspect aspect, int amount) {
    }

    @Override
    public synchronized Aspect getSuctionType(ForgeDirection face) {
        return getAspect();
    }

    @Override
    public synchronized int takeEssentia(Aspect tag, int am, ForgeDirection face) {
        if (!canOutputTo(face) || aspect != tag) {
            return 0;
        }
        int n = Math.min(am, amount);
        return n > 0 && takeFromContainer(tag, n) ? n : 0;
    }

    @Override
    public synchronized int addEssentia(Aspect tag, int am, ForgeDirection face) {
        return canInputFrom(face) ? am - addToContainer(tag, am) : 0;
    }

    @Override
    public synchronized Aspect getEssentiaType(ForgeDirection face) {
        return getAspect();
    }

    @Override
    public synchronized int getEssentiaAmount(ForgeDirection face) {
        return amount;
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }

    @Override
    public boolean renderExtendedTube() {
        return false;
    }

    // ---------------- 存档 ----------------

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        aspect = compound.hasKey("aspect") ? Aspect.getAspect(compound.getString("aspect")) : null;
        amount = aspect == null ? 0 : compound.getInteger("amount");
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        if (aspect != null && amount > 0) {
            compound.setString("aspect", aspect.getTag());
            compound.setInteger("amount", amount);
        }
    }

    @Override
    public void update() {
        if (worldObj == null || worldObj.isRemote || !(this instanceof Input) || ++tick % 2 != 0) {
            return;
        }
        if (amount >= getCapacity()) {
            return;
        }
        for (ForgeDirection face : ForgeDirection.VALID_DIRECTIONS) {
            pull(face);
        }
    }

    /** 同神秘时代 4 的源质罐（TileJarFillable#fillJar）：吸力高于相邻方块时，从它那里吸入 1 点源质。 */
    private void pull(ForgeDirection face) {
        TileEntity te = ThaumcraftApiHelper.getConnectableTile(worldObj, xCoord, yCoord, zCoord, face);
        if (!(te instanceof IEssentiaTransport ic)) {
            return;
        }
        ForgeDirection opposite = face.getOpposite();
        if (!ic.canOutputTo(opposite)) {
            return;
        }
        Aspect ta;
        Aspect current = getAspect();
        if (current != null) {
            ta = current;
        } else if (ic.getEssentiaAmount(opposite) > 0 && ic.getSuctionAmount(opposite) < getSuctionAmount(face)
            && getSuctionAmount(face) >= ic.getMinimumSuction()) {
            ta = ic.getEssentiaType(opposite);
        } else {
            return;
        }
        if (ta != null && ic.getSuctionAmount(opposite) < getSuctionAmount(face)) {
            int taken = ic.takeEssentia(ta, 1, opposite);
            if (taken > 0) {
                addToContainer(ta, taken);
            }
        }
    }

    public static class Input extends TileAspectProvider {

        @Override
        public int getSuctionAmount(ForgeDirection face) {
            return amount < getCapacity() ? 128 : 0;
        }

        @Override
        public boolean canOutputTo(ForgeDirection face) {
            return false;
        }

        @Override
        public boolean canGroupInput() {
            return true;
        }

        @Override
        public MachineComponentAspectProvider provideComponent() {
            return new MachineComponentAspectProvider(IOType.INPUT, new AspectProviderCopy(this));
        }
    }

    public static class Output extends TileAspectProvider {

        @Override
        public int getSuctionAmount(ForgeDirection face) {
            return 0;
        }

        @Override
        public boolean canInputFrom(ForgeDirection face) {
            return false;
        }

        @Override
        public MachineComponentAspectProvider provideComponent() {
            return new MachineComponentAspectProvider(IOType.OUTPUT, new AspectProviderCopy(this));
        }
    }
}
