package com.android.mschrandt.fortune.data;


import android.arch.core.util.Function;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Transformations;
import android.support.annotation.NonNull;

import com.android.mschrandt.fortune.model.Account;
import com.android.mschrandt.fortune.model.AccountCategory;
import com.android.mschrandt.fortune.model.AccountManager;
import com.android.mschrandt.fortune.model.TransactionScheduler;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class AccountManagerRepository {
    private final AccountManagerDao accountManagerDao;
    private final Executor executor;

    public AccountManagerRepository(AccountManagerDao accountManagerDao)
    {
        this.accountManagerDao = accountManagerDao;
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void loadAccountCategories()
    {
        List<AccountCategory> accCategories = accountManagerDao.getAccountCategories();
        for(AccountCategory ac : accCategories)
        {
            AccountCategory.addAccountCategory(ac);
        }
        saveAccountCategories();
    }

    public void saveAccountCategories()
    {
        for(AccountCategory ac : AccountCategory.getAccountCategories())
        {
            accountManagerDao.insertAccountCategories(ac);
        }
    }

    public LiveData<AccountManager> getAccountManager()
    {
        refreshAccountManager();

        LiveData<AccountManager> am = accountManagerDao.getAccountManager();

        am = Transformations.switchMap(am, new Function<AccountManager, LiveData<AccountManager>>() {
            @Override
            public LiveData<AccountManager> apply(final AccountManager inputAm) {
                if(inputAm == null)
                {
                    return null;
                }

                loadAccountCategories();

                List<Account> accounts = accountManagerDao.getAccounts(inputAm.getAccountManagerId());

                for (Account a : accounts) {
                    inputAm.AddAccount(a);
                }

                List<TransactionScheduler> transactionSchedulers = accountManagerDao.getTransactionSchedulers(inputAm.getAccountManagerId());
                for (TransactionScheduler ts : transactionSchedulers) {
                    ts.setAccountFrom(inputAm.GetAccount(ts.getAccountIdFrom()));
                    ts.setAccountTo(inputAm.GetAccount(ts.getAccountIdTo()));
                    inputAm.AddScheduledTransaction(ts);
                }

                MutableLiveData<AccountManager> output = new MutableLiveData<>();
                output.setValue(inputAm);
                return output;
            }
        });


        return am;
    }

    public void refreshAccountManager()
    {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if(accountManagerDao.hasAccountManager() == null)
                {
                    AccountManager am = AccountManager.getInstance();
                    am.setForecastDate(LocalDate.now().plusYears(10));
                    updateAccountManager(am);
                }
            }
        });
    }

    public void updateAccountManager(AccountManager am)
    {
        executor.execute(new FortuneRunnable(am, null, null) {
            @Override
            public void run() {
                accountManagerDao.insertAccountManager(am);
            }
        });
    }

    public void updateAccount(Account a)
    {
        executor.execute(new FortuneRunnable(null, a, null) {
            @Override
            public void run() {
                accountManagerDao.insertAccounts(a);
            }
        });
    }

    public void updateTransactionScheduler(TransactionScheduler ts)
    {
        executor.execute(new FortuneRunnable(null, null, ts) {
            @Override
            public void run() {
                accountManagerDao.insertTransactionSchedulers(ts);
            }
        });
    }

    public void deleteAccount(Account a)
    {
        executor.execute(new FortuneRunnable(null, a, null) {
            @Override
            public void run() {
                accountManagerDao.deleteAccount(a);
            }
        });
    }

    public void deleteTransactionScheduler(TransactionScheduler ts)
    {
        executor.execute(new FortuneRunnable(null, null, ts) {
            @Override
            public void run() {
                accountManagerDao.deleteTransactionScheduler(ts);
            }
        });
    }

    private abstract class FortuneRunnable implements Runnable
    {
        protected AccountManager am;
        protected Account a;
        protected TransactionScheduler ts;

        public FortuneRunnable(AccountManager am, Account a, TransactionScheduler ts)
        {
            this.am = am;
            this.a = a;
            this.ts = ts;
        }
    }

    private abstract class LiveDataRunnable implements Runnable
    {
        protected LiveData<AccountManager> am;

        public LiveDataRunnable(LiveData<AccountManager> am)
        {
            this.am = am;
        }
    }

}
