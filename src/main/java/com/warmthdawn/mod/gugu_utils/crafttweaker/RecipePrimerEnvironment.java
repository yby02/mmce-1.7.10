/*
 * 移植自 gugu-utils 的 RecipePrimerExt（环境部分，https://github.com/ParaParty/gugu-utils，MIT License，Copyright (c) 2020 派瑞派对）。
 * 方法名与参数同原模组：setBiome / setAltitude / setTime / setDimension / setMoonPhase / setWeather。
 * 1.7.10 移植：MineTweaker3 1.7.10 没有 <biome:...> 尖括号写法，脚本中无法写出 IBiome；setBiome 改为接受字符串数组
 * （生物群系名称、1.12.2 原版注册名或数字 ID，同 JSON 的 "biomes"），如 setBiome(["minecraft:desert", "Ice Plains", "12"])。
 * 不再同时提供 setBiome(IBiome[])：两个重载并存时 ZenScript 无法为数组字面量选择重载，脚本会在该行报错。
 */
package com.warmthdawn.mod.gugu_utils.crafttweaker;

import com.warmthdawn.mod.gugu_utils.modularmachenary.environment.envtypes.*;
import com.warmthdawn.mod.gugu_utils.modularmachenary.requirements.RequirementEnvironment;
import com.warmthdawn.mod.gugu_utils.modularmachenary.requirements.types.RequirementTypeEnvironment;
import github.kasuminova.mmce.compat.crt.CraftTweakerAPI;
import github.kasuminova.mmce.compat.crt.ZenRegister;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipePrimer;
import hellfirepvp.modularmachinery.common.machine.IOType;
import net.minecraft.world.biome.BiomeGenBase;
import stanhebben.zenscript.annotations.ZenExpansion;
import stanhebben.zenscript.annotations.ZenMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@ZenRegister
@ZenExpansion("mods.modularmachinery.RecipePrimer")
public final class RecipePrimerEnvironment {

    private RecipePrimerEnvironment() {
    }

    private static RecipePrimer setEnvironment(RecipePrimer primer, EnvironmentType type) {
        primer.appendComponent(new RequirementEnvironment(RequirementTypeEnvironment.INSTANCE, type, IOType.INPUT));
        return primer;
    }

    private static RecipePrimer biomes(RecipePrimer primer, List<String> names) {
        List<BiomeGenBase> list = new ArrayList<>();
        for (String name : names) {
            BiomeGenBase biome = name == null ? null : EnvBiome.findBiome(name);
            if (biome == null) {
                CraftTweakerAPI.logError("[gugu-utils] Unknown biome: " + name + " (use the 1.7.10 biome name or its numeric id)");
            } else {
                list.add(biome);
            }
        }
        return setEnvironment(primer, new EnvBiome(list.toArray(new BiomeGenBase[0])));
    }

    @ZenMethod
    public static RecipePrimer setBiome(RecipePrimer primer, String[] biomes) {
        List<String> names = new ArrayList<>();
        for (String biome : biomes) {
            names.add(biome);
        }
        return biomes(primer, names);
    }

    @ZenMethod
    public static RecipePrimer setAltitude(RecipePrimer primer, int min, int max) {
        return setEnvironment(primer, new EnvAltitude(min, max));
    }

    @ZenMethod
    public static RecipePrimer setTime(RecipePrimer primer, int min, int max) {
        return setEnvironment(primer, new EnvTime(min, max));
    }

    @ZenMethod
    public static RecipePrimer setDimension(RecipePrimer primer, int[] dimensions) {
        return setEnvironment(primer, new EnvDimension(dimensions));
    }

    @ZenMethod
    public static RecipePrimer setMoonPhase(RecipePrimer primer, int[] moonphases) {
        for (int moonphase : moonphases) {
            if (moonphase < 0 || moonphase > 7) {
                CraftTweakerAPI.logError("[gugu-utils] Moon Phase can not be: " + moonphase);
            }
        }
        return setEnvironment(primer, new EnvMoonPhase(moonphases));
    }

    @ZenMethod
    public static RecipePrimer setWeather(RecipePrimer primer, String weather) {
        EnvWeather.Weathers weatherF;
        try {
            weatherF = EnvWeather.Weathers.valueOf(weather.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            CraftTweakerAPI.logError("[gugu-utils] Weather can not be: " + weather);
            return primer;
        }
        return setEnvironment(primer, new EnvWeather(weatherF));
    }
}
