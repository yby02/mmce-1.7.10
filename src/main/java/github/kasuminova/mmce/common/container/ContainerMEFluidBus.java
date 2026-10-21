package github.kasuminova.mmce.common.container;

import github.kasuminova.mmce.common.tile.MEFluidInputBus;
import github.kasuminova.mmce.common.tile.base.MEFluidBus;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fluids.FluidStack;

/**
 * ME 机械流体输入 / 输出总线界面：9 个储罐（只显示），右侧 5 格容量卡槽（同上游，x = 187）。
 * 输入总线另有 9 个流体设定格：拿着装有流体的容器左键设为该流体，空手左键或右键清空。
 */
public class ContainerMEFluidBus extends ContainerMEBase<MEFluidBus> {

    public static final int TANK_X       = 8;
    public static final int INPUT_TANK_Y  = 53;
    public static final int OUTPUT_TANK_Y = 26;
    public static final int CONFIG_Y      = 35;

    public ContainerMEFluidBus(MEFluidBus owner, EntityPlayer opening) {
        super(owner, opening);
        for (int i = 0; i < MEFluidBus.UPGRADE_SLOTS; i++) {
            addSlotToContainer(new SlotFiltered(owner.getUpgrades().asGUIAccess(), i, 187, 8 + 18 * i, MEFluidBus::isCapacityCard));
        }
    }

    public boolean isInput() {
        return owner instanceof MEFluidInputBus;
    }

    @Override
    protected int playerInventoryY() {
        // 与上游相同：输入总线 231 - 82（ContainerUpgradeable），输出总线 195 - 83（ContainerMEFluidOutputBus）
        return owner instanceof MEFluidInputBus ? 149 : 112;
    }

    /** NEI 拖入：流体设定格设为拖入的流体（流体容器或 NEI 的流体显示物品，由客户端解析成流体）。 */
    @Override
    public void onGuiAction(EntityPlayerMP player, int action, int slot, int value, net.minecraft.nbt.NBTTagCompound payload) {
        if (action != ACTION_FLUID_DROP) {
            onGuiAction(player, action, slot, value);
            return;
        }
        if (!(owner instanceof MEFluidInputBus input) || slot < 0 || slot >= MEFluidBus.TANK_SLOT_AMOUNT || payload == null) {
            return;
        }
        FluidStack fluid = FluidStack.loadFluidStackFromNBT(payload);
        if (fluid != null && fluid.getFluid() != null) {
            input.setConfig(slot, fluid);
        }
    }

    @Override
    public void onGuiAction(EntityPlayerMP player, int action, int slot, int value) {
        if (action != ACTION_FLUID_CONFIG || !(owner instanceof MEFluidInputBus input) || slot < 0 || slot >= MEFluidBus.TANK_SLOT_AMOUNT) {
            return;
        }
        FluidStack fluid = value == 0 ? CursorFluidHelper.fluidIn(player.inventory.getItemStack()) : null;
        input.setConfig(slot, fluid);
    }
}
