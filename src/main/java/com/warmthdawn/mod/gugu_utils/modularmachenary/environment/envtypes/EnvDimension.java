/*
 * 移植自 gugu-utils（https://github.com/ParaParty/gugu-utils，MIT License，Copyright (c) 2020 派瑞派对）。
 * 1.7.10 移植：维度名由 1.12 的 DimensionType 改为 DimensionManager 中登记的 WorldProvider 的名称。
 */
package com.warmthdawn.mod.gugu_utils.modularmachenary.environment.envtypes;

import github.kasuminova.mmce.compat.BlockPos;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import java.util.ArrayList;
import java.util.List;

/** 维度：传感器所在维度的 ID 在列表中。 */
public class EnvDimension implements EnvironmentType {

    private final int[] dimensions;

    public EnvDimension(int[] dimensions) {
        this.dimensions = dimensions;
    }

    public int[] getDimensions() {
        return dimensions;
    }

    @Override
    public boolean isMeet(World world, BlockPos pos) {
        int currentDim = world.provider.dimensionId;
        for (int dim : dimensions) {
            if (dim == currentDim) {
                return true;
            }
        }
        return false;
    }

    /** 维度显示名；未登记或无法创建 WorldProvider 时显示 “DIM id”。 */
    public static String dimensionName(int dim) {
        try {
            if (DimensionManager.isDimensionRegistered(dim)) {
                return DimensionManager.createProviderFor(dim).getDimensionName() + " (" + dim + ")";
            }
        } catch (RuntimeException ignored) {
        }
        return "DIM " + dim;
    }

    @Override
    public List<String> getTooltip() {
        List<String> result = new ArrayList<>();
        result.add(StatCollector.translateToLocal("tooltips.gugu-utils.environment.dimension"));
        for (int dim : dimensions) {
            result.add(dimensionName(dim));
        }
        return result;
    }

    @Override
    public String getName() {
        return "dimensions";
    }
}
