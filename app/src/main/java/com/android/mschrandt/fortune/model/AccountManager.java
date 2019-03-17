package com.android.mschrandt.fortune.model;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import java.util.*;
import java.time.*;
import java.time.temporal.*;
import java.text.DecimalFormat;

@Entity
public class AccountManager
{
    //Singleton
    private static AccountManager am;

    @PrimaryKey
    @ColumnInfo(name = "account_manager_id")
    @NonNull
    private UUID accountManagerId;

    @ColumnInfo(name = "forecast_date")
    private LocalDate forecastDate;

    @Ignore
    private ArrayList<Account> accounts;
    @Ignore
    private ArrayList<TransactionScheduler> scheduledTransactions;
    @Ignore
    private ArrayList<Transaction> interestTransactions;

    @Ignore
    private Account interestPaid;
    @ColumnInfo(name = "interest_paid_account_id")
    private UUID interestPaidAccountId;

    @Ignore
    private Account interestEarned;
    @ColumnInfo(name = "interest_earned_account_id")
    private UUID interestEarnedAccountId;

    @Ignore
    private Account income;
    @ColumnInfo(name = "income_account_id")
    private UUID incomeAccountId;

    @Ignore
    private Account expense;
    @ColumnInfo(name = "expense_account_id")
    private UUID expenseAccountId;



    private AccountManager()
    {
        this.accounts = new ArrayList<>();
        this.scheduledTransactions = new ArrayList<>();
        this.interestTransactions = new ArrayList<>();

        this.interestPaid  = new Account("Interest Paid", 0);
        this.interestPaidAccountId = interestPaid.getId();
        this.interestEarned = new Account("Interest Earned", 0);
        this.interestEarnedAccountId = interestEarned.getId();

        this.income  = new Account("Income", 0);
        this.incomeAccountId = income.getId();
        this.expense  = new Account("Expense", 0);
        this.expenseAccountId = expense.getId();

        this.forecastDate = LocalDate.now().plusYears(10);
        this.accountManagerId = UUID.randomUUID();
    }

    public AccountManager(UUID accountManagerId, LocalDate forecastDate, UUID interestPaidAccountId, UUID interestEarnedAccountId, UUID incomeAccountId, UUID expenseAccountId)
    {
        this();
        this.setForecastDate(forecastDate);
        this.setAccountManagerId(accountManagerId);
        this.expense.setId(expenseAccountId);
        this.expenseAccountId = expenseAccountId;
        this.income.setId(incomeAccountId);
        this.incomeAccountId = incomeAccountId;
        this.interestEarned.setId(interestEarnedAccountId);
        this.interestEarnedAccountId = interestEarnedAccountId;
        this.interestPaid.setId(interestPaidAccountId);
        this.interestPaidAccountId = interestPaidAccountId;
    }

    public static AccountManager getInstance()
    {
        if(am == null){ am = new AccountManager(); }
        return am;
    }

    public static void setInstance(AccountManager newAm)
    {
        am = newAm;
    }

    public UUID getAccountManagerId()
    {
        return accountManagerId;
    }
    public void setAccountManagerId(UUID uuid)
    {
        accountManagerId = uuid;
    }

    public Account getIncomeAccount()
    {
        return this.income;
    }

    public Account getExpenseAccount()
    {
        return this.expense;
    }

    public LocalDate getForecastDate()
    {
        return this.forecastDate;
    }

    public void setForecastDate(LocalDate forecastDate)
    {
        this.forecastDate = forecastDate;
        UpdateForecast(forecastDate);
    }

    public UUID getInterestPaidAccountId() {
        return interestPaidAccountId;
    }

    public void setInterestPaidAccountId(UUID interestPaidAccountId) {
        this.interestPaidAccountId = interestPaidAccountId;
        this.interestPaid.setId(interestPaidAccountId);
    }

    public UUID getInterestEarnedAccountId() {
        return interestEarnedAccountId;
    }

    public void setInterestEarnedAccountId(UUID interestEarnedAccountId) {
        this.interestEarnedAccountId = interestEarnedAccountId;
        this.interestEarned.setId(interestEarnedAccountId);
    }

    public UUID getIncomeAccountId() {
        return incomeAccountId;
    }

    public void setIncomeAccountId(UUID incomeAccountId) {
        this.incomeAccountId = incomeAccountId;
        this.income.setId(incomeAccountId);
    }

    public UUID getExpenseAccountId() {
        return expenseAccountId;
    }

    public void setExpenseAccountId(UUID expenseAccountId) {
        this.expenseAccountId = expenseAccountId;
        this.expense.setId(expenseAccountId);
    }

    public void AddAccount(Account account)
    {
        this.accounts.add(account);
        account.setAccountManagerId(this.accountManagerId);
        accounts.sort(Account.categoryComparator);
    }

    public Account GetAccount(UUID accountId)
    {
        for(Account a : accounts)
        {
            if(a.getId().equals(accountId))
            {
                return a;
            }
        }

        if(expense.getId().equals(accountId))
        {
            return expense;
        }
        else if(income.getId().equals(accountId))
        {
            return income;
        }

        return null;
    }

    public void RemoveAccount(UUID accountId)
    {
        accounts.remove(GetAccount(accountId));
    }

    public void AddScheduledTransaction(TransactionScheduler scheduledTransaction)
    {
        this.scheduledTransactions.add(scheduledTransaction);
        scheduledTransaction.setAccountManagerId(this.accountManagerId);
    }

    public TransactionScheduler getTransactionScheduler(UUID transactionId)
    {
        for(TransactionScheduler ts : scheduledTransactions)
        {
            if(ts.getTransactionId().equals(transactionId))
            {
                return ts;
            }
        }

        return null;
    }

    public void RemoveScheduledTransaction(UUID transactionId)
    {
        scheduledTransactions.remove(getTransactionScheduler(transactionId));
    }

    public void RemoveScheduledTransactionsForAccount(UUID accountId)
    {
        for(int i = scheduledTransactions.size() - 1; i >= 0; i--)
        {
            if(scheduledTransactions.get(i).getAccountFrom().getId().equals(accountId) ||
                    scheduledTransactions.get(i).getAccountTo().getId().equals(accountId))
            {
                scheduledTransactions.remove(i);
            }
        }
    }

    public void UpdateForecast(LocalDate forecastTo)
    {
        for(TransactionScheduler schedule : scheduledTransactions)
        {
            schedule.forecast(forecastTo);
        }

        this.interestTransactions = new ArrayList<Transaction>();
        for(Account account : accounts)
        {
            if(account.isAppreciating())
            {
                this.interestTransactions.addAll(ForecastInterest(account, interestEarned, GetTransactionsForAccount(account), forecastTo));
            }
            else if(account.isDepreciating())
            {
                this.interestTransactions.addAll(ForecastInterest(account, interestPaid, GetTransactionsForAccount(account), forecastTo));
            }
        }
    }

    public void PrintProjectedBalanceSheet(LocalDate forecastTo)
    {
        DecimalFormat df = new DecimalFormat("0.00");
        UpdateForecast(forecastTo);
        double startingNetWorth = 0;
        double endingNetWorth = 0;
        for(Account account : accounts)
        {
            ArrayList<Transaction> allTransactions = GetAllTransactionsForAccount(account);
            double endingBalance = account.getStartingBalance();

            if (account.isAsset())
            {
                startingNetWorth += account.getStartingBalance();
            }
            else if(account.isLiability())
            {
                startingNetWorth -= account.getStartingBalance();
            }

            for(Transaction t : allTransactions)
            {

                if(t.getAccountFrom().equals(account))
                {
                    endingBalance -= t.getAmount();
                }
                else
                {
                    endingBalance += t.getAmount();
                }
            }
            System.out.println("==============================");
            System.out.println("Account: " + account);
            System.out.println("Starting balance: " + df.format(account.getStartingBalance()));
            System.out.println("Ending balance: " + df.format(endingBalance));
            System.out.println("Growth: " + df.format(endingBalance - account.getStartingBalance()) + ", " + df.format((endingBalance - account.getStartingBalance())/account.getStartingBalance()*100) + "%");
            System.out.println("------------------------------");

            if (account.isAsset())
            {
                endingNetWorth += endingBalance;
            }
            else if(account.isLiability())
            {
                endingNetWorth -= endingBalance;
            }
      /*
      for (Transaction t : allTransactions)
      {
        System.out.println(t);
      }*/
        }

        System.out.println("=========================");
        System.out.println("Starting net worth: " + df.format(startingNetWorth));
        System.out.println("Ending net worth: " + df.format(endingNetWorth));
        System.out.println("Growth: " + df.format(endingNetWorth - startingNetWorth) + ", " + df.format((endingNetWorth - startingNetWorth)/startingNetWorth*100) + "%");
    }

    public void PrintProjectedIncomeStatement(LocalDate forecastTo)
    {

    }

    public ArrayList<Account> GetAccounts()
    {
        return accounts;
    }

    public ArrayList<Transaction> GetTransactionsForAccount(Account account)
    {
        ArrayList<Transaction> accountTrans = new ArrayList<Transaction>();

        for(TransactionScheduler ts : scheduledTransactions)
        {
            if (ts.getAccountFrom().equals(account) || ts.getAccountTo().equals(account))
            {
                accountTrans.addAll(ts.getForecastedTransactions());
            }
        }

        return accountTrans;
    }

    public ArrayList<Transaction> GetInterestTransactionsForAccount(Account account)
    {
        ArrayList<Transaction> accountTrans = new ArrayList<Transaction>();

        for(Transaction t : interestTransactions)
        {
            if (t.getAccountFrom().equals(account) || t.getAccountTo().equals(account))
            {
                accountTrans.add(t);
            }
        }

        return accountTrans;
    }

    public ArrayList<Transaction> GetAllTransactionsForAccount(Account account)
    {
        ArrayList<Transaction> allTransactions = new ArrayList<Transaction>();
        allTransactions.addAll(GetTransactionsForAccount(account));
        allTransactions.addAll(GetInterestTransactionsForAccount(account));

        Collections.sort(allTransactions, Transaction.postedDateCompare);

        return allTransactions;
    }

    public ArrayList<TransactionScheduler> GetTransactionSchedulersForAccount(Account account)
    {
        ArrayList<TransactionScheduler> ts = new ArrayList<>();
        for(TransactionScheduler cur : scheduledTransactions)
        {
            if(cur.getAccountFrom().equals(account) ||
                    cur.getAccountTo().equals(account))
            {
                ts.add(cur);
            }
        }

        return ts;
    }

    public void PrintTransactionsForAccount(Account account)
    {
        ArrayList<Transaction> allTransactions = GetAllTransactionsForAccount(account);
        for(Transaction t : allTransactions)
        {
            System.out.println(t);
        }
    }

    public ArrayList<Transaction> ForecastInterest(Account sourceAccount,
                                                   Account interestAccount,
                                                   ArrayList<Transaction> accountTransactions,
                                                   LocalDate forecastTo)
    {
        ArrayList<Transaction> interestTransactions = new ArrayList<>();
        Double principal = sourceAccount.getStartingBalance();
        Account accountFrom;
        Account accountTo;

        if (sourceAccount.isAppreciating())
        {
            accountFrom = interestAccount;
            accountTo = sourceAccount;
        }
        else
        {
            accountFrom = sourceAccount;
            accountTo = interestAccount;
        }


        LocalDate lowerBound = LocalDate.now();
        LocalDate calcDate = LocalDate.now();
        LocalDate dateInterestDate = LocalDate.now();

        long totalDays = ChronoUnit.DAYS.between(lowerBound, forecastTo);

        Collections.sort(accountTransactions, Transaction.postedDateCompare);

        //ListIterator currentTransaction = accountTransactions.listIterator();

        // Sum all transactions by day
        LinkedList<DailyTransactions> dailyTransactions = new LinkedList<>();
        DailyTransactions lastTransaction = null;

        for(Transaction t : accountTransactions)
        {
            // If money is leaving account, subtract
            int debitMultiplier = 1;
            if(t.getAccountFrom().equals(sourceAccount))
            {
                debitMultiplier = -1;
            }
            long transactionDay = ChronoUnit.DAYS.between(lowerBound, t.getPostedDate());

            if(lastTransaction == null ||
                    lastTransaction.getDay() < transactionDay)
            {
                dailyTransactions.addLast(new DailyTransactions(transactionDay, debitMultiplier* t.getAmount()));
                lastTransaction = dailyTransactions.getLast();
            }
            else if (lastTransaction.getDay() == transactionDay)
            {
                lastTransaction.addAmount(debitMultiplier * t.getAmount());
            }
            else
            {
                // TODO: Handle exception if transactions are added out of order
            }
        }

        ListIterator transactionsIterator = dailyTransactions.listIterator();
        long lastInterestDay = 0;
        long numInterestDays;

        // Loop through interest calc days
        for(long nextInterestDay =  ChronoUnit.DAYS.between(lowerBound, lowerBound.plus(sourceAccount.getCompoundingPeriod()) );
                nextInterestDay <= totalDays;
                nextInterestDay += ChronoUnit.DAYS.between(calcDate, calcDate.plus(sourceAccount.getCompoundingPeriod()) ))
        {
            double interestToAdd = 0;


            // Loop through transaction days between previous interest calc day and next interest calc day
            while( transactionsIterator.hasNext() )
            {
                DailyTransactions currentDailyTransaction = (DailyTransactions)transactionsIterator.next();
                if(currentDailyTransaction.getDay() >= nextInterestDay)
                {
                    transactionsIterator.previous();
                    break;
                }

                // Add transaction amount to principal
                principal += currentDailyTransaction.getAmount();

                // Calculate interest between previous interest calc day and next interest calc day
                // Update interest amount with calculation
                numInterestDays = currentDailyTransaction.getDay() - lastInterestDay;
                // Do not accrue interest if asset has negative balance, or liability has positive balance.
                if((sourceAccount.isLiability() && principal < 0) ||
                        (sourceAccount.isAsset() && principal > 0)) {
                    interestToAdd += principal * (numInterestDays * sourceAccount.getDailyInterestRate());
                }
                lastInterestDay = currentDailyTransaction.getDay();
            }

            // Add interest between last transaction and next interest calc day
            numInterestDays = nextInterestDay - lastInterestDay;
            // Do not accrue interest if asset has negative balance, or liability has positive balance.
            if((sourceAccount.isLiability() && principal < 0) ||
                    (sourceAccount.isAsset() && principal > 0)) {
                interestToAdd += principal * (numInterestDays * sourceAccount.getDailyInterestRate());
            }
            interestToAdd = Math.round(interestToAdd*100)/100.0;
            lastInterestDay = nextInterestDay;

            // Add interest transaction
            interestTransactions.add(new Transaction(interestToAdd,
                    lowerBound.plusDays(nextInterestDay),
                    "Interest",
                    accountFrom,
                    accountTo));

            // Add interest amount to principal
            principal += interestToAdd;


            calcDate = calcDate.plus(sourceAccount.getCompoundingPeriod());
        }
        return interestTransactions;
    }

    class DailyTransactions
    {
        private long mDay;
        private double mAmount;

        public DailyTransactions(long pDay, double pAmount)
        {
            mDay = pDay;
            mAmount = pAmount;
        }

        public void addAmount(double pAmount)
        {
            mAmount += pAmount;
        }

        public double getAmount() {
            return mAmount;
        }

        public void setAmount(double pAmount) {
            mAmount = pAmount;
        }

        public long getDay() {
            return mDay;
        }

        public void setDay(long pDay) {
            mDay = pDay;
        }
    }
}
