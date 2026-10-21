package hellfirepvp.modularmachinery.common.block.prop;

import github.kasuminova.mmce.compat.IStringSerializable;

import javax.annotation.Nonnull;

public enum WorkingState implements IStringSerializable {
    IDLE,
    WORKING,
    ;

    @Nonnull
    @Override
    public String getName() {
        return name().toLowerCase();
    }
}
