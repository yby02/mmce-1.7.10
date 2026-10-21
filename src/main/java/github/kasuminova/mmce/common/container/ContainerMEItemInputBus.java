package github.kasuminova.mmce.common.container;

import github.kasuminova.mmce.common.tile.MEItemInputBus;
import github.kasuminova.mmce.compat.ItemStacks;
import hellfirepvp.modularmachinery.common.util.ItemUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

/**
 * ME 机械物品输入总线界面（176×204，贴图 meiteminputbus.png）：左侧 4×4 设定格（标记物品与数量），右侧 4×4 内部缓存（只显示）。
 * 设定格的操作同上游（AE2 的“假物品格”）：手持物品左键设为该物品（数量同手持）、右键设为 1 个或 +1；空手左键清空、右键 -1；
 * 滚轮 ±1，按住 SHIFT ±10、CTRL ±100、SHIFT+CTRL 翻倍 / 减半。
 */
public class ContainerMEItemInputBus extends ContainerMEBase<MEItemInputBus> {

    public static final int CONFIG_X = 8;
    public static final int STORED_X = 98;
    public static final int SLOTS_Y  = 35;

    public ContainerMEItemInputBus(MEItemInputBus owner, EntityPlayer opening) {
        super(owner, opening);
    }

    @Override
    protected int playerInventoryY() {
        return 123;
    }

    /** NEI 拖入：设定格设为拖入的物品（只是标记，不产生物品），数量同拖入的堆叠（至少 1）。 */
    @Override
    public void onGuiAction(EntityPlayerMP player, int action, int slot, int value, net.minecraft.nbt.NBTTagCompound payload) {
        if (action != ACTION_CONFIG_DROP) {
            onGuiAction(player, action, slot, value);
            return;
        }
        if (slot < 0 || slot >= MEItemInputBus.SLOTS || payload == null) {
            return;
        }
        ItemStack dropped = ItemStack.loadItemStackFromNBT(payload);
        if (ItemStacks.isEmpty(dropped) || dropped.getItem() == null) {
            return;
        }
        owner.setConfig(slot, ItemUtils.copyStackWithSize(dropped, Math.max(1, dropped.stackSize)));
    }

    @Override
    public void onGuiAction(EntityPlayerMP player, int action, int slot, int value) {
        if (slot < 0 || slot >= MEItemInputBus.SLOTS) {
            return;
        }
        ItemStack config = owner.getConfigInventory().getStackInSlot(slot);
        if (action == ACTION_CONFIG_AMOUNT) {
            if (!ItemStacks.isEmpty(config) && value > 0) {
                owner.setConfig(slot, ItemUtils.copyStackWithSize(config, value));
            }
            return;
        }
        if (action != ACTION_CONFIG_CLICK) {
            return;
        }
        ItemStack held = player.inventory.getItemStack();
        boolean right = value == 1;
        if (ItemStacks.isEmpty(held)) {
            if (ItemStacks.isEmpty(config)) {
                return;
            }
            if (right && config.stackSize > 1) {
                owner.setConfig(slot, ItemUtils.copyStackWithSize(config, config.stackSize - 1));
            } else {
                owner.setConfig(slot, ItemStacks.EMPTY);
            }
            return;
        }
        if (!right) {
            owner.setConfig(slot, held.copy());
        } else if (!ItemStacks.isEmpty(config) && ItemUtils.matchStacks(config, held) && config.stackSize < Integer.MAX_VALUE) {
            owner.setConfig(slot, ItemUtils.copyStackWithSize(config, config.stackSize + 1));
        } else {
            owner.setConfig(slot, ItemUtils.copyStackWithSize(held, 1));
        }
    }
}
