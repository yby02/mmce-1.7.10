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
        final DynamicMachineRenderContext context;
        final List<PositionedStack> blocks = new ArrayList<>();
        final List<PositionedStack> upgrades = new ArrayList<>();
        final Map<PositionedStack, List<String>> upgradeTips = new IdentityHashMap<>();
        final BlockPos min;
        final BlockPos max;
        final int height;
        final int upgradeTop;
        final PositionedStack controllerStack;

        float yaw = BlockArrayRenderHelper.DEFAULT_YAW;
        float pitch = BlockArrayRenderHelper.DEFAULT_PITCH;
        float zoom = 1F;
        /** -1 表示显示全部层，否则为相对最低层的层号。 */
        int layer = -1;
        int lastMouseX = Integer.MIN_VALUE;
        int lastMouseY = Integer.MIN_VALUE;

        CachedStructure(DynamicMachine machine) {
            this.machine = machine;
            this.context = DynamicMachineRenderContext.createContext(machine);
            this.min = context.getPattern().getMin();
            this.max = context.getPattern().getMax();

            ItemStack controller = controllerItem(machine);
            this.controllerStack = controller == null ? null : new PositionedStack(controller, WIDTH - 17, 114, false);

            List<List<ItemStack>> ingredients = context.getStructurePattern().getIngredientList();
            int index = 0;
            for (List<ItemStack> alternatives : ingredients) {
                List<ItemStack> copies = new ArrayList<>();
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
            this.height = upgrades.isEmpty() ? y : y + 10 + upRows * 18 + 2;
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
            List<PositionedStack> all = new ArrayList<>(blocks.size() + upgrades.size());
            all.addAll(blocks);
            all.addAll(upgrades);
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
        if (s.controllerStack != null) {
            GuiDraw.drawTexturedModalRect(s.controllerStack.relx - 1, s.controllerStack.rely - 1, 54, 0, 18, 18);
        }
    }

    @Override
    public void drawExtras(int recipe) {
        CachedStructure s = (CachedStructure) arecipes.get(recipe);
        handleDrag(s, recipe);

        int sizeX = s.max.getX() - s.min.getX() + 1;
        int sizeY = s.max.getY() - s.min.getY() + 1;
        int sizeZ = s.max.getZ() - s.min.getZ() + 1;
        GuiDraw.drawString(EnumChatFormatting.WHITE + s.machine.getLocalizedName(), 0, 0, 0xFFFFFF, true);
        GuiDraw.drawStringR(sizeX + "x" + sizeY + "x" + sizeZ, WIDTH, 0, 0xAAAAAA, true);

        renderStructure(s);

        drawButton(BTN_LAYER_DOWN, "<");
        drawButton(BTN_LAYER_UP, ">");
        drawButton(BTN_ALL, "3D");
        drawButton(BTN_PROJECT, I18n.format("nei.modularmachinery.structure.project"));
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
        GL11.glColor4f(1F, 1F, 1F, 1F);
    }

    private static void drawButton(Rectangle r, String text) {
        GuiDraw.drawRect(r.x, r.y, r.width, r.height, 0xFF555555);
        GuiDraw.drawRect(r.x + 1, r.y + 1, r.width - 2, r.height - 2, 0xFF8B8B8B);
        GuiDraw.drawStringC(text, r.x, r.y + 1, r.width, r.height - 1, 0xFFFFFF, true);
    }

    private void handleDrag(CachedStructure s, int recipe) {
        Point mouse = mouseIn(recipe);
        if (mouse == null) {
            return;
        }
        boolean inside = VIEW.contains(mouse);
        if (Mouse.isButtonDown(0) && (inside || s.lastMouseX != Integer.MIN_VALUE)) {
            if (s.lastMouseX != Integer.MIN_VALUE) {
                s.yaw += (mouse.x - s.lastMouseX) * 1.5F;
                s.pitch = Math.max(-90F, Math.min(90F, s.pitch + (mouse.y - s.lastMouseY) * 1.5F));
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

    private void renderStructure(CachedStructure s) {
        s.context.getRender().render3DGUI(VIEW.x, VIEW.y, VIEW.width, VIEW.height, s.yaw, s.pitch, s.zoom,
            s.layer < 0 ? null : s.min.getY() + s.layer);
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

    /** 把第 recipe 个结构投影到世界（动态结构取最小尺寸）并关闭 NEI 界面。供按钮与开发期自动化测试使用。 */
    public boolean projectToWorld(int recipe) {
        if (recipe < 0 || recipe >= arecipes.size()) {
            return false;
        }
        CachedStructure s = (CachedStructure) arecipes.get(recipe);
        ClientProxy.renderHelper.startPreview(DynamicMachineRenderContext.createContext(s.machine));
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
            // 预览区内的左键用于拖动旋转，不交给 NEI
            return VIEW.contains(mouse);
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
        } else if (VIEW.contains(mouse) && !Mouse.isButtonDown(0)) {
            currenttip.add(I18n.format("nei.modularmachinery.structure.hint.rotate"));
            currenttip.add(I18n.format("nei.modularmachinery.structure.hint.zoom"));
            currenttip.add(I18n.format("nei.modularmachinery.structure.hint.reset"));
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
        for (Map.Entry<PositionedStack, List<String>> e : s.upgradeTips.entrySet()) {
            if (gui.isMouseOver(e.getKey(), recipe)) {
                currenttip.addAll(e.getValue());
                return currenttip;
            }
        }
        for (PositionedStack block : s.blocks) {
            if (gui.isMouseOver(block, recipe) && block.items.length > 1) {
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
