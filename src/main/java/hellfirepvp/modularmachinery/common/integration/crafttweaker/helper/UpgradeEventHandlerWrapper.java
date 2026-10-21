package hellfirepvp.modularmachinery.common.integration.crafttweaker.helper;

import com.github.bsideup.jabel.Desugar;
import github.kasuminova.mmce.compat.crt.ZenRegister;
import github.kasuminova.mmce.common.event.machine.MachineEvent;
import github.kasuminova.mmce.common.upgrade.MachineUpgrade;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;

@Desugar
@ZenRegister
@ZenClass("mods.modularmachinery.UpgradeEventHandlerWrapper")
public record UpgradeEventHandlerWrapper(@ZenGetter("event") MachineEvent event,
                                         @ZenGetter("upgrade") MachineUpgrade upgrade) {
}
