package kport.modularmagic.common.crafting.helper;

/**
 * 配方检查用的仓副本：记录检查开始时的数量，模拟取 / 放只改副本；真正取 / 放时操作原方块实体（{@link #original()}）。
 */
public abstract class AmountProviderCopy<O> {

    protected final O   original;
    protected       int amount;

    protected AmountProviderCopy(O original, int amount) {
        this.original = original;
        this.amount = amount;
    }

    public O original() {
        return original;
    }

    public int getAmount() {
        return amount;
    }

    public abstract int getCapacity();

    /** 模拟取出，返回取出的数量。 */
    public int simulateTake(int max) {
        int n = Math.max(0, Math.min(max, amount));
        amount -= n;
        return n;
    }

    /** 模拟放入，返回放入的数量。 */
    public int simulateAdd(int max) {
        int n = Math.max(0, Math.min(max, getCapacity() - amount));
        amount += n;
        return n;
    }
}
