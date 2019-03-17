package com.android.mschrandt.fortune.activities;

import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;

import com.android.mschrandt.fortune.model.AccountManager;
import com.android.mschrandt.fortune.R;

import java.time.LocalDate;
import java.util.Calendar;

public class ForecastDateActivity extends AppCompatActivity {

    // Extras
    public static final String EXTRA_FORECAST_DATE = "EXTRA_FORECAST_DATE";

    // Layout
    private DatePicker mDatePickerForecastDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forecast_date);
        setTitle("Set Forecast Date");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        AccountManager am = AccountManager.getInstance();
        mDatePickerForecastDate = findViewById(R.id.forecast_date);
        mDatePickerForecastDate.updateDate(am.getForecastDate().getYear(),
                                            am.getForecastDate().getMonthValue()-1,
                                            am.getForecastDate().getDayOfMonth());

        Calendar c = Calendar.getInstance();
        c.set(LocalDate.now().getYear()+1, LocalDate.now().getMonthValue()-1, LocalDate.now().getDayOfMonth());
        mDatePickerForecastDate.setMinDate(c.getTimeInMillis());

        // Button handlers
        FloatingActionButton fabAdd = findViewById(R.id.update_forecast_add);
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    Intent intentAddAcc = new Intent();
                    intentAddAcc.putExtra(EXTRA_FORECAST_DATE, LocalDate.of(mDatePickerForecastDate.getYear(), mDatePickerForecastDate.getMonth()+1, mDatePickerForecastDate.getDayOfMonth()));
                    setResult(RESULT_OK, intentAddAcc);
                    finish();

            }
        });

        FloatingActionButton fabCancel = findViewById(R.id.update_forecast_cancel);
        fabCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId())
        {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
