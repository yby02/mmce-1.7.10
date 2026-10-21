package github.kasuminova.mmce.compat.crt;

import minetweaker.MineTweakerAPI;

/**
 * 1.7.10 移植适配：CraftTweaker 1.12 crafttweaker.CraftTweakerAPI 的门面，转发到 MineTweaker3。
 */
public final class CraftTweakerAPI {

    private CraftTweakerAPI() {
    }

    public static void logInfo(String message) {
        MineTweakerAPI.logInfo(message);
    }

    public static void logWarning(String message) {
        MineTweakerAPI.logWarning(message);
    }

    public static void logError(String message) {
        MineTweakerAPI.logError(message);
    }

    public static void logError(String message, Throwable exception) {
        MineTweakerAPI.logError(message, exception);
    }
}
