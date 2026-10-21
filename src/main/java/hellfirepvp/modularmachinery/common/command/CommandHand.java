/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.command;

import github.kasuminova.mmce.compat.ItemStacks;

import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.network.PktCopyToClipboard;
import hellfirepvp.modularmachinery.common.util.nbt.NBTJsonSerializer;
import github.kasuminova.mmce.compat.RegistryNames;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: CommandHand
 * Created by HellFirePvP
 * Date: 19.08.2017 / 13:13
 */
public class CommandHand extends CommandBase {

    @Override
    public String getCommandName() {
        return "mm-hand";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "command.modularmachinery.hand";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        // 1.7.10 没有副手
        ItemStack held = player.getCurrentEquippedItem();
        if (ItemStacks.isEmpty(held)) {
            player.addChatMessage(new ChatComponentTranslation("command.modularmachinery.hand.empty"));
            return;
        }
        Item heldItem = held.getItem();
        StringBuilder sb = new StringBuilder();
        sb.append(RegistryNames.getName(heldItem));
        if (heldItem.getHasSubtypes()) {
            sb.append("@").append(held.getItemDamage());
        }
        NBTTagCompound cmp = held.writeToNBT(new NBTTagCompound());
        if (cmp.hasKey("tag")) {
            String json = NBTJsonSerializer.serializeNBT(cmp.getTag("tag"));
            if (!json.isEmpty()) {
                sb.append(" (with nbt: ").append(json).append(" )");
            }
        }

        String str = sb.toString();
        player.addChatMessage(new ChatComponentText(str));
        ModularMachinery.NET_CHANNEL.sendTo(new PktCopyToClipboard(str), player);

        int burn = TileEntityFurnace.getItemBurnTime(held);
        if (burn > 0) {
            player.addChatMessage(new ChatComponentText("Fuel BurnTime: " + burn));
        }
    }

}
