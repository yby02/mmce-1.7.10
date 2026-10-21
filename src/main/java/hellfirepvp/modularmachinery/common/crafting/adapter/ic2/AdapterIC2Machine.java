package hellfirepvp.modularmachinery.common.crafting.adapter.ic2;

import github.kasuminova.mmce.common.itemtype.ChancedIngredientStack;
import github.kasuminova.mmce.compat.ItemStacks;
import github.kasuminova.mmce.compat.ResourceLocation;
import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.adapter.RecipeAdapter;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementEnergy;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementIngredientArray;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementItem;
import hellfirepvp.modularmachinery.common.lib.RequirementTypesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.util.ItemUtils;
import ic2.api.recipe.IMachineRecipeManager;
import ic2.api.recipe.IRecipeInput;
import ic2.api.recipe.RecipeOutput;
import github.kasuminova.mmce.common.event.recipe.RecipeEvent;
import minetweaker.util.IEventHandler;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * IC2 机器配方适配器（1.7.10 移植）。
 * <p>
 * 1.12.2 的 IC2 用 {@code Iterable<MachineRecipe<IRecipeInput, Collection<ItemStack>>>}；1.7.10 experimental 的
 * {@link IMachineRecipeManager#getRecipes()} 返回 {@code Map<IRecipeInput, RecipeOutput>}，产物在 {@link RecipeOutput#items}。
 * 转换规则与上游相同：输入有多个候选（矿辞等）时用配料组，否则用单个物品；每条配方耗能 8 RF/t，基础时间由子类给出。
 */
public abstract class AdapterIC2Machine extends RecipeAdapter {

    private final String recipePrefix;
    private final int    baseWorkTime;

    protected AdapterIC2Machine(String machineName, int baseWorkTime) {
        super(new ResourceLocation("ic2", "te_" + machineName));
        this.recipePrefix = "te_" + machineName + "_recipe_";
        this.baseWorkTime = baseWorkTime;
    }

    protected abstract IMachineRecipeManager getManager();

    @Nonnull
    @Override
    public Collection<MachineRecipe> createRecipesFor(ResourceLocation owningMachineName,
                                                      List<RecipeModifier> modifiers,
                                                      List<ComponentRequirement<?, ?>> additionalRequirements,
                                                      Map<Class<?>, List<IEventHandler<RecipeEvent>>> eventHandlers,
                                                      List<String> recipeTooltips) {
        IMachineRecipeManager manager = getManager();
        List<MachineRecipe> recipes = new ArrayList<>();
        if (manager == null) {
            return recipes;
        }
        for (Map.Entry<IRecipeInput, RecipeOutput> entry : manager.getRecipes().entrySet()) {
            IRecipeInput input = entry.getKey();
            RecipeOutput output = entry.getValue();
            if (input == null || output == null || output.items == null) {
                continue;
            }
            List<ItemStack> inputs = new ArrayList<>();
            if (input.getInputs() != null) {
                for (ItemStack candidate : input.getInputs()) {
                    if (candidate != null && candidate.getItem() != null) {
                        inputs.add(candidate);
                    }
                }
            }
            // 没有可用的输入物品时整条跳过（否则生成一条不消耗原料就产出的配方，见熔炉适配器）
            if (inputs.isEmpty()) {
                continue;
            }

            MachineRecipe recipe = createRecipeShell(
                new ResourceLocation("ic2", recipePrefix + incId),
                owningMachineName,
                Math.max(1, Math.round(RecipeModifier.applyModifiers(modifiers, RequirementTypesMM.REQUIREMENT_DURATION, null, baseWorkTime, false))),
                incId, false);

            int inAmount = Math.round(RecipeModifier.applyModifiers(modifiers, RequirementTypesMM.REQUIREMENT_ITEM, IOType.INPUT, input.getAmount(), false));
            if (inAmount > 0) {
                if (inputs.size() > 1) {
                    List<ChancedIngredientStack> ingredients = new ArrayList<>();
                    for (ItemStack candidate : inputs) {
                        ingredients.add(new ChancedIngredientStack(withSize(candidate, inAmount)));
                    }
                    recipe.addRequirement(new RequirementIngredientArray(ingredients));
                } else {
                    recipe.addRequirement(new RequirementItem(IOType.INPUT, withSize(inputs.get(0), inAmount)));
                }
            }

            for (ItemStack out : output.items) {
                if (ItemStacks.isEmpty(out)) {
                    continue;
                }
                int outAmount = Math.round(RecipeModifier.applyModifiers(modifiers, RequirementTypesMM.REQUIREMENT_ITEM, IOType.OUTPUT, ItemStacks.getCount(out), false));
                if (outAmount > 0) {
                    recipe.addRequirement(new RequirementItem(IOType.OUTPUT, ItemUtils.copyStackWithSize(out, outAmount)));
                }
            }

            int inEnergy = Math.round(RecipeModifier.applyModifiers(modifiers, RequirementTypesMM.REQUIREMENT_ENERGY, IOType.INPUT, 8, false));
            if (inEnergy > 0) {
                recipe.addRequirement(new RequirementEnergy(IOType.INPUT, inEnergy));
            }

            recipes.add(recipe);
            incId++;
        }
        return recipes;
    }

    /** 复制并设置数量（IC2 的候选物品数量字段可能为 0，ItemUtils.copyStackWithSize 会把它当作空堆）。 */
    private static ItemStack withSize(ItemStack stack, int size) {
        ItemStack copy = stack.copy();
        copy.stackSize = size;
        return copy;
    }
}
