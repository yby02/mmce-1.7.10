/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.lib;

import github.kasuminova.mmce.common.block.appeng.BlockMEFluidInputBus;
import github.kasuminova.mmce.common.block.appeng.BlockMEFluidOutputBus;
import github.kasuminova.mmce.common.block.appeng.BlockMEItemInputBus;
import github.kasuminova.mmce.common.block.appeng.BlockMEItemOutputBus;
import github.kasuminova.mmce.common.block.appeng.BlockMEPatternMirrorImage;
import github.kasuminova.mmce.common.block.appeng.BlockMEPatternProvider;
import hellfirepvp.modularmachinery.common.block.BlockCasing;
import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.block.BlockEnergyInputHatch;
import hellfirepvp.modularmachinery.common.block.BlockEnergyOutputHatch;
import hellfirepvp.modularmachinery.common.block.BlockFactoryController;
import hellfirepvp.modularmachinery.common.block.BlockFluidInputHatch;
import hellfirepvp.modularmachinery.common.block.BlockFluidOutputHatch;
import hellfirepvp.modularmachinery.common.block.BlockInputBus;
import hellfirepvp.modularmachinery.common.block.BlockOutputBus;
import hellfirepvp.modularmachinery.common.block.BlockParallelController;
import hellfirepvp.modularmachinery.common.block.BlockSmartInterface;
import hellfirepvp.modularmachinery.common.block.BlockUpgradeBus;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: BlocksMM
 * Created by HellFirePvP
 * Date: 28.06.2017 / 20:22
 * <p>
 * 1.7.10 移植：AE2 ME 部件只在装有 AE2 时注册（未装时为 null）；气体总线依赖 MekEng（1.7.10 无对应模组），未移植。
 */
public class BlocksMM {

    public static BlockController        blockController;
    public static BlockFactoryController blockFactoryController;

    public static BlockCasing blockCasing;

    public static BlockInputBus          itemInputBus;
    public static BlockOutputBus         itemOutputBus;
    public static BlockFluidInputHatch   fluidInputHatch;
    public static BlockFluidOutputHatch  fluidOutputHatch;
    public static BlockEnergyInputHatch  energyInputHatch;
    public static BlockEnergyOutputHatch energyOutputHatch;

    public static BlockSmartInterface     smartInterface;
    public static BlockParallelController parallelController;
    public static BlockUpgradeBus         upgradeBus;

    public static BlockMEItemOutputBus      meItemOutputBus;
    public static BlockMEItemInputBus       meItemInputBus;
    public static BlockMEFluidOutputBus     meFluidOutputBus;
    public static BlockMEFluidInputBus      meFluidInputBus;
    public static BlockMEPatternProvider    mePatternProvider;
    public static BlockMEPatternMirrorImage mePatternMirrorImage;

}
