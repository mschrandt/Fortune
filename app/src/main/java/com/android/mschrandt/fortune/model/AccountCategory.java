package com.android.mschrandt.fortune.model;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import java.time.Period;
import java.util.ArrayList;
import java.util.UUID;

@Entity
public class AccountCategory {

    // Static variables
    @Ignore
    private static ArrayList<AccountCategory> mAccountCategories;

    // Member variables
    @PrimaryKey
    @ColumnInfo(name = "account_category_id")
    @NonNull
    private UUID mAccountCategoryId;

    @ColumnInfo(name = "category_description")
    private String mCategoryDescription;

    @ColumnInfo(name = "default_is_asset")
    private boolean mDefaultIsAsset;

    @ColumnInfo(name = "default_is_liability")
    private boolean mDefaultIsLiability;

    @ColumnInfo(name = "default_interest_rate_type")
    private InterestRateType mDefaultInterestRateType;

    @ColumnInfo(name = "default_interest_compounding_frequency")
    private Frequency mDefaultInterestCompoundingFrequency;

    // Constructor
    public AccountCategory(String categoryDescription,
                                boolean defaultIsAsset,
                                boolean defaultIsLiability,
                                InterestRateType defaultInterestRateType,
                                Frequency defaultInterestCompoundingFrequency)
    {
        mAccountCategoryId = UUID.randomUUID();
        mCategoryDescription = categoryDescription;
        mDefaultIsAsset = defaultIsAsset;
        mDefaultIsLiability = defaultIsLiability;
        mDefaultInterestRateType = defaultInterestRateType;
        mDefaultInterestCompoundingFrequency = defaultInterestCompoundingFrequency;
    }

    public AccountCategory(UUID id,
                           String categoryDescription,
                           boolean defaultIsAsset,
                           boolean defaultIsLiability,
                           InterestRateType defaultInterestRateType,
                           Frequency defaultInterestCompoundingFrequency)
    {
        mAccountCategoryId = id;
        mCategoryDescription = categoryDescription;
        mDefaultIsAsset = defaultIsAsset;
        mDefaultIsLiability = defaultIsLiability;
        mDefaultInterestRateType = defaultInterestRateType;
        mDefaultInterestCompoundingFrequency = defaultInterestCompoundingFrequency;
    }

    // Static methods
    public static ArrayList<AccountCategory> getAccountCategories() {
        if(mAccountCategories == null)
        {
            mAccountCategories = new ArrayList<>();

            mAccountCategories.add(new AccountCategory("Cash",true, false,
                    InterestRateType.APY, Frequency.getFrequency(Period.ofMonths(1))));

            mAccountCategories.add(new AccountCategory("Credit Card",false, true,
                    InterestRateType.APR, Frequency.getFrequency(Period.ofMonths(1))));

            mAccountCategories.add(new AccountCategory("Investment",true, false,
                    InterestRateType.APY, Frequency.getFrequency(Period.ofDays(1))));

            mAccountCategories.add(new AccountCategory("Loan",false, true,
                    InterestRateType.APR, Frequency.getFrequency(Period.ofMonths(1))));

            mAccountCategories.add(new AccountCategory("Property",true, false,
                    InterestRateType.APY, Frequency.getFrequency(Period.ofMonths(1))));

        }

        return mAccountCategories;
    }

    public static AccountCategory getAccountCategory(UUID id)
    {
        for(AccountCategory ac : mAccountCategories)
        {
            if(ac.getAccountCategoryId().equals(id))
            {
                return ac;
            }
        }

        return null;
    }

    // Public methods
    public UUID getAccountCategoryId() {
        return mAccountCategoryId;
    }

    public boolean getDefaultIsAsset() {
        return mDefaultIsAsset;
    }

    public boolean getDefaultIsLiability() {
        return mDefaultIsLiability;
    }

    public InterestRateType getDefaultInterestRateType() {
        return mDefaultInterestRateType;
    }

    public Frequency getDefaultInterestCompoundingFrequency() {
        return mDefaultInterestCompoundingFrequency;
    }

    public static void reset()
    {
        if(mAccountCategories != null)
        {
            mAccountCategories.clear();
        }
    }

    public static void addAccountCategory(AccountCategory ac)
    {
        if(mAccountCategories == null)
        {
            mAccountCategories = new ArrayList<AccountCategory>();
        }

        mAccountCategories.add(ac);
    }


    public void setAccountCategoryId(@NonNull UUID mAccountCategoryId) {
        this.mAccountCategoryId = mAccountCategoryId;
    }

    public String getCategoryDescription() {
        return mCategoryDescription;
    }

    public void setCategoryDescription(String mCategoryDescription) {
        this.mCategoryDescription = mCategoryDescription;
    }


    public void setDefaultIsAsset(boolean mDefaultIsAsset) {
        this.mDefaultIsAsset = mDefaultIsAsset;
    }

    public void setDefaultIsLiability(boolean mDefaultIsLiability) {
        this.mDefaultIsLiability = mDefaultIsLiability;
    }


    public void setDefaultInterestRateType(InterestRateType mDefaultInterestRateType) {
        this.mDefaultInterestRateType = mDefaultInterestRateType;
    }


    public void setDefaultInterestCompoundingFrequency(Frequency mDefaultInterestCompoundingFrequency) {
        this.mDefaultInterestCompoundingFrequency = mDefaultInterestCompoundingFrequency;
    }

    // Overrides
    @Override
    public String toString() {
        return mCategoryDescription;
    }

}
