package kport.modularmagic.common.block;

import kport.modularmagic.common.tile.TileManaProvider;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;

/** 魔力输入仓（植物魔法）：魔力发射器对准它发射魔力。 */
public class BlockManaProviderInput extends BlockMagicProvider {

    public BlockManaProviderInput() {
        super("manaproviderinput", TileManaProvider.Input::new);
    }

    @Override
    protected void describe(TileEntity te, EntityPlayer player) {
        if (te instanceof TileManaProvider p) {
            player.addChatMessage(new ChatComponentTranslation("message.modularmachinery.magic.mana", p.getCurrentMana(), p.getManaCapacity()));
        }
    }
}
