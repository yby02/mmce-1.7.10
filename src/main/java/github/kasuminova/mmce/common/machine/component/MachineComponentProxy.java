package github.kasuminova.mmce.common.machine.component;

import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import net.minecraft.tileentity.TileEntity;

public interface MachineComponentProxy<T extends MachineComponent<?>> {

    boolean isSupported(TileEntity te);

    T proxyComponent(TileEntity te);

}
