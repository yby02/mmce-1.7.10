package github.kasuminova.mmce.compat;

/**
 * 1.7.10 移植适配：1.7.10 的 net.minecraft.util.Tuple 不是泛型类，这里提供与 1.12.2 相同的泛型版本。
 */
public class Tuple<A, B> {

    private final A a;
    private final B b;

    public Tuple(A a, B b) {
        this.a = a;
        this.b = b;
    }

    public A getFirst() {
        return a;
    }

    public B getSecond() {
        return b;
    }
}
