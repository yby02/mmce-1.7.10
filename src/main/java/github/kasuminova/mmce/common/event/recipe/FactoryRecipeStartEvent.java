package github.kasuminova.mmce.common.event.recipe;

import github.kasuminova.mmce.compat.crt.ZenRegister;
import hellfirepvp.modularmachinery.common.machine.factory.FactoryRecipeThread;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import stanhebben.zenscript.annotations.ZenClass;

@ZenRegister
@ZenClass("mods.modularmachinery.FactoryRecipeStartEvent")
public class FactoryRecipeStartEvent extends FactoryRecipeEvent {
    public FactoryRecipeStartEvent(FactoryRecipeThread recipeThread, TileMultiblockMachineController controller) {
        super(recipeThread, controller);
    }
}
