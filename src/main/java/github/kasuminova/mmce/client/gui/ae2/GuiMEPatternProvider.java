package github.kasuminova.mmce.client.gui.ae2;

import github.kasuminova.mmce.common.container.ContainerMEPatternProvider;
import github.kasuminova.mmce.common.container.MEGuiActionHandler;
import github.kasuminova.mmce.common.tile.MEPatternProvider;
import github.kasuminova.mmce.common.util.InfItemFluidHandler;
import github.kasuminova.mmce.compat.ItemStacks;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * ME 机械样板供应器界面（256×196，布局、贴图与按钮同上游 GuiMEPatternProvider）：
 * 右侧列表显示缓存中的物品与流体（每行 3 个，滚轮翻页），“清空缓存”按钮，工作模式按钮（左键下一个、右键上一个），
 * 独立存储的流体槽（拿着流体容器点击倒入 / 装出）。
 */
public class GuiMEPatternProvider extends GuiMEBase<ContainerMEPatternProvider> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(ModularMachinery.MODID, "textures/gui/mepatternprovider.png");

    private static final int LIST_X = 180, LIST_Y = 27, LIST_W = 69, LIST_H = 126, PER_ROW = 3, ROWS = 7;
    private static final int RETURN_X = 215, MODE_X = 233, BUTTON_Y = 7;
    private static final int TIP_X = 240, TIP_Y = 157;

    private final MEPatternProvider provider;
    private int scrollRow = 0;

    public GuiMEPatternProvider(MEPatternProvider provider, EntityPlayer player) {
        super(new ContainerMEPatternProvider(provider, player));
        this.provider = provider;
        this.xSize = 256;
        this.ySize = 196;
    }

    public MEPatternProvider getOwner() {
        return provider;
    }

    /** 缓存内容：先物品后流体（输入隔离模式下包含每格样板的独立缓存）。 */
    private List<Object> entries() {
        List<Object> list = new ArrayList<>();
        List<InfItemFluidHandler> handlers = new ArrayList<>();
        handlers.add(provider.getInfHandler());
        for (MachineComponent<?> component : provider.getCombinationComponents()) {
            handlers.add((InfItemFluidHandler) component.getContainerProvider());
        }
        for (InfItemFluidHandler h : handlers) {
            for (ItemStack stack : h.getItemStackList()) {
                if (!ItemStacks.isEmpty(stack)) {
                    list.add(stack);
                }
            }
        }
        for (InfItemFluidHandler h : handlers) {
            for (FluidStack stack : h.getFluidStackList()) {
                if (stack != null && stack.amount > 0) {
                    list.add(stack);
                }
            }
        }
        return list;
    }

    private int maxScroll(int entryCount) {
        int rows = (entryCount + PER_ROW - 1) / PER_ROW;
        return Math.max(0, rows - ROWS);
    }

    private int hoveredEntry(int mouseX, int mouseY, int entryCount) {
        for (int i = 0; i < ROWS * PER_ROW; i++) {
            int index = scrollRow * PER_ROW + i;
            if (index >= entryCount) {
                break;
            }
            if (isHovering(mouseX, mouseY, LIST_X + 1 + 18 * (i % PER_ROW), LIST_Y + 1 + 18 * (i / PER_ROW), 16, 16)) {
                return index;
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

    private static int modeTextureX(MEPatternProvider.WorkModeSetting mode) {
        return switch (mode) {
            case DEFAULT -> 140;
            case BLOCKING_MODE -> 140 + 18;
            case CRAFTING_LOCK_MODE -> 140 + 36;
            case ENHANCED_BLOCKING_MODE -> 140 - 18;
            case ISOLATION_INPUT -> 140 - 36;
        };
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRendererObj.drawString(I18n.format("gui.mepatternprovider.title"), 7, 11, 0x404040);
        fontRendererObj.drawString(I18n.format("gui.mepatternprovider.cached"), 180, 11, 0x404040);
        fontRendererObj.drawString(I18n.format("gui.mepatternprovider.inventory"), 7, 101, 0x404040);
        fontRendererObj.drawString(I18n.format("gui.mepatternprovider.single_inv"), 180, 158, 0x404040);

        // 按钮
        GL11.glColor4f(1F, 1F, 1F, 1F);
        mc.getTextureManager().bindTexture(TEXTURE);
        boolean down = Mouse.isButtonDown(0);
        boolean overReturn = isHovering(mouseX, mouseY, RETURN_X, BUTTON_Y, 16, 16);
        drawTexturedModalRect(RETURN_X, BUTTON_Y, overReturn ? (down ? 176 + 36 : 176 + 18) : 176, 214, 16, 16);
        boolean overMode = isHovering(mouseX, mouseY, MODE_X, BUTTON_Y, 16, 16);
        drawTexturedModalRect(MODE_X, BUTTON_Y, overMode ? (down ? 140 + 90 : 140 + 72) : 140 + 54, 196, 16, 16);
        drawTexturedModalRect(MODE_X, BUTTON_Y, modeTextureX(provider.getWorkMode()), 196, 16, 16);
        boolean overTip = isHovering(mouseX, mouseY, TIP_X, TIP_Y, 9, 11);
        drawTexturedModalRect(TIP_X, TIP_Y, overTip ? 230 + 11 : 230, 214, 9, 11);

        // 独立存储的流体槽
        FluidStack subFluid = provider.getSubFluidHandler().getFluidInSlot(0);
        if (subFluid != null) {
            drawFluid(subFluid, ContainerMEPatternProvider.SUB_TANK_X, ContainerMEPatternProvider.SUB_TANK_Y, 16, 16, 1F);
        }
        if (isHovering(mouseX, mouseY, ContainerMEPatternProvider.SUB_TANK_X, ContainerMEPatternProvider.SUB_TANK_Y, 16, 16)) {
            drawHighlight(ContainerMEPatternProvider.SUB_TANK_X, ContainerMEPatternProvider.SUB_TANK_Y, 16, 16);
        }

        // 缓存列表
        List<Object> entries = entries();
        scrollRow = Math.min(scrollRow, maxScroll(entries.size()));
        for (int i = 0; i < ROWS * PER_ROW; i++) {
            int index = scrollRow * PER_ROW + i;
            if (index >= entries.size()) {
                break;
            }
            int x = LIST_X + 1 + 18 * (i % PER_ROW);
            int y = LIST_Y + 1 + 18 * (i / PER_ROW);
            Object entry = entries.get(index);
            if (entry instanceof ItemStack stack) {
                drawStack(stack, x, y, formatCount(stack.stackSize));
            } else if (entry instanceof FluidStack fluid) {
                drawFluid(fluid, x, y, 16, 16, 1F);
                drawAmount(formatCount(fluid.amount), x, y);
            }
        }
        int hovered = hoveredEntry(mouseX, mouseY, entries.size());
        if (hovered >= 0) {
            int i = hovered - scrollRow * PER_ROW;
            drawHighlight(LIST_X + 1 + 18 * (i % PER_ROW), LIST_Y + 1 + 18 * (i / PER_ROW), 16, 16);
        }
        // 滚动条
        int max = maxScroll(entries.size());
        if (max > 0) {
            int barH = Math.max(8, LIST_H * ROWS / (ROWS + max));
            int barY = LIST_Y + (LIST_H - barH) * scrollRow / max;
            drawRect(LIST_X + 58, barY, LIST_X + 64, barY + barH, 0xFF8B8B8B);
        }
    }

    @Override
    protected void renderHoveredToolTip(int mouseX, int mouseY) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        if (isHovering(mouseX, mouseY, RETURN_X, BUTTON_Y, 16, 16)) {
            List<String> lines = new ArrayList<>();
            lines.add(I18n.format("gui.mepatternprovider.return_items"));
            lines.add(EnumChatFormatting.GRAY + I18n.format("gui.mepatternprovider.return_items.desc"));
            drawTooltip(lines, mouseX, mouseY);
            return;
        }
        if (isHovering(mouseX, mouseY, MODE_X, BUTTON_Y, 16, 16)) {
            MEPatternProvider.WorkModeSetting current = provider.getWorkMode();
            List<String> lines = new ArrayList<>();
            lines.add(I18n.format("gui.mepatternprovider.work_mode.desc"));
            String[] keys = {"default", "blocking_mode", "crafting_lock_mode", "enhanced_blocking_mode", "isolation_input"};
            for (MEPatternProvider.WorkModeSetting mode : MEPatternProvider.WorkModeSetting.values()) {
                lines.add((mode == current ? I18n.format("gui.mepatternprovider.current") : "") + I18n.format("gui.mepatternprovider." + keys[mode.ordinal()] + ".desc"));
            }
            drawTooltip(lines, mouseX, mouseY);
            return;
        }
        if (isHovering(mouseX, mouseY, TIP_X, TIP_Y, 9, 11)) {
            drawTooltip(Collections.singletonList(I18n.format("gui.mepatternprovider.single_inv.desc")), mouseX, mouseY);
            return;
        }
        if (isHovering(mouseX, mouseY, ContainerMEPatternProvider.SUB_TANK_X, ContainerMEPatternProvider.SUB_TANK_Y, 16, 16)) {
            FluidStack fluid = provider.getSubFluidHandler().getFluidInSlot(0);
            List<String> lines = new ArrayList<>();
            lines.add(fluid == null ? I18n.format("tooltip.fluidhatch.empty") : fluid.getLocalizedName());
            if (fluid != null) {
                lines.add(EnumChatFormatting.GRAY + nf.format(fluid.amount) + " mB");
            }
            drawTooltip(lines, mouseX, mouseY);
            return;
        }
        List<Object> entries = entries();
        int hovered = hoveredEntry(mouseX, mouseY, entries.size());
        if (hovered >= 0) {
            Object entry = entries.get(hovered);
            if (entry instanceof ItemStack stack) {
                drawStackTooltip(stack, mouseX, mouseY, Collections.singletonList(
                    EnumChatFormatting.GRAY + I18n.format("gui.meitembus.item_cached", nf.format(stack.stackSize))));
            } else if (entry instanceof FluidStack fluid) {
                List<String> lines = new ArrayList<>();
                lines.add(fluid.getLocalizedName());
                lines.add(EnumChatFormatting.GRAY + nf.format(fluid.amount) + " mB");
                drawTooltip(lines, mouseX, mouseY);
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        if (isHovering(mouseX, mouseY, RETURN_X, BUTTON_Y, 16, 16) && button == 0) {
            sendAction(MEGuiActionHandler.ACTION_RETURN_ITEMS, 0, 0);
            return;
        }
        if (isHovering(mouseX, mouseY, MODE_X, BUTTON_Y, 16, 16) && (button == 0 || button == 1)) {
            int count = MEPatternProvider.WorkModeSetting.values().length;
            int next = (provider.getWorkMode().ordinal() + (button == 0 ? 1 : count - 1)) % count;
            sendAction(MEGuiActionHandler.ACTION_WORK_MODE, 0, next);
            return;
        }
        if (isHovering(mouseX, mouseY, ContainerMEPatternProvider.SUB_TANK_X, ContainerMEPatternProvider.SUB_TANK_Y, 16, 16)) {
            if (!ItemStacks.isEmpty(mc.thePlayer.inventory.getItemStack())) {
                sendAction(MEGuiActionHandler.ACTION_SUB_TANK, 0, button);
            }
            return;
        }
        super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) {
            return;
        }
        int mouseX = Mouse.getEventX() * width / mc.displayWidth;
        int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        if (isHovering(mouseX, mouseY, LIST_X, LIST_Y, LIST_W, LIST_H)) {
            scrollRow = Math.max(0, Math.min(maxScroll(entries().size()), scrollRow + (wheel > 0 ? -1 : 1)));
        }
    }
}
