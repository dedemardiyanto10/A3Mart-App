package com.a3mart.app.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class FormatterUtils {

    private static final DecimalFormatSymbols INDONESIA_SYMBOLS;

    static {
        INDONESIA_SYMBOLS = new DecimalFormatSymbols(new Locale("in", "ID"));
        INDONESIA_SYMBOLS.setCurrencySymbol("Rp");
        INDONESIA_SYMBOLS.setGroupingSeparator('.');
        INDONESIA_SYMBOLS.setMonetaryDecimalSeparator(',');
    }

    public static DecimalFormat getRupiahFormatter() {
        return new DecimalFormat("Rp#,##0.00;-Rp#,##0.00", INDONESIA_SYMBOLS);
    }

    public static DecimalFormat getRupiahNoDecimal() {
        return new DecimalFormat("Rp#,##0;-Rp#,##0", INDONESIA_SYMBOLS);
    }

    public static String formatRupiah(long nominal) {
        return getRupiahFormatter().format(nominal);
    }
}
