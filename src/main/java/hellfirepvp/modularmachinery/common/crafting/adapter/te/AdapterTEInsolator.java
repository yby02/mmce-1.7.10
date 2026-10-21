package hellfirepvp.modularmachinery.common.crafting.adapter.te;

import cofh.thermalexpansion.util.crafting.InsolatorManager;
import github.kasuminova.mmce.common.event.recipe.RecipeEvent;
import github.kasuminova.mmce.compat.ItemStacks;
import github.kasuminova.mmce.compat.ResourceLocation;
import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.adapter.RecipeAdapter;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementEnergy;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementItem;
import hellfirepvp.modularmachinery.common.lib.RequirementTypesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.util.ItemUtils;
import minetweaker.util.IEventHandler;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 热力膨胀灵魂嫁接器（Phytogenic Insolator）配方适配器，配方键 thermalexpansion:insolator。
 * <p>
 * 1.7.10 移植：上游按热力膨胀 5（1.12.2）编写——区分普通 / 树木两种配方、消耗水、肥料只作增产不作输入。
 * 1.7.10 的热力膨胀 4.1.5 没有树木模式和水，每条配方就是“主输入 + 副输入（通常是种子 + 肥料）→ 主产物 + 几率副产物”，
 * 因此这里如实转换两个输入；时间 = 配方能量 / 20，耗能 20 RF/t（同上游）；副产物几率 > 100% 时拆成固定数量 + 余下的几率（同上游）。
 */
public class AdapterTEInsolator extends RecipeAdapter {

    public static final int ENERGY_PER_TICK = 20;

    public AdapterTEInsolator() {
        super(new ResourceLocation("thermalexpansion", "insolator"));
    }

    @Nonnull
    @Override
    public Collection<MachineRecipe> createRecipesFor(ResourceLocation owningMachineName,
                                                      List<RecipeModifier> modifiers,
                                                      List<ComponentRequirement<?, ?>> additionalRequirements,
                                                      Map<Class<?>, List<IEventHandler<RecipeEvent>>> eventHandlers,
                                                      List<String> recipeTooltips) {
        List<MachineRecipe> recipes = new ArrayList<>();
        InsolatorManager.RecipeInsolator[] list = InsolatorManager.getRecipeList();
        if (list == null) {
            return recipes;
        }
        for (InsolatorManager.RecipeInsolator insolator : list) {
            ItemStack primaryInput = insolator.getPrimaryInput();
            ItemStack secondaryInput = insolator.getSecondaryInput();
            ItemStack primaryOutput = insolator.getPrimaryOutput();
            if (ItemStacks.isEmpty(primaryInput) || ItemStacks.isEmpty(primaryOutput)) {
                continue;
            }
            ItemStack secondaryOutput = insolator.getSecondaryOutput();

            MachineRecipe recipe = createRecipeShell(new ResourceLocation("thermalexpansion", "insolator_" + incId),
                owningMachineName,
                Math.max(1, Math.round(RecipeModifier.applyModifiers(modifiers, RequirementTypesMM.REQUIREMENT_DURATION, IOType.INPUT,
                    (float) insolator.getEnergy() / ENERGY_PER_TICK, false))),
                incId, false);
            incId++;

            int energyPerTick = Math.round(RecipeModifier.applyModifiers(modifiers, RequirementTypesMM.REQUIREMENT_ENERGY, IOType.INPUT, ENERGY_PER_TICK, false));
            if (energyPerTick > 0) {
                recipe.addRequirement(new RequirementEnergy(IOType.INPUT, energyPerTick));
            }

            addItem(recipe, modifiers, IOType.INPUT, primaryInput);
            if (!ItemStacks.isEmpty(secondaryInput)) {
                addItem(recipe, modifiers, IOType.INPUT, secondaryInput);
            }
            addItem(recipe, modifiers, IOType.OUTPUT, primaryOutput);

            if (!ItemStacks.isEmpty(secondaryOutput)) {
                int amount = Math.round(RecipeModifier.applyModifiers(modifiers, RequirementTypesMM.REQUIREMENT_ITEM, IOType.OUTPUT, ItemStacks.getCount(secondaryOutput), false));
                float chance = RecipeModifier.applyModifiers(modifiers, RequirementTypesMM.REQUIREMENT_ITEM, IOType.OUTPUT,
                    insolator.getSecondaryOutputChance() / 100F, true);
                if (amount > 0 && chance > 0) {
                    if (chance >= 1) {
                        recipe.addRequirement(new RequirementItem(IOType.OUTPUT, ItemUtils.copyStackWithSize(secondaryOutput, amount * (int) chance)));
                        float rest = chance - (int) chance;
                        if (rest > 0) {
                            RequirementItem extra = new RequirementItem(IOType.OUTPUT, ItemUtils.copyStackWithSize(secondaryOutput, amount));
                            extra.setChance(rest);
                            recipe.addRequirement(extra);
                        }
                    } else {
                        RequirementItem out = new RequirementItem(IOType.OUTPUT, ItemUtils.copyStackWithSize(secondaryOutput, amount));
                        out.setChance(chance);
                        recipe.addRequirement(out);
                    }
                }
            }
            recipes.add(recipe);
        }
        return recipes;
    }

    private static void addItem(MachineRecipe recipe, List<RecipeModifier> modifiers, IOType io, ItemStack stack) {
        int amount = Math.round(RecipeModifier.applyModifiers(modifiers, RequirementTypesMM.REQUIREMENT_ITEM, io, ItemStacks.getCount(stack), false));
        if (amount > 0) {
            recipe.addRequirement(new RequirementItem(io, ItemUtils.copyStackWithSize(stack, amount)));
        }
    }
}
