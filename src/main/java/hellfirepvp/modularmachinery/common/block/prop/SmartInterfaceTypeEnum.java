package hellfirepvp.modularmachinery.common.block.prop;

import github.kasuminova.mmce.compat.IStringSerializable;

public enum SmartInterfaceTypeEnum implements IStringSerializable {
    NUMBER;

    @Override
    public String getName() {
        return name().toLowerCase();
    }
}
