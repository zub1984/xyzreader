package com.example.xyzreader.Adapter;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.ui.DynamicHeightNetworkImageView;
import com.example.xyzreader.ui.ImageLoaderHelper;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by laptop on 4/22/2016.
 */
public class ArticleItemsAdapter extends RecyclerView.Adapter<ArticleItemsAdapter.ViewHolder> {
    private Cursor mCursor;
    private Context mContext;
    private AdapterItemListener adapterItemListener;

    public ArticleItemsAdapter(Context context, AdapterItemListener adapterItemListener) {
        this.mContext = context;
        this.adapterItemListener = adapterItemListener;
    }

    @Override
    public long getItemId(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getLong(ArticleLoader.Query._ID);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_article, parent, false);
        final ViewHolder vh = new ViewHolder(view);
        return vh;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        if (mCursor == null) {
            return;
        }
        mCursor.moveToPosition(position);
        holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
        holder.subtitleView.setText
                (
                        DateUtils.getRelativeTimeSpanString(
                                mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL)
                                .toString() + " by " + mCursor.getString(ArticleLoader.Query.AUTHOR)
                );

        holder.thumbnailView.setImageUrl(
                mCursor.getString(ArticleLoader.Query.THUMB_URL),
                ImageLoaderHelper.getInstance(mContext).getImageLoader()
        );

        holder.thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            holder.thumbnailView.setTransitionName(String.valueOf(position));
        }

        holder.thumbnailView.setTag(String.valueOf(position));
        holder.parent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (adapterItemListener != null) {
                    adapterItemListener.onItemClick(ItemsContract.Items.buildItemUri(getItemId(position)), position, holder.thumbnailView);
                }
            }
        });
    }

    public void swapCursor(final Cursor cursor) {
        this.mCursor = cursor;
        this.notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mCursor == null ? 0 : mCursor.getCount();
    }

    public interface AdapterItemListener {
        public void onItemClick(Uri uri, int position, View v);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.thumbnail)
        DynamicHeightNetworkImageView thumbnailView;
        @Bind(R.id.article_title)
        TextView titleView;
        @Bind(R.id.article_subtitle)
        TextView subtitleView;
        View parent;

        public ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            this.parent = view;
        }
    }
}


