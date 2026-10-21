package hellfirepvp.modularmachinery.common.crafting.requirement.type;

import com.google.gson.JsonObject;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementInterfaceNumInput;
import hellfirepvp.modularmachinery.common.machine.IOType;

public class RequirementTypeInterfaceNumInput extends RequirementType<Float, RequirementInterfaceNumInput> {
    @Override
    public RequirementInterfaceNumInput createRequirement(IOType type, JsonObject jsonObject) {
        return null;
    }
}
