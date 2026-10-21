package github.kasuminova.mmce.client.gui.ae2;

import github.kasuminova.mmce.common.container.ContainerMEItemOutputBus;
import github.kasuminova.mmce.common.tile.MEItemOutputBus;
import github.kasuminova.mmce.compat.ItemStacks;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.text.NumberFormat;
import java.util.Collections;
import java.util.Locale;

/** ME 机械物品输出总线界面：同上游，背景借用 AE2 的 ME 箱子贴图（skychest.png），4×9 缓存只显示。 */
public class GuiMEItemOutputBus extends GuiMEBase<ContainerMEItemOutputBus> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("appliedenergistics2", "textures/guis/skychest.png");

    private final MEItemOutputBus bus;

    public GuiMEItemOutputBus(MEItemOutputBus bus, EntityPlayer player) {
        super(new ContainerMEItemOutputBus(bus, player));
        this.bus = bus;
        this.xSize = 176;
        this.ySize = 195;
    }

    private int hoveredSlot(int mouseX, int mouseY) {
        for (int i = 0; i < MEItemOutputBus.SLOTS; i++) {
            if (isHovering(mouseX, mouseY, ContainerMEItemOutputBus.SLOTS_X + 18 * (i % 9), ContainerMEItemOutputBus.SLOTS_Y + 18 * (i / 9), 16, 16)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1F, 1F, 1F, 1F);
        mc.getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRendererObj.drawString(I18n.format("gui.meitemoutputbus.title"), 8, 8, 0x404040);
        fontRendererObj.drawString(I18n.format("container.inventory"), 8, ySize - 96 + 2, 0x404040);
        for (int i = 0; i < MEItemOutputBus.SLOTS; i++) {
            ItemStack stack = bus.getInternalInventory().getStackInSlot(i);
            drawStack(stack, ContainerMEItemOutputBus.SLOTS_X + 18 * (i % 9), ContainerMEItemOutputBus.SLOTS_Y + 18 * (i / 9),
                ItemStacks.isEmpty(stack) ? null : formatCount(stack.stackSize));
        }
        int hovered = hoveredSlot(mouseX, mouseY);
        if (hovered >= 0) {
            drawHighlight(ContainerMEItemOutputBus.SLOTS_X + 18 * (hovered % 9), ContainerMEItemOutputBus.SLOTS_Y + 18 * (hovered / 9), 16, 16);
        }
    }

    @Override
    protected void renderHoveredToolTip(int mouseX, int mouseY) {
        int hovered = hoveredSlot(mouseX, mouseY);
        if (hovered < 0) {
            return;
        }
        ItemStack stack = bus.getInternalInventory().getStackInSlot(hovered);
        if (!ItemStacks.isEmpty(stack)) {
            drawStackTooltip(stack, mouseX, mouseY, Collections.singletonList(
                EnumChatFormatting.GRAY + I18n.format("gui.meitembus.item_cached", NumberFormat.getNumberInstance(Locale.US).format(stack.stackSize))));
        }
    }
}
