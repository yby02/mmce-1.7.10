package github.kasuminova.mmce.compat.crt;

import cpw.mods.fml.common.discovery.ASMDataTable;
import hellfirepvp.modularmachinery.ModularMachinery;
import minetweaker.MineTweakerAPI;
import minetweaker.annotations.ModOnly;
import cpw.mods.fml.common.Loader;

import java.util.Set;
import java.util.TreeSet;

/**
 * 把所有标注 {@link ZenRegister} 的类注册到 MineTweaker3（对应 CraftTweaker 1.12 的自动注册行为）。
 * 带 {@link ModOnly} 的类只在对应模组存在时注册。
 */
public final class ZenRegistration {

    private ZenRegistration() {
    }

    private static boolean allLoaded(String[] modIds) {
        for (String modId : modIds) {
            if (!Loader.isModLoaded(modId)) {
                return false;
            }
        }
        return true;
    }

    public static void registerAll(ASMDataTable asmData) {
        Set<String> classNames = new TreeSet<>();
        for (ASMDataTable.ASMData data : asmData.getAll(ZenRegister.class.getName())) {
            classNames.add(data.getClassName());
        }
        int registered = 0;
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className, false, ZenRegistration.class.getClassLoader());
                ModOnly modOnly = clazz.getAnnotation(ModOnly.class);
                if (modOnly != null && !allLoaded(modOnly.value())) {
                    continue;
                }
                MineTweakerAPI.registerClass(clazz);
                registered++;
            } catch (Throwable t) {
                ModularMachinery.log.error("[ModularMachinery] Failed to register ZenScript class " + className, t);
            }
        }
        ModularMachinery.log.info("[ModularMachinery] Registered {} ZenScript classes to MineTweaker3.", registered);
    }
}
