package hellfirepvp.modularmachinery.common.crafting.adapter.ic2;

import hellfirepvp.modularmachinery.common.registry.RegistryRecipeAdapters;

/** 只在装有 IC2 时调用，避免未安装 IC2 时加载引用 IC2 类型的适配器类。 */
public final class IC2RecipeAdapters {

    private IC2RecipeAdapters() {
    }

    public static void register() {
        RegistryRecipeAdapters.registerAdapter(new AdapterIC2Compressor());
        RegistryRecipeAdapters.registerAdapter(new AdapterIC2Macerator());
        RegistryRecipeAdapters.registerAdapter(new AdapterIC2Extractor());
    }
}
