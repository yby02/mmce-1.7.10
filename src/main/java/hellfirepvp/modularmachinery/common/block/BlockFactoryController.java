package hellfirepvp.modularmachinery.common.block;

import github.kasuminova.mmce.compat.IBlockState;
import github.kasuminova.mmce.compat.registry.IRegistryNamed;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.tiles.TileFactoryController;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * 工厂控制器。1.7.10 移植：同 {@link BlockController}，朝向存于 meta。
 */
public class BlockFactoryController extends BlockController {
    public static final Map<DynamicMachine, BlockFactoryController> FACTORY_CONTROLLERS = new HashMap<>();

    public BlockFactoryController() {
    }

    public BlockFactoryController(DynamicMachine parentMachine) {
        this();
        this.parentMachine = parentMachine;
        String path = parentMachine.getRegistryName().getPath() + "_factory_controller";
        IRegistryNamed.setName(this, ModularMachinery.MODID, path);
        setBlockName(ModularMachinery.MODID + '.' + path);
    }

    public static BlockFactoryController getControllerWithMachine(DynamicMachine machine) {
        return FACTORY_CONTROLLERS.get(machine);
    }

    @Override
    public boolean onBlockActivated(World worldIn, int x, int y, int z, EntityPlayer playerIn, int side, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            TileEntity te = worldIn.getTileEntity(x, y, z);
            if (te instanceof TileFactoryController) {
                openGui(playerIn, CommonProxy.GuiType.FACTORY.ordinal(), worldIn, x, y, z);
            }
        }
        return true;
    }

    @Override
    public String getLocalizedName() {
        if (parentMachine != null) {
            return StatCollector.translateToLocalFormatted("tile.modularmachinery.machinefactorycontroller.name", parentMachine.getLocalizedName());
        }
        return StatCollector.translateToLocal("tile.modularmachinery.blockfactorycontroller.name");
    }

    @Override
    protected String getFrontOverlayTexture() {
        return "overlay_factory_controller";
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileFactoryController(state);
    }
}
