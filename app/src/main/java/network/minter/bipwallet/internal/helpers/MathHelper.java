package network.minter.bipwallet.internal.helpers;

/**
 * Dogsy. 2017
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */

public final class MathHelper {

    public static float clamp(float input, float min, float max) {
        if (input < min) {
            return min;
        } else if (input > max) {
            return max;
        }

        return input;
    }

    public static int clamp(int input, int min) {
        if (input < min) {
            return min;
        }

        return input;
    }

    public static long clamp(long input, long min) {
        if (input < min) {
            return min;
        }

        return input;
    }

    public static int clamp(int input, int min, int max) {
        if (input < min) {
            return min;
        } else if (input > max) {
            return max;
        }

        return input;
    }

    public static long clamp(long input, long min, long max) {
        if (input < min) {
            return min;
        } else if (input > max) {
            return max;
        }

        return input;
    }
}
