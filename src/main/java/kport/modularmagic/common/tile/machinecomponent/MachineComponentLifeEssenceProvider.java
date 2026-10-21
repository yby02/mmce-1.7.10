package kport.modularmagic.common.tile.machinecomponent;

import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import kport.modularmagic.common.crafting.component.ModularMagicComponents;
import kport.modularmagic.common.crafting.helper.LifeEssenceProviderCopy;

public class MachineComponentLifeEssenceProvider extends MachineComponent<LifeEssenceProviderCopy> {

    private final LifeEssenceProviderCopy copy;

    public MachineComponentLifeEssenceProvider(IOType io, LifeEssenceProviderCopy copy) {
        super(io);
        this.copy = copy;
    }

    @Override
    public ComponentType getComponentType() {
        return ModularMagicComponents.COMPONENT_LIFE_ESSENCE;
    }

    @Override
    public LifeEssenceProviderCopy getContainerProvider() {
        return copy;
    }

    @Override
    public long getGroupID() {
        return ioType == IOType.INPUT ? copy.original().getGroupId() : -1;
    }
}
