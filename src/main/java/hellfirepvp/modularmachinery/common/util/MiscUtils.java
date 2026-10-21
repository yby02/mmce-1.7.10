/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.util;

import com.google.common.collect.Lists;
import github.kasuminova.mmce.compat.EnumFacing;
import github.kasuminova.mmce.compat.BlockPos;
import org.lwjgl.input.Keyboard;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;


/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: MiscUtils
 * Created by HellFirePvP
 * Date: 30.03.2018 / 16:50
 */
public class MiscUtils {
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,###.##");

    public static <K, V, N> Map<K, N> remap(Map<K, V> map, Function<V, N> remapFct) {
        return map.entrySet()
                  .stream()
                  .collect(Collectors.toMap(Map.Entry::getKey, (e) -> remapFct.apply(e.getValue())));
    }

    public static List<String> splitStringBy(String str, String spl) {
        return Lists.newArrayList(str.split(spl));
    }

    public static BlockPos rotateYCCWNorthUntil(BlockPos at, EnumFacing dir) {
        EnumFacing currentFacing = EnumFacing.NORTH;
        BlockPos pos = at;
        while (currentFacing != dir) {
            currentFacing = currentFacing.rotateYCCW();
            pos = new BlockPos(pos.getZ(), pos.getY(), -pos.getX());
        }
        return pos;
    }

    public static BlockArray rotateYCCWNorthUntil(BlockArray array, EnumFacing dir) {
        EnumFacing currentFacing = EnumFacing.NORTH;
        BlockArray rot = array;
        while (currentFacing != dir) {
            currentFacing = currentFacing.rotateYCCW();
            rot = rot.rotateYCCW();
        }
        return rot;
    }

    public static BlockPos rotateYCCW(BlockPos pos) {
        return new BlockPos(pos.getZ(), pos.getY(), -pos.getX());
    }

    public static BlockPos rotateDown(BlockPos pos) {
        return new BlockPos(pos.getX(), pos.getZ(), -pos.getY());
    }

    public static BlockPos rotateUp(BlockPos pos) {
        return new BlockPos(pos.getX(), pos.getZ(), pos.getY());
    }

    public static <T> List<T> flatten(Collection<List<T>> collection) {
        List<T> list = new ArrayList<>(collection.size());
        for (List<T> ts : collection) {
            list.addAll(ts);
        }
        return list;
    }

    @Nullable
    public static <T> T iterativeSearch(Collection<T> collection, Predicate<T> matchingFct) {
        for (T element : collection) {
            if (matchingFct.test(element)) {
                return element;
            }
        }
        return null;
    }

    public static long clamp(long num, long min, long max) {
        if (num < min) {
            return min;
        } else {
            return Math.min(num, max);
        }
    }

    public static String formatNumber(long value) {
        if (value < 1_000L) {
            return String.valueOf(value);
        } else if (value < 1_000_000L) {
            return (double) Math.round((double) value) / 1000.0 + "K";
        } else if (value < 1_000_000_000L) {
            return (double) Math.round((double) (value / 1_000L)) / 1000.0 + "M";
        } else if (value < 1_000_000_000_000L) {
            return (double) Math.round((double) (value / 1_000_000L)) / 1000.0 + "G";
        } else if (value < 1_000_000_000_000_000L) {
            return (double) Math.round((double) (value / 1_000_000_000L)) / 1000.0 + "T";
        } else if (value < 1_000_000_000_000_000_000L) {
            return (double) Math.round((double) (value / 1_000_000_000_000L)) / 1000.0 + "P";
        } else {
            return (double) Math.round((double) (value / 1_000_000_000_000_000L)) / 1000.0 + "E";
        }
    }

    public static String formatNumberToInt(long value) {
        if (value < 1_000L) {
            return String.valueOf(value);
        } else if (value < 1_000_000L) {
            return value / 1_000 + "K";
        } else if (value < 1_000_000_000L) {
            return value / 1_000_000 + "M";
        } else if (value < 1_000_000_000_000L) {
            return value / 1_000_000_000L + "G";
        } else if (value < 1_000_000_000_000_000L) {
            return value / 1_000_000_000_000L + "T";
        } else if (value < 1_000_000_000_000_000_000L) {
            return value / 1_000_000_000_000_000L + "P";
        } else {
            return value / 1_000_000_000_000_000_000L + "E";
        }
    }

    public static String formatDecimal(double value) {
        return DECIMAL_FORMAT.format(value);
    }

    public static String formatFloat(float value, int decimalFraction) {
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits(decimalFraction);
        return nf.format(value);
    }

    public static boolean isTextBoxKey(int i) {
        return i == Keyboard.KEY_BACK || i == Keyboard.KEY_DELETE || i == Keyboard.KEY_LEFT || i == Keyboard.KEY_RIGHT || i == Keyboard.KEY_END ||
            i == Keyboard.KEY_HOME || isCtrlCombo(i, Keyboard.KEY_A) || isCtrlCombo(i, Keyboard.KEY_C) || isCtrlCombo(i, Keyboard.KEY_V) || isCtrlCombo(i, Keyboard.KEY_X);
    }

    /** 1.7.10 移植：对应 1.12.2 GuiScreen.isKeyComboCtrlA/C/V/X（仅客户端调用）。 */
    private static boolean isCtrlCombo(int keyCode, int expected) {
        return keyCode == expected && net.minecraft.client.gui.GuiScreen.isCtrlKeyDown() && !net.minecraft.client.gui.GuiScreen.isShiftKeyDown();
    }

    public static String posToString(BlockPos pos) {
        return String.format("X:%s Y:%s Z:%s", pos.getX(), pos.getY(), pos.getZ());
    }

}
