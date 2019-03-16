package com.android.mschrandt.fortune.activities;

import android.arch.lifecycle.ViewModelProviders;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.android.mschrandt.fortune.data.AccountManagerViewModel;
import com.android.mschrandt.fortune.model.Account;
import com.android.mschrandt.fortune.model.AccountManager;
import com.android.mschrandt.fortune.model.Frequency;
import com.android.mschrandt.fortune.utils.MoneyTextWatcher;
import com.android.mschrandt.fortune.R;
import com.android.mschrandt.fortune.model.TransactionScheduler;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Currency;
import java.util.Locale;
import java.util.UUID;

public class TransactionSchedulerAttributesActivity extends AppCompatActivity {

    // Activity constants
    public static final String EXTRA_TRANSACTION_REQUEST_CODE = "EXTRA_TRANSACTION_REQUEST_CODE";
    public static final String EXTRA_DELETE_TRANSACTION = "EXTRA_DELETE_TRANSACTION";
    public static final String EXTRA_ADD_TRN_DESCRIPTION = "EXTRA_ADD_TRN_DESCRIPTION";
    public static final String EXTRA_ADD_TRN_IN_ACC = "EXTRA_ADD_TRN_IN_ACC";
    public static final String EXTRA_ADD_TRN_DEST_ACC_ID = "EXTRA_ADD_TRN_DEST_ACC_ID";
    public static final String EXTRA_ADD_TRN_AMOUNT = "EXTRA_ADD_TRN_AMOUNT";
    public static final String EXTRA_ADD_TRN_DATE_FROM = "EXTRA_ADD_TRN_DATE_FROM";
    public static final String EXTRA_ADD_TRN_DATE_TO = "EXTRA_ADD_TRN_DATE_TO";
    public static final String EXTRA_ADD_TRN_PERIOD = "EXTRA_ADD_TRN_PERIOD";

    // Data model
    private AccountManagerViewModel mModel;
    private AccountManager mAccountManager;
    private UUID mAccountId;
    private UUID mTransactionId;
    private Account mAccount;
    private ArrayList<Account> mDestAccounts;
    private boolean mIsExpense;
    private boolean mIsTransfer;
    private boolean mIsIncome;
    private boolean mOneTime;
    private boolean mRecurring;

    // Layout
    private EditText mEditTextTransactionDescription;
    private Spinner mSpinnerTransactionType;
    private ArrayList<String> mListTransactionTypes;
    private ConstraintLayout mConstraintlayoutAccountSourceDest;
    private TextView mTextViewAccountName;
    private ToggleButton mToggleButtonSourceFromAccount;
    private Spinner mSpinnerDestinationAccount;
    private ConstraintLayout mConstraintLayoutAmount;
    private Spinner mSpinnerFrequency;
    private TextView mTextViewCurrencySymbol;
    private EditText mEditTextAmount;
    private TextView mTextViewDateFrom;
    private DatePicker mDatePickerDateFrom;
    private TextView mTextViewDateTo;
    private DatePicker mDatePickerDateTo;

    // Activity
    private Intent mIntent;
    private int requestCode;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_scheduler_attributes);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mIntent = getIntent();

        mModel = ViewModelProviders.of(this).get(AccountManagerViewModel.class);
        //mModel.init();

        // Transaction scheduler layout fields
        mEditTextTransactionDescription = findViewById(R.id.transaction_description);
        mSpinnerTransactionType = findViewById(R.id.transaction_type);
        mConstraintlayoutAccountSourceDest = findViewById(R.id.layout_account_source_dest);
        mTextViewAccountName = findViewById(R.id.account_name);
        mToggleButtonSourceFromAccount = findViewById(R.id.source_from_account);
        mSpinnerDestinationAccount = findViewById(R.id.dest_accounts);
        mConstraintLayoutAmount = findViewById(R.id.layout_transaction_amount);
        mSpinnerFrequency = findViewById(R.id.transaction_frequency);
        mTextViewCurrencySymbol = findViewById(R.id.currency_symbol);
        mEditTextAmount = findViewById(R.id.transaction_amount);
        mTextViewDateFrom = findViewById(R.id.date_from_text);
        mDatePickerDateFrom = findViewById(R.id.from_date);
        mTextViewDateTo = findViewById(R.id.date_to_text);
        mDatePickerDateTo = findViewById(R.id.to_date);

        // Get extras
        mAccountManager = AccountManager.getInstance();
        mAccountId = (UUID) mIntent.getSerializableExtra(AccountActivity.EXTRA_ACCOUNT_ID);
        mAccount = mAccountManager.GetAccount(mAccountId);
        requestCode = mIntent.getIntExtra(EXTRA_TRANSACTION_REQUEST_CODE,0);

        // Initialize visible views
        mConstraintlayoutAccountSourceDest.setVisibility(View.GONE);
        mConstraintLayoutAmount.setVisibility(View.GONE);
        mTextViewDateFrom.setVisibility(View.GONE);
        mDatePickerDateFrom.setVisibility(View.GONE);
        mTextViewDateTo.setVisibility(View.GONE);
        mDatePickerDateTo.setVisibility(View.GONE);

        // Add currency formatting to amount
        mEditTextAmount.addTextChangedListener(new MoneyTextWatcher());
        mTextViewCurrencySymbol.setText(Currency.getInstance(Locale.getDefault()).getSymbol());

        // Set account name
        mTextViewAccountName.setText(mAccount.getAccountName());

        // Set minimum date
        mDatePickerDateFrom.setOnDateChangedListener(new DatePicker.OnDateChangedListener() {
            @Override
            public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar c = Calendar.getInstance();
                c.set(year, monthOfYear, dayOfMonth);
                c.add(Calendar.DAY_OF_MONTH, 1);
                mDatePickerDateTo.setMinDate(c.getTimeInMillis());
            }
        });
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, 1);
        mDatePickerDateTo.setMinDate(c.getTimeInMillis());


        // Creating adapter for transaction type spinner
        mListTransactionTypes = new ArrayList<>();
        mListTransactionTypes.add("Select a transaction type");
        mListTransactionTypes.add("Income");
        mListTransactionTypes.add("Expense");
        mListTransactionTypes.add("Transfer");
        ArrayAdapter<String> transTypeAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item,
                mListTransactionTypes){
            @Override
            public boolean isEnabled(int position){
                if(position == 0)
                {
                    // Disable the first item from Spinner
                    // First item will be use for hint
                    return false;
                }
                else
                {
                    return true;
                }
            }
            @Override
            public View getDropDownView(int position, View convertView,
                                        ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                if(position == 0){
                    // Set the hint text color gray
                    tv.setTextColor(Color.GRAY);
                }
                else {
                    tv.setTextColor(Color.BLACK);
                }
                return view;
            }
        };

        transTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerTransactionType.setAdapter(transTypeAdapter);
        mSpinnerTransactionType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch((String) parent.getAdapter().getItem(position))
                {
                    case "Income":
                        mIsExpense = false;
                        mIsIncome = true;
                        mIsTransfer = false;
                        mConstraintlayoutAccountSourceDest.setVisibility(View.GONE);
                        mConstraintLayoutAmount.setVisibility(View.VISIBLE);
                        mTextViewDateFrom.setVisibility(View.VISIBLE);
                        mDatePickerDateFrom.setVisibility(View.VISIBLE);
                        break;
                    case "Expense":
                        mIsExpense = true;
                        mIsIncome = false;
                        mIsTransfer = false;
                        mConstraintlayoutAccountSourceDest.setVisibility(View.GONE);
                        mConstraintLayoutAmount.setVisibility(View.VISIBLE);
                        mTextViewDateFrom.setVisibility(View.VISIBLE);
                        mDatePickerDateFrom.setVisibility(View.VISIBLE);
                        break;
                    case "Transfer":
                        mIsExpense = false;
                        mIsIncome = false;
                        mIsTransfer = true;
                        mConstraintlayoutAccountSourceDest.setVisibility(View.VISIBLE);
                        mConstraintLayoutAmount.setVisibility(View.VISIBLE);
                        mTextViewDateFrom.setVisibility(View.VISIBLE);
                        mDatePickerDateFrom.setVisibility(View.VISIBLE);
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });



        // Initialize dest account spinner
        mDestAccounts = new ArrayList<>();
        mDestAccounts.add(new Account("Select an account", 0));
        for(Account a : mAccountManager.GetAccounts())
        {
            if(!a.equals(mAccount))
            {
                mDestAccounts.add(a);
            }
        }

        ArrayAdapter<Account> destinationAccAdapter = new ArrayAdapter<Account>(this, android.R.layout.simple_spinner_item, mDestAccounts){
            @Override
            public boolean isEnabled(int position){
                if(position == 0)
                {
                    // Disable the first item from Spinner
                    // First item will be use for hint
                    return false;
                }
                else
                {
                    return true;
                }
            }
            @Override
            public View getDropDownView(int position, View convertView,
                                        ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                if(position == 0){
                    // Set the hint text color gray
                    tv.setTextColor(Color.GRAY);
                }
                else {
                    tv.setTextColor(Color.BLACK);
                }
                return view;
            }
        };

        destinationAccAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerDestinationAccount.setAdapter(destinationAccAdapter);
        mSpinnerDestinationAccount.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        // Initialize frequency spinner
        ArrayAdapter<Frequency> frequencyArrayAdapter = new ArrayAdapter<Frequency>(this,
                android.R.layout.simple_spinner_item,
                Frequency.getFrequencyList());
        frequencyArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerFrequency.setAdapter(frequencyArrayAdapter);
        mSpinnerFrequency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(((Frequency) parent.getAdapter().getItem(position)).getPeriod().equals(Period.ZERO))
                {
                    mOneTime = true;
                    mRecurring = false;
                    mTextViewDateFrom.setText("On");
                    mTextViewDateFrom.setVisibility(View.VISIBLE);
                    mDatePickerDateFrom.setVisibility(View.VISIBLE);
                    mTextViewDateTo.setVisibility(View.GONE);
                    mDatePickerDateTo.setVisibility(View.GONE);
                }
                else
                {
                    mOneTime = false;
                    mRecurring = true;
                    mTextViewDateFrom.setText("From");
                    mTextViewDateFrom.setVisibility(View.VISIBLE);
                    mDatePickerDateFrom.setVisibility(View.VISIBLE);
                    mTextViewDateTo.setVisibility(View.VISIBLE);
                    mDatePickerDateTo.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        if(requestCode == AccountActivity.REQUEST_CODE_ADD_TRANSACTION)
        {
            setTitle("New " + mAccount.getAccountName() + " Transaction");
        }
        else if (requestCode == AccountActivity.REQUEST_CODE_EDIT_TRANSACTION)
        {
            setTitle("Edit " + mAccount.getAccountName() + " Transaction");
            mTransactionId = (UUID) mIntent.getSerializableExtra(AccountActivity.EXTRA_TRANSACTION_ID);
            populateFieldsFromTransactionId();
        }

        // Button handlers
        FloatingActionButton fabAdd = findViewById(R.id.add_transaction_add);
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!validateTransactionFields())
                {
                    // do not close
                }
                else
                {
                    Intent intentAddAcc = new Intent();
                    boolean inAcc;
                    UUID destAccountId;

                    if (mIsExpense)
                    {
                        destAccountId = mAccountManager.getExpenseAccount().getId();
                        inAcc = false;
                    }
                    else if (mIsIncome)
                    {
                        destAccountId = mAccountManager.getIncomeAccount().getId();
                        inAcc = true;
                    }
                    else
                    {
                        destAccountId = ((Account) mSpinnerDestinationAccount.getSelectedItem()).getId();
                        inAcc = !mToggleButtonSourceFromAccount.isChecked();
                    }

                    intentAddAcc.putExtra(EXTRA_ADD_TRN_DESCRIPTION, mEditTextTransactionDescription.getText().toString());
                    intentAddAcc.putExtra(EXTRA_ADD_TRN_IN_ACC, inAcc);
                    intentAddAcc.putExtra(EXTRA_ADD_TRN_DEST_ACC_ID, destAccountId);
                    intentAddAcc.putExtra(EXTRA_ADD_TRN_AMOUNT, Double.parseDouble("0"+mEditTextAmount.getText().toString().replaceAll("[^0-9-.]", "")));
                    intentAddAcc.putExtra(EXTRA_ADD_TRN_DATE_FROM, LocalDate.of(mDatePickerDateFrom.getYear(),
                                                                                mDatePickerDateFrom.getMonth()+1,
                                                                                mDatePickerDateFrom.getDayOfMonth()));
                    intentAddAcc.putExtra(EXTRA_ADD_TRN_DATE_TO, LocalDate.of(mDatePickerDateTo.getYear(),
                                                                                mDatePickerDateTo.getMonth()+1,
                                                                                mDatePickerDateTo.getDayOfMonth()));
                    intentAddAcc.putExtra(EXTRA_ADD_TRN_PERIOD, ((Frequency)mSpinnerFrequency.getSelectedItem()).getPeriod());
                    intentAddAcc.putExtra(AccountActivity.EXTRA_TRANSACTION_ID, mTransactionId);
                    setResult(RESULT_OK, intentAddAcc);
                    finish();
                }
            }
        });

        FloatingActionButton fabCancel = findViewById(R.id.add_transaction_cancel);
        fabCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(mTransactionId != null) {
            MenuInflater menuInflater = getMenuInflater();
            menuInflater.inflate(R.menu.menu_transaction, menu);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_remove_transaction:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                builder.setMessage("Are you sure you want to remove this transaction?")
                        .setPositiveButton("Yes",  new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                mModel.deleteTransactionScheduler(mAccountManager.getTransactionScheduler(mTransactionId));
                                mAccountManager.RemoveScheduledTransaction(mTransactionId);
                                Intent deleteTransactionIntent = new Intent();
                                deleteTransactionIntent.putExtra(EXTRA_DELETE_TRANSACTION, true);
                                setResult(RESULT_OK, deleteTransactionIntent);
                                finish();
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,int id) {
                                dialog.cancel();
                            }
                        })
                        .show();
                return true;
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void populateFieldsFromTransactionId()
    {
        TransactionScheduler ts = mAccountManager.getTransactionScheduler(mTransactionId);

        mEditTextTransactionDescription.setText(ts.getDescription());
        mToggleButtonSourceFromAccount.setChecked(ts.getAccountFrom().equals(mAccount));
        mEditTextAmount.setText(String.format("%.2f", ts.getAmount()));
        mDatePickerDateFrom.updateDate(ts.getStartDate().getYear(), ts.getStartDate().getMonthValue()-1, ts.getStartDate().getDayOfMonth());
        mDatePickerDateTo.updateDate(ts.getEndDate().getYear(), ts.getEndDate().getMonthValue()-1, ts.getEndDate().getDayOfMonth());

        // set transaction type spinner
        if(ts.getAccountFrom().equals(mAccountManager.getExpenseAccount()) ||
                ts.getAccountTo().equals(mAccountManager.getExpenseAccount()))
        {
            mSpinnerTransactionType.setSelection(mListTransactionTypes.indexOf("Expense"));
        }
        else if(ts.getAccountFrom().equals(mAccountManager.getIncomeAccount()) ||
                ts.getAccountTo().equals(mAccountManager.getIncomeAccount()))
        {
            mSpinnerTransactionType.setSelection(mListTransactionTypes.indexOf("Income"));
        }
        else
        {
            mSpinnerTransactionType.setSelection(mListTransactionTypes.indexOf("Transfer"));
        }

        // set Dest account spinner
        if(ts.getAccountFrom().equals(mAccount))
        {
            mSpinnerDestinationAccount.setSelection(mDestAccounts.indexOf(ts.getAccountTo()));
        }
        else
        {
            mSpinnerDestinationAccount.setSelection(mDestAccounts.indexOf(ts.getAccountFrom()));
        }

        // set period spinner
        for(int i = 0; i < Frequency.getFrequencyList().size(); i++)
        {
            if(Frequency.getFrequencyList().get(i).getPeriod().equals(ts.getPeriod()))
            {
                mSpinnerFrequency.setSelection(i);
            }
        }
    }

    private boolean validateTransactionFields()
    {
        String errorMessage = "";

        if(mEditTextTransactionDescription.getText().toString().equals(""))
        {
            errorMessage = "Enter a description";
            mEditTextTransactionDescription.requestFocus();
        }
        else if (mSpinnerTransactionType.getSelectedItemPosition() == 0)
        {
            errorMessage = "Select transaction type";
            mSpinnerTransactionType.requestFocus();
        }
        else if (mIsTransfer && mSpinnerDestinationAccount.getSelectedItemPosition() == 0)
        {
            errorMessage = "Select a destination account";
            mSpinnerDestinationAccount.requestFocus();
        }
        else if (mEditTextAmount.getText().toString().equals(""))
        {
            errorMessage = "Enter transaction amount";
            mEditTextAmount.requestFocus();
        }
        else if (mRecurring && !LocalDate.of(mDatePickerDateFrom.getYear(), mDatePickerDateFrom.getMonth()+1, mDatePickerDateFrom.getDayOfMonth()).isBefore(
                LocalDate.of(mDatePickerDateTo.getYear(), mDatePickerDateTo.getMonth()+1, mDatePickerDateTo.getDayOfMonth())))
        {
            errorMessage = "End date must be after start date";
            mDatePickerDateTo.requestFocus();
        }

        if(errorMessage.length() != 0)
        {
            Toast t = Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT);
            t.setGravity(Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL, 0, 0);
            t.show();
            return false;
        }
        else
        {
            return true;
        }
    }
}
