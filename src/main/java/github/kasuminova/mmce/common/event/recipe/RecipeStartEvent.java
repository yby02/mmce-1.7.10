package github.kasuminova.mmce.common.event.recipe;

import github.kasuminova.mmce.compat.crt.ZenRegister;
import hellfirepvp.modularmachinery.common.machine.RecipeThread;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import stanhebben.zenscript.annotations.ZenClass;

@ZenRegister
@ZenClass("mods.modularmachinery.RecipeStartEvent")
public class RecipeStartEvent extends RecipeEvent {
    public RecipeStartEvent(TileMultiblockMachineController controller, RecipeThread thread) {
        super(controller, thread, thread.getContext());
    }

}
