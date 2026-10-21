package github.kasuminova.mmce.compat.crt;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 1.7.10 移植适配：替代 CraftTweaker 1.12 的 crafttweaker.annotations.ZenRegister。
 * MineTweaker3 没有自动扫描注册，带此注解的类在 preInit 时由 {@link ZenRegistration} 通过 ASM 注解表统一调用
 * MineTweakerAPI.registerClass 注册。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ZenRegister {
}
