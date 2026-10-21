/*
 * 移植自 gugu-utils（https://github.com/ParaParty/gugu-utils，MIT License，Copyright (c) 2020 派瑞派对）。
 */
package com.warmthdawn.mod.gugu_utils.modularmachenary.environment.envtypes;

import github.kasuminova.mmce.compat.BlockPos;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/** 月相：夜间且当前月相（0 满月 … 7 盈凸月）在列表中。白天不满足（同原模组）。 */
public class EnvMoonPhase implements EnvironmentType {

    private final int[] phases;

    public EnvMoonPhase(int[] phases) {
        this.phases = phases;
    }

    public int[] getPhases() {
        return phases;
    }

    @Override
    public boolean isMeet(World world, BlockPos pos) {
        if (world.isDaytime()) {
            return false;
        }
        int currentPhase = world.provider.getMoonPhase(world.getWorldTime());
        for (int phase : phases) {
            if (currentPhase == phase) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> getTooltip() {
        List<String> result = new ArrayList<>();
        result.add(StatCollector.translateToLocal("tooltips.gugu-utils.environment.moonphase.desc"));
        for (int phase : phases) {
            result.add(StatCollector.translateToLocal("tooltips.gugu-utils.environment.moonphase." + phase));
        }
        return result;
    }

    @Override
    public String getName() {
        return "moonphase";
    }
}
