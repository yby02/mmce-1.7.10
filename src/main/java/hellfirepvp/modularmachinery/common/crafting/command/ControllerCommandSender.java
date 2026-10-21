/*******************************************************************************
 * HellFirePvP / Modular Machinery 2018
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.crafting.command;

import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import github.kasuminova.mmce.compat.BlockPos;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

/**
 * 以控制器身份执行配方命令。1.7.10 移植：ICommandSender 方法签名改为 1.7.10 版本。
 */
public class ControllerCommandSender implements ICommandSender {
    private final TileMultiblockMachineController controller;

    public ControllerCommandSender(TileMultiblockMachineController controller) {
        this.controller = controller;
    }

    @Override
    public String getCommandSenderName() {
        return "@[MM-Controller]";
    }

    @Override
    public IChatComponent func_145748_c_() {
        return new ChatComponentText(getCommandSenderName());
    }

    @Override
    public void addChatMessage(IChatComponent message) {
    }

    @Override
    public boolean canCommandSenderUseCommand(int permLevel, String commandName) {
        return permLevel <= 2;
    }

    @Override
    public ChunkCoordinates getPlayerCoordinates() {
        BlockPos pos = controller.getPos();
        return new ChunkCoordinates(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public World getEntityWorld() {
        return controller.getWorld();
    }
}
