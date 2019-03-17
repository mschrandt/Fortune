package com.android.mschrandt.fortune.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.mschrandt.fortune.model.Account;
import com.android.mschrandt.fortune.model.AccountCategory;
import com.android.mschrandt.fortune.model.AccountManager;
import com.android.mschrandt.fortune.model.Frequency;
import com.android.mschrandt.fortune.model.InterestRateType;
import com.android.mschrandt.fortune.utils.MoneyTextWatcher;
import com.android.mschrandt.fortune.R;

import java.time.Period;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Locale;
import java.util.UUID;

public class AccountAttributesActivity extends AppCompatActivity {

    // Data model
    private UUID mUUIDAccountId;
    private Account mAccount;
    private AccountManager mAccountManager;
    private boolean mIsLiability = false;
    private boolean mIsAsset = true;

    // Account attribute fields

    private EditText mEditTextAccountName;
    private Spinner mSpinnerAccountCategory;
    private ConstraintLayout mConstraintLayoutAttributes;
    private TextView mTextViewCurrencySymbol;
    private EditText mEditTextStartingBalance;
    private EditText mEditTextInterestRate;
    private Spinner mSpinnerInterestRateType;
    private Spinner mSpinnerInterestRateFrequency;

    // Extra keys
    public static final String EXTRA_ACCOUNT_REQUEST_CODE = "EXTRA_TRANSACTION_REQUEST_CODE";
    public static final String EXTRA_ADD_ACC_NAME = "EXTRA_ADD_ACC_NAME";
    public static final String EXTRA_ADD_ACC_CATEGORY = "EXTRA_ADD_ACC_CATEGORY";
    public static final String EXTRA_ADD_ACC_IS_ASSET = "EXTRA_ADD_ACC_IS_ASSET";
    public static final String EXTRA_ADD_ACC_IS_LIABILITY = "EXTRA_ADD_IS_LIABILITY";
    public static final String EXTRA_ADD_ACC_STARTING_BALANCE = "EXTRA_ADD_ACC_STARTING_BALANCE";
    public static final String EXTRA_ADD_ACC_INTEREST_RATE =  "EXTRA_ADD_ACC_INTEREST_RATE";
    public static final String EXTRA_ADD_ACC_INTEREST_RATE_TYPE = "EXTRA_ADD_ACC_INTEREST_RATE_TYPE";
    public static final String EXTRA_ADD_ACC_INTEREST_RATE_FREQUENCY = "EXTRA_ADD_ACC_INTEREST_RATE_FREQUENCY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_attributes);
        Intent intent = getIntent();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Model
        mAccountManager = AccountManager.getInstance();

        // Add account fields
        mEditTextAccountName = findViewById(R.id.add_account_name);
        mSpinnerAccountCategory = findViewById(R.id.account_category_constraint);
        mConstraintLayoutAttributes = findViewById(R.id.account_attributes);
        mTextViewCurrencySymbol = findViewById(R.id.currency_symbol);
        mEditTextStartingBalance = findViewById(R.id.add_account_balance);
        mEditTextInterestRate = findViewById(R.id.add_account_interest_rate);
        mSpinnerInterestRateType = findViewById(R.id.interest_rate_type);
        mSpinnerInterestRateFrequency = findViewById(R.id.interest_rate_frequency);

        mTextViewCurrencySymbol.setText(Currency.getInstance(Locale.getDefault()).getSymbol());
        mConstraintLayoutAttributes.setVisibility(View.GONE);
        mEditTextStartingBalance.addTextChangedListener(new MoneyTextWatcher());

        // Spinners
        // Interest rate type spinner
        ArrayList<InterestRateType> interestRateTypes = new ArrayList<>();
        interestRateTypes.add(InterestRateType.APR);
        interestRateTypes.add(InterestRateType.APY);
        ArrayAdapter<InterestRateType> interestRateTypeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                interestRateTypes);
        mSpinnerInterestRateType.setAdapter(interestRateTypeAdapter);

        // Interest frequency spinner
        ArrayAdapter<Frequency> frequencyArrayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                Frequency.getFrequencyList());
        frequencyArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerInterestRateFrequency.setAdapter(frequencyArrayAdapter);

        // Account category spinner
        ArrayList<AccountCategory> accountCategories = new ArrayList<>();
        accountCategories.add(new AccountCategory("Select an account category", false, false, InterestRateType.APR, Frequency.getFrequency(Period.ZERO)));
        accountCategories.addAll(AccountCategory.getAccountCategories());

        ArrayAdapter<AccountCategory> accountCategoryArrayAdapter = new ArrayAdapter<AccountCategory>(this,
                android.R.layout.simple_spinner_item,
                accountCategories){
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

        accountCategoryArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerAccountCategory.setAdapter(accountCategoryArrayAdapter);

        mSpinnerAccountCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private int check = 0;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(check++ > 0  && position > 0) {
                    AccountCategory selectedCategory = (AccountCategory) parent.getAdapter().getItem(position);

                    mIsAsset = selectedCategory.getDefaultIsAsset();
                    mIsLiability = selectedCategory.getDefaultIsLiability();
                    mSpinnerInterestRateType.setSelection(((ArrayAdapter<InterestRateType>) mSpinnerInterestRateType.getAdapter()).getPosition(selectedCategory.getDefaultInterestRateType()));
                    mSpinnerInterestRateFrequency.setSelection(((ArrayAdapter<Frequency>) mSpinnerInterestRateFrequency.getAdapter()).getPosition(selectedCategory.getDefaultInterestCompoundingFrequency()));
                    mConstraintLayoutAttributes.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // Initialize account fields
        switch(intent.getIntExtra(EXTRA_ACCOUNT_REQUEST_CODE,0))
        {
            case AccountActivity.REQUEST_CODE_ADD_ACCOUNT:
                setTitle("Add Account");
                break;
            case AccountActivity.REQUEST_CODE_EDIT_ACCOUNT:
                setTitle("Edit Account");
                mUUIDAccountId = (UUID) intent.getSerializableExtra(AccountActivity.EXTRA_ACCOUNT_ID);
                populateAccountFieldsFromId();
                mConstraintLayoutAttributes.setVisibility(View.VISIBLE);
                break;
        }

        // Button handlers
        FloatingActionButton fabAdd = findViewById(R.id.add_account_add);
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!validateAccountFields())
                {
                    // do not close
                }
                else
                {
                    Intent intentAddAcc = new Intent();
                    int liabilityMultiplier = 1;
                    if(mIsLiability)
                    {
                        liabilityMultiplier = -1;
                    }
                    intentAddAcc.putExtra(EXTRA_ADD_ACC_NAME, mEditTextAccountName.getText().toString());
                    intentAddAcc.putExtra(EXTRA_ADD_ACC_CATEGORY, ((AccountCategory) mSpinnerAccountCategory.getSelectedItem()).getAccountCategoryId());
                    intentAddAcc.putExtra(EXTRA_ADD_ACC_IS_ASSET, mIsAsset);
                    intentAddAcc.putExtra(EXTRA_ADD_ACC_IS_LIABILITY, mIsLiability);
                    intentAddAcc.putExtra(EXTRA_ADD_ACC_STARTING_BALANCE, Double.parseDouble("0"+mEditTextStartingBalance.getText().toString().replaceAll("[^0-9-.]", ""))*liabilityMultiplier);
                    intentAddAcc.putExtra(EXTRA_ADD_ACC_INTEREST_RATE, Double.parseDouble("0"+mEditTextInterestRate.getText().toString())/100);
                    intentAddAcc.putExtra(EXTRA_ADD_ACC_INTEREST_RATE_TYPE, (InterestRateType) mSpinnerInterestRateType.getSelectedItem());
                    intentAddAcc.putExtra(EXTRA_ADD_ACC_INTEREST_RATE_FREQUENCY, ((Frequency) mSpinnerInterestRateFrequency.getSelectedItem()).getPeriod());
                    setResult(RESULT_OK, intentAddAcc);
                    finish();
                }
            }
        });

        FloatingActionButton fabCancel = findViewById(R.id.add_account_cancel);
        fabCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId())
        {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void populateAccountFieldsFromId()
    {
        int liabilityMultiplier = 1;

        mAccount = mAccountManager.GetAccount(mUUIDAccountId);
        mIsAsset = mAccount.isAsset();
        mIsLiability = mAccount.isLiability();

        if(mIsLiability)
        {
            liabilityMultiplier = -1;
        }

        mEditTextAccountName.setText(mAccount.getAccountName());
        mSpinnerAccountCategory.setSelection( ((ArrayAdapter<AccountCategory>) mSpinnerAccountCategory.getAdapter()).getPosition(AccountCategory.getAccountCategory(mAccount.getAccountCategoryId())));
        mEditTextStartingBalance.setText(String.format("%.2f", mAccount.getStartingBalance()*liabilityMultiplier));
        mEditTextInterestRate.setText(String.format("%.3f", mAccount.getInterestRate()*100));
        mSpinnerInterestRateType.setSelection( ((ArrayAdapter<InterestRateType>) mSpinnerInterestRateType.getAdapter()).getPosition(mAccount.getInterestRateType()) );
        mSpinnerInterestRateFrequency.setSelection( ((ArrayAdapter<Frequency>) mSpinnerInterestRateFrequency.getAdapter()).getPosition(Frequency.getFrequency(mAccount.getCompoundingPeriod()) ));

    }

    private boolean validateAccountFields()
    {
        String errorMessage = "";

        if(mEditTextAccountName.getText().toString().equals(""))
        {
            errorMessage = "Enter an account name";
            mEditTextAccountName.requestFocus();
        }
        else if (mSpinnerAccountCategory.getSelectedItemPosition() == 0)
        {
            errorMessage = "Select account category";
            mSpinnerAccountCategory.requestFocus();
        }
        else if (mEditTextStartingBalance.getText().toString().equals(""))
        {
            errorMessage = "Enter the starting balance";
            mEditTextStartingBalance.requestFocus();
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
