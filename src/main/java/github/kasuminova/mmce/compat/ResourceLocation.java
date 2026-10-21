package github.kasuminova.mmce.compat;

/**
 * 1.7.10 移植适配：1.7.10 的 net.minecraft.util.ResourceLocation 标注了 @SideOnly(CLIENT)，
 * 专用服务器上不存在。MMCE 大量用它作机器/需求类型/修改器等的注册键，因此提供这个两端通用的替代品。
 * 纹理绑定等客户端渲染仍应使用原版 ResourceLocation（见 {@link #toClient()}）。
 *
 * 与 1.12.2 一致：命名空间与路径统一转为小写；未写命名空间时默认 "minecraft"。
 * 注意：1.7.10 的物品/方块 ID 区分大小写，查找物品/方块时不要使用本类，见 {@link RegistryNames}。
 */
public class ResourceLocation implements Comparable<ResourceLocation> {

    protected final String namespace;
    protected final String path;

    public ResourceLocation(String resourceName) {
        this(split(resourceName));
    }

    public ResourceLocation(String namespace, String path) {
        this(new String[]{namespace, path});
    }

    protected ResourceLocation(String[] parts) {
        this.namespace = (parts[0] == null || parts[0].isEmpty() ? "minecraft" : parts[0]).toLowerCase(java.util.Locale.ROOT);
        this.path = parts[1].toLowerCase(java.util.Locale.ROOT);
    }

    protected static String[] split(String toSplit) {
        String[] result = {"minecraft", toSplit};
        int idx = toSplit.indexOf(':');
        if (idx >= 0) {
            result[1] = toSplit.substring(idx + 1);
            if (idx > 1) {
                result[0] = toSplit.substring(0, idx);
            }
        }
        return result;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getPath() {
        return path;
    }

    /** 1.12.2 旧名 */
    public String getResourceDomain() {
        return namespace;
    }

    /** 1.12.2 旧名 */
    public String getResourcePath() {
        return path;
    }

    /** 仅客户端调用：转换为原版 ResourceLocation 以绑定纹理等。 */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public net.minecraft.util.ResourceLocation toClient() {
        return new net.minecraft.util.ResourceLocation(namespace, path);
    }

    @Override
    public String toString() {
        return namespace + ':' + path;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ResourceLocation)) {
            return false;
        }
        ResourceLocation rl = (ResourceLocation) other;
        return namespace.equals(rl.namespace) && path.equals(rl.path);
    }

    @Override
    public int hashCode() {
        return 31 * namespace.hashCode() + path.hashCode();
    }

    @Override
    public int compareTo(ResourceLocation other) {
        int i = namespace.compareTo(other.namespace);
        return i != 0 ? i : path.compareTo(other.path);
    }
}
