/*
 * 移植自 gugu-utils（https://github.com/ParaParty/gugu-utils，MIT License，Copyright (c) 2020 派瑞派对）。
 * 1.7.10 移植：原模组基于旧版 Modular Machinery 的逐组件 PerTick 需求（RequirementConsumePerTick）：配方开始前与运行中每 tick
 * 逐个检查机器上的环境传感器，任一传感器在其所在位置满足条件即通过。这里用 MMCE 的 PerTickParallelizable 一次拿到全部传感器，
 * 判定方式相同；并行时不限制并行数（满足条件时返回最大并行数，否则为 0）。
 */
package com.warmthdawn.mod.gugu_utils.modularmachenary.requirements;

import com.warmthdawn.mod.gugu_utils.common.Constants;
import com.warmthdawn.mod.gugu_utils.jei.JEIComponentEnvironment;
import com.warmthdawn.mod.gugu_utils.modularmachenary.MMCompoments;
import com.warmthdawn.mod.gugu_utils.modularmachenary.environment.TileEnvironmentHatch;
import com.warmthdawn.mod.gugu_utils.modularmachenary.environment.envtypes.EnvironmentType;
import com.warmthdawn.mod.gugu_utils.modularmachenary.requirements.types.RequirementTypeEnvironment;
import github.kasuminova.mmce.common.concurrent.Sync;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.helper.CraftCheck;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class RequirementEnvironment extends ComponentRequirement.PerTickParallelizable<EnvironmentType, RequirementTypeEnvironment> {

    private final EnvironmentType type;

    public RequirementEnvironment(RequirementTypeEnvironment requirementType, EnvironmentType type, IOType actionType) {
        super(requirementType, actionType);
        this.type = type;
    }

    public EnvironmentType getType() {
        return type;
    }

    /** 错误信息键（同原模组）：craftcheck.failure.gugu-utils:environment.&lt;类型名&gt;。 */
    public String failureKey() {
        return "craftcheck.failure." + Constants.STRING_RESOURCE_ENVIRONMENT + "." + type.getName();
    }

    /** 任一环境传感器在其所在位置满足条件即为真。 */
    public boolean anySensorMeets(List<ProcessingComponent<?>> components) {
        AtomicBoolean met = new AtomicBoolean(false);
        Sync.doSyncAction(() -> {
            for (ProcessingComponent<?> component : components) {
                if (component.getProvidedComponent() instanceof TileEnvironmentHatch hatch && hatch.getWorldObj() != null
                    && type.isMeet(hatch.getWorldObj(), hatch.getPos())) {
                    met.set(true);
                    return;
                }
            }
        });
        return met.get();
    }

    @Override
    public boolean isValidComponent(ProcessingComponent<?> component, RecipeCraftingContext ctx) {
        MachineComponent<?> cmp = component.component();
        return cmp.getComponentType().equals(MMCompoments.COMPONENT_ENVIRONMENT) && cmp.ioType == getActionType();
    }

    @Nonnull
    @Override
    public List<ProcessingComponent<?>> copyComponents(List<ProcessingComponent<?>> components) {
        // 传感器只被读取、不被修改，检查时无需副本
        return components;
    }

    @Nonnull
    @Override
    public CraftCheck canStartCrafting(List<ProcessingComponent<?>> components, RecipeCraftingContext context) {
        return anySensorMeets(components) ? CraftCheck.success() : CraftCheck.failure(failureKey());
    }

    @Override
    public CraftCheck doIOTick(List<ProcessingComponent<?>> components, RecipeCraftingContext context, float durationMultiplier) {
        return anySensorMeets(components) ? CraftCheck.success() : CraftCheck.failure(failureKey());
    }

    @Override
    public int getMaxParallelism(List<ProcessingComponent<?>> components, RecipeCraftingContext context, int maxParallelism) {
        return anySensorMeets(components) ? maxParallelism : 0;
    }

    @Override
    public RequirementEnvironment deepCopy() {
        RequirementEnvironment copy = new RequirementEnvironment(getRequirementType(), type, getActionType());
        copy.postDeepCopy(this);
        return copy;
    }

    @Override
    public RequirementEnvironment deepCopyModified(List<RecipeModifier> modifiers) {
        return deepCopy();
    }

    @Nonnull
    @Override
    public String getMissingComponentErrorMessage(IOType ioType) {
        return "component.missing." + Constants.STRING_RESOURCE_ENVIRONMENT + "." + ioType.name().toLowerCase();
    }

    @Override
    public JEIComponent<EnvironmentType> provideJEIComponent() {
        return new JEIComponentEnvironment(type);
    }
}
