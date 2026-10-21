package github.kasuminova.mmce.compat.registry;

import github.kasuminova.mmce.compat.ResourceLocation;

/**
 * 1.7.10 移植适配：{@link SimpleRegistry} 中条目的最小契约（对应 1.12.2 IForgeRegistryEntry 的 getRegistryName）。
 */
public interface IRegistryEntry {
    ResourceLocation getRegistryName();
}
