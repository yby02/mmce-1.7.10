package github.kasuminova.mmce.compat.items;

import github.kasuminova.mmce.compat.ItemStacks;
import net.minecraft.item.ItemStack;

/**
 * 1.7.10 移植适配：对应 Forge 1.12.2 net.minecraftforge.items.ItemHandlerHelper（MMCE 用到的部分）。
 */
public final class ItemHandlerHelper {

    private ItemHandlerHelper() {
    }

    public static ItemStack insertItem(IItemHandler dest, ItemStack stack, boolean simulate) {
        if (dest == null || ItemStacks.isEmpty(stack)) {
            return stack;
        }
        for (int i = 0; i < dest.getSlots(); i++) {
            stack = dest.insertItem(i, stack, simulate);
            if (ItemStacks.isEmpty(stack)) {
                return ItemStacks.EMPTY;
            }
        }
        return stack;
    }

    /** 对应 1.12.2 ItemHandlerHelper.canItemStacksStack：物品、meta、NBT 完全相同（不检查可堆叠性与数量）。 */
    public static boolean canItemStacksStack(ItemStack a, ItemStack b) {
        if (ItemStacks.isEmpty(a) || ItemStacks.isEmpty(b) || a.getItem() != b.getItem() || a.getItemDamage() != b.getItemDamage()) {
            return false;
        }
        if (a.hasTagCompound() != b.hasTagCompound()) {
            return false;
        }
        return !a.hasTagCompound() || a.getTagCompound().equals(b.getTagCompound());
    }

    /** 对应 1.12.2 ItemHandlerHelper.canItemStacksStackRelaxed：要求可堆叠，仅在有子类型时比较 meta。 */
    public static boolean canItemStacksStackRelaxed(ItemStack a, ItemStack b) {
        if (ItemStacks.isEmpty(a) || ItemStacks.isEmpty(b) || a.getItem() != b.getItem()) {
            return false;
        }
        if (!a.isStackable()) {
            return false;
        }
        if (a.getHasSubtypes() && a.getItemDamage() != b.getItemDamage()) {
            return false;
        }
        if (a.hasTagCompound() != b.hasTagCompound()) {
            return false;
        }
        return !a.hasTagCompound() || a.getTagCompound().equals(b.getTagCompound());
    }

    public static ItemStack copyStackWithSize(ItemStack stack, int size) {
        return ItemStacks.copyWithSize(stack, size);
    }

    public static ItemStack insertItemStacked(IItemHandler inventory, ItemStack stack, boolean simulate) {
        if (inventory == null || ItemStacks.isEmpty(stack)) {
            return stack;
        }
        if (!stack.isStackable()) {
            return insertItem(inventory, stack, simulate);
        }
        int sizeInventory = inventory.getSlots();
        for (int i = 0; i < sizeInventory; i++) {
            ItemStack slot = inventory.getStackInSlot(i);
            if (canItemStacksStackRelaxed(slot, stack)) {
                stack = inventory.insertItem(i, stack, simulate);
                if (ItemStacks.isEmpty(stack)) {
                    return ItemStacks.EMPTY;
                }
            }
        }
        for (int i = 0; i < sizeInventory; i++) {
            if (ItemStacks.isEmpty(inventory.getStackInSlot(i))) {
                stack = inventory.insertItem(i, stack, simulate);
                if (ItemStacks.isEmpty(stack)) {
                    return ItemStacks.EMPTY;
                }
            }
        }
        return stack;
    }

    /** 对应 1.12.2 ItemHandlerHelper.calcRedstoneFromInventory（比较器输出）。 */
    public static int calcRedstoneFromInventory(IItemHandler inv) {
        if (inv == null || inv.getSlots() <= 0) {
            return 0;
        }
        int itemsFound = 0;
        float proportion = 0.0F;
        for (int j = 0; j < inv.getSlots(); ++j) {
            ItemStack stack = inv.getStackInSlot(j);
            if (!ItemStacks.isEmpty(stack)) {
                proportion += (float) stack.stackSize / (float) Math.max(1, Math.min(inv.getSlotLimit(j), stack.getMaxStackSize()));
                ++itemsFound;
            }
        }
        proportion = proportion / (float) inv.getSlots();
        return (int) Math.floor(proportion * 14.0F) + (itemsFound > 0 ? 1 : 0);
    }
}
