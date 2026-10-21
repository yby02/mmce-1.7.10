/*******************************************************************************
 * HellFirePvP / Modular Machinery 2018
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.client.util;

import github.kasuminova.mmce.compat.MathHelper;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.config.Configuration;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: EnergyDisplayUtil
 * Created by HellFirePvP
 * Date: 04.03.2019 / 20:07
 * <p>
 * 1.7.10 移植：1.7.10 没有 FE，默认显示单位改为 RF（FE 选项保留，与 RF 等价）。
 */
public class EnergyDisplayUtil {

    public static boolean displayFETooltip    = true;
    public static boolean displayIC2EUTooltip = true;
    public static boolean displayGTEUTooltip  = true;

    //Available: FE, RF, IC2_EU, GT_EU
    public static EnergyType type = EnergyType.RF;

    public static void loadFromConfig(Configuration cfg) {
        displayFETooltip = cfg.getBoolean("FE_RF_Tooltip", "display.energy", displayFETooltip, "Set to true, if the standard 'energy' FE (or RF) should be displayed in the tooltip of the energy hatch along with its transmission rates.");
        displayIC2EUTooltip = cfg.getBoolean("IC2_EU_Tooltip", "display.energy", displayIC2EUTooltip, "Set to true, if IC2's energy EU should be displayed in the tooltip of the energy hatch. Will only have effect if IC2 is installed.");
        displayGTEUTooltip = cfg.getBoolean("GT_EU_Tooltip", "display.energy", displayGTEUTooltip, "Set to true, if GT's energy EU should be displayed in the tooltip of the energy hatch. Will only have effect if GregTech (community edition) is installed.");

        type = EnergyType.getType(cfg.getString("Display_Energy_Type", "display.energy", type.name(), "Available options: 'FE', 'RF', 'IC2_EU', 'GT_EU' - Default: RF - Set this to one of those 4 types to have GUI, recipe preview and energy be displayed in that type of energy in ALL ModularMachinery things."));
    }

    /**
     * 能量数值（energy 为内部的 RF 数，按 {@link #type} 换算）：平时缩写为 K / M / G…，按住 Shift 时显示完整数值（千分位）。
     * 只在客户端绘制提示时调用（读取键盘状态）。
     */
    public static String formatEnergy(long energy) {
        return formatEnergy(energy, isShiftDown());
    }

    /** exact 为 true 时显示完整数值（千分位），否则缩写。 */
    public static String formatEnergy(long energy, boolean exact) {
        long value = type.formatEnergyForDisplay(energy);
        return exact ? NumberFormat.getNumberInstance(Locale.US).format(value) : MiscUtils.formatNumber(value);
    }

    /**
     * 能量仓界面中储能条的提示：“当前 / 上限 单位”，数值被缩写时再加一行“按住 Shift 显示完整数值”。
     * exact 由调用方传入当前是否按住 Shift（界面中为 {@link #isShiftDown()}）。
     */
    public static java.util.List<String> chargeTooltip(long current, long max, boolean exact) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        lines.add(StatCollector.translateToLocalFormatted("tooltip.energyhatch.charge",
            formatEnergy(current, exact), formatEnergy(max, exact), unit()));
        if (!exact && (type.formatEnergyForDisplay(current) >= 1000L || type.formatEnergyForDisplay(max) >= 1000L)) {
            lines.add(shiftHint());
        }
        return lines;
    }

    /** 当前显示单位（RF / FE / EU）。 */
    public static String unit() {
        return StatCollector.translateToLocal(type.getUnlocalizedFormat());
    }

    /** 数值被缩写（未按 Shift 且不小于 1000）时，提示按 Shift 查看完整数值。 */
    public static boolean needsShiftHint(long... energies) {
        if (isShiftDown()) {
            return false;
        }
        for (long e : energies) {
            if (type.formatEnergyForDisplay(e) >= 1000L) {
                return true;
            }
        }
        return false;
    }

    public static String shiftHint() {
        return EnumChatFormatting.DARK_GRAY + StatCollector.translateToLocal("tooltip.energyhatch.shift");
    }

    public static boolean isShiftDown() {
        return net.minecraft.client.gui.GuiScreen.isShiftKeyDown();
    }

    public enum EnergyType {

        FE(1),
        RF(1),
        IC2_EU(0.25F),
        GT_EU(0.25F);

        private final double multiplier;
        private final String unlocalizedFormat;

        EnergyType(double multiplier) {
            this.multiplier = multiplier;
            this.unlocalizedFormat = "tooltip.energy.type." + name().toLowerCase();
        }

        public static EnergyType getType(String configValue) {
            EnergyType type = RF;
            try {
                type = EnergyType.valueOf(configValue);
            } catch (Exception ignored) {
            }

            return type;
        }

        public long formatEnergyForDisplay(long energy) {
            return MathHelper.lfloor(energy * multiplier);
        }

        public String getUnlocalizedFormat() {
            return unlocalizedFormat;
        }

    }

}
