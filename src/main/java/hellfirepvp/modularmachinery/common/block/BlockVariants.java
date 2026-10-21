/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.block;

import github.kasuminova.mmce.compat.IBlockState;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: BlockVariants
 * Created by HellFirePvP
 * Date: 08.07.2017 / 14:59
 */
public interface BlockVariants {
    Iterable<IBlockState> getValidStates();

    String getBlockStateName(IBlockState state);

}
