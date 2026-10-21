package github.kasuminova.mmce.client.gui.ae2;

import github.kasuminova.mmce.common.container.ContainerMEFluidBus;
import github.kasuminova.mmce.common.container.MEGuiActionHandler;
import github.kasuminova.mmce.common.tile.MEFluidInputBus;
import github.kasuminova.mmce.common.tile.base.MEFluidBus;
import hellfirepvp.modularmachinery.ModularMachinery;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import org.lwjgl.opengl.GL11;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ME 机械流体输入 / 输出总线界面（布局与贴图同上游）：9 个储罐（16×68），输入总线另有 9 个流体设定格；右侧 5 格容量卡槽。
 * 界面宽度含右侧卡槽面板（211），以免点击卡槽被当作点在界面外。
 */
public class GuiMEFluidBus extends GuiMEBase<ContainerMEFluidBus> {

    private static final ResourceLocation TEXTURE_INPUT  = new ResourceLocation(ModularMachinery.MODID, "textures/gui/mefluidinputbus.png");
    private static final ResourceLocation TEXTURE_OUTPUT = new ResourceLocation(ModularMachinery.MODID, "textures/gui/mefluidoutputbus.png");

    private final MEFluidBus bus;
    private final boolean    input;

    public GuiMEFluidBus(MEFluidBus bus, EntityPlayer player) {
        super(new ContainerMEFluidBus(bus, player));
        this.bus = bus;
        this.input = bus instanceof MEFluidInputBus;
        this.xSize = 211;
        this.ySize = input ? 231 : 204;
    }

    private int tankY() {
        return input ? ContainerMEFluidBus.INPUT_TANK_Y : ContainerMEFluidBus.OUTPUT_TANK_Y;
    }

    private int hoveredTank(int mouseX, int mouseY) {
        for (int i = 0; i < MEFluidBus.TANK_SLOT_AMOUNT; i++) {
            if (isHovering(mouseX, mouseY, ContainerMEFluidBus.TANK_X + 18 * i, tankY(), 16, 68)) {
                return i;
            }
        }
        return -1;
    }

    private int hoveredConfig(int mouseX, int mouseY) {
        if (!input) {
            return -1;
        }
        for (int i = 0; i < MEFluidBus.TANK_SLOT_AMOUNT; i++) {
            if (isHovering(mouseX, mouseY, ContainerMEFluidBus.TANK_X + 18 * i, ContainerMEFluidBus.CONFIG_Y, 16, 16)) {
                return i;
            }
        }
        return -1;
    }

    /** 输入总线：把流体容器或 NEI 的流体显示物品拖到流体设定格上，设为其中的流体。 */
    @Override
    public boolean acceptNeiDrop(int mouseX, int mouseY, net.minecraft.item.ItemStack stack, FluidStack fluid) {
        int config = hoveredConfig(mouseX, mouseY);
        if (config < 0 || fluid == null || fluid.getFluid() == null) {
            return false;
        }
        sendAction(MEGuiActionHandler.ACTION_FLUID_DROP, config, 0, fluid.writeToNBT(new net.minecraft.nbt.NBTTagCompound()));
        return true;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1F, 1F, 1F, 1F);
        mc.getTextureManager().bindTexture(input ? TEXTURE_INPUT : TEXTURE_OUTPUT);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        if (input) {
            fontRendererObj.drawString(I18n.format("gui.mefluidinputbus.title"), 8, 6, 0x404040);
            fontRendererObj.drawString(I18n.format("gui.mmce.me.config"), 8, 24, 0x404040);
            fontRendererObj.drawString(I18n.format("gui.mmce.me.stored_fluids"), 8, 125, 0x404040);
            fontRendererObj.drawString(I18n.format("container.inventory"), 8, ySize - 93, 0x404040);
        } else {
            fontRendererObj.drawString(I18n.format("gui.mefluidoutputbus.title"), 8, 8, 0x404040);
            fontRendererObj.drawString(I18n.format("container.inventory"), 8, ySize - 104, 0x404040);
        }

        int capacity = bus.getTanks().getCapacity();
        for (int i = 0; i < MEFluidBus.TANK_SLOT_AMOUNT; i++) {
            FluidStack fluid = bus.getTanks().getFluidInSlot(i);
            if (fluid != null && capacity > 0) {
                drawFluid(fluid, ContainerMEFluidBus.TANK_X + 18 * i, tankY(), 16, 68, (float) fluid.amount / capacity);
            }
            if (input) {
                FluidStack config = ((MEFluidInputBus) bus).getConfig(i);
                if (config != null) {
                    drawFluid(config, ContainerMEFluidBus.TANK_X + 18 * i, ContainerMEFluidBus.CONFIG_Y, 16, 16, 1F);
                }
            }
        }
        int tank = hoveredTank(mouseX, mouseY);
        if (tank >= 0) {
            drawHighlight(ContainerMEFluidBus.TANK_X + 18 * tank, tankY(), 16, 68);
        }
        int config = hoveredConfig(mouseX, mouseY);
        if (config >= 0) {
            drawHighlight(ContainerMEFluidBus.TANK_X + 18 * config, ContainerMEFluidBus.CONFIG_Y, 16, 16);
        }
    }

    @Override
    protected void renderHoveredToolTip(int mouseX, int mouseY) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        int tank = hoveredTank(mouseX, mouseY);
        if (tank >= 0) {
            List<String> lines = new ArrayList<>();
            FluidStack fluid = bus.getTanks().getFluidInSlot(tank);
            lines.add(fluid == null ? I18n.format("tooltip.fluidhatch.empty") : fluid.getLocalizedName());
            lines.add(EnumChatFormatting.GRAY + I18n.format("tooltip.fluidhatch.tank", nf.format(fluid == null ? 0 : fluid.amount), nf.format(bus.getTanks().getCapacity())));
            lines.add(EnumChatFormatting.GRAY + I18n.format("gui.mmce.me.capacity_cards", bus.getInstalledCapacityCards()));
            drawTooltip(lines, mouseX, mouseY);
            return;
        }
        int config = hoveredConfig(mouseX, mouseY);
        if (config >= 0) {
            List<String> lines = new ArrayList<>();
            FluidStack fluid = ((MEFluidInputBus) bus).getConfig(config);
            if (fluid != null) {
                lines.add(fluid.getLocalizedName());
            }
            lines.add(EnumChatFormatting.GRAY + I18n.format("gui.mmce.me.fluid_config.desc"));
            drawTooltip(lines, mouseX, mouseY);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        int config = hoveredConfig(mouseX, mouseY);
        if (config >= 0 && (button == 0 || button == 1)) {
            sendAction(MEGuiActionHandler.ACTION_FLUID_CONFIG, config, button);
            return;
        }
        super.mouseClicked(mouseX, mouseY, button);
    }
}
