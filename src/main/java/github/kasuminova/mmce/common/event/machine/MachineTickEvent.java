package github.kasuminova.mmce.common.event.machine;

import github.kasuminova.mmce.compat.crt.ZenRegister;
import github.kasuminova.mmce.common.event.Phase;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import stanhebben.zenscript.annotations.ZenClass;

@ZenRegister
@ZenClass("mods.modularmachinery.MachineTickEvent")
public class MachineTickEvent extends MachineEvent {
    public final Phase phase;

    public MachineTickEvent(TileMultiblockMachineController controller, Phase phase) {
        super(controller);
        this.phase = phase;
    }
}
