package github.kasuminova.mmce.compat;

/**
 * 1.7.10 移植适配：1.12.2 MathHelper 的方法名（1.7.10 中为 floor_double / clamp_int 等）。
 */
public final class MathHelper {

    private MathHelper() {
    }

    public static int floor(double value) {
        return net.minecraft.util.MathHelper.floor_double(value);
    }

    public static int floor(float value) {
        return net.minecraft.util.MathHelper.floor_float(value);
    }

    public static long lfloor(double value) {
        long i = (long) value;
        return value < (double) i ? i - 1L : i;
    }

    public static int ceil(double value) {
        return net.minecraft.util.MathHelper.ceiling_double_int(value);
    }

    public static int ceil(float value) {
        return net.minecraft.util.MathHelper.ceiling_float_int(value);
    }

    public static int clamp(int num, int min, int max) {
        return num < min ? min : Math.min(num, max);
    }

    public static long clamp(long num, long min, long max) {
        return num < min ? min : Math.min(num, max);
    }

    public static float clamp(float num, float min, float max) {
        return num < min ? min : Math.min(num, max);
    }

    public static double clamp(double num, double min, double max) {
        return num < min ? min : Math.min(num, max);
    }

    public static float sin(float value) {
        return net.minecraft.util.MathHelper.sin(value);
    }

    public static float cos(float value) {
        return net.minecraft.util.MathHelper.cos(value);
    }

    public static float sqrt(float value) {
        return net.minecraft.util.MathHelper.sqrt_float(value);
    }

    public static float sqrt(double value) {
        return net.minecraft.util.MathHelper.sqrt_double(value);
    }
}
