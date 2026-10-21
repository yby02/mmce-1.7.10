package github.kasuminova.mmce.common.network;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import github.kasuminova.mmce.common.container.MEGuiActionHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

/**
 * 客户端 → 服务端：ME 部件界面中的一次操作，交给玩家当前打开的容器（{@link MEGuiActionHandler}）处理。
 * 从 NEI 拖放时带一个载荷（物品或流体的 NBT），其余操作没有载荷。
 */
public class PktMEGuiAction implements IMessage, IMessageHandler<PktMEGuiAction, IMessage> {

    private int            action;
    private int            slot;
    private int            value;
    private NBTTagCompound payload;

    public PktMEGuiAction() {
    }

    public PktMEGuiAction(int action, int slot, int value) {
        this(action, slot, value, null);
    }

    public PktMEGuiAction(int action, int slot, int value, NBTTagCompound payload) {
        this.action = action;
        this.slot = slot;
        this.value = value;
        this.payload = payload;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        action = buf.readByte();
        slot = buf.readShort();
        value = buf.readInt();
        payload = buf.readBoolean() ? ByteBufUtils.readTag(buf) : null;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(action);
        buf.writeShort(slot);
        buf.writeInt(value);
        buf.writeBoolean(payload != null);
        if (payload != null) {
            ByteBufUtils.writeTag(buf, payload);
        }
    }

    @Override
    public IMessage onMessage(PktMEGuiAction message, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().playerEntity;
        if (player.openContainer instanceof MEGuiActionHandler handler) {
            handler.onGuiAction(player, message.action, message.slot, message.value, message.payload);
            player.openContainer.detectAndSendChanges();
        }
        return null;
    }
}
