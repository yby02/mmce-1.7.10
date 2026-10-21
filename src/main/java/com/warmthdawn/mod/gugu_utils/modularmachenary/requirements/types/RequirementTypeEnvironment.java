/*
 * 移植自 gugu-utils（https://github.com/ParaParty/gugu-utils，MIT License，Copyright (c) 2020 派瑞派对）。
 * JSON 写法同原模组：{ "type": "gugu-utils:environment", "io-type": "input", "subType": "...", ... }
 * <ul>
 *   <li>weather：{@code "weather": "sunny" | "raining" | "snowing" | "thundering"}（不区分大小写）</li>
 *   <li>moonphase：{@code "moonphases": [0, 4]}（0 满月 … 7 盈凸月，超出范围的值记录警告并忽略）</li>
 *   <li>dimension：{@code "dimensions": [0, -1]}</li>
 *   <li>biome：{@code "biomeType": "desert" | "warm" | "cool" | "icy"}，或 {@code "biomes": ["Plains", 4, "minecraft:extreme_hills"]}；两者都写时只用 biomeType</li>
 *   <li>altitude / time：{@code "begin": 0, "end": 64}（time 的 begin 可大于 end，表示跨过午夜）</li>
 * </ul>
 * 1.7.10 移植：生物群系按名称或数字 ID 查找（见 EnvBiome）；原模组静默忽略找不到的群系，这里另记录警告。
 */
package com.warmthdawn.mod.gugu_utils.modularmachenary.requirements.types;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.warmthdawn.mod.gugu_utils.GuGuUtils;
import com.warmthdawn.mod.gugu_utils.common.Constants;
import com.warmthdawn.mod.gugu_utils.modularmachenary.environment.envtypes.*;
import com.warmthdawn.mod.gugu_utils.modularmachenary.requirements.RequirementEnvironment;
import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementType;
import hellfirepvp.modularmachinery.common.machine.IOType;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeManager;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RequirementTypeEnvironment extends RequirementType<EnvironmentType, RequirementEnvironment> {

    public static RequirementTypeEnvironment INSTANCE;

    public static void register() {
        INSTANCE = new RequirementTypeEnvironment();
        INSTANCE.setRegistryName(Constants.RESOURCE_ENVIRONMENT);
        CommonProxy.registryPrimer.register(INSTANCE);
    }

    @Nullable
    @Override
    public String requiresModid() {
        return null;
    }

    private static JsonElement required(JsonObject json, String key) {
        if (!json.has(key)) {
            throw new JsonParseException("The ComponentType '" + Constants.STRING_RESOURCE_ENVIRONMENT + "' expects an '" + key + "'-entry!");
        }
        return json.get(key);
    }

    private static JsonArray requiredArray(JsonObject json, String key) {
        JsonElement element = required(json, key);
        if (!element.isJsonArray()) {
            throw new JsonParseException("'" + key + "' of '" + Constants.STRING_RESOURCE_ENVIRONMENT + "' has to be an array!");
        }
        return element.getAsJsonArray();
    }

    private static int[] intArray(JsonArray array) {
        List<Integer> values = new ArrayList<>();
        for (JsonElement element : array) {
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
                values.add(element.getAsInt());
            }
        }
        return values.stream().mapToInt(Integer::intValue).toArray();
    }

    /** 解析 JSON 中的检测条件；也供 ZenScript 方法复用其中的生物群系 / 天气解析。 */
    public static EnvironmentType parseType(JsonObject json) {
        String subType = required(json, "subType").getAsString();
        switch (subType) {
            case "weather": {
                String weather = required(json, "weather").getAsString();
                try {
                    return new EnvWeather(EnvWeather.Weathers.valueOf(weather.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException e) {
                    throw new JsonParseException("Unknown weather '" + weather + "' (sunny, raining, snowing, thundering)");
                }
            }
            case "biome": {
                if (json.has("biomeType")) {
                    String name = json.get("biomeType").getAsString();
                    BiomeManager.BiomeType type = EnvBiome.biomeType(name);
                    if (type == null) {
                        GuGuUtils.log.warn("[gugu-utils] Unknown biomeType '{}' (desert, warm, cool, icy); this requirement never matches", name);
                        return new EnvBiome(new BiomeGenBase[0]);
                    }
                    return new EnvBiome(type);
                }
                return new EnvBiome(biomes(requiredArray(json, "biomes")));
            }
            case "dimension":
                return new EnvDimension(intArray(requiredArray(json, "dimensions")));
            case "moonphase": {
                List<Integer> phases = new ArrayList<>();
                for (int phase : intArray(requiredArray(json, "moonphases"))) {
                    if (phase >= 0 && phase < 8) {
                        phases.add(phase);
                    } else {
                        GuGuUtils.log.warn("[gugu-utils] MoonPhase must be in 0 - 7, got {}", phase);
                    }
                }
                return new EnvMoonPhase(phases.stream().mapToInt(Integer::intValue).toArray());
            }
            case "time":
                return new EnvTime(required(json, "begin").getAsInt(), required(json, "end").getAsInt());
            case "altitude":
                return new EnvAltitude(required(json, "begin").getAsInt(), required(json, "end").getAsInt());
            default:
                throw new JsonParseException("Type：" + subType + " is not supported");
        }
    }

    private static BiomeGenBase[] biomes(JsonArray array) {
        List<BiomeGenBase> list = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonPrimitive()) {
                continue;
            }
            BiomeGenBase biome = EnvBiome.findBiome(element.getAsString());
            if (biome != null) {
                list.add(biome);
            } else {
                GuGuUtils.log.warn("[gugu-utils] Unknown biome '{}' (use the 1.7.10 biome name or its numeric id)", element.getAsString());
            }
        }
        return list.toArray(new BiomeGenBase[0]);
    }

    @Override
    public RequirementEnvironment createRequirement(IOType type, JsonObject jsonObject) {
        return new RequirementEnvironment(this, parseType(jsonObject), type);
    }
}
