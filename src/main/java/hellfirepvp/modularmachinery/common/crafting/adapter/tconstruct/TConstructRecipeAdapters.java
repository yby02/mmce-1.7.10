package hellfirepvp.modularmachinery.common.crafting.adapter.tconstruct;

import hellfirepvp.modularmachinery.common.registry.RegistryRecipeAdapters;

/** 只在装有匠魂时调用，避免未安装时加载引用匠魂类型的适配器类。 */
public final class TConstructRecipeAdapters {

    private TConstructRecipeAdapters() {
    }

    public static void register() {
        RegistryRecipeAdapters.registerAdapter(new AdapterSmelteryMeltingRecipe());
        RegistryRecipeAdapters.registerAdapter(new AdapterSmelteryAlloyRecipe());
    }
}
