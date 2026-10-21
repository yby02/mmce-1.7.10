package kport.modularmagic.common.crafting.requirement.types;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import github.kasuminova.mmce.compat.ResourceLocation;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementType;

/**
 * Modular Magic 的需求类型（1.7.10 子集），注册名与 JSON 写法同上游：
 * <ul>
 *   <li>{@code modularmachinery:aspect}：{@code "aspect": "ignis", "amount": 10}（神秘时代 4 的源质标签名）</li>
 *   <li>{@code modularmachinery:mana}：{@code "amount": 1000, "perTick": false}</li>
 *   <li>{@code modularmachinery:lifeessence}：{@code "amount": 500, "perTick": false}</li>
 * </ul>
 */
public final class ModularMagicRequirements {

    public static final ResourceLocation KEY_REQUIREMENT_ASPECT       = new ResourceLocation(ModularMachinery.MODID, "aspect");
    public static final ResourceLocation KEY_REQUIREMENT_MANA         = new ResourceLocation(ModularMachinery.MODID, "mana");
    public static final ResourceLocation KEY_REQUIREMENT_LIFE_ESSENCE = new ResourceLocation(ModularMachinery.MODID, "lifeessence");

    public static RequirementTypeAspect      REQUIREMENT_ASPECT;
    public static RequirementTypeMana        REQUIREMENT_MANA;
    public static RequirementTypeLifeEssence REQUIREMENT_LIFE_ESSENCE;

    private ModularMagicRequirements() {
    }

    public static void initRequirements() {
        if (Mods.THAUMCRAFT.isPresent()) {
            REQUIREMENT_ASPECT = register(new RequirementTypeAspect(), KEY_REQUIREMENT_ASPECT);
        }
        if (Mods.BOTANIA.isPresent()) {
            REQUIREMENT_MANA = register(new RequirementTypeMana(), KEY_REQUIREMENT_MANA);
        }
        if (Mods.BLOODMAGIC.isPresent()) {
            REQUIREMENT_LIFE_ESSENCE = register(new RequirementTypeLifeEssence(), KEY_REQUIREMENT_LIFE_ESSENCE);
        }
    }

    private static <T extends RequirementType<?, ?>> T register(T type, ResourceLocation key) {
        type.setRegistryName(key);
        CommonProxy.registryPrimer.register(type);
        return type;
    }

    // ---------------- JSON ----------------

    static int requiredInt(JsonObject json, String key, String type) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive() || !json.get(key).getAsJsonPrimitive().isNumber()) {
            throw new JsonParseException("The ComponentType '" + type + "' expects an '" + key + "'-entry that defines the amount!");
        }
        return json.get(key).getAsInt();
    }

    static boolean optionalBoolean(JsonObject json, String key, boolean defaultValue) {
        if (!json.has(key)) {
            return defaultValue;
        }
        if (!json.get(key).isJsonPrimitive() || !json.get(key).getAsJsonPrimitive().isBoolean()) {
            throw new JsonParseException("'" + key + "' must be true or false!");
        }
        return json.get(key).getAsBoolean();
    }

    static String requiredString(JsonObject json, String key, String type) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive() || !json.get(key).getAsJsonPrimitive().isString()) {
            throw new JsonParseException("The ComponentType '" + type + "' expects an '" + key + "'-entry!");
        }
        return json.get(key).getAsString();
    }
}
