package kport.modularmagic.common.crafting.requirement;

import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import kport.modularmagic.common.crafting.component.ModularMagicComponents;
import kport.modularmagic.common.crafting.helper.ManaProviderCopy;
import kport.modularmagic.common.crafting.requirement.types.ModularMagicRequirements;
import kport.modularmagic.common.crafting.requirement.types.RequirementTypeMana;
import kport.modularmagic.common.integration.nei.JEIComponentMagic;
import kport.modularmagic.common.lib.ModularMagicBlocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 魔力需求（植物魔法）：{ "type": "modularmachinery:mana", "io-type": "input", "amount": 1000, "perTick": false }。 */
public class RequirementMana extends RequirementAmount<ItemStack, RequirementTypeMana> {

    public RequirementMana(IOType actionType, int amount, boolean perTick) {
        super(ModularMagicRequirements.REQUIREMENT_MANA, actionType, amount, perTick);
    }

    @Override
    public boolean isValidComponent(ProcessingComponent<?> component, RecipeCraftingContext ctx) {
        MachineComponent<?> cmp = component.getComponent();
        return cmp.getComponentType() == ModularMagicComponents.COMPONENT_MANA
            && component.getProvidedComponent() instanceof ManaProviderCopy
            && cmp.ioType == getActionType();
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public List<ProcessingComponent<?>> copyComponents(List<ProcessingComponent<?>> components) {
        List<ProcessingComponent<?>> list = new ArrayList<>();
        for (ProcessingComponent<?> component : components) {
            list.add(new ProcessingComponent<>((MachineComponent<Object>) component.component(),
                new ManaProviderCopy(((ManaProviderCopy) component.providedComponent()).original()), component.tag()));
        }
        return list;
    }

    @Override
    protected int take(Object provided, int max, boolean simulate) {
        ManaProviderCopy copy = (ManaProviderCopy) provided;
        return simulate ? copy.simulateTake(max) : copy.original().reduceMana(max);
    }

    @Override
    protected int add(Object provided, int max, boolean simulate) {
        ManaProviderCopy copy = (ManaProviderCopy) provided;
        return simulate ? copy.simulateAdd(max) : copy.original().addMana(max);
    }

    @Override
    protected String lessMessage() {
        return "error.modularmachinery.requirement.mana.less";
    }

    @Override
    protected String spaceMessage() {
        return "error.modularmachinery.requirement.mana.space";
    }

    @Override
    public RequirementMana deepCopy() {
        return deepCopyModified(Collections.emptyList());
    }

    @Override
    public RequirementMana deepCopyModified(List<RecipeModifier> modifiers) {
        return new RequirementMana(actionType, Math.round(RecipeModifier.applyModifiers(modifiers, this, (float) amount, false)), perTick);
    }

    @Override
    public JEIComponentMagic provideJEIComponent() {
        ItemStack display = new ItemStack(actionType == IOType.INPUT ? ModularMagicBlocks.manaProviderInput : ModularMagicBlocks.manaProviderOutput);
        return new JEIComponentMagic(display, Collections.singletonList(EnumChatFormatting.AQUA
            + StatCollector.translateToLocalFormatted(perTick ? "tooltip.modularmachinery.magic.mana.pertick" : "tooltip.modularmachinery.magic.mana", amount)));
    }

}
