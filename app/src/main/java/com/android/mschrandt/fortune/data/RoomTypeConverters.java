package com.android.mschrandt.fortune.data;

import android.arch.persistence.room.TypeConverter;

import com.android.mschrandt.fortune.model.Account;
import com.android.mschrandt.fortune.model.Frequency;
import com.android.mschrandt.fortune.model.InterestRateType;

import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;

public class RoomTypeConverters {

    // Local Dates stored as long
    @TypeConverter
    public static long localDateToLong(LocalDate d)
    {
        if(d == null)
        {
            return 0;
        }
        return d.toEpochDay();
    }
    @TypeConverter
    public static LocalDate longToLocalDate(long d)
    {
        return LocalDate.ofEpochDay(d);
    }

    // UUIDs stored as string
    @TypeConverter
    public static String uuidToString(UUID uuid)
    {
        if(uuid == null)
        {
            return null;
        }
        return uuid.toString();
    }

    @TypeConverter
    public static UUID stringToUUID(String s)
    {
        if(s == null)
        {
            return null;
        }

        return UUID.fromString(s);
    }

    // Interest Rate Types
    @TypeConverter
    public static InterestRateType intToInterestRateType(int i)
    {
        return InterestRateType.values()[i];
    }

    @TypeConverter
    public static int InterestRateTypeToInt(InterestRateType ir)
    {
        return ir.ordinal();
    }

    // Periods
    @TypeConverter
    public static Period stringToPeriod(String s)
    {
        if(s == null)
        {
            return null;
        }

        return Period.parse(s);
    }

    @TypeConverter
    public static String periodToString(Period p)
    {
        if(p == null)
        {
            return null;
        }

        return p.toString();
    }

    // Frequencies
    @TypeConverter
    public static String frequencyToString(Frequency f)
    {
        if(f == null)
        {
            return null;
        }

        return f.getPeriod().toString();
    }

    @TypeConverter
    public static Frequency frequencyToString(String s)
    {
        if(s == null)
        {
            return null;
        }

        return Frequency.getFrequency(Period.parse(s));
    }
}
