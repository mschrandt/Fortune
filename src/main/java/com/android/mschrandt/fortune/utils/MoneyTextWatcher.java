package com.android.mschrandt.fortune.utils;

import android.text.Editable;
import android.text.TextWatcher;

import java.text.DecimalFormat;
import java.util.Currency;
import java.util.Locale;

public class MoneyTextWatcher implements TextWatcher {

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {

        if(s.toString().equals(""))
        {
            return;
        }

        String oldString = s.toString();
        String newString = oldString;
        int multiplier = 1;

        // Strip commas and dollar sign characters
        if(newString.startsWith("-"))
        {
            multiplier = -1;
        }
        newString = newString.replaceAll("[^0-9]","");

        // Apply decimal format
        newString = DecimalFormat.getCurrencyInstance(Locale.getDefault()).format(multiplier * Double.parseDouble(newString+"0")/1000);
        newString = newString.replace(Currency.getInstance(Locale.getDefault()).getSymbol(), "");

        if(oldString.equals(newString))
        {
            return;
        }

        s.clear();
        s.append(newString);

    }
}
