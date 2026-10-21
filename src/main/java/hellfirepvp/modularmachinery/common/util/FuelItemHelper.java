/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.util;

import com.google.common.collect.ImmutableList;
import github.kasuminova.mmce.common.util.Sides;
import hellfirepvp.modularmachinery.ModularMachinery;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityFurnace;
import github.kasuminova.mmce.compat.NonNullList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: FuelItemHelper
 * Created by HellFirePvP
 * Date: 23.08.2017 / 16:43
 */
public class FuelItemHelper {

    private static List<ItemStack> knownFuelStacks = null;
    private static Future<Void>    future          = null;

    public static void initialize() {
        // 1.7.10 移植：Item#getCreativeTab / getSubItems 仅存在于客户端；燃料列表只用于配方展示。
        if (!Sides.isClient()) {
            knownFuelStacks = Collections.emptyList();
            return;
        }
        future = CompletableFuture.runAsync(() -> {
            NonNullList<ItemStack> stacks = NonNullList.create();
            for (Object obj : Item.itemRegistry) {
                Item item = (Item) obj;
                CreativeTabs tab = item.getCreativeTab();
                if (tab != null) {
                    // https://github.com/KasumiNova/ModularMachinery-Community-Edition/issues/32
                    try {
                        item.getSubItems(item, tab, stacks);
                    } catch (Throwable e) {
                        ModularMachinery.log.warn("[ModularMachinery] Exception when loading FuelItems!", e);
                    }
                }
            }
            List<ItemStack> out = new LinkedList<>();
            for (ItemStack stack : stacks) {
                try {
                    int burn = TileEntityFurnace.getItemBurnTime(stack); //Respects vanilla values.
                    if (burn > 0) {
                        out.add(stack);
                    }
                } catch (Throwable exc) {
                }
            }
            knownFuelStacks = ImmutableList.copyOf(out);
            future = null;
        });
    }

    public static List<ItemStack> getFuelItems() {
        if (future == null && knownFuelStacks == null) {
            return Collections.emptyList();
        }
        if (future != null && !future.isDone()) {
            try {
                future.get();
            } catch (Throwable e) {
                return new ArrayList<>(0);
            }
        }
        return knownFuelStacks == null ? Collections.emptyList() : knownFuelStacks;
    }

}
