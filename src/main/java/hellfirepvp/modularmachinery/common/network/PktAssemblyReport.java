package hellfirepvp.modularmachinery.common.network;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.kasuminova.mmce.compat.ItemStacks;
import github.kasuminova.mmce.compat.NBTs;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.FluidStack;
import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * 自动组装缺少材料时，把缺少的物品 / 流体清单发给客户端显示在聊天栏。
 * 1.7.10 移植：NBT 列表读取改用 {@link NBTs}；客户端处理放在独立的 @SideOnly 方法中。
 */
public class PktAssemblyReport implements IMessage, IMessageHandler<PktAssemblyReport, IMessage> {

    private List<List<ItemStack>>  itemStackIngList  = new ArrayList<>();
    private List<List<FluidStack>> fluidStackIngList = new ArrayList<>();

    public PktAssemblyReport() {
    }

    public PktAssemblyReport(final List<List<ItemStack>> itemStackIngList, final List<List<FluidStack>> fluidStackIngList) {
        this.itemStackIngList = itemStackIngList;
        this.fluidStackIngList = fluidStackIngList;
    }

    private static String itemStackToString(ItemStack stack) {
        return ItemStacks.getCount(stack) + "x " + stack.getDisplayName();
    }

    private static String fluidStackToString(FluidStack stack) {
        return stack.amount + "mb " + stack.getLocalizedName();
    }

    private static <T> void sendList(List<List<T>> lists, java.util.function.Function<T, String> toText, EntityPlayer player) {
        for (final List<T> list : lists) {
            if (list.isEmpty()) {
                continue;
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < list.size(); i++) {
                sb.append(toText.apply(list.get(i)));
                if (i + 1 < list.size()) {
                    sb.append(' ').append(StatCollector.translateToLocal("message.assembly.tip.or")).append(' ');
                }
            }
            player.addChatMessage(new ChatComponentText(sb.toString()));
        }
    }

    @Override
    public void fromBytes(final ByteBuf buf) {
        NBTTagCompound tag = ByteBufUtils.readTag(buf);
        if (tag == null) {
            return;
        }
        for (NBTBase base : NBTs.elements(tag.getTagList("itemStackList", Constants.NBT.TAG_LIST))) {
            List<ItemStack> list = new ArrayList<>();
            NBTTagList stackTags = (NBTTagList) base;
            for (int i = 0; i < stackTags.tagCount(); i++) {
                NBTTagCompound stackTag = stackTags.getCompoundTagAt(i);
                ItemStack stack = stackTag.hasNoTags() ? null : ItemStack.loadItemStackFromNBT(stackTag);
                if (!ItemStacks.isEmpty(stack)) {
                    list.add(stack);
                }
            }
            if (!list.isEmpty()) {
                itemStackIngList.add(list);
            }
        }
        for (NBTBase base : NBTs.elements(tag.getTagList("fluidStackList", Constants.NBT.TAG_LIST))) {
            List<FluidStack> list = new ArrayList<>();
            NBTTagList stackTags = (NBTTagList) base;
            for (int i = 0; i < stackTags.tagCount(); i++) {
                NBTTagCompound stackTag = stackTags.getCompoundTagAt(i);
                FluidStack stack = stackTag.hasNoTags() ? null : FluidStack.loadFluidStackFromNBT(stackTag);
                if (stack != null) {
                    list.add(stack);
                }
            }
            if (!list.isEmpty()) {
                fluidStackIngList.add(list);
            }
        }
    }

    @Override
    public void toBytes(final ByteBuf buf) {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList items = new NBTTagList();
        for (final List<ItemStack> list : itemStackIngList) {
            NBTTagList stackTags = new NBTTagList();
            for (final ItemStack stack : list) {
                if (!ItemStacks.isEmpty(stack)) {
                    stackTags.appendTag(stack.writeToNBT(new NBTTagCompound()));
                }
            }
            items.appendTag(stackTags);
        }
        tag.setTag("itemStackList", items);
        NBTTagList fluids = new NBTTagList();
        for (final List<FluidStack> list : fluidStackIngList) {
            NBTTagList stackTags = new NBTTagList();
            for (final FluidStack stack : list) {
                stackTags.appendTag(stack.writeToNBT(new NBTTagCompound()));
            }
            fluids.appendTag(stackTags);
        }
        tag.setTag("fluidStackList", fluids);
        ByteBufUtils.writeTag(buf, tag);
    }

    @Override
    public IMessage onMessage(final PktAssemblyReport message, final MessageContext ctx) {
        if (ctx.side == Side.CLIENT) {
            showOnClient(message);
        }
        return null;
    }

    @SideOnly(Side.CLIENT)
    private static void showOnClient(PktAssemblyReport message) {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player == null) {
            return;
        }
        player.addChatMessage(new ChatComponentTranslation("message.assembly.tip.required"));
        sendList(message.itemStackIngList, PktAssemblyReport::itemStackToString, player);
        sendList(message.fluidStackIngList, PktAssemblyReport::fluidStackToString, player);
    }
}
