package github.kasuminova.mmce.compat.crt;

import github.kasuminova.mmce.compat.EnumFacing;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenMethod;

/**
 * 1.7.10 移植适配：同名 ZenClass，替代 CraftTweaker 1.12 的 crafttweaker.world.IFacing。
 */
@ZenRegister
@ZenClass("crafttweaker.world.IFacing")
public final class IFacing {

    private static final IFacing[] VALUES = new IFacing[EnumFacing.VALUES.length];

    static {
        for (EnumFacing facing : EnumFacing.VALUES) {
            VALUES[facing.getIndex()] = new IFacing(facing);
        }
    }

    private final EnumFacing facing;

    private IFacing(EnumFacing facing) {
        this.facing = facing;
    }

    public static IFacing of(EnumFacing facing) {
        return facing == null ? null : VALUES[facing.getIndex()];
    }

    public EnumFacing getInternal() {
        return facing;
    }

    @ZenGetter("name")
    public String getName() {
        return facing.getName();
    }

    @ZenMethod
    public IFacing opposite() {
        return of(facing.getOpposite());
    }

    @ZenMethod
    public static IFacing north() {
        return of(EnumFacing.NORTH);
    }

    @ZenMethod
    public static IFacing south() {
        return of(EnumFacing.SOUTH);
    }

    @ZenMethod
    public static IFacing east() {
        return of(EnumFacing.EAST);
    }

    @ZenMethod
    public static IFacing west() {
        return of(EnumFacing.WEST);
    }

    @ZenMethod
    public static IFacing up() {
        return of(EnumFacing.UP);
    }

    @ZenMethod
    public static IFacing down() {
        return of(EnumFacing.DOWN);
    }

    @Override
    public String toString() {
        return facing.getName();
    }
}
