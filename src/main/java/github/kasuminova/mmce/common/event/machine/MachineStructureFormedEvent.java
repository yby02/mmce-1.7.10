package github.kasuminova.mmce.common.event.machine;

import github.kasuminova.mmce.compat.crt.ZenRegister;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import stanhebben.zenscript.annotations.ZenClass;

@ZenRegister
@ZenClass("mods.modularmachinery.MachineStructureFormedEvent")
public class MachineStructureFormedEvent extends MachineEvent {
    public MachineStructureFormedEvent(TileMultiblockMachineController controller) {
        super(controller);
    }
}
