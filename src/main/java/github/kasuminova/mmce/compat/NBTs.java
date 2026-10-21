package github.kasuminova.mmce.compat;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 1.12.2 NBT 访问器在 1.7.10（MCP stable_12）中的对应实现；部分方法在 1.7.10 仍是 SRG 名。
 */
public final class NBTs {

    private NBTs() {
    }

    /** NBTTagCompound#getKeySet */
    public static Set<String> keySet(NBTTagCompound tag) {
        return tag.func_150296_c();
    }

    /** NBTTagString#getString */
    public static String getString(NBTTagString tag) {
        return tag.func_150285_a_();
    }

    /** NBTTagByteArray#getByteArray */
    public static byte[] getByteArray(NBTTagByteArray tag) {
        return tag.func_150292_c();
    }

    /** NBTTagIntArray#getIntArray */
    public static int[] getIntArray(NBTTagIntArray tag) {
        return tag.func_150302_c();
    }

    /** NBTTagList#getTagType */
    public static int getTagType(NBTTagList list) {
        return list.func_150303_d();
    }

    /**
     * 按顺序返回列表元素的副本（1.7.10 的 NBTTagList 没有按下标读取任意类型元素的公开方法，
     * 因此在副本上从尾部逐个移除）。
     */
    public static List<NBTBase> elements(NBTTagList list) {
        int count = list.tagCount();
        if (count == 0) {
            return Collections.emptyList();
        }
        NBTTagList copy = (NBTTagList) list.copy();
        List<NBTBase> out = new ArrayList<>(count);
        for (int i = count - 1; i >= 0; i--) {
            out.add(copy.removeTag(i));
        }
        Collections.reverse(out);
        return out;
    }

    public static NBTTagCompound copy(NBTTagCompound tag) {
        return (NBTTagCompound) tag.copy();
    }

    /** NBTTagString#quoteAndEscape（1.12.2） */
    public static String quoteAndEscape(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            if (c == '\\' || c == '"') {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.append('"').toString();
    }
}
