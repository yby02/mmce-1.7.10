package hellfirepvp.modularmachinery.client.dev;

import codechicken.nei.ItemList;
import codechicken.nei.ItemPanel;
import codechicken.nei.LayoutManager;
import codechicken.nei.NEIClientConfig;

/**
 * 与 NEI 直接交互的开发期辅助方法，单独成类，NEI 未安装时不会被加载。
 */
final class NeiHooks {

    private NeiHooks() {
    }

    static boolean itemsLoaded() {
        return LayoutManager.itemsLoaded && ItemList.loadFinished && ItemList.items != null && !ItemList.items.isEmpty();
    }

    /** 当前过滤后的物品面板条目数（ItemPanel.items 是遗留静态字段，实际列表在实例的 realItems 中） */
    static int visibleItemCount() {
        ItemPanel panel = LayoutManager.itemPanel;
        return panel == null || panel.realItems == null ? 0 : panel.realItems.size();
    }

    static String diagnostics() {
        return "enabled=" + NEIClientConfig.isEnabled() + " hidden=" + NEIClientConfig.isHidden()
            + " allItems=" + (ItemList.items == null ? -1 : ItemList.items.size())
            + " panelItems=" + visibleItemCount()
            + " search='" + (LayoutManager.searchField == null ? null : LayoutManager.searchField.text()) + "'";
    }

    static void setSearchText(String text) {
        if (LayoutManager.searchField == null) {
            throw new IllegalStateException("NEI search field not initialised");
        }
        LayoutManager.searchField.setText(text);
    }

    static final String STRUCTURE_ID = hellfirepvp.modularmachinery.client.nei.StructurePreviewHandler.ID;

    /** 打开 R（合成）或 U（用途）界面并切换到指定处理器，返回该处理器的配方数；找不到返回 0。 */
    static int openAndSelect(boolean crafting, net.minecraft.item.ItemStack stack, String handlerId) {
        return openAndSelect(crafting, "item", stack, handlerId);
    }

    /** 以 Mekanism 的约定（"gas" + GasStack）查询气体；只在装有 Mekanism 时调用。 */
    static int openGasAndSelect(boolean crafting, String gasName, String handlerId) {
        mekanism.api.gas.Gas gas = mekanism.api.gas.GasRegistry.getGas(gasName);
        if (gas == null) {
            throw new IllegalStateException("gas not registered: " + gasName);
        }
        return openAndSelect(crafting, "gas", new mekanism.api.gas.GasStack(gas, 1000), handlerId);
    }

    /** 当前 MMCE 配方页第 recipe 条配方中各气体格的悬停提示（见 MachineRecipeHandler#describeGasSlots）；不是 MMCE 配方页时返回空列表。 */
    /** 当前 MMCE 配方页第 recipe 条配方的几率标注与各格悬停提示（见 MachineRecipeHandler#describeChances）。 */
    static java.util.List<String> chanceInfo(int recipe) {
        if (net.minecraft.client.Minecraft.getMinecraft().currentScreen instanceof codechicken.nei.recipe.GuiRecipe<?> gui
            && gui.getHandler() instanceof hellfirepvp.modularmachinery.client.nei.MachineRecipeHandler handler) {
            return handler.describeChances(recipe);
        }
        return java.util.Collections.emptyList();
    }

    /** 当前 MMCE 配方页第 recipe 条配方中的环境需求（gugu-utils:environment）："类型: 提示行 | …"（见 MachineRecipeHandler#describeEnvSlots）。 */
    static java.util.List<String> envSlots(int recipe) {
        if (net.minecraft.client.Minecraft.getMinecraft().currentScreen instanceof codechicken.nei.recipe.GuiRecipe<?> gui
            && gui.getHandler() instanceof hellfirepvp.modularmachinery.client.nei.MachineRecipeHandler handler) {
            return handler.describeEnvSlots(recipe);
        }
        return java.util.Collections.emptyList();
    }

    /** 当前 MMCE 配方页第 recipe 条配方第 slot 个环境图标的悬停提示行。 */
    static java.util.List<String> envSlotTooltip(int recipe, int slot) {
        if (net.minecraft.client.Minecraft.getMinecraft().currentScreen instanceof codechicken.nei.recipe.GuiRecipe<?> gui
            && gui.getHandler() instanceof hellfirepvp.modularmachinery.client.nei.MachineRecipeHandler handler) {
            return handler.envSlotTooltip(recipe, slot);
        }
        return java.util.Collections.emptyList();
    }

    static java.util.List<String> gasSlotTooltips(int recipe) {
        if (net.minecraft.client.Minecraft.getMinecraft().currentScreen instanceof codechicken.nei.recipe.GuiRecipe<?> gui
            && gui.getHandler() instanceof hellfirepvp.modularmachinery.client.nei.MachineRecipeHandler handler) {
            return handler.describeGasSlots(recipe);
        }
        return java.util.Collections.emptyList();
    }

    /**
     * 在当前 MMCE 配方页第 recipe 条配方的第 slot 个气体格上按 R（usage = false）或 U（usage = true）：
     * 以该格中心为光标位置调用与按键 / 点击相同的处理函数（不注入系统键鼠），返回是否打开了新的配方页。
     */
    static boolean lookupGasSlot(int recipe, int slot, boolean usage) {
        if (net.minecraft.client.Minecraft.getMinecraft().currentScreen instanceof codechicken.nei.recipe.GuiRecipe<?> gui
            && gui.getHandler() instanceof hellfirepvp.modularmachinery.client.nei.MachineRecipeHandler handler) {
            java.awt.Point center = handler.gasSlotCenter(recipe, slot);
            if (center == null) {
                throw new IllegalStateException("recipe " + recipe + " has no gas slot " + slot);
            }
            return handler.lookupGasAt(recipe, center, usage);
        }
        throw new IllegalStateException("not on an MMCE recipe page: " + net.minecraft.client.Minecraft.getMinecraft().currentScreen);
    }

    /** 当前 NEI 配方页的处理器 ID 与配方数；不是配方页时返回 null。 */
    static String currentPage() {
        if (net.minecraft.client.Minecraft.getMinecraft().currentScreen instanceof codechicken.nei.recipe.GuiRecipe<?> gui) {
            return (gui instanceof codechicken.nei.recipe.GuiUsageRecipe ? "U " : "R ") + gui.getHandler().getHandlerId() + " recipes=" + gui.getHandler().numRecipes();
        }
        return null;
    }

    /** NEI 的流体显示物品（GTNH NEI 的 ItemFluidDisplay）；流体不存在时返回 null。 */
    static net.minecraft.item.ItemStack fluidDisplay(String fluidName) {
        net.minecraftforge.fluids.Fluid fluid = net.minecraftforge.fluids.FluidRegistry.getFluid(fluidName);
        return fluid == null ? null : codechicken.nei.item.ItemFluidDisplay.createStack(new net.minecraftforge.fluids.FluidStack(fluid, 1000));
    }

    private static int openAndSelect(boolean crafting, String id, Object query, String handlerId) {
        boolean opened = crafting
            ? codechicken.nei.recipe.GuiCraftingRecipe.openRecipeGui(id, query)
            : codechicken.nei.recipe.GuiUsageRecipe.openRecipeGui(id, query);
        if (!opened || !(net.minecraft.client.Minecraft.getMinecraft().currentScreen instanceof codechicken.nei.recipe.GuiRecipe<?> gui)) {
            return 0;
        }
        for (int i = 0; i < gui.currenthandlers.size(); i++) {
            codechicken.nei.recipe.IRecipeHandler handler = gui.currenthandlers.get(i);
            if (handlerId.equals(handler.getHandlerId())) {
                // 与点击页签相同：GuiRecipe#setRecipePage（protected）会重建当前处理器与分页
                try {
                    java.lang.reflect.Method setPage = codechicken.nei.recipe.GuiRecipe.class.getDeclaredMethod("setRecipePage", int.class);
                    setPage.setAccessible(true);
                    setPage.invoke(gui, i);
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("cannot switch NEI tab", e);
                }
                return handler.numRecipes();
            }
        }
        return 0;
    }

    static boolean structureContains(String machine) {
        if (net.minecraft.client.Minecraft.getMinecraft().currentScreen instanceof codechicken.nei.recipe.GuiRecipe<?> gui
            && gui.getHandler() instanceof hellfirepvp.modularmachinery.client.nei.StructurePreviewHandler preview) {
            for (int i = 0; i < preview.numRecipes(); i++) {
                if (machine.equals(preview.machineNameOf(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 与点击结构页的“投影”按钮相同。 */
    static boolean projectStructure(int recipe) {
        if (net.minecraft.client.Minecraft.getMinecraft().currentScreen instanceof codechicken.nei.recipe.GuiRecipe<?> gui
            && gui.getHandler() instanceof hellfirepvp.modularmachinery.client.nei.StructurePreviewHandler preview) {
            return preview.projectToWorld(recipe);
        }
        return false;
    }

    static void setStructureView(float yaw, float pitch, float zoom, int layer) {
        if (net.minecraft.client.Minecraft.getMinecraft().currentScreen instanceof codechicken.nei.recipe.GuiRecipe<?> gui
            && gui.getHandler() instanceof hellfirepvp.modularmachinery.client.nei.StructurePreviewHandler preview) {
            preview.setView(0, yaw, pitch, zoom, layer);
        }
    }
}
