package hellfirepvp.modularmachinery.common.crafting.adapter.tconstruct;

import github.kasuminova.mmce.common.event.Phase;
import github.kasuminova.mmce.common.event.recipe.RecipeCheckEvent;
import github.kasuminova.mmce.common.event.recipe.RecipeEvent;
import github.kasuminova.mmce.compat.ResourceLocation;
import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.adapter.RecipeAdapter;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementFluid;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementItem;
import hellfirepvp.modularmachinery.common.lib.RequirementTypesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import mantle.utils.ItemMetaWrapper;
import minetweaker.util.IEventHandler;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import tconstruct.library.crafting.Smeltery;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 匠魂冶炼炉熔化配方适配器（配方键 tconstruct:smeltery_melting）。
 * <p>
 * 1.7.10 移植：1.12.2 的 TinkerRegistry.getAllMeltingRecipies() 在 1.7.10 是 {@link Smeltery#getSmeltingList()}
 * （以物品 + meta 为键）与 {@link Smeltery#getTemperatureList()}。每个物品一条配方：物品输入 → 流体输出，
 * 时间 = 熔化温度 / 10（同上游），配方数据中记录 temperatureRequired。
 * 上游用输入方向的流体修改器计算输出流体量，这里改用输出方向。
 */
public class AdapterSmelteryMeltingRecipe extends RecipeAdapter {

    public AdapterSmelteryMeltingRecipe() {
        super(new ResourceLocation("tconstruct", "smeltery_melting"));
    }

    @Nonnull
    @Override
    public Collection<MachineRecipe> createRecipesFor(ResourceLocation owningMachineName,
                                                      List<RecipeModifier> modifiers,
                                                      List<ComponentRequirement<?, ?>> additionalRequirements,
                                                      Map<Class<?>, List<IEventHandler<RecipeEvent>>> eventHandlers,
                                                      List<String> recipeTooltips) {
        Map<ItemMetaWrapper, FluidStack> smelting = Smeltery.getSmeltingList();
        Map<ItemMetaWrapper, Integer> temperatures = Smeltery.getTemperatureList();
        List<MachineRecipe> recipes = new ArrayList<>(smelting.size());

        for (Map.Entry<ItemMetaWrapper, FluidStack> entry : smelting.entrySet()) {
            ItemMetaWrapper key = entry.getKey();
            FluidStack result = entry.getValue();
            if (key == null || key.item == null || result == null || result.getFluid() == null) {
                continue;
            }
            Integer temp = temperatures.get(key);
            final int temperature = temp == null ? 200 : temp;

            MachineRecipe recipe = createRecipeShell(new ResourceLocation("tconstruct", "smeltery_melting_" + incId),
                owningMachineName,
                Math.max(1, Math.round(RecipeModifier.applyModifiers(modifiers, RequirementTypesMM.REQUIREMENT_DURATION, IOType.INPUT, temperature / 10F, false))),
                incId, false);
            recipe.addRecipeEventHandler(RecipeCheckEvent.class, (IEventHandler<RecipeCheckEvent>) event -> {
                if (event.phase == Phase.START) {
                    event.getActiveRecipe().getDataCompound().setFloat("temperatureRequired", temperature);
                }
            });

            int inAmount = Math.round(RecipeModifier.applyModifiers(modifiers, RequirementTypesMM.REQUIREMENT_ITEM, IOType.INPUT, 1, false));
            if (inAmount > 0) {
                recipe.addRequirement(new RequirementItem(IOType.INPUT, new ItemStack(key.item, inAmount, key.meta == null ? 0 : key.meta)));
            }

            FluidStack output = result.copy();
            int outAmount = Math.round(RecipeModifier.applyModifiers(modifiers, RequirementTypesMM.REQUIREMENT_FLUID, IOType.OUTPUT, output.amount, false));
            if (outAmount > 0) {
                output.amount = outAmount;
                recipe.addRequirement(new RequirementFluid(IOType.OUTPUT, output));
            }

            recipes.add(recipe);
            incId++;
        }
        return recipes;
    }
}
