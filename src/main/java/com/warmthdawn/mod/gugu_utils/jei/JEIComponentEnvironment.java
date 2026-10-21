/*
 * 移植自 gugu-utils（https://github.com/ParaParty/gugu-utils，MIT License，Copyright (c) 2020 派瑞派对）。
 * 1.7.10 移植：NEI 配方页（MachineRecipeHandler）按本组件的版面占一个 18×18 的格子，图标与悬停说明由 EnvironmentIconRenderer 绘制
 * （同原模组的 RendererEnvironment：月相 / 天气 / 海拔 / 生物群系用 recipes_ui.png 中的图标，时间画钟，维度画下界传送门动画）。
 */
package com.warmthdawn.mod.gugu_utils.jei;

import com.warmthdawn.mod.gugu_utils.modularmachenary.environment.envtypes.EnvironmentType;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.integration.recipe.RecipeLayoutPart;

import java.awt.Point;
import java.util.Collections;
import java.util.List;

public class JEIComponentEnvironment extends ComponentRequirement.JEIComponent<EnvironmentType> {

    private final EnvironmentType type;

    public JEIComponentEnvironment(EnvironmentType type) {
        this.type = type;
    }

    public EnvironmentType getType() {
        return type;
    }

    @Override
    public Class<EnvironmentType> getJEIRequirementClass() {
        return EnvironmentType.class;
    }

    @Override
    public List<EnvironmentType> getJEIIORequirements() {
        return Collections.singletonList(type);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public RecipeLayoutPart<EnvironmentType> getLayoutPart(Point offset) {
        return new LayoutPart(offset);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onJEIHoverTooltip(int slotIndex, boolean input, EnvironmentType ingredient, List<String> tooltip) {
        tooltip.addAll(ingredient.getTooltip());
    }

    /** 一个 18×18 的格子（含边框，图标 16×16）；每行最多 3 个，排序值同原模组（50，排在物品前面）。 */
    public static class LayoutPart extends RecipeLayoutPart<EnvironmentType> {

        public LayoutPart(Point offset) {
            super(offset);
        }

        @Override
        public int getComponentWidth() {
            return 18;
        }

        @Override
        public int getComponentHeight() {
            return 18;
        }

        @Override
        public Class<EnvironmentType> getLayoutTypeClass() {
            return EnvironmentType.class;
        }

        @Override
        public int getRendererPaddingX() {
            return 1;
        }

        @Override
        public int getRendererPaddingY() {
            return 1;
        }

        @Override
        public int getMaxHorizontalCount() {
            return 3;
        }

        @Override
        public int getComponentHorizontalGap() {
            return 0;
        }

        @Override
        public int getComponentVerticalGap() {
            return 0;
        }

        @Override
        public int getComponentHorizontalSortingOrder() {
            return 50;
        }

        @Override
        public boolean canBeScaled() {
            return false;
        }
    }
}
