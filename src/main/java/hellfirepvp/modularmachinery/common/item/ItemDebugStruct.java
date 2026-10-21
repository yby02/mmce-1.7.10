/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.item;


import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.tiles.TileMachineController;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import github.kasuminova.mmce.compat.EnumFacing;
import github.kasuminova.mmce.compat.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;


/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: ItemDebugStruct
 * Created by HellFirePvP
 * Date: 29.10.2017 / 20:39
 */
public class ItemDebugStruct extends Item {

    // TODO: Realize it.
    public ItemDebugStruct() {
        setMaxStackSize(1);
        setCreativeTab(CommonProxy.creativeTabModularMachinery);
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World worldIn, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        if (worldIn.isRemote) {
            return true;
        }
        BlockPos pos = new BlockPos(x, y, z);
        TileEntity te = worldIn.getTileEntity(x, y, z);
        if (te instanceof TileMachineController) {
            DynamicMachine dm = ((TileMachineController) te).getBlueprintMachine();
            if (dm != null) {
                BlockArray pattern = dm.getPattern();
                if (pattern != null) {
                    EnumFacing ctrlFacing = BlockController.getFacing(worldIn.getBlockMetadata(x, y, z));
                    EnumFacing face = EnumFacing.NORTH;
                    player.addChatMessage(new ChatComponentText("Attempting structure matching:"));
                    player.addChatMessage(new ChatComponentText("Structure is facing: " + ctrlFacing.name()));
                    DynamicMachine.ModifierReplacementMap replacements = dm.getModifiersAsMatchingReplacements();
                    do {
                        if (face == ctrlFacing) {
                            BlockPos mismatch = pattern.getRelativeMismatchPosition(worldIn, pos, replacements);
                            if (mismatch != null) {
                                player.addChatMessage(new ChatComponentText("Failed at relative position: " + mismatch));
                            }
                        }
                        face = face.rotateYCCW();
                        pattern = pattern.rotateYCCW();
                        replacements = replacements.rotateYCCW();
                    } while (face != EnumFacing.NORTH);
                }
            }
        }
        return true;
    }
}
