/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.registry.internal;

import cpw.mods.fml.common.registry.GameRegistry;
import github.kasuminova.mmce.compat.ResourceLocation;
import github.kasuminova.mmce.compat.registry.IRegistryNamed;
import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.crafting.adapter.RecipeAdapter;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementType;
import hellfirepvp.modularmachinery.common.crafting.tooltip.RequirementTip;
import hellfirepvp.modularmachinery.common.lib.RegistriesMM;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: InternalRegistryPrimer
 * Created by HellFirePvP
 * Date: 13.07.2019 / 09:06
 * <p>
 * 1.7.10 移植：1.12.2 先“预注册”再在 RegistryEvent 中统一提交；1.7.10 在 preInit 中直接注册：
 * - 方块：GameRegistry.registerBlock(block, null, name)，其 ItemBlock 随后以同名 registerItem 注册（FML 会将二者 ID 对齐）
 * - 物品：GameRegistry.registerItem(item, name)
 * - MMCE 自定义类型：写入对应的 {@link hellfirepvp.modularmachinery.common.lib.RegistriesMM} 注册表
 * 注册名取自 1.12.2 风格的 registryName 的 path（命名空间固定为本模组）。
 */
public class InternalRegistryPrimer {

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <V> V register(V entry) {
        if (entry instanceof RequirementType) {
            RegistriesMM.REQUIREMENT_TYPE_REGISTRY.register((RequirementType) entry);
        } else if (entry instanceof ComponentType) {
            RegistriesMM.COMPONENT_TYPE_REGISTRY.register((ComponentType) entry);
        } else if (entry instanceof RecipeAdapter) {
            RegistriesMM.ADAPTER_REGISTRY.register((RecipeAdapter) entry);
        } else if (entry instanceof RequirementTip) {
            RegistriesMM.REQUIREMENT_TIPS_REGISTRY.register((RequirementTip) entry);
        } else if (entry instanceof Block) {
            GameRegistry.registerBlock((Block) entry, null, nameOf(entry));
        } else if (entry instanceof Item) {
            Item item = (Item) entry;
            String name = nameOf(entry);
            if (item instanceof ItemBlock && name == null) {
                ResourceLocation blockName = IRegistryNamed.getName(((ItemBlock) item).field_150939_a);
                name = blockName == null ? null : blockName.getPath();
            }
            if (name == null) {
                throw new IllegalArgumentException("Item without registry name: " + item);
            }
            GameRegistry.registerItem(item, name);
        } else {
            throw new IllegalArgumentException("Unsupported registry entry: " + entry);
        }
        return entry;
    }

    private static String nameOf(Object entry) {
        ResourceLocation rl = IRegistryNamed.getName(entry);
        return rl == null ? null : rl.getPath();
    }

}
