package com.ruuvi.tag.util;

/**
 * Created by admin on 09/09/2017.
 */

public class Utils {
    public static final java.lang.String DB_TIME_FORMAT = "dd.MM.yyyy HH:mm:ss";

    public static boolean tryParse(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}