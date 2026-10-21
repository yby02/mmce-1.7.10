/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.util;

/**
 * <p>IEnergyHandlerAsync</p>
 * <p>顾名思义，它的能量输入与输出是线程安全的，使用全局锁实现。</p>
 */
public interface IEnergyHandlerAsync extends IEnergyHandler, Asyncable {
    /**
     * 从容器中提取能量，提取过程中应当是线程安全的。
     *
     * @param energy 提取数量
     * @return 提取成功返回 true，失败或容量不足以提取时返回 false
     */
    boolean extractEnergy(long energy);

    /**
     * 向容器输出能量，输出过程中应当是线程安全的。
     *
     * @param energy 输出数量
     * @return 输出成功返回 true，失败或剩余容量不足以输出时返回 false
     */
    boolean receiveEnergy(long energy);
}
