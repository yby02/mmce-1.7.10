/*
 * 移植自 gugu-utils（https://github.com/ParaParty/gugu-utils，MIT License，Copyright (c) 2020 派瑞派对）。
 * 1.7.10 移植：原模组的方块实体继承自己的 CommonMMTile，经 mixin 注入旧版 Modular Machinery 的 tryColorize 才能在机器成型时变色；
 * MMCE 已不走那段代码，原模组的仓在 MMCE 中成型后不会变成机器颜色。这里继承 MMCE 的 TileColorableMachineComponent，
 * 由 MMCE 成型时统一上色（TileMultiblockMachineController#distributeCasingColor），颜色随方块实体存档与同步。
 */
package com.warmthdawn.mod.gugu_utils.modularmachenary.environment;

import com.warmthdawn.mod.gugu_utils.modularmachenary.MMCompoments;
import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.tiles.base.MachineComponentTile;
import hellfirepvp.modularmachinery.common.tiles.base.TileColorableMachineComponent;

import javax.annotation.Nullable;

/** 环境传感器：作为输入组件，把自身（所在世界与位置）提供给环境需求检查。 */
public class TileEnvironmentHatch extends TileColorableMachineComponent implements MachineComponentTile {

    @Nullable
    @Override
    public MachineComponent<TileEnvironmentHatch> provideComponent() {
        return new MachineComponent<TileEnvironmentHatch>(IOType.INPUT) {
            @Override
            public ComponentType getComponentType() {
                return MMCompoments.COMPONENT_ENVIRONMENT;
            }

            @Override
            public TileEnvironmentHatch getContainerProvider() {
                return TileEnvironmentHatch.this;
            }
        };
    }
}
