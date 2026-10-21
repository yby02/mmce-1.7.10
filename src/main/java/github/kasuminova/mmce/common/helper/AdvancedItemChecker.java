package github.kasuminova.mmce.common.helper;

import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.item.ItemStack;

@FunctionalInterface
public interface AdvancedItemChecker {
    boolean isMatch(TileMultiblockMachineController controller, ItemStack stack);
}
