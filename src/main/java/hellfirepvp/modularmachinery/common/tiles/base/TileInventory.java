/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.tiles.base;

import hellfirepvp.modularmachinery.common.util.IOInventory;
import net.minecraft.nbt.NBTTagCompound;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: TileInventory
 * Created by HellFirePvP
 * Date: 28.06.2017 / 17:42
 * <p>
 * 1.7.10 移植：通过 {@link IOInventoryTile}（ISidedInventory）对外暴露 {@link IOInventory}。
 */
public abstract class TileInventory extends TileEntityRestrictedTick implements IOInventoryTile {

    protected IOInventory inventory;

    public TileInventory() {
        this.requireUpdateComparatorLevel = true;
    }

    public TileInventory(int size) {
        this.inventory = buildInventory(this, size);
    }

    public abstract IOInventory buildInventory(TileInventory tile, int size);

    public IOInventory getInventory() {
        return inventory;
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        this.inventory = IOInventory.deserialize(this, compound.getCompoundTag("inventory"));
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        compound.setTag("inventory", this.inventory.writeNBT());
    }

    /** 漏斗等直接修改 getStackInSlot 返回的物品栈后会调用 markDirty，这里转为 MMCE 的同步更新。 */
    @Override
    public void markDirty() {
        super.markDirty();
        markNoUpdateSync();
    }
}
