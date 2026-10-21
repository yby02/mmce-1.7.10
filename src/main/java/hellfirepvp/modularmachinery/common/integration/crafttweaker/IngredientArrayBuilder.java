package hellfirepvp.modularmachinery.common.integration.crafttweaker;

import github.kasuminova.mmce.compat.crt.ZenRegister;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("mods.modularmachinery.IngredientArrayBuilder")
public class IngredientArrayBuilder {
    @ZenMethod
    public static IngredientArrayPrimer newBuilder() {
        return new IngredientArrayPrimer();
    }
}
