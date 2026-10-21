package youyihj.mmce.common.item;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.kasuminova.mmce.compat.BlockPos;
import github.kasuminova.mmce.compat.registry.IRegistryNamed;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import youyihj.mmce.common.preview.StructurePreviewHelper;

import java.util.List;

/**
 * @author youyihj
 * <p>
 * 机械投影仪：潜行右键控制器，在该控制器处投影其机器结构（未潜行时右键会打开控制器界面，不会调用到物品）。
 * <p>
 * 1.7.10 移植：工厂控制器 {@link hellfirepvp.modularmachinery.common.block.BlockFactoryController} 继承
 * {@link BlockController}，上游分开的两个判断合为一个；没有副手；纯客户端行为，服务端什么都不做。上游没有合成表。
 */
public class MachineProjector extends Item implements IRegistryNamed {
    public static final MachineProjector INSTANCE = new MachineProjector();

    private MachineProjector() {
        setMaxStackSize(1);
        IRegistryNamed.setName(this, ModularMachinery.MODID, "machine_projector");
        setUnlocalizedName(ModularMachinery.MODID + '.' + "machine_projector");
        setTextureName(ModularMachinery.MODID + ":machine_projector");
        setCreativeTab(CommonProxy.creativeTabModularMachinery);
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World worldIn, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        Block block = worldIn.getBlock(x, y, z);
        if (block instanceof BlockController controller && worldIn.isRemote) {
            DynamicMachine machine = controller.getParentMachine();
            if (machine != null) {
                StructurePreviewHelper.renderMachinePreview(machine, new BlockPos(x, y, z));
                return true;
            }
        }
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        tooltip.add(StatCollector.translateToLocal("tooltip.modularmachinery.machine_projector"));
    }
}
