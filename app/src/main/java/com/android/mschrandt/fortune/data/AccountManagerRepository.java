package com.android.mschrandt.fortune.data;


import android.arch.core.util.Function;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.Transformations;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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
        AccountCategory.reset();
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

        LiveData<AccountManager> liveAccountManager = accountManagerDao.getAccountManager();
        final MediatorLiveData<AccountManager> dataMerger = new MediatorLiveData();
        dataMerger.addSource(liveAccountManager, new Observer<AccountManager>() {
            @Override
            public void onChanged(@Nullable AccountManager accountManager) {
                if(accountManager == null)
                {
                    return;
                }
                AccountManager.setInstance(accountManager);
                executor.execute(new FortuneRunnable(accountManager, null,null) {
                    @Override
                    public void run() {
                        loadAccountCategories();

                        List<Account> accounts = accountManagerDao.getAccounts(am.getAccountManagerId());

                        for (Account a : accounts) {
                            am.AddAccount(a);
                        }

                        List<TransactionScheduler> transactionSchedulers = accountManagerDao.getTransactionSchedulers(am.getAccountManagerId());
                        for (TransactionScheduler ts : transactionSchedulers) {
                            ts.setAccountFrom(am.GetAccount(ts.getAccountIdFrom()));
                            ts.setAccountTo(am.GetAccount(ts.getAccountIdTo()));
                            am.AddScheduledTransaction(ts);
                        }

                        dataMerger.postValue(am);
                    }
                });
            }
        });

        return dataMerger;
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
                    accountManagerDao.insertAccountManager(am);
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

        public LiveDataRunnable(LiveData am)
        {
            this.am = am;
        }
    }

}
