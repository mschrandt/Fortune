package com.android.mschrandt.fortune.activities;

import android.app.Activity;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
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
import com.android.mschrandt.fortune.model.AccountCategory;
import com.android.mschrandt.fortune.model.AccountManager;
import com.android.mschrandt.fortune.data.AccountManagerDatabase;
import com.android.mschrandt.fortune.utils.DateCurrencyAxisLabelFormatter;
import com.android.mschrandt.fortune.model.InterestRateType;
import com.android.mschrandt.fortune.R;
import com.android.mschrandt.fortune.model.Transaction;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.UUID;

public class SummaryActivity extends AppCompatActivity {

    // Activity Constants
    public static final int REQUEST_CODE_VIEW = 2;
    public static final int REQUEST_CODE_SET_DATE = 3;
    public static final String EXTRA_SUM_ACCOUNT_ID = "EXTRA_SUM_ACCOUNT_ID";
    private final int GRAPH_LABEL_WIDTH = 160;

    // Data model
    private AccountManagerViewModel mModel;
    private AccountManager mAccountManager;

    // Layouts
    private ConstraintLayout mConstraintLayoutSummary;
    private ConstraintLayout mConstraintLayoutNoAccounts;
    private RecyclerView mAccSummaryRecyclerView;
    private LinearLayoutManager mAccSummaryLayoutManager;
    private RecyclerView.Adapter mAccSummaryAdapter;
    private GraphView mSummaryGraph;
    private LineGraphSeries mSummaryGraphSeries;
    private LinearLayout mLinearLayoutProjectionText;
    private TextView mTextViewProjectionDate;
    private TextView mTextViewProjectionAmount;
    private TextView mTextViewRefLine;
    private float mLogicalDensity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);

        mAccountManager = AccountManager.getInstance();
        mAccountManager.setForecastDate(LocalDate.now().plusYears(10));
        mModel = ViewModelProviders.of(this).get(AccountManagerViewModel.class);
        mModel.init();

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mLogicalDensity = metrics.density;

        mConstraintLayoutSummary = findViewById(R.id.summary_layout);
        mConstraintLayoutNoAccounts = findViewById(R.id.layout_no_account);
        mConstraintLayoutNoAccounts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAddAccountActivity();
            }
        });

        mAccSummaryRecyclerView = findViewById(R.id.accountsRecyclerView);
        mAccSummaryLayoutManager = new LinearLayoutManager(this);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mAccSummaryRecyclerView.getContext(),
                mAccSummaryLayoutManager.getOrientation());
        mAccSummaryRecyclerView.addItemDecoration(dividerItemDecoration);

        mAccSummaryRecyclerView.setLayoutManager(mAccSummaryLayoutManager);
        mAccSummaryAdapter = new AccountSummaryAdapter(mAccountManager.GetAccounts());
        mAccSummaryRecyclerView.setAdapter(mAccSummaryAdapter);
        mSummaryGraph = findViewById(R.id.summary_graph);
        mSummaryGraphSeries = new LineGraphSeries(){
            @Override
            protected DataPointInterface findDataPoint(float x, float y) {
                return findDataPointAtX(x);
            }
        };

        mSummaryGraphSeries.setDrawBackground(true);
        mLinearLayoutProjectionText = findViewById(R.id.projection_text);
        mTextViewRefLine = findViewById(R.id.ref_line);
        mTextViewProjectionDate = findViewById(R.id.projection_date);
        mTextViewProjectionAmount = findViewById(R.id.projection_amount);

        mModel.getAccountManager().observe(this, new Observer<AccountManager>() {
            @Override
            public void onChanged(@Nullable AccountManager accountManager) {
                if(accountManager != null)
                {
                    mAccountManager = accountManager;
                }

                mAccSummaryAdapter = new AccountSummaryAdapter(mAccountManager.GetAccounts());
                mAccSummaryRecyclerView.setAdapter(mAccSummaryAdapter);
                initializeSummaryGraph();
                updateSummaryGraph();
                showOrHideAccounts();
            }
        });

        initializeSummaryGraph();
        showOrHideAccounts();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_summary, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_add_account:
                startAddAccountActivity();
                return true;
            case R.id.menu_item_update_forecast:
                Intent intentSetForecastDate = new Intent(this, ForecastDateActivity.class);
                this.startActivityForResult(intentSetForecastDate, REQUEST_CODE_SET_DATE);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode)
        {
            case AccountActivity.REQUEST_CODE_ADD_ACCOUNT:
                switch (resultCode)
                {
                    case RESULT_OK:

                        Account newAccount = new Account(data.getStringExtra(AccountAttributesActivity.EXTRA_ADD_ACC_NAME),
                                                        (UUID) data.getSerializableExtra(AccountAttributesActivity.EXTRA_ADD_ACC_CATEGORY),
                                                        data.getBooleanExtra(AccountAttributesActivity.EXTRA_ADD_ACC_IS_ASSET, false),
                                                        data.getBooleanExtra(AccountAttributesActivity.EXTRA_ADD_ACC_IS_LIABILITY, false),
                                                        data.getDoubleExtra(AccountAttributesActivity.EXTRA_ADD_ACC_STARTING_BALANCE, 0),
                                                        true,
                                                        false,
                                                        data.getDoubleExtra(AccountAttributesActivity.EXTRA_ADD_ACC_INTEREST_RATE, 0),
                                                        (InterestRateType) data.getSerializableExtra(AccountAttributesActivity.EXTRA_ADD_ACC_INTEREST_RATE_TYPE),
                                                        (Period) data.getSerializableExtra(AccountAttributesActivity.EXTRA_ADD_ACC_INTEREST_RATE_FREQUENCY));

                        mAccountManager.AddAccount(newAccount);
                        mAccSummaryAdapter.notifyDataSetChanged();
                        //mAccSummaryAdapter.notifyItemInserted(mAccountManager.GetAccounts().size()-1);

                        updateSummaryGraph();
                        showOrHideAccounts();
                        mModel.updateAccount(newAccount);
                        break;
                }
                break;

            case REQUEST_CODE_VIEW:
                mAccSummaryAdapter.notifyDataSetChanged();
                updateSummaryGraph();
                showOrHideAccounts();
                break;

            case REQUEST_CODE_SET_DATE:
                switch (resultCode)
                {
                    case RESULT_OK:
                        //mAccountManager.setForecastDate((LocalDate) data.getSerializableExtra(ForecastDateActivity.EXTRA_FORECAST_DATE));
                        mModel.updateForecastDate((LocalDate) data.getSerializableExtra(ForecastDateActivity.EXTRA_FORECAST_DATE));
                        mAccSummaryAdapter.notifyDataSetChanged();
                        updateSummaryGraph();
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void initializeSummaryGraph()
    {
        mLinearLayoutProjectionText.setVisibility(View.GONE);
        mTextViewRefLine.setVisibility(View.GONE);
        mSummaryGraph.addSeries(mSummaryGraphSeries);

        mSummaryGraph.getGridLabelRenderer().setLabelFormatter(new DateCurrencyAxisLabelFormatter(SummaryActivity.this));

        mSummaryGraph.getGridLabelRenderer().setNumHorizontalLabels(4);
        mSummaryGraph.getGridLabelRenderer().setNumVerticalLabels(5);
        mSummaryGraph.getGridLabelRenderer().setLabelVerticalWidth(GRAPH_LABEL_WIDTH);

        Calendar calendar = Calendar.getInstance();
        Date d1 = calendar.getTime();
        mSummaryGraph.getViewport().setMinX(d1.getTime());

        calendar.set(mAccountManager.getForecastDate().getYear(),
                mAccountManager.getForecastDate().getMonthValue()-1,
                mAccountManager.getForecastDate().getDayOfYear());
        calendar.add(Calendar.MONTH, -1);
        Date d2 = calendar.getTime();

        mSummaryGraph.getViewport().setMaxX(d2.getTime());
        mSummaryGraph.getViewport().setXAxisBoundsManual(true);

        if(mSummaryGraphSeries.getLowestValueY() == mSummaryGraphSeries.getHighestValueY() &&
                mSummaryGraphSeries.getLowestValueY() != 0)
        {
            mSummaryGraph.getViewport().setYAxisBoundsManual(true);
            mSummaryGraph.getViewport().setMinY(mSummaryGraphSeries.getLowestValueY()-1);
            mSummaryGraph.getViewport().setMaxY(mSummaryGraphSeries.getLowestValueY()+1);
        }
        else
        {
            mSummaryGraph.getViewport().setYAxisBoundsManual(false);
        }

        mSummaryGraph.getGridLabelRenderer().setHumanRounding(false,true);

        mSummaryGraphSeries.setOnDataPointTapListener(new OnDataPointTapListener(){
            @Override
            public void onTap(Series series, DataPointInterface dataPoint) {
                Calendar calendar = Calendar.getInstance();
                SimpleDateFormat dateFormatter = new SimpleDateFormat("MM/dd/yy");
                calendar.setTimeInMillis((long) dataPoint.getX());
                mTextViewProjectionDate.setText(dateFormatter.format(calendar.getTime()));
                mTextViewProjectionAmount.setText(NumberFormat.getCurrencyInstance().format(dataPoint.getY()));
                mLinearLayoutProjectionText.setVisibility(View.VISIBLE);
                mTextViewRefLine.setVisibility(View.VISIBLE);
            }
        });
    }

    private void updateSummaryGraph()
    {
        Calendar calendar = Calendar.getInstance();
        Date d1 = calendar.getTime();
        double total = 0;
        mAccountManager.UpdateForecast(mAccountManager.getForecastDate());
        ArrayList<Transaction> allTransactions = new ArrayList<>();
        for(Account a : mAccountManager.GetAccounts())
        {
            int liabilityModifier = 1;
            //if(a.isLiability()){ liabilityModifier = -1;}

            total += liabilityModifier * a.getStartingBalance();

            for(Transaction t : mAccountManager.GetAllTransactionsForAccount(a))
            {
                // Only add transactions that are not transfers between two accounts
                if(!(mAccountManager.GetAccounts().contains(t.getAccountFrom()) &&
                        mAccountManager.GetAccounts().contains(t.getAccountTo())))
                {
                    allTransactions.add(t);
                }
            }
        }

        allTransactions.sort(Transaction.postedDateCompare);

        LinkedList<DataPoint> series = new LinkedList<>();

        series.addLast(new DataPoint(d1, total));

        for (Transaction t : allTransactions)
        {
            int liabilityModifier = 1;

            if(mAccountManager.GetAccounts().contains(t.getAccountFrom()))
            {
                if(t.getAccountFrom().isLiability()){liabilityModifier = -1;}
                total -= liabilityModifier * t.getAmount();
            }
            else {
                if(t.getAccountTo().isLiability()){liabilityModifier = -1;}
                total += liabilityModifier * t.getAmount();
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
            series.addLast(new DataPoint(nextPoint, total));
        }

        // set last point
        calendar.set(mAccountManager.getForecastDate().getYear(),
                mAccountManager.getForecastDate().getMonthValue()-1,
                mAccountManager.getForecastDate().getDayOfYear());
        calendar.add(Calendar.MONTH,-1);
        Date d2 = calendar.getTime();
        if(series.getLast().getX() < d2.getTime())
        {
            series.addLast(new DataPoint(d2.getTime(), series.getLast().getY()));
        }

        mConstraintLayoutSummary.removeView(mSummaryGraph);
        ViewGroup.LayoutParams layoutParams = mSummaryGraph.getLayoutParams();
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(mConstraintLayoutSummary);

        mSummaryGraph = new GraphView(this){
            @Override
            public boolean onTouchEvent(MotionEvent event) {

                // Position projection text
                mLinearLayoutProjectionText.setY(Math.max(0,
                        Math.min(mSummaryGraph.getHeight()-mLinearLayoutProjectionText.getHeight(),
                                event.getY()-(int)(mLinearLayoutProjectionText.getHeight()*2))));
                mLinearLayoutProjectionText.setX(Math.max(0,
                        Math.min(mConstraintLayoutSummary.getWidth()-mLinearLayoutProjectionText.getWidth(),
                                event.getX()-mLinearLayoutProjectionText.getWidth()/2)));

                mTextViewRefLine.setX(Math.max(GRAPH_LABEL_WIDTH+14*mLogicalDensity,
                        Math.min(mConstraintLayoutSummary.getWidth()-14*mLogicalDensity,
                                event.getX())));

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
        mSummaryGraph.setId(R.id.summary_graph);
        mConstraintLayoutSummary.addView(mSummaryGraph, layoutParams);
        constraintSet.applyTo(mConstraintLayoutSummary);

        mSummaryGraphSeries.resetData(series.toArray(new DataPoint[]{}));
        initializeSummaryGraph();
    }

    private void showOrHideAccounts()
    {
        if(mAccountManager.GetAccounts().size() == 0)
        {
            mConstraintLayoutNoAccounts.setVisibility(View.VISIBLE);
            mAccSummaryRecyclerView.setVisibility(View.GONE);
        }
        else
        {
            mConstraintLayoutNoAccounts.setVisibility(View.GONE);
            mAccSummaryRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void startAddAccountActivity()
    {
        Intent intentAddAccount = new Intent(this, AccountAttributesActivity.class);
        intentAddAccount.putExtra(AccountAttributesActivity.EXTRA_ACCOUNT_REQUEST_CODE, AccountActivity.REQUEST_CODE_ADD_ACCOUNT);
        this.startActivityForResult(intentAddAccount, AccountActivity.REQUEST_CODE_ADD_ACCOUNT);
    }

    public class AccountSummaryAdapter extends RecyclerView.Adapter{

        private static final int VIEW_CATEGORY = 1;
        private static final int VIEW_ACCOUNT = 2;

        private ArrayList<Account> mDataSet;

        public class AccViewHolder extends RecyclerView.ViewHolder{
            public ConstraintLayout mRowLayout;

            public AccViewHolder(ConstraintLayout tv){
                super(tv);
                mRowLayout = tv;
            }
        }

        private AccountSummaryAdapter(ArrayList<Account> pDataSet)
        {
            mDataSet = pDataSet;
        }

        @Override
        public int getItemViewType(int position) {
            int viewType = VIEW_CATEGORY;
            UUID prevCategory = mDataSet.get(0).getAccountCategoryId();
            for(int i = 1, j = 0; i <= position; i++)
            {
                if(!mDataSet.get(j).getAccountCategoryId().equals(prevCategory))
                {
                    viewType = VIEW_CATEGORY;
                    prevCategory = mDataSet.get(j).getAccountCategoryId();
                }
                else
                {
                    viewType = VIEW_ACCOUNT;
                    j++;
                }
            }
            return viewType;
        }

        private int getAdjustedPosition(int position)
        {
            int adjustedIndex = 0;
            UUID prevCategory = null;
            for(int i = 0, j = 0; i <= position; i++)
            {
                if(!mDataSet.get(j).getAccountCategoryId().equals(prevCategory))
                {
                    adjustedIndex = i - j;
                    prevCategory = mDataSet.get(j).getAccountCategoryId();
                }
                else
                {
                    adjustedIndex = j;
                    j++;
                }
            }

            return adjustedIndex;
        }

        private UUID getNthAccountCategory(int n)
        {
            UUID prevCat = mDataSet.get(0).getAccountCategoryId();
            int cur = 1;
            int i = 0;
            for(; i < n; cur++)
            {
                if(!mDataSet.get(cur).getAccountCategoryId().equals(prevCat))
                {
                    i++;
                    prevCat = mDataSet.get(cur).getAccountCategoryId();
                }
            }

            return prevCat;
        }


        @Override
        public AccViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ConstraintLayout v = null;
            switch(viewType)
            {
                case VIEW_ACCOUNT:
                    v = (ConstraintLayout) LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.layout_account_summary_list_item, parent, false);

                    v.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intentAddAccount = new Intent(getBaseContext(), AccountActivity.class);
                            int itemPosition = getAdjustedPosition(mAccSummaryRecyclerView.getChildLayoutPosition(v));
                            intentAddAccount.putExtra(EXTRA_SUM_ACCOUNT_ID, mDataSet.get(itemPosition).getId());

                            startActivityForResult(intentAddAccount, REQUEST_CODE_VIEW);
                        }
                    });
                    break;
                case VIEW_CATEGORY:
                    v = (ConstraintLayout) LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.layout_summary_category_list_item, parent, false);

                    break;
            }

            AccViewHolder vh = new AccViewHolder(v);
            return vh;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            position = getAdjustedPosition(position);
            AccViewHolder viewHolder;
            switch(holder.getItemViewType())
            {
                case VIEW_ACCOUNT:
                    Account acc = mDataSet.get(position);
                    viewHolder = (AccViewHolder) holder;
                    TextView accName = viewHolder.mRowLayout.findViewById(R.id.accSumName);
                    accName.setText(acc.toString());

                    TextView accBalance = viewHolder.mRowLayout.findViewById(R.id.accSumBalance);
                    accBalance.setText(NumberFormat.getCurrencyInstance().format(acc.getStartingBalance()));

                    TextView accInterestRate = viewHolder.mRowLayout.findViewById(R.id.acc_summary_interest_rate);
                    if(acc.getInterestRate() == 0)
                    {
                        accInterestRate.setText("");
                    }
                    else
                    {
                        accInterestRate.setText(String.format("%,.2f%% interest rate", acc.getInterestRate()*100));
                    }

                    TextView accProjectedBalance = viewHolder.mRowLayout.findViewById(R.id.acc_sum_projected_balance);
                    double projectedBalance = acc.getStartingBalance();
                    int multiplier;
                    for(Transaction t : mAccountManager.GetAllTransactionsForAccount(acc))
                    {

                        if(t.getAccountFrom().equals(acc))
                        {
                            multiplier = -1;
                        }
                        else {
                            multiplier = 1;
                        }
                        projectedBalance += multiplier * t.getAmount();
                    }

                    accProjectedBalance.setText("projected: " + NumberFormat.getCurrencyInstance().format(projectedBalance));
                    break;
                case VIEW_CATEGORY:
                    viewHolder = (AccViewHolder) holder;
                    TextView accountCategory = viewHolder.mRowLayout.findViewById(R.id.account_category);

                    accountCategory.setText(AccountCategory.getAccountCategory(getNthAccountCategory(position)).toString());
                    break;
            }

        }

        @Override
        public int getItemCount() {
            HashSet<UUID> distinctCategories = new HashSet<>();
            for(Account a : mDataSet)
            {
                distinctCategories.add(a.getAccountCategoryId());
            }
            return mDataSet.size() + distinctCategories.size();
        }
    }
}
