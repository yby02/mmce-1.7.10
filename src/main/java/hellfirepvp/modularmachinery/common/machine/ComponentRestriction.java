package hellfirepvp.modularmachinery.common.machine;

import github.kasuminova.mmce.compat.Worlds;

import hellfirepvp.modularmachinery.common.util.BlockArray;
import github.kasuminova.mmce.compat.IBlockState;
import github.kasuminova.mmce.compat.BlockPos;
import net.minecraft.world.World;

import java.util.List;

public class ComponentRestriction {
    private final int                               minimumCount;
    private final int                               maximumCount;
    private final List<BlockArray.BlockInformation> matching;
    private       String                            restrictionDesc;

    public ComponentRestriction(final int minimumCount,
                                final int maximumCount,
                                final List<BlockArray.BlockInformation> matching,
                                final String restrictionDesc) {
        this.minimumCount = minimumCount;
        this.maximumCount = maximumCount;
        this.matching = matching;
        this.restrictionDesc = restrictionDesc;
    }

    public String getRestrictionDesc() {
        return restrictionDesc;
    }

    public void setRestrictionDesc(final String restrictionDesc) {
        this.restrictionDesc = restrictionDesc;
    }

    public boolean matches(final World world, final BlockPos pos) {
        if (Worlds.isBlockLoaded(world, pos)) {
            return false;
        }

        IBlockState state = Worlds.getBlockState(world, pos);

        int matchCount = 0;

        for (final BlockArray.BlockInformation information : matching) {
            if (information.matchesState(world, pos, state)) {
                matchCount++;
                if (matchCount > maximumCount) {
                    return false;
                }
            }
        }

        return matchCount >= minimumCount;
    }

    public int getMinimumCount() {
        return minimumCount;
    }

    public int getMaximumCount() {
        return maximumCount;
    }

    public List<BlockArray.BlockInformation> getMatching() {
        return matching;
    }
}
