package com.android.mschrandt.fortune.data;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.arch.persistence.room.Room;

import com.android.mschrandt.fortune.model.Account;
import com.android.mschrandt.fortune.model.AccountManager;
import com.android.mschrandt.fortune.model.TransactionScheduler;

import java.time.LocalDate;
import java.util.UUID;

public class AccountManagerViewModel extends AndroidViewModel {
    private AccountManagerRepository accountManagerRepo;

    private LiveData<AccountManager> accountManager;

    public AccountManagerViewModel(Application application) {
        super(application);

        this.accountManagerRepo = new AccountManagerRepository(AccountManagerDatabase.getDatabase(application).accountManagerDao());
    }

    public void init()
    {
        if(this.accountManager == null)
        {
            this.accountManager = accountManagerRepo.getAccountManager();
        }
    }

    public LiveData<AccountManager> getAccountManager()
    {
        return this.accountManager;
    }

    public void updateForecastDate(LocalDate forecastDate)
    {
        accountManager.getValue().setForecastDate(forecastDate);
        accountManagerRepo.updateAccountManager(accountManager.getValue());
    }

    public void updateAccount(Account a)
    {
        accountManagerRepo.updateAccount(a);
    }

    public void updateTransactionScheduler(TransactionScheduler ts)
    {
        accountManagerRepo.updateTransactionScheduler(ts);
    }

    public void deleteAccount(Account a)
    {
        accountManagerRepo.deleteAccount(a);
    }

    public void deleteTransactionScheduler(TransactionScheduler ts)
    {
        accountManagerRepo.deleteTransactionScheduler(ts);
    }

    public void deleteTransactionScheduler(UUID id)
    {
        accountManagerRepo.deleteTransactionScheduler(accountManager.getValue().getTransactionScheduler(id));
    }
}
