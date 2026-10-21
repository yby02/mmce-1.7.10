/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.client.gui;

import com.google.common.collect.Lists;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.client.util.MekanismGasRender;
import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.container.ContainerFluidHatch;
import hellfirepvp.modularmachinery.common.network.PktInteractFluidTankGui;
import hellfirepvp.modularmachinery.common.tiles.base.TileFluidTank;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import github.kasuminova.mmce.compat.client.GlStateManager;
import github.kasuminova.mmce.compat.ItemStacks;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import github.kasuminova.mmce.compat.MathHelper;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: GuiContainerFluidHatch
 * Created by HellFirePvP
 * Date: 09.07.2017 / 11:27
 */
public class GuiContainerFluidHatch extends GuiContainerBase<ContainerFluidHatch> {

    public static final ResourceLocation TEXTURES_FLUID_HATCH = new ResourceLocation(ModularMachinery.MODID, "textures/gui/guibar.png");

    private final TileFluidTank tank;

    public GuiContainerFluidHatch(TileFluidTank tileFluidTank, EntityPlayer opening) {
        super(new ContainerFluidHatch(tileFluidTank, opening));
        this.tank = tileFluidTank;
    }

    @Override
    protected void setWidthHeight() {
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        int offsetX = (this.width - this.xSize) / 2;
        int offsetZ = (this.height - this.ySize) / 2;
        if (mouseButton == 0 &&
            mouseX >= 15 + offsetX && mouseX <= 35 + offsetX &&
            mouseY >= 10 + offsetZ && mouseY <= 71 + offsetZ) {

            ItemStack held = this.mc.thePlayer.inventory.getItemStack();
            if (!ItemStacks.isEmpty(held)) {
                ModularMachinery.NET_CHANNEL.sendToServer(new PktInteractFluidTankGui());
            }
        }
    }

    @Override
    protected void renderHoveredToolTip(int x, int z) {
        super.renderHoveredToolTip(x, z);

        int offsetX = (this.width - this.xSize) / 2;
        int offsetZ = (this.height - this.ySize) / 2;

        if (x >= 15 + offsetX && x <= 35 + offsetX && z >= 10 + offsetZ && z <= 71 + offsetZ) {
            List<String> text = Lists.newArrayList();

            FluidStack content = tank.getTank().getFluid();
            if (content != null && content.amount > 0) {
                // 装有 Mekanism 时流体仓也可装气体，因此标明内容类型（与上游相同）
                if (Mods.MEKANISM.isPresent()) {
                    text.add(I18n.format("tooltip.fluidhatch.fluid"));
                }
                text.add(content.getLocalizedName());
                text.add(I18n.format("tooltip.fluidhatch.tank", String.valueOf(content.amount), String.valueOf(tank.getTank().getCapacity())));
            } else if (!Mods.MEKANISM.isPresent() || !MekanismGasRender.addTankTooltip(tank.getTank(), text)) {
                text.add(I18n.format("tooltip.fluidhatch.empty"));
                text.add(I18n.format("tooltip.fluidhatch.tank", "0", String.valueOf(tank.getTank().getCapacity())));
            }

            FontRenderer font = Minecraft.getMinecraft().fontRenderer;
            drawHoveringText(text, x, z, (font == null ? fontRendererObj : font));
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        FluidStack content = tank.getTank().getFluid();
        if (content != null && content.amount > 0) {
            int fluidColor = content.getFluid().getColor(content);
            float red = (fluidColor >> 16 & 0xFF) / 255F;
            float green = (fluidColor >> 8 & 0xFF) / 255F;
            float blue = (fluidColor & 0xFF) / 255F;

            float percFilled = ((float) content.amount) / ((float) tank.getTank().getCapacity());
            percFilled = MathHelper.clamp(percFilled, 0F, 1F);
            int pxFilled = MathHelper.ceil(percFilled * 61F);
            GlStateManager.color(red, green, blue, 1.0F);
            // 1.7.10 移植：流体贴图是方块图集中的 IIcon
            IIcon icon = content.getFluid().getIcon(content);
            this.mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
            if (icon == null) {
                icon = ((TextureMap) this.mc.getTextureManager().getTexture(TextureMap.locationBlocksTexture)).getAtlasSprite("missingno");
            }
            drawTexturedModelRectFromIcon(15, 10 + 61 - pxFilled, icon, 20, pxFilled);
        } else if (Mods.MEKANISM.isPresent()) {
            MekanismGasRender.drawTankGas(this, tank.getTank(), 15, 10, 20, 61);
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURES_FLUID_HATCH);
        this.drawTexturedModalRect(15, 10, 176, 0, 20, 61);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURES_FLUID_HATCH);
        int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(i, j, 0, 0, this.xSize, this.ySize);
    }

}
