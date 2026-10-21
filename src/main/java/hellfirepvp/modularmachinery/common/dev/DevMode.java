package hellfirepvp.modularmachinery.common.dev;

import net.minecraft.launchwrapper.Launch;

/**
 * 开发期测试工具（/mmce-test 命令、服务端自测、客户端自动场景）的开关。
 * 默认只在开发环境（deobf）启用；发布版中需要显式加 JVM 参数 -Dmmce.dev.tools=true（或指定 -Dmmce.dev.autotest=场景名）才会启用，
 * 用于在重混淆后的发布 jar 上复跑同样的测试。普通玩家的环境里这些工具不会注册任何命令或事件。
 */
public final class DevMode {

    private DevMode() {
    }

    public static boolean isDeobfEnvironment() {
        return Boolean.TRUE.equals(Launch.blackboard.get("fml.deobfuscatedEnvironment"));
    }

    public static boolean isEnabled() {
        return isDeobfEnvironment() || Boolean.getBoolean("mmce.dev.tools") || System.getProperty("mmce.dev.autotest") != null;
    }
}
