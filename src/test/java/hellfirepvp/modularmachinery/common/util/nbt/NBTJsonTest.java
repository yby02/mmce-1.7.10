package hellfirepvp.modularmachinery.common.util.nbt;

import github.kasuminova.mmce.compat.NBTs;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 机器 / 配方 JSON 中 "nbt" 字段的解析（NBTJsonDeserializer）、匹配（NBTMatchingHelper）与导出（NBTJsonSerializer）。
 */
class NBTJsonTest {

    private static NBTTagCompound itemTag(int energy, String name) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("Energy", energy);
        tag.setString("Name", name);
        return tag;
    }

    @Test
    void comparisonOperatorsMatchItemNbt() throws NBTException {
        NBTTagCompound match = NBTJsonDeserializer.deserialize("{\"Energy\": \">=1000\"}");
        assertTrue(NBTMatchingHelper.matchNBTCompound(match, itemTag(1000, "a")));
        assertTrue(NBTMatchingHelper.matchNBTCompound(match, itemTag(5000, "a")));
        assertFalse(NBTMatchingHelper.matchNBTCompound(match, itemTag(999, "a")));

        NBTTagCompound less = NBTJsonDeserializer.deserialize("{\"Energy\": \"<10\"}");
        assertTrue(NBTMatchingHelper.matchNBTCompound(less, itemTag(9, "a")));
        assertFalse(NBTMatchingHelper.matchNBTCompound(less, itemTag(10, "a")));
    }

    @Test
    void stringValuesAreCaseInsensitiveRegex() throws NBTException {
        NBTTagCompound match = NBTJsonDeserializer.deserialize("{\"Name\": \"steel.*\"}");
        assertTrue(NBTMatchingHelper.matchNBTCompound(match, itemTag(0, "Steel Ingot")));
        assertFalse(NBTMatchingHelper.matchNBTCompound(match, itemTag(0, "Iron Ingot")));
    }

    @Test
    void missingKeyOrTagFailsAndNullMatchAccepts() throws NBTException {
        NBTTagCompound match = NBTJsonDeserializer.deserialize("{\"Other\": \"1\"}");
        assertFalse(NBTMatchingHelper.matchNBTCompound(match, itemTag(1, "a")));
        assertFalse(NBTMatchingHelper.matchNBTCompound(match, null));
        assertTrue(NBTMatchingHelper.matchNBTCompound(null, itemTag(1, "a")));
        assertTrue(NBTMatchingHelper.matchNBTCompound(new NBTTagCompound(), null));
    }

    @Test
    void listMatchingConsumesEachElementOnce() throws NBTException {
        NBTTagCompound match = NBTJsonDeserializer.deserialize("{\"L\": [\"1\", \"1\"]}");
        NBTTagCompound two = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        list.appendTag(new NBTTagInt(1));
        list.appendTag(new NBTTagInt(1));
        two.setTag("L", list);
        assertTrue(NBTMatchingHelper.matchNBTCompound(match, two));

        NBTTagCompound one = new NBTTagCompound();
        NBTTagList single = new NBTTagList();
        single.appendTag(new NBTTagInt(1));
        one.setTag("L", single);
        assertFalse(NBTMatchingHelper.matchNBTCompound(match, one));
    }

    @Test
    void malformedJsonReportsPosition() {
        NBTException e = assertThrows(NBTException.class, () -> NBTJsonDeserializer.deserialize("{\"a\": \"1\""));
        assertTrue(e.getMessage().contains("<--[HERE]"), e.getMessage());
    }

    @Test
    void serializerSeparatesEveryListElement() {
        // 上游实现会漏掉第 1、2 个元素之间的逗号，导出的 JSON 无法再读回
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        list.appendTag(new NBTTagString("x"));
        list.appendTag(new NBTTagString("y"));
        list.appendTag(new NBTTagString("z"));
        tag.setTag("l", list);
        assertEquals("{\"l\":[\"x\",\"y\",\"z\"]}", NBTJsonSerializer.serializeNBT(tag));
    }

    @Test
    void serializedNbtParsesBackAndMatchesOriginal() throws NBTException {
        NBTTagCompound tag = itemTag(42, "SteelIngot");
        tag.setByte("B", (byte) 3);
        tag.setLong("Big", 1234567890123L);
        NBTTagCompound inner = new NBTTagCompound();
        inner.setShort("S", (short) 7);
        tag.setTag("Inner", inner);

        String json = NBTJsonSerializer.serializeNBT(tag);
        NBTTagCompound parsed = NBTJsonDeserializer.deserialize(json);
        assertEquals(NBTs.keySet(tag), NBTs.keySet(parsed), json);
        assertTrue(NBTMatchingHelper.matchNBTCompound(parsed, tag), json);
    }

    @Test
    void arraysAreLeftOutSoExportedTileNbtIsValidJson() throws NBTException, java.io.IOException {
        // 上游把字节数组 / int 数组写成空值（"inSlots":,）：导出含物品总线、控制器等方块实体的结构时 JSON 无法读回
        NBTTagCompound tile = new NBTTagCompound();
        tile.setIntArray("inSlots", new int[0]);
        tile.setIntArray("outSlots", new int[]{1, 2});
        tile.setString("id", "tilemachinecontroller");
        tile.setByteArray("bytes", new byte[]{1});
        tile.setInteger("casingColor", 0xC83C3C);
        NBTTagList list = new NBTTagList();
        list.appendTag(new net.minecraft.nbt.NBTTagIntArray(new int[]{3}));
        tile.setTag("arrays", list);
        NBTTagCompound inner = new NBTTagCompound();
        inner.setIntArray("sides", new int[]{0, 1});
        tile.setTag("items", inner);

        String json = NBTJsonSerializer.serializeNBT(tile);
        new com.google.gson.Gson().getAdapter(com.google.gson.JsonElement.class).fromJson(json);   // 严格解析，不合法时抛异常
        NBTTagCompound parsed = NBTJsonDeserializer.deserialize(json);
        assertFalse(parsed.hasKey("inSlots") || parsed.hasKey("outSlots") || parsed.hasKey("bytes"), json);
        assertEquals(0, NBTs.keySet((NBTTagCompound) parsed.getTag("items")).size(), json);
        assertEquals(0, ((NBTTagList) parsed.getTag("arrays")).tagCount(), json);
        assertTrue(NBTMatchingHelper.matchNBTCompound(parsed, tile), json);
    }

    @Test
    void quotesAndBackslashesSurviveRoundTrip() throws NBTException {
        String raw = "say \"hi\" \\ ok";
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("Text", raw);
        NBTTagCompound parsed = NBTJsonDeserializer.deserialize(NBTJsonSerializer.serializeNBT(tag));
        assertEquals(raw, parsed.getString("Text"));
    }
}
