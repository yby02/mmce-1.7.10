/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common;

import ink.ikx.mmce.core.AssemblyEventHandler;

import hellfirepvp.modularmachinery.common.base.Mods;

import hellfirepvp.modularmachinery.common.integration.waila.ModIntegrationWaila;

import hellfirepvp.modularmachinery.common.registry.RegistryCraftingRecipes;

import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.IGuiHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import github.kasuminova.mmce.common.capability.CapabilityUpgrade;
import github.kasuminova.mmce.common.concurrent.TaskExecutor;
import github.kasuminova.mmce.common.handler.UpgradeEventHandler;
import github.kasuminova.mmce.common.util.concurrent.Action;
import github.kasuminova.mmce.common.world.MMWorldEventListener;
import github.kasuminova.mmce.compat.EventBuses;
import github.kasuminova.mmce.compat.MathHelper;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.container.ContainerController;
import hellfirepvp.modularmachinery.common.container.ContainerEnergyHatch;
import hellfirepvp.modularmachinery.common.container.ContainerFactoryController;
import hellfirepvp.modularmachinery.common.container.ContainerFluidHatch;
import hellfirepvp.modularmachinery.common.container.ContainerGroupInputConfig;
import hellfirepvp.modularmachinery.common.container.ContainerItemBus;
import hellfirepvp.modularmachinery.common.container.ContainerParallelController;
import hellfirepvp.modularmachinery.common.container.ContainerSmartInterface;
import hellfirepvp.modularmachinery.common.container.ContainerUpgradeBus;
import hellfirepvp.modularmachinery.common.crafting.IntegrationTypeHelper;
import hellfirepvp.modularmachinery.common.crafting.RecipeRegistry;
import hellfirepvp.modularmachinery.common.crafting.adapter.RecipeAdapterRegistry;
import hellfirepvp.modularmachinery.common.data.ModDataHolder;
import hellfirepvp.modularmachinery.common.dev.DevEnvironmentProbe;
import hellfirepvp.modularmachinery.common.integration.ModIntegrationCrafttweaker;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.MachineBuilder;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.MachineModifier;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.event.MMEvents;
import hellfirepvp.modularmachinery.common.lib.BlocksMM;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import hellfirepvp.modularmachinery.common.machine.factory.FactoryRecipeThread;
import hellfirepvp.modularmachinery.common.registry.RegistryRegistries;
import hellfirepvp.modularmachinery.common.registry.internal.InternalRegistryPrimer;
import hellfirepvp.modularmachinery.common.tiles.TileFactoryController;
import hellfirepvp.modularmachinery.common.tiles.TileMachineController;
import hellfirepvp.modularmachinery.common.tiles.TileParallelController;
import hellfirepvp.modularmachinery.common.tiles.TileSmartInterface;
import hellfirepvp.modularmachinery.common.tiles.TileUpgradeBus;
import hellfirepvp.modularmachinery.common.tiles.base.MachineGroupInput;
import hellfirepvp.modularmachinery.common.tiles.base.TileEnergyHatch;
import hellfirepvp.modularmachinery.common.tiles.base.TileFluidTank;
import hellfirepvp.modularmachinery.common.tiles.base.TileItemBus;
import hellfirepvp.modularmachinery.common.util.BlockArrayCache;
import hellfirepvp.modularmachinery.common.util.FuelItemHelper;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: CommonProxy
 * Created by HellFirePvP
 * Date: 26.06.2017 / 21:00
 * <p>
 * 1.7.10 移植：
 * - 1.12.2 在 RegistryEvent 中注册方块/物品/自定义类型；1.7.10 在 preInit 末尾（读取机器 JSON 之后）直接注册。
 * - 事件监听器通过 {@link EventBuses} 同时注册到 FML 与 Forge 两条总线。
 * - GUI 类型的序号与 1.12.2 保持一致；AE2 / Blood Magic 相关 GUI 随对应功能暂未移植，其枚举项保留以免序号错位。
 */
public class CommonProxy implements IGuiHandler {

    public static final ModDataHolder          dataHolder = new ModDataHolder();
    public static       CreativeTabs           creativeTabModularMachinery;
    public static       InternalRegistryPrimer registryPrimer;

    public CommonProxy() {
        registryPrimer = new InternalRegistryPrimer();
    }

    public static void loadModData(File configDir) {
        dataHolder.setup(configDir);
        if (dataHolder.requiresDefaultMachinery()) {
            dataHolder.copyDefaultMachinery();
        }
    }

    public void preInit() {
        creativeTabModularMachinery = new CreativeTabs(ModularMachinery.MODID) {
            @Override
            public Item getTabIconItem() {
                return Item.getItemFromBlock(BlocksMM.blockController);
            }
        };

        NetworkRegistry.INSTANCE.registerGuiHandler(ModularMachinery.instance, this);

        ModIntegrationCrafttweaker.register();

        MachineRegistry.preloadMachines();
        RegistryRegistries.initializeAll();

        CapabilityUpgrade.register();

        EventBuses.register(ModularMachinery.EXECUTE_MANAGER);
        EventBuses.register(AssemblyEventHandler.INSTANCE);
        EventBuses.register(new UpgradeEventHandler());
        EventBuses.register(MMWorldEventListener.INSTANCE);
        EventBuses.register(github.kasuminova.mmce.common.handler.SelectiveUpdateHandler.INSTANCE);
        EventBuses.register(github.kasuminova.mmce.common.handler.GroupInputConfigHandler.INSTANCE);

        ModularMachinery.EXECUTE_MANAGER.init();
        ModularMachinery.log.info(String.format("[ModularMachinery-CE] Parallel executor is ready (%s Threads), Let's get started!!!", TaskExecutor.THREAD_COUNT));
    }

    public void init() {
        RegistryCraftingRecipes.register();
        FuelItemHelper.initialize();
        IntegrationTypeHelper.filterModIdComponents();
        IntegrationTypeHelper.filterModIdRequirementTypes();
        if (Mods.WAILA.isPresent()) {
            ModIntegrationWaila.sendIMC();
        }
    }

    public void postInit() {
        MachineRegistry.registerMachines(MachineRegistry.loadMachines(null));
        MachineRegistry.registerMachines(MachineBuilder.WAIT_FOR_LOAD);

        MachineModifier.loadAll();
        MMEvents.registryAll();
        RecipeAdapterRegistry.registerDynamicMachineAdapters();

        RecipeRegistry.getRegistry().loadRecipeRegistry(null, true);
        for (Action action : FactoryRecipeThread.WAIT_FOR_ADD) {
            action.doAction();
        }
        FactoryRecipeThread.WAIT_FOR_ADD.clear();

        DevEnvironmentProbe.runIfDevEnvironment();
    }

    public void loadComplete() {
        CompletableFuture.runAsync(() -> BlockArrayCache.buildCache(MachineRegistry.getLoadedMachines()));
    }

    public void serverStarting(FMLServerStartingEvent event) {
    }

    public void registerBlockModel(Block block) {
    }

    public void registerItemModel(Item item) {
    }

    public void registerItemModelWithCustomName(Item item) {
    }

    @Nullable
    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        GuiType type = GuiType.values()[MathHelper.clamp(ID, 0, GuiType.values().length - 1)];
        Class<? extends TileEntity> required = type.requiredTileEntity;
        TileEntity present = null;
        if (required != null) {
            TileEntity te = world.getTileEntity(x, y, z);
            if (te != null && required.isAssignableFrom(te.getClass())) {
                present = te;
            } else {
                return null;
            }
        }

        switch (type) {
            case CONTROLLER -> {
                return new ContainerController((TileMachineController) present, player);
            }
            case FACTORY -> {
                return new ContainerFactoryController((TileFactoryController) present, player);
            }
            case BUS_INVENTORY -> {
                return new ContainerItemBus((TileItemBus) present, player);
            }
            case TANK_INVENTORY -> {
                return new ContainerFluidHatch((TileFluidTank) present, player);
            }
            case ENERGY_INVENTORY -> {
                return new ContainerEnergyHatch((TileEnergyHatch) present, player);
            }
            case SMART_INTERFACE -> {
                return new ContainerSmartInterface((TileSmartInterface) present, player);
            }
            case PARALLEL_CONTROLLER -> {
                return new ContainerParallelController((TileParallelController) present, player);
            }
            case UPGRADE_BUS -> {
                return new ContainerUpgradeBus((TileUpgradeBus) present, player);
            }
            case GUI_GROUP_INPUT_CONFIG -> {
                if (present instanceof MachineGroupInput m && m.canGroupInput()) {
                    return new ContainerGroupInputConfig(present, player);
                }
            }
            case ME_ITEM_OUTPUT_BUS, ME_ITEM_INPUT_BUS, ME_FLUID_OUTPUT_BUS, ME_FLUID_INPUT_BUS, ME_PATTERN_PROVIDER -> {
                if (Mods.AE2.isPresent()) {
                    return github.kasuminova.mmce.common.integration.ae2.AE2GuiFactory.createContainer(type, present, player);
                }
            }
            case GUI_ESSENCE_PROVIDER -> {
                if (Mods.BLOODMAGIC.isPresent() && present instanceof kport.modularmagic.common.tile.TileLifeEssenceProvider tile) {
                    return new kport.modularmagic.common.container.ContainerLifeEssence(tile, player);
                }
            }
            default -> {
            }
        }

        return null;
    }

    @Nullable
    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return null;
    }

    public enum GuiType {

        CONTROLLER(TileMachineController.class),
        FACTORY(TileFactoryController.class),
        BUS_INVENTORY(TileItemBus.class),
        TANK_INVENTORY(TileFluidTank.class),
        ENERGY_INVENTORY(TileEnergyHatch.class),
        SMART_INTERFACE(TileSmartInterface.class),
        PARALLEL_CONTROLLER(TileParallelController.class),
        UPGRADE_BUS(TileUpgradeBus.class),
        BLUEPRINT_PREVIEW(null),
        // ME 部件：只在装有 AE2 时有对应方块；这里写 TileEntity.class，具体类型由 AE2GuiFactory 判断（不在本类中引用 AE2 的类）。
        // 输出总线的“堆叠上限”界面与气体总线（MekEng）未移植，保留枚举项以保持序号与 1.12.2 一致
        ME_ITEM_OUTPUT_BUS(TileEntity.class),
        ME_ITEM_OUTPUT_BUS_STACK_SIZE(null),
        ME_ITEM_INPUT_BUS(TileEntity.class),
        ME_FLUID_OUTPUT_BUS(TileEntity.class),
        ME_FLUID_INPUT_BUS(TileEntity.class),
        ME_GAS_OUTPUT_BUS(null),
        ME_GAS_INPUT_BUS(null),
        ME_PATTERN_PROVIDER(TileEntity.class),
        // 生命精华仓（血魔法 1）
        GUI_ESSENCE_PROVIDER(TileEntity.class),
        GUI_GROUP_INPUT_CONFIG(TileEntity.class)
        ;

        public final Class<? extends TileEntity> requiredTileEntity;

        GuiType(@Nullable Class<? extends TileEntity> requiredTileEntity) {
            this.requiredTileEntity = requiredTileEntity;
        }
    }
}
