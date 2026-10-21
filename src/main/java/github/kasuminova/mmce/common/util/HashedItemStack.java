package github.kasuminova.mmce.common.util;

import github.kasuminova.mmce.compat.RegistryNames;

import github.kasuminova.mmce.compat.ItemStacks;

import com.github.bsideup.jabel.Desugar;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nonnull;
import java.util.Base64;
import java.util.Objects;

/**
 * A simple wrapper for ItemStack, which can be used as a key in a Map.
 * From StellarCore.
 */
@Desugar
public record HashedItemStack(ItemStack stack, int stackHashCode, boolean hasTag) {

    /**
     * Create a HashedItemStack from an ItemStack copy.
     */
    public static HashedItemStack of(@Nonnull final ItemStack stack) {
        return ofUnsafe(stack.copy());
    }

    /**
     * Create a HashedItemStack from an ItemStack without copy.
     */
    public static HashedItemStack ofUnsafe(@Nonnull final ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        boolean hasTag = tag != null && !tag.hasNoTags();
        int hash;
        if (hasTag) {
            hash = Objects.hash(stack.getItem(),
                stack.isItemStackDamageable() ? stack.getItemDamage() : stack.getHasSubtypes() ? stack.getItemDamage() : 0, tag);
        } else {
            hash = Objects.hash(stack.getItem(),
                stack.isItemStackDamageable() ? stack.getItemDamage() : stack.getHasSubtypes() ? stack.getItemDamage() : 0);
        }
        return new HashedItemStack(stack, hash, hasTag);
    }

    public static HashedItemStack ofMeta(@Nonnull final ItemStack stack) {
        return ofMetaUnsafe(stack.copy());
    }

    public static HashedItemStack ofMetaUnsafe(@Nonnull final ItemStack stack) {
        return new HashedItemStack(stack, Objects.hash(stack.getItem(), stack.getItemDamage()), false);
    }

    public static String stackToString(@Nonnull final ItemStack stack) {
        String stackTagStr = null;
        String registryName = Objects.requireNonNull(RegistryNames.getName(stack.getItem())).toString();
        if (stack.getTagCompound() != null) {
            stackTagStr = stack.getTagCompound().toString();
        }
        return strToBase64(registryName) + "_" + stack.getItemDamage() + (stackTagStr == null ? "" : "_" + strToBase64(stackTagStr));
    }

    public static String strToBase64(final String str) {
        return Base64.getEncoder().encodeToString(str.getBytes());
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

    @Override
    public boolean equals(final Object o) {
        if (o instanceof HashedItemStack hashedItemStack) {
            if (hasTag && !hashedItemStack.hasTag) {
                return false;
            }
            return stackEqualsNonNBT(stack, hashedItemStack.stack) && (!hasTag || ItemStack.areItemStackTagsEqual(stack, hashedItemStack.stack));
        }
        return false;
    }

    public HashedItemStack copy() {
        return new HashedItemStack(stack.copy(), stackHashCode, hasTag);
    }

    @Override
    public int hashCode() {
        return stackHashCode;
    }
}
