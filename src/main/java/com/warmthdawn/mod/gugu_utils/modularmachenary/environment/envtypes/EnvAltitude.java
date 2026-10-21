/*
 * 移植自 gugu-utils（https://github.com/ParaParty/gugu-utils，MIT License，Copyright (c) 2020 派瑞派对）。
 */
package com.warmthdawn.mod.gugu_utils.modularmachenary.environment.envtypes;

import github.kasuminova.mmce.compat.BlockPos;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.List;

/** 海拔：传感器的 Y 坐标在 [begin, end] 内。 */
public class EnvAltitude implements EnvironmentType {

    private final int begin;
    private final int end;

    public EnvAltitude(int begin, int end) {
        this.begin = begin;
        this.end = end;
    }

    public int getBegin() {
        return begin;
    }

    public int getEnd() {
        return end;
    }

    @Override
    public boolean isMeet(World world, BlockPos pos) {
        return pos.getY() >= begin && pos.getY() <= end;
    }

    @Override
    public List<String> getTooltip() {
        return Collections.singletonList(StatCollector.translateToLocalFormatted("tooltips.gugu-utils.environment.altitude", begin, end));
    }

    @Override
    public String getName() {
        return "altitude";
    }
}
