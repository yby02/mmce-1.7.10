package github.kasuminova.mmce.common.integration.ae2;

import github.kasuminova.mmce.common.container.ContainerMEFluidBus;
import github.kasuminova.mmce.common.container.ContainerMEItemInputBus;
import github.kasuminova.mmce.common.container.ContainerMEItemOutputBus;
import github.kasuminova.mmce.common.container.ContainerMEPatternProvider;
import github.kasuminova.mmce.common.tile.MEFluidInputBus;
import github.kasuminova.mmce.common.tile.MEFluidOutputBus;
import github.kasuminova.mmce.common.tile.MEItemInputBus;
import github.kasuminova.mmce.common.tile.MEItemOutputBus;
import github.kasuminova.mmce.common.tile.MEPatternProvider;
import hellfirepvp.modularmachinery.common.CommonProxy;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nullable;

/** ME 部件界面的服务端容器（只在装有 AE2 时调用）。 */
public final class AE2GuiFactory {

    private AE2GuiFactory() {
    }

    @Nullable
    public static Object createContainer(CommonProxy.GuiType type, TileEntity te, EntityPlayer player) {
        return switch (type) {
            case ME_ITEM_INPUT_BUS -> te instanceof MEItemInputBus bus ? new ContainerMEItemInputBus(bus, player) : null;
            case ME_ITEM_OUTPUT_BUS -> te instanceof MEItemOutputBus bus ? new ContainerMEItemOutputBus(bus, player) : null;
            case ME_FLUID_INPUT_BUS -> te instanceof MEFluidInputBus bus ? new ContainerMEFluidBus(bus, player) : null;
            case ME_FLUID_OUTPUT_BUS -> te instanceof MEFluidOutputBus bus ? new ContainerMEFluidBus(bus, player) : null;
            case ME_PATTERN_PROVIDER -> te instanceof MEPatternProvider provider ? new ContainerMEPatternProvider(provider, player) : null;
            default -> null;
        };
    }
}
