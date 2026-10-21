package hellfirepvp.modularmachinery.client.util;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import hellfirepvp.modularmachinery.common.item.ItemBlockController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import java.util.UUID;

/**
 * 控制器物品（破坏后保留了所有者信息）的提示：当前玩家是否为所有者。
 * 1.7.10 移植：取自上游 ClientHandler 中与 1.7.10 相关的部分（ME 物品提示与缓冲池调试信息不移植）。
 */
public class ControllerOwnerTooltip {

    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.itemStack;
        if (stack == null || !(stack.getItem() instanceof ItemBlockController)) {
            return;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey("owner")) {
            return;
        }
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player == null) {
            return;
        }
        try {
            UUID owner = UUID.fromString(tag.getString("owner"));
            event.toolTip.add(I18n.format(owner.equals(player.getGameProfile().getId())
                ? "tooltip.item.controller.owner.self" : "tooltip.item.controller.owner.not_self"));
        } catch (IllegalArgumentException e) {
            event.toolTip.add(EnumChatFormatting.RED + "NBT read error.");
        }
    }
}
