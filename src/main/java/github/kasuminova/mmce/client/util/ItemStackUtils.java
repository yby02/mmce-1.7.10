package github.kasuminova.mmce.client.util;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * 支持超过 127 数量的物品栈 NBT 读写（原版 Count 为 byte）。
 * 1.7.10 移植：ItemStack(NBTTagCompound) 构造器改为 loadItemStackFromNBT；serializeNBT 改为 writeToNBT。
 */
public class ItemStackUtils {

    public static ItemStack readNBTOversize(final NBTTagCompound tag) {
        ItemStack stack = ItemStack.loadItemStackFromNBT(tag);
        if (stack != null) {
            stack.stackSize = tag.getInteger("Count");
        }
        return stack;
    }

    public static NBTTagCompound writeNBTOversize(final ItemStack stack) {
        return writeNBTOversize(stack, new NBTTagCompound());
    }

    public static NBTTagCompound writeNBTOversize(final ItemStack stack, NBTTagCompound tag) {
        stack.writeToNBT(tag);
        if (stack.stackSize > Byte.MAX_VALUE) {
            tag.setInteger("Count", stack.stackSize);
        }
        return tag;
    }

}
