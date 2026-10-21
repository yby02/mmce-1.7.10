package hellfirepvp.modularmachinery.common.integration.crafttweaker.upgrade;

import github.kasuminova.mmce.compat.ItemStacks;

import github.kasuminova.mmce.compat.crt.CraftTweakerAPI;
import github.kasuminova.mmce.compat.crt.ZenRegister;
import minetweaker.api.item.IItemStack;
import github.kasuminova.mmce.compat.crt.CraftTweakerMC;
import github.kasuminova.mmce.common.capability.CapabilityUpgrade;
import github.kasuminova.mmce.common.upgrade.MachineUpgrade;
import github.kasuminova.mmce.common.upgrade.SimpleDynamicMachineUpgrade;
import github.kasuminova.mmce.common.upgrade.SimpleMachineUpgrade;
import github.kasuminova.mmce.common.upgrade.registry.RegistryUpgrade;
import net.minecraft.item.ItemStack;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import javax.annotation.Nullable;

@ZenRegister
@ZenClass("mods.modularmachinery.MachineUpgradeHelper")
public class MachineUpgradeHelper {

    /**
     * 为一个物品注册升级能力，只有在注册了之后才能够添加升级。
     *
     * @param itemStack 物品
     */
    @ZenMethod
    public static void registerSupportedItem(IItemStack itemStack) {
        ItemStack stack = CraftTweakerMC.getItemStack(itemStack);
        if (!ItemStacks.isEmpty(stack)) {
            RegistryUpgrade.addSupportedItem(stack);
        }
    }

    /**
     * 为一个物品添加固定的机械升级，会在物品被创建时自动添加物品。
     *
     * @param itemStack   物品
     * @param upgradeName 升级名称
     */
    @ZenMethod
    public static void addFixedUpgrade(IItemStack itemStack, String upgradeName) {
        ItemStack stack = CraftTweakerMC.getItemStack(itemStack);
        if (ItemStacks.isEmpty(stack)) {
            return;
        }
        MachineUpgrade upgrade = RegistryUpgrade.getUpgrade(upgradeName);
        if (upgrade == null) {
            CraftTweakerAPI.logError("[ModularMachinery] Cloud not find MachineUpgrade " + upgradeName + '!');
            return;
        }
        RegistryUpgrade.addFixedUpgrade(stack, upgrade);
    }

    /**
     * 将一个升级应用至机械升级，相当于直接写入相关升级的 NBT.
     *
     * @param stackCT     物品
     * @param upgradeName 名称
     * @return 添加了目标机械升级的物品。
     */
    @ZenMethod
    public static IItemStack addUpgradeToIItemStack(IItemStack stackCT, String upgradeName) {
        ItemStack stack = CraftTweakerMC.getItemStack(stackCT);
        if (!RegistryUpgrade.supportsUpgrade(stack)) {
            CraftTweakerAPI.logWarning("[ModularMachinery] " + stackCT.getDefinition().getId() + " does not support upgrade!");
            return stackCT;
        }
        CapabilityUpgrade capability = CapabilityUpgrade.get(stack);
        if (capability == null) {
            return stackCT;
        }
        MachineUpgrade upgrade = RegistryUpgrade.getUpgrade(upgradeName);
        if (upgrade == null) {
            CraftTweakerAPI.logWarning("[ModularMachinery] Cloud not found MachineUpgrade " + upgradeName + '!');
            return stackCT;
        }
        capability.getUpgrades().add(upgrade);
        return CraftTweakerMC.getIItemStack(stack);
    }

    /**
     * 获取一个已注册的机械升级。
     *
     * @param upgradeName 升级名称。
     * @return 机械升级，如果无则为 null
     */
    @Nullable
    @ZenMethod
    public static MachineUpgrade getUpgrade(String upgradeName) {
        return RegistryUpgrade.getUpgrade(upgradeName);
    }

    /**
     * 将 MachineUpgrade 转换为 SimpleDynamicMachineUpgrade
     *
     * @param upgrade 升级
     * @return 强制转换后的升级，如果不支持则为 null。
     */
    @ZenMethod
    public static SimpleDynamicMachineUpgrade castToSimpleDynamicMachineUpgrade(MachineUpgrade upgrade) {
        return upgrade instanceof SimpleDynamicMachineUpgrade ? (SimpleDynamicMachineUpgrade) upgrade : null;
    }

    /**
     * 将 MachineUpgrade 转换为 SimpleMachineUpgrade
     *
     * @param upgrade 升级
     * @return 强制转换后的升级，如果不支持则为 null。
     */
    @ZenMethod
    public static SimpleMachineUpgrade castToSimpleMachineUpgrade(MachineUpgrade upgrade) {
        return upgrade instanceof SimpleMachineUpgrade ? (SimpleMachineUpgrade) upgrade : null;
    }
}
