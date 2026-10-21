package github.kasuminova.mmce.common.container;

import github.kasuminova.mmce.common.tile.MEPatternProvider;
import github.kasuminova.mmce.compat.items.SlotItemHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

/**
 * ME 机械样板供应器界面（256×196，贴图 mepatternprovider.png）：4×9 样板格、独立存储（2 个物品格 + 1 个流体槽）、
 * 右侧缓存列表、“清空缓存”与工作模式按钮。位置同上游。
 */
public class ContainerMEPatternProvider extends ContainerMEBase<MEPatternProvider> {

    public static final int SUB_TANK_X = 232;
    public static final int SUB_TANK_Y = 172;

    public ContainerMEPatternProvider(MEPatternProvider owner, EntityPlayer opening) {
        super(owner, opening);
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new SlotFiltered(owner.getPatterns().asGUIAccess(), row * 9 + col, 8 + col * 18, 28 + row * 18, MEPatternProvider::isPattern));
            }
        }
        for (int i = 0; i < MEPatternProvider.SUB_ITEM_HANDLER_SLOTS; i++) {
            addSlotToContainer(new SlotItemHandler(owner.getSubItemHandler().asGUIAccess(), i, 181 + i * 18, 172));
        }
    }

    @Override
    protected int playerInventoryY() {
        return 114;
    }

    @Override
    public void onGuiAction(EntityPlayerMP player, int action, int slot, int value) {
        switch (action) {
            case ACTION_WORK_MODE -> {
                MEPatternProvider.WorkModeSetting[] modes = MEPatternProvider.WorkModeSetting.values();
                if (value >= 0 && value < modes.length) {
                    owner.setWorkMode(modes[value]);
                }
            }
            case ACTION_RETURN_ITEMS -> owner.returnItemsScheduled();
            case ACTION_SUB_TANK -> CursorFluidHelper.interact(player, owner.getSubFluidHandler());
            default -> {
            }
        }
    }
}
