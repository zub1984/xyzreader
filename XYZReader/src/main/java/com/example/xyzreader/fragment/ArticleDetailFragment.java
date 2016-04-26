package com.example.xyzreader.fragment;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.xyzreader.Activity.ArticleDetailActivity;
import com.example.xyzreader.Activity.ArticleListActivity;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.squareup.picasso.Picasso;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetailFragment";
    public static final String ARG_ITEM_ID = "item_id";
    private Cursor mCursor;
    private long mItemId;


    private View mRootView;
    @Bind(R.id.article_body)
    TextView mBodyView;
    @Bind(R.id.article_title)
    TextView mTitleView;
    @Bind(R.id.article_byline)
    TextView mBylineView;
    @Bind(R.id.photo)
    ImageView mPhotoView;
    @Bind(R.id.meta_bar)
    LinearLayout mMetaBar;

    @Bind(R.id.detail_toolbar)
    Toolbar mToolbar;
    @Bind(R.id.detail_collapsing_toolbar)
    CollapsingToolbarLayout mCollapsingToolbar;
    @Bind(R.id.share_fab)
    FloatingActionButton mFab;

    private int mMutedColor = 0xFF333333;
    private int mThemeDark;
    private int mThemePrimary;

    private static final String ARG_ITEM_IMAGE_POSITION = "arg_item_image_position";
    private static final String ARG_STARTING_ITEM_IMAGE_POSITION = "arg_starting_item_image_position";
    private int mStartingPosition;
    private int mArticlePosition;
    private boolean mIsTransitioning;
    private long mBackgroundImageFadeMillis;

    /*private final Callback mImageCallback = new Callback() {
        @Override
        public void onSuccess() {
            startPostponedEnterTransition();
        }

        @Override
        public void onError() {
            startPostponedEnterTransition();
        }
    };*/

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId,int position, int startingPosition) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);

        arguments.putInt(ARG_ITEM_IMAGE_POSITION, position);
        arguments.putInt(ARG_STARTING_ITEM_IMAGE_POSITION, startingPosition);

        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
            mStartingPosition = getArguments().getInt(ARG_STARTING_ITEM_IMAGE_POSITION);
            mArticlePosition = getArguments().getInt(ARG_ITEM_IMAGE_POSITION);
            mIsTransitioning = savedInstanceState == null && mStartingPosition == mArticlePosition;
            mBackgroundImageFadeMillis = getResources().getInteger(R.integer.fragment_details_background_image_fade_millis);
        }

        mThemePrimary = ContextCompat.getColor(getActivity(), R.color.theme_primary);
        mThemeDark = ContextCompat.getColor(getActivity(), R.color.theme_primary_dark);
        setHasOptionsMenu(true);
    }

    public ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);
        ButterKnife.bind(this, mRootView);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
                getActivityCast().supportFinishAfterTransition();
            }
        });

        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText("sample text")
                        .getIntent(), getString(R.string.action_share)));
            }
        });
        bindViews();
        return mRootView;
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //TODO - to check about using the shared element name with itemId
            //mTitleView.setTransitionName(getActivity().getString(R.string.shared_element_transition) + mItemId);
            mPhotoView.setTransitionName(String.valueOf(mArticlePosition));
        }

        mBylineView.setMovementMethod(new LinkMovementMethod());
        mBodyView.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "Rosario-Regular.ttf"));

        if (mCursor != null) {
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);

            mTitleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            mBylineView.setText(Html.fromHtml(
                    DateUtils.getRelativeTimeSpanString(
                            mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by <font color='#ffffff'>"
                            + mCursor.getString(ArticleLoader.Query.AUTHOR)
                            + "</font>"));

            mBodyView.setText(Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY)));

            String image_url = mCursor.getString(ArticleLoader.Query.PHOTO_URL);
            BitmapLoaderTarget bitmapLoaderTarget = new BitmapLoaderTarget();

            // use Picasso library
            Picasso.with(getActivity())
                    .load(image_url)
                    .placeholder(R.drawable.empty_detail)
                    .error(R.drawable.empty_detail)
                    .into(bitmapLoaderTarget);

          /*  ImageLoaderHelper.getInstance(getActivity()).getImageLoader()
                    .get(mCursor.getString(ArticleLoader.Query.PHOTO_URL), new ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                            Bitmap bitmap = imageContainer.getBitmap();
                            if (bitmap != null && !bitmap.isRecycled()) {
                                mPhotoView.setImageBitmap(imageContainer.getBitmap());
                                Palette.from(bitmap).generate(paletteListener);
                            }
                        }

                        @Override
                        public void onErrorResponse(VolleyError volleyError) {

                        }
                    });*/

            mCollapsingToolbar.setTitle(mCursor.getString(ArticleLoader.Query.TITLE));
            mCollapsingToolbar.setExpandedTitleColor(
                    ActivityCompat.getColor(getActivity(), android.R.color.transparent));

        } else {
            mRootView.setVisibility(View.GONE);
            mTitleView.setText("N/A");
            mBylineView.setText("N/A");
            mBodyView.setText("N/A");
        }
    }


    public class BitmapLoaderTarget implements com.squareup.picasso.Target{
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            Palette.from(bitmap).generate(paletteListener);
            mPhotoView.setImageBitmap(bitmap);
            startPostponedEnterTransition();
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {
            startPostponedEnterTransition();
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
            startPostponedEnterTransition();
        }
    }


    Palette.PaletteAsyncListener paletteListener = new Palette.PaletteAsyncListener() {
        public void onGenerated(Palette palette) {
            applyPalette(palette);
        }
    };

    private void applyPalette(Palette palette) {
        mCollapsingToolbar.setStatusBarScrimColor(palette.getDarkMutedColor(mThemeDark));
        mCollapsingToolbar.setContentScrimColor(palette.getMutedColor(mThemePrimary));
        mMetaBar.setBackgroundColor(palette.getDarkMutedColor(mMutedColor));

        if (isAdded()) {
            mFab.setRippleColor(
                    palette.getLightVibrantColor(
                            ContextCompat.getColor(getActivity(), R.color.ltgray)));

            mFab.setBackgroundTintList(
                    ColorStateList.valueOf(
                            palette.getVibrantColor(
                                    ContextCompat.getColor(getActivity(), R.color.theme_accent))));

            mBodyView.setLinkTextColor(palette.getVibrantColor(
                    ContextCompat.getColor(getActivity(), R.color.theme_accent)));
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }
        bindViews();
        scheduleStartPostponedEnterTransition(mTitleView);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void scheduleStartPostponedEnterTransition(final View sElement){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            sElement.getViewTreeObserver().addOnPreDrawListener(
                    new ViewTreeObserver.OnPreDrawListener() {
                        @Override
                        public boolean onPreDraw() {
                            sElement.getViewTreeObserver().removeOnPreDrawListener(this);
                            getActivity().startPostponedEnterTransition();
                            return true;
                        }
                    });
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void startPostponedEnterTransition() {
        if (mArticlePosition == mStartingPosition) {
            mPhotoView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    mPhotoView.getViewTreeObserver().removeOnPreDrawListener(this);
                    getActivity().startPostponedEnterTransition();
                    return true;
                }
            });
        }
    }

    /**
     * Returns the shared element that should be transitioned back to the previous Activity,
     * or null if the view is not visible on the screen.
     */
    @Nullable
    public ImageView getAlbumImage() {
        if (isViewInBounds(getActivity().getWindow().getDecorView(), mPhotoView)) {
            return mPhotoView;
        }
        return null;
    }

    /**
     * Returns true if {@param view} is contained within {@param container}'s bounds.
     */
    private static boolean isViewInBounds(@NonNull View container, @NonNull View view) {
        Rect containerBounds = new Rect();
        container.getHitRect(containerBounds);
        return view.getLocalVisibleRect(containerBounds);
    }

  }
