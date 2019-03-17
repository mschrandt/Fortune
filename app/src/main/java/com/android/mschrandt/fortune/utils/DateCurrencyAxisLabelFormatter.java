package com.android.mschrandt.fortune.utils;

import android.content.Context;

import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;

public class DateCurrencyAxisLabelFormatter extends DateAsXAxisLabelFormatter {

    public DateCurrencyAxisLabelFormatter(Context context)
    {
        super(context);
    }

    @Override
    public String formatLabel(double value, boolean isValueX) {
        if (isValueX) {
            // show normal x values
            return super.formatLabel(value, isValueX);
        } else {
            // show currency for y values
            String suffix = "";

            if(Math.abs(value) >= 1000000000)
            {
                value /= 1000000000;
                suffix = "B";
            }
            else if(Math.abs(value) >= 1000000)
            {
                value /= 1000000;
                suffix = "M";
            }
            else if(Math.abs(value) >= 1000)
            {
                value /= 1000;
                suffix = "K";
            }

            String label = String.format("%.2f",value)+suffix;
            if(label.length() == 6)
            {
                label = String.format("%.1f",value)+suffix;
            }
            else if(label.length() > 6)
            {
                label = String.format("%.0f",value)+suffix;
            }

            return "$"+label;
        }
    }
}
