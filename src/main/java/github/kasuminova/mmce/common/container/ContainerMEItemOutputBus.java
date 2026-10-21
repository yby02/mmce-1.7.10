package github.kasuminova.mmce.common.container;

import github.kasuminova.mmce.common.tile.MEItemOutputBus;
import net.minecraft.entity.player.EntityPlayer;

/** ME 机械物品输出总线界面（176×195，AE2 的 skychest.png）：4×9 内部缓存，只显示。 */
public class ContainerMEItemOutputBus extends ContainerMEBase<MEItemOutputBus> {

    public static final int SLOTS_X = 8;
    public static final int SLOTS_Y = 24;

    public ContainerMEItemOutputBus(MEItemOutputBus owner, EntityPlayer opening) {
        super(owner, opening);
    }

    @Override
    protected int playerInventoryY() {
        return 113;
    }
}
