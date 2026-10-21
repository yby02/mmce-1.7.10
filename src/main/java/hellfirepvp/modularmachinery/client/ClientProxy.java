/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.client;

import hellfirepvp.modularmachinery.common.base.Mods;

import cpw.mods.fml.common.FMLCommonHandler;
import github.kasuminova.mmce.compat.MathHelper;
import hellfirepvp.modularmachinery.client.dev.DevClientAutomation;
import hellfirepvp.modularmachinery.client.gui.GuiContainerEnergyHatch;
import hellfirepvp.modularmachinery.client.gui.GuiContainerFluidHatch;
import hellfirepvp.modularmachinery.client.gui.GuiContainerGroupInputConfig;
import hellfirepvp.modularmachinery.client.gui.GuiContainerItemBus;
import hellfirepvp.modularmachinery.client.gui.GuiContainerParallelController;
import hellfirepvp.modularmachinery.client.gui.GuiContainerSmartInterface;
import hellfirepvp.modularmachinery.client.gui.GuiContainerUpgradeBus;
import hellfirepvp.modularmachinery.client.gui.GuiFactoryController;
import hellfirepvp.modularmachinery.client.gui.GuiMachineController;
import hellfirepvp.modularmachinery.client.gui.GuiScreenBlueprint;
import hellfirepvp.modularmachinery.client.render.MachineComponentRenderer;
import hellfirepvp.modularmachinery.client.util.BlockArrayPreviewRenderHelper;
import hellfirepvp.modularmachinery.client.util.DebugOverlayHelper;
import hellfirepvp.modularmachinery.client.util.ControllerOwnerTooltip;
import hellfirepvp.modularmachinery.client.util.SelectionBoxRenderHelper;
import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.item.ItemBlueprint;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.tiles.TileFactoryController;
import hellfirepvp.modularmachinery.common.tiles.TileMachineController;
import hellfirepvp.modularmachinery.common.tiles.TileParallelController;
import hellfirepvp.modularmachinery.common.tiles.TileSmartInterface;
import hellfirepvp.modularmachinery.common.tiles.TileUpgradeBus;
import hellfirepvp.modularmachinery.common.tiles.base.MachineGroupInput;
import hellfirepvp.modularmachinery.common.tiles.base.TileEnergyHatch;
import hellfirepvp.modularmachinery.common.tiles.base.TileFluidTank;
import hellfirepvp.modularmachinery.common.tiles.base.TileItemBus;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: ClientProxy
 * Created by HellFirePvP
 * Date: 26.06.2017 / 21:01
 * <p>
 * 1.7.10 移植：
 * - 方块模型 / 颜色处理器 → ISBRH（{@link MachineComponentRenderer}）与 Block#colorMultiplier；
 * - TickEvent、网络事件在 FML 总线，渲染与界面事件在 Forge 总线；
 * - 蓝图预览界面（{@link GuiScreenBlueprint}）与世界投影（{@link BlockArrayPreviewRenderHelper}）在 P8 移植，GeckoLib 控制器模型不移植。
 */
public class ClientProxy extends CommonProxy {

    public static final ClientScheduler               clientScheduler = new ClientScheduler();
    public static final BlockArrayPreviewRenderHelper renderHelper    = new BlockArrayPreviewRenderHelper();

    @Override
    public void preInit() {
        MachineComponentRenderer.register();

        FMLCommonHandler.instance().bus().register(clientScheduler);
        SelectionBoxRenderHelper selectionBoxRenderHelper = new SelectionBoxRenderHelper();
        MinecraftForge.EVENT_BUS.register(selectionBoxRenderHelper);
        FMLCommonHandler.instance().bus().register(selectionBoxRenderHelper);
        MinecraftForge.EVENT_BUS.register(new DebugOverlayHelper());
        MinecraftForge.EVENT_BUS.register(new ControllerOwnerTooltip());

        super.preInit();
    }

    @Override
    public void init() {
        super.init();
        DevClientAutomation.registerIfRequested();
    }

    @Nullable
    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
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
                return new GuiMachineController((TileMachineController) present, player);
            }
            case FACTORY -> {
                return new GuiFactoryController((TileFactoryController) present, player);
            }
            case BUS_INVENTORY -> {
                return new GuiContainerItemBus((TileItemBus) present, player);
            }
            case TANK_INVENTORY -> {
                return new GuiContainerFluidHatch((TileFluidTank) present, player);
            }
            case ENERGY_INVENTORY -> {
                return new GuiContainerEnergyHatch((TileEnergyHatch) present, player);
            }
            case SMART_INTERFACE -> {
                return new GuiContainerSmartInterface((TileSmartInterface) present, player);
            }
            case PARALLEL_CONTROLLER -> {
                return new GuiContainerParallelController((TileParallelController) present, player);
            }
            case UPGRADE_BUS -> {
                return new GuiContainerUpgradeBus((TileUpgradeBus) present, player);
            }
            case GUI_GROUP_INPUT_CONFIG -> {
                if (present instanceof MachineGroupInput m && m.canGroupInput()) {
                    return new GuiContainerGroupInputConfig(present, player);
                }
            }
            case BLUEPRINT_PREVIEW -> {
                // 1.7.10 没有副手，x 恒为 0（主手）
                DynamicMachine machine = ItemBlueprint.getAssociatedMachine(player.getCurrentEquippedItem());
                if (machine != null) {
                    return new GuiScreenBlueprint(machine);
                }
            }
            case ME_ITEM_OUTPUT_BUS, ME_ITEM_INPUT_BUS, ME_FLUID_OUTPUT_BUS, ME_FLUID_INPUT_BUS, ME_PATTERN_PROVIDER -> {
                if (Mods.AE2.isPresent()) {
                    return github.kasuminova.mmce.client.gui.ae2.AE2ClientGuiFactory.createGui(type, present, player);
                }
            }
            case GUI_ESSENCE_PROVIDER -> {
                if (Mods.BLOODMAGIC.isPresent() && present instanceof kport.modularmagic.common.tile.TileLifeEssenceProvider tile) {
                    return new kport.modularmagic.client.gui.GuiContainerLifeEssence(tile, player);
                }
            }
            default -> {
                // 气体总线：不移植
            }
        }
        return null;
    }
}
