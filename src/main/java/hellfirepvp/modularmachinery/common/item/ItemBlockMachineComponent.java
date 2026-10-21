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
import github.kasuminova.mmce.compat.ItemStacks;
import github.kasuminova.mmce.compat.registry.IRegistryNamed;
import hellfirepvp.modularmachinery.common.block.BlockMachineComponent;
import hellfirepvp.modularmachinery.common.data.Config;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import java.util.List;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: ItemBlockMachineComponent
 * Created by HellFirePvP
 * Date: 15.08.2017 / 16:25
 * <p>
 * 1.7.10 移植：1.7.10 的 Block 没有 addInformation，由本类转发到 {@link BlockMachineComponent#addInformation}；
 * 物品着色使用 1.7.10 的 getColorFromItemStack。
 */
public class ItemBlockMachineComponent extends ItemBlock implements ItemDynamicColor, IRegistryNamed {

    public ItemBlockMachineComponent(Block block) {
        super(block);
    }

    @Override
    public int getColorFromItemstack(ItemStack stack, int tintIndex) {
        if (ItemStacks.isEmpty(stack)) {
            return 0;
        }
        return Config.machineColor;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getColorFromItemStack(ItemStack stack, int pass) {
        return getColorFromItemstack(stack, pass);
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        if (field_150939_a instanceof BlockMachineComponent) {
            ((BlockMachineComponent) field_150939_a).addInformation(stack, player, (List<String>) tooltip, advanced);
        }
    }
}
