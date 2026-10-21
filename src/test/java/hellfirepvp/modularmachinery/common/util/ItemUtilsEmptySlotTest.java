package hellfirepvp.modularmachinery.common.util;

import github.kasuminova.mmce.compat.items.IItemHandlerModifiable;
import github.kasuminova.mmce.compat.items.InventoryItemHandler;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 配料组消耗时，前一个候选已足量后，下一个候选以数量 0 去查找物品栏。1.7.10 的空槽是 null，
 * 修正前“空槽匹配数量为 0 的物品”后调用 s.getTagCompound() 抛 NPE，使整个配方搜索中断（自测 adapters 中矿辞配方不运行）。
 */
class ItemUtilsEmptySlotTest {

    @Test
    void emptySlotsNeverMatch() {
        Item ingot = new Item().setMaxStackSize(64);
        InventoryBasic inv = new InventoryBasic("test", false, 3);
        inv.setInventorySlotContents(0, new ItemStack(ingot, 2));
        IItemHandlerModifiable handler = new InventoryItemHandler(inv, null);

        // 数量为 0 的匹配物：修正前空槽（null）被当作匹配并抛 NPE；现在空槽跳过，非空槽与“空”匹配物也不相等
        Int2ObjectMap<ItemStack> zeroCount = ItemUtils.findItemsIndexedInInventory(handler, new ItemStack(ingot, 0), true, null);
        assertTrue(zeroCount.isEmpty());

        Int2ObjectMap<ItemStack> one = ItemUtils.findItemsIndexedInInventory(handler, new ItemStack(ingot, 1), true, null);
        assertEquals(1, one.size());
        assertTrue(one.containsKey(0));

        Item other = new Item().setMaxStackSize(64);
        Int2ObjectMap<ItemStack> none = ItemUtils.findItemsIndexedInInventory(handler, new ItemStack(other, 0), true, null);
        assertTrue(none.isEmpty());
    }
}
