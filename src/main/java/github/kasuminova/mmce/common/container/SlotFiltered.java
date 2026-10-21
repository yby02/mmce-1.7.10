package github.kasuminova.mmce.common.container;

import github.kasuminova.mmce.compat.ItemStacks;
import github.kasuminova.mmce.compat.items.IItemHandler;
import github.kasuminova.mmce.compat.items.SlotItemHandler;
import net.minecraft.item.ItemStack;

import java.util.function.Predicate;

/** 只接受满足条件的物品的格（AE2 样板、容量卡）；不像 SlotItemHandler 那样试放来判断，避免触发物品栏监听。 */
public class SlotFiltered extends SlotItemHandler {

    private final Predicate<ItemStack> filter;

    public SlotFiltered(IItemHandler itemHandler, int index, int xPosition, int yPosition, Predicate<ItemStack> filter) {
        super(itemHandler, index, xPosition, yPosition);
        this.filter = filter;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return !ItemStacks.isEmpty(stack) && filter.test(stack);
    }
}
