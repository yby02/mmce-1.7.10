package github.kasuminova.mmce.compat.items;

import net.minecraft.item.ItemStack;

/**
 * 1.7.10 移植适配：与 Forge 1.12.2 net.minecraftforge.items.IItemHandler 相同的接口形状，供 MMCE 内部库存逻辑使用。
 * 区别：1.7.10 中空栈为 null（见 {@link github.kasuminova.mmce.compat.ItemStacks}）。
 * 与外部模组交互时，由 TileEntity 实现 ISidedInventory 并委托到本接口（见 {@link ItemHandlerInventory}）。
 */
public interface IItemHandler {

    int getSlots();

    /** 返回槽位中的物品；空槽返回 null。调用方不得修改返回值。 */
    ItemStack getStackInSlot(int slot);

    /** 返回未能插入的剩余部分；全部插入时返回 null。 */
    ItemStack insertItem(int slot, ItemStack stack, boolean simulate);

    /** 返回取出的物品；未取出时返回 null。 */
    ItemStack extractItem(int slot, int amount, boolean simulate);

    int getSlotLimit(int slot);

    default boolean isItemValid(int slot, ItemStack stack) {
        return true;
    }
}
