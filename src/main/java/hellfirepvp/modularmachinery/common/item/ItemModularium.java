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
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.data.Config;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * 模块化金属锭。1.7.10 移植：贴图 modularmachinery:modularium，按机器颜色着色。
 */
public class ItemModularium extends Item implements ItemDynamicColor, IRegistryNamed {

    public ItemModularium() {
        setMaxStackSize(64);
        setCreativeTab(CommonProxy.creativeTabModularMachinery);
        setTextureName(ModularMachinery.MODID + ":modularium");
    }

    @Override
    public int getColorFromItemstack(ItemStack stack, int tintIndex) {
        return Config.machineColor;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getColorFromItemStack(ItemStack stack, int pass) {
        return getColorFromItemstack(stack, pass);
    }
}
