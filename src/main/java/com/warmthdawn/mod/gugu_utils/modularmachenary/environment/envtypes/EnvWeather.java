/*
 * 移植自 gugu-utils（https://github.com/ParaParty/gugu-utils，MIT License，Copyright (c) 2020 派瑞派对）。
 * 1.7.10 移植：World#canSnowAt(pos, false) 在 1.7.10 为 World#func_147478_e(x, y, z, false)。
 */
package com.warmthdawn.mod.gugu_utils.modularmachenary.environment.envtypes;

import github.kasuminova.mmce.compat.BlockPos;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldInfo;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** 天气：晴（不下雨也不打雷）、雨、雪（下雨且该处会下雪）、雷暴。 */
public class EnvWeather implements EnvironmentType {

    public enum Weathers {
        SUNNY,
        RAINING,
        SNOWING,
        THUNDERING
    }

    private final Weathers weather;

    public EnvWeather(Weathers weather) {
        this.weather = weather;
    }

    public Weathers getWeather() {
        return weather;
    }

    @Override
    public boolean isMeet(World world, BlockPos pos) {
        WorldInfo info = world.getWorldInfo();
        return switch (weather) {
            case SUNNY -> !info.isRaining() && !info.isThundering();
            case RAINING -> info.isRaining();
            case SNOWING -> (info.isRaining() || info.isThundering()) && world.func_147478_e(pos.getX(), pos.getY(), pos.getZ(), false);
            case THUNDERING -> info.isThundering();
        };
    }

    @Override
    public List<String> getTooltip() {
        return Collections.singletonList(StatCollector.translateToLocal("tooltips.gugu-utils.environment.weather." + weather.name().toLowerCase(Locale.ROOT)));
    }

    @Override
    public String getName() {
        return "weather";
    }
}
