package kport.modularmagic.common.crafting.component;

import github.kasuminova.mmce.compat.ResourceLocation;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.crafting.ComponentType;

import javax.annotation.Nullable;

/**
 * Modular Magic 的组件类型（1.7.10 子集）：源质（神秘时代 4）、魔力（植物魔法）、生命精华（血魔法 1）。
 * 注册名同上游（modularmachinery:aspect / mana / lifeessence）。上游的星能、灵气、GP、恶魔意志、Impetus 没有 1.7.10 对应模组，未移植。
 */
public final class ModularMagicComponents {

    public static final ResourceLocation KEY_COMPONENT_ASPECT       = new ResourceLocation(ModularMachinery.MODID, "aspect");
    public static final ResourceLocation KEY_COMPONENT_MANA         = new ResourceLocation(ModularMachinery.MODID, "mana");
    public static final ResourceLocation KEY_COMPONENT_LIFE_ESSENCE = new ResourceLocation(ModularMachinery.MODID, "lifeessence");

    public static ComponentType COMPONENT_ASPECT;
    public static ComponentType COMPONENT_MANA;
    public static ComponentType COMPONENT_LIFE_ESSENCE;

    private ModularMagicComponents() {
    }

    private static final class ModComponent extends ComponentType {
        private final String modid;

        private ModComponent(String modid) {
            this.modid = modid;
        }

        @Nullable
        @Override
        public String requiresModid() {
            return modid;
        }
    }

    public static void initComponents() {
        if (Mods.THAUMCRAFT.isPresent()) {
            COMPONENT_ASPECT = register(new ModComponent(Mods.THAUMCRAFT.modid), KEY_COMPONENT_ASPECT);
        }
        if (Mods.BOTANIA.isPresent()) {
            COMPONENT_MANA = register(new ModComponent(Mods.BOTANIA.modid), KEY_COMPONENT_MANA);
        }
        if (Mods.BLOODMAGIC.isPresent()) {
            COMPONENT_LIFE_ESSENCE = register(new ModComponent(Mods.BLOODMAGIC.modid), KEY_COMPONENT_LIFE_ESSENCE);
        }
    }

    private static ComponentType register(ComponentType type, ResourceLocation key) {
        type.setRegistryName(key);
        CommonProxy.registryPrimer.register(type);
        return type;
    }
}
