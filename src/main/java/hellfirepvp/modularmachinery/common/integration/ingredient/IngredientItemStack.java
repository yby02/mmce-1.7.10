package hellfirepvp.modularmachinery.common.integration.ingredient;

import com.github.bsideup.jabel.Desugar;
import net.minecraft.item.ItemStack;

@Desugar
public record IngredientItemStack(ItemStack stack, int min, int max, float chance) {

    public IngredientItemStack copy() {
        return new IngredientItemStack(stack.copy(), min, max, chance);
    }

}
