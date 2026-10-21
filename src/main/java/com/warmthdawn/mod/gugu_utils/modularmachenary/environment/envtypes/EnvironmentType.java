/*
 * 移植自 gugu-utils（https://github.com/ParaParty/gugu-utils，MIT License，Copyright (c) 2020 派瑞派对），
 * 1.7.10 移植的修改见各类注释。许可证全文见 jar 中的 LICENSE-gugu-utils.txt。
 */
package com.warmthdawn.mod.gugu_utils.modularmachenary.environment.envtypes;

import github.kasuminova.mmce.compat.BlockPos;
import net.minecraft.world.World;

import java.util.List;

/** 环境传感器的一种检测条件（天气、月相、维度、生物群系、海拔、时间）。 */
public interface EnvironmentType {

    /** 传感器所在位置 pos 是否满足条件。 */
    boolean isMeet(World world, BlockPos pos);

    /** NEI 配方页的悬停说明（客户端调用）。 */
    List<String> getTooltip();

    /** 条件名，用于错误信息键 {@code craftcheck.failure.gugu-utils:environment.<name>}。 */
    String getName();
}
