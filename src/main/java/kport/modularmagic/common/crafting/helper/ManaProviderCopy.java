package kport.modularmagic.common.crafting.helper;

import kport.modularmagic.common.tile.TileManaProvider;

public class ManaProviderCopy extends AmountProviderCopy<TileManaProvider> {

    public ManaProviderCopy(TileManaProvider original) {
        super(original, original.getCurrentMana());
    }

    @Override
    public int getCapacity() {
        return original.getManaCapacity();
    }
}
