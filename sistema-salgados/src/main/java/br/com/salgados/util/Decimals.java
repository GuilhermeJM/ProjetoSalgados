package br.com.salgados.util;

import java.math.BigDecimal;

public final class Decimals {

    private Decimals() {
    }

    /** toPlainString evita notação científica (ex.: "1E+3") no banco. */
    public static String toDb(BigDecimal value) {
        return value.toPlainString();
    }

    public static BigDecimal fromDb(String value) {
        return new BigDecimal(value);
    }
}