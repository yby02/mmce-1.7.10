package github.kasuminova.mmce.common.block.appeng;

import appeng.api.implementations.items.IMemoryCard;
import appeng.api.implementations.items.MemoryCardMessages;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.kasuminova.mmce.common.tile.MEPatternMirrorImage;
import github.kasuminova.mmce.compat.BlockPos;
import github.kasuminova.mmce.compat.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/** ME 机械样板镜像：手持记录了供应器坐标的 AE2 内存卡右键绑定；其他情况右键显示绑定的坐标。 */
public class BlockMEPatternMirrorImage extends BlockMEMachineComponent {

    public BlockMEPatternMirrorImage() {
        super("mepatternmirrorimage");
    }

    @Override
    public boolean onBlockActivated(World worldIn, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        if (worldIn.isRemote) {
            return true;
        }
        if (!(worldIn.getTileEntity(x, y, z) instanceof MEPatternMirrorImage tileEntity)) {
            return true;
        }
        ItemStack heldItem = player.getHeldItem();
        if (heldItem != null && !player.isSneaking() && heldItem.getItem() instanceof IMemoryCard memoryCard) {
            NBTTagCompound data = memoryCard.getData(heldItem);
            if (BlockMEPatternProvider.MEMORY_CARD_NAME.equals(memoryCard.getSettingsName(heldItem))
                && data.getInteger("dim") == worldIn.provider.dimensionId) {
                tileEntity.providerPos = new BlockPos(data.getInteger("x"), data.getInteger("y"), data.getInteger("z"));
                tileEntity.markForUpdateSync();
                player.addChatMessage(new ChatComponentTranslation("message.blockmepatternprovider.load"));
            } else {
                memoryCard.notifyUser(player, MemoryCardMessages.INVALID_MACHINE);
            }
            return true;
        }
        BlockPos pPos = tileEntity.providerPos;
        if (pPos == null) {
            player.addChatMessage(new ChatComponentTranslation("message.blockmepatternprovider.tip0"));
        } else {
            player.addChatMessage(new ChatComponentTranslation("message.blockmepatternprovider.tip1", pPos.getX(), pPos.getY(), pPos.getZ()));
        }
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new MEPatternMirrorImage();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable EntityPlayer player, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add(StatCollector.translateToLocal("tooltip.mepatternmirrorimage"));
    }
}
