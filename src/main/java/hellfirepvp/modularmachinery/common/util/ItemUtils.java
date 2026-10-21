/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.util;

import github.kasuminova.mmce.compat.ItemStacks;

import github.kasuminova.mmce.common.helper.AdvancedItemChecker;
import github.kasuminova.mmce.common.util.OredictCache;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.util.nbt.NBTMatchingHelper;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityFurnace;
import github.kasuminova.mmce.compat.NonNullList;
import github.kasuminova.mmce.compat.items.IItemHandler;
import github.kasuminova.mmce.compat.items.IItemHandlerModifiable;
import github.kasuminova.mmce.compat.items.ItemStackHandler;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: ItemUtils
 * Created by HellFirePvP
 * Date: 28.06.2017 / 12:42
 */
public class ItemUtils {

    public static void decrStackInInventory(ItemStackHandler handler, int slot) {
        if (slot < 0 || slot >= handler.getSlots()) {
            return;
        }
        ItemStack st = handler.getStackInSlot(slot);
        if (ItemStacks.isEmpty(st)) {
            return;
        }
        ItemStacks.setCount(st, ItemStacks.getCount(st) - 1);
        if (ItemStacks.getCount(st) <= 0) {
            handler.setStackInSlot(slot, ItemStacks.EMPTY);
        }
    }

    //Negative amount: overhead fuel burnt
    //Positive amount: Failure/couldn't find enough fuel
    public static int consumeFromInventoryFuel(IItemHandlerModifiable handler, int fuelAmtToConsume, boolean simulate, @Nullable NBTTagCompound matchNBTTag) {
        Int2ObjectMap<ItemStack> contents = findItemsIndexedInInventoryFuel(handler, matchNBTTag);
        if (contents.isEmpty()) {
            return fuelAmtToConsume;
        }

        for (int slot : contents.keySet()) {
            ItemStack inSlot = contents.get(slot);
            if (inSlot.getItem().hasContainerItem(inSlot)) {
                if (ItemStacks.getCount(inSlot) > 1) {
                    continue; //uh... rip. we won't consume 16 buckets at once.
                }
                ItemStack stack = ItemStacks.getContainerItem(inSlot);
                fuelAmtToConsume -= TileEntityFurnace.getItemBurnTime(inSlot);
                if (!simulate) {
                    handler.setStackInSlot(slot, ItemStacks.copy(stack));
                }
                if (fuelAmtToConsume <= 0) {
                    break;
                }
            }
            int fuelPer = TileEntityFurnace.getItemBurnTime(inSlot);
            int toConsumeDiv = fuelAmtToConsume / fuelPer;
            int fuelMod = fuelAmtToConsume % fuelPer;

            int toConsume = toConsumeDiv + (fuelMod > 0 ? 1 : 0);
            int toRemove = Math.min(toConsume, ItemStacks.getCount(inSlot));

            fuelAmtToConsume -= toRemove * fuelPer;
            if (!simulate) {
                handler.setStackInSlot(slot, copyStackWithSize(inSlot, ItemStacks.getCount(inSlot) - toRemove));
            }
            if (fuelAmtToConsume <= 0) {
                break;
            }
        }
        return fuelAmtToConsume;
    }

    public static boolean consumeFromInventory(IItemHandlerModifiable handler, ItemStack toConsume, boolean simulate, @Nullable NBTTagCompound matchNBTTag) {
        Int2ObjectMap<ItemStack> contents = findItemsIndexedInInventory(handler, toConsume, false, matchNBTTag);
        if (contents.isEmpty()) {
            return false;
        }

        int cAmt = ItemStacks.getCount(toConsume);
        for (int slot : contents.keySet()) {
            ItemStack inSlot = contents.get(slot);
            if (inSlot.getItem().hasContainerItem(inSlot)) {
                if (ItemStacks.getCount(inSlot) > 1) {
                    continue; //uh... rip. we won't consume 16 buckets at once.
                }
                ItemStack stack = ItemStacks.getContainerItem(inSlot);
                cAmt--;
                if (!simulate) {
                    handler.setStackInSlot(slot, ItemStacks.copy(stack));
                }
                if (cAmt <= 0) {
                    break;
                }
            }
            int toRemove = Math.min(cAmt, ItemStacks.getCount(inSlot));
            cAmt -= toRemove;
            if (!simulate) {
                handler.setStackInSlot(slot, copyStackWithSize(inSlot, ItemStacks.getCount(inSlot) - toRemove));
            }
            if (cAmt <= 0) {
                break;
            }
        }
        return cAmt <= 0;
    }

    public static boolean consumeFromInventory(IItemHandlerModifiable handler, ItemStack toConsume, boolean simulate, AdvancedItemChecker itemChecker, TileMultiblockMachineController controller) {
        Int2ObjectMap<ItemStack> contents = findItemsIndexedInInventory(handler, toConsume, false, itemChecker, controller);
        if (contents.isEmpty()) {
            return false;
        }

        int cAmt = ItemStacks.getCount(toConsume);
        for (int slot : contents.keySet()) {
            ItemStack inSlot = contents.get(slot);
            if (inSlot.getItem().hasContainerItem(inSlot)) {
                if (ItemStacks.getCount(inSlot) > 1) {
                    continue; //uh... rip. we won't consume 16 buckets at once.
                }
                ItemStack stack = ItemStacks.getContainerItem(inSlot);
                cAmt--;
                if (!simulate) {
                    handler.setStackInSlot(slot, ItemStacks.copy(stack));
                }
                if (cAmt <= 0) {
                    break;
                }
            }
            int toRemove = Math.min(cAmt, ItemStacks.getCount(inSlot));
            cAmt -= toRemove;
            if (!simulate) {
                handler.setStackInSlot(slot, copyStackWithSize(inSlot, ItemStacks.getCount(inSlot) - toRemove));
            }
            if (cAmt <= 0) {
                break;
            }
        }
        return cAmt <= 0;
    }

    public static int consumeAll(IItemHandlerModifiable handler, ItemStack toConsume, AdvancedItemChecker itemChecker, TileMultiblockMachineController controller) {
        Int2ObjectMap<ItemStack> contents = findItemsIndexedInInventory(handler, toConsume, false, itemChecker, controller);
        if (ItemStacks.getCount(toConsume) <= 0 || contents.isEmpty()) {
            return 0;
        }
        return consumeAllInternal(handler, contents, ItemStacks.getCount(toConsume));
    }

    public static int consumeAll(IItemHandlerModifiable handler, ItemStack toConsume, @Nullable NBTTagCompound matchNBTTag) {
        Int2ObjectMap<ItemStack> contents = findItemsIndexedInInventory(handler, toConsume, false, matchNBTTag);
        if (ItemStacks.getCount(toConsume) <= 0 || contents.isEmpty()) {
            return 0;
        }
        return consumeAllInternal(handler, contents, ItemStacks.getCount(toConsume));
    }

    public static int consumeAll(IItemHandlerModifiable handler, String oreName, int amount, AdvancedItemChecker itemChecker, TileMultiblockMachineController controller) {
        Int2ObjectMap<ItemStack> contents = findItemsIndexedInInventoryOreDict(handler, oreName, itemChecker, controller);
        if (amount <= 0 || contents.isEmpty()) {
            return 0;
        }
        return consumeAllInternal(handler, contents, amount);
    }

    public static int consumeAll(IItemHandlerModifiable handler, String oreName, int amount, @Nullable NBTTagCompound matchNBTTag) {
        Int2ObjectMap<ItemStack> contents = findItemsIndexedInInventoryOreDict(handler, oreName, matchNBTTag);
        if (amount <= 0 || contents.isEmpty()) {
            return 0;
        }
        return consumeAllInternal(handler, contents, amount);
    }

    public static int damageAll(IItemHandlerModifiable handler, ItemStack toDamage, int amount, int damagePerUse, AdvancedItemChecker itemChecker, TileMultiblockMachineController controller) {
        if (amount <= 0 || damagePerUse <= 0) {
            return 0;
        }
        Int2ObjectMap<ItemStack> contents = findItemsIndexedInInventory(handler, toDamage, false, itemChecker, controller);
        if (contents.isEmpty()) {
            return 0;
        }
        return damageAllInternal(handler, contents, amount, damagePerUse);
    }

    public static int damageAll(IItemHandlerModifiable handler, ItemStack toDamage, int amount, int damagePerUse, @Nullable NBTTagCompound matchNBTTag) {
        if (amount <= 0 || damagePerUse <= 0) {
            return 0;
        }
        Int2ObjectMap<ItemStack> contents = findItemsIndexedInInventory(handler, toDamage, false, matchNBTTag);
        if (contents.isEmpty()) {
            return 0;
        }
        return damageAllInternal(handler, contents, amount, damagePerUse);
    }

    public static int damageAll(IItemHandlerModifiable handler, String oreName, int amount, int damagePerUse, AdvancedItemChecker itemChecker, TileMultiblockMachineController controller) {
        if (amount <= 0 || damagePerUse <= 0) {
            return 0;
        }
        Int2ObjectMap<ItemStack> contents = findItemsIndexedInInventoryOreDict(handler, oreName, itemChecker, controller);
        if (contents.isEmpty()) {
            return 0;
        }
        return damageAllInternal(handler, contents, amount, damagePerUse);
    }

    public static int damageAll(IItemHandlerModifiable handler, String oreName, int amount, int damagePerUse, @Nullable NBTTagCompound matchNBTTag) {
        if (amount <= 0 || damagePerUse <= 0) {
            return 0;
        }
        Int2ObjectMap<ItemStack> contents = findItemsIndexedInInventoryOreDict(handler, oreName, matchNBTTag);
        if (contents.isEmpty()) {
            return 0;
        }
        return damageAllInternal(handler, contents, amount, damagePerUse);
    }

    public static boolean hasDamageableEntry(final String oreDictName) {
        if (oreDictName == null || oreDictName.isEmpty()) {
            return false;
        }
        List<ItemStack> entries = OreDictionary.getOres(oreDictName);
        for (ItemStack entry : entries) {
            if (ItemStacks.isEmpty(entry)) {
                continue;
            }
            // 1.7.10 移植：Item#getSubItems / getCreativeTab 仅客户端存在，服务端无法展开通配 meta 的子物品；
            // 为保证两端结果一致，这里只按物品本身判断（isItemStackDamageable 对通配 meta 同样依据物品耐久）。
            if (entry.isItemStackDamageable()) {
                return true;
            }
        }
        return false;
    }

    public static int insertAll(@Nonnull ItemStack stack, IItemHandlerModifiable handler, int maxInsert) {
        if (ItemStacks.getCount(stack) <= 0) {
            return 0;
        }

        int inserted = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            int maxStackSize = handler.getSlotLimit(i);
            if (maxStackSize <= 64) {
                maxStackSize = Math.min(maxStackSize, stack.getMaxStackSize());
            }
            ItemStack in = handler.getStackInSlot(i);
            int count = ItemStacks.getCount(in);
            if (count >= maxStackSize) {
                continue;
            }

            if (ItemStacks.isEmpty(in)) {
                int toInsert = Math.min(maxInsert - inserted, maxStackSize);
                handler.setStackInSlot(i, copyStackWithSize(stack, toInsert));
                inserted += toInsert;
            } else {
                if (stackEqualsNonNBT(stack, in) && matchTags(stack, in)) {
                    int toInsert = Math.min(maxInsert - inserted, maxStackSize - count);
                    handler.setStackInSlot(i, copyStackWithSize(stack, toInsert + count));
                    inserted += toInsert;
                }
            }

            if (inserted >= maxInsert) {
                break;
            }
        }

        return inserted;
    }

    private static int consumeAllInternal(IItemHandlerModifiable handler, Int2ObjectMap<ItemStack> contents, int maxConsume) {
        int cAmt = 0;
        for (final Int2ObjectMap.Entry<ItemStack> content : contents.int2ObjectEntrySet()) {
            int slot = content.getIntKey();
            ItemStack stack = content.getValue();
            int count = ItemStacks.getCount(stack);
            if (count > 1) {
                if (stack.getItem().hasContainerItem(stack)) {
                    continue; //uh... rip. we won't consume 16 buckets at once.
                }
            }

            int toConsume = Math.min(maxConsume - cAmt, count);
            handler.setStackInSlot(slot, copyStackWithSize(stack, count - toConsume));
            cAmt += toConsume;

            if (cAmt >= maxConsume) {
                break;
            }
        }

        return cAmt;
    }

    private static int damageAllInternal(IItemHandlerModifiable handler, Int2ObjectMap<ItemStack> contents, int maxOperations, int damagePerUse) {
        int operations = 0;
        if (damagePerUse <= 0) {
            return 0;
        }
        for (final Int2ObjectMap.Entry<ItemStack> content : contents.int2ObjectEntrySet()) {
            int slot = content.getIntKey();
            ItemStack stack = content.getValue();
            if (ItemStacks.isEmpty(stack) || !stack.isItemStackDamageable() || stack.getMaxDamage() <= 0) {
                continue;
            }

            while (operations < maxOperations && !ItemStacks.isEmpty(stack)) {
                int newDamage = stack.getItemDamage() + damagePerUse;
                if (newDamage >= stack.getMaxDamage()) {
                    ItemStacks.shrink(stack, 1);
                    if (!ItemStacks.isEmpty(stack)) {
                        stack.setItemDamage(0);
                    }
                } else {
                    stack.setItemDamage(newDamage);
                }
                operations++;
            }

            handler.setStackInSlot(slot, stack);

            if (operations >= maxOperations) {
                break;
            }
        }

        return operations;
    }

    public static boolean stackEqualsNonNBT(@Nonnull ItemStack stack, @Nonnull ItemStack other) {
        if (ItemStacks.isEmpty(stack) && ItemStacks.isEmpty(other)) {
            return true;
        }
        if (ItemStacks.isEmpty(stack) || ItemStacks.isEmpty(other)) {
            return false;
        }
        Item sItem = stack.getItem();
        Item oItem = other.getItem();
        if (sItem.getHasSubtypes() || oItem.getHasSubtypes()) {
            return sItem.equals(other.getItem()) &&
                (stack.getItemDamage() == other.getItemDamage() ||
                    stack.getItemDamage() == OreDictionary.WILDCARD_VALUE ||
                    other.getItemDamage() == OreDictionary.WILDCARD_VALUE);
        } else {
            return sItem.equals(other.getItem());
        }
    }

    public static boolean matchTags(@Nonnull ItemStack stack, @Nonnull ItemStack other) {
        return ItemStacks.areItemStackTagsEqual(stack, other);
    }

    @Nonnull
    public static ItemStack copyStackWithSize(@Nonnull ItemStack stack, int amount) {
        if (ItemStacks.isEmpty(stack) || amount <= 0) {
            return ItemStacks.EMPTY;
        }
        ItemStack s = stack.copy();
        ItemStacks.setCount(s, amount);
        return s;
    }

    /**
     * 向指定容器插入指定的物品，返回未插入的物品。
     *
     * @param external       容器
     * @param willBeInserted 要插入的物品
     * @return 未被插入的物品，如果全部插入，返回空物品
     */
    public static ItemStack insertItemStackToContainer(IItemHandler external, ItemStack willBeInserted) {
        ItemStack beInserted = willBeInserted;
        for (int externalSlotId = 0; externalSlotId < external.getSlots(); externalSlotId++) {
            ItemStack stackInSlot = external.getStackInSlot(externalSlotId);

            if (ItemStacks.isEmpty(stackInSlot)) {
                ItemStack notInserted = external.insertItem(externalSlotId, beInserted, false);
                if (ItemStacks.isEmpty(notInserted)) {
                    return ItemStacks.EMPTY;
                } else {
                    beInserted = notInserted;
                    continue;
                }
            }

            if (matchStacks(stackInSlot, willBeInserted)) {
                ItemStack notInserted = external.insertItem(externalSlotId, beInserted, false);
                if (ItemStacks.isEmpty(notInserted)) {
                    return ItemStacks.EMPTY;
                } else {
                    beInserted = notInserted;
                }
            }
        }

        return beInserted;
    }

    public static Int2ObjectMap<ItemStack> findItemsIndexedInInventoryFuel(IItemHandlerModifiable handler, @Nullable NBTTagCompound matchNBTTag) {
        Int2ObjectMap<ItemStack> stacksOut = new Int2ObjectOpenHashMap<>(handler.getSlots() * 2);
        for (int j = 0; j < handler.getSlots(); j++) {
            ItemStack s = handler.getStackInSlot(j);
            if (TileEntityFurnace.getItemBurnTime(s) > 0 && NBTMatchingHelper.matchNBTCompound(matchNBTTag, s.getTagCompound())) {
                stacksOut.put(j, s);
            }
        }
        return stacksOut;
    }

    public static Int2ObjectMap<ItemStack> findItemsIndexedInInventoryOreDict(IItemHandlerModifiable handler, String oreDict, @Nullable NBTTagCompound matchNBTTag) {
        Int2ObjectMap<ItemStack> stacksOut = new Int2ObjectOpenHashMap<>(handler.getSlots() * 2);
        for (int j = 0; j < handler.getSlots(); j++) {
            ItemStack s = handler.getStackInSlot(j);
            if (ItemStacks.isEmpty(s)) {
                continue;
            }
            int[] ids = OredictCache.getOreIDsFast(s);
            for (int id : ids) {
                if (OreDictionary.getOreName(id).equals(oreDict) && NBTMatchingHelper.matchNBTCompound(matchNBTTag, s.getTagCompound())) {
                    stacksOut.put(j, s);
                    break;
                }
            }
        }
        return stacksOut;
    }

    public static Int2ObjectMap<ItemStack> findItemsIndexedInInventoryOreDict(IItemHandlerModifiable handler, String oreDict, AdvancedItemChecker itemChecker, TileMultiblockMachineController controller) {
        Int2ObjectMap<ItemStack> stacksOut = new Int2ObjectOpenHashMap<>(handler.getSlots() * 2);
        for (int j = 0; j < handler.getSlots(); j++) {
            ItemStack s = handler.getStackInSlot(j);
            if (ItemStacks.isEmpty(s)) {
                continue;
            }
            int[] ids = OredictCache.getOreIDsFast(s);
            for (int id : ids) {
                if (OreDictionary.getOreName(id).equals(oreDict) && itemChecker.isMatch(controller, s)) {
                    stacksOut.put(j, s);
                    break;
                }
            }
        }
        return stacksOut;
    }

    public static Int2ObjectMap<ItemStack> findItemsIndexedInInventory(IItemHandlerModifiable handler, ItemStack match, boolean strict, @Nullable NBTTagCompound matchNBTTag) {
        Int2ObjectMap<ItemStack> stacksOut = new Int2ObjectOpenHashMap<>(handler.getSlots() * 2);
        for (int j = 0; j < handler.getSlots(); j++) {
            ItemStack s = handler.getStackInSlot(j);
            // 1.7.10 移植修正：空槽是 null。上游中数量为 0 的匹配物（配料组前一个候选已足量时）会与空槽“匹配”，
            // 1.12.2 的 ItemStack.EMPTY 不是 null 所以无害；1.7.10 中随后的 s.getTagCompound() 抛 NPE，中断整个配方搜索
            if (ItemStacks.isEmpty(s)) {
                continue;
            }
            if ((strict ? matchStacks(s, match) : matchStackLoosely(s, match)) && NBTMatchingHelper.matchNBTCompound(matchNBTTag, s.getTagCompound())) {
                stacksOut.put(j, s);
            }
        }
        return stacksOut;
    }

    public static Int2ObjectMap<ItemStack> findItemsIndexedInInventory(IItemHandlerModifiable handler, ItemStack match, boolean strict, AdvancedItemChecker itemChecker, TileMultiblockMachineController controller) {
        Int2ObjectMap<ItemStack> stacksOut = new Int2ObjectOpenHashMap<>(handler.getSlots() * 2);
        for (int j = 0; j < handler.getSlots(); j++) {
            ItemStack s = handler.getStackInSlot(j);
            if (ItemStacks.isEmpty(s)) {
                continue;
            }
            if ((strict ? matchStacks(s, match) : matchStackLoosely(s, match)) && itemChecker.isMatch(controller, s)) {
                stacksOut.put(j, s);
            }
        }
        return stacksOut;
    }

    public static boolean matchStacks(@Nonnull ItemStack stack, @Nonnull ItemStack other) {
        if (!ItemStacks.areItemsEqual(stack, other)) {
            return false;
        }
        return ItemStacks.areItemStackTagsEqual(stack, other);
    }

    public static boolean matchStackLoosely(@Nonnull ItemStack stack, @Nonnull ItemStack other) {
        if (ItemStacks.isEmpty(stack)) {
            return ItemStacks.isEmpty(other);
        }
        return OreDictionary.itemMatches(other, stack, false);
    }

    public static boolean stackNotInList(final List<ItemStack> list, final ItemStack stackFromBlockState) {
        for (final ItemStack stack : list) {
            if (matchStacks(stackFromBlockState, stack)) {
                return false;
            }
        }
        return true;
    }

    public static ItemStack getOredictItem(final RecipeCraftingContext context, final String oreDictName, final NBTTagCompound tag) {
        ItemStack stack = ItemStacks.EMPTY;
        for (ItemStack oreInstance : OreDictionary.getOres(oreDictName)) {
            if (ItemStacks.isEmpty(oreInstance)) {
                continue;
            }
            stack = copyStackWithSize(oreInstance, 1);

            if (!ItemStacks.isEmpty(stack)) { //Try all options first..
                break;
            }
        }

        if (ItemStacks.isEmpty(stack)) {
            throw new IllegalArgumentException("Unknown ItemStack: Cannot find an item in oredict '" + oreDictName + "'!");
        }

        if (tag != null) {
            stack.setTagCompound((NBTTagCompound) tag.copy());
        }
        return stack;
    }

    @Nonnull
    public static List<ProcessingComponent<?>> copyItemHandlerComponents(final List<ProcessingComponent<?>> components) {
        List<ProcessingComponent<?>> list = new ArrayList<>();
        for (ProcessingComponent<?> component : components) {
            Object provided = component.getProvidedComponent();
            IItemHandlerImpl handler = null;

            if (provided instanceof IItemHandlerImpl handlerMM) {
                handler = handlerMM.copy();
            } else if (provided instanceof IItemHandlerModifiable handlerDefault) {
                handler = new IItemHandlerImpl(handlerDefault);
            }

            if (handler != null) {
                list.add(new ProcessingComponent<>(
                    (MachineComponent<Object>) component.component(),
                    handler,
                    component.getTag())
                );
            }
        }
        return list;
    }

    @Nonnull
    public static List<ProcessingComponent<?>> fastCopyItemHandlerComponents(final List<ProcessingComponent<?>> components) {
        List<ProcessingComponent<?>> list = new ArrayList<>();
        for (ProcessingComponent<?> component : components) {
            ProcessingComponent<Object> objectProcessingComponent = new ProcessingComponent<>(
                (MachineComponent<Object>) component.component(),
                ((IItemHandlerImpl) component.getProvidedComponent()).fastCopy(),
                component.getTag());
            list.add(objectProcessingComponent);
        }
        return list;
    }
}
