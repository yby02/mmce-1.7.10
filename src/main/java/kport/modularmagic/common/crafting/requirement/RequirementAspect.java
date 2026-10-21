package kport.modularmagic.common.crafting.requirement;

import cpw.mods.fml.common.registry.GameRegistry;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import kport.modularmagic.common.crafting.component.ModularMagicComponents;
import kport.modularmagic.common.crafting.helper.AspectProviderCopy;
import kport.modularmagic.common.crafting.requirement.types.ModularMagicRequirements;
import kport.modularmagic.common.crafting.requirement.types.RequirementTypeAspect;
import kport.modularmagic.common.integration.nei.JEIComponentMagic;
import kport.modularmagic.common.lib.ModularMagicBlocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 源质需求（神秘时代 4）：{ "type": "modularmachinery:aspect", "io-type": "input", "aspect": "ignis", "amount": 10 }。与上游一样不支持每 tick。 */
public class RequirementAspect extends RequirementAmount<ItemStack, RequirementTypeAspect> {

    public final Aspect aspect;

    public RequirementAspect(IOType actionType, int amount, Aspect aspect) {
        super(ModularMagicRequirements.REQUIREMENT_ASPECT, actionType, amount, false);
        this.aspect = aspect;
    }

    @Override
    public boolean isValidComponent(ProcessingComponent<?> component, RecipeCraftingContext ctx) {
        MachineComponent<?> cmp = component.getComponent();
        return cmp.getComponentType() == ModularMagicComponents.COMPONENT_ASPECT
            && component.getProvidedComponent() instanceof AspectProviderCopy
            && cmp.ioType == getActionType();
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public List<ProcessingComponent<?>> copyComponents(List<ProcessingComponent<?>> components) {
        List<ProcessingComponent<?>> list = new ArrayList<>();
        for (ProcessingComponent<?> component : components) {
            list.add(new ProcessingComponent<>((MachineComponent<Object>) component.component(),
                new AspectProviderCopy(((AspectProviderCopy) component.providedComponent()).original()), component.tag()));
        }
        return list;
    }

    @Override
    protected int take(Object provided, int max, boolean simulate) {
        AspectProviderCopy copy = (AspectProviderCopy) provided;
        if (simulate) {
            return copy.simulateTake(aspect, max);
        }
        int n = Math.min(max, copy.original().containerContains(aspect));
        return n > 0 && copy.original().takeFromContainer(aspect, n) ? n : 0;
    }

    @Override
    protected int add(Object provided, int max, boolean simulate) {
        AspectProviderCopy copy = (AspectProviderCopy) provided;
        return simulate ? copy.simulateAdd(aspect, max) : max - copy.original().addToContainer(aspect, max);
    }

    @Override
    protected String lessMessage() {
        return "error.modularmachinery.requirement.aspect.less";
    }

    @Override
    protected String spaceMessage() {
        return "error.modularmachinery.requirement.aspect.out";
    }

    @Override
    public RequirementAspect deepCopy() {
        return deepCopyModified(Collections.emptyList());
    }

    @Override
    public RequirementAspect deepCopyModified(List<RecipeModifier> modifiers) {
        return new RequirementAspect(actionType, Math.round(RecipeModifier.applyModifiers(modifiers, this, (float) amount, false)), aspect);
    }

    /** NEI 中显示为对应源质的晶体（神秘时代 4 的 ItemCrystalEssence），找不到时显示源质仓。 */
    @Override
    public JEIComponentMagic provideJEIComponent() {
        ItemStack display = null;
        Item crystal = GameRegistry.findItem("Thaumcraft", "ItemCrystalEssence");
        if (crystal instanceof IEssentiaContainerItem container) {
            display = new ItemStack(crystal);
            container.setAspects(display, new AspectList().add(aspect, amount));
        }
        if (display == null) {
            display = new ItemStack(actionType == IOType.INPUT ? ModularMagicBlocks.aspectProviderInput : ModularMagicBlocks.aspectProviderOutput);
        }
        return new JEIComponentMagic(display, Collections.singletonList(EnumChatFormatting.LIGHT_PURPLE
            + StatCollector.translateToLocalFormatted("tooltip.modularmachinery.magic.aspect", aspect.getName(), amount)));
    }
}
