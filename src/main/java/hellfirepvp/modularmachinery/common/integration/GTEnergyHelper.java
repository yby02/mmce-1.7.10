package hellfirepvp.modularmachinery.common.integration;

import gregtech.api.interfaces.tileentity.IEnergyConnected;
import hellfirepvp.modularmachinery.common.block.prop.EnergyHatchData;
import hellfirepvp.modularmachinery.common.tiles.base.TileEnergyHatch;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * GregTech 5（GTNH GT5-Unofficial）的 EU 直连：能量仓实现 GT 的 IEnergyConnected，GT 导线 / 机器直接按“电压 × 电流”收发 EU。
 * 1 EU = 4 RF（与 IC2 相同的换算）；电压上限与电流取能量仓等级的 GT 设定（EnergyHatchData 的 gtvoltage / gtamperage）。
 * <p>
 * 1.7.10 移植：对应上游的 GTEnergyContainer（GTCEu 的 IEnergyContainer）。超压时与上游一样爆炸（配置项 enable-GT-Explodes）。
 * 只在装有 GT5 时调用。
 */
public final class GTEnergyHelper {

    private GTEnergyHelper() {
    }

    /** GT 电压等级：ULV = 0（8 EU/t）、LV = 1（32）……超过最高等级时返回最高等级。 */
    public static int tierOf(long voltage) {
        int tier = 0;
        long v = 8;
        while (voltage > v && tier < EnergyHatchData.GT_VOLTAGE_NAMES.length - 1) {
            v *= 4;
            tier++;
        }
        return tier;
    }

    /** 输入仓接收 GT 能量，返回实际使用的电流。 */
    public static long accept(TileEnergyHatch hatch, long voltage, long amperage) {
        if (voltage <= 0 || amperage <= 0) {
            return 0;
        }
        EnergyHatchData size = hatch.getTier();
        long maxAmperage = Math.min(size.getGtAmperage(), amperage);
        if (voltage > size.getGTEnergyTransferVoltage()) {
            if (EnergyHatchData.enableGTExplodes) {
                hatch.getWorld().setBlockToAir(hatch.xCoord, hatch.yCoord, hatch.zCoord);
                hatch.getWorld().createExplosion(null, hatch.xCoord + 0.5, hatch.yCoord + 0.5, hatch.zCoord + 0.5, tierOf(voltage), true);
            }
            return maxAmperage;
        }
        long spaceEU = (hatch.getMaxEnergy() - hatch.getCurrentEnergy()) / 4L;
        long accepting = Math.min(spaceEU / voltage, maxAmperage);
        if (accepting <= 0) {
            return 0;
        }
        hatch.setCurrentEnergy(hatch.getCurrentEnergy() + accepting * voltage * 4L);
        hatch.markNoUpdateSync();
        return accepting;
    }

    // GT 导线的节点图（BaseMetaPipeEntity#getNode / GenerateNodeMapPower）。这两个类的父类型牵涉 ModularUI 等，编译期不引用，运行时反射一次后缓存。
    private static Class<?>                         pipeClass;
    private static java.lang.reflect.Method         getNode;
    private static java.lang.reflect.Constructor<?> generateNodeMap;
    private static boolean                          nodeMapUnavailable;

    private static void ensurePowerNodes(TileEntity te) {
        if (nodeMapUnavailable) {
            return;
        }
        try {
            if (pipeClass == null) {
                pipeClass = Class.forName("gregtech.api.metatileentity.BaseMetaPipeEntity");
                getNode = pipeClass.getMethod("getNode");
                generateNodeMap = Class.forName("gregtech.api.graphs.GenerateNodeMapPower").getConstructor(pipeClass);
            }
            if (pipeClass.isInstance(te) && getNode.invoke(te) == null) {
                generateNodeMap.newInstance(te);
            }
        } catch (ReflectiveOperationException | LinkageError e) {
            nodeMapUnavailable = true;
            hellfirepvp.modularmachinery.ModularMachinery.log.warn("[GT] cannot build GregTech cable node maps, energy output hatches will only feed GT machines directly", e);
        }
    }

    /** 输出仓向相邻的 GT 能量设备（导线、机器）输出，按本仓的 GT 电压与电流；返回送出的 RF 数。 */
    public static long emit(TileEnergyHatch hatch, long maxRF) {
        EnergyHatchData size = hatch.getTier();
        long voltage = size.getGTEnergyTransferVoltage();
        if (voltage <= 0) {
            return 0;
        }
        long amps = Math.min(size.getGtAmperage(), maxRF / 4L / voltage);
        long usedRF = 0;
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            if (amps <= 0) {
                break;
            }
            TileEntity te = hatch.getWorld().getTileEntity(hatch.xCoord + side.offsetX, hatch.yCoord + side.offsetY, hatch.zCoord + side.offsetZ);
            if (!(te instanceof IEnergyConnected target) || te instanceof TileEnergyHatch) {
                continue;
            }
            // GT 导线按“节点图”分配能量，节点图由向导线输出的一方生成（GT 机器在 BaseMetaTileEntity#generatePowerNodes 中做同样的事）；
            // 导线布局改变时 GT 会清掉节点图，这里在下次输出前重新生成
            ensurePowerNodes(te);
            // 与 GT 自己的 IEnergyConnected.Util.emitEnergyToNetwork 相同：直接注入，由接收方在 injectEnergyUnits 中判断是否接受
            // （GT 导线的 inputEnergyFrom 为 false，但会在 injectEnergyUnits 中把能量送进导线网络）
            ForgeDirection from = side.getOpposite();
            long used = target.injectEnergyUnits(from, voltage, amps);
            if (used > 0) {
                amps -= used;
                usedRF += used * voltage * 4L;
            }
        }
        return usedRF;
    }
}
