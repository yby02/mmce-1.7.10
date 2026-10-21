package github.kasuminova.mmce.compat.crt;

import github.kasuminova.mmce.compat.BlockPos;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenMethod;

/**
 * 1.7.10 移植适配：MineTweaker3 没有 crafttweaker.world.IBlockPos，这里以同名 ZenClass 提供，保持 1.12 脚本的 import 可用。
 */
@ZenRegister
@ZenClass("crafttweaker.world.IBlockPos")
public final class IBlockPos {

    private final BlockPos pos;

    public IBlockPos(BlockPos pos) {
        this.pos = pos;
    }

    public BlockPos getInternal() {
        return pos;
    }

    @ZenGetter("x")
    public int getX() {
        return pos.getX();
    }

    @ZenGetter("y")
    public int getY() {
        return pos.getY();
    }

    @ZenGetter("z")
    public int getZ() {
        return pos.getZ();
    }

    @ZenMethod
    public IBlockPos getOffset(IFacing facing, int amount) {
        return new IBlockPos(pos.offset(facing.getInternal(), amount));
    }

    @ZenMethod
    public IBlockPos add(int x, int y, int z) {
        return new IBlockPos(pos.add(x, y, z));
    }

    @ZenMethod
    public IBlockPos add(IBlockPos other) {
        return new IBlockPos(pos.add(other.pos));
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof IBlockPos && ((IBlockPos) obj).pos.equals(pos);
    }

    @Override
    public int hashCode() {
        return pos.hashCode();
    }

    @Override
    public String toString() {
        return pos.toString();
    }
}
