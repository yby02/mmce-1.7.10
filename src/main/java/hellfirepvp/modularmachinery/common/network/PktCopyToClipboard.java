/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.nio.charset.StandardCharsets;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: PktCopyToClipboard
 * Created by HellFirePvP
 * Date: 19.08.2017 / 13:45
 */
public class PktCopyToClipboard implements IMessage, IMessageHandler<PktCopyToClipboard, IMessage> {

    private String strToCopy;

    public PktCopyToClipboard() {
    }

    public PktCopyToClipboard(String strToCopy) {
        this.strToCopy = strToCopy;
    }

    @SideOnly(Side.CLIENT)
    private static void handleCopy(String strToCopy) {
        // 1.7.10 移植：客户端包处理器本身在主线程执行
        if (Desktop.isDesktopSupported()) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(strToCopy), null);
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int length = buf.readInt();
        byte[] strBytes = new byte[length];
        buf.readBytes(strBytes, 0, length);
        strToCopy = new String(strBytes, StandardCharsets.UTF_8);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        byte[] strBytes = strToCopy.getBytes(StandardCharsets.UTF_8);
        buf.writeInt(strBytes.length);
        buf.writeBytes(strBytes);
    }

    @Override
    public IMessage onMessage(PktCopyToClipboard message, MessageContext ctx) {
        handleCopy(message.strToCopy);
        return null;
    }
}
