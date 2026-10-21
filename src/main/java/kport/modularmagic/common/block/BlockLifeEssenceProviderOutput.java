package kport.modularmagic.common.block;

import kport.modularmagic.common.tile.TileLifeEssenceProvider;

/** 生命精华输出仓（血魔法 1）。 */
public class BlockLifeEssenceProviderOutput extends BlockLifeEssenceProvider {

    public BlockLifeEssenceProviderOutput() {
        super("lifeessenceprovideroutput", TileLifeEssenceProvider.Output::new);
    }
}
