package github.kasuminova.mmce.common.event.machine;

import github.kasuminova.mmce.compat.crt.ZenRegister;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import stanhebben.zenscript.annotations.ZenClass;

@ZenRegister
@ZenClass("mods.modularmachinery.MachineStructureUpdateEvent")
public class MachineStructureUpdateEvent extends MachineEvent {
    public MachineStructureUpdateEvent(TileMultiblockMachineController controller) {
        super(controller);
    }
}
