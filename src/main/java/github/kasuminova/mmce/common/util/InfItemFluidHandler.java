package github.kasuminova.mmce.common.util;

import github.kasuminova.mmce.compat.ItemStacks;
import github.kasuminova.mmce.compat.fluids.FluidTankProperties;
import github.kasuminova.mmce.compat.fluids.IFluidHandler;
import github.kasuminova.mmce.compat.fluids.IFluidTankProperties;
import github.kasuminova.mmce.compat.items.IItemHandlerModifiable;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * 容量不限的物品 + 流体处理器（ME 样板供应器向机器提供的输入组件）。
 * <p>
 * 1.7.10 移植：取自上游 github.kasuminova.mmce.common.util.InfItemFluidHandler，改为实现移植版的
 * {@link IItemHandlerModifiable} / {@link IFluidHandler}（空槽为 null）。Mekanism 气体部分依赖 MekEng（AE2 的气体扩展），
 * 1.7.10 没有对应模组，未移植。
 * <ul>
 *   <li>物品：可选的“子物品栏”排在前面（样板供应器界面中可手动放入的 2 格），其后是不限数量的列表。</li>
 *   <li>流体：列表中每种流体一项，数量不限；可选的“子流体处理器”在抽取时优先使用。</li>
 * </ul>
 */
public class InfItemFluidHandler implements IItemHandlerModifiable, IFluidHandler {

    protected final List<ItemStack>  itemStackList  = new ArrayList<>();
    protected final List<FluidStack> fluidStackList = new ArrayList<>();

    protected volatile IItemHandlerModifiable subItemHandler;
    protected volatile IFluidHandler          subFluidHandler;
    protected volatile IntConsumer            onItemChanged;
    protected volatile IntConsumer            onFluidChanged;

    public InfItemFluidHandler() {
    }

    public InfItemFluidHandler(IItemHandlerModifiable subItemHandler, IFluidHandler subFluidHandler) {
        this.subItemHandler = subItemHandler;
        this.subFluidHandler = subFluidHandler;
    }

    private void itemChanged(int slot) {
        IntConsumer c = onItemChanged;
        if (c != null) {
            c.accept(slot);
        }
    }

    private void fluidChanged(int slot) {
        IntConsumer c = onFluidChanged;
        if (c != null) {
            c.accept(slot);
        }
    }

    // ---------------- 流体 ----------------

    @Override
    public synchronized IFluidTankProperties[] getTankProperties() {
        List<IFluidTankProperties> list = new ArrayList<>();
        for (FluidStack stack : fluidStackList) {
            list.add(new FluidTankProperties(stack, Integer.MAX_VALUE));
        }
        if (subFluidHandler != null) {
            for (IFluidTankProperties p : subFluidHandler.getTankProperties()) {
                list.add(p);
            }
        }
        return list.toArray(new IFluidTankProperties[0]);
    }

    @Override
    public synchronized int fill(FluidStack resource, boolean doFill) {
        if (resource == null || resource.amount <= 0) {
            return 0;
        }
        if (!doFill) {
            return resource.amount;
        }
        int toFill = resource.amount;
        for (int i = 0; i < fluidStackList.size() && toFill > 0; i++) {
            FluidStack inSlot = fluidStackList.get(i);
            if (inSlot == null) {
                fluidStackList.set(i, new FluidStack(resource, toFill));
                fluidChanged(i);
                toFill = 0;
            } else if (inSlot.isFluidEqual(resource)) {
                int canFill = Math.min(toFill, Integer.MAX_VALUE - inSlot.amount);
                inSlot.amount += canFill;
                toFill -= canFill;
                fluidChanged(i);
            }
        }
        if (toFill > 0) {
            fluidStackList.add(new FluidStack(resource, toFill));
            fluidChanged(fluidStackList.size() - 1);
        }
        return resource.amount;
    }

    @Override
    public synchronized FluidStack drain(FluidStack resource, boolean doDrain) {
        if (resource == null) {
            return null;
        }
        if (subFluidHandler != null) {
            FluidStack drained = subFluidHandler.drain(resource, false);
            if (drained != null && drained.amount >= resource.amount) {
                return doDrain ? subFluidHandler.drain(resource, true) : drained;
            }
        }
        for (int i = 0; i < fluidStackList.size(); i++) {
            FluidStack inSlot = fluidStackList.get(i);
            if (inSlot != null && inSlot.isFluidEqual(resource)) {
                int canDrain = Math.min(inSlot.amount, resource.amount);
                if (doDrain) {
                    inSlot.amount -= canDrain;
                    if (inSlot.amount <= 0) {
                        fluidStackList.set(i, null);
                    }
                    fluidChanged(i);
                }
                return new FluidStack(resource, canDrain);
            }
        }
        return null;
    }

    @Override
    public synchronized FluidStack drain(int maxDrain, boolean doDrain) {
        if (subFluidHandler != null) {
            FluidStack drained = subFluidHandler.drain(maxDrain, doDrain);
            if (drained != null) {
                return drained;
            }
        }
        for (int i = 0; i < fluidStackList.size(); i++) {
            FluidStack inSlot = fluidStackList.get(i);
            if (inSlot != null) {
                int canDrain = Math.min(inSlot.amount, maxDrain);
                FluidStack out = new FluidStack(inSlot, canDrain);
                if (doDrain) {
                    inSlot.amount -= canDrain;
                    if (inSlot.amount <= 0) {
                        fluidStackList.set(i, null);
                    }
                    fluidChanged(i);
                }
                return out;
            }
        }
        return null;
    }

    // ---------------- 物品 ----------------

    private int subSlots() {
        IItemHandlerModifiable sub = subItemHandler;
        return sub == null ? 0 : sub.getSlots();
    }

    @Override
    public int getSlots() {
        return itemStackList.size() + subSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        int sub = subSlots();
        if (slot < sub) {
            return subItemHandler.getStackInSlot(slot);
        }
        int trueSlot = slot - sub;
        return trueSlot < itemStackList.size() ? itemStackList.get(trueSlot) : null;
    }

    @Override
    public synchronized void setStackInSlot(int slot, ItemStack stack) {
        int sub = subSlots();
        if (slot < sub) {
            subItemHandler.setStackInSlot(slot, stack);
            return;
        }
        int trueSlot = slot - sub;
        ItemStack value = ItemStacks.isEmpty(stack) ? null : stack;
        if (trueSlot >= itemStackList.size()) {
            itemStackList.add(value);
        } else {
            itemStackList.set(trueSlot, value);
        }
        itemChanged(trueSlot);
    }

    @Override
    public synchronized ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (ItemStacks.isEmpty(stack)) {
            return null;
        }
        int sub = subSlots();
        if (slot < sub) {
            return subItemHandler.insertItem(slot, stack, simulate);
        }
        int trueSlot = slot - sub;
        if (trueSlot >= itemStackList.size()) {
            return stack;
        }
        ItemStack inSlot = itemStackList.get(trueSlot);
        if (ItemStacks.isEmpty(inSlot)) {
            if (!simulate) {
                itemStackList.set(trueSlot, stack.copy());
                itemChanged(trueSlot);
            }
            return null;
        }
        if (inSlot.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(inSlot, stack)) {
            int toInsert = Math.min(stack.stackSize, Integer.MAX_VALUE - inSlot.stackSize);
            if (!simulate) {
                inSlot.stackSize += toInsert;
                itemChanged(trueSlot);
            }
            if (toInsert >= stack.stackSize) {
                return null;
            }
            ItemStack rest = stack.copy();
            rest.stackSize -= toInsert;
            return rest;
        }
        return stack;
    }

    /** 追加物品：先合并到相同物品的格，否则放进第一个空格或新增一格（样板推送时使用）。 */
    public synchronized void appendItem(ItemStack stack) {
        if (ItemStacks.isEmpty(stack)) {
            return;
        }
        int emptySlot = -1;
        for (int i = 0; i < itemStackList.size(); i++) {
            ItemStack inSlot = itemStackList.get(i);
            if (ItemStacks.isEmpty(inSlot)) {
                if (emptySlot < 0) {
                    emptySlot = i;
                }
                continue;
            }
            if (inSlot.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(inSlot, stack)
                && (long) inSlot.stackSize + stack.stackSize <= Integer.MAX_VALUE) {
                inSlot.stackSize += stack.stackSize;
                itemChanged(i);
                return;
            }
        }
        if (emptySlot >= 0) {
            itemStackList.set(emptySlot, stack.copy());
            itemChanged(emptySlot);
        } else {
            itemStackList.add(stack.copy());
            itemChanged(itemStackList.size() - 1);
        }
    }

    @Override
    public synchronized ItemStack extractItem(int slot, int amount, boolean simulate) {
        int sub = subSlots();
        if (slot < sub) {
            return subItemHandler.extractItem(slot, amount, simulate);
        }
        int trueSlot = slot - sub;
        if (amount <= 0 || trueSlot >= itemStackList.size()) {
            return null;
        }
        ItemStack inSlot = itemStackList.get(trueSlot);
        if (ItemStacks.isEmpty(inSlot)) {
            return null;
        }
        int toExtract = Math.min(amount, inSlot.stackSize);
        ItemStack out = inSlot.copy();
        out.stackSize = toExtract;
        if (!simulate) {
            inSlot.stackSize -= toExtract;
            if (inSlot.stackSize <= 0) {
                itemStackList.set(trueSlot, null);
            }
            itemChanged(trueSlot);
        }
        return out;
    }

    @Override
    public int getSlotLimit(int slot) {
        return Integer.MAX_VALUE;
    }

    public synchronized boolean isEmpty() {
        for (ItemStack stack : itemStackList) {
            if (!ItemStacks.isEmpty(stack)) {
                return false;
            }
        }
        for (FluidStack stack : fluidStackList) {
            if (stack != null && stack.amount > 0) {
                return false;
            }
        }
        return true;
    }

    public List<ItemStack> getItemStackList() {
        return itemStackList;
    }

    public List<FluidStack> getFluidStackList() {
        return fluidStackList;
    }

    public void setOnItemChanged(IntConsumer onItemChanged) {
        this.onItemChanged = onItemChanged;
    }

    public void setOnFluidChanged(IntConsumer onFluidChanged) {
        this.onFluidChanged = onFluidChanged;
    }

    // ---------------- NBT ----------------

    public synchronized void writeToNBT(NBTTagCompound tag, String subTagName) {
        NBTTagCompound sub = new NBTTagCompound();
        NBTTagList items = new NBTTagList();
        for (ItemStack stack : itemStackList) {
            if (!ItemStacks.isEmpty(stack)) {
                NBTTagCompound itemTag = new NBTTagCompound();
                stack.writeToNBT(itemTag);
                // ItemStack 在 NBT 中只存一个字节的数量，这里另存完整数量
                itemTag.setInteger("FullCount", stack.stackSize);
                items.appendTag(itemTag);
            }
        }
        sub.setTag("items", items);
        NBTTagList fluids = new NBTTagList();
        for (FluidStack stack : fluidStackList) {
            if (stack != null && stack.amount > 0) {
                fluids.appendTag(stack.writeToNBT(new NBTTagCompound()));
            }
        }
        sub.setTag("fluids", fluids);
        tag.setTag(subTagName, sub);
    }

    public synchronized void readFromNBT(NBTTagCompound tag, String subTagName) {
        itemStackList.clear();
        fluidStackList.clear();
        if (!tag.hasKey(subTagName)) {
            return;
        }
        NBTTagCompound sub = tag.getCompoundTag(subTagName);
        NBTTagList items = sub.getTagList("items", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < items.tagCount(); i++) {
            NBTTagCompound itemTag = items.getCompoundTagAt(i);
            ItemStack stack = ItemStack.loadItemStackFromNBT(itemTag);
            if (stack != null) {
                if (itemTag.hasKey("FullCount")) {
                    stack.stackSize = itemTag.getInteger("FullCount");
                }
                itemStackList.add(stack);
            }
        }
        NBTTagList fluids = sub.getTagList("fluids", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < fluids.tagCount(); i++) {
            FluidStack stack = FluidStack.loadFluidStackFromNBT(fluids.getCompoundTagAt(i));
            if (stack != null) {
                fluidStackList.add(stack);
            }
        }
    }
}
