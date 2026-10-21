package hellfirepvp.modularmachinery.client.nei;

import codechicken.nei.api.INEIGuiAdapter;
import codechicken.nei.recipe.StackInfo;
import github.kasuminova.mmce.client.gui.ae2.GuiMEBase;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/**
 * NEI 拖放到 ME 部件界面：物品拖到 ME 物品输入总线的设定格，流体容器 / 流体显示物品拖到 ME 流体输入总线的流体设定格
 * （对应上游 GuiMEItemInputBus 的 JEI 拖放）。只在装有 AE2 时注册。
 */
public class MEGuiNeiDragHandler extends INEIGuiAdapter {

    @Override
    public boolean handleDragNDrop(GuiContainer gui, int mouseX, int mouseY, ItemStack draggedStack, int button) {
        if (!(gui instanceof GuiMEBase<?> me) || draggedStack == null) {
            return false;
        }
        FluidStack fluid = StackInfo.getFluid(draggedStack);
        return me.acceptNeiDrop(mouseX, mouseY, draggedStack, fluid);
    }
}
