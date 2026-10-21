package github.kasuminova.mmce.common.integration.ae2;

import github.kasuminova.mmce.common.block.appeng.BlockMEFluidInputBus;
import github.kasuminova.mmce.common.block.appeng.BlockMEFluidOutputBus;
import github.kasuminova.mmce.common.block.appeng.BlockMEItemInputBus;
import github.kasuminova.mmce.common.block.appeng.BlockMEItemOutputBus;
import github.kasuminova.mmce.common.block.appeng.BlockMEPatternMirrorImage;
import github.kasuminova.mmce.common.block.appeng.BlockMEPatternProvider;
import github.kasuminova.mmce.common.tile.MEFluidInputBus;
import github.kasuminova.mmce.common.tile.MEFluidOutputBus;
import github.kasuminova.mmce.common.tile.MEItemInputBus;
import github.kasuminova.mmce.common.tile.MEItemOutputBus;
import github.kasuminova.mmce.common.tile.MEPatternMirrorImage;
import github.kasuminova.mmce.common.tile.MEPatternProvider;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.lib.BlocksMM;
import hellfirepvp.modularmachinery.common.registry.RegistryBlocks;
import net.minecraft.tileentity.TileEntity;

/**
 * AE2 ME 部件的方块与方块实体注册（只在装有 AE2 时调用，本类及其引用的类不会在未装 AE2 时加载）。
 * 方块注册名与 1.12.2 相同（类名小写，如 blockmeiteminputbus），结构 JSON 中可直接使用。
 */
public final class AE2Registry {

    private AE2Registry() {
    }

    public static void registerBlocks() {
        BlocksMM.meItemOutputBus = RegistryBlocks.registerBlockWithItem(new BlockMEItemOutputBus());
        BlocksMM.meItemInputBus = RegistryBlocks.registerBlockWithItem(new BlockMEItemInputBus());
        BlocksMM.meFluidOutputBus = RegistryBlocks.registerBlockWithItem(new BlockMEFluidOutputBus());
        BlocksMM.meFluidInputBus = RegistryBlocks.registerBlockWithItem(new BlockMEFluidInputBus());
        BlocksMM.mePatternProvider = RegistryBlocks.registerBlockWithItem(new BlockMEPatternProvider());
        BlocksMM.mePatternMirrorImage = RegistryBlocks.registerBlockWithItem(new BlockMEPatternMirrorImage());
    }

    public static void registerTiles() {
        registerTile(MEItemOutputBus.class);
        registerTile(MEItemInputBus.class);
        registerTile(MEFluidOutputBus.class);
        registerTile(MEFluidInputBus.class);
        registerTile(MEPatternProvider.class);
        registerTile(MEPatternMirrorImage.class);
    }

    /** 方块实体 ID 同上游 registerTileWithModID：modularmachinery:类名小写。 */
    private static void registerTile(Class<? extends TileEntity> tile) {
        RegistryBlocks.registerTile(tile, ModularMachinery.MODID + ":" + tile.getSimpleName().toLowerCase());
    }
}
