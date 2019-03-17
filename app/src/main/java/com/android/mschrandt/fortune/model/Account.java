package com.android.mschrandt.fortune.model;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.UUID;

/***
 * Represents a single financial account. Accounts should be contained within an AccountManager.
 */
@Entity
public class Account implements Serializable
{
    // Member variables
    @PrimaryKey
    @ColumnInfo(name = "account_id")
    @NonNull
    private UUID id;

    @ForeignKey(entity = AccountManager.class, childColumns = "account_manager_id", parentColumns = "account_manager_id")
    @ColumnInfo(name = "account_manager_id")
    private UUID accountManagerId;

    @ColumnInfo(name = "account_name")
    private String accountName;

    @ColumnInfo(name = "account_category_id")
    private UUID accountCategoryId;

    @ColumnInfo(name = "starting_balance")
    private double startingBalance;

    @ColumnInfo(name = "asset")
    private boolean asset;

    @ColumnInfo(name = "liability")
    private boolean liability;

    @ColumnInfo(name = "is_appreciating")
    private boolean isAppreciating;

    @ColumnInfo(name = "is_depreciating")
    private boolean isDepreciating;

    @ColumnInfo(name = "interest_rate")
    private double interestRate;

    @Ignore
    private double dailyInterestRate;

    @ColumnInfo(name = "interest_rate_type")
    private InterestRateType interestRateType;

    @ColumnInfo(name = "compounding_period")
    private Period compoundingPeriod;

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAccountManagerId() {return accountManagerId;}
    public void setAccountManagerId(UUID id) { this.accountManagerId = id;}

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public UUID getAccountCategoryId() {
        return accountCategoryId;
    }

    public void setAccountCategoryId(UUID accountCategoryId) {
        this.accountCategoryId = accountCategoryId;
    }

    public double getStartingBalance() {
        return startingBalance;
    }

    public void setStartingBalance(double startingBalance) {
        this.startingBalance = startingBalance;
    }

    public boolean isAsset() {
        return asset;
    }

    public void setAsset(boolean asset) {
        this.asset = asset;
    }

    public boolean isLiability() {
        return liability;
    }

    public void setLiability(boolean liability) {
        this.liability = liability;
    }

    public boolean isAppreciating() {
        return isAppreciating;
    }

    public void setAppreciating(boolean appreciating) {
        isAppreciating = appreciating;
    }

    public boolean isDepreciating() {
        return isDepreciating;
    }

    public void setDepreciating(boolean depreciating) {
        isDepreciating = depreciating;
    }

    public double getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(double interestRate) {
        this.interestRate = interestRate;
        setDailyInterestRate();
    }

    private void setDailyInterestRate()
    {
        if (this.interestRateType.equals(InterestRateType.APR))
        {
            dailyInterestRate = interestRate/365.25;
        }
        else if (this.interestRateType.equals(InterestRateType.APY))
        {

            double n = compoundingPeriodsPerYear();
            dailyInterestRate = (Math.pow((interestRate+1), 1.0/n)-1)*n/365.25;
        }
    }

    private double compoundingPeriodsPerYear()
    {
        LocalDate d = LocalDate.now();
        for(int i = 0; i < 100; i++)
        {
            d = d.plus(compoundingPeriod);
        }
        return 365.25/(ChronoUnit.DAYS.between(LocalDate.now(), d)/100.0);
    }

    public double getDailyInterestRate()
    {
        return dailyInterestRate;
    }

    public InterestRateType getInterestRateType() {
        return interestRateType;
    }

    public void setInterestRateType(InterestRateType interestRateType){
        this.interestRateType = interestRateType;
        setDailyInterestRate();
    }

    public Period getCompoundingPeriod() {
        return compoundingPeriod;
    }

    public void setCompoundingPeriod(Period compoundingPeriod) {
        this.compoundingPeriod = compoundingPeriod;
        setDailyInterestRate();
    }


    // Constructors

    public Account(String accountName,
                   double startingBalance)
    {
        this(accountName, null, false, false, startingBalance, false, false, 0, InterestRateType.APR, Period.ZERO);
    }

    public Account(String accountName,
                   UUID accountCategoryId,
                   boolean assetAccount,
                   boolean liabilityAccount,
                   double startingBalance,
                   boolean isAppreciating,
                   boolean isDepreciating,
                   double interestRate,
                   InterestRateType interestRateType,
                   Period compoundingPeriod)
    {
        this.id = UUID.randomUUID();
        this.accountCategoryId = accountCategoryId;
        this.accountName = accountName;
        this.asset = assetAccount;
        this.liability = liabilityAccount;
        this.startingBalance = startingBalance;
        this.isAppreciating = isAppreciating;
        this.isDepreciating = isDepreciating;
        this.interestRate = interestRate;
        this.interestRateType = interestRateType;
        this.compoundingPeriod = compoundingPeriod;
        setDailyInterestRate();
    }

    // Overrides
    @Override
    public String toString()
    {
        return this.accountName;
    }

    @Override
    public boolean equals(Object obj) {
        try
        {
            return this.id.equals(((Account)obj).getId());
        }
        catch(Exception e)
        {
            return false;
        }
    }

    public static Comparator<Account> categoryComparator = new Comparator<Account>() {
        public int compare(Account ac1, Account ac2)
        {
            int categoryCompare = AccountCategory.getAccountCategory(ac1.getAccountCategoryId()).toString().compareTo(
                    AccountCategory.getAccountCategory(ac2.getAccountCategoryId()).toString());

            if (categoryCompare == 0)
            {
                return ac1.toString().compareToIgnoreCase(ac2.toString());
            }
            return categoryCompare;

        }};
}
