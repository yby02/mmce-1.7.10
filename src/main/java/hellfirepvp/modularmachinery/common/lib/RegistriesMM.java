/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.lib;

import github.kasuminova.mmce.compat.ResourceLocation;
import github.kasuminova.mmce.compat.registry.SimpleRegistry;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.crafting.adapter.RecipeAdapter;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementType;
import hellfirepvp.modularmachinery.common.crafting.tooltip.RequirementTip;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: RegistriesMM
 * Created by HellFirePvP
 * <p>
 * 1.7.10 移植：ForgeRegistry 替换为 {@link SimpleRegistry}，在类加载时即可用。
 */
public class RegistriesMM {

    public static final ResourceLocation                     ADAPTER_REGISTRY_NAME          = new ResourceLocation(ModularMachinery.MODID, "recipeadapters");
    public static final ResourceLocation                     COMPONENT_TYPE_REGISTRY_NAME   = new ResourceLocation(ModularMachinery.MODID, "componenttypes");
    public static final ResourceLocation                     REQUIREMENT_TYPE_REGISTRY_NAME = new ResourceLocation(ModularMachinery.MODID, "requirementtypes");
    public static final ResourceLocation                     REQUIREMENT_TIPS_REGISTRY_NAME = new ResourceLocation(ModularMachinery.MODID, "requirementtips");
    public static final SimpleRegistry<RecipeAdapter>         ADAPTER_REGISTRY          = new SimpleRegistry<>(ADAPTER_REGISTRY_NAME);
    public static final SimpleRegistry<ComponentType>         COMPONENT_TYPE_REGISTRY   = new SimpleRegistry<>(COMPONENT_TYPE_REGISTRY_NAME);
    public static final SimpleRegistry<RequirementType<?, ?>> REQUIREMENT_TYPE_REGISTRY = new SimpleRegistry<>(REQUIREMENT_TYPE_REGISTRY_NAME);
    public static final SimpleRegistry<RequirementTip>        REQUIREMENT_TIPS_REGISTRY = new SimpleRegistry<>(REQUIREMENT_TIPS_REGISTRY_NAME);

    private RegistriesMM() {
    }

}
