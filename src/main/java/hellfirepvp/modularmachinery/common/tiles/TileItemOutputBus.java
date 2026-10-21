/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.tiles;

import github.kasuminova.mmce.compat.Worlds;

import github.kasuminova.mmce.compat.ItemStacks;

import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.block.prop.ItemBusSize;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.tiles.base.MachineComponentTile;
import hellfirepvp.modularmachinery.common.tiles.base.TileInventory;
import hellfirepvp.modularmachinery.common.tiles.base.TileItemBus;
import hellfirepvp.modularmachinery.common.util.IOInventory;
import hellfirepvp.modularmachinery.common.util.ItemUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import github.kasuminova.mmce.compat.EnumFacing;
import github.kasuminova.mmce.compat.BlockPos;
import github.kasuminova.mmce.compat.items.InventoryItemHandler;
import github.kasuminova.mmce.compat.items.IItemHandler;

import javax.annotation.Nullable;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: TileItemOutputBus
 * Created by HellFirePvP
 * Date: 07.07.2017 / 18:41
 */
public class TileItemOutputBus extends TileItemBus implements MachineComponentTile {
    public static int minWorkDelay = 5;
    public static int maxWorkDelay = 60;

    public TileItemOutputBus() {
    }

    public TileItemOutputBus(ItemBusSize type) {
        super(type);
    }

    @Override
    public void doRestrictedTick() {
        if (getWorld().isRemote || !canWork(minWorkDelay, maxWorkDelay)) {
            return;
        }

        for (EnumFacing facing : EnumFacing.VALUES) {
            BlockPos offset = getPos().offset(facing);
            TileEntity te = Worlds.getTileEntity(getWorld(), offset);
            // 1.7.10 移植修正：上游只跳过物品总线，相邻的升级总线（同样对外提供物品栏）会被当作外部容器——
            // 输出总线把产物推进升级总线，输入总线把升级物品拉走
            if (te == null || te instanceof TileItemBus || te instanceof TileUpgradeBus) {
                continue;
            }

            EnumFacing accessingSide = facing.getOpposite();

            IItemHandler itemHandler = InventoryItemHandler.of(te, accessingSide);
            if (itemHandler == null) {
                continue;
            }

            try {
                outputToExternal(itemHandler);
            } catch (Exception e) {
                ModularMachinery.log.error("Exception when insert item: ", e);
            }
        }
    }

    private synchronized void outputToExternal(IItemHandler external) {
        boolean successAtLeastOnce = false;

        for (int externalSlotId = 0; externalSlotId < external.getSlots(); externalSlotId++) {
            ItemStack externalStack = external.getStackInSlot(externalSlotId);
            int slotLimit = external.getSlotLimit(externalSlotId);
            if (!ItemStacks.isEmpty(externalStack) && ItemStacks.getCount(externalStack) >= slotLimit) {
                continue;
            }

            for (int internalSlotId = 0; internalSlotId < inventory.getSlots(); internalSlotId++) {
                ItemStack internalStack = inventory.getStackInSlot(internalSlotId);
                if (ItemStacks.isEmpty(internalStack)) {
                    continue;
                }

                if (ItemStacks.isEmpty(externalStack)) {
                    ItemStack notInserted = external.insertItem(externalSlotId, internalStack, false);
                    // Safeguard against Storage Drawers virtual slot
                    if (ItemStacks.getCount(notInserted) == ItemStacks.getCount(internalStack)) {
                        break;
                    }
                    inventory.setStackInSlot(internalSlotId, notInserted);
                    successAtLeastOnce = true;
                    if (ItemStacks.isEmpty(notInserted)) {
                        break;
                    }
                    continue;
                }

                if (!ItemUtils.matchStacks(internalStack, externalStack)) {
                    continue;
                }

                // Extract internal item to external.
                ItemStack notInserted = external.insertItem(externalSlotId, internalStack, false);
                inventory.setStackInSlot(internalSlotId, notInserted);

                successAtLeastOnce = true;
                if (ItemStacks.isEmpty(notInserted)) {
                    break;
                }
            }
        }

        if (successAtLeastOnce) {
            incrementSuccessCounter(maxWorkDelay, minWorkDelay);
            super.markNoUpdate();
        } else {
            decrementSuccessCounter();
        }
    }

    @Override
    public void markNoUpdate() {
        super.markNoUpdate();
        inventoryChanged = true;
    }

    @Override
    public IOInventory buildInventory(TileInventory tile, int size) {
        int[] slots = new int[size];
        for (int i = 0; i < size; i++) {
            slots[i] = i;
        }
        return new IOInventory(tile, new int[]{}, slots);
    }

    @Nullable
    @Override
    public MachineComponent.ItemBus provideComponent() {
        return new MachineComponent.ItemBus(IOType.OUTPUT) {
            @Override
            public IOInventory getContainerProvider() {
                return TileItemOutputBus.this.inventory;
            }
        };
    }

}
