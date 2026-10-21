package github.kasuminova.mmce.compat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * 1.7.10 移植适配：1.12.2 NonNullList 的替代。
 * 注意：1.7.10 用 null 表示空 ItemStack，因此本类<strong>允许</strong> null 元素（与 1.12.2 语义相反）。
 */
public class NonNullList<E> extends ArrayList<E> {

    public NonNullList() {
        super();
    }

    public NonNullList(Collection<? extends E> c) {
        super(c);
    }

    public static <E> NonNullList<E> create() {
        return new NonNullList<>();
    }

    public static <E> NonNullList<E> withSize(int size, E fill) {
        Object[] arr = new Object[size];
        Arrays.fill(arr, fill);
        @SuppressWarnings("unchecked")
        java.util.List<E> list = (java.util.List<E>) Arrays.asList(arr);
        return new NonNullList<>(list);
    }
}
