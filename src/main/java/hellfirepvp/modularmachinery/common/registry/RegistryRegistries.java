/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.registry;

/**
 * 1.7.10 移植：1.12.2 在 RegistryEvent.NewRegistry 中创建 ForgeRegistry；
 * 1.7.10 的 {@link hellfirepvp.modularmachinery.common.lib.RegistriesMM} 在类加载时即已创建，这里只按 1.12.2 的事件顺序
 * （需求类型 → 组件类型 → 配方适配器 → 需求提示 → 方块 → 物品）依次初始化各注册内容。
 */
public class RegistryRegistries {

    private RegistryRegistries() {
    }

    /** 在 preInit 中调用。 */
    public static void initializeAll() {
        RegistryRequirementTypes.initialize();
        RegistryComponentTypes.initialize();
        RegistryRecipeAdapters.initialize();
        RegistryRequirementTips.initialize();
        RegistryBlocks.initialize();
        RegistryItems.initialize();
    }

}
