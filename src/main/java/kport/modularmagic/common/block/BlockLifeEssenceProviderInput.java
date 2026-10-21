package kport.modularmagic.common.block;

import kport.modularmagic.common.tile.TileLifeEssenceProvider;

/** 生命精华输入仓（血魔法 1）。 */
public class BlockLifeEssenceProviderInput extends BlockLifeEssenceProvider {

    public BlockLifeEssenceProviderInput() {
        super("lifeessenceproviderinput", TileLifeEssenceProvider.Input::new);
    }
}
