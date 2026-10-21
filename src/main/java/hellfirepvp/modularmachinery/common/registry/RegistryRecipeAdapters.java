/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.registry;

import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.crafting.adapter.AdapterMinecraftFurnace;
import hellfirepvp.modularmachinery.common.crafting.adapter.RecipeAdapter;
import hellfirepvp.modularmachinery.common.crafting.adapter.ic2.IC2RecipeAdapters;
import hellfirepvp.modularmachinery.common.crafting.adapter.tconstruct.TConstructRecipeAdapters;
import hellfirepvp.modularmachinery.common.crafting.adapter.te.TERecipeAdapters;
import hellfirepvp.modularmachinery.common.base.Mods;

import static hellfirepvp.modularmachinery.common.lib.RecipeAdaptersMM.MINECRAFT_FURNACE;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: RegistryRecipeAdapters
 * Created by HellFirePvP
 * Date: 13.07.2019 / 09:05
 */
public class RegistryRecipeAdapters {

    private RegistryRecipeAdapters() {
    }

    public static void initialize() {
        MINECRAFT_FURNACE = registerAdapter(new AdapterMinecraftFurnace());
        // 1.7.10 移植：按各模组 1.7.10 版本的配方 API 重写；注册入口放在单独的类里，未安装对应模组时不加载。
        // NuclearCraft Overhauled、Thaumcraft 6 没有 1.7.10 版本，对应适配器不移植。
        if (Mods.IC2.isPresent()) {
            IC2RecipeAdapters.register();
        }
        if (Mods.TCONSTRUCT.isPresent()) {
            TConstructRecipeAdapters.register();
        }
        if (Mods.THERMAL_EXPANSION.isPresent()) {
            TERecipeAdapters.register();
        }
    }

    public static <T extends RecipeAdapter> T registerAdapter(T adapter) {
        CommonProxy.registryPrimer.register(adapter);
        return adapter;
    }

}
