package com.android.mschrandt.fortune.data;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;
import android.content.Context;

import com.android.mschrandt.fortune.model.Account;
import com.android.mschrandt.fortune.model.AccountCategory;
import com.android.mschrandt.fortune.model.AccountManager;
import com.android.mschrandt.fortune.model.TransactionScheduler;

@Database(entities = {AccountManager.class,
        Account.class,
        TransactionScheduler.class,
        AccountCategory.class}, version = 1, exportSchema = false)
@TypeConverters({RoomTypeConverters.class})
public abstract class AccountManagerDatabase extends RoomDatabase {
    public abstract AccountManagerDao accountManagerDao();

    private static volatile AccountManagerDatabase INSTANCE;

    static AccountManagerDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AccountManagerDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AccountManagerDatabase.class, "fortune_database").build();
                }
            }
        }
        return INSTANCE;
    }
}
