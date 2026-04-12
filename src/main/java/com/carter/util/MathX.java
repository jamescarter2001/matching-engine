package com.carter.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MathX {

    /**
     * Binary bits already align with powers of two.
     * @param n
     * @return
     */
    public static boolean isPowerOfTwo(int n) {
        return (n & (n - 1)) == 0;
    }

}
