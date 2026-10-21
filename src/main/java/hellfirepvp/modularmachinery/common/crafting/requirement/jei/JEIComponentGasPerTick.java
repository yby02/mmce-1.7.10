package hellfirepvp.modularmachinery.common.crafting.requirement.jei;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementGasPerTick;
import hellfirepvp.modularmachinery.common.integration.recipe.RecipeLayoutPart;
import hellfirepvp.modularmachinery.common.machine.IOType;
import mekanism.api.gas.GasStack;
import net.minecraft.client.resources.I18n;

import java.awt.*;
import java.util.Collections;
import java.util.List;

/**
 * 1.7.10 移植：与 {@link JEIComponentGas} 相同，只为 NEI 配方页（client/nei/MachineRecipeHandler）提供版面与提示，气体格由配方页自绘。
 */
public class JEIComponentGasPerTick extends ComponentRequirement.JEIComponent<GasStack> {
    private final RequirementGasPerTick requirement;

    public JEIComponentGasPerTick(RequirementGasPerTick requirement) {
        this.requirement = requirement;
    }

    @Override
    public Class<GasStack> getJEIRequirementClass() {
        return GasStack.class;
    }

    @Override
    public List<GasStack> getJEIIORequirements() {
        return Collections.singletonList(requirement.required.copy());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public RecipeLayoutPart<GasStack> getLayoutPart(Point offset) {
        return new RecipeLayoutPart.GasTank(offset);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onJEIHoverTooltip(int slotIndex, boolean input, GasStack ingredient, List<String> tooltip) {
        IOType ioType = requirement.getActionType();
        if (ioType == IOType.INPUT) {
            tooltip.add(I18n.format("tooltip.fluid_pertick.in", ingredient.amount));
        } else if (ioType == IOType.OUTPUT) {
            tooltip.add(I18n.format("tooltip.fluid_pertick.out", ingredient.amount));
        }
    }
}
