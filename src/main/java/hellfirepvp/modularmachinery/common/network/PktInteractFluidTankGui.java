/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.kasuminova.mmce.compat.ItemStacks;
import github.kasuminova.mmce.compat.fluids.FluidContainerInteraction;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.container.ContainerFluidHatch;
import hellfirepvp.modularmachinery.common.tiles.base.TileFluidTank;
import hellfirepvp.modularmachinery.common.util.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidContainerRegistry;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: PktInteractFluidTankGui
 * Created by HellFirePvP
 * Date: 02.03.2019 / 14:20
 * <p>
 * 1.7.10 移植：
 * - 1.7.10 的 SimpleNetworkWrapper 处理器本身在主线程执行，不需要 addScheduledTask；
 * - 鼠标上拿着多个流体容器时，只倒空其中 1 个，倒空后的容器放入背包（上游会把整叠替换成 1 个空容器）。
 */
public class PktInteractFluidTankGui implements IMessage, IMessageHandler<PktInteractFluidTankGui, IMessage> {

    private ItemStack held;

    public PktInteractFluidTankGui() {
        this(ItemStacks.EMPTY);
    }

    public PktInteractFluidTankGui(ItemStack held) {
        this.held = held;
    }

    @SideOnly(Side.CLIENT)
    private static void updateClientHand(PktInteractFluidTankGui pkt) {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player == null || !(player.openContainer instanceof ContainerFluidHatch)) {
            return;
        }
        if (!ItemStacks.isEmpty(player.inventory.getItemStack())) { //Has to have had an item before... obviously.. right?
            player.inventory.setItemStack(pkt.held);
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.held = ByteBufUtils.readItemStack(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeItemStack(buf, this.held);
    }

    @Override
    public IMessage onMessage(PktInteractFluidTankGui pkt, MessageContext ctx) {
        if (ctx.side == Side.SERVER) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (!(player.openContainer instanceof ContainerFluidHatch fluidHatch)) {
                return null;
            }

            TileFluidTank hatch = fluidHatch.getOwner();
            ItemStack holding = player.inventory.getItemStack();
            if (ItemStacks.isEmpty(holding)) {
                return null;
            }

            ItemStack[] fas = FluidContainerInteraction.tryEmptyContainer(holding, hatch.getTank(), FluidContainerRegistry.BUCKET_VOLUME, true);
            if (fas == null) {
                return null;
            }

            ItemStack emptied = fas[0];
            ItemStack result;
            if (holding.stackSize > 1) {
                result = ItemStacks.copyWithSize(holding, holding.stackSize - 1);
                if (!ItemStacks.isEmpty(emptied) && !player.inventory.addItemStackToInventory(emptied)) {
                    player.dropPlayerItemWithRandomChoice(emptied, false);
                }
                player.inventoryContainer.detectAndSendChanges();
            } else {
                result = emptied;
            }
            player.inventory.setItemStack(result);
            ModularMachinery.NET_CHANNEL.sendTo(new PktInteractFluidTankGui(result), player);
            return null;
        }

        updateClientHand(pkt);
        return null;
    }
}
