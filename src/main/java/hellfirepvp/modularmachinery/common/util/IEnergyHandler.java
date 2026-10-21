/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.util;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: IEnergyHandler
 * Created by HellFirePvP
 * Date: 28.06.2017 / 12:26
 */
public interface IEnergyHandler {
    /**
     * 获取容器中剩余能量
     *
     * @return 剩余能量
     */
    long getCurrentEnergy();

    /**
     * 设置容器中的能量数值，非线程安全。
     *
     * @param energy 数值
     */
    void setCurrentEnergy(long energy);

    /**
     * 获取容器最大能量值
     *
     * @return 最大能量
     */
    long getMaxEnergy();

    /**
     * 获取容器剩余能量空间
     *
     * @return 最大可输入的能量
     */
    default long getRemainingCapacity() {
        return getMaxEnergy() - getCurrentEnergy();
    }

}
