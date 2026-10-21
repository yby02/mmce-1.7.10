package github.kasuminova.mmce.common.tile;

import appeng.api.AEApi;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.networking.events.MENetworkChannelsChanged;
import appeng.api.networking.events.MENetworkCraftingPatternChange;
import appeng.api.networking.events.MENetworkEventSubscribe;
import appeng.api.networking.events.MENetworkPowerStatusChange;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.me.GridAccessException;
import appeng.util.Platform;
import appeng.util.inv.MEInventoryCrafting;
import github.kasuminova.mmce.common.event.machine.MachineEvent;
import github.kasuminova.mmce.common.event.recipe.FactoryRecipeFinishEvent;
import github.kasuminova.mmce.common.event.recipe.RecipeFinishEvent;
import github.kasuminova.mmce.common.integration.ae2.AE2FCSupport;
import github.kasuminova.mmce.common.tile.base.MEMachineComponent;
import github.kasuminova.mmce.common.tile.base.MachineCombinationComponent;
import github.kasuminova.mmce.common.util.FluidTankArray;
import github.kasuminova.mmce.common.util.InfItemFluidHandler;
import github.kasuminova.mmce.compat.ItemStacks;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.lib.BlocksMM;
import hellfirepvp.modularmachinery.common.lib.ComponentTypesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.tiles.base.MachineComponentTileNotifiable;
import hellfirepvp.modularmachinery.common.util.IOInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * ME 机械样板供应器：36 格处理样板，AE2 合成时把样板的原料（物品与流体）放进不限容量的缓存，
 * 机器直接从缓存中取用（组件类型为 {@link ComponentTypesMM#COMPONENT_ITEM_FLUID_GAS}）。
 * 另有 2 格可手动放物品、1 个流体槽的“独立存储”，同样被机器识别，清空缓存时不清空。
 * <p>
 * 五种工作模式同上游：默认、阻挡（缓存非空时不接收新样板）、合成锁定（机器完成一次配方前不接收）、
 * 强化阻挡（缓存非空时只接收同一样板）、输入隔离（每格样板一个独立缓存，各自作为一个组件，组设置被忽略）。
 * <p>
 * 1.7.10 移植说明：
 * <ul>
 *   <li>AE2 rv3 的合成 CPU 推送样板时使用 {@link MEInventoryCrafting}，其中的流体原料是 {@link IAEFluidStack}，直接注入流体缓存；
 *       AE2FC 的流体滴（ItemFluidDrop / ItemFluidPacket）在装有 AE2FC 时同样换成流体。</li>
 *   <li>上游借用 AE2 1.12 的 DualityInterface / IInterfaceHost 只为让 AE2 终端显示它（接口终端）与重命名，1.7.10 未移植这两项。</li>
 *   <li>Mekanism 气体依赖 MekEng（AE2 1.12 的气体扩展），1.7.10 没有对应模组，未移植。</li>
 *   <li>界面中缓存内容的显示不使用上游的单独数据包，而是随方块实体更新同步（打开界面期间由 SelectiveUpdateHandler 发送）。</li>
 * </ul>
 */
public class MEPatternProvider extends MEMachineComponent implements ICraftingProvider, MachineComponentTileNotifiable, MachineCombinationComponent {

    public static final int PATTERNS               = 36;
    public static final int SUB_ITEM_HANDLER_SLOTS = 2;

    protected       IOInventory               subItemHandler  = buildSubItemHandler();
    protected final FluidTankArray            subFluidHandler = new FluidTankArray(1, Integer.MAX_VALUE);
    protected       IOInventory               patterns        = buildPatternInventory();
    protected final ICraftingPatternDetails[] details         = new ICraftingPatternDetails[PATTERNS];
    protected       WorkModeSetting           workMode        = WorkModeSetting.DEFAULT;
    protected volatile boolean                machineCompleted  = true;
    protected       boolean                   shouldReturnItems = false;
    protected       int                       currentPatternIdx = -1;
    protected       ICraftingPatternDetails   currentPattern    = null;
    private         boolean                   patternsLoaded    = false;

    protected final InfItemFluidHandler       handler               = new InfItemFluidHandler(subItemHandler, subFluidHandler);
    protected final List<MachineComponent<?>> combinationComponents = new ArrayList<>();

    public MEPatternProvider() {
        handler.setOnItemChanged(slot -> markNoUpdateSync());
        handler.setOnFluidChanged(slot -> markNoUpdateSync());
        subFluidHandler.setListener(slot -> markNoUpdateSync());

        for (int i = 0; i < PATTERNS; i++) {
            InfItemFluidHandler isolated = new InfItemFluidHandler(subItemHandler, subFluidHandler);
            isolated.setOnItemChanged(slot -> markNoUpdateSync());
            isolated.setOnFluidChanged(slot -> markNoUpdateSync());
            long groupId = getUniqueGroupID();
            combinationComponents.add(new MachineComponent<InfItemFluidHandler>(IOType.INPUT) {
                @Override
                public ComponentType getComponentType() {
                    return ComponentTypesMM.COMPONENT_ITEM_FLUID_GAS;
                }

                @Override
                public InfItemFluidHandler getContainerProvider() {
                    return isolated;
                }

                @Override
                public long getGroupID() {
                    return groupId;
                }
            });
        }
    }

    private IOInventory buildSubItemHandler() {
        int[] slots = {0, 1};
        IOInventory inv = new IOInventory(this, new int[0], new int[0]);
        inv.setMiscSlots(slots);
        inv.setStackLimit(64, slots);
        return inv;
    }

    private IOInventory buildPatternInventory() {
        int[] slots = new int[PATTERNS];
        for (int i = 0; i < PATTERNS; i++) {
            slots[i] = i;
        }
        IOInventory inv = new IOInventory(this, new int[0], new int[0]);
        inv.setMiscSlots(slots);
        inv.setStackLimit(1, slots);
        inv.setListener(this::onPatternChanged);
        return inv;
    }

    public static boolean isPattern(ItemStack stack) {
        return !ItemStacks.isEmpty(stack) && stack.getItem() instanceof ICraftingPatternItem;
    }

    @Override
    public ItemStack getVisualItemStack() {
        return new ItemStack(BlocksMM.mePatternProvider);
    }

    public List<MachineComponent<?>> getCombinationComponents() {
        return combinationComponents;
    }

    @Override
    public boolean canGroupInput() {
        return true;
    }

    @Nullable
    @Override
    public MachineComponent<InfItemFluidHandler> provideComponent() {
        if (workMode == WorkModeSetting.ISOLATION_INPUT) {
            return null;
        }
        return new MachineComponent<InfItemFluidHandler>(IOType.INPUT) {
            @Override
            public ComponentType getComponentType() {
                return ComponentTypesMM.COMPONENT_ITEM_FLUID_GAS;
            }

            @Override
            public InfItemFluidHandler getContainerProvider() {
                return handler;
            }

            @Override
            public long getGroupID() {
                return getGroupId();
            }
        };
    }

    @Nonnull
    @Override
    public Collection<MachineComponent<?>> provideComponents() {
        if (workMode == WorkModeSetting.ISOLATION_INPUT) {
            return combinationComponents;
        }
        return Collections.emptyList();
    }

    // ---------------- AE2 合成 ----------------

    @MENetworkEventSubscribe
    public void stateChange(final MENetworkChannelsChanged change) {
        notifyNeighbors();
    }

    @MENetworkEventSubscribe
    public void stateChange(final MENetworkPowerStatusChange change) {
        notifyNeighbors();
    }

    @Override
    public void provideCrafting(final ICraftingProviderHelper craftingTracker) {
        if (!proxy.isActive() || !proxy.isPowered()) {
            return;
        }
        for (ICraftingPatternDetails detail : details) {
            if (detail != null) {
                craftingTracker.addCraftingOption(this, detail);
            }
        }
    }

    @Override
    public boolean pushPattern(final ICraftingPatternDetails patternDetails, final InventoryCrafting table) {
        if (!acceptsPattern(patternDetails)) {
            return false;
        }

        InfItemFluidHandler target = null;
        if (workMode == WorkModeSetting.ISOLATION_INPUT) {
            for (int i = 0; i < details.length; i++) {
                if (patternDetails.equals(details[i])) {
                    target = (InfItemFluidHandler) combinationComponents.get(i).getContainerProvider();
                    break;
                }
            }
        } else {
            target = this.handler;
        }
        if (target == null) {
            return false;
        }

        int slots = table.getSizeInventory();
        for (int slot = 0; slot < slots; slot++) {
            if (table instanceof MEInventoryCrafting meTable) {
                IAEStack<?> aeStack = meTable.getAEStackInSlot(slot);
                if (aeStack instanceof IAEFluidStack fluid) {
                    target.fill(fluid.getFluidStack(), true);
                    continue;
                }
                if (aeStack instanceof IAEItemStack item) {
                    pushItem(target, item.getItemStack());
                    continue;
                }
            }
            pushItem(target, table.getStackInSlot(slot));
        }

        handleNewPattern(patternDetails);
        machineCompleted = workMode != WorkModeSetting.CRAFTING_LOCK_MODE;
        return true;
    }

    private static void pushItem(InfItemFluidHandler target, ItemStack stack) {
        if (ItemStacks.isEmpty(stack)) {
            return;
        }
        if (Mods.AE2FCR.isPresent()) {
            FluidStack fluid = AE2FCSupport.toFluid(stack);
            if (fluid != null) {
                target.fill(fluid, true);
                return;
            }
        }
        target.appendItem(stack);
    }

    private boolean acceptsPattern(final ICraftingPatternDetails patternDetails) {
        if (patternDetails.isCraftable() || !proxy.isActive() || !proxy.isPowered()) {
            return false;
        }
        return workMode != WorkModeSetting.ENHANCED_BLOCKING_MODE || handler.isEmpty() || currentPattern == null || currentPattern.equals(patternDetails);
    }

    private void handleNewPattern(final ICraftingPatternDetails patternDetails) {
        if (workMode == WorkModeSetting.ENHANCED_BLOCKING_MODE) {
            if (!patternDetails.equals(currentPattern)) {
                setCurrentPattern(patternDetails);
            }
        } else {
            resetCurrentPattern();
        }
    }

    @Override
    public boolean isBusy() {
        return (workMode == WorkModeSetting.CRAFTING_LOCK_MODE && !machineCompleted)
            || (workMode == WorkModeSetting.BLOCKING_MODE && !handler.isEmpty());
    }

    @Override
    protected void notifyNeighbors() {
        postPatternChange();
        super.notifyNeighbors();
    }

    private void postPatternChange() {
        if (proxy.isActive()) {
            try {
                proxy.getGrid().postEvent(new MENetworkCraftingPatternChange(this, proxy.getNode()));
            } catch (GridAccessException ignored) {
            }
        }
    }

    private void onPatternChanged(int slot) {
        if (worldObj == null || worldObj.isRemote || !patternsLoaded) {
            return;
        }
        refreshPattern(slot);
        postPatternChange();
    }

    protected void refreshPatterns() {
        patternsLoaded = true;
        for (int i = 0; i < PATTERNS; i++) {
            refreshPattern(i);
        }
        if (currentPatternIdx != -1 && currentPatternIdx < details.length) {
            setCurrentPattern(details[currentPatternIdx]);
        }
        postPatternChange();
    }

    protected void refreshPattern(final int slot) {
        details[slot] = null;

        ItemStack pattern = patterns.getStackInSlot(slot);
        if (!isPattern(pattern)) {
            return;
        }

        ICraftingPatternDetails detail = ((ICraftingPatternItem) pattern.getItem()).getPatternForItem(pattern, worldObj);
        if (detail != null && !detail.isCraftable()) {
            details[slot] = detail;
        }

        if (workMode == WorkModeSetting.ENHANCED_BLOCKING_MODE && slot == currentPatternIdx) {
            // 读取 NBT 后第一次刷新
            if (currentPattern == null) {
                currentPattern = detail;
            } else if (!currentPattern.equals(detail)) {
                resetCurrentPattern();
            }
        }
    }

    public ICraftingPatternDetails getPatternDetails(int slot) {
        return details[slot];
    }

    // ---------------- 清空缓存 ----------------

    public void returnItemsScheduled() {
        if (!shouldReturnItems) {
            shouldReturnItems = true;
            ModularMachinery.EXECUTE_MANAGER.addSyncTask(this::returnItems);
        }
    }

    private void returnItems() {
        if (!shouldReturnItems || !proxy.isActive() || !proxy.isPowered()) {
            shouldReturnItems = false;
            return;
        }
        shouldReturnItems = false;
        machineCompleted = true;

        synchronized (handler) {
            returnItem(handler);
        }
        for (MachineComponent<?> component : combinationComponents) {
            InfItemFluidHandler h = (InfItemFluidHandler) component.getContainerProvider();
            synchronized (h) {
                returnItem(h);
            }
        }
        markNoUpdateSync();
    }

    private void returnItem(InfItemFluidHandler h) {
        try {
            List<ItemStack> itemStackList = h.getItemStackList();
            List<FluidStack> fluidStackList = h.getFluidStackList();

            IMEMonitor<IAEItemStack> itemInv = proxy.getStorage().getItemInventory();
            for (int i = 0; i < itemStackList.size(); i++) {
                ItemStack stack = itemStackList.get(i);
                if (ItemStacks.isEmpty(stack)) {
                    continue;
                }
                IAEItemStack notInserted = Platform.poweredInsert(proxy.getEnergy(), itemInv, AEApi.instance().storage().createItemStack(stack), source);
                itemStackList.set(i, notInserted == null ? null : notInserted.getItemStack());
            }

            IMEMonitor<IAEFluidStack> fluidInv = proxy.getStorage().getFluidInventory();
            for (int i = 0; i < fluidStackList.size(); i++) {
                FluidStack stack = fluidStackList.get(i);
                if (stack == null) {
                    continue;
                }
                IAEFluidStack notInserted = Platform.poweredInsert(proxy.getEnergy(), fluidInv, AEApi.instance().storage().createFluidStack(stack), source);
                fluidStackList.set(i, notInserted == null ? null : notInserted.getFluidStack());
            }
            itemStackList.removeIf(ItemStacks::isEmpty);
            fluidStackList.removeIf(s -> s == null || s.amount <= 0);
        } catch (GridAccessException ignored) {
        }
    }

    private void resetCurrentPattern() {
        currentPatternIdx = -1;
        currentPattern = null;
    }

    private void setCurrentPattern(final ICraftingPatternDetails pattern) {
        if (pattern == null) {
            resetCurrentPattern();
            return;
        }
        for (int i = 0; i < details.length; i++) {
            if (pattern.equals(details[i])) {
                currentPatternIdx = i;
            }
        }
        currentPattern = pattern;
    }

    // ---------------- 访问 ----------------

    public IOInventory getSubItemHandler() {
        return subItemHandler;
    }

    public FluidTankArray getSubFluidHandler() {
        return subFluidHandler;
    }

    public InfItemFluidHandler getInfHandler() {
        return handler;
    }

    public IOInventory getPatterns() {
        return patterns;
    }

    public WorkModeSetting getWorkMode() {
        return workMode;
    }

    public boolean isMachineCompleted() {
        return machineCompleted;
    }

    public void setWorkMode(final WorkModeSetting workMode) {
        if (this.workMode == WorkModeSetting.ISOLATION_INPUT || workMode == WorkModeSetting.ISOLATION_INPUT) {
            returnItemsScheduled();
        }
        this.workMode = workMode;
        if (workMode != WorkModeSetting.CRAFTING_LOCK_MODE) {
            this.machineCompleted = true;
        }
        if (workMode != WorkModeSetting.ENHANCED_BLOCKING_MODE) {
            resetCurrentPattern();
        }
        markForUpdateSync();
    }

    // ---------------- NBT ----------------

    @Override
    public void readCustomNBT(final NBTTagCompound compound) {
        super.readCustomNBT(compound);
        readProviderNBT(compound);
        if (compound.hasKey("machineCompleted")) {
            machineCompleted = compound.getBoolean("machineCompleted");
        }
    }

    public void readProviderNBT(final NBTTagCompound compound) {
        if (compound.hasKey("subItemHandler")) {
            subItemHandler.readNBT(compound.getCompoundTag("subItemHandler"));
        }
        subFluidHandler.readFromNBT(compound, "subFluidHandler");
        if (compound.hasKey("patterns")) {
            patterns.readNBT(compound.getCompoundTag("patterns"));
        }

        readProviderHandlerNBT(compound);

        if (compound.hasKey("currentPatternIdx") && workMode == WorkModeSetting.ENHANCED_BLOCKING_MODE) {
            currentPatternIdx = compound.getByte("currentPatternIdx");
        } else {
            resetCurrentPattern();
        }
        if (patternsLoaded && worldObj != null && !worldObj.isRemote) {
            refreshPatterns();
        }
    }

    public void readProviderHandlerNBT(final NBTTagCompound compound) {
        int mode = compound.getByte("workMode");
        workMode = mode >= 0 && mode < WorkModeSetting.values().length ? WorkModeSetting.values()[mode] : WorkModeSetting.DEFAULT;
        handler.readFromNBT(compound, "handler");
        NBTTagCompound nbt = compound.getCompoundTag("components");
        for (int i = 0; i < combinationComponents.size(); i++) {
            ((InfItemFluidHandler) combinationComponents.get(i).getContainerProvider()).readFromNBT(nbt, "handler#" + i);
        }
    }

    @Override
    public void writeCustomNBT(final NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        writeProviderNBT(compound);
        compound.setBoolean("machineCompleted", machineCompleted);
    }

    public NBTTagCompound writeProviderNBT(final NBTTagCompound compound) {
        compound.setTag("patterns", patterns.writeNBT());
        compound.setTag("subItemHandler", subItemHandler.writeNBT());
        subFluidHandler.writeToNBT(compound, "subFluidHandler");
        if (!handler.isEmpty() && currentPatternIdx != -1) {
            compound.setByte("currentPatternIdx", (byte) currentPatternIdx);
        }
        if (workMode != WorkModeSetting.DEFAULT) {
            compound.setByte("workMode", (byte) workMode.ordinal());
        }
        handler.writeToNBT(compound, "handler");
        NBTTagCompound nbt = new NBTTagCompound();
        for (int i = 0; i < combinationComponents.size(); i++) {
            InfItemFluidHandler h = (InfItemFluidHandler) combinationComponents.get(i).getContainerProvider();
            if (!h.isEmpty()) {
                h.writeToNBT(nbt, "handler#" + i);
            }
        }
        if (!nbt.hasNoTags()) {
            compound.setTag("components", nbt);
        }
        return compound;
    }

    public boolean isAllDefault() {
        for (int i = 0; i < subItemHandler.getSlots(); i++) {
            if (!ItemStacks.isEmpty(subItemHandler.getStackInSlot(i))) {
                return false;
            }
        }
        if (subFluidHandler.getFluidInSlot(0) != null) {
            return false;
        }
        for (int i = 0; i < patterns.getSlots(); i++) {
            if (!ItemStacks.isEmpty(patterns.getStackInSlot(i))) {
                return false;
            }
        }
        for (MachineComponent<?> component : combinationComponents) {
            if (!((InfItemFluidHandler) component.getContainerProvider()).isEmpty()) {
                return false;
            }
        }
        return workMode == WorkModeSetting.DEFAULT && handler.isEmpty();
    }

    // ---------------- 生命周期 ----------------

    @Override
    public void validate() {
        super.validate();
        if (worldObj != null && !worldObj.isRemote) {
            ModularMachinery.EXECUTE_MANAGER.addSyncTask(this::refreshPatterns);
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        patternsLoaded = false;
    }

    @Override
    public void onMachineEvent(final MachineEvent event) {
        if (event instanceof RecipeFinishEvent || event instanceof FactoryRecipeFinishEvent) {
            if (!machineCompleted) {
                machineCompleted = true;
                ModularMachinery.EXECUTE_MANAGER.addSyncTask(() -> event.getController().setSearchRecipeImmediately(true));
            }
        }
    }

    public enum WorkModeSetting {
        DEFAULT,
        BLOCKING_MODE,
        CRAFTING_LOCK_MODE,
        ENHANCED_BLOCKING_MODE,
        ISOLATION_INPUT
    }
}
