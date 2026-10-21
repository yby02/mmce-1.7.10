package kport.modularmagic.common.crafting.requirement;

import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.helper.CraftCheck;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementType;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.util.Asyncable;
import hellfirepvp.modularmachinery.common.util.ResultChance;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Modular Magic 中“按数量取 / 放”的需求（源质、魔力、生命精华）的公共实现：可并行，可选每 tick 进行（perTick）。
 * 子类只需实现对单个仓（的检查副本或原方块实体）取 / 放若干数量。
 * <p>
 * 1.7.10 移植：上游三个需求类中重复的 takeAll / addAll 合并到这里，并修正上游的两处问题——
 * <ul>
 *   <li>全部满足时返回的是“数量”而不是“并行份数”（部分满足时又返回份数），getMaxParallelism 因此可能得到远大于实际可用的并行数；
 *       这里统一返回份数。</li>
 *   <li>上游 RequirementLifeEssence#finishCrafting 判断的是 INPUT，输出生命精华的配方（非每 tick）结束时不产出，输入的反而在结束时尝试再加回；
 *       这里与魔力、源质一致，在结束时处理 OUTPUT。</li>
 * </ul>
 */
public abstract class RequirementAmount<T, V extends RequirementType<T, ? extends ComponentRequirement.PerTick<T, V>>>
    extends ComponentRequirement.PerTickParallelizable<T, V> implements Asyncable {

    public final int     amount;
    public final boolean perTick;

    protected RequirementAmount(V type, IOType actionType, int amount, boolean perTick) {
        super(type, actionType);
        this.amount = amount;
        this.perTick = perTick;
    }

    /** 从仓中取出至多 max；simulate 时操作检查副本（ProcessingComponent 提供的对象），否则操作原方块实体。返回实际取出的数量。 */
    protected abstract int take(Object provided, int max, boolean simulate);

    /** 向仓中放入至多 max，返回实际放入的数量。 */
    protected abstract int add(Object provided, int max, boolean simulate);

    protected abstract String lessMessage();

    protected abstract String spaceMessage();

    /** 对全部组件取 / 放 maxMultiplier 份，返回满足的份数（不超过 maxMultiplier）。 */
    protected int transferAll(List<ProcessingComponent<?>> components, RecipeCraftingContext context, float maxMultiplier, boolean simulate, boolean take) {
        int unit = Math.round(RecipeModifier.applyModifiers(context, this, (float) this.amount, false));
        if (unit <= 0) {
            return (int) maxMultiplier;
        }
        int max = (int) (unit * maxMultiplier);
        int done = 0;
        for (ProcessingComponent<?> component : components) {
            Object provided = component == null ? null : component.getProvidedComponent();
            if (provided == null) {
                continue;
            }
            done += take ? take(provided, max - done, simulate) : add(provided, max - done, simulate);
            if (done >= max) {
                break;
            }
        }
        return done / unit;
    }

    @Nonnull
    @Override
    public CraftCheck canStartCrafting(List<ProcessingComponent<?>> components, RecipeCraftingContext context) {
        switch (getActionType()) {
            case INPUT -> {
                if (transferAll(components, context, parallelism, true, true) < parallelism) {
                    return CraftCheck.failure(lessMessage());
                }
            }
            case OUTPUT -> {
                if (!ignoreOutputCheck && transferAll(components, context, parallelism, true, false) < parallelism) {
                    return CraftCheck.failure(spaceMessage());
                }
            }
        }
        return CraftCheck.success();
    }

    @Override
    public CraftCheck doIOTick(List<ProcessingComponent<?>> components, RecipeCraftingContext context, float durationMultiplier) {
        if (!perTick) {
            return CraftCheck.success();
        }
        switch (getActionType()) {
            case INPUT -> {
                if (transferAll(components, context, parallelism, false, true) < parallelism) {
                    return CraftCheck.failure(lessMessage());
                }
            }
            case OUTPUT -> {
                if (transferAll(components, context, parallelism, false, false) < parallelism) {
                    return CraftCheck.failure(spaceMessage());
                }
            }
        }
        return CraftCheck.success();
    }

    @Override
    public void startCrafting(List<ProcessingComponent<?>> components, RecipeCraftingContext context, ResultChance chance) {
        if (!perTick && getActionType() == IOType.INPUT) {
            transferAll(components, context, parallelism, false, true);
        }
    }

    @Override
    public void finishCrafting(List<ProcessingComponent<?>> components, RecipeCraftingContext context, ResultChance chance) {
        if (!perTick && getActionType() == IOType.OUTPUT) {
            transferAll(components, context, parallelism, false, false);
        }
    }

    @Override
    public int getMaxParallelism(List<ProcessingComponent<?>> components, RecipeCraftingContext context, int maxParallelism) {
        if (ignoreOutputCheck && actionType == IOType.OUTPUT) {
            return maxParallelism;
        }
        boolean take = actionType == IOType.INPUT;
        if (parallelizeUnaffected) {
            return transferAll(components, context, 1, true, take) >= 1 ? maxParallelism : 0;
        }
        return transferAll(components, context, maxParallelism, true, take);
    }

    @Nonnull
    @Override
    public String getMissingComponentErrorMessage(IOType ioType) {
        return "error.modularmachinery.component.invalid";
    }
}
