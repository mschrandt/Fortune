package com.android.mschrandt.fortune.model;

import java.io.Serializable;
import java.util.*;
import java.time.LocalDate;

public class Transaction implements Serializable
{
  private double amount;
  private LocalDate postedDate;
  private String transactionType;
  private Account accountFrom;
  private Account accountTo;

  public Transaction(double amount,
                     LocalDate postedDate,
                     String transactionType,
                     Account accountFrom,
                     Account accountTo)
  {
    this.amount = Math.round(amount*100)/100.0;
    this.postedDate = postedDate;
    this.transactionType = transactionType;
    this.accountFrom = accountFrom;
    this.accountTo = accountTo;
  }

  public Account getAccountFrom()
  {
    return accountFrom;
  }

  public Account getAccountTo()
  {
    return accountTo;
  }

  public double getAmount() { return this.amount; }

  public LocalDate getPostedDate() { return this.postedDate; }

  public String toString()
  {
    return postedDate + ": $" + amount + " " + transactionType + " - Debit: " + accountFrom + "; Credit: " + accountTo;
  }

  public static Comparator<Transaction> postedDateCompare = new Comparator<Transaction>() {
    public int compare(Transaction t1, Transaction t2)
    {
      return (t1.postedDate.compareTo(t2.postedDate));
    }};
}
