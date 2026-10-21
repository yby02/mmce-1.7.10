package kport.modularmagic.common.crafting.requirement;

import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import kport.modularmagic.common.crafting.component.ModularMagicComponents;
import kport.modularmagic.common.crafting.helper.LifeEssenceProviderCopy;
import kport.modularmagic.common.crafting.requirement.types.ModularMagicRequirements;
import kport.modularmagic.common.crafting.requirement.types.RequirementTypeLifeEssence;
import kport.modularmagic.common.integration.nei.JEIComponentMagic;
import kport.modularmagic.common.lib.ModularMagicBlocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 生命精华（LP）需求（血魔法 1）：{ "type": "modularmachinery:lifeessence", "io-type": "input", "amount": 500, "perTick": false }。 */
public class RequirementLifeEssence extends RequirementAmount<ItemStack, RequirementTypeLifeEssence> {

    public RequirementLifeEssence(IOType actionType, int amount, boolean perTick) {
        super(ModularMagicRequirements.REQUIREMENT_LIFE_ESSENCE, actionType, amount, perTick);
    }

    @Override
    public boolean isValidComponent(ProcessingComponent<?> component, RecipeCraftingContext ctx) {
        MachineComponent<?> cmp = component.getComponent();
        return cmp.getComponentType() == ModularMagicComponents.COMPONENT_LIFE_ESSENCE
            && component.getProvidedComponent() instanceof LifeEssenceProviderCopy
            && cmp.ioType == getActionType();
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public List<ProcessingComponent<?>> copyComponents(List<ProcessingComponent<?>> components) {
        List<ProcessingComponent<?>> list = new ArrayList<>();
        for (ProcessingComponent<?> component : components) {
            list.add(new ProcessingComponent<>((MachineComponent<Object>) component.component(),
                new LifeEssenceProviderCopy(((LifeEssenceProviderCopy) component.providedComponent()).original()), component.tag()));
        }
        return list;
    }

    @Override
    protected int take(Object provided, int max, boolean simulate) {
        LifeEssenceProviderCopy copy = (LifeEssenceProviderCopy) provided;
        return simulate ? copy.simulateTake(max) : copy.original().removeLifeEssenceCache(max);
    }

    @Override
    protected int add(Object provided, int max, boolean simulate) {
        LifeEssenceProviderCopy copy = (LifeEssenceProviderCopy) provided;
        return simulate ? copy.simulateAdd(max) : copy.original().addLifeEssenceCache(max);
    }

    @Override
    protected String lessMessage() {
        return "error.modularmachinery.requirement.lifeessence.lp.less";
    }

    @Override
    protected String spaceMessage() {
        return "error.modularmachinery.requirement.lifeessence.lp.more";
    }

    @Override
    public RequirementLifeEssence deepCopy() {
        return deepCopyModified(Collections.emptyList());
    }

    @Override
    public RequirementLifeEssence deepCopyModified(List<RecipeModifier> modifiers) {
        return new RequirementLifeEssence(actionType, Math.round(RecipeModifier.applyModifiers(modifiers, this, (float) amount, false)), perTick);
    }

    @Override
    public JEIComponentMagic provideJEIComponent() {
        ItemStack display = new ItemStack(actionType == IOType.INPUT ? ModularMagicBlocks.lifeEssenceProviderInput : ModularMagicBlocks.lifeEssenceProviderOutput);
        return new JEIComponentMagic(display, Collections.singletonList(EnumChatFormatting.RED
            + StatCollector.translateToLocalFormatted(perTick ? "tooltip.modularmachinery.magic.lifeessence.pertick" : "tooltip.modularmachinery.magic.lifeessence", amount)));
    }
}
