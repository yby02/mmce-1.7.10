package hellfirepvp.modularmachinery.common.integration.crafttweaker.helper;

import github.kasuminova.mmce.compat.crt.ZenRegister;
import minetweaker.api.item.IItemStack;
import github.kasuminova.mmce.common.helper.IMachineController;
import stanhebben.zenscript.annotations.ZenClass;

@ZenRegister
@ZenClass("mods.modularmachinery.AdvancedItemModifier")
@FunctionalInterface
public interface AdvancedItemModifierCT {
    IItemStack apply(IMachineController controller, IItemStack stack);
}
