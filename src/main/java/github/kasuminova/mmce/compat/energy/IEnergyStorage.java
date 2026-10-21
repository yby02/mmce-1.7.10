package github.kasuminova.mmce.compat.energy;

/**
 * 1.7.10 移植适配：与 Forge 1.12.2 net.minecraftforge.energy.IEnergyStorage（FE）相同的接口形状。
 * 1.7.10 没有 Forge Energy；MMCE 内部以 RF 为单位，对外由能量仓实现 CoFH RF API / IC2 EU API（软依赖）。
 */
public interface IEnergyStorage {

    int receiveEnergy(int maxReceive, boolean simulate);

    int extractEnergy(int maxExtract, boolean simulate);

    int getEnergyStored();

    int getMaxEnergyStored();

    boolean canExtract();

    boolean canReceive();
}
