/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.crafting;

import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementType;
import hellfirepvp.modularmachinery.common.lib.RegistriesMM;
import github.kasuminova.mmce.compat.ResourceLocation;
import cpw.mods.fml.common.Loader;
import github.kasuminova.mmce.compat.registry.SimpleRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: IntegrationTypeHelper
 * Created by HellFirePvP
 * Date: 13.07.2019 / 10:04
 */
public class IntegrationTypeHelper {

    public static void filterModIdComponents() {
        SimpleRegistry<ComponentType> componentTypeRegistry = RegistriesMM.COMPONENT_TYPE_REGISTRY;

        List<ComponentType> removableTypes = new ArrayList<>();
        for (ComponentType type : RegistriesMM.COMPONENT_TYPE_REGISTRY) {
            String modid = type.requiresModid();
            if (modid != null && !Loader.isModLoaded(modid)) {
                ModularMachinery.log.info("[Modular Machinery] Removing componentType " + type.getRegistryName() + " because " + modid + " is not loaded!");
                removableTypes.add(type);
            }
        }
        removableTypes.forEach(type -> componentTypeRegistry.remove(type.getRegistryName()));
    }

    public static void filterModIdRequirementTypes() {
        SimpleRegistry<RequirementType<?, ?>> requirementTypeRegistry = RegistriesMM.REQUIREMENT_TYPE_REGISTRY;

        List<RequirementType<?, ?>> removableTypes = new ArrayList<>();
        for (RequirementType<?, ?> type : RegistriesMM.REQUIREMENT_TYPE_REGISTRY) {
            String modid = type.requiresModid();
            if (modid != null && !Loader.isModLoaded(modid)) {
                ModularMachinery.log.info("[Modular Machinery] Removing requirementType " + type.getRegistryName() + " because " + modid + " is not loaded!");
                removableTypes.add(type);
            }
        }
        removableTypes.forEach(type -> requirementTypeRegistry.remove(type.getRegistryName()));
    }

    public static RequirementType<?, ?> searchRequirementType(String name) {
        for (RequirementType<?, ?> type : RegistriesMM.REQUIREMENT_TYPE_REGISTRY) {
            ResourceLocation key = type.getRegistryName();
            if (key.getPath().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }

}
