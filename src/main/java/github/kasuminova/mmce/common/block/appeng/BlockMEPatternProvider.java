package github.kasuminova.mmce.common.block.appeng;

import appeng.api.implementations.items.IMemoryCard;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.kasuminova.mmce.common.tile.MEPatternProvider;
import github.kasuminova.mmce.compat.IBlockState;
import hellfirepvp.modularmachinery.common.CommonProxy;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/**
 * ME 机械样板供应器。潜行手持 AE2 内存卡右键记录坐标（供样板镜像绑定）；破坏时样板、缓存与设置保存在掉落物的 NBT 中（同上游）。
 * 1.7.10 移植：上游用石英切割刀打开 AE2 的重命名界面，GTNH AE2 rv3 中该界面只接受 AE2 自己的方块实体，未移植。
 */
public class BlockMEPatternProvider extends BlockMEMachineComponent {

    public static final String MEMORY_CARD_NAME = "tile.modularmachinery.blockmepatternprovider";

    public BlockMEPatternProvider() {
        super("mepatternprovider");
    }

    @Override
    public boolean onBlockActivated(World worldIn, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        if (worldIn.isRemote) {
            return true;
        }
        ItemStack heldItem = player.getHeldItem();
        if (heldItem != null && player.isSneaking() && heldItem.getItem() instanceof IMemoryCard memoryCard) {
            NBTTagCompound data = new NBTTagCompound();
            data.setInteger("x", x);
            data.setInteger("y", y);
            data.setInteger("z", z);
            data.setInteger("dim", worldIn.provider.dimensionId);
            memoryCard.setMemoryCardContents(heldItem, MEMORY_CARD_NAME, data);
            player.addChatMessage(new ChatComponentTranslation("message.blockmepatternprovider.save"));
            return true;
        }
        if (worldIn.getTileEntity(x, y, z) instanceof MEPatternProvider) {
            openGui(player, CommonProxy.GuiType.ME_PATTERN_PROVIDER.ordinal(), worldIn, x, y, z);
        }
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new MEPatternProvider();
    }

    @Override
    protected boolean dropsHandledInBreakBlock() {
        return true;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        TileEntity te = world.getTileEntity(x, y, z);
        ItemStack dropped = new ItemStack(Item.getItemFromBlock(this));
        if (te instanceof MEPatternProvider provider && !provider.isAllDefault()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setTag("patternProvider", provider.writeProviderNBT(new NBTTagCompound()));
            dropped.setTagCompound(tag);
        }
        spawnAsEntity(world, x, y, z, dropped);
        super.breakBlock(world, x, y, z, block, meta);
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, x, y, z, placer, stack);
        TileEntity te = world.getTileEntity(x, y, z);
        NBTTagCompound tag = stack.getTagCompound();
        if (!world.isRemote && te instanceof MEPatternProvider provider && tag != null && tag.hasKey("patternProvider")) {
            provider.readProviderNBT(tag.getCompoundTag("patternProvider"));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable EntityPlayer player, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey("patternProvider")) {
            tooltip.add(StatCollector.translateToLocal("gui.mepatternprovider.nbt_stored"));
        }
        tooltip.add(StatCollector.translateToLocal("tooltip.groupinput.block"));
    }
}
