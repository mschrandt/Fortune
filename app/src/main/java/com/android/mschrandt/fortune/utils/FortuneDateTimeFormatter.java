package com.android.mschrandt.fortune.utils;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;

public class FortuneDateTimeFormatter {
    private static DateTimeFormatter dateTimeFormatter;

    public static DateTimeFormatter get()
    {
        if(dateTimeFormatter == null)
        {
            dateTimeFormatter = DateTimeFormatter.ofPattern("MM/d/yy").withLocale(Locale.getDefault());
        }

        return dateTimeFormatter;
    }
}
