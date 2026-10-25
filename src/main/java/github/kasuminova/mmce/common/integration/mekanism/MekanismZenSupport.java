package github.kasuminova.mmce.common.integration.mekanism;

import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipePrimer;
import hellfirepvp.modularmachinery.common.machine.IOType;
import mekanism.api.gas.GasRegistry;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import minetweaker.api.liquid.ILiquidStack;

import java.lang.reflect.Method;

/**
 * 只在装有 Mekanism 时调用：从 ZenScript 的气体对象中取出气体注册名与数量。
 * <p>
 * 1.7.10 的 Mekanism 气体 ZenScript 类型由 <b>ModTweaker2</b> 提供（{@code mods.mekanism.gas.IGasStack}，{@code <gas:...>}）。
 * 本移植早期也注册过同名的 {@code @ZenClass} 与 {@code <gas:...>} 尖括号，与 ModTweaker 冲突：同一个 Zen 类名被注册两次，
 * {@code <gas:x>} 解析出的对象不一定是其他模组方法所期望的 Java 类型，于是这些模组的 {@code addRecipe(...)} 全部报
 * “a method available but none matches the parameters”。该注册已移除。
 * <p>
 * 这里改为鸭子类型：任何提供 {@code getName()} / {@code getAmount()} 的 {@link IIngredient}（且名字能解析为已注册气体）
 * 都可当作气体使用，因此 ModTweaker 的 {@code <gas:...>} 也能直接传给 {@code addGasInput} 等方法，不依赖具体实现类。
 */
public final class MekanismZenSupport {

    private MekanismZenSupport() {
    }

    private static Method findNoArgMethod(Class<?> clazz, String name) {
        try {
            Method method = clazz.getMethod(name);
            if (method.getParameterCount() != 0) {
                return null;
            }
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException | SecurityException e) {
            return null;
        }
    }

    /** 该 IIngredient 是否表示一个 Mekanism 气体：不是物品、不是流体，且名字能解析为已注册气体。 */
    public static boolean isGas(IIngredient ingredient) {
        if (ingredient == null || ingredient instanceof IItemStack || ingredient instanceof ILiquidStack) {
            return false;
        }
        String name = gasName(ingredient);
        return name != null && GasRegistry.getGas(name) != null;
    }

    /** 读取气体注册名；不是气体对象时为 null。 */
    public static String gasName(IIngredient ingredient) {
        if (ingredient == null) {
            return null;
        }
        Method getName = findNoArgMethod(ingredient.getClass(), "getName");
        if (getName == null) {
            return null;
        }
        try {
            Object value = getName.invoke(ingredient);
            return value instanceof String name ? name : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    /** 读取数量；对象没有 {@code getAmount()} 时按 1 计。 */
    public static int gasAmount(IIngredient ingredient) {
        if (ingredient == null) {
            return 1;
        }
        Method getAmount = findNoArgMethod(ingredient.getClass(), "getAmount");
        if (getAmount == null) {
            return 1;
        }
        try {
            Object value = getAmount.invoke(ingredient);
            return value instanceof Integer amount ? amount : 1;
        } catch (Exception ignored) {
            return 1;
        }
    }

    /** {@code addInput} / {@code addOutput} 识别出气体时调用。 */
    public static void addGas(RecipePrimer primer, IIngredient ingredient, IOType ioType) {
        String name = gasName(ingredient);
        if (name == null) {
            return;
        }
        if (ioType == IOType.INPUT) {
            primer.addGasInput(name, gasAmount(ingredient));
        } else {
            primer.addGasOutput(name, gasAmount(ingredient));
        }
    }
}
