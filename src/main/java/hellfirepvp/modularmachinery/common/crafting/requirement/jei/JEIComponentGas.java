package hellfirepvp.modularmachinery.common.crafting.requirement.jei;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementGas;
import hellfirepvp.modularmachinery.common.integration.recipe.RecipeLayoutPart;
import mekanism.api.gas.GasStack;

import java.awt.*;
import java.util.Collections;
import java.util.List;

/**
 * 1.7.10 移植：JEI 部件在 1.7.10 中只为 NEI 配方页（client/nei/MachineRecipeHandler）提供版面与提示；
 * 气体格由配方页自绘（Mekanism 1.7.10 没有气体显示物品）。
 */
public class JEIComponentGas extends ComponentRequirement.JEIComponent<GasStack> {

    private final RequirementGas requirement;

    public JEIComponentGas(RequirementGas requirement) {
        this.requirement = requirement;
    }

    @Override
    public Class<GasStack> getJEIRequirementClass() {
        return GasStack.class;
    }

    @Override
    public List<GasStack> getJEIIORequirements() {
        return Collections.singletonList(requirement.required);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public RecipeLayoutPart<GasStack> getLayoutPart(Point offset) {
        return new RecipeLayoutPart.GasTank(offset);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onJEIHoverTooltip(int slotIndex, boolean input, GasStack ingredient, List<String> tooltip) {
        JEIComponentItem.addChanceTooltip(input, tooltip, requirement.chance);
    }
}
