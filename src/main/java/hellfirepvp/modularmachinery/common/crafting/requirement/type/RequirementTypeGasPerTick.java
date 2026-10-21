package hellfirepvp.modularmachinery.common.crafting.requirement.type;

import com.google.gson.JsonObject;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementGasPerTick;
import hellfirepvp.modularmachinery.common.machine.IOType;
import mekanism.api.gas.GasStack;

import javax.annotation.Nullable;

/**
 * 与上游相同：gas_pertick 只能由 ZenScript 创建，JSON 中不支持（createRequirement 返回 null）。
 */
public class RequirementTypeGasPerTick extends RequirementType<GasStack, RequirementGasPerTick> {

    @Nullable
    @Override
    public String requiresModid() {
        // 1.7.10 移植：Mekanism 1.7.10 的 modid 是 "Mekanism"（与 RequirementTypeGas 相同）
        return "Mekanism";
    }

    @Override
    public RequirementGasPerTick createRequirement(IOType type, JsonObject jsonObject) {
        return null;
    }
}
