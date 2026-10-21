/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.util.nbt;

import java.util.List;

import github.kasuminova.mmce.compat.NBTs;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.common.util.Constants;

import java.util.Iterator;
import java.util.Set;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: NBTJsonSerializer
 * Created by HellFirePvP
 * Date: 19.08.2017 / 13:56
 */
public class NBTJsonSerializer {

    /**
     * 1.7.10 移植：上游只处理数字、字符串、列表与复合标签，字节数组 / int 数组被写成空值（如 "inSlots":,），
     * 结果不是合法 JSON。建造选择工具导出结构时会写入方块实体的 NBT，而物品总线、控制器等都有 int 数组，导出的文件无法再读回。
     * 机器 JSON 的 NBT 写法表达不了数组（读回只会得到列表，匹配时永远不等于数组），所以这里跳过数组：
     * 复合标签中省略该键、列表中省略该元素。匹配只检查写出的键，省略只会让匹配更宽松。
     */
    public static String serializeNBT(NBTBase nbtBase) {
        String out = serializeOrNull(nbtBase);
        return out == null ? "{}" : out;
    }

    /** 无法表示的类型（字节数组、int 数组）返回 null。 */
    private static String serializeOrNull(NBTBase nbtBase) {
        StringBuilder sb = new StringBuilder();
        switch (nbtBase.getId()) {
            case Constants.NBT.TAG_BYTE:
            case Constants.NBT.TAG_SHORT:
            case Constants.NBT.TAG_INT:
            case Constants.NBT.TAG_LONG:
            case Constants.NBT.TAG_FLOAT:
            case Constants.NBT.TAG_DOUBLE:
                sb.append(NBTs.quoteAndEscape(nbtBase.toString()));
                break;
            case Constants.NBT.TAG_STRING:
                sb.append(NBTs.quoteAndEscape(NBTs.getString((NBTTagString) nbtBase)));
                break;
            case Constants.NBT.TAG_LIST:
                sb.append('[');
                NBTTagList listTag = (NBTTagList) nbtBase;

                // 1.7.10 移植：修正上游分隔符位置错误（原实现会漏掉第 1、2 个元素之间的逗号）
                List<NBTBase> elements = NBTs.elements(listTag);
                boolean firstElement = true;
                for (NBTBase element : elements) {
                    String value = serializeOrNull(element);
                    if (value == null) {
                        continue;
                    }
                    if (!firstElement) {
                        sb.append(',');
                    }
                    sb.append(value);
                    firstElement = false;
                }
                sb.append(']');
                break;
            case Constants.NBT.TAG_COMPOUND:
                sb.append('{');
                NBTTagCompound cmpTag = (NBTTagCompound) nbtBase;
                Set<String> collection = NBTs.keySet(cmpTag);

                int i = 0;
                Iterator<String> it = collection.iterator();
                while (it.hasNext()) {
                    final String s = it.next();
                    String value = serializeOrNull(cmpTag.getTag(s));
                    if (value == null) {
                        continue;
                    }
                    if (i != 0) {
                        sb.append(',');
                    }
                    sb.append(NBTs.quoteAndEscape(s)).append(':').append(value);
                    i++;
                }

                sb.append('}');
                break;
            default:
                return null;
        }
        return sb.toString();
    }

}
