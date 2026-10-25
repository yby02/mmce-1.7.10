package hellfirepvp.modularmachinery.client.util;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 「所需方块」列表的合并规则。
 * <p>
 * 关键回归点：{@code BlockArray#getIngredientList()} 已把“只接受一种方块”的位置合并过、数量存在 stackSize；
 * 合并多选择位置时若按条目个数计数，会把这些数量重置成 1（曾经的实际 bug）。
 */
class StructureIngredientMergerTest {

    private static Item a;
    private static Item b;
    private static Item c;

    @BeforeAll
    static void createItems() {
        a = new Item().setUnlocalizedName("test_a");
        b = new Item().setUnlocalizedName("test_b");
        c = new Item().setUnlocalizedName("test_c");
    }

    /** 已合并的单选择条目：数量必须原样保留。 */
    @Test
    void keepsExistingStackSizeOfSingleChoiceEntries() {
        List<List<ItemStack>> input = new ArrayList<>();
        input.add(new ArrayList<>(Arrays.asList(new ItemStack(a, 9))));

        List<List<ItemStack>> merged = StructureIngredientMerger.merge(input);

        assertEquals(1, merged.size());
        assertEquals(9, merged.get(0).get(0).stackSize, "单选择条目的数量不应被重置");
    }

    /** 9 处都接受同一组替选：合并为一条，数量 9，组内每个替选都带同样的数量。 */
    @Test
    void mergesIdenticalAlternativeGroupsAndSumsCount() {
        List<List<ItemStack>> input = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            input.add(new ArrayList<>(Arrays.asList(new ItemStack(a), new ItemStack(b))));
        }

        List<List<ItemStack>> merged = StructureIngredientMerger.merge(input);

        assertEquals(1, merged.size(), "9 处相同替选应合并为 1 条");
        List<ItemStack> group = merged.get(0);
        assertEquals(2, group.size());
        assertEquals(9, group.get(0).stackSize);
        assertEquals(9, group.get(1).stackSize, "轮换时整组数量应一致");
    }

    /** 替选顺序不同视为同一组。 */
    @Test
    void alternativeOrderDoesNotMatter() {
        List<List<ItemStack>> input = new ArrayList<>();
        input.add(new ArrayList<>(Arrays.asList(new ItemStack(a), new ItemStack(b))));
        input.add(new ArrayList<>(Arrays.asList(new ItemStack(b), new ItemStack(a))));

        List<List<ItemStack>> merged = StructureIngredientMerger.merge(input);

        assertEquals(1, merged.size());
        assertEquals(2, merged.get(0).get(0).stackSize);
    }

    /** 不同组合不合并，各自数量独立。 */
    @Test
    void differentGroupsStaySeparate() {
        List<List<ItemStack>> input = new ArrayList<>();
        input.add(new ArrayList<>(Arrays.asList(new ItemStack(a, 3))));
        input.add(new ArrayList<>(Arrays.asList(new ItemStack(b), new ItemStack(c))));
        input.add(new ArrayList<>(Arrays.asList(new ItemStack(b), new ItemStack(c))));

        List<List<ItemStack>> merged = StructureIngredientMerger.merge(input);

        assertEquals(2, merged.size());
        assertEquals(3, merged.get(0).get(0).stackSize);
        assertEquals(2, merged.get(1).get(0).stackSize);
    }

    /** meta 参与分组键：同物品不同 meta 不能合并。 */
    @Test
    void metaSeparatesGroups() {
        List<List<ItemStack>> input = new ArrayList<>();
        input.add(new ArrayList<>(Arrays.asList(new ItemStack(a, 1, 0))));
        input.add(new ArrayList<>(Arrays.asList(new ItemStack(a, 1, 1))));

        List<List<ItemStack>> merged = StructureIngredientMerger.merge(input);

        assertEquals(2, merged.size());
        assertTrue(merged.get(0).get(0).getItemDamage() != merged.get(1).get(0).getItemDamage());
    }
}
