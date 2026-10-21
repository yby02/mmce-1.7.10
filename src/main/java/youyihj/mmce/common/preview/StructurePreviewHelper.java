package youyihj.mmce.common.preview;

import com.google.common.base.Objects;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.kasuminova.mmce.compat.BlockPos;
import hellfirepvp.modularmachinery.client.ClientProxy;
import hellfirepvp.modularmachinery.client.util.BlockArrayPreviewRenderHelper;
import hellfirepvp.modularmachinery.client.util.DynamicMachineRenderContext;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.ChatComponentTranslation;

/**
 * 机械投影仪的世界投影：直接复用蓝图预览的 {@link BlockArrayPreviewRenderHelper}——开始投影后立即放置，
 * 此时准星正对着被右键的控制器，投影按控制器朝向旋转并对齐控制器。
 */
@SideOnly(Side.CLIENT)
public class StructurePreviewHelper {
    private static BlockPos renderPos = null;

    public static void renderMachinePreview(DynamicMachine machine, BlockPos pos) {
        BlockArrayPreviewRenderHelper renderHelper = ClientProxy.renderHelper;
        if (!Objects.equal(renderPos, pos)) {
            renderHelper.unloadWorld();
        }
        renderPos = pos;
        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        if (player != null) {
            player.addChatMessage(
                new ChatComponentTranslation("message.machine_projector.project", machine.getLocalizedName())
            );
        }
        DynamicMachineRenderContext context = DynamicMachineRenderContext.createContext(machine);
        context.snapSamples();
        renderHelper.startPreview(context);
        renderHelper.placePreview();
    }

    public static void reset() {
        renderPos = null;
    }
}
