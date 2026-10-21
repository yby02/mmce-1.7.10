package github.kasuminova.mmce.compat.crt;

import net.minecraft.util.AxisAlignedBB;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;

/**
 * 1.7.10 移植适配：同名 ZenClass，替代 CraftTweaker 1.12 的 crafttweaker.util.IAxisAlignedBB。
 */
@ZenRegister
@ZenClass("crafttweaker.util.IAxisAlignedBB")
public final class IAxisAlignedBB {

    private final AxisAlignedBB box;

    public IAxisAlignedBB(AxisAlignedBB box) {
        this.box = box;
    }

    public AxisAlignedBB getInternal() {
        return box;
    }

    @ZenGetter("minX")
    public double getMinX() {
        return box.minX;
    }

    @ZenGetter("minY")
    public double getMinY() {
        return box.minY;
    }

    @ZenGetter("minZ")
    public double getMinZ() {
        return box.minZ;
    }

    @ZenGetter("maxX")
    public double getMaxX() {
        return box.maxX;
    }

    @ZenGetter("maxY")
    public double getMaxY() {
        return box.maxY;
    }

    @ZenGetter("maxZ")
    public double getMaxZ() {
        return box.maxZ;
    }
}
