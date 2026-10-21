package github.kasuminova.mmce.compat.registry;

import github.kasuminova.mmce.compat.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 1.7.10 移植适配：替代 MMCE 在 1.12.2 中通过 RegistryBuilder 创建的 ForgeRegistry（不保存、可修改的纯内存注册表）。
 * 保持注册顺序，便于与 1.12.2 的遍历顺序一致。
 */
public class SimpleRegistry<V extends IRegistryEntry> implements Iterable<V> {

    private final ResourceLocation name;
    private final Map<ResourceLocation, V> entries = new LinkedHashMap<>();

    public SimpleRegistry(ResourceLocation name) {
        this.name = name;
    }

    public ResourceLocation getRegistryName() {
        return name;
    }

    public synchronized void register(V value) {
        ResourceLocation key = value.getRegistryName();
        if (key == null) {
            throw new IllegalArgumentException("Can't register an entry without registry name to " + name + ": " + value);
        }
        entries.put(key, value);
    }

    public synchronized V getValue(ResourceLocation key) {
        return entries.get(key);
    }

    public synchronized boolean containsKey(ResourceLocation key) {
        return entries.containsKey(key);
    }

    public synchronized V remove(ResourceLocation key) {
        return entries.remove(key);
    }

    public synchronized Collection<V> getValuesCollection() {
        return Collections.unmodifiableCollection(new java.util.ArrayList<>(entries.values()));
    }

    public synchronized Set<ResourceLocation> getKeys() {
        return Collections.unmodifiableSet(new java.util.LinkedHashSet<>(entries.keySet()));
    }

    /** 遍历注册时刻的快照，遍历中可安全 remove。 */
    @Override
    public Iterator<V> iterator() {
        return getValuesCollection().iterator();
    }
}
