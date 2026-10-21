package hellfirepvp.modularmachinery.common.integration.crafttweaker.modifier;

import github.kasuminova.mmce.compat.ItemStacks;

import github.kasuminova.mmce.compat.crt.CraftTweakerAPI;
import github.kasuminova.mmce.compat.crt.ZenRegister;
import minetweaker.api.item.IItemStack;
import github.kasuminova.mmce.compat.crt.CraftTweakerMC;
import hellfirepvp.modularmachinery.common.modifier.MultiBlockModifierReplacement;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import net.minecraft.item.ItemStack;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

@ZenRegister
@ZenClass("mods.modularmachinery.MultiblockModifierBuilder")
public class MultiBlockModifierBuilder {
    private final List<RecipeModifier> modifiers        = new LinkedList<>();
    private final List<String>         descriptions     = new ArrayList<>();
    private final String               modifierName;
    private       BlockArray           blockArray       = null;
    private       ItemStack            descriptiveStack = ItemStacks.EMPTY;

    private MultiBlockModifierBuilder() {
        this.modifierName = null;
    }

    private MultiBlockModifierBuilder(String modifierName) {
        this.modifierName = modifierName;
    }

    @ZenMethod
    public static MultiBlockModifierBuilder newBuilder() {
        return new MultiBlockModifierBuilder();
    }

    @ZenMethod
    public static MultiBlockModifierBuilder newBuilder(String modifierName) {
        return new MultiBlockModifierBuilder(modifierName);
    }

    @ZenMethod
    public MultiBlockModifierBuilder setBlockArray(BlockArray blockArray) {
        this.blockArray = blockArray;
        return this;
    }

    @ZenMethod
    public MultiBlockModifierBuilder addModifier(RecipeModifier... modifiers) {
        this.modifiers.addAll(Arrays.asList(modifiers));
        return this;
    }

    @ZenMethod
    public MultiBlockModifierBuilder setDescriptiveStack(final IItemStack descriptiveStack) {
        this.descriptiveStack = CraftTweakerMC.getItemStack(descriptiveStack);
        return this;
    }

    @ZenMethod
    public MultiBlockModifierReplacement build() {
        if (blockArray == null) {
            CraftTweakerAPI.logError("BlockArray cannot be null!");
            return null;
        } else {
            return new MultiBlockModifierReplacement(modifierName, blockArray, modifiers, descriptions, descriptiveStack);
        }
    }
}
