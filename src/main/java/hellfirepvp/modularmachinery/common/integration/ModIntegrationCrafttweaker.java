/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.integration;

import github.kasuminova.mmce.common.concurrent.RecipeCraftingContextPool;
import github.kasuminova.mmce.common.upgrade.registry.RegistryUpgrade;
import github.kasuminova.mmce.common.util.concurrent.Action;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.crafting.RecipeRegistry;
import hellfirepvp.modularmachinery.common.crafting.adapter.RecipeAdapter;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.MachineBuilder;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.MachineModifier;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.event.MMEvents;
import hellfirepvp.modularmachinery.common.lib.RegistriesMM;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import hellfirepvp.modularmachinery.common.machine.factory.FactoryRecipeThread;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import hellfirepvp.modularmachinery.common.util.BlockArrayCache;
import hellfirepvp.modularmachinery.common.util.IBlockStateDescriptor;
import minetweaker.MineTweakerImplementationAPI;

import java.util.concurrent.CompletableFuture;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: ModIntegrationCrafttweaker
 * Created by HellFirePvP
 * Date: 18.08.2017 / 10:44
 * <p>
 * 1.7.10 移植：
 * - 1.12.2 中 CraftTweaker 在 init/postInit 阶段运行脚本，之后再由 ZenUtils 的 ScriptReloadEvent 处理热重载；
 * - 1.7.10 的 MineTweaker3 在服务器启动时（以及 /mt reload 时）运行脚本，并提供 onReloadEvent（运行前）与 onPostReload（运行后）。
 * 因此每次脚本运行都按 1.12.2 的“热重载”流程处理：运行前清理 CrT 注册的配方/机器/事件，运行后重新加载机器、修改器、事件与配方。
 * JSON 定义的机器与配方仍在 postInit 中加载（与 1.12.2 相同），并在每次重载时一并重新读取。
 */
public final class ModIntegrationCrafttweaker {

    private ModIntegrationCrafttweaker() {
    }

    public static void register() {
        MineTweakerImplementationAPI.onReloadEvent(event -> onScriptsReloading());
        MineTweakerImplementationAPI.onPostReload(event -> onScriptsReloaded());
    }

    private static void onScriptsReloading() {
        ModularMachinery.log.info("[ModularMachinery] Scripts reloading, clearing CraftTweaker defined content...");

        RecipeRegistry.getRegistry().clearLingeringRecipes();
        MachineBuilder.WAIT_FOR_LOAD.clear();

        RegistryUpgrade.clearAll();

        int cleared = RecipeRegistry.registeredRecipeCount();
        RecipeRegistry.getRegistry().clearAllRecipes();
        // Reset RecipeAdapterIncId
        RegistriesMM.ADAPTER_REGISTRY.getValuesCollection().forEach(RecipeAdapter::resetIncId);

        for (DynamicMachine loadedMachine : MachineRegistry.getLoadedMachines()) {
            loadedMachine.getMachineEventHandlers().clear();
            loadedMachine.getSmartInterfaceTypes().clear();
            loadedMachine.getCoreThreadPreset().clear();
            loadedMachine.getModifiers().clear();
            loadedMachine.getMultiBlockModifiers().clear();
        }
        // Clear Pool
        IBlockStateDescriptor.clearPool();
        BlockArray.BlockInformation.clearPool();
        // 重新读取 JSON 机器（首次运行时同样执行，以便与 1.12.2 一样在脚本运行前重置机器上的脚本内容）
        MachineRegistry.preloadMachines();
        MachineRegistry.reloadMachine(MachineRegistry.loadMachines(null));
        ModularMachinery.log.info("[ModularMachinery] Cleared {} recipes, {} machines loaded.", cleared, MachineRegistry.getLoadedMachines().size());
    }

    private static void onScriptsReloaded() {
        MachineRegistry.reloadMachine(MachineBuilder.WAIT_FOR_LOAD);

        CompletableFuture<Void> future = CompletableFuture.runAsync(() ->
            BlockArrayCache.buildCache(MachineRegistry.getLoadedMachines()));

        MachineModifier.loadAll();
        MMEvents.registryAll();

        // 不再调用 RecipeRegistry.reloadAdapters()：上游只在首次运行脚本后调用它（ZenUtils 重载时不调用）。这里重载开始时已清空配方、
        // 重置适配器编号，下面的 loadRecipeRegistry 会重新生成 JSON 配方与适配器配方；两者都调用会使每个适配器的配方注册两遍
        // （编号继续递增、名字不同，无法去重；MT3 每次服务器启动都会触发一次重载）。
        RecipeCraftingContextPool.onReload();
        RecipeRegistry.getRegistry().loadRecipeRegistry(null, true);
        for (Action action : FactoryRecipeThread.WAIT_FOR_ADD) {
            action.doAction();
        }
        FactoryRecipeThread.WAIT_FOR_ADD.clear();

        future.join();

        ModularMachinery.log.info("[ModularMachinery] Scripts loaded, {} recipes registered.", RecipeRegistry.registeredRecipeCount());
    }

}
