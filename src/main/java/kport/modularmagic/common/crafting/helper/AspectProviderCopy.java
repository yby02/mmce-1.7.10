package kport.modularmagic.common.crafting.helper;

import kport.modularmagic.common.tile.TileAspectProvider;
import thaumcraft.api.aspects.Aspect;

/** 源质仓的检查副本：只装一种源质，模拟取 / 放时同时检查源质种类。 */
public class AspectProviderCopy extends AmountProviderCopy<TileAspectProvider> {

    private Aspect aspect;

    public AspectProviderCopy(TileAspectProvider original) {
        super(original, original.getAmount());
        this.aspect = original.getAspect();
    }

    @Override
    public int getCapacity() {
        return original.getCapacity();
    }

    public int simulateTake(Aspect tag, int max) {
        if (aspect != tag) {
            return 0;
        }
        int n = simulateTake(max);
        if (amount == 0) {
            aspect = null;
        }
        return n;
    }

    public int simulateAdd(Aspect tag, int max) {
        if (aspect != null && aspect != tag) {
            return 0;
        }
        int n = simulateAdd(max);
        if (n > 0) {
            aspect = tag;
        }
        return n;
    }
}
