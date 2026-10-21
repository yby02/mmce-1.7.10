package kport.modularmagic.common.crafting.helper;

import kport.modularmagic.common.tile.TileLifeEssenceProvider;

public class LifeEssenceProviderCopy extends AmountProviderCopy<TileLifeEssenceProvider> {

    public LifeEssenceProviderCopy(TileLifeEssenceProvider original) {
        super(original, original.getLifeEssenceCache());
    }

    @Override
    public int getCapacity() {
        return original.getCacheCapacity();
    }
}
