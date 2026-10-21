/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.item;

import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import java.util.UUID;

/**
 * 控制器的 ItemBlock：显示机器名称，放置时写入所有者。
 * <p>
 * 1.7.10 移植：placeBlockAt 使用 1.7.10 Forge 的 x/y/z + meta 签名。
 */
public class ItemBlockController extends ItemBlockMachineComponent {

    private final BlockController ctrlBlock;

    public ItemBlockController(final BlockController ctrlBlock) {
        super(ctrlBlock);
        this.ctrlBlock = ctrlBlock;
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return ctrlBlock.getLocalizedName();
    }

    @Override
    public int getColorFromItemstack(ItemStack stack, int tintIndex) {
        return ctrlBlock.getColorFromItemstack(stack, tintIndex);
    }

    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
                                float hitX, float hitY, float hitZ, int metadata) {
        if (super.placeBlockAt(stack, player, world, x, y, z, side, hitX, hitY, hitZ, metadata)) {
            TileEntity tile = world.getTileEntity(x, y, z);
            if (tile instanceof TileMultiblockMachineController ctrl) {
                NBTTagCompound stackTag = stack.getTagCompound();
                if (stackTag != null && stackTag.hasKey("owner")) {
                    String ownerUUIDStr = stackTag.getString("owner");
                    try {
                        ctrl.setOwner(UUID.fromString(ownerUUIDStr));
                    } catch (Exception e) {
                        ModularMachinery.log.warn("Invalid owner uuid " + ownerUUIDStr, e);
                    }
                } else {
                    ctrl.setOwner(player.getGameProfile().getId());
                }
            }
            return true;
        }
        return false;
    }
}
