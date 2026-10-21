package github.kasuminova.mmce.compat.registry;

import github.kasuminova.mmce.compat.ResourceLocation;
import hellfirepvp.modularmachinery.ModularMachinery;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * 1.7.10 移植适配：1.7.10 的 Block/Item 没有注册名字段。MMCE 自己的方块与物品实现本接口，
 * 在注册前即可设置/读取 1.12.2 风格的注册名；注册时由 {@link hellfirepvp.modularmachinery.common.registry.internal.InternalRegistryPrimer}
 * 以注册名的 path 作为 GameRegistry 名称。
 */
public interface IRegistryNamed {

    Map<Object, ResourceLocation> NAMES = Collections.synchronizedMap(new IdentityHashMap<>());

    default ResourceLocation getRegistryName() {
        return NAMES.get(this);
    }

    default void setRegistryNameInternal(ResourceLocation name) {
        NAMES.put(this, name);
    }

    static void setName(Object target, String domain, String path) {
        NAMES.put(target, new ResourceLocation(domain, path));
    }

    static void setName(Object target, String name) {
        NAMES.put(target, new ResourceLocation(name.indexOf(':') >= 0 ? name : ModularMachinery.MODID + ":" + name));
    }

    static ResourceLocation getName(Object target) {
        return NAMES.get(target);
    }
}
