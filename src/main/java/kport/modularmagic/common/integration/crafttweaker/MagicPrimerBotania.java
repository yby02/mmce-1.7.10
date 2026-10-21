package kport.modularmagic.common.integration.crafttweaker;

import github.kasuminova.mmce.compat.crt.ZenRegister;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipePrimer;
import hellfirepvp.modularmachinery.common.machine.IOType;
import kport.modularmagic.common.crafting.requirement.RequirementMana;
import minetweaker.annotations.ModOnly;
import stanhebben.zenscript.annotations.ZenExpansion;
import stanhebben.zenscript.annotations.ZenMethod;

/** RecipePrimer 的魔力方法（同上游 MagicPrimer）：{@code .addManaInput(1000, false)}、{@code .addManaOutput(5000, false)}。只在装有植物魔法时注册。 */
@ZenRegister
@ModOnly("Botania")
@ZenExpansion("mods.modularmachinery.RecipePrimer")
public final class MagicPrimerBotania {

    private MagicPrimerBotania() {
    }

    @ZenMethod
    public static RecipePrimer addManaInput(RecipePrimer primer, int amount, boolean perTick) {
        primer.appendComponent(new RequirementMana(IOType.INPUT, amount, perTick));
        return primer;
    }

    @ZenMethod
    public static RecipePrimer addManaOutput(RecipePrimer primer, int amount, boolean perTick) {
        primer.appendComponent(new RequirementMana(IOType.OUTPUT, amount, perTick));
        return primer;
    }
}
