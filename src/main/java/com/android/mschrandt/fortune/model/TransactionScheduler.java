package com.android.mschrandt.fortune.model;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import java.io.Serializable;
import java.util.*;
import java.time.LocalDate;
import java.time.Period;

@Entity
public class TransactionScheduler implements Serializable
{
    @PrimaryKey
    @ColumnInfo(name = "transaction_id")
    @NonNull
    private UUID transactionId;

    @ColumnInfo(name = "amount")
    private double amount;

    @ColumnInfo(name = "period")
    private Period period;

    @ColumnInfo(name = "start_date")
    private LocalDate startDate;

    @ColumnInfo(name = "end_date")
    private LocalDate endDate;

    @ForeignKey(entity = AccountManager.class, parentColumns = "account_manager_id", childColumns = "account_manager_id")
    @ColumnInfo(name = "account_manager_id")
    private UUID accountManagerId;

    @ForeignKey(entity = Account.class, parentColumns = "account_id", childColumns = "account_id_from")
    @ColumnInfo(name = "account_id_from")
    private UUID accountIdFrom;

    @Ignore
    private Account accountFrom;

    @ForeignKey(entity = Account.class, parentColumns = "account_id", childColumns = "account_id_to")
    @ColumnInfo(name = "account_id_to")
    private UUID accountIdTo;

    @Ignore
    private Account accountTo;

    @ColumnInfo(name = "description")
    private String description;

    @Ignore
    private ArrayList<Transaction> forecastedTransactions;

    public TransactionScheduler() {}

    public TransactionScheduler(double amount,
                                Period period,
                                LocalDate startDate,
                                LocalDate endDate,
                                Account accountFrom,
                                Account accountTo,
                                String description)
    {
        this.transactionId = UUID.randomUUID();
        this.amount = amount;
        this.period = period;
        this.startDate = startDate;
        this.endDate = endDate;
        this.accountFrom = accountFrom;
        this.accountIdFrom = accountFrom.getId();
        this.accountTo = accountTo;
        this.accountIdTo = accountTo.getId();
        this.description = description;

        this.forecastedTransactions = new ArrayList<>();
    }

    public void forecast(LocalDate throughDate)
    {
        this.forecastedTransactions = new ArrayList<>();

        LocalDate lowerBound = firstOccurrenceAfterToday();

        if (lowerBound == null)
        {
            return;
        }

        if (period.equals(Period.ZERO) && !lowerBound.isAfter(throughDate))
        {
            forecastedTransactions.add(new Transaction(this.amount,
                    lowerBound,
                    this.description,
                    this.accountFrom,
                    this.accountTo));
        }
        else
        {
            while (!lowerBound.isAfter(throughDate) && !lowerBound.isAfter(endDate))
            {
                forecastedTransactions.add(new Transaction(this.amount,
                        lowerBound,
                        this.description,
                        this.accountFrom,
                        this.accountTo));
                lowerBound = lowerBound.plus(this.period);
            }
        }

    }

    public void printForecastedTransactions()
    {
        for(Transaction t : forecastedTransactions)
        {
            System.out.println(t);
        }
    }

    // Private functions
    private LocalDate firstOccurrenceAfterToday()
    {
        LocalDate start = this.startDate;
        // One time transactions - return null if it occurs before today
        if (period.equals(Period.ZERO))
        {
            if (!start.isAfter(LocalDate.now()))
            {
                return null;
            }
            return start;
        }

        // Recurring transactions - return null if last date is before today
        while (!start.isAfter(LocalDate.now()))
        {
            start = start.plus(this.period);

            if (start.isAfter(this.endDate))
            {
                return null;
            }
        }
        return start;
    }

    // Getters and setters


    public UUID getTransactionId() {
        return transactionId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public Period getPeriod() {
        return period;
    }

    public void setPeriod(Period period) {
        this.period = period;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Account getAccountFrom() {
        return accountFrom;
    }

    public void setAccountFrom(Account accountFrom) {
        this.accountFrom = accountFrom;
        this.accountIdFrom = accountFrom.getId();
    }

    public Account getAccountTo() {
        return accountTo;
    }

    public void setAccountTo(Account accountTo) {
        this.accountTo = accountTo;
        this.accountIdTo = accountTo.getId();
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public UUID getAccountIdFrom() {
        return accountIdFrom;
    }

    public void setAccountIdFrom(UUID accountIdFrom) {
        this.accountIdFrom = accountIdFrom;
    }

    public UUID getAccountIdTo() {
        return accountIdTo;
    }

    public void setAccountIdTo(UUID accountIdTo) {
        this.accountIdTo = accountIdTo;
    }

    public UUID getAccountManagerId() {
        return accountManagerId;
    }

    public void setAccountManagerId(UUID accountManagerId) {
        this.accountManagerId = accountManagerId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ArrayList<Transaction> getForecastedTransactions()
    {
        return this.forecastedTransactions;
    }

    public TransactionCategory getTransactionCategory()
    {
        AccountManager am = AccountManager.getInstance();

        if(getAccountFrom().equals(am.getIncomeAccount()) ||
                getAccountTo().equals(am.getIncomeAccount()))
        {
            return TransactionCategory.INCOME;
        }
        else if(getAccountFrom().equals(am.getExpenseAccount()) ||
                getAccountTo().equals(am.getExpenseAccount()))
        {
            return TransactionCategory.EXPENSE;
        }
        else
        {
            return TransactionCategory.TRANSFER;
        }
    }

    public static Comparator<TransactionScheduler> transactionSchedulerComparator = new Comparator<TransactionScheduler>() {
        public int compare(TransactionScheduler t1, TransactionScheduler t2)
        {
            // Determine common account, use other account for comparison
            Account a1;
            Account a2;

            if(t1.getAccountFrom().equals(t2.getAccountFrom()))
            {
                a1 = t1.getAccountTo();
                a2 = t2.getAccountTo();
            }
            else if(t1.getAccountFrom().equals(t2.getAccountTo()))
            {
                a1 = t1.getAccountTo();
                a2 = t2.getAccountFrom();
            }
            else if(t1.getAccountTo().equals(t2.getAccountFrom()))
            {
                a1 = t1.getAccountFrom();
                a2 = t2.getAccountTo();
            }
            else
            {
                a1 = t1.getAccountFrom();
                a2 = t1.getAccountTo();
            }

            ArrayList<Account> accOrder = new ArrayList<>();
            accOrder.add(AccountManager.getInstance().getIncomeAccount());
            accOrder.add(AccountManager.getInstance().getExpenseAccount());
            accOrder.addAll(AccountManager.getInstance().GetAccounts());

            int accOrderCompare = accOrder.indexOf(a1) - accOrder.indexOf(a2);
            if(accOrderCompare != 0)
            {
                return accOrderCompare;
            }

            int startDateCompare = t1.getStartDate().compareTo(t2.getStartDate());
            if(startDateCompare != 0)
            {
                return startDateCompare;
            }

            return(a1.getAccountName().compareTo(a2.getAccountName()));
        }};

    // Overrides
    @Override
    public String toString() {
        return this.description;
    }

    @Override
    public boolean equals(Object obj) {
        try
        {
            return this.transactionId.equals(((TransactionScheduler) obj).getTransactionId());
        }
        catch (Exception e)
        {
            return false;
        }
    }
}
