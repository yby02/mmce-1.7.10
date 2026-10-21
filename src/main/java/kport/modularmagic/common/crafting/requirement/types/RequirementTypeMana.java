package kport.modularmagic.common.crafting.requirement.types;

import com.google.gson.JsonObject;
import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementType;
import hellfirepvp.modularmachinery.common.machine.IOType;
import kport.modularmagic.common.crafting.requirement.RequirementMana;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;

public class RequirementTypeMana extends RequirementType<ItemStack, RequirementMana> {

    @Override
    public RequirementMana createRequirement(IOType type, JsonObject json) {
        int amount = ModularMagicRequirements.requiredInt(json, "amount", "modularmachinery:mana");
        boolean perTick = ModularMagicRequirements.optionalBoolean(json, "perTick", false);
        return new RequirementMana(type, amount, perTick);
    }

    @Nullable
    @Override
    public String requiresModid() {
        return Mods.BOTANIA.modid;
    }
}
