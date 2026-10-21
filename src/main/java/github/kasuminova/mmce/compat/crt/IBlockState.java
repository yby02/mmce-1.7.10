package github.kasuminova.mmce.compat.crt;

import github.kasuminova.mmce.compat.RegistryNames;
import minetweaker.api.block.IBlockDefinition;
import minetweaker.api.minecraft.MineTweakerMC;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenMethod;

/**
 * 1.7.10 移植适配：同名 ZenClass，替代 CraftTweaker 1.12 的 crafttweaker.block.IBlockState。
 * 内部是“方块 + meta”（见 {@link github.kasuminova.mmce.compat.IBlockState}）。
 */
@ZenRegister
@ZenClass("crafttweaker.block.IBlockState")
public final class IBlockState implements IBlockStateMatcher {

    private final github.kasuminova.mmce.compat.IBlockState state;

    public IBlockState(github.kasuminova.mmce.compat.IBlockState state) {
        this.state = state;
    }

    public github.kasuminova.mmce.compat.IBlockState getInternal() {
        return state;
    }

    @ZenGetter("block")
    public IBlockDefinition getBlockDefinition() {
        return MineTweakerMC.getBlockDefinition(state.getBlock());
    }

    @ZenGetter("meta")
    public int getMeta() {
        return state.getMetadata();
    }

    @ZenGetter("registryName")
    public String getRegistryName() {
        return RegistryNames.getName(state.getBlock());
    }

    @Override
    @ZenMethod
    public boolean matches(IBlockState other) {
        return other != null && state.equals(other.state);
    }

    @Override
    public java.util.Collection<IBlockState> getMatchingBlockStates() {
        return java.util.Collections.singletonList(this);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof IBlockState && ((IBlockState) obj).state.equals(state);
    }

    @Override
    public int hashCode() {
        return state.hashCode();
    }

    @Override
    public String toString() {
        return state.toString();
    }
}
