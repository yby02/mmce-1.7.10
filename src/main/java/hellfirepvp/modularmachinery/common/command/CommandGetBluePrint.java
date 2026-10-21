package hellfirepvp.modularmachinery.common.command;

import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.item.ItemBlueprint;
import hellfirepvp.modularmachinery.common.lib.ItemsMM;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import github.kasuminova.mmce.compat.ResourceLocation;
import net.minecraft.util.ChatComponentTranslation;

public class CommandGetBluePrint extends CommandBase {
    @Override
    public String getCommandName() {
        return "mm-get_blueprint";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "command.modularmachinery.get_blueprint";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (!(sender instanceof final EntityPlayer player)) {
            sender.addChatMessage(new ChatComponentTranslation("command.modularmachinery.get_blueprint.player_only"));
            return;
        }
        if (args.length < 1) {
            sender.addChatMessage(new ChatComponentTranslation(getCommandUsage(sender)));
            return;
        }

        String machineName = args[0];

        DynamicMachine machine = MachineRegistry.getRegistry().getMachine(new ResourceLocation(ModularMachinery.MODID, machineName));
        if (machine == null) {
            sender.addChatMessage(new ChatComponentTranslation(
                "command.modularmachinery.get_blueprint.not_found", machineName));
            return;
        }

        ItemStack blueprint = new ItemStack(ItemsMM.blueprint);
        ItemBlueprint.setAssociatedMachine(blueprint, machine);

        if (player.inventory.addItemStackToInventory(blueprint)) {
            sender.addChatMessage(
                new ChatComponentTranslation("command.modularmachinery.get_blueprint.success"));
        }
    }
}
