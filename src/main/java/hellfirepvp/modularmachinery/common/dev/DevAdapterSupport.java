package hellfirepvp.modularmachinery.common.dev;

import cofh.thermalexpansion.util.crafting.InsolatorManager;
import ic2.api.recipe.IMachineRecipeManager;
import ic2.api.recipe.Recipes;
import tconstruct.library.crafting.Smeltery;

/**
 * 仅开发环境：读取各模组自己的配方表大小，供自测 adapters 与适配器生成的配方数对照。
 * 每个方法只在对应模组已安装时由 {@link DevSelfTest} 调用。
 */
final class DevAdapterSupport {

    private DevAdapterSupport() {
    }

    static int ic2(String manager) {
        IMachineRecipeManager m = switch (manager) {
            case "macerator" -> Recipes.macerator;
            case "compressor" -> Recipes.compressor;
            case "extractor" -> Recipes.extractor;
            default -> null;
        };
        return m == null ? -1 : m.getRecipes().size();
    }

    static int ticMelting() {
        return Smeltery.getSmeltingList().size();
    }

    static int ticAlloy() {
        return Smeltery.getAlloyList().size();
    }

    static int teInsolator() {
        InsolatorManager.RecipeInsolator[] list = InsolatorManager.getRecipeList();
        return list == null ? -1 : list.length;
    }
}
