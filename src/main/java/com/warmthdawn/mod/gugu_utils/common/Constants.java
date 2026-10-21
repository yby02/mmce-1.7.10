/*
 * 移植自 gugu-utils（https://github.com/ParaParty/gugu-utils，MIT License，Copyright (c) 2020 派瑞派对）。
 */
package com.warmthdawn.mod.gugu_utils.common;

import com.warmthdawn.mod.gugu_utils.GuGuUtils;
import github.kasuminova.mmce.compat.ResourceLocation;

/** 注册名、语言键前缀（同原模组）。 */
public final class Constants {

    public static final String           NAME_ENVIRONMENTHATCH          = "environmenthatch";
    public static final String           NAME_RANGED_CONSTRUCTION_TOOL  = "constructionranged";
    /** 组件类型与需求类型的注册名（JSON 中的 "type": "gugu-utils:environment"）。 */
    public static final ResourceLocation RESOURCE_ENVIRONMENT           = new ResourceLocation(GuGuUtils.MODID, "environment");
    public static final String           STRING_RESOURCE_ENVIRONMENT    = GuGuUtils.MODID + ":environment";
    /** 方块实体 ID（同原模组：gugu-utils:environmenthatch）。 */
    public static final String           TILE_ENVIRONMENTHATCH          = GuGuUtils.MODID + ":" + NAME_ENVIRONMENTHATCH;

    private Constants() {
    }
}
