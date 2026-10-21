package kport.modularmagic.client.gui;

import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.client.gui.GuiContainerBase;
import kport.modularmagic.common.container.ContainerLifeEssence;
import kport.modularmagic.common.tile.TileLifeEssenceProvider;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/** 生命精华仓界面（背景同上游，inventory_tiny.png），另外显示 LP 缓存与宝珠主人。 */
public class GuiContainerLifeEssence extends GuiContainerBase<ContainerLifeEssence> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(ModularMachinery.MODID, "textures/gui/inventory_tiny.png");

    private final TileLifeEssenceProvider tile;

    public GuiContainerLifeEssence(TileLifeEssenceProvider tile, EntityPlayer opening) {
        super(new ContainerLifeEssence(tile, opening));
        this.tile = tile;
    }

    @Override
    protected void setWidthHeight() {
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String owner = tile.getOwner();
        fontRendererObj.drawString(I18n.format("gui.modularmachinery.magic.lifeessence.cache", tile.getLifeEssenceCache(), tile.getCacheCapacity()), 8, 52, 0x404040);
        fontRendererObj.drawString(owner == null ? I18n.format("error.modularmachinery.requirement.lifeessence.orb") : I18n.format("gui.modularmachinery.magic.lifeessence.owner", owner), 8, 62, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1F, 1F, 1F, 1F);
        mc.getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect((width - xSize) / 2, (height - ySize) / 2, 0, 0, xSize, ySize);
    }
}
