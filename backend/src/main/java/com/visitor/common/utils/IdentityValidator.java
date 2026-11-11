package com.visitor.common.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 手机号、身份证号基础校验。
 */
public final class IdentityValidator {

    private static final int[] ID_WEIGHTS = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
    private static final char[] ID_CHECK_CODES = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};

    private IdentityValidator() {
    }

    public static boolean isPhone(String value) {
        return value != null && value.trim().matches("^1[3-9]\\d{9}$");
    }

    public static boolean isIdCard(String value) {
        if (value == null) {
            return false;
        }
        String id = value.trim().toUpperCase();
        if (!id.matches("^\\d{17}[\\dX]$")) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += (id.charAt(i) - '0') * ID_WEIGHTS[i];
        }
        if (ID_CHECK_CODES[sum % 11] != id.charAt(17)) {
            return false;
        }
        try {
            LocalDate birth = LocalDate.parse(id.substring(6, 14), DateTimeFormatter.BASIC_ISO_DATE);
            return !birth.isAfter(LocalDate.now());
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
