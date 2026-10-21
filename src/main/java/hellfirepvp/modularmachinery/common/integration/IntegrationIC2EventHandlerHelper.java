/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.integration;

import cpw.mods.fml.common.Optional;
import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergyTile;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.MinecraftForge;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: IntegrationIC2EventHandlerHelper
 * Created by HellFirePvP
 * Date: 17.08.2017 / 00:22
 * <p>
 * 1.7.10 移植：IC2 1.7.10 API 的能量网络注册事件，调用方须先检查 Mods.IC2。
 */
public class IntegrationIC2EventHandlerHelper {

    @Optional.Method(modid = "IC2")
    public static void onEnergyTileUnLoaded(TileEntity energyTile) {
        MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent((IEnergyTile) energyTile));
    }

    @Optional.Method(modid = "IC2")
    public static void onEnergyTileLoaded(TileEntity energyTile) {
        MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent((IEnergyTile) energyTile));
    }

}
