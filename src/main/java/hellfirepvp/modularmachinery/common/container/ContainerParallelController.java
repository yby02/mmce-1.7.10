package hellfirepvp.modularmachinery.common.container;

import hellfirepvp.modularmachinery.common.tiles.TileParallelController;
import net.minecraft.entity.player.EntityPlayer;

public class ContainerParallelController extends ContainerBase<TileParallelController> {
    public ContainerParallelController(TileParallelController owner, EntityPlayer opening) {
        super(owner, opening);
    }
}
