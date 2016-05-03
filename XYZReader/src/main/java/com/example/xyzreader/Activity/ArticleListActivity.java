package com.example.xyzreader.Activity;

import android.annotation.TargetApi;
import android.app.LoaderManager;
import android.app.SharedElementCallback;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.xyzreader.Adapter.ArticleItemsAdapter;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.UpdaterService;
import com.example.xyzreader.ui.SpaceItemDecoration;
import com.example.xyzreader.utils.Constants;
import com.example.xyzreader.utils.NetworkUtils;

import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.BindDimen;
import butterknife.BindInt;
import butterknife.ButterKnife;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 * <p>
 * Many thanks to alexjlockwood for shared transition example
 * https://github.com/alexjlockwood/activity-transitions
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>,
        AppBarLayout.OnOffsetChangedListener,
        ArticleItemsAdapter.AdapterItemListener {


    @Bind(R.id.toolbar)
    Toolbar mToolbar;
    @Bind(R.id.main_appbar)
    AppBarLayout mAppBarLayout;
    @Bind(R.id.main_logo)
    ImageView mLogo;
    @Bind(R.id.activity_list_container)
    CoordinatorLayout mCoordinatorLayout;
    @Bind(R.id.swipe_refresh_layout)
    SwipeRefreshLayout mSwipeRefreshLayout;
    @Bind(R.id.recycler_view)
    RecyclerView mRecyclerView;
    @BindInt(R.integer.list_column_count)
    int mColumnCount;
    @BindDimen(R.dimen.normal_space)
    int mItemsViewSpace;

    @Nullable
    @Bind(android.support.design.R.id.snackbar_text)
    TextView snackBarText;

    private boolean mIsAppStart;
    private boolean mLogoShown;

    private View.OnClickListener mSnackBarOnClickListener;
    private Snackbar mSnackBar;
    private int mMaxAppBarScrollRange;
    ArticleItemsAdapter mAdapter = null;
    private boolean mIsRefreshing = false;


    private Bundle mTmpReenterState;
    private SharedElementCallback mCallback;
    private boolean mIsDetailsActivityStarted;


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void SharedElementCallbackTransition() {
        mCallback = new SharedElementCallback() {
            @Override
            public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                if (mTmpReenterState != null) {
                    int startingPosition = mTmpReenterState.getInt(Constants.EXTRA_STARTING_ITEM_POSITION);
                    int currentPosition = mTmpReenterState.getInt(Constants.EXTRA_CURRENT_ITEM_POSITION);
                    if (startingPosition != currentPosition) {
                        // If startingPosition != currentPosition the user must have swiped to a
                        // different page in the DetailsActivity. We must update the shared element
                        // so that the correct one falls into place.
                        String newTransitionName = String.valueOf(currentPosition);
                        View newSharedElement = mRecyclerView.findViewWithTag(newTransitionName);
                        if (newSharedElement != null) {
                            names.clear();
                            names.add(newTransitionName);
                            sharedElements.clear();
                            sharedElements.put(newTransitionName, newSharedElement);
                        }
                    }
                    mTmpReenterState = null;
                } else {
                    // If mTmpReenterState is null, then the activity is exiting.
                    View navigationBar = findViewById(android.R.id.navigationBarBackground);
                    View statusBar = findViewById(android.R.id.statusBarBackground);
                    if (navigationBar != null) {
                        names.add(navigationBar.getTransitionName());
                        sharedElements.put(navigationBar.getTransitionName(), navigationBar);
                    }
                    if (statusBar != null) {
                        names.add(statusBar.getTransitionName());
                        sharedElements.put(statusBar.getTransitionName(), statusBar);
                    }
                }
            }
        };
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);
        ButterKnife.bind(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            SharedElementCallbackTransition();
            setExitSharedElementCallback(mCallback);
        }

        mIsAppStart = true;

        initLogo();
        initAppbarLayout();
        initSwipeRefresh();
        setSnackBarListener();

        StaggeredGridLayoutManager sglm = new StaggeredGridLayoutManager(mColumnCount, StaggeredGridLayoutManager.VERTICAL);
        SpaceItemDecoration spaceItemDecoration = new SpaceItemDecoration(mItemsViewSpace);

        mAdapter = new ArticleItemsAdapter(this, this);
        mAdapter.setHasStableIds(true);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(sglm);
        mRecyclerView.addItemDecoration(spaceItemDecoration);

        getLoaderManager().initLoader(0, null, this);

        if (savedInstanceState == null) {
            refresh();
        }
    }


    private void initLogo() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Animation animation = AnimationUtils.loadAnimation(this, R.anim.slide_down_logo);
            mLogo.setAnimation(animation);
        }
    }

    private void initAppbarLayout() {
        //mAppBarLayout = (AppBarLayout) findViewById(R.id.main_appbar);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mMaxAppBarScrollRange = mAppBarLayout.getTotalScrollRange();
            mAppBarLayout.addOnOffsetChangedListener(this);
        }
    }

    private void initSwipeRefresh() {
        mSwipeRefreshLayout.setColorSchemeColors(getResources().getIntArray(R.array.progress_colors));
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });
    }

    private void setSnackBarListener() {
        mSnackBarOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refresh();
            }
        };
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

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onActivityReenter(int requestCode, Intent data) {
        super.onActivityReenter(requestCode, data);
        mTmpReenterState = new Bundle(data.getExtras());
        int startingPosition = mTmpReenterState.getInt(Constants.EXTRA_STARTING_ITEM_POSITION);
        int currentPosition = mTmpReenterState.getInt(Constants.EXTRA_CURRENT_ITEM_POSITION);
        if (startingPosition != currentPosition) {
            mRecyclerView.scrollToPosition(currentPosition);
        }
        postponeEnterTransition();
        mRecyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mRecyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                // TODO: figure out why it is necessary to request layout here in order to get a smooth transition.
                mRecyclerView.requestLayout();
                startPostponedEnterTransition();
                return true;
            }
        });
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
    protected void onResume() {
        super.onResume();
        mIsDetailsActivityStarted = false;
    }


    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

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
        mAdapter.swapCursor(cursor);
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

                if (percentage >= Constants.PERCENT_TO_ANIMATE_LOGO && mLogoShown) {
                    mLogoShown = false;
                    mLogo.animate().scaleX(0).scaleY(0).setDuration(200).start();
                }

                if (percentage < Constants.PERCENT_TO_ANIMATE_LOGO && !mLogoShown) {
                    mLogoShown = true;
                    mLogo.animate().scaleX(1).scaleY(1).setDuration(200).start();
                }
            }
        }
    }


    @Override
    public void onItemClick(Uri uri, int position, View view) {
        ActivityOptionsCompat options = null;
        if (mIsRefreshing) {
            Toast.makeText(ArticleListActivity.this,
                    R.string.still_loading, Toast.LENGTH_SHORT).show();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            options = ActivityOptionsCompat.makeSceneTransitionAnimation(this, view, view.getTransitionName());

        }

        if (!mIsDetailsActivityStarted) {
            mIsDetailsActivityStarted = true;
            Intent intent = new Intent(ArticleListActivity.this, ArticleDetailActivity.class)
                    .putExtra(Constants.EXTRA_STARTING_ITEM_POSITION, position);
            ActivityCompat.startActivity(ArticleListActivity.this, intent, options == null ? null : options.toBundle());
        }
    }


    /* SnackBar showing no connectivity */
    private void showSnackBar() {
        mSnackBar = Snackbar
                .make(mCoordinatorLayout, R.string.no_internet, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.retry, mSnackBarOnClickListener);

        //http://stackoverflow.com/questions/31842983/getresources-getcolor-is-deprecated
        mSnackBar.setActionTextColor(ContextCompat.getColor(this, R.color.theme_accent));
        View v = mSnackBar.getView();
        v.setBackgroundColor(Color.DKGRAY);
        snackBarText.setTextColor(Color.WHITE);
        mSnackBar.show();
    }

    private void hideSnackBar() {
        if (null != mSnackBar) {
            mSnackBar.dismiss();
        }
    }
}
