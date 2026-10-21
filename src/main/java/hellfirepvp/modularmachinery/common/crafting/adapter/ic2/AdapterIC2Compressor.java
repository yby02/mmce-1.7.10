package hellfirepvp.modularmachinery.common.crafting.adapter.ic2;

import ic2.api.recipe.IMachineRecipeManager;
import ic2.api.recipe.Recipes;

/** IC2 压缩机（300 t、8 RF/t，同上游）。配方键 ic2:te_compressor。 */
public class AdapterIC2Compressor extends AdapterIC2Machine {

    public AdapterIC2Compressor() {
        super("compressor", 300);
    }

    @Override
    protected IMachineRecipeManager getManager() {
        return Recipes.compressor;
    }
}
