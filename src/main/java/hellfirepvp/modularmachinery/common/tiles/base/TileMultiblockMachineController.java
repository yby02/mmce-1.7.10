package hellfirepvp.modularmachinery.common.tiles.base;

import github.kasuminova.mmce.compat.ItemStacks;

import github.kasuminova.mmce.compat.Worlds;

import com.mojang.authlib.GameProfile;
import github.kasuminova.mmce.compat.crt.IBlockStateMatcher;
import minetweaker.api.minecraft.MineTweakerMC;
import minetweaker.api.data.IData;
import minetweaker.api.item.IItemStack;
import github.kasuminova.mmce.compat.crt.CraftTweakerMC;
import minetweaker.api.player.IPlayer;
import github.kasuminova.mmce.compat.crt.IBlockPos;
import github.kasuminova.mmce.compat.crt.IFacing;
import github.kasuminova.mmce.compat.crt.IWorld;
import github.kasuminova.mmce.common.event.Phase;
import github.kasuminova.mmce.common.event.machine.MachineStructureFormedEvent;
import github.kasuminova.mmce.common.event.machine.MachineStructureUpdateEvent;
import github.kasuminova.mmce.common.event.machine.MachineTickEvent;
import github.kasuminova.mmce.common.event.recipe.RecipeCheckEvent;
import github.kasuminova.mmce.common.helper.IBlockStatePredicate;
import github.kasuminova.mmce.common.helper.IDynamicPatternInfo;
import github.kasuminova.mmce.common.helper.IMachineController;
import github.kasuminova.mmce.common.machine.component.MachineComponentProxyRegistry;
import github.kasuminova.mmce.common.tile.base.MachineCombinationComponent;
import github.kasuminova.mmce.common.upgrade.DynamicMachineUpgrade;
import github.kasuminova.mmce.common.upgrade.MachineUpgrade;
import github.kasuminova.mmce.common.upgrade.UpgradeType;
import github.kasuminova.mmce.common.util.DynamicPattern;
import github.kasuminova.mmce.common.util.TimeRecorder;
import github.kasuminova.mmce.common.util.concurrent.ActionExecutor;
import github.kasuminova.mmce.common.world.MMWorldEventListener;
import github.kasuminova.mmce.common.world.MachineComponentManager;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.block.BlockStatedMachineComponent;
import hellfirepvp.modularmachinery.common.block.prop.WorkingState;
import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentSelectorTag;
import hellfirepvp.modularmachinery.common.crafting.helper.CraftingStatus;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.item.ItemBlueprint;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import hellfirepvp.modularmachinery.common.machine.TaggedPositionBlockArray;
import hellfirepvp.modularmachinery.common.modifier.MultiBlockModifierReplacement;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.modifier.SingleBlockModifierReplacement;
import hellfirepvp.modularmachinery.common.tiles.TileParallelController;
import hellfirepvp.modularmachinery.common.tiles.TileSmartInterface;
import hellfirepvp.modularmachinery.common.tiles.TileUpgradeBus;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import hellfirepvp.modularmachinery.common.util.BlockArrayCache;
import hellfirepvp.modularmachinery.common.util.IOInventory;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import hellfirepvp.modularmachinery.common.util.SmartInterfaceData;
import hellfirepvp.modularmachinery.common.util.SmartInterfaceType;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.block.Block;
import github.kasuminova.mmce.compat.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import github.kasuminova.mmce.compat.EnumFacing;
import github.kasuminova.mmce.compat.ResourceLocation;
import net.minecraft.util.AxisAlignedBB;
import github.kasuminova.mmce.compat.BlockPos;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.oredict.OreDictionary;
import stanhebben.zenscript.annotations.ZenMethod;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.IntStream;

/**
 * 多方块机械控制器的公共实现（结构检查、组件扫描、修改器/升级、CrT 接口、NBT）。
 * <p>
 * 1.7.10 移植：
 * - GeckoLib 控制器动画与 BlockModelHider（Multiblocked/组件模型隐藏）不移植（见 MIGRATION_PLAN 4.3）。
 * - ME 样板供应器相关逻辑（InfItemFluidHandler 去重、写入机器名）随 AE2 部件在 P5 移植。
 * - formed 状态不再写入方块状态（1.7.10 meta 只保存朝向），客户端渲染直接读取 {@link #isStructureFormed()}。
 */
@SuppressWarnings("unused")
public abstract class TileMultiblockMachineController extends TileEntityRestrictedTick implements SelectiveUpdateTileEntity, IMachineController {
    public static final int BLUEPRINT_SLOT = 0, ACCELERATOR_SLOT = 1;
    public static int structureCheckDelay = 30, maxStructureCheckDelay = 200;
    public static boolean delayedStructureCheck                 = true;
    public static boolean cleanCustomDataOnStructureCheckFailed = false;
    public static boolean enableSecuritySystem                  = false;
    public static boolean enableFullDataSync                    = false;

    public static int      usedTimeCache       = 0;
    public static int      searchUsedTimeCache = 0;
    public static WorkMode workModeCache       = WorkMode.ASYNC;

    protected final Map<String, List<RecipeModifier>> foundModifiers  = new ConcurrentHashMap<>();
    protected final Map<String, RecipeModifier>       customModifiers = new ConcurrentHashMap<>();

    protected final Map<TileSmartInterface.SmartInterfaceProvider, String>  foundSmartInterfaces     = new ConcurrentHashMap<>();
    protected final Map<String, List<MachineUpgrade>>                       foundUpgrades            = new ConcurrentHashMap<>();
    protected final List<TileUpgradeBus.UpgradeBusProvider>                 foundUpgradeBuses        = new ArrayList<>();
    protected final List<TileParallelController.ParallelControllerProvider> foundParallelControllers = new ArrayList<>();
    protected final Map<TileEntity, ProcessingComponent<?>>                 generalComponents        = new ConcurrentHashMap<>();
    protected final Map<Long, Map<TileEntity, ProcessingComponent<?>>>      foundComponents          = new ConcurrentHashMap<>();

    protected final TimeRecorder                             timeRecorder               = new TimeRecorder();
    protected       boolean                                  searchRecipeImmediately    = false;
    protected       EnumFacing                               controllerRotation         = null;
    protected       DynamicMachine.ModifierReplacementMap    foundReplacements          = null;
    protected       IOInventory                              inventory;
    protected       NBTTagCompound                           customData                 = new NBTTagCompound();
    protected       DynamicMachine                           prevMachine                = null;
    protected       DynamicMachine                           foundMachine               = null;
    protected       DynamicMachine                           parentMachine              = null;
    protected       TaggedPositionBlockArray                 foundPattern               = null;
    protected       Map<String, DynamicPattern.Status>       foundDynamicPatterns       = new HashMap<>();
    protected       ActionExecutor                           tickExecutor               = null;
    protected       WorkMode                                 workMode                   = WorkMode.ASYNC;
    protected       UUID                                     owner                      = null;
    protected       int                                      structureCheckCounter      = 0;
    protected       int                                      recipeResearchRetryCounter = 0;
    protected       int                                      lastStrongPower            = -1;
    protected       int                                      lastStructureCheckTick     = -1;
    protected       long                                     executeGroupId             = -1;
    protected       Object                                   animationFactory           = null;
    protected       boolean                                  loaded                     = false;

    public TileMultiblockMachineController() {
        this.inventory = buildInventory();
        this.inventory.setStackLimit(1, BLUEPRINT_SLOT);
    }

    public static void loadFromConfig(Configuration config) {
        //最短结构检查间隔
        structureCheckDelay = config.getInt("structure-check-delay", "general",
            30, 1, 1200,
            "The multiblock structure checks the structural integrity at how often? (TimeUnit: Tick)");
        //延迟结构检查
        delayedStructureCheck = config.getBoolean("delayed-structure-check", "general",
            true, "When enabled, the structure check interval in the idle state is incrementally increased to ease the performance footprint.");
        //最长结构检查间隔
        maxStructureCheckDelay = config.getInt("max-structure-check-delay", "general",
            100, 2, 1200,
            "When delayed-structure-check is enabled, what is the maximum check interval? (TimeUnit: Tick)");

        //检查最短结构检查间隔是否大于最长结构检查间隔
        if (structureCheckDelay >= maxStructureCheckDelay) {
            ModularMachinery.log.warn("structure-check-delay is bigger than or equal max-structure-check-delay!, use default value...");
            structureCheckDelay = 30;
            maxStructureCheckDelay = 100;
        }

        //当结构检查失败时，是否清空自定义数据
        cleanCustomDataOnStructureCheckFailed = config.getBoolean("clean-custom-data-on-structure-check-failed", "general",
            false, "When enabled, the customData will be cleared when multiblock structure check failed.");

        enableSecuritySystem = config.getBoolean("enable-security-system", "general", false,
            "When enabled, players using the controller will have their owner checked and non-owners will be denied access.");
        enableFullDataSync = config.getBoolean("enable-full-data-sync", "general", false,
            "When enabled, the controller sends the full NBT to the client at the start and completion of the recipe, which can be helpful for machinery where the client needs to perform special operations.");
    }

    public <T> void addComponent(MachineComponent<T> component, @Nullable ComponentSelectorTag tag, TileEntity te, Map<Long, Map<TileEntity, ProcessingComponent<?>>> components) {
        T handler = component.getContainerProvider();
        Long groupId = component.getGroupID();
        // 1.7.10 移植：ME 样板供应器的 InfItemFluidHandler 去重逻辑随 AE2 部件在 P5 恢复
        MachineComponentManager.INSTANCE.checkComponentShared(te, this);
        if (groupId < 0) generalComponents.put(te, new ProcessingComponent<>(component, handler, tag));
        else components.computeIfAbsent(
            groupId,
            i -> new ConcurrentHashMap<>()
        ).put(te, new ProcessingComponent<>(component, handler, tag));
    }

    @Override
    public final void doRestrictedTick() {
        if (getWorld().isRemote) {
            return;
        }
        timeRecorder.updateUsedTime(tickExecutor);

        final long tickStart = System.nanoTime();

        // Controller Tick
        doControllerTick();

        timeRecorder.incrementUsedTime((int) TimeUnit.MICROSECONDS.convert(System.nanoTime() - tickStart, TimeUnit.NANOSECONDS));
    }

    public abstract void doControllerTick();

    protected IOInventory buildInventory() {
        return (IOInventory) new IOInventory(this, new int[0], new int[0]).setMiscSlots(BLUEPRINT_SLOT);
    }

    protected int getStrongPower() {
        if (lastStrongPower == -1) {
            lastStrongPower = Worlds.getStrongPower(getWorld(), getPos());
        }
        return lastStrongPower;
    }

    public void onNeighborChange() {
        lastStrongPower = Worlds.getStrongPower(getWorld(), getPos());
    }

    public long getExecuteGroupId() {
        return executeGroupId;
    }

    public void setExecuteGroupId(final long executeGroupId) {
        this.executeGroupId = executeGroupId;
    }

    protected void addRecipeResearchUsedTime(int time) {
        timeRecorder.addRecipeResearchUsedTime(time);
    }

    public int usedTimeAvg() {
        return timeRecorder.usedTimeAvg();
    }

    public int recipeSearchUsedTimeAvg() {
        return timeRecorder.recipeSearchUsedTimeAvg();
    }

    public TimeRecorder getTimeRecorder() {
        return timeRecorder;
    }

    public boolean isSearchRecipeImmediately() {
        return searchRecipeImmediately;
    }

    public void setSearchRecipeImmediately(final boolean searchRecipeImmediately) {
        this.searchRecipeImmediately = searchRecipeImmediately;
    }

    public int getMaxParallelism() {
        int parallelism = foundMachine.getInternalParallelism();
        int maxParallelism = foundMachine.getMaxParallelism();
        for (TileParallelController.ParallelControllerProvider provider : foundParallelControllers) {
            parallelism += provider.getParallelism();

            if (parallelism >= maxParallelism) {
                return maxParallelism;
            }
        }
        return Math.max(1, parallelism);
    }

    @Nullable
    public DynamicMachine getFoundMachine() {
        return foundMachine;
    }

    /**
     * Only for preview, DO NOT USE THIS METHOD ON TRUE WORLD!
     */
    public void setFoundMachine(final DynamicMachine foundMachine) {
        this.foundMachine = foundMachine;
    }

    public TaggedPositionBlockArray getFoundPattern() {
        return foundPattern;
    }

    public boolean isParallelized() {
        if (foundMachine != null) {
            return foundMachine.isParallelizable() && getMaxParallelism() > 1;
        } else {
            return false;
        }
    }

    @Nullable
    public DynamicMachine getBlueprintMachine() {
        ItemStack blueprintSlotted = this.inventory.getStackInSlot(BLUEPRINT_SLOT);
        if (!ItemStacks.isEmpty(blueprintSlotted)) {
            return ItemBlueprint.getAssociatedMachine(blueprintSlotted);
        }
        return null;
    }

    public abstract CraftingStatus getControllerStatus();

    public abstract void setControllerStatus(CraftingStatus status);

    public IOInventory getInventory() {
        return inventory;
    }

    public EnumFacing getControllerRotation() {
        return controllerRotation;
    }

    protected boolean canCheckStructure() {
        if (lastStructureCheckTick == -1 || (isStructureFormed() && foundComponents.isEmpty())) {
            return true;
        }
        if (!delayedStructureCheck) {
            return ticksExisted % structureCheckDelay == 0;
        }
        if (isStructureFormed()) {
            if (ticksExisted % Math.min(structureCheckDelay + currentRecipeSearchDelay(), maxStructureCheckDelay) == 0) {
                return true;
            } else {
                BlockPos pos = getPos();
                BlockPos min = foundPattern.getMin();
                BlockPos max = foundPattern.getMax();
                return MMWorldEventListener.INSTANCE.isAreaChanged(getWorld(), pos.add(min), pos.add(max));
            }
        } else {
            return ticksExisted % Math.min(structureCheckDelay + this.structureCheckCounter * 5, maxStructureCheckDelay) == 0;
        }
    }

    public int currentRecipeSearchDelay() {
        return Math.min(20 + this.recipeResearchRetryCounter * 5, 100);
    }

    public boolean isStructureFormed() {
        return this.foundMachine != null && this.foundPattern != null;
    }

    protected void resetMachine(boolean clearData) {
        if (clearData) {
            setControllerStatus(CraftingStatus.MISSING_STRUCTURE);
            incrementStructureCheckCounter();
            resetRecipeSearchRetryCount();

            if (cleanCustomDataOnStructureCheckFailed) {
                customData = new NBTTagCompound();
                customModifiers.clear();
            }

            if (workMode == WorkMode.SYNC) {
                notifyStructureFormedState(false);
            } else {
                ModularMachinery.EXECUTE_MANAGER.addSyncTask(() -> notifyStructureFormedState(false));
            }
        }
        updateStatedMachineComponentSync(false);

        prevMachine = foundMachine;
        foundMachine = null;
        foundPattern = null;
        foundReplacements = null;
        foundDynamicPatterns.clear();
    }

    protected void resetRecipe() {
    }

    public void resetRecipeSearchRetryCount() {
        this.recipeResearchRetryCounter = 0;
    }

    public int incrementStructureCheckCounter() {
        structureCheckCounter++;
        return structureCheckCounter;
    }

    protected boolean matchesRotation(TaggedPositionBlockArray pattern, DynamicMachine machine, EnumFacing ctrlRotation) {
        if (pattern == null) {
            return false;
        }
        if (!Worlds.isAreaLoaded(getWorld(), pattern.getPatternBoundingBox(getPos()))) {
            return false;
        }

        DynamicMachine.ModifierReplacementMap replacements = machine.getModifiersAsMatchingReplacements();

        EnumFacing rotation = EnumFacing.NORTH;
        while (rotation != ctrlRotation) {
            rotation = rotation.rotateYCCW();
            replacements = replacements.rotateYCCW();
        }

        if (pattern.matches(getWorld(), getPos(), false, replacements) && matchesDynamicPatternRotation(machine, rotation)) {
            this.foundPattern = pattern;
            this.foundMachine = machine;
            this.foundReplacements = replacements;
            return true;
        }
        resetMachine(false);
        return false;
    }

    protected boolean matchesDynamicPattern(final DynamicMachine machine) {
        for (final DynamicPattern.Status status : foundDynamicPatterns.values()) {
            DynamicPattern pattern = status.pattern();
            DynamicPattern.MatchResult result = pattern.matches(this, true, controllerRotation);
            if (!result.isMatched()) {
                return false;
            }
        }

        return true;
    }

    protected boolean matchesDynamicPatternRotation(final DynamicMachine machine, final EnumFacing rotation) {
        this.foundDynamicPatterns.clear();

        Map<String, DynamicPattern> dynamicPatterns = machine.getDynamicPatterns();
        if (dynamicPatterns.isEmpty()) {
            return true;
        }

        List<DynamicPattern.Status> foundDynamicPatterns = new ArrayList<>();
        for (final DynamicPattern dynamicPattern : dynamicPatterns.values()) {
            DynamicPattern.MatchResult matchResult = dynamicPattern.matches(this, false, rotation);
            if (matchResult.isMatched()) {
                foundDynamicPatterns.add(new DynamicPattern.Status(
                    dynamicPattern, matchResult.matchFacing(), matchResult.size())
                );
            } else {
                return false;
            }
        }

        for (final DynamicPattern.Status pattern : foundDynamicPatterns) {
            this.foundDynamicPatterns.put(pattern.pattern().getName(), pattern);
        }
        return true;
    }

    protected void distributeCasingColor() {
        if (this.foundMachine == null || this.foundPattern == null) {
            return;
        }
        int color = this.foundMachine.getMachineColor();
        // Colorize Controller.
        setMachineColor(color);
        // Colorize Components.
        this.foundPattern.getTileBlocksArray().keySet().forEach(pos -> tryColorize(this.getPos().add(pos), color));
    }

    /**
     * 尝试对方块上色。
     *
     * @param pos   位置
     * @param color 颜色
     */
    private void tryColorize(BlockPos pos, int color) {
        TileEntity te = Worlds.getTileEntity(this.getWorld(), pos);
        if (te instanceof final ColorableMachineTile colorable) {
            if (colorable.getMachineColor() != color) {
                colorable.setMachineColor(color);
            }
        }
        // 1.7.10 移植：ME 机械样板供应器写入控制器名称的逻辑在 P5 恢复
    }

    public void resetStructureCheckCounter() {
        this.structureCheckCounter = 0;
    }

    protected void checkRotation() {
    }

    protected boolean doStructureCheck() {
        if (!canCheckStructure()) {
            return true;
        }
        checkRotation();
        // 检查多方块结构中的某个方块的区块是否被卸载，以免一些重要的配方运行失败。
        // 可能会提高一些性能开销，但是玩家体验更加重要。
        // Check if a block of a chunk in a multiblock structure is unloaded, so that some important recipes do not fail to run.
        // It may raise some performance overhead, but the player experience is more important.
        if (!checkStructure()) {
            if (getControllerStatus() != CraftingStatus.CHUNK_UNLOADED) {
                setControllerStatus(CraftingStatus.CHUNK_UNLOADED);
                markNoUpdateSync();
            }
            return false;
        }
        if (!isStructureFormed()) {
            if (getControllerStatus() != CraftingStatus.MISSING_STRUCTURE) {
                setControllerStatus(CraftingStatus.MISSING_STRUCTURE);
                markNoUpdateSync();
            }
            return false;
        }
        updateComponents();
        new MachineStructureUpdateEvent(this).postEvent();
        return true;
    }

    protected void updateStatedMachineComponentSync(final boolean working) {
        if (foundPattern == null) {
            return;
        }

        if (workMode == WorkMode.SYNC) {
            updateStatedMachineComponent(working);
        } else {
            ModularMachinery.EXECUTE_MANAGER.addSyncTask(() -> updateStatedMachineComponent(working));
        }
        requireUpdateComparatorLevel = true;
        markForUpdateSync();
    }

    protected void updateStatedMachineComponent(final boolean working) {
        if (foundPattern == null) {
            return;
        }
        final long start = System.nanoTime() / 1000;

        foundPattern.getPattern().forEach((pos, blockInfo) -> {
            if (!blockInfo.hasStatedMachineComponent()) {
                return;
            }

            BlockPos realPos = getPos().add(pos);
            IBlockState blockState = Worlds.getBlockState(getWorld(), realPos);
            Block block = blockState.getBlock();
            if (!(block instanceof BlockStatedMachineComponent)) {
                return;
            }

            Worlds.setBlockState(getWorld(), realPos, blockState.withMeta(
                (working ? WorkingState.WORKING : WorkingState.IDLE).ordinal()), 2);
        });

        timeRecorder.addUsedTime((int) (System.nanoTime() / 1000 - start));
    }

    public RecipeCraftingContext createContext(ActiveMachineRecipe activeRecipe) {
        RecipeCraftingContext context = foundMachine.createContext(activeRecipe, this);
        context.addModifier(MiscUtils.flatten(this.foundModifiers.values()));
        context.addModifier(customModifiers.values());
        return context;
    }

    protected void onStructureFormed() {
        new MachineStructureFormedEvent(this).postEvent();
        new MachineStructureUpdateEvent(this).postEvent();

        if (!foundDynamicPatterns.isEmpty()) {
            addDynamicPatternToBlockArray();
        }

        if (workMode == WorkMode.SYNC) {
            notifyStructureFormedState(true);
        } else {
            ModularMachinery.EXECUTE_MANAGER.addSyncTask(() -> notifyStructureFormedState(true));
        }

        requireUpdateComparatorLevel = true;
        resetStructureCheckCounter();
        if (prevMachine != null && !prevMachine.equals(foundMachine)) {
            resetRecipe();
        } else {
            prevMachine = null;
        }
        markNoUpdateSync();
    }

    public void notifyStructureFormedState(boolean formed) {
        // 1.7.10 移植：1.12.2 把 formed 写入方块状态以切换模型；1.7.10 的 meta 只保存朝向，
        // 这里改为通知客户端重新渲染控制器，由渲染器读取 isStructureFormed()。
        World world = getWorld();
        if (world == null || controllerRotation == null) {
            return;
        }
        if (world.isRemote) {
            world.markBlockRangeForRenderUpdate(xCoord, yCoord, zCoord, xCoord, yCoord, zCoord);
        } else {
            world.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    private void addDynamicPatternToBlockArray() {
        this.foundPattern = new TaggedPositionBlockArray(foundPattern);
        for (final DynamicPattern.Status status : foundDynamicPatterns.values()) {
            DynamicPattern pattern = status.pattern();
            pattern.addPatternToBlockArray(foundPattern, status.size(), status.matchFacing(), controllerRotation);
        }
        this.foundPattern.flushTileBlocksCache();
    }

    /**
     * <p>检查机械结构。</p>
     * <p>Check machine structure.</p>
     *
     * @return Returns false when there is a square block in the structure that is not loaded, and true in all other cases.
     */
    protected boolean checkStructure() {
        if (!canCheckStructure()) {
            return true;
        }

        lastStructureCheckTick = ticksExisted;

        if (isStructureFormed()) {
            BlockPos ctrlPos = getPos();
            //Is chunk area loaded? Prevention of unanticipated consumption of something.
            if (!Worlds.isAreaLoaded(getWorld(), foundPattern.getPatternBoundingBox(ctrlPos))) {
                return false;
            }
            if (this.foundMachine.isRequiresBlueprint() && !this.foundMachine.equals(getBlueprintMachine())) {
                resetMachine(true);
            } else if (
                !foundPattern.matches(getWorld(), ctrlPos, true, this.foundReplacements) ||
                    !matchesDynamicPattern(foundMachine)) {
                resetMachine(true);
            }
        }

        if (this.foundMachine != null && this.foundPattern != null && this.controllerRotation != null && this.foundReplacements != null) {
            return true;
        }

        resetMachine(false);

        // First, check blueprint machine.
        DynamicMachine blueprint = getBlueprintMachine();
        if (blueprint != null) {
            if (matchesRotation(
                BlockArrayCache.getBlockArrayCache(blueprint.getPattern(), controllerRotation),
                blueprint, controllerRotation)) {
                onStructureFormed();
                return true;
            }
        }

        // After, check parentMachine.
        if (parentMachine != null) {
            if (parentMachine.isRequiresBlueprint() && !parentMachine.equals(blueprint)) {
                // ParentMachine needs blueprint, but controller not has that, end check.
                return true;
            }
            if (matchesRotation(
                BlockArrayCache.getBlockArrayCache(parentMachine.getPattern(), controllerRotation),
                parentMachine, controllerRotation)) {
                onStructureFormed();
                return true;
            }
            // This controller is dedicated to parentMachine, it cannot become other, end check.
            return true;
        }

        // Finally, check all registered machinery.
        checkAllPatterns();

        if (!isStructureFormed()) {
            resetMachine(true);
        }
        return true;
    }

    protected void checkAllPatterns() {
        BlockPos ctrlPos = getPos();
        for (DynamicMachine machine : MachineRegistry.getRegistry()) {
            if (machine.isRequiresBlueprint()) {
                continue;
            }
            TaggedPositionBlockArray pattern = BlockArrayCache.getBlockArrayCache(
                machine.getPattern(), controllerRotation);

            if (!Worlds.isAreaLoaded(getWorld(), pattern.getPatternBoundingBox(ctrlPos))) {
                continue;
            }

            if (matchesRotation(pattern, machine, controllerRotation)) {
                onStructureFormed();
                break;
            }
        }
    }

    protected void updateComponents() {
        if (this.foundMachine == null || this.foundPattern == null || this.controllerRotation == null || this.foundReplacements == null) {
            this.foundComponents.forEach((i, map) -> map.forEach((te, c) -> MachineComponentManager.INSTANCE.removeOwner(te, this)));
            this.foundComponents.clear();
            this.generalComponents.clear();
            this.foundModifiers.clear();
            this.foundSmartInterfaces.clear();

            resetMachine(false);
            return;
        }
        if (!canCheckStructure()) {
            return;
        }

        this.foundUpgrades.clear();
        this.mergedUpgradeCopies.clear();
        this.foundUpgradeBuses.clear();
        this.foundComponents.forEach((i, map) -> map.forEach((te, c) -> MachineComponentManager.INSTANCE.removeOwner(te, this)));
        this.foundComponents.clear();
        this.generalComponents.clear();
        this.foundSmartInterfaces.clear();
        this.foundParallelControllers.clear();
        Map<Long, Map<TileEntity, ProcessingComponent<?>>> found = new Long2ObjectOpenHashMap<>();

        this.foundPattern.getTileBlocksArray().forEach((pos, info) -> checkAndAddComponents(pos, getPos(), found));
        if (found.isEmpty()) this.foundComponents.put(0L, generalComponents);
        else {
            this.foundComponents.putAll(found);
            this.foundComponents.values().forEach(c -> c.putAll(generalComponents));
        }
        this.foundModifiers.clear();
        updateModifiers();
        updateMultiBlockModifiers();
        if (workMode == WorkMode.SYNC) {
            distributeCasingColor();
        } else {
            ModularMachinery.EXECUTE_MANAGER.addSyncTask(this::distributeCasingColor);
        }
    }

    private void checkAndAddComponents(final BlockPos pos, final BlockPos ctrlPos, final Map<Long,Map<TileEntity, ProcessingComponent<?>>> found) {
        BlockPos realPos = ctrlPos.add(pos);

        if (!Worlds.isBlockLoaded(getWorld(), realPos)) {
            return;
        }
        TileEntity te = Worlds.getTileEntity(getWorld(), realPos);
        List<MachineComponent<?>> components = new ObjectArrayList<>();
        if (te instanceof MachineComponentTile mte) {
            MachineComponent<?> pc;
            if ((pc = mte.provideComponent()) != null)
                components.add(pc);
        } else {
            if (te == null) {
                return;
            }
            MachineComponent<?> proxiedComponent = MachineComponentProxyRegistry.INSTANCE.proxy(te);
            if (proxiedComponent == null) {
                return;
            }
            components.add(proxiedComponent);
        }

        if (te instanceof MachineCombinationComponent mcc) {
            components.addAll(mcc.provideComponents());
        }

        if (components.isEmpty()) {
            return;
        }

        ComponentSelectorTag tag = this.foundPattern.getTag(pos);

        for (var component : components) {
            if (!component.isAsyncSupported()) {
                workMode = WorkMode.SEMI_SYNC;
            }

            addComponent(component, tag, te, found);
            if (component instanceof TileParallelController.ParallelControllerProvider p) {
                this.foundParallelControllers.add(p);
                return;
            }
            checkAndAddUpgradeBus(component);
            checkAndAddSmartInterface(component, realPos);
        }
    }

    public void checkAndAddUpgradeBus(final MachineComponent<?> component) {
        if (!(component instanceof final TileUpgradeBus.UpgradeBusProvider upgradeBus)) {
            return;
        }
        upgradeBus.boundMachine(this);
        foundUpgradeBuses.add(upgradeBus);

        Map<UpgradeType, List<MachineUpgrade>> found = upgradeBus.getUpgrades(this);
        found.forEach((type, newUpgrades) -> {
            List<MachineUpgrade> upgrades = foundUpgrades.computeIfAbsent(type.getName(), v -> new ArrayList<>());
            add:
            for (final MachineUpgrade newUpgrade : newUpgrades) {
                // 1.7.10 移植修正：多个升级总线装有同类升级时，上游把前一个的数量加到后一个总线缓存的实例上并丢弃它——
                // 后面的总线不生效，而且它的数量在每次结构检查时继续累加。改为：同类（非动态）升级的数量 = 各总线之和（不超过上限），
                // 累加在控制器自己的副本上，不改动升级总线缓存的实例。动态升级与具体物品绑定，不合并。
                if (!(newUpgrade instanceof DynamicMachineUpgrade)) {
                    for (int i = 0; i < upgrades.size(); i++) {
                        MachineUpgrade u = upgrades.get(i);
                        if (u instanceof DynamicMachineUpgrade || !newUpgrade.equals(u)) {
                            continue;
                        }
                        if (!mergedUpgradeCopies.contains(u)) {
                            u = copyForMerge(u);
                            upgrades.set(i, u);
                        }
                        u.setStackSize(Math.min(u.getStackSize() + newUpgrade.getStackSize(), u.getType().getMaxStackSize()));
                        continue add;
                    }
                }
                upgrades.add(newUpgrade);
            }
        });
    }

    /** 合并多个升级总线时由控制器持有的升级副本（每次重新收集组件时清空）。 */
    private final Set<MachineUpgrade> mergedUpgradeCopies = Collections.newSetFromMap(new IdentityHashMap<>());

    private MachineUpgrade copyForMerge(MachineUpgrade base) {
        MachineUpgrade copy = base.copy(null);
        copy.readNBT(base.writeNBT());
        copy.setParentBus(base.getParentBus());
        copy.setStackSize(base.getStackSize());
        mergedUpgradeCopies.add(copy);
        return copy;
    }


    protected void updateMultiBlockModifiers() {
        for (MultiBlockModifierReplacement mod : foundMachine.getMultiBlockModifiers()) {
            if (!mod.matches(this)) {
                continue;
            }
            this.foundModifiers.put(mod.getModifierName(), mod.getModifiers());
        }
    }

    protected void updateModifiers() {
        int rotations = 0;
        EnumFacing rot = EnumFacing.NORTH;
        while (rot != this.controllerRotation) {
            rot = rot.rotateYCCW();
            rotations++;
        }

        for (Map.Entry<BlockPos, List<SingleBlockModifierReplacement>> offsetModifiers : foundMachine.getModifiers().entrySet()) {
            BlockPos at = offsetModifiers.getKey();
            for (int i = 0; i < rotations; i++) {
                at = new BlockPos(at.getZ(), at.getY(), -at.getX());
            }
            BlockPos realAt = this.getPos().add(at);
            for (SingleBlockModifierReplacement mod : offsetModifiers.getValue()) {
                BlockArray.BlockInformation info = mod.getBlockInformation();
                for (int i = 0; i < rotations; i++) {
                    info = info.copyRotateYCCW();
                }
                if (info.matches(getWorld(), realAt, true)) {
                    foundModifiers.put(mod.getModifierName(), mod.getModifiers());
                }
            }
        }
    }

    public void checkAndAddSmartInterface(MachineComponent<?> component, BlockPos realPos) {
        if (!(component instanceof final TileSmartInterface.SmartInterfaceProvider smartInterface) || foundMachine.smartInterfaceTypesIsEmpty()) {
            return;
        }
        SmartInterfaceData data = smartInterface.getMachineData(getPos());
        Map<String, SmartInterfaceType> notFoundInterface = foundMachine.getFilteredType(foundSmartInterfaces.values());

        if (data != null) {
            String type = data.getType();

            if (notFoundInterface.containsKey(type)) {
                foundSmartInterfaces.put(smartInterface, type);
            } else {
                smartInterface.removeMachineData(realPos);
            }
        } else {
            if (notFoundInterface.isEmpty()) {
                Optional<SmartInterfaceType> typeOpt = foundMachine.getFirstSmartInterfaceType();
                if (typeOpt.isPresent()) {
                    SmartInterfaceType type = typeOpt.get();
                    smartInterface.addMachineData(getPos(), foundMachine.getRegistryName(), type.getType(), type.getDefaultValue(), true);
                    foundSmartInterfaces.put(smartInterface, type.getType());
                }
            } else {
                SmartInterfaceType type = notFoundInterface.values().stream().sorted().findFirst().get();
                smartInterface.addMachineData(getPos(), foundMachine.getRegistryName(), type.getType(), type.getDefaultValue(), true);
                foundSmartInterfaces.put(smartInterface, type.getType());
            }
        }
    }

    public IWorld getIWorld() {
        return CraftTweakerMC.getIWorld(getWorld());
    }

    public github.kasuminova.mmce.compat.crt.IBlockState getIBlockState() {
        return CraftTweakerMC.getBlockState(Worlds.getBlockState(getWorld(), getPos()));
    }

    @Override
    public IFacing getFacing() {
        return CraftTweakerMC.getIFacing(controllerRotation);
    }

    public IBlockPos getIPos() {
        return CraftTweakerMC.getIBlockPos(getPos());
    }

    @Override
    public IBlockPos rotateWithControllerFacing(final IBlockPos posCT) {
        BlockPos pos = CraftTweakerMC.getBlockPos(posCT);
        return CraftTweakerMC.getIBlockPos(MiscUtils.rotateYCCWNorthUntil(pos, controllerRotation == null ? EnumFacing.NORTH : controllerRotation));
    }

    public String getFormedMachineName() {
        return isStructureFormed() ? foundMachine.getRegistryName().toString() : null;
    }

    public IData getCustomData() {
        return CraftTweakerMC.getIDataModifyable(customData);
    }

    public void setCustomData(IData data) {
        customData = CraftTweakerMC.getNBTCompound(data);
        markNoUpdateSync();
    }

    public NBTTagCompound getCustomDataTag() {
        return customData;
    }

    public void setCustomDataTag(NBTTagCompound customData) {
        this.customData = customData;
    }

    public boolean hasModifier(String key) {
        return customModifiers.containsKey(key);
    }

    /**
     * <p>机器开始检查配方能否工作。</p>
     *
     * @param context RecipeCraftingContext
     * @return CraftingCheckResult
     */
    public RecipeCraftingContext.CraftingCheckResult onCheck(RecipeCraftingContext context) {
        RecipeCraftingContext.CraftingCheckResult failure = checkPreStartResult(context);
        if (failure != null) {
            return failure;
        }

        RecipeCraftingContext.CraftingCheckResult result = context.getActiveRecipe().canStartCrafting(context);
        return checkStartResult(context, result);
    }

    /**
     * <p>机器开始检查配方能否工作，只在重新开始时调用。</p>
     *
     * @param context RecipeCraftingContext
     * @return CraftingCheckResult
     */
    public RecipeCraftingContext.CraftingCheckResult onRestartCheck(RecipeCraftingContext context) {
        RecipeCraftingContext.CraftingCheckResult failure = checkPreStartResult(context);
        if (failure != null) {
            return failure;
        }

        RecipeCraftingContext.CraftingCheckResult result = context.getActiveRecipe().canRestartCrafting(context);
        return checkStartResult(context, result);
    }

    @Nullable
    public RecipeCraftingContext.CraftingCheckResult checkPreStartResult(final RecipeCraftingContext context) {
        RecipeCheckEvent event = new RecipeCheckEvent(this, context, Phase.START);
        event.postEvent();

        if (event.isFailure()) {
            RecipeCraftingContext.CraftingCheckResult failure = new RecipeCraftingContext.CraftingCheckResult();
            failure.addError(event.getFailureReason());
            return failure;
        }
        return null;
    }

    @Nonnull
    public RecipeCraftingContext.CraftingCheckResult checkStartResult(final RecipeCraftingContext context, final RecipeCraftingContext.CraftingCheckResult result) {
        if (result.isFailure()) {
            return result;
        }

        RecipeCheckEvent event = new RecipeCheckEvent(this, context, Phase.END);
        event.postEvent();

        if (event.isFailure()) {
            RecipeCraftingContext.CraftingCheckResult failure = new RecipeCraftingContext.CraftingCheckResult();
            failure.addError(event.getFailureReason());
            return failure;
        }

        return result;
    }

    /**
     * <p>机器开始执行逻辑。</p>
     */
    public void onMachineTick(Phase phase) {
        new MachineTickEvent(this, phase).postEvent();
    }

    @Override
    public void addPermanentModifier(String key, RecipeModifier newModifier) {
        if (newModifier != null) {
            customModifiers.put(key, newModifier);
            flushContextModifier();
        }
    }

    @Override
    public void removePermanentModifier(String key) {
        if (hasModifier(key)) {
            customModifiers.remove(key);
            flushContextModifier();
        }
    }

    public abstract void flushContextModifier();

    public Map<TileSmartInterface.SmartInterfaceProvider, String> getFoundSmartInterfaces() {
        return foundSmartInterfaces;
    }

    public Map<String, List<MachineUpgrade>> getFoundUpgrades() {
        return foundUpgrades;
    }

    public List<TileUpgradeBus.UpgradeBusProvider> getFoundUpgradeBuses() {
        return foundUpgradeBuses;
    }

    public List<TileParallelController.ParallelControllerProvider> getFoundParallelControllers() {
        return foundParallelControllers;
    }

    public Map<Long, Map<TileEntity, ProcessingComponent<?>>> getFoundComponents() {
        return foundComponents;
    }

    public Map<TileEntity, ProcessingComponent<?>> getGeneralComponents() {
        return generalComponents;
    }

    public DynamicMachine.ModifierReplacementMap getFoundReplacements() {
        return foundReplacements;
    }

    public Map<String, RecipeModifier> getCustomModifiers() {
        return customModifiers;
    }

    public Map<String, List<RecipeModifier>> getFoundModifiers() {
        return foundModifiers;
    }

    @Nullable
    public SmartInterfaceData getSmartInterfaceData(String requiredType) {
        AtomicReference<TileSmartInterface.SmartInterfaceProvider> reference = new AtomicReference<>(null);
        foundSmartInterfaces.forEach((provider, type) -> {
            if (type.equals(requiredType)) {
                reference.set(provider);
            }
        });
        TileSmartInterface.SmartInterfaceProvider smartInterface = reference.get();
        if (smartInterface != null) {
            return smartInterface.getMachineData(getPos());
        } else {
            return null;
        }
    }

    public SmartInterfaceData[] getSmartInterfaceDataList() {
        List<SmartInterfaceData> dataList = new ArrayList<>();
        BlockPos ctrlPos = getPos();
        foundSmartInterfaces.forEach((provider, type) -> {
            SmartInterfaceData data = provider.getMachineData(ctrlPos);
            if (data != null) {
                dataList.add(data);
            }
        });
        return dataList.toArray(new SmartInterfaceData[0]);
    }

    public String[] getFoundModifierReplacements() {
        return foundModifiers.keySet().toArray(new String[0]);
    }

    public boolean hasModifierReplacement(String modifierName) {
        return foundModifiers.containsKey(modifierName);
    }

    @Override
    public boolean hasMachineUpgrade(final String upgradeName) {
        List<MachineUpgrade> upgrades = foundUpgrades.get(upgradeName);
        return upgrades != null && !upgrades.isEmpty();
    }

    @Nullable
    @Override
    public MachineUpgrade[] getMachineUpgrade(final String upgradeName) {
        List<MachineUpgrade> upgrades = foundUpgrades.get(upgradeName);
        if (upgrades == null) {
            return new MachineUpgrade[0];
        }

        List<MachineUpgrade> filtered = new ArrayList<>();
        for (final MachineUpgrade upgrade : upgrades) {
            TileUpgradeBus parentBus = upgrade.getParentBus();
            if (parentBus == null) {
                continue;
            }
            upgrade.readNBT(parentBus.provideComponent().getUpgradeCustomData(upgrade));
            filtered.add(upgrade);
        }

        return filtered.toArray(new MachineUpgrade[0]);
    }

    @Nullable
    @Override
    public IDynamicPatternInfo getDynamicPattern(final String patternName) {
        return foundDynamicPatterns.get(patternName);
    }

    @Override
    public int getBlocksInPattern(final IItemStack blockStack) {
        if (foundPattern == null || blockStack == null) {
            return 0;
        }
        // 1.7.10 移植：MT3 的 IBlockDefinition 没有方块状态 API，直接比较方块与 meta
        Block block = MineTweakerMC.getBlock(blockStack);
        if (block == null) {
            return 0;
        }
        int meta = blockStack.getDamage();
        return getBlocksInPatternInternal(state -> state.getBlock() == block && (meta == OreDictionary.WILDCARD_VALUE || state.getMetadata() == meta));
    }

    @Override
    public int getBlocksInPattern(final IBlockStateMatcher blockStateMatcher) {
        if (foundPattern == null) {
            return 0;
        }
        return getBlocksInPatternInternal(state -> blockStateMatcher.matches(CraftTweakerMC.getBlockState(state)));
    }

    @Override
    public int getBlocksInPattern(final String blockName) {
        if (foundPattern == null) {
            return 0;
        }
        List<IBlockState> applicable = BlockArray.BlockInformation.getDescriptor(blockName).getApplicable();
        return getBlocksInPatternInternal(applicable::contains);
    }

    @Override
    public int getBlocksInPattern(final IBlockStatePredicate predicate) {
        if (foundPattern == null) {
            return 0;
        }
        return getBlocksInPatternInternal(state -> predicate.test(CraftTweakerMC.getBlockState(state)));
    }

    @Override
    public IBlockPos[] getBlockPosInPattern(IItemStack blockStack) {
        if (foundPattern == null || blockStack == null) {
            return new IBlockPos[0];
        }
        Block block = MineTweakerMC.getBlock(blockStack);
        if (block == null) {
            return new IBlockPos[0];
        }
        int meta = blockStack.getDamage();
        return getBlockPossInPatternInternal(state -> state.getBlock() == block && (meta == OreDictionary.WILDCARD_VALUE || state.getMetadata() == meta));
    }

    @Override
    public IBlockPos[] getBlockPosInPattern(IBlockStateMatcher blockStateMatcher) {
        if (foundPattern == null) {
            return new IBlockPos[0];
        }
        return getBlockPossInPatternInternal(state -> blockStateMatcher.matches(CraftTweakerMC.getBlockState(state)));
    }

    @Override
    public IBlockPos[] getBlockPosInPattern(String blockName) {
        if (foundPattern == null) {
            return new IBlockPos[0];
        }
        List<IBlockState> applicable = BlockArray.BlockInformation.getDescriptor(blockName).getApplicable();
        return getBlockPossInPatternInternal(applicable::contains);
    }

    @Override
    public IBlockPos[] getBlockPosInPattern(IBlockStatePredicate predicate) {
        if (foundPattern == null) {
            return new IBlockPos[0];
        }
        return getBlockPossInPatternInternal(state -> predicate.test(CraftTweakerMC.getBlockState(state)));
    }

    public int getBlocksInPatternInternal(final Predicate<IBlockState> predicate) {
        if (foundPattern == null) {
            return 0;
        }
        int count = 0;
        for (final BlockPos pos : foundPattern.getPattern().keySet()) {
            BlockPos realPos = getPos().add(pos.getX(), pos.getY(), pos.getZ());
            IBlockState state = Worlds.getBlockState(getWorld(), realPos);
            if (state.getBlock() == Blocks.air) {
                continue;
            }
            if (predicate.test(state)) {
                count++;
            }
        }
        return count;
    }

    public IBlockPos[] getBlockPossInPatternInternal(final Predicate<IBlockState> predicate) {
        if (foundPattern == null) {
            return new IBlockPos[0];
        }
        List<IBlockPos> poss = new ObjectArrayList<>();
        for (final BlockPos pos : foundPattern.getPattern().keySet()) {
            BlockPos realPos = getPos().add(pos.getX(), pos.getY(), pos.getZ());
            IBlockState state = Worlds.getBlockState(getWorld(), realPos);
            if (state.getBlock() == Blocks.air) {
                continue;
            }
            if (predicate.test(state)) {
                poss.add(CraftTweakerMC.getIBlockPos(pos));
            }
        }
        return poss.toArray(new IBlockPos[0]);
    }

    @Nullable
    @Override
    @SuppressWarnings("ConstantValue")
    public IPlayer getOwnerIPlayer() {
        if (owner == null) {
            return null;
        }
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) {
            return null;
        }
        for (Object o : server.getConfigurationManager().playerEntityList) {
            EntityPlayerMP player = (EntityPlayerMP) o;
            if (owner.equals(player.getUniqueID())) {
                return CraftTweakerMC.getIPlayer(player);
            }
        }
        return null;
    }

    @Nullable
    @Override
    public String getOwnerName() {
        if (owner == null) {
            return null;
        }
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) {
            return null;
        }
        GameProfile profile = server.func_152358_ax().func_152652_a(owner);
        return profile != null ? profile.getName() : null;
    }

    @Nullable
    @Override
    public String getOwnerUUIDString() {
        return owner == null ? null : owner.toString();
    }

    public Map<String, DynamicPattern.Status> getDynamicPatterns() {
        return foundDynamicPatterns;
    }

    public TileMultiblockMachineController getController() {
        return this;
    }

    public void incrementRecipeSearchRetryCount() {
        recipeResearchRetryCounter++;
    }

    @Override
    public void markNoUpdate() {
        super.markNoUpdate();
        requireUpdateComparatorLevel = false;
    }

    @Override
    public void validate() {
        super.validate();
        loaded = true;
    }

    @Override
    public void invalidate() {
        super.invalidate();
        loaded = false;
        this.foundComponents.forEach((i, map) -> map.forEach((te,c) -> MachineComponentManager.INSTANCE.removeOwner(te, this)));

    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        this.inventory = IOInventory.deserialize(this, compound.getCompoundTag("items"));
        this.inventory.setStackLimit(1, BLUEPRINT_SLOT);

        if (compound.hasKey("owner")) {
            String ownerUUIDStr = compound.getString("owner");
            try {
                this.owner = UUID.fromString(ownerUUIDStr);
            } catch (Exception e) {
                ModularMachinery.log.warn("Invalid owner uuid {}", ownerUUIDStr, e);
            }
        }

        readMachineNBT(compound);

        if (loaded && worldObj != null && worldObj.isRemote) {
            notifyStructureFormedState(isStructureFormed());
        }
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);

        compound.setTag("items", this.inventory.writeNBT());

        if (this.owner != null) {
            compound.setString("owner", this.owner.toString());
        }

        if (this.parentMachine != null) {
            compound.setString("parentMachine", this.parentMachine.getRegistryName().toString());
        }
        if (this.prevMachine != null) {
            compound.setString("prevMachine", this.prevMachine.getRegistryName().toString());
        }
        if (this.controllerRotation != null) {
            compound.setByte("rotation", (byte) this.controllerRotation.getHorizontalIndex());
        }
        if (this.foundMachine != null) {
            compound.setString("machine", this.foundMachine.getRegistryName().toString());

            if (!foundDynamicPatterns.isEmpty()) {
                NBTTagList tagList = new NBTTagList();
                foundDynamicPatterns.values().forEach(pattern -> {
                    NBTTagCompound patternTag = new NBTTagCompound();
                    pattern.writeToNBT(patternTag);
                    tagList.appendTag(patternTag);
                });
                compound.setTag("dynamicPatterns", tagList);
            }
            if (!customData.hasNoTags()) {
                compound.setTag("customData", customData);
            }
            if (!customModifiers.isEmpty()) {
                NBTTagList tagList = new NBTTagList();
                customModifiers.forEach((key, modifier) -> {
                    if (key != null && modifier != null) {
                        NBTTagCompound modifierTag = new NBTTagCompound();
                        modifierTag.setString("key", key);
                        modifierTag.setTag("modifier", modifier.serialize());
                        tagList.appendTag(modifierTag);
                    }
                });
                compound.setTag("customModifier", tagList);
            }
        }
    }

    protected void readMachineNBT(NBTTagCompound compound) {
        if (!compound.hasKey("machine") || !compound.hasKey("rotation")) {
            resetMachine(true);
            return;
        }

        ResourceLocation rl = new ResourceLocation(compound.getString("machine"));
        DynamicMachine machine = MachineRegistry.getRegistry().getMachine(rl);
        if (machine == null) {
            ModularMachinery.log.info("Couldn't find machine named {} for controller at {}", rl, getPos());
            resetMachine(true);
            return;
        }
        this.foundMachine = machine;
        this.controllerRotation = EnumFacing.byHorizontalIndex(compound.getByte("rotation"));

        if (compound.hasKey("prevMachine")) {
            this.prevMachine = MachineRegistry.getRegistry().getMachine(new ResourceLocation(compound.getString("prevMachine")));
        }

        TaggedPositionBlockArray pattern = BlockArrayCache.getBlockArrayCache(machine.getPattern(), this.controllerRotation);
        if (pattern == null) {
            ModularMachinery.log.info("{} has a empty pattern cache! Please report this to the mod author.", rl);
            resetMachine(true);
            return;
        }
        this.foundPattern = pattern;

        if (compound.hasKey("dynamicPatterns", Constants.NBT.TAG_LIST)) {
            NBTTagList dynPatterns = compound.getTagList("dynamicPatterns", Constants.NBT.TAG_COMPOUND);
            IntStream.range(0, dynPatterns.tagCount())
                     .mapToObj(dynPatterns::getCompoundTagAt)
                     .map(tag -> DynamicPattern.Status.readFromNBT(tag, foundMachine))
                     .filter(Objects::nonNull)
                     .forEach(patternStatus -> foundDynamicPatterns.put(patternStatus.getPatternName(), patternStatus));
            if (!foundDynamicPatterns.isEmpty()) {
                addDynamicPatternToBlockArray();
            }
        }

        DynamicMachine.ModifierReplacementMap replacements = machine.getModifiersAsMatchingReplacements();
        EnumFacing offset = EnumFacing.NORTH;
        while (offset != this.controllerRotation) {
            replacements = replacements.rotateYCCW();
            offset = offset.rotateY();
        }
        this.foundReplacements = replacements;

        if (compound.hasKey("customData")) {
            this.customData = compound.getCompoundTag("customData");
        }
        this.customModifiers.clear();
        if (compound.hasKey("customModifier")) {
            NBTTagList tagList = compound.getTagList("customModifier", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < tagList.tagCount(); i++) {
                NBTTagCompound modifierTag = tagList.getCompoundTagAt(i);
                this.customModifiers.put(modifierTag.getString("key"), RecipeModifier.deserialize(modifierTag.getCompoundTag("modifier")));
            }
        }
    }

    /** 1.12.2 的 shouldRefresh：仅方块变化时才替换 TileEntity（朝向 meta 变化不重建）。 */
    @Override
    public boolean shouldRefresh(Block oldBlock, Block newBlock, int oldMeta, int newMeta, World world, int x, int y, int z) {
        return oldBlock != newBlock;
    }

    @ZenMethod
    public int getTicksExisted() {
        return ticksExisted;
    }

    public WorkMode getWorkMode() {
        return workMode;
    }

    public void setWorkMode(final WorkMode workMode) {
        this.workMode = workMode;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(final UUID owner) {
        this.owner = owner;
    }

    @Nonnull
    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }

    @Override
    public double getMaxRenderDistanceSquared() {
        return 65536D;
    }

    public enum StructureCheckMode {
        FULL, OPTIONAL
    }

    public enum WorkMode {
        ASYNC(EnumChatFormatting.GREEN + "ASYNC" + EnumChatFormatting.WHITE),
        SEMI_SYNC(EnumChatFormatting.YELLOW + "SEMI-SYNC" + EnumChatFormatting.WHITE),
        SYNC(EnumChatFormatting.RED + "SYNC" + EnumChatFormatting.WHITE);

        private final String displayName;

        WorkMode(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum Type {
        MISSING_STRUCTURE,
        CHUNK_UNLOADED,
        NO_RECIPE,
        IDLE,
        CRAFTING;

        public String getUnlocalizedDescription() {
            return "gui.controller.status." + this.name().toLowerCase();
        }

    }
}
