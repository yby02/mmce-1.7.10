package kport.modularmagic.common.tile.machinecomponent;

import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import kport.modularmagic.common.crafting.component.ModularMagicComponents;
import kport.modularmagic.common.crafting.helper.ManaProviderCopy;

public class MachineComponentManaProvider extends MachineComponent<ManaProviderCopy> {

    private final ManaProviderCopy copy;

    public MachineComponentManaProvider(IOType io, ManaProviderCopy copy) {
        super(io);
        this.copy = copy;
    }

    @Override
    public ComponentType getComponentType() {
        return ModularMagicComponents.COMPONENT_MANA;
    }

    @Override
    public ManaProviderCopy getContainerProvider() {
        return copy;
    }

    @Override
    public long getGroupID() {
        return ioType == IOType.INPUT ? copy.original().getGroupId() : -1;
    }
}
