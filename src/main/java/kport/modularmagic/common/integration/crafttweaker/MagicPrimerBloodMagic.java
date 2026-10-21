package kport.modularmagic.common.integration.crafttweaker;

import github.kasuminova.mmce.compat.crt.ZenRegister;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipePrimer;
import hellfirepvp.modularmachinery.common.machine.IOType;
import kport.modularmagic.common.crafting.requirement.RequirementLifeEssence;
import minetweaker.annotations.ModOnly;
import stanhebben.zenscript.annotations.ZenExpansion;
import stanhebben.zenscript.annotations.ZenMethod;

/** RecipePrimer 的生命精华方法（同上游 MagicPrimer）：{@code .addLifeEssenceInput(500, false)}、{@code .addLifeEssenceOutput(1000, false)}。只在装有血魔法 1 时注册。 */
@ZenRegister
@ModOnly("AWWayofTime")
@ZenExpansion("mods.modularmachinery.RecipePrimer")
public final class MagicPrimerBloodMagic {

    private MagicPrimerBloodMagic() {
    }

    @ZenMethod
    public static RecipePrimer addLifeEssenceInput(RecipePrimer primer, int amount, boolean perTick) {
        primer.appendComponent(new RequirementLifeEssence(IOType.INPUT, amount, perTick));
        return primer;
    }

    @ZenMethod
    public static RecipePrimer addLifeEssenceOutput(RecipePrimer primer, int amount, boolean perTick) {
        primer.appendComponent(new RequirementLifeEssence(IOType.OUTPUT, amount, perTick));
        return primer;
    }
}
