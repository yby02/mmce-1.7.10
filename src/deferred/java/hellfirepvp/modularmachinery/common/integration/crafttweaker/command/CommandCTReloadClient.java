package hellfirepvp.modularmachinery.common.integration.crafttweaker.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import youyihj.zenutils.impl.reload.ReloadCommand;

/**
 * Similar to {@link CommandCTReload}, but only works in the client.
 */
public class CommandCTReloadClient extends CommandBase {
    @Override
    public String getName() {
        return "mm-reload_client";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "command.modularmachinery.reload_client";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        ReloadCommand.reloadScripts(sender);
    }
}
