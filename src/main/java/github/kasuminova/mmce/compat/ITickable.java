package github.kasuminova.mmce.compat;

/**
 * 1.7.10 移植适配：1.12.2 的 ITickable。1.7.10 由 TileEntity#updateEntity 驱动，
 * {@link hellfirepvp.modularmachinery.common.tiles.base.TileEntitySynchronized} 会把 updateEntity 转发到 {@link #update()}，
 * 并只让实现了本接口的 TileEntity 参与 tick（canUpdate）。
 */
public interface ITickable {
    void update();
}
