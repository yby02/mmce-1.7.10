/*
 * 移植自 gugu-utils（https://github.com/ParaParty/gugu-utils，MIT License，Copyright (c) 2020 派瑞派对）。
 */
package com.warmthdawn.mod.gugu_utils.modularmachenary.environment.envtypes;

import github.kasuminova.mmce.compat.BlockPos;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.List;

/** 时间：一天内的 tick（0–23999）在 [begin, end] 内；begin 大于 end 时表示跨过午夜（从 begin 到次日 end）。 */
public class EnvTime implements EnvironmentType {

    private final int begin;
    private final int end;

    public EnvTime(int begin, int end) {
        this.begin = begin;
        this.end = end;
    }

    /** tick → “时:分”（0 tick 为 6:00，同原版时钟）。 */
    public static String translateTime(int time) {
        int hour = (time / 1000 + 6) % 24;
        int minute = (int) (((double) (time % 1000)) * 60 / 1000);
        return String.format("%d:%02d", hour, minute);
    }

    @Override
    public boolean isMeet(World world, BlockPos pos) {
        int time = (int) (world.getWorldTime() % 24000);
        if (begin <= end) {
            return time >= begin && time <= end;
        }
        return time >= begin || time <= end;
    }

    @Override
    public List<String> getTooltip() {
        return Collections.singletonList(StatCollector.translateToLocalFormatted("tooltips.gugu-utils.environment.time", translateTime(begin), translateTime(end)));
    }

    @Override
    public String getName() {
        return "time";
    }
}
