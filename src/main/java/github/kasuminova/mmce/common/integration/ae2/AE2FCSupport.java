package github.kasuminova.mmce.common.integration.ae2;

import com.glodblock.github.common.item.ItemFluidDrop;
import com.glodblock.github.common.item.ItemFluidPacket;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;

/** AE2FC（AE2 Fluid Crafting Rework）：流体样板中代表流体的物品换成流体。只在装有 AE2FC 时调用。 */
public final class AE2FCSupport {

    private AE2FCSupport() {
    }

    @Nullable
    public static FluidStack toFluid(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof ItemFluidDrop) {
            return ItemFluidDrop.getFluidStack(stack);
        }
        if (item instanceof ItemFluidPacket) {
            return ItemFluidPacket.getFluidStack(stack);
        }
        return null;
    }
}
