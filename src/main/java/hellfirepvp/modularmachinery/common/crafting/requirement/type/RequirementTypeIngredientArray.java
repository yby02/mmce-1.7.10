package hellfirepvp.modularmachinery.common.crafting.requirement.type;

import github.kasuminova.mmce.compat.RegistryNames;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import github.kasuminova.mmce.common.itemtype.ChancedIngredientStack;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementIngredientArray;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.util.nbt.NBTJsonDeserializer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTException;
import github.kasuminova.mmce.compat.ResourceLocation;
import github.kasuminova.mmce.compat.MathHelper;

import java.util.ArrayList;
import java.util.List;

public class RequirementTypeIngredientArray extends RequirementType<ItemStack, RequirementIngredientArray> {

    /**
     * Example:
     * {@code
     * {
     * "type": "modularmachinery:ingredient_array_input",
     * "io-type": "input",
     * "items": [
     * {
     * "item": "contenttweaker:programming_circuit_a",
     * "amount": 2
     * },
     * {
     * "item": "contenttweaker:programming_circuit_b",
     * "amount": 2
     * }
     * ],
     * "chance": 0.5
     * }
     * }
     */
    @Override
    public RequirementIngredientArray createRequirement(IOType type, JsonObject jsonObject) {
        RequirementIngredientArray req;

        JsonArray items;
        if (jsonObject.has("items") || jsonObject.get("items").isJsonArray()) {
            items = jsonObject.getAsJsonArray("items");
        } else {
            throw new JsonParseException("'items' must be a item array or must be exists!");
        }

        List<ChancedIngredientStack> itemArray = new ArrayList<>(items.size());

        for (JsonElement itemJsonElement : items) {
            JsonObject subItem;
            String itemDefinition;
            if (itemJsonElement.isJsonObject()) {
                subItem = itemJsonElement.getAsJsonObject();
                itemDefinition = subItem.getAsJsonPrimitive("item").getAsString();
            } else {
                throw new JsonParseException("The ComponentType 'item' expects an 'item'-entry that defines the item!");
            }

            ResourceLocation res = new ResourceLocation(itemDefinition);

            int meta = 0;
            int indexMeta = itemDefinition.indexOf('@');
            if (indexMeta != -1 && indexMeta != itemDefinition.length() - 1) {
                try {
                    meta = Integer.parseInt(itemDefinition.substring(indexMeta + 1));
                } catch (NumberFormatException exc) {
                    throw new JsonParseException("Expected a metadata number, got " + itemDefinition.substring(indexMeta + 1), exc);
                }
            }
            int amount = 1;
            if (jsonObject.has("amount")) {
                if (!jsonObject.get("amount").isJsonPrimitive() || !jsonObject.getAsJsonPrimitive("amount").isNumber()) {
                    throw new JsonParseException("'amount', if defined, needs to be a amount-number!");
                }
                amount = MathHelper.clamp(jsonObject.getAsJsonPrimitive("amount").getAsInt(), 1, 64);
            }

            // 1.7.10 移植：查找时去掉 @meta 后缀（上游把带 @meta 的整串当注册名，导致带 meta 的条目找不到物品）
            Item item = RegistryNames.findItem(indexMeta == -1 ? itemDefinition : itemDefinition.substring(0, indexMeta));
            ItemStack stack;
            ChancedIngredientStack ingredientStack;

            if (res.getNamespace().equalsIgnoreCase("ore")) {
                ingredientStack = new ChancedIngredientStack(itemDefinition.substring(4), amount);
            } else {
                if (item == null) {
                    throw new JsonParseException("Couldn't find item with registryName '" + itemDefinition + "' !");
                }

                if (meta > 0) {
                    stack = new ItemStack(item, amount, meta);
                } else {
                    stack = new ItemStack(item, amount);
                }
                ingredientStack = new ChancedIngredientStack(stack);
            }

            if (jsonObject.has("chance")) {
                if (!jsonObject.get("chance").isJsonPrimitive() || !jsonObject.getAsJsonPrimitive("chance").isNumber()) {
                    throw new JsonParseException("'chance', if defined, needs to be a chance-number between 0 and 1!");
                }
                float chance = jsonObject.getAsJsonPrimitive("chance").getAsFloat();
                if (chance >= 0 && chance <= 1) {
                    ingredientStack.chance = chance;
                }
            }

            if (jsonObject.has("nbt")) {
                if (!jsonObject.has("nbt") || !jsonObject.get("nbt").isJsonObject()) {
                    throw new JsonParseException("The ComponentType 'nbt' expects a json compound that defines the NBT tag!");
                }
                String nbtString = jsonObject.getAsJsonObject("nbt").toString();
                try {
                    ingredientStack.tag = NBTJsonDeserializer.deserialize(nbtString);
                } catch (NBTException exc) {
                    throw new JsonParseException("Error trying to parse NBTTag! Rethrowing exception...", exc);
                }
            }

            itemArray.add(ingredientStack);
        }

        req = new RequirementIngredientArray(itemArray);

        if (jsonObject.has("chance")) {
            if (!jsonObject.get("chance").isJsonPrimitive() || !jsonObject.getAsJsonPrimitive("chance").isNumber()) {
                throw new JsonParseException("'chance', if defined, needs to be a chance-number between 0 and 1!");
            }
            float chance = jsonObject.getAsJsonPrimitive("chance").getAsFloat();
            if (chance >= 0 && chance <= 1) {
                req.setChance(chance);
            }
        }

        return req;
    }
}
