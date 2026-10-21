package kport.modularmagic.common.integration.crafttweaker;

import github.kasuminova.mmce.compat.crt.CraftTweakerAPI;
import github.kasuminova.mmce.compat.crt.ZenRegister;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipePrimer;
import hellfirepvp.modularmachinery.common.machine.IOType;
import kport.modularmagic.common.crafting.requirement.RequirementAspect;
import minetweaker.annotations.ModOnly;
import stanhebben.zenscript.annotations.ZenExpansion;
import stanhebben.zenscript.annotations.ZenMethod;
import thaumcraft.api.aspects.Aspect;

/**
 * RecipePrimer 的源质方法（同上游 MagicPrimer）：{@code .addAspectInput("ignis", 10)}、{@code .addAspectOutput("aqua", 5)}。只在装有神秘时代 4 时注册。
 */
@ZenRegister
@ModOnly("Thaumcraft")
@ZenExpansion("mods.modularmachinery.RecipePrimer")
public final class MagicPrimerThaumcraft {

    private MagicPrimerThaumcraft() {
    }

    private static RecipePrimer aspect(RecipePrimer primer, IOType io, String tag, int amount) {
        Aspect aspect = tag == null ? null : Aspect.getAspect(tag.toLowerCase());
        if (aspect == null) {
            CraftTweakerAPI.logError("[ModularMachinery] Unknown aspect '" + tag + "' (use the Thaumcraft 4 aspect tag, e.g. ignis)");
            return primer;
        }
        primer.appendComponent(new RequirementAspect(io, amount, aspect));
        return primer;
    }

    @ZenMethod
    public static RecipePrimer addAspectInput(RecipePrimer primer, String aspect, int amount) {
        return aspect(primer, IOType.INPUT, aspect, amount);
    }

    @ZenMethod
    public static RecipePrimer addAspectOutput(RecipePrimer primer, String aspect, int amount) {
        return aspect(primer, IOType.OUTPUT, aspect, amount);
    }
}
