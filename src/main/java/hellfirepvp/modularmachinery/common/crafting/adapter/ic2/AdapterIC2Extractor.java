package hellfirepvp.modularmachinery.common.crafting.adapter.ic2;

import ic2.api.recipe.IMachineRecipeManager;
import ic2.api.recipe.Recipes;

/** IC2 提取机（300 t、8 RF/t；上游 1.12.2 没有这个适配器，1.7.10 的接口与压缩机 / 打粉机相同）。配方键 ic2:te_extractor。 */
public class AdapterIC2Extractor extends AdapterIC2Machine {

    public AdapterIC2Extractor() {
        super("extractor", 300);
    }

    @Override
    protected IMachineRecipeManager getManager() {
        return Recipes.extractor;
    }
}
