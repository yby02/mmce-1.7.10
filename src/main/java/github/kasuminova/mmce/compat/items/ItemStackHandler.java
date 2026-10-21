package github.kasuminova.mmce.compat.items;

import github.kasuminova.mmce.compat.ItemStacks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/**
 * 1.7.10 移植适配：对应 Forge 1.12.2 net.minecraftforge.items.ItemStackHandler（null 表示空槽）。
 */
public class ItemStackHandler implements IItemHandlerModifiable {

    protected ItemStack[] stacks;

    public ItemStackHandler() {
        this(1);
    }

    public ItemStackHandler(int size) {
        stacks = new ItemStack[size];
    }

    public void setSize(int size) {
        stacks = new ItemStack[size];
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        validateSlotIndex(slot);
        stacks[slot] = ItemStacks.normalize(stack);
        onContentsChanged(slot);
    }

    @Override
    public int getSlots() {
        return stacks.length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        validateSlotIndex(slot);
        return stacks[slot];
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (ItemStacks.isEmpty(stack)) {
            return ItemStacks.EMPTY;
        }
        if (!isItemValid(slot, stack)) {
            return stack;
        }
        validateSlotIndex(slot);
        ItemStack existing = stacks[slot];
        int limit = getStackLimit(slot, stack);
        if (!ItemStacks.isEmpty(existing)) {
            if (!ItemHandlerHelper.canItemStacksStack(stack, existing)) {
                return stack;
            }
            limit -= existing.stackSize;
        }
        if (limit <= 0) {
            return stack;
        }
        boolean reachedLimit = stack.stackSize > limit;
        if (!simulate) {
            if (ItemStacks.isEmpty(existing)) {
                stacks[slot] = reachedLimit ? ItemStacks.copyWithSize(stack, limit) : stack.copy();
            } else {
                existing.stackSize += reachedLimit ? limit : stack.stackSize;
            }
            onContentsChanged(slot);
        }
        return reachedLimit ? ItemStacks.copyWithSize(stack, stack.stackSize - limit) : ItemStacks.EMPTY;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0) {
            return ItemStacks.EMPTY;
        }
        validateSlotIndex(slot);
        ItemStack existing = stacks[slot];
        if (ItemStacks.isEmpty(existing)) {
            return ItemStacks.EMPTY;
        }
        int toExtract = Math.min(amount, existing.getMaxStackSize());
        if (existing.stackSize <= toExtract) {
            if (!simulate) {
                stacks[slot] = ItemStacks.EMPTY;
                onContentsChanged(slot);
            }
            return simulate ? existing.copy() : existing;
        }
        if (!simulate) {
            stacks[slot] = ItemStacks.copyWithSize(existing, existing.stackSize - toExtract);
            onContentsChanged(slot);
        }
        return ItemStacks.copyWithSize(existing, toExtract);
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    protected int getStackLimit(int slot, ItemStack stack) {
        return Math.min(getSlotLimit(slot), stack.getMaxStackSize());
    }

    public NBTTagCompound serializeNBT() {
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < stacks.length; i++) {
            if (!ItemStacks.isEmpty(stacks[i])) {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setInteger("Slot", i);
                stacks[i].writeToNBT(tag);
                list.appendTag(tag);
            }
        }
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setTag("Items", list);
        nbt.setInteger("Size", stacks.length);
        return nbt;
    }

    public void deserializeNBT(NBTTagCompound nbt) {
        setSize(nbt.hasKey("Size") ? nbt.getInteger("Size") : stacks.length);
        NBTTagList list = nbt.getTagList("Items", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            int slot = tag.getInteger("Slot");
            if (slot >= 0 && slot < stacks.length) {
                stacks[slot] = ItemStack.loadItemStackFromNBT(tag);
            }
        }
        onLoad();
    }

    protected void validateSlotIndex(int slot) {
        if (slot < 0 || slot >= stacks.length) {
            throw new RuntimeException("Slot " + slot + " not in valid range - [0," + stacks.length + ")");
        }
    }

    protected void onLoad() {
    }

    protected void onContentsChanged(int slot) {
    }
}
