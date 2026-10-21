package kport.modularmagic.common.block;

import kport.modularmagic.common.tile.TileAspectProvider;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;

/** 源质输出仓（神秘时代 4）：源质管道从中抽走源质。 */
public class BlockAspectProviderOutput extends BlockMagicProvider {

    public BlockAspectProviderOutput() {
        super("aspectprovideroutput", TileAspectProvider.Output::new);
    }

    @Override
    protected void describe(TileEntity te, EntityPlayer player) {
        if (te instanceof TileAspectProvider p) {
            player.addChatMessage(new ChatComponentTranslation("message.modularmachinery.magic.aspect",
                p.getAspect() == null ? "-" : p.getAspect().getName(), p.getAmount(), p.getCapacity()));
        }
    }
}
