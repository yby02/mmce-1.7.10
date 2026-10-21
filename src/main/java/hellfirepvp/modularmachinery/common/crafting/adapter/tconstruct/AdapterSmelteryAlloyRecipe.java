package hellfirepvp.modularmachinery.common.crafting.adapter.tconstruct;

import github.kasuminova.mmce.common.event.recipe.RecipeEvent;
import github.kasuminova.mmce.compat.ResourceLocation;
import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.adapter.RecipeAdapter;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementFluid;
import hellfirepvp.modularmachinery.common.lib.RequirementTypesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import minetweaker.util.IEventHandler;
import net.minecraftforge.fluids.FluidStack;
import tconstruct.library.crafting.AlloyMix;
import tconstruct.library.crafting.Smeltery;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 匠魂冶炼炉合金配方适配器（配方键 tconstruct:smeltery_alloy）：多种流体输入 → 一种流体输出，时间 4 t（同上游）。
 * 1.7.10 移植：数据来自 {@link Smeltery#getAlloyList()}（{@link AlloyMix}）；输出流体量改用输出方向的修改器（上游用了输入方向）。
 */
public class AdapterSmelteryAlloyRecipe extends RecipeAdapter {

    public static final int WORK_TIME = 4;

    public AdapterSmelteryAlloyRecipe() {
        super(new ResourceLocation("tconstruct", "smeltery_alloy"));
    }

    @Nonnull
    @Override
    public Collection<MachineRecipe> createRecipesFor(ResourceLocation owningMachineName,
                                                      List<RecipeModifier> modifiers,
                                                      List<ComponentRequirement<?, ?>> additionalRequirements,
                                                      Map<Class<?>, List<IEventHandler<RecipeEvent>>> eventHandlers,
                                                      List<String> recipeTooltips) {
        List<AlloyMix> alloys = Smeltery.getAlloyList();
        List<MachineRecipe> recipes = new ArrayList<>(alloys.size());

        for (AlloyMix alloy : alloys) {
            if (alloy == null || alloy.result == null || alloy.mixers == null || alloy.mixers.isEmpty()) {
                continue;
            }
            MachineRecipe recipe = createRecipeShell(new ResourceLocation("tconstruct", "smeltery_alloy_" + incId),
                owningMachineName,
                Math.max(1, Math.round(RecipeModifier.applyModifiers(modifiers, RequirementTypesMM.REQUIREMENT_DURATION, IOType.INPUT, WORK_TIME, false))),
                incId, false);

            int validInputs = 0;
            for (FluidStack fluid : alloy.mixers) {
                if (fluid == null || fluid.getFluid() == null) {
                    continue;
                }
                validInputs++;
                FluidStack input = fluid.copy();
                int inAmount = Math.round(RecipeModifier.applyModifiers(modifiers, RequirementTypesMM.REQUIREMENT_FLUID, IOType.INPUT, input.amount, false));
                if (inAmount > 0) {
                    input.amount = inAmount;
                    recipe.addRequirement(new RequirementFluid(IOType.INPUT, input));
                }
            }
            // 原料流体全部无效时整条跳过（否则生成一条不消耗原料就产出的配方，见熔炉适配器）
            if (validInputs == 0) {
                continue;
            }

            FluidStack output = alloy.result.copy();
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
