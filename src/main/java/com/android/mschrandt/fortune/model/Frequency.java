package com.android.mschrandt.fortune.model;

import java.time.Period;
import java.util.ArrayList;

public class Frequency {

    // Member variables
    private static ArrayList<Frequency> frequencyList;

    private Period period;
    private String periodLabel;

    // Constructor
    public Frequency(Period period,
              String periodLabel)
    {
        this.period = period;
        this.periodLabel = periodLabel;
    }


    // Static methods
    public static ArrayList<Frequency> getFrequencyList()
    {
        if(frequencyList == null){
            frequencyList = new ArrayList<>();
            frequencyList.add(new Frequency(Period.ZERO, "one time"));
            frequencyList.add(new Frequency(Period.ofDays(1), "daily"));
            frequencyList.add(new Frequency(Period.ofWeeks(1), "weekly"));
            frequencyList.add(new Frequency(Period.ofWeeks(2), "2x monthly"));
            frequencyList.add(new Frequency(Period.ofMonths(1), "monthly"));
            frequencyList.add(new Frequency(Period.ofMonths(3), "quarterly"));
            frequencyList.add(new Frequency(Period.ofMonths(6), "biannually"));
            frequencyList.add(new Frequency(Period.ofYears(1), "annually"));

        }

        return frequencyList;
    }

    public static Frequency getFrequency(Period p)
    {
        for(Frequency f : getFrequencyList())
        {
            if(f.getPeriod().equals(p))
            {
                return f;
            }
        }

        return null;
    }

    // Public methods
    public Period getPeriod()
    {
        return this.period;
    }

    // Overrides
    @Override
    public String toString() {
        return periodLabel;
    }
}
