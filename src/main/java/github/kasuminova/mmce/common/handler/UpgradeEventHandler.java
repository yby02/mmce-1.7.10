package github.kasuminova.mmce.common.handler;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.kasuminova.mmce.common.capability.CapabilityUpgrade;
import github.kasuminova.mmce.common.upgrade.MachineUpgrade;
import github.kasuminova.mmce.common.upgrade.registry.RegistryUpgrade;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

/**
 * 1.7.10 移植：1.12.2 的 AttachCapabilitiesEvent 不存在，升级数据改为按需从物品读取（见 {@link CapabilityUpgrade#get(ItemStack)}）。
 */
@SuppressWarnings("MethodMayBeStatic")
public class UpgradeEventHandler {

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onUpgradeItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.itemStack;
        if (!RegistryUpgrade.supportsUpgrade(stack)) {
            return;
        }
        CapabilityUpgrade upgrade = CapabilityUpgrade.get(stack);
        if (upgrade == null) {
            return;
        }
        List<MachineUpgrade> upgrades = upgrade.getUpgrades();
        List<String> toolTip = event.toolTip;
        upgrades.forEach(machineUpgrade -> {
            toolTip.add(machineUpgrade.getType().getLocalizedName());
            toolTip.addAll(machineUpgrade.getDescriptions());
        });
    }
}
