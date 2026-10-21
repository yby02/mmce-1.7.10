package hellfirepvp.modularmachinery.common.integration.crafttweaker.helper;

import github.kasuminova.mmce.compat.crt.ZenRegister;
import github.kasuminova.mmce.compat.crt.IBlockState;
import minetweaker.api.data.IData;
import github.kasuminova.mmce.compat.crt.IBlockPos;
import github.kasuminova.mmce.compat.crt.IWorld;
import stanhebben.zenscript.annotations.ZenClass;

@ZenRegister
@ZenClass("mods.modularmachinery.AdvancedBlockChecker")
@FunctionalInterface
public interface AdvancedBlockCheckerCT {
    boolean isMatch(IWorld world, IBlockPos pos, IBlockState blockState, IData nbt);
}
