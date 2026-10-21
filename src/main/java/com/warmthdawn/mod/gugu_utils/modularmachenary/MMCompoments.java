/*
 * 移植自 gugu-utils（https://github.com/ParaParty/gugu-utils，MIT License，Copyright (c) 2020 派瑞派对），原类名与拼写保留。
 */
package com.warmthdawn.mod.gugu_utils.modularmachenary;

import com.warmthdawn.mod.gugu_utils.common.Constants;
import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.crafting.ComponentType;

import javax.annotation.Nullable;

/** 环境传感器的组件类型 gugu-utils:environment（不依赖其他模组）。 */
public final class MMCompoments {

    public static ComponentType COMPONENT_ENVIRONMENT;

    private MMCompoments() {
    }

    public static void register() {
        COMPONENT_ENVIRONMENT = new ComponentEnvironment();
        COMPONENT_ENVIRONMENT.setRegistryName(Constants.RESOURCE_ENVIRONMENT);
        CommonProxy.registryPrimer.register(COMPONENT_ENVIRONMENT);
    }

    public static final class ComponentEnvironment extends ComponentType {
        @Nullable
        @Override
        public String requiresModid() {
            return null;
        }
    }
}
