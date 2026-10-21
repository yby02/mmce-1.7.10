package github.kasuminova.mmce.common.helper;

import github.kasuminova.mmce.compat.Worlds;

import github.kasuminova.mmce.compat.crt.ZenRegister;
import github.kasuminova.mmce.compat.crt.CraftTweakerMC;
import github.kasuminova.mmce.compat.crt.IBlockPos;
import github.kasuminova.mmce.compat.crt.IWorld;
import net.minecraft.tileentity.TileEntity;
import github.kasuminova.mmce.compat.BlockPos;
import net.minecraft.world.World;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("mods.modularmachinery.MachineController")
public class MachineController {

    /**
     * 获取对应 IWorld 中的某个坐标的控制器。
     *
     * @param worldCT IWorld
     * @param posCT   IBlockPos
     * @return 如果无控制器则返回 null，否则返回 IMachineController 实例。
     */
    @ZenMethod
    public static IMachineController getControllerAt(IWorld worldCT, IBlockPos posCT) {
        World world = CraftTweakerMC.getWorld(worldCT);
        BlockPos pos = CraftTweakerMC.getBlockPos(posCT);
        if (world == null || pos == null || !Worlds.isBlockLoaded(world, pos)) {
            return null;
        }

        TileEntity te = Worlds.getTileEntity(world, pos);
        return te instanceof IMachineController ? (IMachineController) te : null;
    }

    /**
     * 获取对应 IWorld 中的某个坐标的控制器。
     *
     * @param worldCT IWorld
     * @return 如果无控制器则返回 null，否则返回 IMachineController 实例。
     */
    @ZenMethod
    public static IMachineController getControllerAt(IWorld worldCT, int x, int y, int z) {
        World world = CraftTweakerMC.getWorld(worldCT);
        BlockPos pos = new BlockPos(x, y, z);

        if (world == null || !Worlds.isBlockLoaded(world, pos)) {
            return null;
        }

        TileEntity te = Worlds.getTileEntity(world, pos);
        return te instanceof IMachineController ? (IMachineController) te : null;
    }

}
