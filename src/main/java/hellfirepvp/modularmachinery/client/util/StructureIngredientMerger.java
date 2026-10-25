package hellfirepvp.modularmachinery.client.util;

import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 把结构预览的「所需方块」条目按可接受组合同类合并。
 * <p>
 * {@code BlockArray#getIngredientList()} 已经做过一次合并：<b>只接受一种方块</b>的位置会被归并，数量累加在
 * {@code stackSize} 里；而<b>可接受多种方块</b>的位置是一处一条。后者在 NEI 里会显示成一长串同样的格子，
 * 所以这里再合并一次：把「可接受集合相同」的条目并成一条，数量 = 各条目 {@code stackSize} 之和。
 * <p>
 * 注意数量必须累加已有 {@code stackSize}，不能数条目个数 —— 否则会把第一种情况已经算好的数量重置成 1。
 * 合并后同一组内每个替选的 {@code stackSize} 都设为总数，NEI 轮换时会整组同步变化。
 */
public final class StructureIngredientMerger {

    private StructureIngredientMerger() {
    }

    /** @return 合并后的条目；每组内每个 ItemStack 的 stackSize 为该组的总数量 */
    public static List<List<ItemStack>> merge(List<List<ItemStack>> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, List<ItemStack>> representatives = new LinkedHashMap<>();
        Map<String, Integer> totals = new LinkedHashMap<>();

        for (List<ItemStack> alternatives : ingredients) {
            if (alternatives == null || alternatives.isEmpty()) {
                continue;
            }
            List<ItemStack> clean = new ArrayList<>(alternatives.size());
            for (ItemStack stack : alternatives) {
                if (stack != null && stack.getItem() != null) {
                    clean.add(stack);
                }
            }
            if (clean.isEmpty()) {
                continue;
            }
            String key = keyOf(clean);
            int amount = Math.max(1, clean.get(0).stackSize);
            if (!representatives.containsKey(key)) {
                representatives.put(key, clean);
                totals.put(key, amount);
            } else {
                totals.put(key, totals.get(key) + amount);
            }
        }

        List<List<ItemStack>> out = new ArrayList<>(representatives.size());
        for (Map.Entry<String, List<ItemStack>> entry : representatives.entrySet()) {
            int total = totals.get(entry.getKey());
            List<ItemStack> merged = new ArrayList<>(entry.getValue().size());
            for (ItemStack stack : entry.getValue()) {
                ItemStack copy = stack.copy();
                copy.stackSize = total;
                merged.add(copy);
            }
            out.add(merged);
        }
        return out;
    }

    /** 组合键：按 item + meta 排序后拼接，顺序不同视为同一组。 */
    public static String keyOf(List<ItemStack> alternatives) {
        List<String> ids = new ArrayList<>(alternatives.size());
        for (ItemStack stack : alternatives) {
            ids.add(stack.getItem().getUnlocalizedName() + "@" + stack.getItemDamage());
        }
        Collections.sort(ids);
        return String.join("|", ids);
    }
}
