package github.kasuminova.mmce.client.gui.ae2;

import github.kasuminova.mmce.common.container.ContainerMEItemInputBus;
import github.kasuminova.mmce.common.container.MEGuiActionHandler;
import github.kasuminova.mmce.common.tile.MEItemInputBus;
import github.kasuminova.mmce.compat.ItemStacks;
import hellfirepvp.modularmachinery.ModularMachinery;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** ME 机械物品输入总线界面（布局与贴图同上游 GuiMEItemInputBus；设定格的操作见 {@link ContainerMEItemInputBus}）。 */
public class GuiMEItemInputBus extends GuiMEBase<ContainerMEItemInputBus> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(ModularMachinery.MODID, "textures/gui/meiteminputbus.png");

    private final MEItemInputBus bus;

    public GuiMEItemInputBus(MEItemInputBus bus, EntityPlayer player) {
        super(new ContainerMEItemInputBus(bus, player));
        this.bus = bus;
        this.xSize = 176;
        this.ySize = 204;
    }

    private static int slotX(int base, int slot) {
        return base + 18 * (slot % 4);
    }

    private static int slotY(int slot) {
        return ContainerMEItemInputBus.SLOTS_Y + 18 * (slot / 4);
    }

    /** 鼠标所在的格：返回 [0, 16) 为设定格，[16, 32) 为缓存格，-1 为都不在。 */
    private int hoveredSlot(int mouseX, int mouseY) {
        for (int i = 0; i < MEItemInputBus.SLOTS; i++) {
            if (isHovering(mouseX, mouseY, slotX(ContainerMEItemInputBus.CONFIG_X, i), slotY(i), 16, 16)) {
                return i;
            }
            if (isHovering(mouseX, mouseY, slotX(ContainerMEItemInputBus.STORED_X, i), slotY(i), 16, 16)) {
                return MEItemInputBus.SLOTS + i;
            }
        }
        return -1;
    }

    /** 拖到设定格上：设为该物品（只是标记），数量同 NEI 拖动的堆叠。 */
    @Override
    public boolean acceptNeiDrop(int mouseX, int mouseY, ItemStack stack, net.minecraftforge.fluids.FluidStack fluid) {
        int hovered = hoveredSlot(mouseX, mouseY);
        if (hovered < 0 || hovered >= MEItemInputBus.SLOTS || ItemStacks.isEmpty(stack)) {
            return false;
        }
        sendAction(MEGuiActionHandler.ACTION_CONFIG_DROP, hovered, 0, stack.writeToNBT(new net.minecraft.nbt.NBTTagCompound()));
        return true;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1F, 1F, 1F, 1F);
        mc.getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRendererObj.drawString(I18n.format("gui.meiteminputbus.title"), 8, 8, 0x404040);
        fontRendererObj.drawString(I18n.format("gui.mmce.me.config"), 8, 24, 0x404040);
        fontRendererObj.drawString(I18n.format("gui.mmce.me.stored_items"), 97, 24, 0x404040);
        fontRendererObj.drawString(I18n.format("container.inventory"), 8, ySize - 93, 0x404040);

        for (int i = 0; i < MEItemInputBus.SLOTS; i++) {
            ItemStack config = bus.getConfigInventory().getStackInSlot(i);
            drawStack(config, slotX(ContainerMEItemInputBus.CONFIG_X, i), slotY(i), ItemStacks.isEmpty(config) ? null : formatCount(config.stackSize));
            ItemStack stored = bus.getInternalInventory().getStackInSlot(i);
            drawStack(stored, slotX(ContainerMEItemInputBus.STORED_X, i), slotY(i), ItemStacks.isEmpty(stored) ? null : formatCount(stored.stackSize));
        }
        int hovered = hoveredSlot(mouseX, mouseY);
        if (hovered >= 0) {
            int slot = hovered % MEItemInputBus.SLOTS;
            int base = hovered < MEItemInputBus.SLOTS ? ContainerMEItemInputBus.CONFIG_X : ContainerMEItemInputBus.STORED_X;
            drawHighlight(slotX(base, slot), slotY(slot), 16, 16);
        }
    }

    @Override
    protected void renderHoveredToolTip(int mouseX, int mouseY) {
        int hovered = hoveredSlot(mouseX, mouseY);
        if (hovered < 0) {
            return;
        }
        List<String> extra = new ArrayList<>();
        ItemStack stack;
        if (hovered < MEItemInputBus.SLOTS) {
            stack = bus.getConfigInventory().getStackInSlot(hovered);
            if (ItemStacks.isEmpty(stack)) {
                return;
            }
            extra.add(EnumChatFormatting.GRAY + I18n.format("gui.meiteminputbus.items_marked", NumberFormat.getNumberInstance(Locale.US).format(stack.stackSize)));
            extra.addAll(actionInfo());
        } else {
            stack = bus.getInternalInventory().getStackInSlot(hovered - MEItemInputBus.SLOTS);
            if (ItemStacks.isEmpty(stack)) {
                return;
            }
            extra.add(EnumChatFormatting.GRAY + I18n.format("gui.meitembus.item_cached", NumberFormat.getNumberInstance(Locale.US).format(stack.stackSize)));
        }
        drawStackTooltip(stack, mouseX, mouseY, extra);
    }

    private static List<String> actionInfo() {
        List<String> tooltip = new ArrayList<>();
        tooltip.add(EnumChatFormatting.GRAY + I18n.format("gui.meiteminputbus.inv_action"));
        boolean shift = isShiftKeyDown();
        boolean ctrl = isCtrlKeyDown();
        if (shift && ctrl) {
            tooltip.add(EnumChatFormatting.GRAY + I18n.format("gui.meiteminputbus.inv_action.multiply", "SHIFT + CTRL"));
            tooltip.add(EnumChatFormatting.GRAY + I18n.format("gui.meiteminputbus.inv_action.divide", "SHIFT + CTRL"));
        } else if (shift || ctrl) {
            String key = ctrl ? "CTRL" : "SHIFT";
            int step = ctrl ? 100 : 10;
            tooltip.add(EnumChatFormatting.GRAY + I18n.format("gui.meiteminputbus.inv_action.increase", key, step));
            tooltip.add(EnumChatFormatting.GRAY + I18n.format("gui.meiteminputbus.inv_action.decrease", key, step));
        } else {
            tooltip.add(EnumChatFormatting.GRAY + I18n.format("gui.meiteminputbus.inv_action.increase.normal"));
            tooltip.add(EnumChatFormatting.GRAY + I18n.format("gui.meiteminputbus.inv_action.decrease.normal"));
        }
        return tooltip;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        int hovered = hoveredSlot(mouseX, mouseY);
        if (hovered >= 0 && hovered < MEItemInputBus.SLOTS && (button == 0 || button == 1)) {
            sendAction(MEGuiActionHandler.ACTION_CONFIG_CLICK, hovered, button);
            return;
        }
        super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) {
            return;
        }
        int mouseX = Mouse.getEventX() * width / mc.displayWidth;
        int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        int hovered = hoveredSlot(mouseX, mouseY);
        if (hovered < 0 || hovered >= MEItemInputBus.SLOTS) {
            return;
        }
        ItemStack config = bus.getConfigInventory().getStackInSlot(hovered);
        if (ItemStacks.isEmpty(config)) {
            return;
        }
        int amount = updatedCount(wheel > 0, config.stackSize);
        if (amount != config.stackSize) {
            sendAction(MEGuiActionHandler.ACTION_CONFIG_AMOUNT, hovered, amount);
        }
    }

    /** 同上游 GuiMEItemInputBus#getUpdatedCount。 */
    private static int updatedCount(boolean up, int current) {
        boolean shift = isShiftKeyDown();
        boolean ctrl = isCtrlKeyDown();
        if (shift && ctrl) {
            return up ? (current <= Integer.MAX_VALUE / 2 ? current * 2 : Integer.MAX_VALUE) : Math.max(1, current / 2);
        }
        int step = ctrl ? 100 : shift ? 10 : 1;
        return up ? (int) Math.min((long) current + step, Integer.MAX_VALUE) : Math.max(1, current - step);
    }
}
