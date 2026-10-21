/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.util.nbt;

import net.minecraft.nbt.NBTBase.NBTPrimitive;

import javax.annotation.Nullable;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: NBTComparableNumber
 * Created by HellFirePvP
 * Date: 19.08.2017 / 21:18
 */
public interface NBTComparableNumber {

    boolean test(NBTPrimitive numberTag);

    enum ComparisonMode {
        LESS_EQUAL("<="),
        EQUAL("=="),
        GREATER_EQUAL(">="),

        LESS("<"),
        GREATER(">");

        private final String identifier;

        ComparisonMode(String identifier) {
            this.identifier = identifier;
        }

        @Nullable
        public static ComparisonMode peekMode(String strModeAndValue) {
            lblModes:
            for (ComparisonMode mode : values()) {
                char[] charArray = mode.identifier.toCharArray();
                for (int i = 0; i < charArray.length; i++) {
                    char c = charArray[i];
                    if (strModeAndValue.charAt(i) != c) {
                        continue lblModes;
                    }
                }
                return mode;
            }
            return null;
        }

        public String getIdentifier() {
            return identifier;
        }

        public boolean testByte(byte original, byte toTest) {
            return switch (this) {
                case LESS -> toTest < original;
                case LESS_EQUAL -> toTest <= original;
                case EQUAL -> toTest == original;
                case GREATER_EQUAL -> toTest >= original;
                case GREATER -> toTest > original;
            };
        }

        public boolean testInt(int original, int toTest) {
            return switch (this) {
                case LESS -> toTest < original;
                case LESS_EQUAL -> toTest <= original;
                case EQUAL -> toTest == original;
                case GREATER_EQUAL -> toTest >= original;
                case GREATER -> toTest > original;
            };
        }

        public boolean testShort(short original, short toTest) {
            return switch (this) {
                case LESS -> toTest < original;
                case LESS_EQUAL -> toTest <= original;
                case EQUAL -> toTest == original;
                case GREATER_EQUAL -> toTest >= original;
                case GREATER -> toTest > original;
            };
        }

        public boolean testLong(long original, long toTest) {
            return switch (this) {
                case LESS -> toTest < original;
                case LESS_EQUAL -> toTest <= original;
                case EQUAL -> toTest == original;
                case GREATER_EQUAL -> toTest >= original;
                case GREATER -> toTest > original;
            };
        }

        public boolean testFloat(float original, float toTest) {
            return switch (this) {
                case LESS -> toTest < original;
                case LESS_EQUAL -> toTest <= original;
                case EQUAL -> toTest == original;
                case GREATER_EQUAL -> toTest >= original;
                case GREATER -> toTest > original;
            };
        }

        public boolean testDouble(double original, double toTest) {
            return switch (this) {
                case LESS -> toTest < original;
                case LESS_EQUAL -> toTest <= original;
                case EQUAL -> toTest == original;
                case GREATER_EQUAL -> toTest >= original;
                case GREATER -> toTest > original;
            };
        }

    }

}
