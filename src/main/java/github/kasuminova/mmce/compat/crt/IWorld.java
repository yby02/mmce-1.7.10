package github.kasuminova.mmce.compat.crt;

import github.kasuminova.mmce.compat.Worlds;
import net.minecraft.world.World;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenMethod;

/**
 * 1.7.10 移植适配：同名 ZenClass，替代 CraftTweaker 1.12 的 crafttweaker.world.IWorld（仅保留 MMCE 常用成员）。
 */
@ZenRegister
@ZenClass("crafttweaker.world.IWorld")
public final class IWorld {

    private final World world;

    public IWorld(World world) {
        this.world = world;
    }

    public World getInternal() {
        return world;
    }

    @ZenGetter("dimension")
    public int getDimension() {
        return Worlds.getDimension(world);
    }

    @ZenGetter("remote")
    public boolean isRemote() {
        return world.isRemote;
    }

    @ZenGetter("time")
    public long getTime() {
        return world.getTotalWorldTime();
    }

    @ZenGetter("worldTime")
    public long getWorldTime() {
        return world.getWorldTime();
    }

    @ZenMethod
    public boolean isDayTime() {
        return world.isDaytime();
    }

    @ZenMethod
    public boolean isRaining() {
        return world.isRaining();
    }

    @ZenMethod
    public IBlockState getBlockState(IBlockPos pos) {
        return new IBlockState(Worlds.getBlockState(world, pos.getInternal()));
    }

    @ZenMethod
    public int getRedstonePower(IBlockPos pos) {
        return Worlds.getRedstonePowerFromNeighbors(world, pos.getInternal());
    }
}
