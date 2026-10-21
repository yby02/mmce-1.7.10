package hellfirepvp.modularmachinery.common.crafting.adapter.te;

import hellfirepvp.modularmachinery.common.registry.RegistryRecipeAdapters;

/** 只在装有热力膨胀时调用，避免未安装时加载引用热力膨胀类型的适配器类。 */
public final class TERecipeAdapters {

    private TERecipeAdapters() {
    }

    public static void register() {
        RegistryRecipeAdapters.registerAdapter(new AdapterTEInsolator());
    }
}
