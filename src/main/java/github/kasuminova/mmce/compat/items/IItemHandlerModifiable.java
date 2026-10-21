package github.kasuminova.mmce.compat.items;

import net.minecraft.item.ItemStack;

/**
 * 1.7.10 移植适配：对应 Forge 1.12.2 net.minecraftforge.items.IItemHandlerModifiable。
 */
public interface IItemHandlerModifiable extends IItemHandler {

    void setStackInSlot(int slot, ItemStack stack);
}
