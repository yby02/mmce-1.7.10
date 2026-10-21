package kport.modularmagic.common.crafting.requirement.types;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementType;
import hellfirepvp.modularmachinery.common.machine.IOType;
import kport.modularmagic.common.crafting.requirement.RequirementAspect;
import net.minecraft.item.ItemStack;
import thaumcraft.api.aspects.Aspect;

import javax.annotation.Nullable;

public class RequirementTypeAspect extends RequirementType<ItemStack, RequirementAspect> {

    @Override
    public RequirementAspect createRequirement(IOType type, JsonObject json) {
        int amount = ModularMagicRequirements.requiredInt(json, "amount", "modularmachinery:aspect");
        String tag = ModularMagicRequirements.requiredString(json, "aspect", "modularmachinery:aspect");
        Aspect aspect = Aspect.getAspect(tag.toLowerCase());
        if (aspect == null) {
            throw new JsonParseException("Unknown aspect '" + tag + "' (use the Thaumcraft 4 aspect tag, e.g. ignis, aqua, terra)");
        }
        return new RequirementAspect(type, amount, aspect);
    }

    @Nullable
    @Override
    public String requiresModid() {
        return Mods.THAUMCRAFT.modid;
    }
}
