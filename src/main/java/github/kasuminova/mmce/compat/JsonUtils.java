package github.kasuminova.mmce.compat;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;

import java.io.Reader;
import java.io.StringReader;

/**
 * 1.7.10 移植适配：复刻 1.12.2 net.minecraft.util.JsonUtils 中 MMCE 用到的方法（1.7.10 的同名类 API 不同）。
 * lenient=true 时允许 JSON 中出现注释、未加引号的键等宽松语法，与 1.12.2 行为一致。
 */
public final class JsonUtils {

    private JsonUtils() {
    }

    public static String getString(JsonObject json, String memberName) {
        if (json.has(memberName)) {
            JsonElement element = json.get(memberName);
            if (element.isJsonPrimitive()) {
                return element.getAsString();
            }
            throw new JsonSyntaxException("Expected " + memberName + " to be a string, was " + element);
        }
        throw new JsonSyntaxException("Missing " + memberName + ", expected to find a string");
    }

    public static String getString(JsonObject json, String memberName, String fallback) {
        return json.has(memberName) ? getString(json, memberName) : fallback;
    }

    public static JsonArray getJsonArray(JsonObject json, String memberName) {
        if (json.has(memberName)) {
            JsonElement element = json.get(memberName);
            if (element.isJsonArray()) {
                return element.getAsJsonArray();
            }
            throw new JsonSyntaxException("Expected " + memberName + " to be a JsonArray, was " + element);
        }
        throw new JsonSyntaxException("Missing " + memberName + ", expected to find a JsonArray");
    }

    public static JsonArray getJsonArray(JsonObject json, String memberName, JsonArray fallback) {
        return json.has(memberName) ? getJsonArray(json, memberName) : fallback;
    }

    public static <T> T fromJson(Gson gson, Reader reader, Class<T> adapter, boolean lenient) {
        try {
            JsonReader jsonReader = new JsonReader(reader);
            jsonReader.setLenient(lenient);
            return gson.getAdapter(adapter).read(jsonReader);
        } catch (java.io.IOException e) {
            throw new JsonParseException(e);
        }
    }

    public static <T> T fromJson(Gson gson, String json, Class<T> adapter, boolean lenient) {
        return fromJson(gson, new StringReader(json), adapter, lenient);
    }

    public static <T> T fromJson(Gson gson, Reader reader, Class<T> adapter) {
        return fromJson(gson, reader, adapter, false);
    }

    public static <T> T fromJson(Gson gson, String json, Class<T> adapter) {
        return fromJson(gson, json, adapter, false);
    }
}
