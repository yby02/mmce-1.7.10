package hellfirepvp.modularmachinery.common.network;

import hellfirepvp.modularmachinery.common.container.ContainerParallelController;
import hellfirepvp.modularmachinery.common.tiles.TileParallelController;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class PktParallelControllerUpdate implements IMessage, IMessageHandler<PktParallelControllerUpdate, IMessage> {
    private int newParallelism = 0;

    public PktParallelControllerUpdate() {
    }

    public PktParallelControllerUpdate(int newParallelism) {
        this.newParallelism = newParallelism;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        newParallelism = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(newParallelism);
    }

    @Override
    public IMessage onMessage(PktParallelControllerUpdate message, MessageContext ctx) {
        if (message == null) {
            return null;
        }

        EntityPlayerMP player = ctx.getServerHandler().playerEntity;
        if (!(player.openContainer instanceof ContainerParallelController)) {
            return null;
        }

        int newParallelism = message.newParallelism;
        TileParallelController owner = ((ContainerParallelController) player.openContainer).getOwner();
        TileParallelController.ParallelControllerProvider provider = owner.provideComponent();
        if (provider.getMaxParallelism() >= newParallelism && newParallelism >= 0) {
            provider.setParallelism(newParallelism);
        }

        return null;
    }
}
