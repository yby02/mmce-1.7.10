package github.kasuminova.mmce.compat;

import net.minecraft.item.ItemStack;

/**
 * 1.7.10 移植适配：1.12.2 的 ItemStack 永不为 null（空栈为 ItemStack.EMPTY），并提供 isEmpty/getCount/grow/shrink；
 * 1.7.10 用 null 表示空栈且只有 stackSize 字段。上游代码中的这些调用统一改写为本类的静态方法。
 */
public final class ItemStacks {

    /** 1.7.10 中空栈即 null。 */
    public static final ItemStack EMPTY = null;

    private ItemStacks() {
    }

    public static boolean isEmpty(ItemStack stack) {
        return stack == null || stack.getItem() == null || stack.stackSize <= 0;
    }

    public static int getCount(ItemStack stack) {
        return isEmpty(stack) ? 0 : stack.stackSize;
    }

    public static void setCount(ItemStack stack, int count) {
        if (stack != null) {
            stack.stackSize = count;
        }
    }

    public static void grow(ItemStack stack, int amount) {
        if (stack != null) {
            stack.stackSize += amount;
        }
    }

    public static void shrink(ItemStack stack, int amount) {
        if (stack != null) {
            stack.stackSize -= amount;
        }
    }

    public static ItemStack copy(ItemStack stack) {
        return stack == null ? null : stack.copy();
    }

    public static ItemStack copyWithSize(ItemStack stack, int size) {
        if (isEmpty(stack) || size <= 0) {
            return EMPTY;
        }
        ItemStack copy = stack.copy();
        copy.stackSize = size;
        return copy;
    }

    /** 空栈规范化为 null，便于存储。 */
    public static ItemStack normalize(ItemStack stack) {
        return isEmpty(stack) ? EMPTY : stack;
    }

    /** 对应 1.12.2 ItemStack.areItemsEqual：物品与 meta 相同（不比较 NBT 与数量）。 */
    public static boolean areItemsEqual(ItemStack a, ItemStack b) {
        if (isEmpty(a) || isEmpty(b)) {
            return isEmpty(a) && isEmpty(b);
        }
        return a.getItem() == b.getItem() && a.getItemDamage() == b.getItemDamage();
    }

    /** 对应 1.12.2 ItemStack.areItemStackTagsEqual。 */
    public static boolean areItemStackTagsEqual(ItemStack a, ItemStack b) {
        if (isEmpty(a) && isEmpty(b)) {
            return true;
        }
        if (isEmpty(a) || isEmpty(b)) {
            return false;
        }
        return ItemStack.areItemStackTagsEqual(a, b);
    }

    /** 对应 1.12.2 ItemStack.areItemStacksEqual（含数量）。 */
    public static boolean areItemStacksEqual(ItemStack a, ItemStack b) {
        if (isEmpty(a) && isEmpty(b)) {
            return true;
        }
        if (isEmpty(a) || isEmpty(b)) {
            return false;
        }
        return ItemStack.areItemStacksEqual(a, b);
    }

    public static int getMaxStackSize(ItemStack stack) {
        return isEmpty(stack) ? 64 : stack.getMaxStackSize();
    }

    /** 对应 1.12.2 ForgeHooks.getContainerItem：返回合成后留下的容器物品，无容器或已损坏时返回空。 */
    public static ItemStack getContainerItem(ItemStack stack) {
        if (isEmpty(stack) || !stack.getItem().hasContainerItem(stack)) {
            return EMPTY;
        }
        ItemStack container = stack.getItem().getContainerItem(stack);
        if (!isEmpty(container) && container.isItemStackDamageable() && container.getItemDamage() > container.getMaxDamage()) {
            return EMPTY;
        }
        return normalize(container);
    }
}
