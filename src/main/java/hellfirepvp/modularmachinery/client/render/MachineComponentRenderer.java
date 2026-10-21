package hellfirepvp.modularmachinery.client.render;

import com.gtnewhorizons.angelica.api.ThreadSafeISBRH;
import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;
import hellfirepvp.modularmachinery.common.block.BlockMachineComponent;
import hellfirepvp.modularmachinery.common.block.render.MachineBlockRenderType;
import hellfirepvp.modularmachinery.common.dev.DevMode;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import org.lwjgl.opengl.GL11;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 1.7.10 移植：替代 1.12.2 的双层方块模型（blockmodel_overlay*.json）。
 * <ul>
 *   <li>世界中：第一遍按普通方块渲染（底层贴图，乘机器颜色）；第二遍进入覆盖层状态再渲染一次，
 *       方块此时返回覆盖层贴图、白色，并跳过没有覆盖层的面。两遍都走原版 renderStandardBlock，环境光遮蔽与亮度与原版方块一致。</li>
 *   <li>物品栏中：逐面绘制底层（乘物品颜色）与覆盖层。</li>
 * </ul>
 * Angelica：本类没有可变状态（覆盖层状态是 {@link MachineBlockRenderType} 中的线程局部变量，Tessellator 只在方法内取用），
 * 方块侧的取色、取贴图只读方块、元数据与方块实体字段，因此标记为线程安全，允许 Angelica 在区块构建线程上直接调用同一实例。
 * 未安装 Angelica 时该注解类不存在，JVM 会忽略它。
 */
@ThreadSafeISBRH(perThread = false)
public class MachineComponentRenderer implements ISimpleBlockRenderingHandler {

    /** 仅开发 / 测试环境：记录调用过 {@link #renderWorldBlock} 的线程名，供自动场景核对 Angelica 是否在区块构建线程上调用。 */
    private static final boolean TRACK_THREADS = DevMode.isEnabled();
    public static final Set<String> WORLD_RENDER_THREADS = ConcurrentHashMap.newKeySet();

    private final int renderId;

    public MachineComponentRenderer(int renderId) {
        this.renderId = renderId;
    }

    public static void register() {
        int id = RenderingRegistry.getNextAvailableRenderId();
        MachineBlockRenderType.renderId = id;
        RenderingRegistry.registerBlockHandler(new MachineComponentRenderer(id));
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {
        if (TRACK_THREADS) {
            WORLD_RENDER_THREADS.add(Thread.currentThread().getName());
        }
        boolean rendered = renderer.renderStandardBlock(block, x, y, z);
        if (renderer.hasOverrideBlockTexture()) {
            // 破坏进度等覆盖贴图时只画一遍
            return rendered;
        }
        MachineBlockRenderType.beginOverlayPass();
        try {
            rendered |= renderer.renderStandardBlock(block, x, y, z);
        } finally {
            MachineBlockRenderType.endOverlayPass();
        }
        return rendered;
    }

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
        int color = block.getRenderColor(metadata);
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;

        block.setBlockBoundsForItemRender();
        renderer.setRenderBoundsFromBlock(block);
        GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        boolean alphaTest = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        for (int side = 0; side < 6; side++) {
            drawFace(block, renderer, side, block.getIcon(side, metadata), r, g, b);
            if (block instanceof BlockMachineComponent) {
                IIcon overlay = ((BlockMachineComponent) block).getOverlayIcon(side, metadata);
                if (overlay != null) {
                    drawFace(block, renderer, side, overlay, 1.0F, 1.0F, 1.0F);
                }
            }
        }
        if (!alphaTest) {
            GL11.glDisable(GL11.GL_ALPHA_TEST);
        }
        GL11.glTranslatef(0.5F, 0.5F, 0.5F);
    }

    private static void drawFace(Block block, RenderBlocks renderer, int side, IIcon icon, float r, float g, float b) {
        if (icon == null) {
            return;
        }
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        // 各面明暗由物品渲染时的 GL 光照（法线）负责，与原版 renderBlockAsItem 相同
        tessellator.setColorOpaque_F(r, g, b);
        switch (side) {
            case 0:
                tessellator.setNormal(0.0F, -1.0F, 0.0F);
                renderer.renderFaceYNeg(block, 0.0D, 0.0D, 0.0D, icon);
                break;
            case 1:
                tessellator.setNormal(0.0F, 1.0F, 0.0F);
                renderer.renderFaceYPos(block, 0.0D, 0.0D, 0.0D, icon);
                break;
            case 2:
                tessellator.setNormal(0.0F, 0.0F, -1.0F);
                renderer.renderFaceZNeg(block, 0.0D, 0.0D, 0.0D, icon);
                break;
            case 3:
                tessellator.setNormal(0.0F, 0.0F, 1.0F);
                renderer.renderFaceZPos(block, 0.0D, 0.0D, 0.0D, icon);
                break;
            case 4:
                tessellator.setNormal(-1.0F, 0.0F, 0.0F);
                renderer.renderFaceXNeg(block, 0.0D, 0.0D, 0.0D, icon);
                break;
            default:
                tessellator.setNormal(1.0F, 0.0F, 0.0F);
                renderer.renderFaceXPos(block, 0.0D, 0.0D, 0.0D, icon);
                break;
        }
        tessellator.draw();
    }

    @Override
    public boolean shouldRender3DInInventory(int modelId) {
        return true;
    }

    @Override
    public int getRenderId() {
        return renderId;
    }
}
