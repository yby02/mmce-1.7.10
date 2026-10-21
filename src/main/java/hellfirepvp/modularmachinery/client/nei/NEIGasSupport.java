package hellfirepvp.modularmachinery.client.nei;

import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.GuiUsageRecipe;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasRegistry;
import mekanism.api.gas.GasStack;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;

/**
 * NEI 配方页中 Mekanism 气体的查询与提示。所有 Mekanism 类型集中在本类中，{@link MachineRecipeHandler} 只在
 * {@code Mods.MEKANISM.isPresent()} 时调用。
 * <p>
 * 查询约定与 Mekanism 1.7.10 自己的 NEI 处理器相同：气体用 {@code openRecipeGui("gas", GasStack)} 查询，
 * 因此在 Mekanism 机器 GUI 的气体槽上按 R / U 也能查到 MMCE 配方。
 */
final class NEIGasSupport {

    static final String GAS_ID = "gas";

    private NEIGasSupport() {
    }

    static boolean isGas(Object value) {
        return value instanceof GasStack stack && stack.getGas() != null;
    }

    static boolean sameGas(Object a, Object b) {
        return a instanceof GasStack x && b instanceof GasStack y && x.getGas() != null && x.getGas() == y.getGas();
    }

    /** Mekanism 把气体同时注册为同名流体；对该流体的 NEI 流体显示物品按 R / U 时也匹配对应气体。 */
    static boolean matchesFluid(Object gas, FluidStack fluid) {
        if (!(gas instanceof GasStack stack) || fluid == null || fluid.getFluid() == null) {
            return false;
        }
        Gas byFluid = GasRegistry.getGas(fluid.getFluid());
        return byFluid != null && byFluid == stack.getGas();
    }

    static String amountText(Object gas) {
        return String.valueOf(((GasStack) gas).amount);
    }

    static void addTooltip(Object gas, List<String> tooltip) {
        GasStack stack = (GasStack) gas;
        tooltip.add(stack.getGas().getLocalizedName());
        tooltip.add(EnumChatFormatting.GRAY + I18n.format("tooltip.fluidhatch.gas") + " " + stack.amount + " mB");
    }

    /** 打开该气体的合成（usage=false）或用途（usage=true）查询。 */
    static boolean open(Object gas, boolean usage) {
        GasStack query = ((GasStack) gas).copy();
        return usage ? GuiUsageRecipe.openRecipeGui(GAS_ID, query) : GuiCraftingRecipe.openRecipeGui(GAS_ID, query);
    }
}
