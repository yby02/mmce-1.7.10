package hellfirepvp.modularmachinery.client.nei;

import codechicken.lib.gui.GuiDraw;
import com.github.bsideup.jabel.Desugar;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.NEIServerUtils;
import codechicken.nei.PositionedStack;
import codechicken.nei.item.ItemFluidDisplay;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.StackInfo;
import codechicken.nei.recipe.TemplateRecipeHandler;
import github.kasuminova.mmce.compat.ItemStacks;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.client.util.EnergyDisplayUtil;
import hellfirepvp.modularmachinery.client.util.MekanismGasRender;
import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.RecipeRegistry;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementEnergy;
import hellfirepvp.modularmachinery.common.crafting.requirement.jei.JEIComponentGas;
import hellfirepvp.modularmachinery.common.crafting.requirement.jei.JEIComponentGasPerTick;
import hellfirepvp.modularmachinery.common.crafting.tooltip.RequirementTip;
import hellfirepvp.modularmachinery.common.integration.recipe.RecipeLayoutPart;
import hellfirepvp.modularmachinery.common.lib.RegistriesMM;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fluids.FluidStack;
import org.lwjgl.opengl.GL11;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 1.7.10 移植：替代 1.12.2 的 JEI 分类 CategoryDynamicRecipe / DynamicRecipeWrapper，每台机器一个 NEI 配方页。
 * <p>
 * 版面算法与上游一致：统计该机器全部配方中每种输入/输出组件（物品、流体、能量……）的最大数量，按组件排序依次排布输入、
 * 进度箭头、输出，下方绘制需求提示（RequirementTip）与配方提示。物品与流体都是 NEI 栏位（流体使用 NEI 的流体显示物品），
 * 因此可以对它们按 R / U 查询；能量绘制为能量条并带悬停提示。
 * <p>
 * Mekanism 气体（装有 Mekanism 时）：1.7.10 的 Mekanism 没有气体显示物品，气体格由本页自绘（图标 + 数量），悬停显示气体名与数量；
 * 在气体格上按 R / U 或左 / 右键点击按 Mekanism 的约定发起 {@code "gas"} 查询，本页也响应 {@code "gas"} 查询
 * （因此在 Mekanism 机器 GUI 的气体槽上按 R / U 同样能查到 MMCE 配方）；对气体对应流体的 NEI 流体显示物品按 R / U 也会匹配。
 * 每 tick 气体需求（gas_pertick，只能由脚本创建）同样自绘为气体格，数量为每 tick 的量，悬停提示中给出“每 Tick 消耗 / 产出”。
 */
public class MachineRecipeHandler extends TemplateRecipeHandler {

    static final String ICONS = ModularMachinery.MODID + ":textures/gui/jeirecipeicons_ce.png";
    /** Mekanism 1.7.10 的 NEI 气体查询 ID（与 NEIGasSupport.GAS_ID 相同；此处单独定义，避免未装 Mekanism 时加载 NEIGasSupport）。 */
    static final String GAS_ID = "gas";

    private final DynamicMachine machine;
    private Layout layout;
    private int layoutRecipeCount = -1;

    public MachineRecipeHandler(DynamicMachine machine) {
        this.machine = machine;
    }

    public DynamicMachine getMachine() {
        return machine;
    }

    public static String handlerIdFor(DynamicMachine machine) {
        return ModularMachinery.MODID + ".recipe." + machine.getRegistryName().getPath();
    }

    // ---------------- 基本信息 ----------------

    @Override
    public TemplateRecipeHandler newInstance() {
        return new MachineRecipeHandler(machine);
    }

    @Override
    public String getRecipeName() {
        return machine.getLocalizedName();
    }

    @Override
    public String getOverlayIdentifier() {
        return handlerIdFor(machine);
    }

    @Override
    public String getHandlerId() {
        return handlerIdFor(machine);
    }

    @Override
    public String getGuiTexture() {
        return ICONS;
    }

    @Override
    public int getRecipeHeight(int recipe) {
        return layout().height + 6;
    }

    @Override
    public int recipiesPerPage() {
        return Math.max(1, 130 / Math.max(1, layout().height + 4));
    }

    // ---------------- 版面 ----------------

    private static Iterable<MachineRecipe> visibleRecipes(DynamicMachine machine) {
        List<MachineRecipe> out = new ArrayList<>();
        for (MachineRecipe recipe : RecipeRegistry.getRecipesFor(machine)) {
            if (recipe.getLoadJEI()) {
                out.add(recipe);
            }
        }
        return out;
    }

    Layout layout() {
        int count = 0;
        for (MachineRecipe ignored : visibleRecipes(machine)) {
            count++;
        }
        if (layout == null || count != layoutRecipeCount) {
            layout = Layout.build(visibleRecipes(machine));
            layoutRecipeCount = count;
        }
        return layout;
    }

    /** 某台机器全部配方共用的版面（与上游 CategoryDynamicRecipe#buildRecipeComponents 相同的算法）。 */
    static final class Layout {
        final Map<IOType, Map<Class<?>, List<RecipeLayoutPart<?>>>> parts = new EnumMap<>(IOType.class);
        final List<RecipeLayoutPart<?>> energyParts = new ArrayList<>();
        Rectangle arrow;
        int width;
        int height;
        int partsHeight;

        static Layout build(Iterable<MachineRecipe> recipes) {
            Layout layout = new Layout();
            Map<IOType, Map<Class<?>, Integer>> counts = new EnumMap<>(IOType.class);
            Map<Class<?>, ComponentRequirement.JEIComponent<?>> found = new LinkedHashMap<>();
            int longestTooltip = 0;
            int widestTooltip = 0;
            for (MachineRecipe recipe : recipes) {
                Map<IOType, Map<Class<?>, Integer>> temp = new EnumMap<>(IOType.class);
                for (ComponentRequirement<?, ?> req : recipe.getCraftingRequirements()) {
                    ComponentRequirement.JEIComponent<?> comp = req.provideJEIComponent();
                    if (comp == null) {
                        continue;
                    }
                    temp.computeIfAbsent(req.getActionType(), t -> new HashMap<>()).merge(comp.getJEIRequirementClass(), 1, Integer::sum);
                    found.putIfAbsent(comp.getJEIRequirementClass(), comp);
                }
                temp.forEach((io, map) -> map.forEach((clazz, amount) ->
                    counts.computeIfAbsent(io, t -> new HashMap<>()).merge(clazz, amount, Math::max)));
                int tipLength = 0;
                for (List<String> tip : tooltipBlocks(recipe)) {
                    for (String line : tip) {
                        widestTooltip = Math.max(widestTooltip, GuiDraw.getStringWidth(line));
                    }
                    tipLength += RequirementTip.LINE_HEIGHT * tip.size() + RequirementTip.SPLIT_HEIGHT;
                }
                longestTooltip = Math.max(longestTooltip, tipLength);
            }

            int offsetX = 4;
            int highestY = 0;
            List<Class<?>> classes = new ArrayList<>(found.keySet());
            classes.sort((a, b) -> found.get(b).getTemplateLayout().getComponentHorizontalSortingOrder()
                - found.get(a).getTemplateLayout().getComponentHorizontalSortingOrder());
            int[] cursor = {offsetX, highestY};
            layoutSide(layout, IOType.INPUT, classes, found, counts, cursor);
            offsetX = cursor[0] + 4;
            highestY = cursor[1];
            int arrowX = offsetX;
            offsetX += 22 + 4;
            classes.sort((a, b) -> found.get(a).getTemplateLayout().getComponentHorizontalSortingOrder()
                - found.get(b).getTemplateLayout().getComponentHorizontalSortingOrder());
            cursor[0] = offsetX;
            layoutSide(layout, IOType.OUTPUT, classes, found, counts, cursor);
            offsetX = cursor[0];
            highestY = Math.max(highestY, cursor[1]);

            layout.arrow = new Rectangle(arrowX, Math.max(0, highestY / 4), 22, 15);
            layout.partsHeight = highestY;
            layout.height = highestY + longestTooltip;
            layout.width = Math.max(offsetX, widestTooltip + 4);
            return layout;
        }

        private static void layoutSide(Layout layout, IOType io, List<Class<?>> classes,
                                       Map<Class<?>, ComponentRequirement.JEIComponent<?>> found,
                                       Map<IOType, Map<Class<?>, Integer>> counts, int[] cursor) {
            Map<Class<?>, Integer> amounts = counts.get(io);
            if (amounts == null) {
                return;
            }
            int offsetX = cursor[0];
            int highestY = cursor[1];
            for (Class<?> clazz : classes) {
                Integer amount = amounts.get(clazz);
                if (amount == null) {
                    continue;
                }
                RecipeLayoutPart<?> template = found.get(clazz).getTemplateLayout();
                int partX = offsetX;
                int startX = offsetX;
                int partY = 0;
                for (int i = 0; i < amount; i++) {
                    if (i > 0 && i % template.getMaxHorizontalCount(amount) == 0) {
                        partY += template.getComponentHeight() + template.getComponentVerticalGap();
                        partX = startX;
                    }
                    RecipeLayoutPart<?> part = found.get(clazz).getLayoutPart(new Point(partX, partY));
                    layout.parts.computeIfAbsent(io, t -> new HashMap<>()).computeIfAbsent(clazz, c -> new ArrayList<>()).add(part);
                    if (part instanceof RecipeLayoutPart.Energy) {
                        layout.energyParts.add(part);
                    }
                    partX += template.getComponentWidth() + template.getComponentHorizontalGap();
                    offsetX = Math.max(offsetX, partX);
                    highestY = Math.max(highestY, partY + template.getComponentHeight());
                }
            }
            cursor[0] = offsetX;
            cursor[1] = highestY;
        }
    }

    /** 上游 drawInfo 中的提示块：各 RequirementTip 一块，最后是配方自身的提示。 */
    static List<List<String>> tooltipBlocks(MachineRecipe recipe) {
        List<List<String>> blocks = new ArrayList<>();
        for (RequirementTip tip : RegistriesMM.REQUIREMENT_TIPS_REGISTRY) {
            Collection<ComponentRequirement<?, ?>> filtered = tip.filterRequirements(recipe, recipe.getCraftingRequirements());
            if (!filtered.isEmpty()) {
                List<String> lines = tip.buildTooltip(recipe, filtered);
                if (!lines.isEmpty()) {
                    blocks.add(lines);
                }
            }
        }
        List<String> own = recipe.getFormattedTooltip();
        if (!own.isEmpty()) {
            blocks.add(own);
        }
        return blocks;
    }

    // ---------------- 配方缓存 ----------------

    public class CachedMachineRecipe extends CachedRecipe {
        final MachineRecipe recipe;
        final List<PositionedStack> inputs = new ArrayList<>();
        final List<PositionedStack> outputs = new ArrayList<>();
        final Map<PositionedStack, SlotInfo> slotInfo = new IdentityHashMap<>();
        final List<GasSlot> gasSlots = new ArrayList<>();
        /** 几率小于 100% 的格子（副产物等），绘制时在格子左上角标注几率。 */
        final List<ChanceMark> chanceMarks = new ArrayList<>();
        /** gugu-utils 环境传感器的检测条件（自绘图标 + 悬停说明）。 */
        final List<EnvSlot> envSlots = new ArrayList<>();
        final List<List<String>> tooltips;
        long energyIn;
        long energyOut;

        CachedMachineRecipe(MachineRecipe recipe) {
            this.recipe = recipe;
            Layout layout = layout();
            Map<IOType, Map<Class<?>, Integer>> used = new EnumMap<>(IOType.class);
            for (ComponentRequirement<?, ?> req : recipe.getCraftingRequirements()) {
                if (req instanceof RequirementEnergy energy) {
                    if (req.getActionType() == IOType.INPUT) {
                        energyIn += energy.getRequiredEnergyPerTick();
                    } else {
                        energyOut += energy.getRequiredEnergyPerTick();
                    }
                    continue;
                }
                ComponentRequirement.JEIComponent<?> comp = req.provideJEIComponent();
                if (comp == null) {
                    continue;
                }
                req.initializeJEIRequirements();
                Class<?> clazz = comp.getJEIRequirementClass();
                List<RecipeLayoutPart<?>> partsOfType = layout.parts.getOrDefault(req.getActionType(), new HashMap<>()).get(clazz);
                int index = used.computeIfAbsent(req.getActionType(), t -> new HashMap<>()).merge(clazz, 1, Integer::sum) - 1;
                if (partsOfType == null || index >= partsOfType.size()) {
                    continue;
                }
                Point offset = partsOfType.get(index).getOffset();
                if (req instanceof ComponentRequirement.ChancedRequirement chanced && chanced.getChance() < 1F) {
                    chanceMarks.add(new ChanceMark(offset.x + 1, offset.y + 1, chanced.getChance()));
                }
                if (comp instanceof com.warmthdawn.mod.gugu_utils.jei.JEIComponentEnvironment env) {
                    envSlots.add(new EnvSlot(offset.x + 1, offset.y + 1, env.getType()));
                    continue;
                }
                if (Mods.MEKANISM.isPresent() && isGasComponent(comp)) {
                    List<?> gases = comp.getJEIIORequirements();
                    if (!gases.isEmpty()) {
                        gasSlots.add(new GasSlot(offset.x + 1, offset.y + 1, gases.get(0), comp, req.getActionType() == IOType.INPUT));
                    }
                    continue;
                }
                List<ItemStack> displayStacks = toDisplayStacks(comp.getJEIIORequirements());
                if (displayStacks.isEmpty()) {
                    continue;
                }
                PositionedStack stack = new PositionedStack(displayStacks, offset.x + 1, offset.y + 1, true);
                slotInfo.put(stack, new SlotInfo(comp, req.getActionType() == IOType.INPUT));
                (req.getActionType() == IOType.INPUT ? inputs : outputs).add(stack);
            }
            this.tooltips = tooltipBlocks(recipe);
        }

        @Override
        public List<PositionedStack> getIngredients() {
            return getCycledIngredients(cycleticks / 20, inputs);
        }

        @Override
        public PositionedStack getResult() {
            return null;
        }

        @Override
        public List<PositionedStack> getOtherStacks() {
            return getCycledIngredients(cycleticks / 20, outputs);
        }

        boolean producesItem(ItemStack result) {
            return matches(outputs, false, result);
        }

        boolean consumesItem(ItemStack ingredient) {
            return matches(inputs, true, ingredient);
        }

        /** 是否有与 gas 同种气体的输入（input=true）或输出格；只在装有 Mekanism 时调用。 */
        boolean hasGas(Object gas, boolean input) {
            for (GasSlot slot : gasSlots) {
                if (slot.input() == input && NEIGasSupport.sameGas(slot.gas(), gas)) {
                    return true;
                }
            }
            return false;
        }

        private boolean matches(List<PositionedStack> stacks, boolean input, ItemStack query) {
            FluidStack queryFluid = StackInfo.getFluid(query);
            for (PositionedStack stack : stacks) {
                for (ItemStack candidate : stack.items) {
                    if (queryFluid != null) {
                        FluidStack fluid = StackInfo.getFluid(candidate);
                        if (fluid != null && fluid.getFluid() == queryFluid.getFluid()) {
                            return true;
                        }
                    } else if (NEIServerUtils.areStacksSameTypeCrafting(candidate, query)) {
                        return true;
                    }
                }
            }
            if (queryFluid != null && !gasSlots.isEmpty() && Mods.MEKANISM.isPresent()) {
                for (GasSlot slot : gasSlots) {
                    if (slot.input() == input && NEIGasSupport.matchesFluid(slot.gas(), queryFluid)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /** 气体需求（gas 与 gas_pertick）的 JEI 部件；只在装有 Mekanism 时调用。 */
    private static boolean isGasComponent(ComponentRequirement.JEIComponent<?> comp) {
        return comp instanceof JEIComponentGas || comp instanceof JEIComponentGasPerTick;
    }

    @Desugar
    record SlotInfo(ComponentRequirement.JEIComponent<?> component, boolean input) {
    }

    /** 环境检测条件的格子：左上角坐标（16×16 图标区）与条件。 */
    @Desugar
    record EnvSlot(int x, int y, com.warmthdawn.mod.gugu_utils.modularmachenary.environment.envtypes.EnvironmentType type) {
        boolean contains(Point mouse) {
            return mouse.x >= x && mouse.x < x + 16 && mouse.y >= y && mouse.y < y + 16;
        }
    }

    /** 格子左上角（16×16 图标区）坐标与几率。 */
    @Desugar
    record ChanceMark(int x, int y, float chance) {
        String text() {
            return chanceText(chance);
        }
    }

    /** 几率文字：整数百分比不带小数，否则保留 1 位（不足 1% 保留 2 位）；为 0 时显示 0%，过小时显示 <0.01%。 */
    static String chanceText(float chance) {
        if (chance <= 0F) {
            return "0%";
        }
        if (chance < 0.0001F) {
            return "<0.01%";
        }
        float percent = chance * 100F;
        if (Math.abs(percent - Math.round(percent)) < 0.005F) {
            return Math.round(percent) + "%";
        }
        return String.format(java.util.Locale.ROOT, percent < 1F ? "%.2f%%" : "%.1f%%", percent);
    }

    /** 自绘的气体格：左上角坐标（16×16 图标区）、GasStack（以 Object 保存，避免本类直接引用 Mekanism 类型）。 */
    @Desugar
    record GasSlot(int x, int y, Object gas, ComponentRequirement.JEIComponent<?> component, boolean input) {
        boolean contains(Point mouse) {
            return mouse.x >= x && mouse.x < x + 16 && mouse.y >= y && mouse.y < y + 16;
        }
    }

    private static List<ItemStack> toDisplayStacks(List<?> values) {
        List<ItemStack> out = new ArrayList<>();
        for (Object value : values) {
            if (value instanceof ItemStack stack && !ItemStacks.isEmpty(stack)) {
                out.add(stack.copy());
            } else if (value instanceof FluidStack fluid && fluid.getFluid() != null) {
                ItemStack display = ItemFluidDisplay.createStack(fluid);
                if (display != null) {
                    out.add(display);
                }
            }
        }
        return out;
    }

    // ---------------- 查询 ----------------

    @Override
    public void loadCraftingRecipes(String outputId, Object... results) {
        if (getOverlayIdentifier().equals(outputId)) {
            for (MachineRecipe recipe : visibleRecipes(machine)) {
                arecipes.add(new CachedMachineRecipe(recipe));
            }
            return;
        }
        if (GAS_ID.equals(outputId)) {
            loadGasRecipes(results, false);
            return;
        }
        super.loadCraftingRecipes(outputId, results);
    }

    @Override
    public void loadUsageRecipes(String inputId, Object... ingredients) {
        if (GAS_ID.equals(inputId)) {
            loadGasRecipes(ingredients, true);
            return;
        }
        super.loadUsageRecipes(inputId, ingredients);
    }

    private void loadGasRecipes(Object[] query, boolean usage) {
        if (!Mods.MEKANISM.isPresent() || query.length == 0 || !NEIGasSupport.isGas(query[0])) {
            return;
        }
        for (MachineRecipe recipe : visibleRecipes(machine)) {
            CachedMachineRecipe cached = new CachedMachineRecipe(recipe);
            if (cached.hasGas(query[0], usage)) {
                arecipes.add(cached);
            }
        }
    }

    @Override
    public void loadCraftingRecipes(ItemStack result) {
        for (MachineRecipe recipe : visibleRecipes(machine)) {
            CachedMachineRecipe cached = new CachedMachineRecipe(recipe);
            if (cached.producesItem(result)) {
                arecipes.add(cached);
            }
        }
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        boolean catalyst = isCatalyst(ingredient);
        for (MachineRecipe recipe : visibleRecipes(machine)) {
            CachedMachineRecipe cached = new CachedMachineRecipe(recipe);
            if (catalyst || cached.consumesItem(ingredient)) {
                arecipes.add(cached);
            }
        }
    }

    private boolean isCatalyst(ItemStack stack) {
        BlockController controller = BlockController.getControllerWithMachine(machine);
        return controller != null && stack != null && stack.getItem() == Item.getItemFromBlock(controller);
    }

    // ---------------- 绘制 ----------------

    @Override
    public void drawBackground(int recipe) {
        Layout layout = layout();
        GL11.glColor4f(1F, 1F, 1F, 1F);
        GuiDraw.changeTexture(ICONS);
        for (Map<Class<?>, List<RecipeLayoutPart<?>>> byClass : layout.parts.values()) {
            for (List<RecipeLayoutPart<?>> list : byClass.values()) {
                for (RecipeLayoutPart<?> part : list) {
                    Point p = part.getOffset();
                    if (part instanceof RecipeLayoutPart.Energy) {
                        GuiDraw.drawTexturedModalRect(p.x, p.y, 36, 0, 18, 54);
                    } else {
                        GuiDraw.drawTexturedModalRect(p.x, p.y, 54, 0, 18, 18);
                    }
                }
            }
        }
        GuiDraw.drawTexturedModalRect(layout.arrow.x, layout.arrow.y, 72, 0, 22, 15);
    }

    @Override
    public void drawExtras(int recipe) {
        CachedMachineRecipe cached = (CachedMachineRecipe) arecipes.get(recipe);
        Layout layout = layout();
        GL11.glColor4f(1F, 1F, 1F, 1F);
        GuiDraw.changeTexture(ICONS);
        // 进度箭头动画
        int total = Math.max(1, cached.recipe.getRecipeTotalTickTime());
        int px = (int) Math.ceil((cycleticks % total) / (float) total * 22);
        GuiDraw.drawTexturedModalRect(layout.arrow.x, layout.arrow.y, 72, 15, px, 15);
        // 有能量需求时点亮能量条（输入、输出各自的能量部件）
        for (Map.Entry<IOType, Map<Class<?>, List<RecipeLayoutPart<?>>>> e : layout.parts.entrySet()) {
            long energy = e.getKey() == IOType.INPUT ? cached.energyIn : cached.energyOut;
            List<RecipeLayoutPart<?>> energyParts = e.getValue().get(Long.class);
            if (energy > 0 && energyParts != null) {
                for (RecipeLayoutPart<?> part : energyParts) {
                    GuiDraw.drawTexturedModalRect(part.getOffset().x, part.getOffset().y, 18, 0, 18, 54);
                }
            }
        }
        // 气体格：图标 + 右下角数量（半尺寸字体）
        if (!cached.gasSlots.isEmpty()) {
            for (GasSlot gas : cached.gasSlots) {
                MekanismGasRender.drawGasIcon(GuiDraw.gui, gas.gas(), gas.x(), gas.y());
                GL11.glPushMatrix();
                GL11.glTranslatef(gas.x() + 16, gas.y() + 12, 300F);
                GL11.glScalef(0.5F, 0.5F, 1F);
                GuiDraw.drawStringR(NEIGasSupport.amountText(gas.gas()), 0, 0, 0xFFFFFF, true);
                GL11.glPopMatrix();
            }
            GL11.glColor4f(1F, 1F, 1F, 1F);
        }
        // 环境检测条件（gugu-utils 环境传感器）：图标同原模组
        for (EnvSlot env : cached.envSlots) {
            com.warmthdawn.mod.gugu_utils.jei.EnvironmentIconRenderer.draw(env.type(), env.x(), env.y());
        }
        if (!cached.envSlots.isEmpty()) {
            GL11.glColor4f(1F, 1F, 1F, 1F);
            GuiDraw.changeTexture(ICONS);
        }
        // 几率小于 100% 的格子：左上角用半尺寸黄色字标注几率（右下角是数量），悬停提示中另有完整说明
        if (!cached.chanceMarks.isEmpty()) {
            for (ChanceMark mark : cached.chanceMarks) {
                GL11.glPushMatrix();
                GL11.glTranslatef(mark.x(), mark.y(), 300F);
                GL11.glScalef(0.5F, 0.5F, 1F);
                GuiDraw.drawString(mark.text(), 0, 0, 0xFFFF55, true);
                GL11.glPopMatrix();
            }
            GL11.glColor4f(1F, 1F, 1F, 1F);
        }
        // 提示文字（自下而上排布，与上游一致）
        int offsetY = layout.height;
        for (List<String> block : cached.tooltips) {
            offsetY -= RequirementTip.LINE_HEIGHT * block.size() + RequirementTip.SPLIT_HEIGHT;
        }
        offsetY += RequirementTip.SPLIT_HEIGHT;
        for (List<String> block : cached.tooltips) {
            for (String line : block) {
                GuiDraw.drawString(line, 4, offsetY, 0xFFFFFF, true);
                offsetY += RequirementTip.LINE_HEIGHT;
            }
            offsetY += RequirementTip.SPLIT_HEIGHT;
        }
        GL11.glColor4f(1F, 1F, 1F, 1F);
    }

    @Override
    public List<String> handleTooltip(GuiRecipe<?> gui, List<String> currenttip, int recipe) {
        if (!currenttip.isEmpty() || recipe >= arecipes.size()) {
            return currenttip;
        }
        CachedMachineRecipe cached = (CachedMachineRecipe) arecipes.get(recipe);
        Point mouse = gui.getRecipeMousePosition(recipe);
        Layout layout = layout();
        if (layout.arrow.contains(mouse)) {
            int ticks = cached.recipe.getRecipeTotalTickTime();
            currenttip.add(I18n.format("tooltip.machinery.duration.seconds", ticks / 20F) + I18n.format("tooltip.machinery.duration.tick", ticks));
            return currenttip;
        }
        GasSlot gas = hoveredGas(cached, mouse);
        if (gas != null) {
            addGasTooltip(gas, currenttip);
            return currenttip;
        }
        for (EnvSlot env : cached.envSlots) {
            if (env.contains(mouse)) {
                currenttip.addAll(env.type().getTooltip());
                return currenttip;
            }
        }
        for (Map.Entry<IOType, Map<Class<?>, List<RecipeLayoutPart<?>>>> e : layout.parts.entrySet()) {
            List<RecipeLayoutPart<?>> energyParts = e.getValue().get(Long.class);
            if (energyParts == null) {
                continue;
            }
            long energy = e.getKey() == IOType.INPUT ? cached.energyIn : cached.energyOut;
            for (RecipeLayoutPart<?> part : energyParts) {
                Rectangle area = new Rectangle(part.getOffset().x, part.getOffset().y, 18, 54);
                if (energy > 0 && area.contains(mouse)) {
                    String unit = I18n.format(EnergyDisplayUtil.type.getUnlocalizedFormat());
                    long perTick = EnergyDisplayUtil.type.formatEnergyForDisplay(energy);
                    long totalEnergy = EnergyDisplayUtil.type.formatEnergyForDisplay(energy * cached.recipe.getRecipeTotalTickTime());
                    currenttip.add((e.getKey() == IOType.INPUT ? EnumChatFormatting.RED : EnumChatFormatting.GREEN)
                        + MiscUtils.formatNumber(perTick) + " " + unit + "/t");
                    currenttip.add(EnumChatFormatting.GRAY + MiscUtils.formatNumber(totalEnergy) + " " + unit);
                    return currenttip;
                }
            }
        }
        return currenttip;
    }

    private static void addGasTooltip(GasSlot gas, List<String> tooltip) {
        NEIGasSupport.addTooltip(gas.gas(), tooltip);
        addComponentTooltip(gas.component(), gas.input(), gas.gas(), tooltip);
    }

    /** 开发期自动化（客户端场景 p4）用：第 recipe 条配方中每个气体格的悬停提示（与鼠标悬停时相同），前缀 in / out。 */
    public List<String> describeGasSlots(int recipe) {
        List<String> out = new ArrayList<>();
        if (recipe < 0 || recipe >= arecipes.size()) {
            return out;
        }
        for (GasSlot gas : ((CachedMachineRecipe) arecipes.get(recipe)).gasSlots) {
            List<String> tooltip = new ArrayList<>();
            addGasTooltip(gas, tooltip);
            out.add((gas.input() ? "in " : "out ") + String.join(" | ", tooltip));
        }
        return out;
    }

    /** 开发期自动化（客户端场景 gugu）用：第 recipe 条配方中每个环境格的条件名与悬停说明（“名称: 行1 | 行2”）。 */
    public List<String> describeEnvSlots(int recipe) {
        List<String> out = new ArrayList<>();
        if (recipe < 0 || recipe >= arecipes.size()) {
            return out;
        }
        for (EnvSlot env : ((CachedMachineRecipe) arecipes.get(recipe)).envSlots) {
            out.add(env.type().getName() + ": " + String.join(" | ", env.type().getTooltip()));
        }
        return out;
    }

    /** 开发期自动化用：第 recipe 条配方第 slot 个环境图标的悬停提示（与 handleTooltip 追加的内容相同）。 */
    public List<String> envSlotTooltip(int recipe, int slot) {
        if (recipe < 0 || recipe >= arecipes.size()) {
            return new ArrayList<>();
        }
        List<EnvSlot> slots = ((CachedMachineRecipe) arecipes.get(recipe)).envSlots;
        return slot < 0 || slot >= slots.size() ? new ArrayList<>() : new ArrayList<>(slots.get(slot).type().getTooltip());
    }

    /** 开发期自动化（客户端场景 p4）用：第 recipe 条配方中几率标注的文字，以及每个物品 / 流体格悬停时由组件追加的提示（前缀 in / out）。 */
    public List<String> describeChances(int recipe) {
        List<String> out = new ArrayList<>();
        if (recipe < 0 || recipe >= arecipes.size()) {
            return out;
        }
        CachedMachineRecipe cached = (CachedMachineRecipe) arecipes.get(recipe);
        for (ChanceMark mark : cached.chanceMarks) {
            out.add("mark " + mark.text());
        }
        for (Map.Entry<PositionedStack, SlotInfo> e : cached.slotInfo.entrySet()) {
            List<String> tip = new ArrayList<>();
            ItemStack stack = e.getKey().items[0];
            Object ingredient = StackInfo.isFluidDisplayItem(stack) ? StackInfo.getFluid(stack) : stack;
            try {
                addComponentTooltip(e.getValue().component(), e.getValue().input(), ingredient, tip);
            } catch (ClassCastException ignored) {
            }
            if (!tip.isEmpty()) {
                out.add((e.getValue().input() ? "in " : "out ") + stack.getDisplayName() + ": " + String.join(" | ", tip));
            }
        }
        return out;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void addComponentTooltip(ComponentRequirement.JEIComponent component, boolean input, Object ingredient, List<String> tooltip) {
        component.onJEIHoverTooltip(0, input, ingredient, tooltip);
    }

    private static GasSlot hoveredGas(CachedMachineRecipe cached, Point mouse) {
        for (GasSlot gas : cached.gasSlots) {
            if (gas.contains(mouse)) {
                return gas;
            }
        }
        return null;
    }

    /**
     * 光标位于第 recipe 条配方的 mouse 处（相对配方区域）时按 R / 左键（usage = false）或 U / 右键（usage = true）：
     * 命中气体格则按 Mekanism 的约定发起气体查询。keyTyped / mouseClicked 与自动化测试共用。
     */
    public boolean lookupGasAt(int recipe, Point mouse, boolean usage) {
        if (recipe < 0 || recipe >= arecipes.size()) {
            return false;
        }
        GasSlot gas = hoveredGas((CachedMachineRecipe) arecipes.get(recipe), mouse);
        return gas != null && NEIGasSupport.open(gas.gas(), usage);
    }

    /** 第 recipe 条配方中第 index 个气体格的中心点（相对配方区域）；不存在时返回 null。 */
    public Point gasSlotCenter(int recipe, int index) {
        if (recipe < 0 || recipe >= arecipes.size()) {
            return null;
        }
        List<GasSlot> slots = ((CachedMachineRecipe) arecipes.get(recipe)).gasSlots;
        return index < 0 || index >= slots.size() ? null : new Point(slots.get(index).x() + 8, slots.get(index).y() + 8);
    }

    @Override
    public boolean keyTyped(GuiRecipe<?> gui, char keyChar, int keyCode, int recipe) {
        if (NEIClientConfig.isKeyHashDown("gui.recipe") && lookupGasAt(recipe, gui.getRecipeMousePosition(recipe), false)) {
            return true;
        }
        if (NEIClientConfig.isKeyHashDown("gui.usage") && lookupGasAt(recipe, gui.getRecipeMousePosition(recipe), true)) {
            return true;
        }
        return super.keyTyped(gui, keyChar, keyCode, recipe);
    }

    @Override
    public boolean mouseClicked(GuiRecipe<?> gui, int button, int recipe) {
        if ((button == 0 || button == 1) && lookupGasAt(recipe, gui.getRecipeMousePosition(recipe), button == 1)) {
            return true;
        }
        return super.mouseClicked(gui, button, recipe);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<String> handleItemTooltip(GuiRecipe<?> gui, ItemStack stack, List<String> currenttip, int recipe) {
        if (stack == null || recipe >= arecipes.size()) {
            return currenttip;
        }
        CachedMachineRecipe cached = (CachedMachineRecipe) arecipes.get(recipe);
        for (Map.Entry<PositionedStack, SlotInfo> e : cached.slotInfo.entrySet()) {
            if (gui.isMouseOver(e.getKey(), recipe)) {
                ComponentRequirement.JEIComponent component = e.getValue().component();
                Object ingredient = StackInfo.isFluidDisplayItem(stack) ? StackInfo.getFluid(stack) : stack;
                try {
                    component.onJEIHoverTooltip(0, e.getValue().input(), ingredient, currenttip);
                } catch (ClassCastException ignored) {
                    // 组件期望的类型与显示物品不同时不追加提示
                }
                break;
            }
        }
        return currenttip;
    }
}
