package com.android.mschrandt.fortune.data;

import java.util.List;
import java.util.UUID;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import com.android.mschrandt.fortune.model.Account;
import com.android.mschrandt.fortune.model.AccountCategory;
import com.android.mschrandt.fortune.model.AccountManager;
import com.android.mschrandt.fortune.model.TransactionScheduler;

import static android.arch.persistence.room.OnConflictStrategy.REPLACE;

@Dao
public interface AccountManagerDao {

    @Query("SELECT * FROM AccountManager LIMIT 1")
    public AccountManager hasAccountManager();

    @Query("SELECT * FROM AccountManager LIMIT 1")
    public LiveData<AccountManager> getAccountManager();

    @Query("SELECT * FROM Account WHERE account_manager_id = :accountManagerId")
    public List<Account> getAccounts(UUID accountManagerId);

    @Query("SELECT * FROM TransactionScheduler WHERE account_manager_id = :accountManagerId")
    public List<TransactionScheduler> getTransactionSchedulers(UUID accountManagerId);

    @Query("SELECT * FROM AccountCategory")
    public List<AccountCategory> getAccountCategories();

    @Insert(onConflict = REPLACE)
    public void insertAccountManager(AccountManager am);

    @Insert(onConflict = REPLACE)
    public void insertAccounts(Account... accounts);

    @Insert(onConflict = REPLACE)
    public void insertTransactionSchedulers(TransactionScheduler... transactionSchedulers);

    @Insert(onConflict = REPLACE)
    public void insertAccountCategories(AccountCategory... accountCategories);

    @Update
    public void updateAccountManager(AccountManager am);

    @Delete
    public void deleteAccountManager(AccountManager am);

    @Delete
    public void deleteAccount(Account account);

    @Delete
    public void deleteTransactionScheduler(TransactionScheduler ts);


}
