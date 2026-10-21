package github.kasuminova.mmce.client.gui.ae2;

import github.kasuminova.mmce.common.tile.MEFluidInputBus;
import github.kasuminova.mmce.common.tile.MEFluidOutputBus;
import github.kasuminova.mmce.common.tile.MEItemInputBus;
import github.kasuminova.mmce.common.tile.MEItemOutputBus;
import github.kasuminova.mmce.common.tile.MEPatternProvider;
import hellfirepvp.modularmachinery.common.CommonProxy;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nullable;

/** ME 部件的客户端界面（只在装有 AE2 时调用）。 */
public final class AE2ClientGuiFactory {

    private AE2ClientGuiFactory() {
    }

    @Nullable
    public static Object createGui(CommonProxy.GuiType type, TileEntity te, EntityPlayer player) {
        return switch (type) {
            case ME_ITEM_INPUT_BUS -> te instanceof MEItemInputBus bus ? new GuiMEItemInputBus(bus, player) : null;
            case ME_ITEM_OUTPUT_BUS -> te instanceof MEItemOutputBus bus ? new GuiMEItemOutputBus(bus, player) : null;
            case ME_FLUID_INPUT_BUS -> te instanceof MEFluidInputBus bus ? new GuiMEFluidBus(bus, player) : null;
            case ME_FLUID_OUTPUT_BUS -> te instanceof MEFluidOutputBus bus ? new GuiMEFluidBus(bus, player) : null;
            case ME_PATTERN_PROVIDER -> te instanceof MEPatternProvider provider ? new GuiMEPatternProvider(provider, player) : null;
            default -> null;
        };
    }
}
