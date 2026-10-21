/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.item;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.kasuminova.mmce.compat.BlockPos;
import github.kasuminova.mmce.compat.registry.IRegistryNamed;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.selection.PlayerStructureSelectionHelper;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import java.util.List;

/**
 * 建造选择工具：右键方块加入/移出选区，潜行右键控制器把选区导出为机器 JSON。
 * <p>
 * 1.7.10 移植：权限判断改为 1.7.10 的 OP 检查（ServerConfigurationManager#func_152596_g），
 * 控制器朝向由 meta 读取。
 */
public class ItemConstructTool extends Item implements IRegistryNamed {

    public ItemConstructTool() {
        setMaxStackSize(1);
        setCreativeTab(CommonProxy.creativeTabModularMachinery);
        setTextureName(ModularMachinery.MODID + ":wrench");
        setFull3D();
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        tooltip.add(StatCollector.translateToLocal("tooltip.constructtool.creative"));
    }

    private static boolean canSendCommands(EntityPlayer player) {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) {
            return false;
        }
        // 单人游戏（开启作弊）或服务器 OP
        return server.isSinglePlayer() && server.getServerOwner() != null && server.getServerOwner().equals(player.getCommandSenderName())
            || server.getConfigurationManager().func_152596_g(player.getGameProfile());
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World worldIn, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote && player.capabilities.isCreativeMode && canSendCommands(player)) {
            Block block = worldIn.getBlock(x, y, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (block instanceof BlockController) {
                PlayerStructureSelectionHelper.finalizeSelection(BlockController.getFacing(worldIn.getBlockMetadata(x, y, z)), worldIn, pos, player);
                PlayerStructureSelectionHelper.purgeSelection(player);
                PlayerStructureSelectionHelper.sendSelection(player);
            } else {
                PlayerStructureSelectionHelper.toggleInSelection(player, pos);
                PlayerStructureSelectionHelper.sendSelection(player);
            }
        }
        return true;
    }
}
