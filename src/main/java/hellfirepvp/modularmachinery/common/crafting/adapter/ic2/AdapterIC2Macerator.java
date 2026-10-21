package hellfirepvp.modularmachinery.common.crafting.adapter.ic2;

import ic2.api.recipe.IMachineRecipeManager;
import ic2.api.recipe.Recipes;

/** IC2 打粉机（300 t、8 RF/t，同上游）。配方键 ic2:te_macerator。 */
public class AdapterIC2Macerator extends AdapterIC2Machine {

    public AdapterIC2Macerator() {
        super("macerator", 300);
    }

    @Override
    protected IMachineRecipeManager getManager() {
        return Recipes.macerator;
    }
}
