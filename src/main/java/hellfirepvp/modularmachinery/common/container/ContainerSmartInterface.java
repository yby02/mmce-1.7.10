package hellfirepvp.modularmachinery.common.container;

import hellfirepvp.modularmachinery.common.tiles.TileSmartInterface;
import net.minecraft.entity.player.EntityPlayer;

public class ContainerSmartInterface extends ContainerBase<TileSmartInterface> {
    public ContainerSmartInterface(TileSmartInterface owner, EntityPlayer opening) {
        super(owner, opening);
    }
}
