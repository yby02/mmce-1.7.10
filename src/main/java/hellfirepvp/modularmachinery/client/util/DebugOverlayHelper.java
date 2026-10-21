/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.client.util;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: DebugOverlayHelper
 * Created by HellFirePvP
 * Date: 15.08.2017 / 15:20
 * <p>
 * 1.7.10 移植：F3 界面右侧显示准星所指方块的 meta（写机器 JSON 时用于确定 "modid:block@meta"）。
 */
public class DebugOverlayHelper {
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onTextOverlay(RenderGameOverlayEvent.Text event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings.showDebugInfo && event.right != null && mc.theWorld != null) {
            MovingObjectPosition hit = mc.objectMouseOver;
            if (hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                int meta = mc.theWorld.getBlockMetadata(hit.blockX, hit.blockY, hit.blockZ);
                event.right.add("");
                event.right.add("serialized as metadata: " + meta);
            }
        }
    }

}
