package github.kasuminova.mmce.compat.crt;

import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import java.util.Collection;

/**
 * 1.7.10 移植适配：同名 ZenClass，替代 CraftTweaker 1.12 的 crafttweaker.block.IBlockStateMatcher。
 */
@ZenRegister
@ZenClass("crafttweaker.block.IBlockStateMatcher")
public interface IBlockStateMatcher {

    @ZenMethod
    boolean matches(IBlockState state);

    Collection<IBlockState> getMatchingBlockStates();
}
