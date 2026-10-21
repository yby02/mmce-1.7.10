package github.kasuminova.mmce.common.helper;


import github.kasuminova.mmce.compat.crt.ZenRegister;
import github.kasuminova.mmce.compat.crt.IBlockState;
import stanhebben.zenscript.annotations.ZenClass;

import java.util.function.Predicate;

@ZenRegister
@ZenClass("mods.modularmachinery.IBlockStatePredicate")
@FunctionalInterface
public interface IBlockStatePredicate extends Predicate<IBlockState> {

    @Override
    boolean test(IBlockState t);

}
