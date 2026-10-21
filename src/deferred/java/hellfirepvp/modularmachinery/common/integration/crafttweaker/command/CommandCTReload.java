package hellfirepvp.modularmachinery.common.integration.crafttweaker.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumChatFormatting;
import youyihj.zenutils.impl.reload.ReloadCommand;

import static crafttweaker.mc1120.commands.SpecialMessagesChat.getNormalMessage;

/**
 * Similar to /ct reload, but can be executed by the server.
 */
public class CommandCTReload extends CommandBase {

    @Override
    public String getName() {
        return "mm-reload";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "command.modularmachinery.reload";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if (server.isDedicatedServer()) {
            sender.addChatMessage(getNormalMessage(EnumChatFormatting.RED + "DedicatedServer detected! May be cause issues!"));
        }

        ReloadCommand.reloadScripts(sender);
    }
}
