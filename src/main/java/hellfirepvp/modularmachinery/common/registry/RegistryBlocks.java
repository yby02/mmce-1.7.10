/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.registry;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.kasuminova.mmce.common.integration.ae2.AE2Registry;
import github.kasuminova.mmce.compat.registry.IRegistryNamed;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.block.BlockCasing;
import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.block.BlockCustomName;
import hellfirepvp.modularmachinery.common.block.BlockDynamicColor;
import hellfirepvp.modularmachinery.common.block.BlockEnergyInputHatch;
import hellfirepvp.modularmachinery.common.block.BlockEnergyOutputHatch;
import hellfirepvp.modularmachinery.common.block.BlockFactoryController;
import hellfirepvp.modularmachinery.common.block.BlockFluidInputHatch;
import hellfirepvp.modularmachinery.common.block.BlockFluidOutputHatch;
import hellfirepvp.modularmachinery.common.block.BlockInputBus;
import hellfirepvp.modularmachinery.common.block.BlockMachineComponent;
import hellfirepvp.modularmachinery.common.block.BlockOutputBus;
import hellfirepvp.modularmachinery.common.block.BlockParallelController;
import hellfirepvp.modularmachinery.common.block.BlockSmartInterface;
import hellfirepvp.modularmachinery.common.block.BlockStatedMachineComponent;
import hellfirepvp.modularmachinery.common.block.BlockUpgradeBus;
import hellfirepvp.modularmachinery.common.data.Config;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.MachineBuilder;
import hellfirepvp.modularmachinery.common.item.ItemBlockController;
import hellfirepvp.modularmachinery.common.item.ItemBlockCustomName;
import hellfirepvp.modularmachinery.common.item.ItemBlockMachineComponent;
import hellfirepvp.modularmachinery.common.item.ItemBlockMachineComponentCustomName;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import hellfirepvp.modularmachinery.common.tiles.TileEnergyInputHatch;
import hellfirepvp.modularmachinery.common.tiles.TileEnergyOutputHatch;
import hellfirepvp.modularmachinery.common.tiles.TileFactoryController;
import hellfirepvp.modularmachinery.common.tiles.TileFluidInputHatch;
import hellfirepvp.modularmachinery.common.tiles.TileFluidOutputHatch;
import hellfirepvp.modularmachinery.common.tiles.TileItemInputBus;
import hellfirepvp.modularmachinery.common.tiles.TileItemOutputBus;
import hellfirepvp.modularmachinery.common.tiles.TileMachineController;
import hellfirepvp.modularmachinery.common.tiles.TileParallelController;
import hellfirepvp.modularmachinery.common.tiles.TileSmartInterface;
import hellfirepvp.modularmachinery.common.tiles.TileUpgradeBus;
import hellfirepvp.modularmachinery.common.tiles.base.TileColorableMachineComponent;
import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static hellfirepvp.modularmachinery.common.lib.BlocksMM.blockCasing;
import static hellfirepvp.modularmachinery.common.lib.BlocksMM.blockController;
import static hellfirepvp.modularmachinery.common.lib.BlocksMM.blockFactoryController;
import static hellfirepvp.modularmachinery.common.lib.BlocksMM.energyInputHatch;
import static hellfirepvp.modularmachinery.common.lib.BlocksMM.energyOutputHatch;
import static hellfirepvp.modularmachinery.common.lib.BlocksMM.fluidInputHatch;
import static hellfirepvp.modularmachinery.common.lib.BlocksMM.fluidOutputHatch;
import static hellfirepvp.modularmachinery.common.lib.BlocksMM.itemInputBus;
import static hellfirepvp.modularmachinery.common.lib.BlocksMM.itemOutputBus;
import static hellfirepvp.modularmachinery.common.lib.BlocksMM.parallelController;
import static hellfirepvp.modularmachinery.common.lib.BlocksMM.smartInterface;
import static hellfirepvp.modularmachinery.common.lib.BlocksMM.upgradeBus;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: RegistryBlocks
 * Created by HellFirePvP
 * Date: 28.06.2017 / 20:22
 * <p>
 * 1.7.10 移植：
 * - 方块在 preInit 中直接注册（见 InternalRegistryPrimer）；注册名沿用 1.12.2（类名小写），保证配置与结构 JSON 中的 ID 一致。
 * - 1.12.2 中为 ResourceLoader 生成每台机器控制器 blockstate 文件的逻辑不再需要（1.7.10 无 blockstate），已移除。
 * - AE2 ME 部件只在装有 AE2 时注册（见 AE2Registry）；Modular Magic 的仓只在对应模组存在时注册（见 ModularMagicBlocks）。
 */
public class RegistryBlocks {

    public static final  List<BlockDynamicColor> pendingIBlockColorBlocks = new LinkedList<>();
    private static final List<Block>             blockModelRegister       = new ArrayList<>();

    public static void initialize() {
        registerBlocks();
        registerTiles();
        registerBlockModels();
    }

    private static void registerBlocks() {
        blockController = prepareRegister(new BlockController());
        prepareItemBlockRegister(new ItemBlockController(blockController));
        blockFactoryController = prepareRegister(new BlockFactoryController());
        prepareItemBlockRegister(blockFactoryController);

        registerCustomControllers();

        registerExampleStatedMachineComponent();
        registerCustomStatedMachineComponent();

        blockCasing = prepareRegister(new BlockCasing());
        prepareItemBlockRegister(blockCasing);

        itemInputBus = prepareRegister(new BlockInputBus());
        prepareItemBlockRegister(itemInputBus);
        itemOutputBus = prepareRegister(new BlockOutputBus());
        prepareItemBlockRegister(itemOutputBus);
        fluidInputHatch = prepareRegister(new BlockFluidInputHatch());
        prepareItemBlockRegister(fluidInputHatch);
        fluidOutputHatch = prepareRegister(new BlockFluidOutputHatch());
        prepareItemBlockRegister(fluidOutputHatch);
        energyInputHatch = prepareRegister(new BlockEnergyInputHatch());
        prepareItemBlockRegister(energyInputHatch);
        energyOutputHatch = prepareRegister(new BlockEnergyOutputHatch());
        prepareItemBlockRegister(energyOutputHatch);
        smartInterface = prepareRegister(new BlockSmartInterface());
        prepareItemBlockRegister(smartInterface);
        parallelController = prepareRegister(new BlockParallelController());
        prepareItemBlockRegister(parallelController);
        upgradeBus = prepareRegister(new BlockUpgradeBus());
        prepareItemBlockRegister(upgradeBus);

        if (Mods.AE2.isPresent()) {
            AE2Registry.registerBlocks();
        }
        if (Mods.THAUMCRAFT.isPresent() || Mods.BOTANIA.isPresent() || Mods.BLOODMAGIC.isPresent()) {
            kport.modularmagic.common.lib.ModularMagicBlocks.registerBlocks();
        }
    }

    /** 以类名小写为注册名注册方块及其物品（供可选模组的方块使用）。 */
    public static <T extends Block> T registerBlockWithItem(T block) {
        prepareRegister(block);
        prepareItemBlockRegister(block);
        return block;
    }

    private static void registerTiles() {
        registerTile(TileColorableMachineComponent.class);

        registerTile(TileMachineController.class);
        registerTile(TileFactoryController.class);

        registerTile(TileFluidInputHatch.class);
        registerTile(TileFluidOutputHatch.class);
        registerTile(TileItemOutputBus.class);
        registerTile(TileItemInputBus.class);
        registerTile(TileEnergyInputHatch.class);
        registerTile(TileEnergyOutputHatch.class);

        registerTile(TileSmartInterface.class);
        registerTile(TileParallelController.class);
        registerTile(TileUpgradeBus.class);

        if (Mods.AE2.isPresent()) {
            AE2Registry.registerTiles();
        }
        if (Mods.THAUMCRAFT.isPresent() || Mods.BOTANIA.isPresent() || Mods.BLOODMAGIC.isPresent()) {
            kport.modularmagic.common.lib.ModularMagicBlocks.registerTiles();
        }
    }

    private static void registerExampleStatedMachineComponent() {
        BlockStatedMachineComponent block = new BlockStatedMachineComponent();
        IRegistryNamed.setName(block, ModularMachinery.MODID, "crushing_wheels");
        registerStatedMachineComponent(block);
    }

    private static void registerCustomStatedMachineComponent() {
        for (final BlockStatedMachineComponent block : BlockStatedMachineComponent.WAIT_FOR_REGISTRY) {
            registerStatedMachineComponent(block);
        }
        BlockStatedMachineComponent.WAIT_FOR_REGISTRY.clear();
    }

    public static void registerStatedMachineComponent(final BlockStatedMachineComponent block) {
        String name = IRegistryNamed.getName(block).getPath();
        block.setBlockName(ModularMachinery.MODID + '.' + name);
        prepareRegisterWithCustomName(block);
        ItemBlockCustomName itemBlock = new ItemBlockCustomName(block) {
            @Nonnull
            @Override
            @SideOnly(Side.CLIENT)
            public String getItemStackDisplayName(@Nonnull final ItemStack stack) {
                return block.getLocalizedName();
            }
        };
        IRegistryNamed.setName(itemBlock, ModularMachinery.MODID, name);
        prepareItemBlockRegisterWithCustomName(itemBlock);
    }

    private static void registerCustomControllers() {
        if (Config.onlyOneMachineController) {
            return;
        }

        List<DynamicMachine> waitForLoadMachines = MachineRegistry.getWaitForLoadMachines();
        for (MachineBuilder builder : MachineBuilder.PRE_LOAD_MACHINES.values()) {
            waitForLoadMachines.add(builder.getMachine());
        }
        MachineBuilder.PRE_LOAD_MACHINES.clear();

        if (Config.mocCompatibleMode) {
            for (DynamicMachine machine : waitForLoadMachines) {
                if (machine.isFactoryOnly()) {
                    continue;
                }

                BlockController ctrlBlock = prepareRegisterWithCustomName(new BlockController("modularcontroller", machine));
                BlockController.MOC_MACHINE_CONTROLLERS.put(machine, ctrlBlock);

                ItemBlockController ctrlBlockItem = new ItemBlockController(ctrlBlock);
                IRegistryNamed.NAMES.put(ctrlBlockItem, IRegistryNamed.getName(ctrlBlock));
                prepareItemBlockRegisterWithCustomName(ctrlBlockItem);
            }
        }

        for (DynamicMachine machine : waitForLoadMachines) {
            if (machine.isHasFactory() || Config.enableFactoryControllerByDefault) {
                BlockFactoryController factoryBlock = prepareRegisterWithCustomName(new BlockFactoryController(machine));
                BlockFactoryController.FACTORY_CONTROLLERS.put(machine, factoryBlock);

                ItemBlockController ctrlBlockItem = new ItemBlockController(factoryBlock);
                IRegistryNamed.NAMES.put(ctrlBlockItem, IRegistryNamed.getName(factoryBlock));
                prepareItemBlockRegisterWithCustomName(ctrlBlockItem);
            }

            if (machine.isFactoryOnly()) {
                continue;
            }

            BlockController ctrlBlock = prepareRegisterWithCustomName(new BlockController(machine));
            BlockController.MACHINE_CONTROLLERS.put(machine, ctrlBlock);

            ItemBlockController ctrlBlockItem = new ItemBlockController(ctrlBlock);
            IRegistryNamed.NAMES.put(ctrlBlockItem, IRegistryNamed.getName(ctrlBlock));
            prepareItemBlockRegisterWithCustomName(ctrlBlockItem);
        }
    }

    private static void registerBlockModels() {
        for (Block block : blockModelRegister) {
            ModularMachinery.proxy.registerBlockModel(block);
        }
    }

    public static void registerTile(Class<? extends TileEntity> tile, String name) {
        GameRegistry.registerTileEntity(tile, name);
    }

    private static void registerTile(Class<? extends TileEntity> tile) {
        // 与 1.12.2 相同的 TileEntity ID（类名小写）
        registerTile(tile, tile.getSimpleName().toLowerCase());
    }

    private static ItemBlock prepareItemBlockRegister(Block block) {
        if (block instanceof BlockMachineComponent) {
            if (block instanceof BlockCustomName) {
                return prepareItemBlockRegister(new ItemBlockMachineComponentCustomName(block));
            } else {
                return prepareItemBlockRegister(new ItemBlockMachineComponent(block));
            }
        } else {
            if (block instanceof BlockCustomName) {
                return prepareItemBlockRegister(new ItemBlockCustomName(block));
            } else {
                return prepareItemBlockRegister(new ItemBlock(block));
            }
        }
    }

    private static <T extends ItemBlock> T prepareItemBlockRegister(T item) {
        String name = item.field_150939_a.getClass().getSimpleName().toLowerCase();
        IRegistryNamed.setName(item, ModularMachinery.MODID, name);
        item.setUnlocalizedName(ModularMachinery.MODID + '.' + name);

        RegistryItems.ITEM_BLOCKS.add(item);
        return item;
    }

    private static <T extends ItemBlock> T prepareItemBlockRegisterWithCustomName(T item) {
        RegistryItems.ITEM_BLOCKS_WITH_CUSTOM_NAME.add(item);
        return item;
    }

    private static <T extends Block> T prepareRegister(T block) {
        String name = block.getClass().getSimpleName().toLowerCase();
        IRegistryNamed.setName(block, ModularMachinery.MODID, name);
        block.setBlockName(ModularMachinery.MODID + '.' + name);

        return prepareRegisterWithCustomName(block);
    }

    private static <T extends Block> T prepareRegisterWithCustomName(T block) {
        blockModelRegister.add(block);
        CommonProxy.registryPrimer.register(block);
        if (block instanceof BlockDynamicColor) {
            pendingIBlockColorBlocks.add((BlockDynamicColor) block);
        }
        return block;
    }
}
