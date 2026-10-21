package kport.modularmagic.common.lib;

import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.registry.RegistryBlocks;
import kport.modularmagic.common.block.BlockAspectProviderInput;
import kport.modularmagic.common.block.BlockAspectProviderOutput;
import kport.modularmagic.common.block.BlockLifeEssenceProviderInput;
import kport.modularmagic.common.block.BlockLifeEssenceProviderOutput;
import kport.modularmagic.common.block.BlockManaProviderInput;
import kport.modularmagic.common.block.BlockManaProviderOutput;
import kport.modularmagic.common.tile.TileAspectProvider;
import kport.modularmagic.common.tile.TileLifeEssenceProvider;
import kport.modularmagic.common.tile.TileManaProvider;
import net.minecraft.block.Block;

/**
 * Modular Magic 的方块（1.7.10 子集）。注册名同上游（类名小写：blockaspectproviderinput 等），方块实体 ID 同上游（modularmachinery:tileaspectproviderinput 等）。
 * 每组方块只在对应模组存在时注册（未装时字段为 null）。
 */
public final class ModularMagicBlocks {

    public static Block aspectProviderInput;
    public static Block aspectProviderOutput;
    public static Block manaProviderInput;
    public static Block manaProviderOutput;
    public static Block lifeEssenceProviderInput;
    public static Block lifeEssenceProviderOutput;

    private ModularMagicBlocks() {
    }

    public static boolean anyPresent() {
        return Mods.THAUMCRAFT.isPresent() || Mods.BOTANIA.isPresent() || Mods.BLOODMAGIC.isPresent();
    }

    public static void registerBlocks() {
        if (Mods.THAUMCRAFT.isPresent()) {
            aspectProviderInput = RegistryBlocks.registerBlockWithItem(new BlockAspectProviderInput());
            aspectProviderOutput = RegistryBlocks.registerBlockWithItem(new BlockAspectProviderOutput());
        }
        if (Mods.BOTANIA.isPresent()) {
            manaProviderInput = RegistryBlocks.registerBlockWithItem(new BlockManaProviderInput());
            manaProviderOutput = RegistryBlocks.registerBlockWithItem(new BlockManaProviderOutput());
        }
        if (Mods.BLOODMAGIC.isPresent()) {
            lifeEssenceProviderInput = RegistryBlocks.registerBlockWithItem(new BlockLifeEssenceProviderInput());
            lifeEssenceProviderOutput = RegistryBlocks.registerBlockWithItem(new BlockLifeEssenceProviderOutput());
        }
    }

    public static void registerTiles() {
        String prefix = ModularMachinery.MODID + ":";
        if (Mods.THAUMCRAFT.isPresent()) {
            RegistryBlocks.registerTile(TileAspectProvider.Input.class, prefix + "tileaspectproviderinput");
            RegistryBlocks.registerTile(TileAspectProvider.Output.class, prefix + "tileaspectprovideroutput");
        }
        if (Mods.BOTANIA.isPresent()) {
            RegistryBlocks.registerTile(TileManaProvider.Input.class, prefix + "tilemanainput");
            RegistryBlocks.registerTile(TileManaProvider.Output.class, prefix + "tilemanaoutput");
        }
        if (Mods.BLOODMAGIC.isPresent()) {
            RegistryBlocks.registerTile(TileLifeEssenceProvider.Input.class, prefix + "tilelifeessenceproviderinput");
            RegistryBlocks.registerTile(TileLifeEssenceProvider.Output.class, prefix + "tilelifeessenceprovideroutput");
        }
    }
}
