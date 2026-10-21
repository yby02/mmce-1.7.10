package hellfirepvp.modularmachinery.common.network;

import hellfirepvp.modularmachinery.common.container.ContainerSmartInterface;
import hellfirepvp.modularmachinery.common.tiles.TileSmartInterface;
import hellfirepvp.modularmachinery.common.util.SmartInterfaceData;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class PktSmartInterfaceUpdate implements IMessage, IMessageHandler<PktSmartInterfaceUpdate, IMessage> {
    private SmartInterfaceData newData = null;

    public PktSmartInterfaceUpdate() {
    }

    public PktSmartInterfaceUpdate(SmartInterfaceData newData) {
        this.newData = newData;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        NBTTagCompound tag = ByteBufUtils.readTag(buf);
        if (tag != null) {
            newData = SmartInterfaceData.deserialize(tag);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeTag(buf, newData.serialize());
    }

    @Override
    public IMessage onMessage(PktSmartInterfaceUpdate message, MessageContext ctx) {
        if (message == null) {
            return null;
        }

        EntityPlayerMP player = ctx.getServerHandler().playerEntity;
        if (!(player.openContainer instanceof ContainerSmartInterface)) {
            return null;
        }

        SmartInterfaceData newData = message.newData;
        TileSmartInterface owner = ((ContainerSmartInterface) player.openContainer).getOwner();
        TileSmartInterface.SmartInterfaceProvider provider = owner.provideComponent();
        SmartInterfaceData machineData = provider.getMachineData(newData.getPos());
        if (machineData != null) {
            machineData.setValue(newData.getValue());
            TileSmartInterface.onDataUpdate(owner, newData);
            owner.markForUpdateSync();
        }

        return null;
    }
}
