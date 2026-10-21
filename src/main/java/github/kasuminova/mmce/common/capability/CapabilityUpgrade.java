package github.kasuminova.mmce.common.capability;

import github.kasuminova.mmce.common.upgrade.DynamicMachineUpgrade;
import github.kasuminova.mmce.common.upgrade.MachineUpgrade;
import github.kasuminova.mmce.common.upgrade.registry.RegistryUpgrade;
import github.kasuminova.mmce.compat.ItemStacks;
import github.kasuminova.mmce.compat.ResourceLocation;
import hellfirepvp.modularmachinery.ModularMachinery;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 机械升级数据。
 * <p>
 * 1.7.10 移植：1.12.2 通过 AttachCapabilitiesEvent 把本对象作为 ItemStack 能力挂载，并随物品 NBT 中的 ForgeCaps 序列化。
 * 1.7.10 没有物品能力系统，改为：
 * - {@link #get(ItemStack)} 按 ItemStack 实例缓存（弱引用），首次访问时从物品 NBT 的 {@link #NBT_KEY} 读取，缺失则使用注册的默认升级；
 * - 持有者（如升级总线）在保存前调用 {@link #syncToNBT(ItemStack)} 写回物品 NBT。
 */
public class CapabilityUpgrade {
    public static final  ResourceLocation CAPABILITY_NAME    = new ResourceLocation(ModularMachinery.MODID, "upgrade_cap");
    public static final  String           NBT_KEY            = "mmceUpgradeCap";
    private static final NBTTagCompound   EMPTY_TAG_COMPOUND = new NBTTagCompound();

    private static final Map<ItemStack, CapabilityUpgrade> CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    private final List<MachineUpgrade> upgrades = new ArrayList<>();

    /** 保留与 1.12.2 相同的调用点；1.7.10 无需注册能力。 */
    public static void register() {
    }

    /**
     * 获取物品上的升级数据，物品不支持升级时返回 null（对应 1.12.2 的 stack.getCapability(MACHINE_UPGRADE_CAPABILITY, null)）。
     */
    @Nullable
    public static CapabilityUpgrade get(ItemStack stack) {
        if (ItemStacks.isEmpty(stack) || !RegistryUpgrade.supportsUpgrade(stack)) {
            return null;
        }
        synchronized (CACHE) {
            CapabilityUpgrade cached = CACHE.get(stack);
            if (cached != null) {
                return cached;
            }
            CapabilityUpgrade upgrade = new CapabilityUpgrade();
            NBTTagCompound tag = stack.getTagCompound();
            if (tag != null && tag.hasKey(NBT_KEY)) {
                upgrade.readNBT(tag.getCompoundTag(NBT_KEY));
            } else {
                List<MachineUpgrade> upgradeList = RegistryUpgrade.getItemUpgradeList(stack);
                if (upgradeList != null) {
                    upgradeList.forEach(u -> upgrade.getUpgrades().add(u.copy(stack)));
                }
            }
            CACHE.put(stack, upgrade);
            return upgrade;
        }
    }

    /** 把缓存中的升级数据写回物品 NBT（仅当该物品已被访问过）。 */
    public static void syncToNBT(ItemStack stack) {
        if (ItemStacks.isEmpty(stack)) {
            return;
        }
        CapabilityUpgrade cached = CACHE.get(stack);
        if (cached == null) {
            return;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setTag(NBT_KEY, cached.writeNBT());
    }

    public List<MachineUpgrade> getUpgrades() {
        return upgrades;
    }

    @SuppressWarnings("unchecked")
    public void readNBT(NBTTagCompound tag) {
        upgrades.clear();
        for (final String upgradeType : (java.util.Set<String>) tag.func_150296_c()) {
            MachineUpgrade upgrade = RegistryUpgrade.getUpgrade(upgradeType);
            if (upgrade == null) {
                continue;
            }

            upgrade = upgrade.copy(ItemStacks.EMPTY);
            upgrades.add(upgrade);
            if (upgrade instanceof DynamicMachineUpgrade) {
                NBTTagCompound upgradeTag = tag.getCompoundTag(upgradeType);
                ((DynamicMachineUpgrade) upgrade).readItemNBT(upgradeTag);
            }
        }
    }

    public NBTTagCompound writeNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        for (final MachineUpgrade upgrade : upgrades) {
            if (upgrade instanceof DynamicMachineUpgrade) {
                tag.setTag(upgrade.getType().getName(), ((DynamicMachineUpgrade) upgrade).writeItemNBT());
            } else {
                tag.setTag(upgrade.getType().getName(), EMPTY_TAG_COMPOUND.copy());
            }
        }

        return tag;
    }
}
