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
import github.kasuminova.mmce.compat.ResourceLocation;
import github.kasuminova.mmce.compat.registry.IRegistryNamed;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: ItemBlueprint
 * Created by HellFirePvP
 * Date: 28.06.2017 / 18:40
 * <p>
 * 1.7.10 移植：没有副手，打开预览 GUI 时 x 参数恒为 0（主手）；NBT 格式与 1.12.2 相同（dynamicmachine 键）。
 */
public class ItemBlueprint extends Item implements IRegistryNamed {

    public static final String DYNAMIC_MACHINE_NBT_KEY = "dynamicmachine";

    public ItemBlueprint() {
        setMaxStackSize(16);
        setCreativeTab(CommonProxy.creativeTabModularMachinery);
        setTextureName(ModularMachinery.MODID + ":blueprint");
    }

    @Nullable
    public static DynamicMachine getAssociatedMachine(ItemStack stack) {
        ResourceLocation key = getAssociatedMachineKey(stack);
        return key == null ? null : MachineRegistry.getRegistry().getMachine(key);
    }

    @Nullable
    public static ResourceLocation getAssociatedMachineKey(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) {
            return null;
        }
        if (!stack.getTagCompound().hasKey(DYNAMIC_MACHINE_NBT_KEY)) {
            return null;
        }
        return new ResourceLocation(stack.getTagCompound().getString(DYNAMIC_MACHINE_NBT_KEY));
    }

    public static void setAssociatedMachine(ItemStack stack, DynamicMachine machine) {
        setAssociatedMachine(stack, machine.getRegistryName());
    }

    public static void setAssociatedMachine(ItemStack stack, ResourceLocation machineKey) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound().setString(DYNAMIC_MACHINE_NBT_KEY, machineKey.toString());
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        for (DynamicMachine machine : MachineRegistry.getRegistry()) {
            ItemStack stack = new ItemStack(this);
            setAssociatedMachine(stack, machine);
            list.add(stack);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        DynamicMachine machine = getAssociatedMachine(stack);
        if (machine == null) {
            tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("tooltip.machinery.empty"));
        } else {
            tooltip.add(EnumChatFormatting.GRAY + machine.getLocalizedName());
        }
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World worldIn, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        if (worldIn.isRemote && getAssociatedMachine(stack) != null) {
            player.openGui(ModularMachinery.instance, CommonProxy.GuiType.BLUEPRINT_PREVIEW.ordinal(), worldIn, 0, 0, 0);
        }
        return true;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack held, World worldIn, EntityPlayer player) {
        if (worldIn.isRemote && getAssociatedMachine(held) != null) {
            player.openGui(ModularMachinery.instance, CommonProxy.GuiType.BLUEPRINT_PREVIEW.ordinal(), worldIn, 0, 0, 0);
        }
        return held;
    }

}
