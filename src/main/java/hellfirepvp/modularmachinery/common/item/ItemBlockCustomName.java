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
import github.kasuminova.mmce.compat.registry.IRegistryNamed;
import hellfirepvp.modularmachinery.common.block.BlockCustomName;
import hellfirepvp.modularmachinery.common.block.BlockMachineComponent;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import java.util.List;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: ItemBlockCustomName
 * Created by HellFirePvP
 * Date: 28.06.2017 / 20:51
 * <p>
 * 1.7.10 移植：翻译键沿用 1.12.2 的 “tile.modularmachinery.&lt;方块&gt;.&lt;变体&gt;”（1.12.2 的 getTranslationKey 对应 1.7.10 的 getUnlocalizedName）。
 */
public class ItemBlockCustomName extends ItemBlock implements IRegistryNamed {

    public ItemBlockCustomName(Block block) {
        super(block);
        setHasSubtypes(true);
    }

    @Override
    public int getMetadata(int damage) {
        return damage;
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        Block block = field_150939_a;
        if (block instanceof BlockCustomName) {
            String identifier = ((BlockCustomName) block).getIdentifierForMeta(stack.getItemDamage());
            return super.getUnlocalizedName(stack) + "." + identifier;
        }
        return super.getUnlocalizedName(stack);
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
