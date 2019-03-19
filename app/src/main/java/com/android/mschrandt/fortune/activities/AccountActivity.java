package com.android.mschrandt.fortune.activities;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.mschrandt.fortune.data.AccountManagerViewModel;
import com.android.mschrandt.fortune.model.Account;
import com.android.mschrandt.fortune.model.AccountManager;
import com.android.mschrandt.fortune.utils.DateCurrencyAxisLabelFormatter;
import com.android.mschrandt.fortune.model.Frequency;
import com.android.mschrandt.fortune.model.InterestRateType;
import com.android.mschrandt.fortune.R;
import com.android.mschrandt.fortune.model.Transaction;
import com.android.mschrandt.fortune.model.TransactionCategory;
import com.android.mschrandt.fortune.model.TransactionScheduler;
import com.android.mschrandt.fortune.utils.FortuneDateTimeFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.Series;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Locale;
import java.util.UUID;

public class AccountActivity extends AppCompatActivity {

    // Activity Constants
    public static final int REQUEST_CODE_ADD_TRANSACTION = 1;
    public static final int REQUEST_CODE_EDIT_TRANSACTION = 2;
    public static final int REQUEST_CODE_ADD_ACCOUNT = 5;
    public static final int REQUEST_CODE_EDIT_ACCOUNT = 4;
    public static final int REQUEST_CODE_SET_DATE = 6;
    public static final String EXTRA_ACCOUNT_ID = "EXTRA_ACCOUNT_ID";
    public static final String EXTRA_TRANSACTION_ID = "EXTRA_TRANSACTION_ID";

    // Data model
    private AccountManager mAccountManager;
    private Account mAccount;
    private UUID mAccountId;
    private ArrayList<TransactionScheduler> mAccountTransactions;
    private AccountManagerViewModel mModel;

    // Layouts
    private ConstraintLayout mConstraintLayoutAccount;
    private ConstraintLayout mConstraintLayoutNoTransactions;
    private RecyclerView mTransactionsRecyclerView;
    private LinearLayoutManager mAccountLayoutManager;
    private AccountActivity.AccountAdapter mAccAdapter;
    private GraphView mAccountGraph;
    private LineGraphSeries mAccountGraphSeries;
    private LinearLayout mLinearLayoutProjectionText;
    private TextView mTextViewProjectionDate;
    private TextView mTextViewProjectionAmount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        mAccountManager = AccountManager.getInstance();
        mModel = ViewModelProviders.of(this).get(AccountManagerViewModel.class);
        mModel.init();

        mAccountId = (UUID) getIntent().getSerializableExtra(SummaryActivity.EXTRA_SUM_ACCOUNT_ID);
        mAccount = mAccountManager.GetAccount(mAccountId);

        setTitle(mAccount.getAccountName());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialize Layouts
        mConstraintLayoutAccount = findViewById(R.id.account_layout);
        mConstraintLayoutNoTransactions = findViewById(R.id.layout_no_transaction);
        mConstraintLayoutNoTransactions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAddTransactionActivity();
            }
        });
        mTransactionsRecyclerView = findViewById(R.id.transaction_recycler_view);
        mAccountLayoutManager = new LinearLayoutManager(this);
        mTransactionsRecyclerView.setLayoutManager(mAccountLayoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mTransactionsRecyclerView.getContext(),
                mAccountLayoutManager.getOrientation());
        mTransactionsRecyclerView.addItemDecoration(dividerItemDecoration);

        mAccountTransactions = mAccountManager.GetTransactionSchedulersForAccount(mAccount);
        mAccountTransactions.sort(TransactionScheduler.transactionSchedulerComparator);
        mAccAdapter = new AccountActivity.AccountAdapter(mAccountTransactions);
        mTransactionsRecyclerView.setAdapter(mAccAdapter);
        mAccountGraph = findViewById(R.id.account_graph);
        mAccountGraphSeries = new LineGraphSeries(){
            @Override
            protected DataPointInterface findDataPoint(float x, float y) {
                return findDataPointAtX(x);
            }
        };
        mAccountGraphSeries.setDrawBackground(true);

        mLinearLayoutProjectionText = findViewById(R.id.projection_text);
        mTextViewProjectionDate = findViewById(R.id.projection_date);
        mTextViewProjectionAmount = findViewById(R.id.projection_amount);

        mModel.getAccountManager().observe(this, new Observer<AccountManager>() {
            @Override
            public void onChanged(@Nullable AccountManager accountManager) {
                if(accountManager != null)
                {
                    mAccountManager = accountManager;
                }

                initializeAccountGraph();
                updateSummaryGraph();
                showOrHideTransactions();
            }
        });

        initializeAccountGraph();
        updateSummaryGraph();
        showOrHideTransactions();
    }

    private void initializeAccountGraph()
    {
        mLinearLayoutProjectionText.setVisibility(View.GONE);
        mAccountGraph.addSeries(mAccountGraphSeries);

        mAccountGraph.getGridLabelRenderer().setLabelFormatter(new DateCurrencyAxisLabelFormatter(AccountActivity.this));

        mAccountGraph.getGridLabelRenderer().setNumHorizontalLabels(4);
        mAccountGraph.getGridLabelRenderer().setNumVerticalLabels(5);
        mAccountGraph.getGridLabelRenderer().setLabelVerticalWidth(160);

        Calendar calendar = Calendar.getInstance();
        Date d1 = calendar.getTime();
        mAccountGraph.getViewport().setMinX(d1.getTime());

        calendar.set(mAccountManager.getForecastDate().getYear(),
                mAccountManager.getForecastDate().getMonthValue()-1,
                mAccountManager.getForecastDate().getDayOfYear());
        calendar.add(Calendar.MONTH, -1);
        Date d2 = calendar.getTime();
        mAccountGraph.getViewport().setMaxX(d2.getTime());
        mAccountGraph.getViewport().setXAxisBoundsManual(true);

        if(mAccountGraphSeries.getLowestValueY() == mAccountGraphSeries.getHighestValueY() &&
                mAccountGraphSeries.getLowestValueY() != 0)
        {
            mAccountGraph.getViewport().setYAxisBoundsManual(true);
            mAccountGraph.getViewport().setMinY(mAccountGraphSeries.getLowestValueY()-1);
            mAccountGraph.getViewport().setMaxY(mAccountGraphSeries.getLowestValueY()+1);
        }
        else
        {
            mAccountGraph.getViewport().setYAxisBoundsManual(false);
        }

        mAccountGraph.getGridLabelRenderer().setHumanRounding(false,true);

        mAccountGraphSeries.setOnDataPointTapListener(new OnDataPointTapListener(){
            @Override
            public void onTap(Series series, DataPointInterface dataPoint) {
                Calendar calendar = Calendar.getInstance();
                SimpleDateFormat dateFormatter = new SimpleDateFormat("MM/dd/yy", Locale.getDefault());
                calendar.setTimeInMillis((long) dataPoint.getX());
                mTextViewProjectionDate.setText(dateFormatter.format(calendar.getTime()));
                mTextViewProjectionAmount.setText(NumberFormat.getCurrencyInstance().format(dataPoint.getY()));
                mLinearLayoutProjectionText.setVisibility(View.VISIBLE);
            }
        });
    }

    private void updateSummaryGraph()
    {
        Calendar calendar = Calendar.getInstance();

        Date d1 = calendar.getTime();

        double total = 0;

        mAccountManager.UpdateForecast(mAccountManager.getForecastDate());
        ArrayList<Transaction> allTransactions = new ArrayList<>(mAccountManager.GetAllTransactionsForAccount(mAccount));

        total += mAccount.getStartingBalance();

        allTransactions.sort(Transaction.postedDateCompare);

        LinkedList<DataPoint> series = new LinkedList<>();

        series.addLast(new DataPoint(d1, total));

        for (Transaction t : allTransactions)
        {
            if(t.getAccountFrom().equals(mAccount))
            {
                total -= t.getAmount();
            }
            else
            {
                total += t.getAmount();
            }

            calendar.set(t.getPostedDate().getYear(),
                    t.getPostedDate().getMonthValue()-1,
                    t.getPostedDate().getDayOfMonth());
            Date nextPoint = calendar.getTime();

            calendar.add(Calendar.DATE, -1);
            Date prevPoint = calendar.getTime();
            // Update last point with new total (same x value)
            if(series.getLast().getX() == nextPoint.getTime())
            {
                series.removeLast();
            }
            // Add point on the day before the new point is added
            else if(series.getLast().getX() < prevPoint.getTime())
            {
                DataPoint priorDayPoint = series.getLast();
                series.addLast(new DataPoint(prevPoint.getTime(), priorDayPoint.getY()));
            }
            series.addLast(new DataPoint(calendar.getTime(), total));
        }

        // set last point
        calendar.set(mAccountManager.getForecastDate().getYear(),
                mAccountManager.getForecastDate().getMonthValue()-1,
                mAccountManager.getForecastDate().getDayOfYear());
        calendar.add(Calendar.MONTH, -1);
        Date d2 = calendar.getTime();

        if(series.getLast().getX() < d2.getTime())
        {
            series.addLast(new DataPoint(d2.getTime(), series.getLast().getY()));
        }

        mConstraintLayoutAccount.removeView(mAccountGraph);
        ViewGroup.LayoutParams layoutParams = mAccountGraph.getLayoutParams();
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(mConstraintLayoutAccount);

        mAccountGraph = new GraphView(this){
            @Override
            public boolean onTouchEvent(MotionEvent event) {
                switch(event.getAction())
                {
                    case MotionEvent.ACTION_MOVE:
                        for (Series s : getSeries()) {
                            s.onTap(event.getX(), event.getY());
                        }
                        if (mSecondScale != null) {
                            for (Series s : mSecondScale.getSeries()) {
                                s.onTap(event.getX(), event.getY());
                            }
                        }
                        break;
                    default:
                        return super.onTouchEvent(event);

                }
                return true;
            }};
        mConstraintLayoutAccount.addView(mAccountGraph, layoutParams);
        mAccountGraph.setId(R.id.account_graph);
        constraintSet.applyTo(mConstraintLayoutAccount);

        mAccountGraphSeries.resetData(series.toArray(new DataPoint[]{}));
        initializeAccountGraph();
    }

    private void showOrHideTransactions()
    {
        if(mAccountTransactions.size() == 0)
        {
            mConstraintLayoutNoTransactions.setVisibility(View.VISIBLE);
            mTransactionsRecyclerView.setVisibility(View.GONE);
        }
        else{
            mConstraintLayoutNoTransactions.setVisibility(View.GONE);
            mTransactionsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void startAddTransactionActivity()
    {
        Intent intentAddTransactionScheduler = new Intent(this, TransactionSchedulerAttributesActivity.class);
        intentAddTransactionScheduler.putExtra(TransactionSchedulerAttributesActivity.EXTRA_TRANSACTION_REQUEST_CODE, REQUEST_CODE_ADD_TRANSACTION);
        intentAddTransactionScheduler.putExtra(EXTRA_ACCOUNT_ID, mAccountId);
        this.startActivityForResult(intentAddTransactionScheduler, REQUEST_CODE_ADD_TRANSACTION);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_account, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_add_transaction_scheduler:
                startAddTransactionActivity();
                return true;
            case R.id.menu_item_edit_account:
                Intent intentEditAccount = new Intent(this, AccountAttributesActivity.class);
                intentEditAccount.putExtra(EXTRA_ACCOUNT_ID, mAccountId);
                intentEditAccount.putExtra(AccountAttributesActivity.EXTRA_ACCOUNT_REQUEST_CODE, REQUEST_CODE_EDIT_ACCOUNT);
                this.startActivityForResult(intentEditAccount, REQUEST_CODE_EDIT_ACCOUNT);
                return true;
            case R.id.menu_item_remove_account:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                builder.setMessage("Are you sure you want to remove this account and all associated transactions?")
                        .setPositiveButton("Yes",  new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                for(TransactionScheduler ts : mAccountManager.GetTransactionSchedulersForAccount(mAccount))
                                {
                                    mModel.deleteTransactionScheduler(ts);
                                }
                                mModel.deleteAccount(mAccount);
                                mAccountManager.RemoveScheduledTransactionsForAccount(mAccountId);
                                mAccountManager.RemoveAccount(mAccountId);
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
            case R.id.menu_item_update_forecast:
                Intent intentSetForecastDate = new Intent(this, ForecastDateActivity.class);
                this.startActivityForResult(intentSetForecastDate, REQUEST_CODE_SET_DATE);
                return true;
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode)
        {
            case REQUEST_CODE_ADD_TRANSACTION:
                switch (resultCode)
                {
                    case RESULT_OK:
                        UUID destAccountId = (UUID) data.getSerializableExtra(TransactionSchedulerAttributesActivity.EXTRA_ADD_TRN_DEST_ACC_ID);
                        Account accountFrom, accountTo;

                        if(data.getBooleanExtra(TransactionSchedulerAttributesActivity.EXTRA_ADD_TRN_IN_ACC, false))
                        {
                            accountFrom = mAccountManager.GetAccount(destAccountId);
                            accountTo = mAccount;
                        }
                        else
                        {
                            accountFrom = mAccount;
                            accountTo = mAccountManager.GetAccount(destAccountId);
                        }

                        TransactionScheduler ts = new TransactionScheduler(
                                data.getDoubleExtra(TransactionSchedulerAttributesActivity.EXTRA_ADD_TRN_AMOUNT, 0),
                                (Period) data.getSerializableExtra(TransactionSchedulerAttributesActivity.EXTRA_ADD_TRN_PERIOD),
                                (LocalDate) data.getSerializableExtra(TransactionSchedulerAttributesActivity.EXTRA_ADD_TRN_DATE_FROM),
                                (LocalDate) data.getSerializableExtra(TransactionSchedulerAttributesActivity.EXTRA_ADD_TRN_DATE_TO),
                                accountFrom,
                                accountTo,
                                data.getStringExtra(TransactionSchedulerAttributesActivity.EXTRA_ADD_TRN_DESCRIPTION));

                        mAccountManager.AddScheduledTransaction(ts);
                        mAccountTransactions.add(ts);
                        mAccountTransactions.sort(TransactionScheduler.transactionSchedulerComparator);

                        mAccAdapter.notifyDataSetChanged();
                        mModel.updateTransactionScheduler(ts);
                        updateSummaryGraph();
                        showOrHideTransactions();
                        break;
                }
                break;
            case REQUEST_CODE_EDIT_TRANSACTION:
                switch(resultCode)
                {
                    case RESULT_OK:
                        if(data.getBooleanExtra(TransactionSchedulerAttributesActivity.EXTRA_DELETE_TRANSACTION, false))
                        {
                            mAccountTransactions = mAccountManager.GetTransactionSchedulersForAccount(mAccount);
                            mAccAdapter.mDataSet = mAccountTransactions;
                            mAccAdapter.notifyDataSetChanged();
                            updateSummaryGraph();
                            showOrHideTransactions();
                            break;
                        }
                        UUID destAccountId = (UUID) data.getSerializableExtra(TransactionSchedulerAttributesActivity.EXTRA_ADD_TRN_DEST_ACC_ID);
                        Account accountFrom, accountTo;

                        if(data.getBooleanExtra(TransactionSchedulerAttributesActivity.EXTRA_ADD_TRN_IN_ACC, false))
                        {
                            accountFrom = mAccountManager.GetAccount(destAccountId);
                            accountTo = mAccount;
                        }
                        else
                        {
                            accountFrom = mAccount;
                            accountTo = mAccountManager.GetAccount(destAccountId);
                        }

                        TransactionScheduler ts = mAccountManager.getTransactionScheduler((UUID) data.getSerializableExtra(EXTRA_TRANSACTION_ID));
                        ts.setDescription(data.getStringExtra(TransactionSchedulerAttributesActivity.EXTRA_ADD_TRN_DESCRIPTION));
                        ts.setAmount(data.getDoubleExtra(TransactionSchedulerAttributesActivity.EXTRA_ADD_TRN_AMOUNT,0));
                        ts.setPeriod((Period) data.getSerializableExtra(TransactionSchedulerAttributesActivity.EXTRA_ADD_TRN_PERIOD));
                        ts.setStartDate((LocalDate) data.getSerializableExtra(TransactionSchedulerAttributesActivity.EXTRA_ADD_TRN_DATE_FROM));
                        ts.setEndDate((LocalDate) data.getSerializableExtra(TransactionSchedulerAttributesActivity.EXTRA_ADD_TRN_DATE_TO));
                        ts.setAccountFrom(accountFrom);
                        ts.setAccountTo(accountTo);
                        mAccountTransactions.sort(TransactionScheduler.transactionSchedulerComparator);

                        mAccAdapter.notifyDataSetChanged();
                        mModel.updateTransactionScheduler(ts);
                        updateSummaryGraph();
                        showOrHideTransactions();
                        break;
                }
                break;
            case REQUEST_CODE_EDIT_ACCOUNT:
                switch(resultCode)
                {
                    case RESULT_OK:
                        mAccount.setAccountName(data.getStringExtra(AccountAttributesActivity.EXTRA_ADD_ACC_NAME));
                        mAccount.setAccountCategoryId((UUID) data.getSerializableExtra(AccountAttributesActivity.EXTRA_ADD_ACC_CATEGORY));
                        mAccount.setAsset(data.getBooleanExtra(AccountAttributesActivity.EXTRA_ADD_ACC_IS_ASSET, false));
                        mAccount.setLiability(data.getBooleanExtra(AccountAttributesActivity.EXTRA_ADD_ACC_IS_LIABILITY, false));
                        mAccount.setStartingBalance(data.getDoubleExtra(AccountAttributesActivity.EXTRA_ADD_ACC_STARTING_BALANCE, 0));
                        mAccount.setInterestRate(data.getDoubleExtra(AccountAttributesActivity.EXTRA_ADD_ACC_INTEREST_RATE, 0));
                        mAccount.setInterestRateType((InterestRateType) data.getSerializableExtra(AccountAttributesActivity.EXTRA_ADD_ACC_INTEREST_RATE_TYPE));
                        mAccount.setCompoundingPeriod((Period) data.getSerializableExtra(AccountAttributesActivity.EXTRA_ADD_ACC_INTEREST_RATE_FREQUENCY));
                        setTitle(mAccount.getAccountName());
                        mModel.updateAccount(mAccount);
                        updateSummaryGraph();
                        showOrHideTransactions();
                        break;
                }
                break;
            case REQUEST_CODE_SET_DATE:
                switch (resultCode)
                {
                    case RESULT_OK:
                        mModel.updateForecastDate((LocalDate) data.getSerializableExtra(ForecastDateActivity.EXTRA_FORECAST_DATE));
                        updateSummaryGraph();
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public class AccountAdapter extends RecyclerView.Adapter<AccountActivity.AccountAdapter.TransactionSchedulerViewHolder>{

        private static final int VIEW_CATEGORY = 1;
        private static final int VIEW_TRANSACTION = 2;

        private ArrayList<TransactionScheduler> mDataSet;

        public class TransactionSchedulerViewHolder extends RecyclerView.ViewHolder{
            public ConstraintLayout mRowLayout;

            public TransactionSchedulerViewHolder(ConstraintLayout tv){
                super(tv);
                mRowLayout = tv;
            }
        }

        private AccountAdapter(ArrayList<TransactionScheduler> pDataSet)
        {
            mDataSet = pDataSet;
        }

        @Override
        public int getItemViewType(int position) {
            int viewType = VIEW_CATEGORY;
            TransactionCategory prevCategory = mDataSet.get(0).getTransactionCategory();

            for(int i = 1, j = 0; i <= position; i++)
            {
                if(!mDataSet.get(j).getTransactionCategory().equals(prevCategory))
                {
                    viewType = VIEW_CATEGORY;
                    prevCategory = mDataSet.get(j).getTransactionCategory();
                }
                else
                {
                    viewType = VIEW_TRANSACTION;
                    j++;
                }
            }
            return viewType;
        }

        private int getAdjustedPosition(int position)
        {
            int adjustedIndex = 0;
            TransactionCategory prevCategory = null;
            for(int i = 0, j = 0; i <= position; i++)
            {
                if(!mDataSet.get(j).getTransactionCategory().equals(prevCategory))
                {
                    adjustedIndex = i - j;
                    prevCategory = mDataSet.get(j).getTransactionCategory();
                }
                else
                {
                    adjustedIndex = j;
                    j++;
                }
            }

            return adjustedIndex;
        }

        private TransactionCategory getNthTransactionCategory(int n)
        {
            TransactionCategory prevCat = mDataSet.get(0).getTransactionCategory();
            int cur = 1;
            int i = 0;
            for(; i < n; cur++)
            {
                if(!mDataSet.get(cur).getTransactionCategory().equals(prevCat))
                {
                    i++;
                    prevCat = mDataSet.get(cur).getTransactionCategory();
                }
            }

            return prevCat;
        }

        @Override
        public AccountActivity.AccountAdapter.TransactionSchedulerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ConstraintLayout v = null;
            switch(viewType)
            {
                case VIEW_TRANSACTION:
                    v = (ConstraintLayout) LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.layout_account_transaction_scheduler_list_item, parent, false);

                    v.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intentEditTransaction = new Intent(getBaseContext(), TransactionSchedulerAttributesActivity.class);
                            int itemPosition = mTransactionsRecyclerView.getChildLayoutPosition(v);
                            intentEditTransaction.putExtra(TransactionSchedulerAttributesActivity.EXTRA_TRANSACTION_REQUEST_CODE, REQUEST_CODE_EDIT_TRANSACTION);
                            intentEditTransaction.putExtra(EXTRA_ACCOUNT_ID, mAccountId);
                            intentEditTransaction.putExtra(EXTRA_TRANSACTION_ID, mDataSet.get(getAdjustedPosition(itemPosition)).getTransactionId());

                            startActivityForResult(intentEditTransaction, REQUEST_CODE_EDIT_TRANSACTION);
                        }
                    });
                    break;
                case VIEW_CATEGORY:
                    v = (ConstraintLayout) LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.layout_transaction_category_list_item, parent, false);

                    break;

            }

            AccountActivity.AccountAdapter.TransactionSchedulerViewHolder vh = new AccountActivity.AccountAdapter.TransactionSchedulerViewHolder(v);
            return vh;
        }

        @Override
        public void onBindViewHolder(@NonNull AccountActivity.AccountAdapter.TransactionSchedulerViewHolder holder, int position) {
            position = getAdjustedPosition(position);

            switch(holder.getItemViewType())
            {
                case VIEW_TRANSACTION:
                    TransactionScheduler ts = mDataSet.get(position);
                    String dateRange;
                    String frequency = "";

                    if(ts.getPeriod().equals(Period.ZERO))
                    {
                        dateRange = "On " + ts.getStartDate().format(FortuneDateTimeFormatter.get());
                    }
                    else
                    {
                        dateRange = "From " + ts.getStartDate().format(FortuneDateTimeFormatter.get()) +
                                " to " + ts.getEndDate().format(FortuneDateTimeFormatter.get());
                        for(Frequency f : Frequency.getFrequencyList())
                        {
                            if(f.getPeriod().equals(ts.getPeriod()))
                            {
                                frequency = f.toString();
                            }
                        }
                    }

                    TextView transactionDescription = holder.mRowLayout.findViewById(R.id.transaction_description);
                    transactionDescription.setText(ts.getDescription());

                    TextView transactionDateRange = holder.mRowLayout.findViewById(R.id.transaction_date_range);
                    transactionDateRange.setText(dateRange);

                    TextView transactionFrequency = holder.mRowLayout.findViewById(R.id.transaction_frequency);
                    transactionFrequency.setText(frequency);

                    TextView transactionDetails = holder.mRowLayout.findViewById(R.id.transaction_amount);
                    transactionDetails.setText(NumberFormat.getCurrencyInstance().format(mDataSet.get(position).getAmount()));
                    break;
                case VIEW_CATEGORY:
                    TextView transactionCategory = holder.mRowLayout.findViewById(R.id.transaction_category);
                    transactionCategory.setText(getNthTransactionCategory(position).toString());
                    break;
            }

        }

        @Override
        public int getItemCount() {
            HashSet<TransactionCategory> distinctCategories = new HashSet<>();
            for(TransactionScheduler t : mDataSet)
            {
                distinctCategories.add(t.getTransactionCategory());
            }
            return mDataSet.size() + distinctCategories.size();
        }
    }
}
