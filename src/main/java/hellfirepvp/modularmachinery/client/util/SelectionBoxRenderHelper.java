/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.client.util;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import github.kasuminova.mmce.compat.BlockPos;
import github.kasuminova.mmce.compat.ItemStacks;
import hellfirepvp.modularmachinery.client.ClientProxy;
import hellfirepvp.modularmachinery.common.item.ItemConstructTool;
import hellfirepvp.modularmachinery.common.selection.PlayerStructureSelectionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: SelectionBoxRenderHelper
 * Created by HellFirePvP
 * Date: 22.08.2017 / 23:05
 * <p>
 * 1.7.10 移植：只有主手；断线清理事件在 FML 总线上（见 ClientProxy）。
 * 1.7.10 的右键方块事件被取消后，同一次右键还会再触发一次 RIGHT_CLICK_AIR（见 Minecraft#func_147121_ag），放置投影的那一 tick 一并取消。
 */
public class SelectionBoxRenderHelper {
    @SubscribeEvent
    public void onRenderLast(RenderWorldLastEvent event) {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player == null) {
            return;
        }

        ItemStack held = player.getCurrentEquippedItem();
        if (!ItemStacks.isEmpty(held) && held.getItem() instanceof ItemConstructTool) {
            PlayerStructureSelectionHelper.StructureSelection sel = PlayerStructureSelectionHelper.clientSelection;
            if (sel != null) {
                BlockPos playerPos = new BlockPos(player.posX, player.posY, player.posZ);
                List<BlockPos> toRender = sel.getSelectedPositions().stream()
                                             .filter((pos) -> pos.distanceSq(playerPos) <= 1024)
                                             .collect(Collectors.toList());
                RenderingUtils.drawWhiteOutlineCubes(toRender, event.partialTicks);
            }
        }

        ClientProxy.renderHelper.renderTranslucentBlocks(event.partialTicks);
    }

    @SubscribeEvent
    public void onRightClick(PlayerInteractEvent event) {
        if (!event.world.isRemote || event.entityPlayer != Minecraft.getMinecraft().thePlayer) {
            return;
        }
        if (event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK || event.action == PlayerInteractEvent.Action.RIGHT_CLICK_AIR) {
            if (ClientProxy.renderHelper.placePreview()
                || event.action == PlayerInteractEvent.Action.RIGHT_CLICK_AIR && ClientProxy.renderHelper.wasPlacedThisTick()) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void purgeDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        PlayerStructureSelectionHelper.clientSelection = null;
        ClientProxy.renderHelper.unloadWorld();
    }

    @SubscribeEvent
    public void onWorldChange(WorldEvent.Unload unload) {
        if (unload.world.isRemote) {
            ClientProxy.renderHelper.unloadWorld();
        }
    }

}
