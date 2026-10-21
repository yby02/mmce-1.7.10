package github.kasuminova.mmce.common.util;

import github.kasuminova.mmce.compat.ItemStacks;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OredictCache {
    private static final Map<Integer, Map<Integer, int[]>> ORE_ID_CACHE_MAP = new ConcurrentHashMap<>();

    public static int[] getOreIDsFast(@Nonnull ItemStack stack) {
        if (ItemStacks.isEmpty(stack)) {
            return new int[0];
        }

        // 1.7.10 移植：用数字 ID 代替 IRegistryDelegate
        int id = Item.getIdFromItem(stack.getItem());
        if (id < 0) {
            return new int[0];
        }
        int damageOffset = id | ((stack.getItemDamage() + 1) << 16);

        Map<Integer, int[]> map = ORE_ID_CACHE_MAP.get(id);
        if (map == null) {
            synchronized (ORE_ID_CACHE_MAP) {
                map = ORE_ID_CACHE_MAP.computeIfAbsent(id, k -> new ConcurrentHashMap<>());
            }
        }

        int[] oreIDs = map.get(damageOffset);
        if (oreIDs == null) {
            synchronized (map) {
                oreIDs = map.get(damageOffset);
                if (oreIDs == null) {
                    map.put(damageOffset, oreIDs = OreDictionary.getOreIDs(stack));
                }
            }
        }

        return oreIDs;
    }
}
