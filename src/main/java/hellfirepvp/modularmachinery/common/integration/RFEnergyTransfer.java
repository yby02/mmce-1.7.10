package hellfirepvp.modularmachinery.common.integration;

import cofh.api.energy.IEnergyReceiver;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * 1.7.10 移植：RF（CoFH RF API）能量推送。所有对 cofh.api.energy 的直接引用集中在此类，
 * 仅当 RF API 存在（Mods.REDSTONEFLUXAPI）时才会被调用和加载。
 */
public final class RFEnergyTransfer {

    private RFEnergyTransfer() {
    }

    /**
     * 向 te 推送最多 maxTransfer RF。
     *
     * @param side 从接收方看来能量进入的面（即发送方方向的反方向）
     * @return 实际被接收的 RF
     */
    public static int push(TileEntity te, ForgeDirection side, int maxTransfer) {
        if (maxTransfer <= 0 || !(te instanceof IEnergyReceiver receiver)) {
            return 0;
        }
        try {
            if (!receiver.canConnectEnergy(side)) {
                return 0;
            }
            return Math.max(0, receiver.receiveEnergy(side, maxTransfer, false));
        } catch (Exception ignored) {
            return 0;
        }
    }
}
