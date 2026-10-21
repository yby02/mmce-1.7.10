package github.kasuminova.mmce.common.block.appeng;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.kasuminova.mmce.common.tile.MEItemOutputBus;
import github.kasuminova.mmce.compat.IBlockState;
import github.kasuminova.mmce.compat.ItemStacks;
import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.util.IOInventory;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/** ME 机械物品输出总线。破坏时缓存中尚未送入 ME 的物品保存在掉落物的 NBT 中（同上游）。 */
public class BlockMEItemOutputBus extends BlockMEMachineComponent {

    public BlockMEItemOutputBus() {
        super("meoutputbus");
    }

    @Override
    public boolean onBlockActivated(World worldIn, int x, int y, int z, EntityPlayer playerIn, int side, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote && worldIn.getTileEntity(x, y, z) instanceof MEItemOutputBus) {
            openGui(playerIn, CommonProxy.GuiType.ME_ITEM_OUTPUT_BUS.ordinal(), worldIn, x, y, z);
        }
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new MEItemOutputBus();
    }

    @Override
    protected boolean dropsHandledInBreakBlock() {
        return true;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        TileEntity te = world.getTileEntity(x, y, z);
        ItemStack dropped = new ItemStack(Item.getItemFromBlock(this));
        if (te instanceof MEItemOutputBus bus && bus.hasItem()) {
            IOInventory inventory = bus.getInternalInventory();
            NBTTagCompound tag = new NBTTagCompound();
            tag.setTag("inventory", inventory.writeNBT());
            dropped.setTagCompound(tag);
            for (int i = 0; i < inventory.getSlots(); i++) {
                inventory.setStackInSlot(i, ItemStacks.EMPTY);
            }
        }
        spawnAsEntity(world, x, y, z, dropped);
        super.breakBlock(world, x, y, z, block, meta);
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, x, y, z, placer, stack);
        TileEntity te = world.getTileEntity(x, y, z);
        NBTTagCompound tag = stack.getTagCompound();
        if (!world.isRemote && te instanceof MEItemOutputBus bus && tag != null && tag.hasKey("inventory")) {
            bus.readInventoryNBT(tag.getCompoundTag("inventory"));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable EntityPlayer player, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey("inventory")) {
            tooltip.add(StatCollector.translateToLocal("gui.meitembus.nbt_stored"));
        }
    }
}
