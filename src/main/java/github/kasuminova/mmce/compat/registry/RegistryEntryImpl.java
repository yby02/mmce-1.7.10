package github.kasuminova.mmce.compat.registry;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import github.kasuminova.mmce.compat.ResourceLocation;

/**
 * 1.7.10 移植适配：替代 Forge 1.12.2 的 IForgeRegistryEntry.Impl，为 MMCE 自定义注册表中的条目提供注册名。
 */
public abstract class RegistryEntryImpl<V> implements IRegistryEntry {

    private ResourceLocation registryName = null;

    @SuppressWarnings("unchecked")
    public final V setRegistryName(String name) {
        if (getRegistryName() != null) {
            throw new IllegalStateException("Attempted to set registry name with existing registry name! New: " + name + " Old: " + getRegistryName());
        }
        this.registryName = new ResourceLocation(name.indexOf(':') >= 0 ? name : activeModId() + ":" + name);
        return (V) this;
    }

    public final V setRegistryName(ResourceLocation name) {
        return setRegistryName(name.toString());
    }

    public final V setRegistryName(String modID, String name) {
        return setRegistryName(modID + ":" + name);
    }

    @Override
    public final ResourceLocation getRegistryName() {
        return registryName;
    }

    private static String activeModId() {
        ModContainer mc = Loader.instance().activeModContainer();
        return mc == null ? "minecraft" : mc.getModId().toLowerCase(java.util.Locale.ROOT);
    }
}
