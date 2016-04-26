package com.example.xyzreader.Activity;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.app.SharedElementCallback;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.fragment.ArticleDetailFragment;
import com.example.xyzreader.ui.WindowInsetsViewPager;

import java.util.List;
import java.util.Map;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private Cursor mCursor;
    //private int mSelectedItemIdPosition;
    private WindowInsetsViewPager mPager;
    private MyPagerAdapter mPagerAdapter;
    private long mStartId;


    private static final String STATE_CURRENT_PAGE_POSITION = "state_current_page_position";
    private int mCurrentPosition;
    private int mStartingPosition;
    private boolean mIsReturning;
    private SharedElementCallback mCallback;
    private ArticleDetailFragment mCurrentDetailsFragment;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void SharedElementCallbackTransition() {
        mCallback = new SharedElementCallback() {
            @Override
            public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                if (mIsReturning) {
                    ImageView sharedElement = mCurrentDetailsFragment.getAlbumImage();
                    if (sharedElement == null) {
                        // If shared element is null, then it has been scrolled off screen and
                        // no longer visible. In this case we cancel the shared element transition by
                        // removing the shared element from the shared elements map.
                        names.clear();
                        sharedElements.clear();
                    } else if (mStartingPosition != mCurrentPosition) {
                        // If the user has swiped to a different ViewPager page, then we need to
                        // remove the old shared element and replace it with the new shared element
                        // that should be transitioned instead.
                        names.clear();
                        names.add(sharedElement.getTransitionName());
                        sharedElements.clear();
                        sharedElements.put(sharedElement.getTransitionName(), sharedElement);
                    }
                }
            }
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }

        setContentView(R.layout.activity_article_detail);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            postponeEnterTransition();
            SharedElementCallbackTransition();
            setEnterSharedElementCallback(mCallback);
        }

        mStartingPosition = getIntent().getIntExtra(ArticleListActivity.EXTRA_STARTING_ITEM_POSITION, 0);
        if (savedInstanceState == null) {
            mCurrentPosition=mStartingPosition;
            if (getIntent() != null && getIntent().getData() != null) {
                mStartId = ItemsContract.Items.getItemId(getIntent().getData());
            }
        }
        else{
            mCurrentPosition = savedInstanceState.getInt(STATE_CURRENT_PAGE_POSITION);
        }

        getLoaderManager().initLoader(0, null, this);

        mPagerAdapter = new MyPagerAdapter(getFragmentManager());
        mPager = (WindowInsetsViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mPagerAdapter);
        mPager.setPageMargin((int) TypedValue
                .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()));
        mPager.setPageMarginDrawable(new ColorDrawable(0x22000000));
        mPager.setCurrentItem(mCurrentPosition);

        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }

            @Override
            public void onPageSelected(int position) {
                if (mCursor != null) {
                    mCursor.moveToPosition(position);
                    mCurrentPosition = position;
                }
            }
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_CURRENT_PAGE_POSITION, mCurrentPosition);
    }

    @Override
    public void finishAfterTransition() {
        mIsReturning = true;
        Intent data = new Intent();
        data.putExtra(ArticleListActivity.EXTRA_STARTING_ITEM_POSITION, mStartingPosition);
        data.putExtra(ArticleListActivity.EXTRA_CURRENT_ITEM_POSITION, mCurrentPosition);
        setResult(RESULT_OK, data);
        super.finishAfterTransition();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mCursor = cursor;
        mPagerAdapter.notifyDataSetChanged();
        if (mCursor.moveToPosition(mCurrentPosition)) {
            mPager.setCurrentItem(mCurrentPosition, false);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        mPagerAdapter.notifyDataSetChanged();
    }


    private class MyPagerAdapter extends FragmentStatePagerAdapter {
        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            mCurrentDetailsFragment = (ArticleDetailFragment) object;
        }

        @Override
        public Fragment getItem(int position) {
            mCursor.moveToPosition(position);
            return ArticleDetailFragment.newInstance(mCursor.getLong(ArticleLoader.Query._ID),position,position);
        }

        @Override
        public int getCount() {
            return (mCursor != null) ? mCursor.getCount() : 0;
        }
    }
}
