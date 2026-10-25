package hellfirepvp.modularmachinery.client.nei;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.TemplateRecipeHandler;
import github.kasuminova.mmce.compat.BlockPos;
import github.kasuminova.mmce.compat.IBlockState;
import github.kasuminova.mmce.compat.ItemStacks;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.client.ClientProxy;
import hellfirepvp.modularmachinery.client.util.BlockArrayPreviewRenderHelper;
import hellfirepvp.modularmachinery.client.util.BlockArrayRenderHelper;
import hellfirepvp.modularmachinery.client.util.DynamicMachineRenderContext;
import hellfirepvp.modularmachinery.client.util.PreviewRayPicker;
import hellfirepvp.modularmachinery.client.util.StructureIngredientMerger;
import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.block.BlockFactoryController;
import hellfirepvp.modularmachinery.common.item.ItemBlueprint;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import hellfirepvp.modularmachinery.common.modifier.SingleBlockModifierReplacement;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import hellfirepvp.modularmachinery.common.util.IBlockStateDescriptor;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.OreDictionary;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * 1.7.10 移植：替代 1.12.2 的 JEI 结构预览（CategoryStructurePreview / MachineStructurePreviewPanel），所有机器共用一个 NEI 页面。
 * <ul>
 *   <li>3D 结构：鼠标左键拖动旋转、滚轮缩放、右键复位；可切换只显示某一层；替选方块按时间轮换显示。</li>
 *   <li>方块清单：每个位置可接受的方块（含数量，多种可选时自动轮换）；升级方块（机器修改器）单独列出，悬停显示效果说明。</li>
 *   <li>查询：对控制器或绑定了机器的蓝图按 R 打开对应结构；对某个方块按 U 列出用到它的机器结构。</li>
 * </ul>
 * <ul>
 *   <li>投影：把该结构投影到世界中（动态结构取最小尺寸），见 {@link BlockArrayPreviewRenderHelper}。</li>
 * </ul>
 * 3D 渲染与蓝图预览界面共用 {@link DynamicMachineRenderContext} / {@link BlockArrayRenderHelper}：
 * 动态结构按最小尺寸显示，升级方块的位置在原方块与升级方块之间轮换。带 TileEntity 特殊渲染（TESR）的方块只按普通方块渲染。
 */
public class StructurePreviewHandler extends TemplateRecipeHandler {

    public static final String ID = ModularMachinery.MODID + ".structure";

    static final int WIDTH = 166;
    static final Rectangle VIEW = new Rectangle(0, 11, WIDTH, 100);
    static final Rectangle BTN_LAYER_DOWN = new Rectangle(0, 113, 12, 10);
    static final Rectangle BTN_LAYER_UP = new Rectangle(14, 113, 12, 10);
    static final Rectangle BTN_ALL = new Rectangle(28, 113, 18, 10);
    static final Rectangle BTN_PROJECT = new Rectangle(WIDTH - 17 - 3 - 40, 113, 40, 10);
    /** 动态结构尺寸 −/+（只对有 dynamic-patterns 的机器显示），叠在预览区左上角。 */
    static final Rectangle BTN_DYN_MINUS = new Rectangle(VIEW.x + 2, VIEW.y + 2, 13, 13);
    static final Rectangle BTN_DYN_PLUS = new Rectangle(VIEW.x + 17, VIEW.y + 2, 13, 13);
    /** 与蓝图预览界面共用的按钮贴图（{@code guiblueprint_new.png}，13x13，列步进 15）。 */
    static final ResourceLocation BLUEPRINT_TEXTURE =
        new ResourceLocation(ModularMachinery.MODID, "textures/gui/guiblueprint_new.png");
    static final int TEX_DYN_PLUS_U = 184;
    static final int TEX_DYN_PLUS_V = 60;
    static final int TEX_DYN_MINUS_U = 184;
    static final int TEX_DYN_MINUS_V = 75;
    static final int LIST_TOP = 136;
    static final int PER_ROW = 9;

    @Override
    public TemplateRecipeHandler newInstance() {
        return new StructurePreviewHandler();
    }

    @Override
    public String getRecipeName() {
        return I18n.format("jei.category.preview");
    }

    @Override
    public String getOverlayIdentifier() {
        return ID;
    }

    @Override
    public String getHandlerId() {
        return ID;
    }

    @Override
    public String getGuiTexture() {
        return MachineRecipeHandler.ICONS;
    }

    @Override
    public int recipiesPerPage() {
        return 1;
    }

    @Override
    public int getRecipeHeight(int recipe) {
        if (recipe < 0 || recipe >= arecipes.size()) {
            return 200;
        }
        return ((CachedStructure) arecipes.get(recipe)).height;
    }

    // ---------------- 查询 ----------------

    @Override
    public void loadCraftingRecipes(String outputId, Object... results) {
        if (ID.equals(outputId)) {
            for (DynamicMachine machine : MachineRegistry.getLoadedMachines()) {
                arecipes.add(new CachedStructure(machine));
            }
            return;
        }
        super.loadCraftingRecipes(outputId, results);
    }

    @Override
    public void loadCraftingRecipes(ItemStack result) {
        DynamicMachine machine = machineOf(result);
        if (machine != null) {
            arecipes.add(new CachedStructure(machine));
        }
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        if (ItemStacks.isEmpty(ingredient) || !(ingredient.getItem() instanceof ItemBlock)) {
            return;
        }
        Block block = Block.getBlockFromItem(ingredient.getItem());
        if (block == null || block == Blocks.air || block instanceof BlockController) {
            return;
        }
        int meta = ingredient.getItemDamage();
        for (DynamicMachine machine : MachineRegistry.getLoadedMachines()) {
            if (usesBlock(machine, block, meta)) {
                arecipes.add(new CachedStructure(machine));
            }
        }
    }

    static DynamicMachine machineOf(ItemStack stack) {
        if (ItemStacks.isEmpty(stack)) {
            return null;
        }
        if (stack.getItem() instanceof ItemBlueprint) {
            return ItemBlueprint.getAssociatedMachine(stack);
        }
        Block block = Block.getBlockFromItem(stack.getItem());
        if (block instanceof BlockController controller) {
            return controller.getParentMachine();
        }
        return null;
    }

    private static boolean usesBlock(DynamicMachine machine, Block block, int meta) {
        IBlockState state = IBlockState.of(block, meta == OreDictionary.WILDCARD_VALUE ? 0 : meta);
        List<BlockArray.BlockInformation> infos = new ArrayList<>(machine.getPattern().getPattern().values());
        for (List<SingleBlockModifierReplacement> list : machine.getModifiers().values()) {
            for (SingleBlockModifierReplacement replacement : list) {
                infos.add(replacement.getBlockInformation());
            }
        }
        for (BlockArray.BlockInformation info : infos) {
            for (IBlockStateDescriptor descriptor : info.getMatchingStates()) {
                if (meta == OreDictionary.WILDCARD_VALUE) {
                    for (IBlockState applicable : descriptor.getApplicable()) {
                        if (applicable.getBlock() == block) {
                            return true;
                        }
                    }
                } else if (descriptor.accepts(state)) {
                    return true;
                }
            }
        }
        return false;
    }

    // ---------------- 结构缓存 ----------------

    public class CachedStructure extends CachedRecipe {
        final DynamicMachine machine;
        DynamicMachineRenderContext context;
        final List<PositionedStack> blocks = new ArrayList<>();
        final List<PositionedStack> upgrades = new ArrayList<>();
        final Map<PositionedStack, List<String>> upgradeTips = new IdentityHashMap<>();
        BlockPos min;
        BlockPos max;
        int height;
        int upgradeTop;
        PositionedStack controllerStack;

        /** 动态结构段数；非动态结构为 0（与蓝图预览界面一致）。 */
        int dynamicSize;

        float yaw = BlockArrayRenderHelper.DEFAULT_YAW;
        float pitch = BlockArrayRenderHelper.DEFAULT_PITCH;
        float zoom = 1F;
        /** -1 表示显示全部层，否则为相对最低层的层号。 */
        int layer = -1;
        int lastMouseX = Integer.MIN_VALUE;
        int lastMouseY = Integer.MIN_VALUE;

        /** 被点选的方块坐标（渲染用的、已平移的结构坐标）；null 表示未选中。 */
        BlockPos selectedPos;
        /** 被点选坐标可接受的方块（含该位置的升级方块）。作为原生 ingredient 交给 NEI 绘制与提示。 */
        final List<PositionedStack> selectedSlots = new ArrayList<>();
        /** 该坐标全部可接受方块。 */
        final List<ItemStack> selectedAll = new ArrayList<>();
        /** 可见窗口的起始下标（横向滚动）。 */
        int selectedScroll;
        /** 渲染时拾取到的方块（由深度缓冲反投影得到）。 */
        BlockPos hoveredBlock;
        /** 点选区起始 y；未选中时为 -1。 */
        int selectedTop = -1;
        /** 不含点选区时的页面高度。 */
        int baseHeight;
        /** 按下左键的起点与累计拖动距离，用于区分“点击选方块”和“拖动旋转”。 */
        int pressX = Integer.MIN_VALUE;
        int pressY = Integer.MIN_VALUE;
        int dragDistance;

        CachedStructure(DynamicMachine machine) {
            this.machine = machine;
            this.dynamicSize = DynamicMachineRenderContext.minDynamicPatternSize(machine);
            rebuild(dynamicSize);
        }

        boolean hasDynamicPattern() {
            return !machine.getDynamicPatterns().isEmpty();
        }

        int minDynamicSize() {
            return DynamicMachineRenderContext.minDynamicPatternSize(machine);
        }

        int maxDynamicSize() {
            return DynamicMachineRenderContext.maxDynamicPatternSize(machine);
        }

        /** 调整动态结构尺寸；超出 [min, max] 时忽略。 */
        void setDynamicSize(int size) {
            int clamped = Math.max(minDynamicSize(), Math.min(maxDynamicSize(), size));
            if (clamped == dynamicSize) {
                return;
            }
            rebuild(clamped);
        }

        /** 以指定动态尺寸重建预览内容，保留当前视角。 */
        private void rebuild(int size) {
            this.dynamicSize = size;
            this.context = DynamicMachineRenderContext.createContext(machine, size);
            this.min = context.getPattern().getMin();
            this.max = context.getPattern().getMax();
            this.blocks.clear();
            this.upgrades.clear();
            this.upgradeTips.clear();
            this.selectedPos = null;
            this.selectedSlots.clear();
            this.selectedAll.clear();
            this.selectedScroll = 0;
            this.hoveredBlock = null;
            this.selectedTop = -1;
            if (layer >= 0) {
                layer = Math.min(layer, layerCount() - 1);
            }

            ItemStack controller = controllerItem(machine);
            this.controllerStack = controller == null ? null : new PositionedStack(controller, WIDTH - 17, 114, false);

            // getIngredientList() 已合并“单一选择”的位置（数量在 stackSize 里），
            // 这里再按“可接受组合”合并多选择的位置，避免同一组方块占满整个列表。
            List<List<ItemStack>> ingredients = StructureIngredientMerger.merge(
                context.getStructurePattern().getIngredientList());
            int index = 0;
            for (List<ItemStack> alternatives : ingredients) {
                List<ItemStack> copies = new ArrayList<>(alternatives.size());
                for (ItemStack stack : alternatives) {
                    if (!ItemStacks.isEmpty(stack)) {
                        copies.add(stack.copy());
                    }
                }
                if (copies.isEmpty()) {
                    continue;
                }
                blocks.add(new PositionedStack(copies, 1 + (index % PER_ROW) * 18, LIST_TOP + 1 + (index / PER_ROW) * 18, false));
                index++;
            }
            int rows = (index + PER_ROW - 1) / PER_ROW;
            int y = LIST_TOP + rows * 18 + 2;

            int upIndex = 0;
            for (Map.Entry<BlockPos, List<SingleBlockModifierReplacement>> entry : machine.getModifiers().entrySet()) {
                for (SingleBlockModifierReplacement replacement : entry.getValue()) {
                    List<ItemStack> stacks = new ArrayList<>(replacement.getBlockInformation().getIngredientList());
                    stacks.removeIf(ItemStacks::isEmpty);
                    if (stacks.isEmpty()) {
                        continue;
                    }
                    PositionedStack stack = new PositionedStack(stacks, 1 + (upIndex % PER_ROW) * 18, y + 10 + 1 + (upIndex / PER_ROW) * 18, false);
                    List<String> tip = new ArrayList<>();
                    BlockPos p = entry.getKey();
                    tip.add(EnumChatFormatting.GREEN + I18n.format("nei.modularmachinery.structure.upgrade.pos", p.getX(), p.getY(), p.getZ()));
                    tip.addAll(replacement.getDescriptionLines());
                    upgradeTips.put(stack, tip);
                    upgrades.add(stack);
                    upIndex++;
                }
            }
            this.upgradeTop = upgrades.isEmpty() ? -1 : y;
            int upRows = (upIndex + PER_ROW - 1) / PER_ROW;
            this.baseHeight = upgrades.isEmpty() ? y : y + 10 + upRows * 18 + 2;
            this.height = baseHeight;
            this.selectedTop = -1;
        }

        /** 该坐标（渲染坐标）可接受的方块：结构本身的替选 + 该位置的升级方块。 */
        List<ItemStack> stacksAt(BlockPos pos) {
            BlockArray.BlockInformation info = context.getPattern().getPattern().get(pos);
            if (info == null) {
                return Collections.emptyList();
            }
            List<ItemStack> out = new ArrayList<>();
            for (ItemStack stack : info.getIngredientList()) {
                if (!ItemStacks.isEmpty(stack)) {
                    out.add(stack);
                }
            }
            return out;
        }

        /** 该坐标上的升级方块说明；不是升级方块时返回 null。 */
        List<String> upgradeTipAt(BlockPos pos, ItemStack stack) {
            BlockPos machinePos = pos.subtract(context.getMoveOffset());
            List<SingleBlockModifierReplacement> mods = machine.getModifiers().get(machinePos);
            if (mods == null) {
                return null;
            }
            for (SingleBlockModifierReplacement mod : mods) {
                for (ItemStack candidate : mod.getBlockInformation().getIngredientList()) {
                    if (ItemStacks.isEmpty(candidate) || candidate.getItem() != stack.getItem()
                        || candidate.getItemDamage() != stack.getItemDamage()) {
                        continue;
                    }
                    List<String> tip = new ArrayList<>();
                    tip.add(EnumChatFormatting.GREEN + I18n.format(
                        "nei.modularmachinery.structure.upgrade.pos", machinePos.getX(), machinePos.getY(), machinePos.getZ()));
                    tip.addAll(mod.getDescriptionLines());
                    return tip;
                }
            }
            return null;
        }

        /**
         * 设置被点选的坐标与其可用方块。列表作为原生 ingredient 追加在升级方块区下方，
         * 由 NEI 负责绘制图标、显示 native tooltip 以及页面自身的滚动条，多于一页时自然可滚。
         */
        void setSelection(BlockPos pos, List<ItemStack> stacks) {
            selectedSlots.clear();
            selectedAll.clear();
            selectedScroll = 0;
            if (pos == null || stacks.isEmpty()) {
                selectedPos = null;
                selectedTop = -1;
                height = baseHeight;
                return;
            }
            selectedPos = pos;
            selectedTop = baseHeight;
            selectedAll.addAll(stacks);
            applySelectedWindow();
        }

        /** 按行铺开全部可接受方块（不滚动，一屏放不下时由 NEI 的页面滚动条处理）。 */
        void applySelectedWindow() {
            selectedSlots.clear();
            int y = selectedTop + 11;
            int index = 0;
            for (ItemStack stack : selectedAll) {
                selectedSlots.add(new PositionedStack(stack,
                    1 + (index % PER_ROW) * 18, y + (index / PER_ROW) * 18, false));
                index++;
            }
            int rows = (index + PER_ROW - 1) / PER_ROW;
            height = y + rows * 18 + 2;
        }

        /** 鼠标是否落在点选列表的某个格子上。 */
        PositionedStack selectionSlotAt(Point mouse) {
            for (PositionedStack slot : selectedSlots) {
                if (slot.contains(mouse.x, mouse.y)) {
                    return slot;
                }
            }
            return null;
        }
        /**
         * 把预览区内的鼠标坐标还原成射线，返回第一个（看得见的）方块。
         * 具体几何与射线推进在 {@link PreviewRayPicker} 中实现，由单元测试覆盖。
         */
        BlockPos pick(int mouseX, int mouseY) {
            Integer sliceY = layer < 0 ? null : min.getY() + layer;
            return PreviewRayPicker.pick(context.getPattern().getPattern(), min, max,
                VIEW.x, VIEW.y, VIEW.width, VIEW.height, yaw, pitch, zoom, mouseX, mouseY, sliceY);
        }

        int layerCount() {
            return max.getY() - min.getY() + 1;
        }

        @Override
        public PositionedStack getResult() {
            return controllerStack;
        }

        @Override
        public List<PositionedStack> getIngredients() {
            List<PositionedStack> all = new ArrayList<>(blocks.size() + upgrades.size() + selectedSlots.size());
            all.addAll(blocks);
            all.addAll(upgrades);
            all.addAll(selectedSlots);
            return getCycledIngredients(cycleticks / 20, all);
        }
    }

    static ItemStack controllerItem(DynamicMachine machine) {
        BlockController controller = BlockController.getControllerWithMachine(machine);
        if (controller == null) {
            controller = BlockFactoryController.FACTORY_CONTROLLERS.get(machine);
        }
        Item item = controller == null ? null : Item.getItemFromBlock(controller);
        return item == null ? null : new ItemStack(item);
    }

    // ---------------- 绘制 ----------------

    @Override
    public void drawBackground(int recipe) {
        CachedStructure s = (CachedStructure) arecipes.get(recipe);
        GuiDraw.drawRect(VIEW.x, VIEW.y, VIEW.width, VIEW.height, 0x40000000);
        GL11.glColor4f(1F, 1F, 1F, 1F);
        GuiDraw.changeTexture(MachineRecipeHandler.ICONS);
        for (PositionedStack stack : s.blocks) {
            GuiDraw.drawTexturedModalRect(stack.relx - 1, stack.rely - 1, 54, 0, 18, 18);
        }
        for (PositionedStack stack : s.upgrades) {
            GuiDraw.drawTexturedModalRect(stack.relx - 1, stack.rely - 1, 54, 0, 18, 18);
        }
        for (PositionedStack stack : s.selectedSlots) {
            GuiDraw.drawTexturedModalRect(stack.relx - 1, stack.rely - 1, 54, 0, 18, 18);
        }
        if (s.controllerStack != null) {
            GuiDraw.drawTexturedModalRect(s.controllerStack.relx - 1, s.controllerStack.rely - 1, 54, 0, 18, 18);
        }
    }

    @Override
    public void drawExtras(int recipe) {
        CachedStructure s = (CachedStructure) arecipes.get(recipe);
        handleDrag(s, recipe);
        handleSelectionRelease(s, recipe);

        int sizeX = s.max.getX() - s.min.getX() + 1;
        int sizeY = s.max.getY() - s.min.getY() + 1;
        int sizeZ = s.max.getZ() - s.min.getZ() + 1;
        GuiDraw.drawString(EnumChatFormatting.WHITE + s.machine.getLocalizedName(), 0, 0, 0xFFFFFF, true);
        GuiDraw.drawStringR(sizeX + "x" + sizeY + "x" + sizeZ, WIDTH, 0, 0xAAAAAA, true);

        renderStructure(s, recipe);
        drawSelectionHighlight(s);

        Point mouse = mouseIn(recipe);
        drawButton(BTN_LAYER_DOWN, "<");
        drawButton(BTN_LAYER_UP, ">");
        drawButton(BTN_ALL, "3D");
        drawButton(BTN_PROJECT, I18n.format("nei.modularmachinery.structure.project"));
        if (s.hasDynamicPattern()) {
            drawIconButton(BTN_DYN_MINUS, TEX_DYN_MINUS_U, TEX_DYN_MINUS_V,
                s.dynamicSize > s.minDynamicSize(), mouse == null ? -1 : mouse.x, mouse == null ? -1 : mouse.y);
            drawIconButton(BTN_DYN_PLUS, TEX_DYN_PLUS_U, TEX_DYN_PLUS_V,
                s.dynamicSize < s.maxDynamicSize(), mouse == null ? -1 : mouse.x, mouse == null ? -1 : mouse.y);
            GuiDraw.drawString(EnumChatFormatting.YELLOW + I18n.format(
                "nei.modularmachinery.structure.dynamic_size", s.dynamicSize),
                BTN_DYN_PLUS.x + BTN_DYN_PLUS.width + 3, BTN_DYN_PLUS.y + 3, 0xFFFFFF, true);
        }
        String layerText = s.layer < 0
            ? I18n.format("nei.modularmachinery.structure.layer.all")
            : I18n.format("nei.modularmachinery.structure.layer", s.layer + 1, s.layerCount());
        GuiDraw.drawString(layerText, BTN_ALL.x + BTN_ALL.width + 4, BTN_ALL.y + 1, 0xFFFFFF, true);
        GuiDraw.drawString(I18n.format("nei.modularmachinery.structure.blocks"), 0, LIST_TOP - 10, 0xFFFFFF, true);
        if (s.machine.isRequiresBlueprint()) {
            GuiDraw.drawStringR(EnumChatFormatting.RED + I18n.format("nei.modularmachinery.structure.blueprint"), WIDTH - 20, LIST_TOP - 10, 0xFFFFFF, true);
        }
        if (s.upgradeTop >= 0) {
            GuiDraw.drawString(I18n.format("nei.modularmachinery.structure.upgrades"), 0, s.upgradeTop, 0xFFFFFF, true);
        }
        if (s.selectedTop >= 0) {
            GuiDraw.drawString(I18n.format("nei.modularmachinery.structure.selected"), 0, s.selectedTop, 0xFFFFFF, true);
        }
        GL11.glColor4f(1F, 1F, 1F, 1F);
    }

    /** 点选坐标后，在预览区底部列出该坐标可接受的方块（含升级方块）：固定一行，超出用滚轮横向滚动。 */
    private void handleSelectionRelease(CachedStructure s, int recipe) {
        if (s.pressX == Integer.MIN_VALUE) {
            return;
        }
        if (Mouse.isButtonDown(0)) {
            return;
        }
        int pressX = s.pressX;
        int pressY = s.pressY;
        boolean click = s.dragDistance <= 3;
        s.pressX = Integer.MIN_VALUE;
        s.pressY = Integer.MIN_VALUE;
        s.dragDistance = 0;
        if (!click) {
            return;
        }
        BlockPos picked = s.hoveredBlock;
        s.setSelection(picked, picked == null ? Collections.emptyList() : s.stacksAt(picked));
    }

    private static void drawButton(Rectangle r, String text) {
        GuiDraw.drawRect(r.x, r.y, r.width, r.height, 0xFF555555);
        GuiDraw.drawRect(r.x + 1, r.y + 1, r.width - 2, r.height - 2, 0xFF8B8B8B);
        GuiDraw.drawStringC(text, r.x, r.y + 1, r.width, r.height - 1, 0xFFFFFF, true);
    }

    /**
     * 用蓝图预览界面同一张按钮贴图绘制 13x13 图标按钮：列步进 15（0 普通 / 1 悬停 / 2 按下），禁用时压暗。
     */
    private static void drawIconButton(Rectangle r, int texU, int texV, boolean enabled, int mouseX, int mouseY) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(BLUEPRINT_TEXTURE);
        int column = 0;
        if (enabled) {
            if (mouseX >= r.x && mouseY >= r.y && mouseX < r.x + r.width && mouseY < r.y + r.height) {
                column = Mouse.isButtonDown(0) ? 2 : 1;
            }
        } else {
            column = 2;
        }
        float shade = enabled ? 1F : 0.45F;
        GL11.glColor4f(shade, shade, shade, 1F);
        GuiDraw.drawTexturedModalRect(r.x, r.y, texU + column * 15, texV, r.width, r.height);
        GL11.glColor4f(1F, 1F, 1F, 1F);
    }

    /** 高亮被点选的方块：关掉深度测试，画它的 12 条棱（与原项目的方块高亮等效）。 */
    private void drawSelectionHighlight(CachedStructure s) {
        if (s.selectedPos == null) {
            return;
        }
        BlockPos pos = s.selectedPos;
        double x0 = pos.getX();
        double y0 = pos.getY();
        double z0 = pos.getZ();
        double x1 = x0 + 1;
        double y1 = y0 + 1;
        double z1 = z0 + 1;
        double[][] c = new double[8][];
        c[0] = projectPoint(s, x0, y0, z0);
        c[1] = projectPoint(s, x1, y0, z0);
        c[2] = projectPoint(s, x1, y0, z1);
        c[3] = projectPoint(s, x0, y0, z1);
        c[4] = projectPoint(s, x0, y1, z0);
        c[5] = projectPoint(s, x1, y1, z0);
        c[6] = projectPoint(s, x1, y1, z1);
        c[7] = projectPoint(s, x0, y1, z1);
        int[][] edges = { { 0, 1 }, { 1, 2 }, { 2, 3 }, { 3, 0 }, { 4, 5 }, { 5, 6 }, { 6, 7 }, { 7, 4 },
            { 0, 4 }, { 1, 5 }, { 2, 6 }, { 3, 7 } };
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glLineWidth(2F);
        GL11.glColor4f(1F, 0.25F, 0.25F, 1F);
        GL11.glBegin(GL11.GL_LINES);
        for (int[] edge : edges) {
            GL11.glVertex2d(c[edge[0]][0], c[edge[0]][1]);
            GL11.glVertex2d(c[edge[1]][0], c[edge[1]][1]);
        }
        GL11.glEnd();
        GL11.glLineWidth(1F);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glColor4f(1F, 1F, 1F, 1F);
    }

    /** 与 {@link BlockArrayRenderHelper#render3DGUI} 相同的正向变换：结构坐标 → 页面坐标。 */
    private static double[] projectPoint(CachedStructure s, double x, double y, double z) {
        // 复用与拾取相同、且被单元测试覆盖的投影实现，避免两份数学不一致导致高亮错位
        BlockArray pattern = s.context.getPattern();
        return PreviewRayPicker.project(pattern.getMin(), pattern.getMax(),
            VIEW.x, VIEW.y, VIEW.width, VIEW.height, s.yaw, s.pitch, s.zoom, x, y, z);
    }

    private void handleDrag(CachedStructure s, int recipe) {
        Point mouse = mouseIn(recipe);
        if (mouse == null) {
            return;
        }
        boolean inside = VIEW.contains(mouse);
        if (Mouse.isButtonDown(0) && (inside || s.lastMouseX != Integer.MIN_VALUE)) {
            if (s.lastMouseX != Integer.MIN_VALUE) {
                int dx = mouse.x - s.lastMouseX;
                int dy = mouse.y - s.lastMouseY;
                s.yaw += dx * 1.5F;
                s.pitch = Math.max(-90F, Math.min(90F, s.pitch + dy * 1.5F));
                s.dragDistance += Math.abs(dx) + Math.abs(dy);
            }
            s.lastMouseX = mouse.x;
            s.lastMouseY = mouse.y;
        } else {
            s.lastMouseX = Integer.MIN_VALUE;
            s.lastMouseY = Integer.MIN_VALUE;
        }
    }

    private static Point mouseIn(int recipe) {
        if (Minecraft.getMinecraft().currentScreen instanceof GuiRecipe<?> gui) {
            return gui.getRecipeMousePosition(recipe);
        }
        return null;
    }

    private void renderStructure(CachedStructure s, int recipe) {
        Point mouse = mouseIn(recipe);
        int pickX = -1;
        int pickY = -1;
        if (mouse != null && VIEW.contains(mouse)) {
            pickX = mouse.x;
            pickY = mouse.y;
        }
        s.context.getRender().render3DGUI(VIEW.x, VIEW.y, VIEW.width, VIEW.height, s.yaw, s.pitch, s.zoom,
            s.layer < 0 ? null : s.min.getY() + s.layer, pickX, pickY);
        s.hoveredBlock = s.context.getRender().getPickedBlock();
        if (s.hoveredBlock == null && pickX >= 0) {
            // 深度缓冲不可用（读到 1.0）时退回解析射线，保证仍能选中
            s.hoveredBlock = s.pick(pickX, pickY);
        }
    }

    // ---------------- 交互 ----------------

    /** 设置第 recipe 个结构的视角（yaw/pitch 为角度，layer = -1 表示全部层）。供外部调用与开发期自动化测试使用。 */
    public void setView(int recipe, float yaw, float pitch, float zoom, int layer) {
        if (recipe < 0 || recipe >= arecipes.size()) {
            return;
        }
        CachedStructure s = (CachedStructure) arecipes.get(recipe);
        s.yaw = yaw;
        s.pitch = pitch;
        s.zoom = zoom;
        s.layer = layer < 0 ? -1 : Math.min(layer, s.layerCount() - 1);
    }

    /** 把第 recipe 个结构按当前动态尺寸投影到世界并关闭 NEI 界面。供按钮与开发期自动化测试使用。 */
    public boolean projectToWorld(int recipe) {
        if (recipe < 0 || recipe >= arecipes.size()) {
            return false;
        }
        CachedStructure s = (CachedStructure) arecipes.get(recipe);
        ClientProxy.renderHelper.startPreview(DynamicMachineRenderContext.createContext(s.machine, s.dynamicSize));
        Minecraft.getMinecraft().displayGuiScreen(null);
        return true;
    }

    /** 第 recipe 个结构对应的机器注册名；越界返回 null。 */
    public String machineNameOf(int recipe) {
        return recipe < 0 || recipe >= arecipes.size() ? null : ((CachedStructure) arecipes.get(recipe)).machine.getRegistryName().toString();
    }


    @Override
    public boolean mouseClicked(GuiRecipe<?> gui, int button, int recipe) {
        if (recipe < 0 || recipe >= arecipes.size()) {
            return false;
        }
        CachedStructure s = (CachedStructure) arecipes.get(recipe);
        Point mouse = gui.getRecipeMousePosition(recipe);
        if (button == 0) {
            if (BTN_LAYER_DOWN.contains(mouse)) {
                s.layer = s.layer <= 0 ? s.layerCount() - 1 : s.layer - 1;
                return true;
            }
            if (BTN_LAYER_UP.contains(mouse)) {
                s.layer = s.layer < 0 || s.layer >= s.layerCount() - 1 ? 0 : s.layer + 1;
                return true;
            }
            if (BTN_ALL.contains(mouse)) {
                s.layer = -1;
                return true;
            }
            if (BTN_PROJECT.contains(mouse)) {
                return projectToWorld(recipe);
            }
            if (s.hasDynamicPattern()) {
                if (BTN_DYN_MINUS.contains(mouse)) {
                    s.setDynamicSize(s.dynamicSize - 1);
                    return true;
                }
                if (BTN_DYN_PLUS.contains(mouse)) {
                    s.setDynamicSize(s.dynamicSize + 1);
                    return true;
                }
            }
            if (VIEW.contains(mouse)) {
                // 记录按下位置：松开时若几乎没移动就当作“点击选方块”，否则是拖动旋转
                s.pressX = mouse.x;
                s.pressY = mouse.y;
                s.dragDistance = 0;
                return true;
            }
            return false;
        }
        if (button == 1 && VIEW.contains(mouse)) {
            s.yaw = BlockArrayRenderHelper.DEFAULT_YAW;
            s.pitch = BlockArrayRenderHelper.DEFAULT_PITCH;
            s.zoom = 1F;
            return true;
        }
        return super.mouseClicked(gui, button, recipe);
    }

    @Override
    public boolean mouseScrolled(GuiRecipe<?> gui, int scroll, int recipe) {
        if (recipe < 0 || recipe >= arecipes.size()) {
            return false;
        }
        Point mouse = gui.getRecipeMousePosition(recipe);
        if (!VIEW.contains(mouse)) {
            return false;
        }
        CachedStructure s = (CachedStructure) arecipes.get(recipe);
        s.zoom = Math.max(0.3F, Math.min(5F, s.zoom * (scroll > 0 ? 1.15F : 1F / 1.15F)));
        return true;
    }

    @Override
    public List<String> handleTooltip(GuiRecipe<?> gui, List<String> currenttip, int recipe) {
        if (!currenttip.isEmpty() || recipe < 0 || recipe >= arecipes.size()) {
            return currenttip;
        }
        Point mouse = gui.getRecipeMousePosition(recipe);
        CachedStructure s = (CachedStructure) arecipes.get(recipe);
        if (BTN_LAYER_DOWN.contains(mouse)) {
            currenttip.add(I18n.format("gui.preview.button.layer_render_scrollbar.down.tip"));
        } else if (BTN_LAYER_UP.contains(mouse)) {
            currenttip.add(I18n.format("gui.preview.button.layer_render_scrollbar.up.tip"));
        } else if (BTN_ALL.contains(mouse)) {
            currenttip.add(I18n.format("gui.preview.button.toggle_3d_render.tip"));
        } else if (BTN_PROJECT.contains(mouse)) {
            currenttip.add(I18n.format("gui.preview.button.place_world_preview.tip"));
        } else if (s.hasDynamicPattern() && BTN_DYN_MINUS.contains(mouse)) {
            currenttip.add(I18n.format("nei.modularmachinery.structure.dynamic_size.minus.tip", s.dynamicSize));
        } else if (s.hasDynamicPattern() && BTN_DYN_PLUS.contains(mouse)) {
            currenttip.add(I18n.format("nei.modularmachinery.structure.dynamic_size.plus.tip", s.dynamicSize));
        } else if (s.selectionSlotAt(mouse) != null) {
            // 点选列表里的方块：总是给出名称；是升级方块时再追加效果说明
            PositionedStack slot = s.selectionSlotAt(mouse);
            if (slot != null && !ItemStacks.isEmpty(slot.item)) {
                currenttip.add(slot.item.getDisplayName());
                List<String> upgradeTip = s.selectedPos == null ? null : s.upgradeTipAt(s.selectedPos, slot.item);
                if (upgradeTip != null) {
                    currenttip.addAll(upgradeTip);
                } else {
                    currenttip.add(EnumChatFormatting.GRAY + I18n.format("nei.modularmachinery.structure.selected"));
                }
            }
        } else if (VIEW.contains(mouse) && !Mouse.isButtonDown(0)) {
            currenttip.add(I18n.format("nei.modularmachinery.structure.hint.rotate"));
            currenttip.add(I18n.format("nei.modularmachinery.structure.hint.zoom"));
            currenttip.add(I18n.format("nei.modularmachinery.structure.hint.reset"));
            currenttip.add(I18n.format("nei.modularmachinery.structure.hint.select"));
            if (s.machine.isRequiresBlueprint()) {
                currenttip.add(EnumChatFormatting.RED + I18n.format("gui.preview.button.machine_info.requires_blueprint"));
            }
        }
        return currenttip;
    }

    @Override
    public List<String> handleItemTooltip(GuiRecipe<?> gui, ItemStack stack, List<String> currenttip, int recipe) {
        if (recipe < 0 || recipe >= arecipes.size()) {
            return currenttip;
        }
        CachedStructure s = (CachedStructure) arecipes.get(recipe);
        // 不用 gui.isMouseOver(stack, recipe)：它要求 NEI 内部的 RecipeWidget#getPositionedStackMouseOver 命中，
        // 对自行绘制在「升级方块」区的图标不成立，导致 description 一直不显示。
        // 这里直接用悬停格自身的 relx / rely 与配方内鼠标坐标比较，只依赖本模组的数据。
        Point mouse = mouseIn(recipe);
        if (mouse == null) {
            return currenttip;
        }
        for (Map.Entry<PositionedStack, List<String>> e : s.upgradeTips.entrySet()) {
            if (e.getKey().contains(mouse.x, mouse.y)) {
                currenttip.addAll(e.getValue());
                return currenttip;
            }
        }
        // 点选列表里的方块：若是该坐标的升级方块，追加坐标 + 效果说明。
        // 必须放在这里（handleItemTooltip）而不是只放在 handleTooltip：NEI 会先把物品自身的
        // tooltip 填进 currenttip，而 handleTooltip 在非空时直接返回，导致说明永远不显示。
        PositionedStack selectedSlot = s.selectionSlotAt(mouse);
        if (selectedSlot != null && s.selectedPos != null && !ItemStacks.isEmpty(selectedSlot.item)) {
            List<String> upgradeTip = s.upgradeTipAt(s.selectedPos, selectedSlot.item);
            if (upgradeTip != null) {
                currenttip.addAll(upgradeTip);
            }
            return currenttip;
        }
        for (PositionedStack block : s.blocks) {
            if (block.contains(mouse.x, mouse.y) && block.items.length > 1) {
                currenttip.add(EnumChatFormatting.GRAY + I18n.format("nei.modularmachinery.structure.alternatives", block.items.length));
                return currenttip;
            }
        }
        return currenttip;
    }

    /** 供配置类注册目录图标时使用。 */
    static List<ItemStack> allControllers() {
        List<ItemStack> out = new ArrayList<>();
        for (DynamicMachine machine : MachineRegistry.getLoadedMachines()) {
            ItemStack stack = controllerItem(machine);
            if (stack != null) {
                out.add(stack);
            }
        }
        return out.isEmpty() ? Collections.emptyList() : out;
    }
}
