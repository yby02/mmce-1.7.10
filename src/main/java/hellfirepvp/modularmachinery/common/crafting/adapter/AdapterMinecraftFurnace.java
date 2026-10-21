/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.crafting.adapter;

import github.kasuminova.mmce.compat.RegistryNames;

import minetweaker.util.IEventHandler;
import github.kasuminova.mmce.common.event.Phase;
import github.kasuminova.mmce.common.event.recipe.RecipeCheckEvent;
import github.kasuminova.mmce.common.event.recipe.RecipeEvent;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementEnergy;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementItem;
import hellfirepvp.modularmachinery.common.lib.RequirementTypesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import github.kasuminova.mmce.compat.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: AdapterMinecraftFurnace
 * Created by HellFirePvP
 * Date: 23.07.2017 / 14:48
 */
public class AdapterMinecraftFurnace extends RecipeAdapter {

    public AdapterMinecraftFurnace() {
        super(new ResourceLocation("minecraft", "furnace"));
    }

    @Nonnull
    @Override
    public Collection<MachineRecipe> createRecipesFor(ResourceLocation owningMachineName,
                                                      List<RecipeModifier> modifiers,
                                                      List<ComponentRequirement<?, ?>> additionalRequirements,
                                                      Map<Class<?>, List<IEventHandler<RecipeEvent>>> eventHandlers,
                                                      List<String> recipeTooltips) {
        FurnaceRecipes furnaceRecipes = FurnaceRecipes.smelting();
        Map<ItemStack, ItemStack> inputOutputMap = furnaceRecipes.getSmeltingList();

        List<MachineRecipe> smeltingRecipes = new ArrayList<>(inputOutputMap.size());
        List<String> skipped = new ArrayList<>();
        for (Map.Entry<ItemStack, ItemStack> smelting : inputOutputMap.entrySet()) {
            ItemStack input = smelting.getKey();
            ItemStack output = smelting.getValue();
            // 1.7.10 移植：模组用没有物品形式的方块调用 GameRegistry.addSmelting(Block, ...) 时，配方表里的输入是物品为 null 的 ItemStack
            //（原版熔炉也用不上它）。上游在输入数量为 0 时只是不加输入需求，于是生成一条不消耗任何原料就产出的配方：
            // 配方搜索总是先选中它，机器凭空产出、放进去的原料也不再被消耗（用户报告：合金炉凭空产出金锭）。这类条目整条跳过。
            if (input == null || input.getItem() == null || output == null || output.getItem() == null) {
                skipped.add(describe(input) + " -> " + describe(output));
                continue;
            }
            int tickTime = Math.round(Math.max(1, RecipeModifier.applyModifiers(modifiers, RequirementTypesMM.REQUIREMENT_DURATION, null, 120, false)));
            float experience = furnaceRecipes.func_151398_b(output);

            MachineRecipe recipe = createRecipeShell(
                new ResourceLocation("minecraft", "smelting_" + incId + "_" + describe(input) + "_" + describe(output)),
                owningMachineName,
                tickTime, 0, false);
            recipe.addRecipeEventHandler(RecipeCheckEvent.class, (IEventHandler<RecipeCheckEvent>) event -> {
                if (event.phase == Phase.START) {
                    event.getActiveRecipe().getDataCompound().setFloat("experience", experience);
                }
            });

            // 熔炉按物品与 meta 匹配、每次消耗 1 个，输入的数量字段可能是 0（有的模组这样注册），按 1 计
            int inAmount = Math.round(RecipeModifier.applyModifiers(modifiers, RequirementTypesMM.REQUIREMENT_ITEM, IOType.INPUT, Math.max(1, input.stackSize), false));
            if (inAmount > 0) {
                recipe.addRequirement(new RequirementItem(IOType.INPUT, withSize(input, inAmount)));
            }

            int outAmount = Math.round(RecipeModifier.applyModifiers(modifiers, RequirementTypesMM.REQUIREMENT_ITEM, IOType.OUTPUT, Math.max(1, output.stackSize), false));
            if (outAmount > 0) {
                recipe.addRequirement(new RequirementItem(IOType.OUTPUT, withSize(output, outAmount)));
            }

            int inEnergy = Math.round(RecipeModifier.applyModifiers(modifiers, RequirementTypesMM.REQUIREMENT_ENERGY, IOType.INPUT, 20, false));
            if (inEnergy > 0) {
                recipe.addRequirement(new RequirementEnergy(IOType.INPUT, inEnergy));
            }

            smeltingRecipes.add(recipe);
        }
        if (!skipped.isEmpty()) {
            ModularMachinery.log.warn("[ModularMachinery] Furnace adapter for {}: skipped {} smelting entries whose input or output has no item "
                + "(registered by a mod with a block that has no item form): {}", owningMachineName, skipped.size(), skipped);
        }
        incId++;
        return smeltingRecipes;
    }

    /** 复制并设置数量（不经过 ItemUtils.copyStackWithSize：它把数量字段为 0 的原堆视为空堆）。 */
    private static ItemStack withSize(ItemStack stack, int size) {
        ItemStack copy = stack.copy();
        copy.stackSize = size;
        return copy;
    }

    /**
     * 1.7.10 移植：上游用 ItemStack#toString（数量x未本地化名@meta）拼配方名，原木 log / log2 的未本地化名都是 tile.log，
     * 两条木炭配方会重名导致其中一条加载失败；这里改用注册名。
     */
    private static String describe(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return "empty";
        }
        String name = String.valueOf(RegistryNames.getName(stack.getItem())).replace(':', '.');
        return stack.stackSize + "x" + name + "@" + stack.getItemDamage();
    }
}
