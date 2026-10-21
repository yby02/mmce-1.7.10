package github.kasuminova.mmce.compat.items;

import github.kasuminova.mmce.compat.EnumFacing;
import github.kasuminova.mmce.compat.ItemStacks;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 1.7.10 的 IInventory / ISidedInventory 包装（对应 Forge 1.12.2 InvWrapper / SidedInvWrapper）以及堆叠判定。
 * 物品总线自动导入导出通过它访问相邻容器。
 */
class InventoryItemHandlerTest {

    private static Item ingot;
    private static Item tool;

    @BeforeAll
    static void createItems() {
        ingot = new Item().setMaxStackSize(64);
        tool = new Item().setMaxStackSize(1).setMaxDamage(100);
    }

    @Test
    void insertMergesAndReturnsRemainder() {
        InventoryBasic inv = new InventoryBasic("test", false, 2);
        IItemHandlerModifiable handler = new InventoryItemHandler(inv, null);
        assertEquals(2, handler.getSlots());

        assertNull(handler.insertItem(0, new ItemStack(ingot, 40), false));
        ItemStack remain = handler.insertItem(0, new ItemStack(ingot, 40), false);
        assertEquals(64, inv.getStackInSlot(0).stackSize);
        assertEquals(16, remain.stackSize);

        // 模拟插入不改变容器
        ItemStack simulated = handler.insertItem(1, new ItemStack(ingot, 10), true);
        assertNull(simulated);
        assertNull(inv.getStackInSlot(1));
    }

    @Test
    void insertRefusesDifferentMeta() {
        InventoryBasic inv = new InventoryBasic("test", false, 1);
        IItemHandlerModifiable handler = new InventoryItemHandler(inv, null);
        handler.insertItem(0, new ItemStack(ingot, 1, 0), false);
        ItemStack other = new ItemStack(ingot, 5, 1);
        assertEquals(5, handler.insertItem(0, other, false).stackSize);
        assertEquals(1, inv.getStackInSlot(0).stackSize);
    }

    @Test
    void extractSimulatesAndRemoves() {
        InventoryBasic inv = new InventoryBasic("test", false, 1);
        inv.setInventorySlotContents(0, new ItemStack(ingot, 10));
        IItemHandlerModifiable handler = new InventoryItemHandler(inv, null);

        assertEquals(4, handler.extractItem(0, 4, true).stackSize);
        assertEquals(10, inv.getStackInSlot(0).stackSize);
        assertEquals(4, handler.extractItem(0, 4, false).stackSize);
        assertEquals(6, inv.getStackInSlot(0).stackSize);
        assertEquals(6, handler.extractItem(0, 64, false).stackSize);
        assertNull(inv.getStackInSlot(0));
        assertTrue(ItemStacks.isEmpty(handler.extractItem(0, 1, false)));
    }

    @Test
    void sidedInventoryRespectsFaceSlotsAndPermissions() {
        SidedInv inv = new SidedInv();
        IItemHandlerModifiable top = new InventoryItemHandler(inv, EnumFacing.UP);
        IItemHandlerModifiable bottom = new InventoryItemHandler(inv, EnumFacing.DOWN);
        assertEquals(1, top.getSlots());
        assertEquals(1, bottom.getSlots());

        // 顶面只能放入 0 号槽
        assertNull(top.insertItem(0, new ItemStack(ingot, 3), false));
        assertEquals(3, inv.getStackInSlot(0).stackSize);
        // 底面只能从 1 号槽取出，且不能放入
        assertEquals(2, bottom.insertItem(0, new ItemStack(ingot, 2), false).stackSize);
        inv.setInventorySlotContents(1, new ItemStack(ingot, 5));
        assertEquals(5, bottom.extractItem(0, 64, false).stackSize);
        // 顶面不允许取出
        assertTrue(ItemStacks.isEmpty(top.extractItem(0, 1, false)));
    }

    @Test
    void stackingRulesMatchForge112() {
        ItemStack a = new ItemStack(ingot, 1, 0);
        assertTrue(ItemHandlerHelper.canItemStacksStack(a, new ItemStack(ingot, 30, 0)));
        assertFalse(ItemHandlerHelper.canItemStacksStack(a, new ItemStack(ingot, 1, 2)));
        assertFalse(ItemHandlerHelper.canItemStacksStack(a, null));

        // 不可堆叠物品：canItemStacksStack 只比较物品/meta/NBT，Relaxed 版本要求可堆叠
        ItemStack t1 = new ItemStack(tool, 1, 5);
        assertTrue(ItemHandlerHelper.canItemStacksStack(t1, new ItemStack(tool, 1, 5)));
        assertFalse(ItemHandlerHelper.canItemStacksStackRelaxed(t1, new ItemStack(tool, 1, 5)));
    }

    @Test
    void comparatorOutputMatchesVanillaFormula() {
        InventoryBasic inv = new InventoryBasic("test", false, 2);
        IItemHandlerModifiable handler = new InventoryItemHandler(inv, null);
        assertEquals(0, ItemHandlerHelper.calcRedstoneFromInventory(handler));
        inv.setInventorySlotContents(0, new ItemStack(ingot, 64));
        // 一个满槽 + 一个空槽：floor(0.5 * 14) + 1 = 8
        assertEquals(8, ItemHandlerHelper.calcRedstoneFromInventory(handler));
        inv.setInventorySlotContents(1, new ItemStack(ingot, 64));
        assertEquals(15, ItemHandlerHelper.calcRedstoneFromInventory(handler));
    }

    /** 2 槽：顶面可放入 0 号槽，底面可取出 1 号槽。 */
    private static final class SidedInv extends InventoryBasic implements ISidedInventory {
        SidedInv() {
            super("sided", false, 2);
        }

        @Override
        public int[] getAccessibleSlotsFromSide(int side) {
            return side == 1 ? new int[]{0} : side == 0 ? new int[]{1} : new int[0];
        }

        @Override
        public boolean canInsertItem(int slot, ItemStack stack, int side) {
            return side == 1 && slot == 0;
        }

        @Override
        public boolean canExtractItem(int slot, ItemStack stack, int side) {
            return side == 0 && slot == 1;
        }
    }
}
