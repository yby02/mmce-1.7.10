package ink.ikx.mmce.common.utils;

import java.util.Arrays;
import java.util.Objects;

public class MiscUtils {

    public static boolean areNull(Object... objs) {
        return Arrays.stream(objs).anyMatch(Objects::isNull);
    }

}
