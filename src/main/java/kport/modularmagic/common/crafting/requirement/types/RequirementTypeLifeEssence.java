package kport.modularmagic.common.crafting.requirement.types;

import com.google.gson.JsonObject;
import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementType;
import hellfirepvp.modularmachinery.common.machine.IOType;
import kport.modularmagic.common.crafting.requirement.RequirementLifeEssence;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;

public class RequirementTypeLifeEssence extends RequirementType<ItemStack, RequirementLifeEssence> {

    @Override
    public RequirementLifeEssence createRequirement(IOType type, JsonObject json) {
        int amount = ModularMagicRequirements.requiredInt(json, "amount", "modularmachinery:lifeessence");
        boolean perTick = ModularMagicRequirements.optionalBoolean(json, "perTick", false);
        return new RequirementLifeEssence(type, amount, perTick);
    }

    @Nullable
    @Override
    public String requiresModid() {
        return Mods.BLOODMAGIC.modid;
    }
}
