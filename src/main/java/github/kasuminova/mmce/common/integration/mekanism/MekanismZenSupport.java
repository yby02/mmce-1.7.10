package github.kasuminova.mmce.common.integration.mekanism;

import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipePrimer;
import hellfirepvp.modularmachinery.common.machine.IOType;
import minetweaker.MineTweakerAPI;
import minetweaker.api.item.IIngredient;

/** 只在装有 Mekanism 时调用：注册 {@code <gas:...>} 尖括号，供 RecipePrimer 的 addInput / addOutput 识别气体。 */
public final class MekanismZenSupport {

    private MekanismZenSupport() {
    }

    public static void registerBracketHandler() {
        MineTweakerAPI.registerBracketHandler(new GasBracketHandler());
    }

    public static boolean isGas(IIngredient ingredient) {
        return ingredient instanceof ZenGasStack;
    }

    public static void addGas(RecipePrimer primer, IIngredient ingredient, IOType ioType) {
        if (ioType == IOType.INPUT) {
            primer.addGasInput((ZenGasStack) ingredient);
        } else {
            primer.addGasOutput((ZenGasStack) ingredient);
        }
    }
}
