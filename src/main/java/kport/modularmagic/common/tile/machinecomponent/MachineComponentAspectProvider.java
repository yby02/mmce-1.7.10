package kport.modularmagic.common.tile.machinecomponent;

import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import kport.modularmagic.common.crafting.component.ModularMagicComponents;
import kport.modularmagic.common.crafting.helper.AspectProviderCopy;

public class MachineComponentAspectProvider extends MachineComponent<AspectProviderCopy> {

    private final AspectProviderCopy copy;

    public MachineComponentAspectProvider(IOType io, AspectProviderCopy copy) {
        super(io);
        this.copy = copy;
    }

    @Override
    public ComponentType getComponentType() {
        return ModularMagicComponents.COMPONENT_ASPECT;
    }

    @Override
    public AspectProviderCopy getContainerProvider() {
        return copy;
    }

    @Override
    public long getGroupID() {
        return ioType == IOType.INPUT ? copy.original().getGroupId() : -1;
    }
}
