package hellfirepvp.modularmachinery.client.dev;

import github.kasuminova.mmce.common.tile.MEFluidInputBus;
import github.kasuminova.mmce.common.tile.MEItemInputBus;
import github.kasuminova.mmce.common.tile.MEPatternProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fluids.FluidStack;

/**
 * 仅开发环境：客户端场景 ae2 中读取客户端一侧（由方块实体更新同步来的）ME 部件状态。AE2 相关的类只在这里引用，
 * DevClientAutomation 在未装 AE2 的发布环境中也会加载，不能直接引用它们。
 */
final class DevClientAE2 {

    private DevClientAE2() {
    }

    private static TileEntity te(Minecraft mc, int[] pos) {
        return mc.theWorld.getTileEntity(pos[0], pos[1], pos[2]);
    }

    /** 物品输入总线某个设定格的数量（无设定或不是该方块时为 -1）。 */
    static int itemConfigCount(Minecraft mc, int[] pos, int slot) {
        if (te(mc, pos) instanceof MEItemInputBus bus) {
            ItemStack s = bus.getConfigInventory().getStackInSlot(slot);
            return s == null ? -1 : s.stackSize;
        }
        return -1;
    }

    /** 物品输入总线某个设定格的“注册名@meta x 数量”（无设定或不是该方块时为 null）。 */
    static String itemConfig(Minecraft mc, int[] pos, int slot) {
        if (te(mc, pos) instanceof MEItemInputBus bus) {
            ItemStack s = bus.getConfigInventory().getStackInSlot(slot);
            return s == null ? null : net.minecraft.item.Item.itemRegistry.getNameForObject(s.getItem()) + "@" + s.getItemDamage() + " x" + s.stackSize;
        }
        return null;
    }

    /**
     * 像 NEI 放下拖动的物品那样，依次交给 NEI 注册的界面处理器（GuiInfo.guiHandlers）：落点为当前界面内坐标 (guiX, guiY)，
     * 界面尺寸 xSize × ySize（GuiContainer 把界面放在屏幕中央）。返回是否有处理器接收。
     */
    static boolean neiDrop(Minecraft mc, int xSize, int ySize, int guiX, int guiY, ItemStack stack) {
        net.minecraft.client.gui.inventory.GuiContainer gui = (net.minecraft.client.gui.inventory.GuiContainer) mc.currentScreen;
        int x = (gui.width - xSize) / 2 + guiX;
        int y = (gui.height - ySize) / 2 + guiY;
        for (codechicken.nei.api.INEIGuiHandler handler : codechicken.nei.api.GuiInfo.guiHandlers) {
            if (handler.handleDragNDrop(gui, x, y, stack, 0)) {
                return true;
            }
        }
        return false;
    }

    /** 流体输入总线某个设定格的流体名（无设定时为 null）。 */
    static String fluidConfig(Minecraft mc, int[] pos, int slot) {
        if (te(mc, pos) instanceof MEFluidInputBus bus) {
            FluidStack f = bus.getConfig(slot);
            return f == null ? null : f.getFluid().getName();
        }
        return null;
    }

    static int fluidCapacity(Minecraft mc, int[] pos) {
        return te(mc, pos) instanceof MEFluidInputBus bus ? bus.getTanks().getCapacity() : -1;
    }

    static String providerMode(Minecraft mc, int[] pos) {
        return te(mc, pos) instanceof MEPatternProvider p ? p.getWorkMode().name() : null;
    }

    static int providerSubTank(Minecraft mc, int[] pos) {
        if (te(mc, pos) instanceof MEPatternProvider p) {
            FluidStack f = p.getSubFluidHandler().getFluidInSlot(0);
            return f == null ? 0 : f.amount;
        }
        return -1;
    }

    /** 客户端看到的样板供应器缓存条目数（物品 + 流体）。 */
    static int providerCacheEntries(Minecraft mc, int[] pos) {
        if (te(mc, pos) instanceof MEPatternProvider p) {
            int n = 0;
            for (ItemStack s : p.getInfHandler().getItemStackList()) {
                n += s == null ? 0 : 1;
            }
            for (FluidStack f : p.getInfHandler().getFluidStackList()) {
                n += f == null ? 0 : 1;
            }
            return n;
        }
        return -1;
    }
}
