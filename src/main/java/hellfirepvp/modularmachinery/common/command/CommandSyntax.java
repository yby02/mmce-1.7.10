/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.command;

import hellfirepvp.modularmachinery.common.crafting.RecipeRegistry;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: CommandSyntax
 * Created by HellFirePvP
 * Date: 16.08.2017 / 15:00
 */
public class CommandSyntax extends CommandBase {

    @Override
    public String getCommandName() {
        return "mm-syntax";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "command.modularmachinery.syntax";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        sender.addChatMessage(new ChatComponentText("Testing Machines:"));
        MachineRegistry.preloadMachines();
        MachineRegistry.loadMachines(sender);
        sender.addChatMessage(new ChatComponentText(""));
        sender.addChatMessage(new ChatComponentText("Testing Recipes:"));
        RecipeRegistry.getRegistry().loadRecipeRegistry(sender, false);
    }

}
