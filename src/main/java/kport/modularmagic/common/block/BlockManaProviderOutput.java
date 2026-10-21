package kport.modularmagic.common.block;

import kport.modularmagic.common.tile.TileManaProvider;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;

/** 魔力输出仓（植物魔法）：登记为魔力池，相邻的魔力发射器、功能花等从中取用魔力。 */
public class BlockManaProviderOutput extends BlockMagicProvider {

    public BlockManaProviderOutput() {
        super("manaprovideroutput", TileManaProvider.Output::new);
    }

    @Override
    protected void describe(TileEntity te, EntityPlayer player) {
        if (te instanceof TileManaProvider p) {
            player.addChatMessage(new ChatComponentTranslation("message.modularmachinery.magic.mana", p.getCurrentMana(), p.getManaCapacity()));
        }
    }
}
