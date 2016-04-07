package com.example.xyzreader.ui;

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.UpdaterService;
import com.example.xyzreader.utils.Constants;
import com.example.xyzreader.utils.NetworkUtils;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>, AppBarLayout.OnOffsetChangedListener {

    private boolean mIsAppStart;
    private ImageView mLogo;
    private boolean mLogoShown;
    private static int PERCENT_TO_ANIMATE_LOGO = 20;

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;

    private View.OnClickListener mSnackBarOnClickListener;
    private Snackbar mSnackBar;
    private AppBarLayout mAppBarLayout;
    private int mMaxAppBarScrollRange;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);

        mIsAppStart = true;

        initLogo();
        initAppbarLayout();
        initSwipeRefresh();
        initRecyclerView();
        setSnackBarListener();
        initLoader();

        if (savedInstanceState == null) {
            refresh();
        }
    }


    private void initLogo() {
        mLogo = (ImageView) findViewById(R.id.main_logo);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Animation animation = AnimationUtils.loadAnimation(this, R.anim.slide_down_logo);
            mLogo.setAnimation(animation);
        }
    }

    private void initAppbarLayout() {
        mAppBarLayout = (AppBarLayout) findViewById(R.id.main_appbar);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mMaxAppBarScrollRange = mAppBarLayout.getTotalScrollRange();
            mAppBarLayout.addOnOffsetChangedListener(this);
        }
    }

    private void initSwipeRefresh() {
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });
    }


    private void initRecyclerView() {
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
    }

    private void setSnackBarListener() {
        mSnackBarOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refresh();
            }
        };
    }

    private void initLoader() {
        getLoaderManager().initLoader(0, null, this);
    }


    @Override
    public void onEnterAnimationComplete() {
        mIsAppStart = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (mIsAppStart) {
                mRecyclerView.scheduleLayoutAnimation();
            }
        }
        super.onEnterAnimationComplete();
    }

    /* This method shows SnackBar if no connectivity found, or launch the update
     * the update service otherwise
     * */
    private void refresh() {
        if (!NetworkUtils.isNetworkAvailable(getApplicationContext())) {
            if (mSwipeRefreshLayout.isRefreshing()) {
                mSwipeRefreshLayout.setRefreshing(false);
            }
            showSnackBar();
        } else {
            hideSnackBar();
            startService(new Intent(this, UpdaterService.class));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    private boolean mIsRefreshing = false;

    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                updateRefreshingUI();
            }
        }
    };

    private void updateRefreshingUI() {
        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Adapter adapter = new Adapter(cursor);
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
        int columnCount = getResources().getInteger(R.integer.list_column_count);
        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(sglm);
    }


    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (!mIsAppStart) {
                if (mMaxAppBarScrollRange == 0)
                    mMaxAppBarScrollRange = appBarLayout.getTotalScrollRange();

                int percentage = (Math.abs(verticalOffset) * 100) / mMaxAppBarScrollRange;

                if (percentage >= PERCENT_TO_ANIMATE_LOGO && mLogoShown) {
                    mLogoShown = false;
                    mLogo.animate().scaleX(0).scaleY(0).setDuration(200).start();
                }

                if (percentage < PERCENT_TO_ANIMATE_LOGO && !mLogoShown) {
                    mLogoShown = true;
                    mLogo.animate().scaleX(1).scaleY(1).setDuration(200).start();
                }
            }
        }
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private Cursor mCursor;

        public Adapter(Cursor cursor) {
            mCursor = cursor;
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(ArticleLoader.Query._ID);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            //Inflate a layout that will be used to the RecyclerView UI
            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            final ViewHolder vh = new ViewHolder(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mIsRefreshing) {
                        Toast.makeText(ArticleListActivity.this,
                                R.string.till_loading, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Intent intent = new Intent(ArticleListActivity.this, ArticleDetailActivity.class)
                            .putExtra(Constants.SELECTED_ITEM_POSITION, vh.getAdapterPosition());

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        // Add transition
                        TextView title = (TextView) view.findViewById(R.id.article_title);
                        String transitionName = getString(R.string.shared_element_transition)
                                + String.valueOf(getItemId(vh.getAdapterPosition()));
                        title.setTransitionName(transitionName);

                        Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                                ArticleListActivity.this,
                                title,
                                transitionName).toBundle();
                        ActivityCompat.startActivity(ArticleListActivity.this, intent, bundle);
                    } else {
                        startActivity(intent);
                    }
                }
            });
            return vh;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            mCursor.moveToPosition(position);
            holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            holder.subtitleView.setText(
                    DateUtils.getRelativeTimeSpanString(
                            mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by "
                            + mCursor.getString(ArticleLoader.Query.AUTHOR));
            holder.thumbnailView.setImageUrl(
                    mCursor.getString(ArticleLoader.Query.THUMB_URL),
                    ImageLoaderHelper.getInstance(ArticleListActivity.this).getImageLoader());
            holder.thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));
        }

        @Override
        public int getItemCount() {
            return mCursor.getCount();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public DynamicHeightNetworkImageView thumbnailView;
        public TextView titleView;
        public TextView subtitleView;

        public ViewHolder(View view) {
            super(view);
            thumbnailView = (DynamicHeightNetworkImageView) view.findViewById(R.id.thumbnail);
            titleView = (TextView) view.findViewById(R.id.article_title);
            subtitleView = (TextView) view.findViewById(R.id.article_subtitle);
        }
    }


    /* SnackBar showing no connectivity */
    private void showSnackBar() {
        mSnackBar = Snackbar
                .make(findViewById(R.id.activity_list_container), R.string.no_internet, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.retry, mSnackBarOnClickListener);
        mSnackBar.setActionTextColor(getResources().getColor(R.color.theme_accent));
        View v = mSnackBar.getView();
        v.setBackgroundColor(Color.DKGRAY);
        TextView textView = (TextView) v.findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextColor(Color.WHITE);
        mSnackBar.show();
    }

    private void hideSnackBar() {
        if (null != mSnackBar) {
            mSnackBar.dismiss();
        }
    }
}
