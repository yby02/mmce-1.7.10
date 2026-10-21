/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.client.gui;

import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.client.ClientProxy;
import hellfirepvp.modularmachinery.client.ClientScheduler;
import hellfirepvp.modularmachinery.client.util.DynamicMachineRenderContext;
import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.block.BlockFactoryController;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: GuiScreenBlueprint
 * Created by HellFirePvP
 * Date: 09.07.2017 / 21:08
 * <p>
 * 1.7.10 移植：上游基于自研 Widget 框架（GuiScreenDynamic / PreviewPanels / WorldSceneRenderer），1.7.10 改为普通 GuiScreen，
 * 沿用 guiblueprint_new.png 的布局与按钮贴图：
 * <ul>
 *   <li>3D 预览：左键拖动旋转、滚轮缩放、右键复位；分层模式俯视并可逐层切换；</li>
 *   <li>下方按钮：动态结构尺寸 + / −（仅有动态结构的机器）、投影到世界、替选方块轮换开关、3D / 分层切换；</li>
 *   <li>底部为所需方块清单（含数量，滚轮翻页）。</li>
 * </ul>
 * 打开另一台机器的蓝图预览时清除世界中已有的投影。
 */
public class GuiScreenBlueprint extends GuiScreen {
    public static final ResourceLocation GUI_TEXTURE =
        new ResourceLocation(ModularMachinery.MODID, "textures/gui/guiblueprint_new.png");

    public static final int X_SIZE = 184;
    public static final int Y_SIZE = 220;

    static final int VIEW_X = 6, VIEW_Y = 26, VIEW_W = 172, VIEW_H = 150;
    static final int LIST_X = 5, LIST_Y = 179, LIST_COLUMNS = 9, LIST_ROWS = 2;

    public static final int BTN_DYNAMIC_PLUS     = 0;
    public static final int BTN_DYNAMIC_SUBTRACT = 1;
    public static final int BTN_PLACE_WORLD      = 2;
    public static final int BTN_CYCLE            = 3;
    public static final int BTN_LAYER_MODE       = 4;
    public static final int BTN_LAYER_UP         = 5;
    public static final int BTN_LAYER_DOWN       = 6;

    private final DynamicMachine machine;
    private final boolean        hasDynamicPattern;
    private final ItemStack      controllerStack;

    private DynamicMachineRenderContext context;
    private int guiLeft, guiTop;

    private TexturedButton cycleButton, layerModeButton, layerUp, layerDown, dynamicPlus, dynamicSubtract;

    private boolean dragging = false;
    private int     lastMouseX, lastMouseY;
    private int     listScroll = 0;

    private List<ItemStack> ingredients    = Collections.emptyList();
    private long            ingredientsKey = Long.MIN_VALUE;

    public GuiScreenBlueprint(DynamicMachine machine) {
        this.machine = machine;
        this.hasDynamicPattern = !machine.getDynamicPatterns().isEmpty();
        this.context = DynamicMachineRenderContext.createContext(machine);
        BlockController controller = BlockController.getControllerWithMachine(machine);
        if (controller == null) {
            controller = BlockFactoryController.getControllerWithMachine(machine);
        }
        Item item = controller == null ? null : Item.getItemFromBlock(controller);
        this.controllerStack = item == null ? null : new ItemStack(item);
        ClientProxy.renderHelper.clearIfOtherMachine(machine);
    }

    @Override
    public void initGui() {
        this.guiLeft = (this.width - X_SIZE) / 2;
        this.guiTop = (this.height - Y_SIZE) / 2;
        buttonList.clear();

        // 下方按钮行，右对齐（与上游 bottomMenu 相同的顺序与贴图）
        List<TexturedButton> bottom = new ArrayList<>();
        if (hasDynamicPattern) {
            dynamicPlus = new TexturedButton(BTN_DYNAMIC_PLUS, 13, 184, 60, 15, 2, -1, () -> Collections.singletonList(
                I18n.format("gui.preview.button.dynamic_pattern_plus.tip", context.getDynamicPatternSize())));
            dynamicSubtract = new TexturedButton(BTN_DYNAMIC_SUBTRACT, 13, 184, 75, 15, 2, -1, () -> Collections.singletonList(
                I18n.format("gui.preview.button.dynamic_pattern_subtract.tip", context.getDynamicPatternSize())));
            bottom.add(dynamicPlus);
            bottom.add(dynamicSubtract);
        }
        bottom.add(new TexturedButton(BTN_PLACE_WORLD, 13, 184, 45, 15, 2, -1,
            () -> Collections.singletonList(I18n.format("gui.preview.button.place_world_preview.tip"))));
        cycleButton = new TexturedButton(BTN_CYCLE, 13, 184, 105, 15, 2, -1, () -> cycleButton.toggled
            ? Collections.singletonList(I18n.format("gui.preview.button.disable_cycle_replaceable_blocks.tip"))
            : Arrays.asList(I18n.format("gui.preview.button.enable_cycle_replaceable_blocks.tip.0"),
            I18n.format("gui.preview.button.enable_cycle_replaceable_blocks.tip.1")));
        cycleButton.toggled = context.getShiftSnap() == -1;
        bottom.add(cycleButton);
        layerModeButton = new TexturedButton(BTN_LAYER_MODE, 13, 184, 30, 15, 3, -1, () -> Collections.singletonList(layerModeButton.toggled
            ? I18n.format("gui.preview.button.toggle_3d_render.tip")
            : I18n.format("gui.preview.button.toggle_layer_render.tip")));
        bottom.add(layerModeButton);
        int x = guiLeft + X_SIZE - 6 - (bottom.size() * 15 - 2);
        for (TexturedButton button : bottom) {
            button.xPosition = x;
            button.yPosition = guiTop + 161;
            buttonList.add(button);
            x += 15;
        }

        // 分层模式的上 / 下一层按钮（右上角）
        layerUp = new TexturedButton(BTN_LAYER_UP, 9, 184, 121, 11, 2, 3,
            () -> Collections.singletonList(I18n.format("gui.preview.button.layer_render_scrollbar.up.tip")));
        layerDown = new TexturedButton(BTN_LAYER_DOWN, 9, 184, 132, 11, 2, 3,
            () -> Collections.singletonList(I18n.format("gui.preview.button.layer_render_scrollbar.down.tip")));
        layerUp.xPosition = layerDown.xPosition = guiLeft + X_SIZE - 6 - 9 - 2;
        layerUp.yPosition = guiTop + VIEW_Y + 3;
        layerDown.yPosition = guiTop + VIEW_Y + 14;
        buttonList.add(layerUp);
        buttonList.add(layerDown);
        updateButtons();
    }

    private void updateButtons() {
        boolean layered = !context.doesRenderIn3D();
        layerModeButton.toggled = layered;
        cycleButton.toggled = context.getShiftSnap() == -1;
        layerUp.visible = layerDown.visible = layered;
        layerUp.enabled = context.hasSliceUp();
        layerDown.enabled = context.hasSliceDown();
        if (hasDynamicPattern) {
            dynamicPlus.enabled = context.getDynamicPatternSize() < DynamicMachineRenderContext.maxDynamicPatternSize(machine);
            dynamicSubtract.enabled = context.getDynamicPatternSize() > DynamicMachineRenderContext.minDynamicPatternSize(machine);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    public DynamicMachine getMachine() {
        return machine;
    }

    public DynamicMachineRenderContext getContext() {
        return context;
    }

    /** 按钮（id 见 BTN_* 常量）；不存在时返回 null。 */
    public GuiButton getButton(int id) {
        for (Object o : buttonList) {
            if (((GuiButton) o).id == id) {
                return (GuiButton) o;
            }
        }
        return null;
    }

    // ---------------- 操作（按钮与外部调用共用） ----------------

    /** 按当前预览（含动态结构尺寸与替选方块取样）开始世界投影并关闭界面。 */
    public void placeWorldPreview() {
        DynamicMachineRenderContext renderContext = DynamicMachineRenderContext.createContext(machine, context.getDynamicPatternSize());
        renderContext.setShiftSnap(context.getShiftSnap() == -1 ? ClientScheduler.getClientTick() : context.getShiftSnap());
        ClientProxy.renderHelper.startPreview(renderContext);
        mc.displayGuiScreen(null);
    }

    /** 设置动态结构尺寸（限制在各动态结构的最小 / 最大尺寸之间），保留当前视角。 */
    public void setDynamicPatternSize(int size) {
        int min = DynamicMachineRenderContext.minDynamicPatternSize(machine);
        int max = DynamicMachineRenderContext.maxDynamicPatternSize(machine);
        int clamped = Math.max(min, Math.min(max, size));
        if (clamped == context.getDynamicPatternSize()) {
            return;
        }
        DynamicMachineRenderContext newContext = DynamicMachineRenderContext.createContext(machine, clamped);
        newContext.copyViewFrom(context);
        context = newContext;
        ingredientsKey = Long.MIN_VALUE;
        listScroll = 0;
        if (layerModeButton != null) {
            updateButtons();
        }
    }

    public void toggleLayerMode() {
        if (context.doesRenderIn3D()) {
            context.setTo2D();
        } else {
            context.setTo3D();
        }
        updateButtons();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case BTN_DYNAMIC_PLUS -> setDynamicPatternSize(context.getDynamicPatternSize() + 1);
            case BTN_DYNAMIC_SUBTRACT -> setDynamicPatternSize(context.getDynamicPatternSize() - 1);
            case BTN_PLACE_WORLD -> placeWorldPreview();
            case BTN_CYCLE -> {
                if (context.getShiftSnap() == -1) {
                    context.snapSamples();
                } else {
                    context.releaseSamples();
                }
                ingredientsKey = Long.MIN_VALUE;
            }
            case BTN_LAYER_MODE -> toggleLayerMode();
            case BTN_LAYER_UP -> context.sliceUp();
            case BTN_LAYER_DOWN -> context.sliceDown();
            default -> {
            }
        }
        if (mc.currentScreen == this) {
            updateButtons();
        }
    }

    // ---------------- 输入 ----------------

    private boolean inView(int mouseX, int mouseY) {
        return mouseX >= guiLeft + VIEW_X && mouseX < guiLeft + VIEW_X + VIEW_W && mouseY >= guiTop + VIEW_Y && mouseY < guiTop + 158;
    }

    private boolean inList(int mouseX, int mouseY) {
        return mouseX >= guiLeft + LIST_X && mouseX < guiLeft + LIST_X + 174 && mouseY >= guiTop + LIST_Y && mouseY < guiTop + LIST_Y + 36;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        for (Object o : buttonList) {
            GuiButton b = (GuiButton) o;
            if (b.visible && b.mousePressed(mc, mouseX, mouseY)) {
                return;
            }
        }
        if (inView(mouseX, mouseY)) {
            if (mouseButton == 0) {
                dragging = true;
                lastMouseX = mouseX;
                lastMouseY = mouseY;
            } else if (mouseButton == 1) {
                context.resetRotation();
            }
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (dragging && clickedMouseButton == 0) {
            context.rotateRender((mouseX - lastMouseX) * 1.5F, (mouseY - lastMouseY) * 1.5F);
            lastMouseX = mouseX;
            lastMouseY = mouseY;
        }
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int state) {
        super.mouseMovedOrUp(mouseX, mouseY, state);
        if (state == 0) {
            dragging = false;
        }
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) {
            return;
        }
        int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        if (inView(mouseX, mouseY)) {
            if (wheel > 0) {
                context.zoomIn();
            } else {
                context.zoomOut();
            }
        } else if (inList(mouseX, mouseY)) {
            int maxScroll = Math.max(0, (ingredients.size() + LIST_COLUMNS - 1) / LIST_COLUMNS - LIST_ROWS);
            listScroll = Math.max(0, Math.min(maxScroll, listScroll + (wheel > 0 ? -1 : 1)));
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        super.keyTyped(typedChar, keyCode);
        if (keyCode == mc.gameSettings.keyBindInventory.getKeyCode()) {
            mc.displayGuiScreen(null);
        }
    }

    // ---------------- 绘制 ----------------

    private List<ItemStack> ingredients() {
        long key = context.getShiftSnap() != -1 ? context.getShiftSnap() : ClientScheduler.getClientTick() / BlockArray.BlockInformation.CYCLE_TICK_SPEED;
        if (key != ingredientsKey) {
            ingredientsKey = key;
            List<ItemStack> list = new ArrayList<>(context.getDescriptiveStacks());
            list.sort((left, right) -> Integer.compare(right.stackSize, left.stackSize));
            if (controllerStack != null) {
                list.add(0, controllerStack.copy());
            }
            ingredients = list;
        }
        return ingredients;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(GUI_TEXTURE);
        this.drawTexturedModalRect(guiLeft, guiTop, 0, 0, X_SIZE, Y_SIZE);

        // 标题：前缀 + 机器名
        String prefix = machine.getPrefix();
        String prefixKey = machine.getRegistryName().getNamespace() + "." + machine.getRegistryName().getPath() + ".prefix";
        if (prefix != null && !prefix.isEmpty() && !prefix.equals(prefixKey)) {
            drawCenteredClipped(prefix, guiLeft + 5, guiTop + 5, 36, 18);
        }
        drawCenteredClipped(machine.getLocalizedName(), guiLeft + 43, guiTop + 5, 136, 18);

        // 3D 预览
        context.renderAt(guiLeft + VIEW_X + 1, guiTop + VIEW_Y + 1, VIEW_W - 2, VIEW_H - 2);

        // 状态栏
        BlockArray pattern = context.getPattern();
        String size = (pattern.getMax().getX() - pattern.getMin().getX() + 1) + "x"
            + (pattern.getMax().getY() - pattern.getMin().getY() + 1) + "x"
            + (pattern.getMax().getZ() - pattern.getMin().getZ() + 1);
        fontRendererObj.drawStringWithShadow(size, guiLeft + VIEW_X + 3, guiTop + VIEW_Y + 3, 0xAAAAAA);
        int line = guiTop + VIEW_Y + 13;
        if (!context.doesRenderIn3D()) {
            fontRendererObj.drawStringWithShadow(I18n.format("nei.modularmachinery.structure.layer", context.getRenderSlice() + 1, context.getLayerCount()),
                guiLeft + VIEW_X + 3, line, 0xFFFFFF);
            line += 10;
        }
        if (hasDynamicPattern) {
            fontRendererObj.drawStringWithShadow(I18n.format("gui.blueprint.dynamic_pattern_size", context.getDynamicPatternSize()),
                guiLeft + VIEW_X + 3, line, 0xFFFF55);
        }
        if (machine.isRequiresBlueprint()) {
            String text = EnumChatFormatting.RED + I18n.format("nei.modularmachinery.structure.blueprint");
            fontRendererObj.drawStringWithShadow(text, guiLeft + VIEW_X + 3, guiTop + 164, 0xFFFFFF);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);

        // 方块清单
        List<ItemStack> list = ingredients();
        ItemStack hovered = null;
        this.mc.getTextureManager().bindTexture(GUI_TEXTURE);
        GL11.glColor4f(1F, 1F, 1F, 1F);
        int first = listScroll * LIST_COLUMNS;
        for (int i = 0; i < LIST_COLUMNS * LIST_ROWS; i++) {
            int sx = guiLeft + LIST_X + (i % LIST_COLUMNS) * 18;
            int sy = guiTop + LIST_Y + (i / LIST_COLUMNS) * 18;
            drawTexturedModalRect(sx, sy, 184, 194, 18, 18);
        }
        int maxScroll = Math.max(0, (list.size() + LIST_COLUMNS - 1) / LIST_COLUMNS - LIST_ROWS);
        int thumbY = guiTop + LIST_Y + 1 + (maxScroll == 0 ? 0 : (34 - 13) * listScroll / maxScroll);
        drawTexturedModalRect(guiLeft + LIST_X + LIST_COLUMNS * 18 + 3, thumbY, maxScroll == 0 ? 214 : 184, 143, 8, 13);

        RenderHelper.enableGUIStandardItemLighting();
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        for (int i = 0; i < LIST_COLUMNS * LIST_ROWS && first + i < list.size(); i++) {
            ItemStack stack = list.get(first + i);
            int sx = guiLeft + LIST_X + (i % LIST_COLUMNS) * 18 + 1;
            int sy = guiTop + LIST_Y + (i / LIST_COLUMNS) * 18 + 1;
            itemRender.renderItemAndEffectIntoGUI(fontRendererObj, mc.getTextureManager(), stack, sx, sy);
            itemRender.renderItemOverlayIntoGUI(fontRendererObj, mc.getTextureManager(), stack, sx, sy);
            if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) {
                hovered = stack;
            }
        }
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL11.GL_LIGHTING);

        // 提示
        if (hovered != null) {
            renderToolTip(hovered, mouseX, mouseY);
            return;
        }
        for (Object o : buttonList) {
            if (o instanceof TexturedButton b && b.visible && b.isMouseOver(mouseX, mouseY)) {
                func_146283_a(b.tooltip.get(), mouseX, mouseY);
                return;
            }
        }
        if (mouseX >= guiLeft + 5 && mouseX < guiLeft + 179 && mouseY >= guiTop + 5 && mouseY < guiTop + 23) {
            func_146283_a(machineInfo(), mouseX, mouseY);
        }
    }

    private void drawCenteredClipped(String text, int x, int y, int w, int h) {
        String clipped = fontRendererObj.trimStringToWidth(text, w - 4);
        fontRendererObj.drawStringWithShadow(clipped, x + (w - fontRendererObj.getStringWidth(clipped)) / 2, y + (h - 8) / 2, 0xFFFFFF);
    }

    private List<String> machineInfo() {
        BlockArray pattern = context.getPattern();
        List<String> tips = new ArrayList<>();
        tips.add(machine.getLocalizedName());
        tips.add(I18n.format("gui.preview.button.machine_info"));
        tips.add(I18n.format("gui.preview.button.machine_info.xyz.0"));
        tips.add(I18n.format("gui.preview.button.machine_info.xyz.1",
            pattern.getMax().getX() - pattern.getMin().getX() + 1,
            pattern.getMax().getY() - pattern.getMin().getY() + 1,
            pattern.getMax().getZ() - pattern.getMin().getZ() + 1));
        tips.add(I18n.format("gui.preview.button.machine_info.controller_y_pos", Math.abs(machine.getPattern().getMin().getY())));
        if (hasDynamicPattern) {
            tips.add(I18n.format("gui.preview.button.machine_info.dynamic_pattern"));
        }
        if (machine.isRequiresBlueprint()) {
            tips.add(I18n.format("gui.preview.button.machine_info.requires_blueprint"));
        }
        // 操作提示放在标题提示里，避免悬停在预览区时遮挡结构
        tips.add(EnumChatFormatting.GRAY + I18n.format("nei.modularmachinery.structure.hint.rotate"));
        tips.add(EnumChatFormatting.GRAY + I18n.format("nei.modularmachinery.structure.hint.zoom"));
        tips.add(EnumChatFormatting.GRAY + I18n.format("nei.modularmachinery.structure.hint.reset"));
        return tips;
    }

    /**
     * guiblueprint_new.png 中的贴图按钮：横向依次为普通、悬停、按下等状态，间隔 step 像素。
     * toggledColumn 为选中状态所用的列；disabledColumn 为不可用状态的列（-1 表示没有，改为变暗绘制普通贴图）。
     */
    static final class TexturedButton extends GuiButton {
        final int                    texX, texY, step, toggledColumn, disabledColumn;
        final Supplier<List<String>> tooltip;
        boolean toggled = false;

        TexturedButton(int id, int size, int texX, int texY, int step, int toggledColumn, int disabledColumn, Supplier<List<String>> tooltip) {
            super(id, 0, 0, size, size, "");
            this.texX = texX;
            this.texY = texY;
            this.step = step;
            this.toggledColumn = toggledColumn;
            this.disabledColumn = disabledColumn;
            this.tooltip = tooltip;
        }

        boolean isMouseOver(int mouseX, int mouseY) {
            return mouseX >= xPosition && mouseY >= yPosition && mouseX < xPosition + width && mouseY < yPosition + height;
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY) {
            if (!visible) {
                return;
            }
            mc.getTextureManager().bindTexture(GUI_TEXTURE);
            float shade = 1F;
            int column;
            if (!enabled) {
                column = disabledColumn >= 0 ? disabledColumn : 0;
                shade = disabledColumn >= 0 ? 1F : 0.45F;
            } else if (toggled) {
                column = toggledColumn;
            } else if (isMouseOver(mouseX, mouseY)) {
                column = Mouse.isButtonDown(0) ? 2 : 1;
            } else {
                column = 0;
            }
            GL11.glColor4f(shade, shade, shade, 1F);
            drawTexturedModalRect(xPosition, yPosition, texX + column * step, texY, width, height);
            GL11.glColor4f(1F, 1F, 1F, 1F);
        }
    }

}
