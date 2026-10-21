package hellfirepvp.modularmachinery.common.network;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import ink.ikx.mmce.core.AssemblyEventHandler;
import io.netty.buffer.ByteBuf;

/**
 * 客户端 → 服务端：玩家开始世界投影时发送投影的机器与动态结构尺寸，投影清除时发送空机器名。
 * 投影期间对同一台机器使用自动组装时，服务端按此尺寸组装动态结构（对应 1.12.2 PktAutoAssemblyRequest 中的 dynamicPatternSize）。
 */
public class PktAssemblyPatternSize implements IMessage, IMessageHandler<PktAssemblyPatternSize, IMessage> {

    private String machine = "";
    private short  size    = 0;

    public PktAssemblyPatternSize() {
    }

    /** machine 为空字符串表示清除。 */
    public PktAssemblyPatternSize(String machine, int size) {
        this.machine = machine;
        this.size = (short) Math.max(0, Math.min(Short.MAX_VALUE, size));
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        machine = ByteBufUtils.readUTF8String(buf);
        size = buf.readShort();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, machine);
        buf.writeShort(size);
    }

    @Override
    public IMessage onMessage(PktAssemblyPatternSize message, MessageContext ctx) {
        AssemblyEventHandler.setProjectionPatternSize(ctx.getServerHandler().playerEntity, message.machine, message.size);
        return null;
    }
}
