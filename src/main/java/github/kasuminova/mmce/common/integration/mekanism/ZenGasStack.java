package github.kasuminova.mmce.common.integration.mekanism;

import github.kasuminova.mmce.compat.crt.ZenRegister;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasRegistry;
import mekanism.api.gas.GasStack;
import minetweaker.annotations.ModOnly;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemCondition;
import minetweaker.api.item.IItemStack;
import minetweaker.api.item.IItemTransformer;
import minetweaker.api.item.IngredientOr;
import minetweaker.api.liquid.ILiquidStack;
import minetweaker.api.player.IPlayer;
import stanhebben.zenscript.annotations.OperatorType;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenMethod;
import stanhebben.zenscript.annotations.ZenOperator;

import java.util.Collections;
import java.util.List;

/**
 * ZenScript 中的 Mekanism 气体（{@code <gas:名称>}，可 {@code * 数量}）。
 * <p>
 * 1.7.10 移植：1.12.2 由 Mekanism 自带的 CraftTweaker 集成提供 {@code mods.mekanism.gas.IGasStack}；1.7.10 的 Mekanism 没有
 * MineTweaker 集成，由本类（同名 ZenClass）与 {@link GasBracketHandler} 提供，使上游脚本的写法——
 * {@code addGasInput(<gas:x> * n)}、复数方法、{@code addInput} / {@code addOutput} 自动识别气体——可以直接使用。
 * 实现 {@link IIngredient} 只是为了能放进 {@code addInputs(...)} 等以 IIngredient 为参数的方法，它不匹配任何物品或流体。
 */
@ZenRegister
@ModOnly("Mekanism")
@ZenClass("mods.mekanism.gas.IGasStack")
public class ZenGasStack implements IIngredient {

    private final String name;
    private final int    amount;

    public ZenGasStack(String name, int amount) {
        this.name = name;
        this.amount = amount;
    }

    @ZenGetter("name")
    public String getName() {
        return name;
    }

    @Override
    @ZenGetter("amount")
    public int getAmount() {
        return amount;
    }

    @ZenGetter("displayName")
    public String getDisplayName() {
        Gas gas = GasRegistry.getGas(name);
        return gas == null ? name : gas.getLocalizedName();
    }

    @ZenOperator(OperatorType.MUL)
    @ZenMethod
    public ZenGasStack withAmount(int amount) {
        return new ZenGasStack(name, amount);
    }

    /** 对应的 Mekanism 气体；气体名未注册时为 null。 */
    public GasStack toGasStack() {
        Gas gas = GasRegistry.getGas(name);
        return gas == null ? null : new GasStack(gas, Math.max(0, amount));
    }

    // ---------------- IIngredient ----------------

    @Override
    public String getMark() {
        return null;
    }

    @Override
    public List<IItemStack> getItems() {
        return Collections.emptyList();
    }

    @Override
    public List<ILiquidStack> getLiquids() {
        return Collections.emptyList();
    }

    @Override
    public IIngredient amount(int amount) {
        return withAmount(amount);
    }

    @Override
    public IIngredient or(IIngredient ingredient) {
        return new IngredientOr(this, ingredient);
    }

    @Override
    public IIngredient transform(IItemTransformer transformer) {
        return this;
    }

    @Override
    public IIngredient only(IItemCondition condition) {
        return this;
    }

    @Override
    public IIngredient marked(String mark) {
        return this;
    }

    @Override
    public boolean matches(IItemStack item) {
        return false;
    }

    @Override
    public boolean matchesExact(IItemStack item) {
        return false;
    }

    @Override
    public boolean matches(ILiquidStack liquid) {
        return false;
    }

    @Override
    public boolean contains(IIngredient ingredient) {
        return ingredient instanceof ZenGasStack other && other.name.equals(name) && other.amount <= amount;
    }

    @Override
    public IItemStack applyTransform(IItemStack item, IPlayer byPlayer) {
        return item;
    }

    @Override
    public boolean hasTransformers() {
        return false;
    }

    @Override
    public Object getInternal() {
        return toGasStack();
    }

    @Override
    public String toString() {
        return "<gas:" + name + ">" + (amount == 1 ? "" : " * " + amount);
    }
}
