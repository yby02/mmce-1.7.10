package github.kasuminova.mmce.compat;

import cpw.mods.fml.common.registry.GameRegistry;
import hellfirepvp.modularmachinery.ModularMachinery;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 1.7.10 移植适配：替代 1.12.2 的 ForgeRegistries.ITEMS/BLOCKS 按 "modid:name" 查找。
 * 1.7.10 注册名区分大小写（如 Mekanism:Ingot）；为兼容沿用 1.12.2 习惯写成小写的配置，
 * 精确匹配失败时再做一次不区分大小写的查找，并记录一次警告。
 */
public final class RegistryNames {

    private static final Set<String> WARNED = new HashSet<>();

    private RegistryNames() {
    }

    public static Item findItem(String id) {
        String[] parts = split(id);
        Item item = GameRegistry.findItem(parts[0], parts[1]);
        if (item != null) {
            return item;
        }
        Object byName = Item.itemRegistry.getObject(parts[0] + ":" + parts[1]);
        if (byName instanceof Item) {
            return (Item) byName;
        }
        for (Object key : Item.itemRegistry.getKeys()) {
            String name = String.valueOf(key);
            if (name.equalsIgnoreCase(parts[0] + ":" + parts[1])) {
                warnCase(id, name);
                return (Item) Item.itemRegistry.getObject(name);
            }
        }
        return null;
    }

    public static Block findBlock(String id) {
        String[] parts = split(id);
        Block block = GameRegistry.findBlock(parts[0], parts[1]);
        if (block != null) {
            return block;
        }
        String full = parts[0] + ":" + parts[1];
        if (Block.blockRegistry.containsKey(full)) {
            return (Block) Block.blockRegistry.getObject(full);
        }
        for (Object key : Block.blockRegistry.getKeys()) {
            String name = String.valueOf(key);
            if (name.equalsIgnoreCase(full)) {
                warnCase(id, name);
                return (Block) Block.blockRegistry.getObject(name);
            }
        }
        return null;
    }

    public static String getName(Item item) {
        if (item == null) {
            return "minecraft:air";
        }
        GameRegistry.UniqueIdentifier uid = GameRegistry.findUniqueIdentifierFor(item);
        return uid != null ? uid.modId + ":" + uid.name : String.valueOf(Item.itemRegistry.getNameForObject(item));
    }

    public static String getName(Block block) {
        if (block == null || block == Blocks.air) {
            return "minecraft:air";
        }
        GameRegistry.UniqueIdentifier uid = GameRegistry.findUniqueIdentifierFor(block);
        return uid != null ? uid.modId + ":" + uid.name : String.valueOf(Block.blockRegistry.getNameForObject(block));
    }

    /** 拆分 "modid:name"；缺省命名空间为 minecraft。 */
    public static String[] split(String id) {
        int idx = id.indexOf(':');
        if (idx < 0) {
            return new String[]{"minecraft", id};
        }
        return new String[]{id.substring(0, idx), id.substring(idx + 1)};
    }

    private static void warnCase(String requested, String actual) {
        if (WARNED.add(requested.toLowerCase(Locale.ROOT))) {
            ModularMachinery.log.warn("Registry name '{}' matched '{}' only case-insensitively. 1.7.10 IDs are case-sensitive; please use '{}'.",
                requested, actual, actual);
        }
    }
}
