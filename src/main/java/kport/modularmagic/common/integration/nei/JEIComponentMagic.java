package kport.modularmagic.common.integration.nei;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.integration.recipe.RecipeLayoutPart;
import net.minecraft.item.ItemStack;

import java.awt.Point;
import java.util.Collections;
import java.util.List;

/**
 * NEI 配方页中的源质 / 魔力 / 生命精华：以一个代表物品显示在物品格中，悬停显示种类与数量。
 * 1.7.10 移植：上游为每种需求实现了 JEI 的自定义原料与渲染器（JEIComponentAspect 等）；1.7.10 的 NEI 配方页（MachineRecipeHandler）
 * 只显示物品与流体，这里用代表物品（源质用对应源质的晶体，魔力 / 生命精华用对应的仓）加悬停说明代替。
 */
public class JEIComponentMagic extends ComponentRequirement.JEIComponent<ItemStack> {

    private final ItemStack    display;
    private final List<String> lines;

    public JEIComponentMagic(ItemStack display, List<String> lines) {
        this.display = display;
        this.lines = lines;
    }

    @Override
    public Class<ItemStack> getJEIRequirementClass() {
        return ItemStack.class;
    }

    @Override
    public List<ItemStack> getJEIIORequirements() {
        return display == null ? Collections.emptyList() : Collections.singletonList(display);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public RecipeLayoutPart<ItemStack> getLayoutPart(Point offset) {
        return new RecipeLayoutPart.Item(offset);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onJEIHoverTooltip(int slotIndex, boolean input, ItemStack ingredient, List<String> tooltip) {
        tooltip.addAll(lines);
    }
}
