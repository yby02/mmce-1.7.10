package hellfirepvp.modularmachinery.client.nei;

import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;
import codechicken.nei.recipe.GuiRecipeTab;
import codechicken.nei.recipe.HandlerInfo;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.Tags;
import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.block.BlockFactoryController;
import hellfirepvp.modularmachinery.common.lib.ItemsMM;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * NEI 插件入口（NEI 会自动发现类名以 NEI 开头、以 Config 结尾并实现 IConfigureNEI 的类；未安装 NEI 时本包不会被加载）。
 * <ul>
 *   <li>每台机器注册一个配方页，控制器与工厂控制器作为催化剂（对其按 U 显示该机器全部配方）；</li>
 *   <li>注册一个共用的结构预览页（对控制器 / 蓝图按 R，对方块按 U）。</li>
 * </ul>
 */
public class NEIModularMachineryConfig implements IConfigureNEI {

    @Override
    public void loadConfig() {
        int handlers = 0;
        for (DynamicMachine machine : MachineRegistry.getLoadedMachines()) {
            MachineRecipeHandler handler = new MachineRecipeHandler(machine);
            API.registerRecipeHandler(handler);
            API.registerUsageHandler(handler);
            String icon = null;
            BlockController controller = BlockController.getControllerWithMachine(machine);
            if (controller != null) {
                API.addRecipeCatalyst(new ItemStack(controller), handler);
                icon = Item.itemRegistry.getNameForObject(Item.getItemFromBlock(controller));
            }
            BlockFactoryController factory = BlockFactoryController.FACTORY_CONTROLLERS.get(machine);
            if (factory != null) {
                API.addRecipeCatalyst(new ItemStack(factory), handler);
                if (icon == null) {
                    icon = Item.itemRegistry.getNameForObject(Item.getItemFromBlock(factory));
                }
            }
            int height = Math.max(40, handler.layout().height + 6);
            registerInfo(handler.getHandlerId(), icon, height, Math.max(1, Math.min(4, 130 / height)));
            handlers++;
        }

        StructurePreviewHandler preview = new StructurePreviewHandler();
        API.registerRecipeHandler(preview);
        API.registerUsageHandler(preview);
        if (ItemsMM.blueprint != null) {
            API.addRecipeCatalyst(new ItemStack(ItemsMM.blueprint), preview);
        }
        registerInfo(StructurePreviewHandler.ID, Item.itemRegistry.getNameForObject(ItemsMM.blueprint), 200, 1);
        if (hellfirepvp.modularmachinery.common.base.Mods.AE2.isPresent()) {
            API.registerNEIGuiHandler(new MEGuiNeiDragHandler());
        }
        ModularMachinery.log.info("[ModularMachinery] NEI: registered {} machine recipe handlers and the structure preview.", handlers);
    }

    private static void registerInfo(String handlerId, String iconItem, int height, int perPage) {
        HandlerInfo info = new HandlerInfo(handlerId, getModName(), ModularMachinery.MODID, false, null);
        if (iconItem != null) {
            info.setItem(iconItem, null);
        }
        info.setHandlerDimensions(height, 0, perPage);
        // loadHandlerInfo() 会清空 handlerMap 后再合并 IMC 表，两边都放
        GuiRecipeTab.handlerAdderFromIMC.put(handlerId, info);
        GuiRecipeTab.handlerMap.put(handlerId, info);
    }

    private static String getModName() {
        return "Modular Machinery: Community Edition";
    }

    @Override
    public String getName() {
        return getModName();
    }

    @Override
    public String getVersion() {
        return Tags.VERSION;
    }
}
