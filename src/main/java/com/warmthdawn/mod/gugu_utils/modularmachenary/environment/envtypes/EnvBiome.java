/*
 * 移植自 gugu-utils（https://github.com/ParaParty/gugu-utils，MIT License，Copyright (c) 2020 派瑞派对），原类名 EnvBoime。
 * 1.7.10 移植：
 * - 生物群系没有注册名，按数字 ID、1.12.2 原版注册名（如 "minecraft:ice_flats"，原版生物群系 ID 两版相同，按 ID 对应）
 *   或名称（BiomeGenBase#biomeName，不区分大小写，忽略空格、下划线和命名空间，如 "Ice Plains" / "ice_plains"）查找。
 * - biomeType 只接受 Forge 1.7.10 的 BiomeManager.BiomeType（desert / warm / cool / icy）；原模组遇到其他名称时会经 EnumHelper 新建一个空类型，
 *   这里不新建枚举，而是记录警告并得到空列表（同样永远不满足）。
 */
package com.warmthdawn.mod.gugu_utils.modularmachenary.environment.envtypes;

import github.kasuminova.mmce.compat.BlockPos;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 生物群系：传感器所在处的生物群系在列表中。 */
public class EnvBiome implements EnvironmentType {

    private final BiomeGenBase[] biomes;
    private final String         biomeType;

    /** 1.12.2 原版生物群系注册名 -> ID（原模组的 JSON 用注册名；1.7.10 原版生物群系的 ID 与 1.12.2 相同）。 */
    private static final Map<String, Integer> LEGACY_IDS = new HashMap<>();

    static {
        String[] names = {
            "ocean", "plains", "desert", "extreme_hills", "forest", "taiga", "swampland", "river", "hell", "sky",
            "frozen_ocean", "frozen_river", "ice_flats", "ice_mountains", "mushroom_island", "mushroom_island_shore", "beaches",
            "desert_hills", "forest_hills", "taiga_hills", "smaller_extreme_hills", "jungle", "jungle_hills", "jungle_edge",
            "deep_ocean", "stone_beach", "cold_beach", "birch_forest", "birch_forest_hills", "roofed_forest", "taiga_cold",
            "taiga_cold_hills", "redwood_taiga", "redwood_taiga_hills", "extreme_hills_with_trees", "savanna", "savanna_rock",
            "mesa", "mesa_rock", "mesa_clear_rock"
        };
        for (int id = 0; id < names.length; id++) {
            LEGACY_IDS.put(names[id], id);
        }
        // 变种（ID + 128）
        String[] mutated = {
            "plains", "desert", "extreme_hills", "forest", "taiga", "swampland", "ice_flats", "jungle", "jungle_edge",
            "birch_forest", "birch_forest_hills", "roofed_forest", "taiga_cold", "redwood_taiga", "redwood_taiga_hills",
            "extreme_hills_with_trees", "savanna", "savanna_rock", "mesa", "mesa_rock", "mesa_clear_rock"
        };
        for (String name : mutated) {
            LEGACY_IDS.put("mutated_" + name, LEGACY_IDS.get(name) + 128);
        }
    }

    public EnvBiome(BiomeGenBase[] biomes) {
        this.biomes = biomes;
        this.biomeType = null;
    }

    public EnvBiome(BiomeManager.BiomeType type) {
        this.biomeType = type.name();
        List<BiomeGenBase> list = new ArrayList<>();
        // 1.12.2 的 BiomeManager 默认把沙漠、热带草原、平原列为 DESERT；Forge 1.7.10 的 DESERT 列表默认是空的，
        // 这三个由世界生成（GenLayerBiome，默认世界类型）自行加入。这里补上，使 biomeType "desert" 与原模组一致
        if (type == BiomeManager.BiomeType.DESERT) {
            list.add(BiomeGenBase.desert);
            list.add(BiomeGenBase.savanna);
            list.add(BiomeGenBase.plains);
        }
        List<BiomeManager.BiomeEntry> entries = BiomeManager.getBiomes(type);
        if (entries != null) {
            for (BiomeManager.BiomeEntry entry : entries) {
                if (entry.biome != null && !list.contains(entry.biome)) {
                    list.add(entry.biome);
                }
            }
        }
        this.biomes = list.toArray(new BiomeGenBase[0]);
    }

    public BiomeGenBase[] getBiomes() {
        return biomes;
    }

    /** Forge 1.7.10 的生物群系类型（desert / warm / cool / icy，不区分大小写）；不是这几个时返回 null。 */
    public static BiomeManager.BiomeType biomeType(String name) {
        for (BiomeManager.BiomeType type : BiomeManager.BiomeType.values()) {
            if (type.name().equalsIgnoreCase(name.trim())) {
                return type;
            }
        }
        return null;
    }

    private static String normalize(String name) {
        String s = name.toLowerCase(Locale.ROOT);
        int colon = s.indexOf(':');
        if (colon >= 0) {
            s = s.substring(colon + 1);
        }
        return s.replace(" ", "").replace("_", "");
    }

    /** 按数字 ID 或名称查找生物群系；找不到时返回 null。 */
    public static BiomeGenBase findBiome(String nameOrId) {
        String key = nameOrId.trim();
        BiomeGenBase[] all = BiomeGenBase.getBiomeGenArray();
        try {
            int id = Integer.parseInt(key);
            return id >= 0 && id < all.length ? all[id] : null;
        } catch (NumberFormatException ignored) {
        }
        String lower = key.toLowerCase(Locale.ROOT);
        Integer legacy = LEGACY_IDS.get(lower.startsWith("minecraft:") ? lower.substring("minecraft:".length()) : lower);
        if (legacy != null && legacy < all.length && all[legacy] != null) {
            return all[legacy];
        }
        String wanted = normalize(key);
        for (BiomeGenBase biome : all) {
            if (biome != null && biome.biomeName != null && normalize(biome.biomeName).equals(wanted)) {
                return biome;
            }
        }
        return null;
    }

    @Override
    public boolean isMeet(World world, BlockPos pos) {
        BiomeGenBase current = world.getBiomeGenForCoords(pos.getX(), pos.getZ());
        for (BiomeGenBase biome : biomes) {
            if (biome == current) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> getTooltip() {
        List<String> result = new ArrayList<>();
        result.add(StatCollector.translateToLocal("tooltips.gugu-utils.environment.biome"));
        if (biomeType != null) {
            result.add(StatCollector.translateToLocalFormatted("tooltips.gugu-utils.environment.biome.type", biomeType));
        }
        for (BiomeGenBase biome : biomes) {
            result.add(biome.biomeName);
        }
        return result;
    }

    @Override
    public String getName() {
        return "biomes";
    }
}
