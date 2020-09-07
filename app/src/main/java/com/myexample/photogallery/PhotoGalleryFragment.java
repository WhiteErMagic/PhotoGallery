package com.myexample.photogallery;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.LruCache;
import android.view.Display;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Adapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;

public class PhotoGalleryFragment extends Fragment{
    private FetchItemsTask fetchItemsTask;
    private static final String TAG = "PhotoGalleryFragment";
    private RecyclerView mPhotoRecyclerView;
    private List<GalleryItem> mItems = new ArrayList<>();
    private int mPage = 1;
    private boolean mLoading;
    private int mOrientation;
    private boolean mIsChanged;
    private Intent intent;
    private String mQuery;
    private boolean tap = true;
    private boolean mColChange = false;
    private boolean mCanColChange = false;
    private boolean mPhotoType = true;

    private List<GalleryItem> items;
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;
    private static final long DOUBLE_CLICK_TIME_DELTA = 300;//milliseconds
    long lastClickTime = 0;


    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        intent = new Intent( getActivity(), PhotoGalleryBigItem.class);
        mLoading = true;
        items = new ArrayList<>();
        ParamRecyclerView.reConfigDisplay(getActivity());
        updateItems();

        Handler responseHandler = new Handler();
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setThumbnailDownloadListener(
                new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
                    @Override
                    public void onThumbnailDownloaded(PhotoHolder photoHolder,
                                                      Bitmap bitmap) {
                        Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                        photoHolder.bindDrawable(drawable);
                        photoHolder.setPict(true);
                    }
                }
        );
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        menuInflater.inflate(R.menu.fragment_photo_gallery, menu);

        MenuItem menuItem = menu.findItem(R.id.menu_type);
        if(mPhotoType)
            menuItem.setTitle(R.string.type_video);
        else
            menuItem.setTitle(R.string.type_photo);


        menuItem = menu.findItem(R.id.menu_item_search);
        final SearchView searchView = (SearchView) menuItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                //Log.d(TAG, "QueryTextSubmit: " + s);
                searchView.clearFocus();
                QueryPreferences.setStoredQuery(getActivity(), s);
                mQuery = s;
                items.clear();
                updateItems();
                mPhotoRecyclerView.getAdapter().notifyDataSetChanged();
                return true;
            }
            @Override
            public boolean onQueryTextChange(String s) {
                //Log.d(TAG, "QueryTextChange: " + s);
                if(s.isEmpty())
                    mQuery = null;
                return false;
            }
        });

        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = QueryPreferences.getStoredQuery(getActivity());
                searchView.setQuery(query, false);
            }
        });

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                mQuery = null;
                mItems.clear();
                updateItems();

                return false;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_update:
                updateItems();
                return true;
            case R.id.menu_type:
                if(mPhotoType) {
                    item.setTitle(R.string.type_photo);
                    mPhotoType = false;
                }else {
                    item.setTitle(R.string.type_video);
                    mPhotoType = true;
                }
                changeType();
                return true;
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(), null);
                updateItems();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateItems() {
        fetchItemsTask = new FetchItemsTask();
        fetchItemsTask.execute(mPage);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.quit();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("mOrientation", mOrientation);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        if(savedInstanceState != null) {
            mOrientation = savedInstanceState.getInt("mOrientation");
            if (mOrientation != 0 &&
                    getResources().getConfiguration().orientation != mOrientation)
                mIsChanged = true;
        }

        mOrientation = getResources().getConfiguration().orientation;
        mPhotoRecyclerView = (RecyclerView) v.findViewById(R.id.photo_recycler_view);
        ParamRecyclerView.reConfigDisplay(getActivity());
        if(ParamRecyclerView.getCol(mColChange) > 2)
            mCanColChange = true;

        reConfigRecyclerView(ParamRecyclerView.getCol(mColChange));

        mPhotoRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if( dy >= 0) {
                    GridLayoutManager  lm = (GridLayoutManager) recyclerView.getLayoutManager();
                    int totalItemCount = lm.getItemCount();
                    int lastVisibleItem = lm.findLastVisibleItemPosition();
                    if(lastVisibleItem + 1 >= totalItemCount/2*1.5 && !mLoading){
                        mLoading = true;
                        mPage++;
                        updateItems();
                    }
                }
            }
        });

        if(!isOnline(getActivity())){
            getMess(R.string.load_error);
        }

        setupAdapter();
        return v;
    }

    private void changeType(){
        mItems.clear();
        mThumbnailDownloader.clearQueue();
        //mPhotoRecyclerView.getAdapter().notifyDataSetChanged();
        updateItems();
    }

    private void reConfigRecyclerView(int col){
        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), col));
        mIsChanged = true;
        setupAdapter();
    }

    public static boolean isOnline(Context context)
    {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting())
        {
            return true;
        }
        return false;
    }

    private void getMess(int arg){
        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
        dialog.setTitle(R.string.error_title)
                .setMessage(arg)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                }).show();
    }

    private void setupAdapter() {
        if (isAdded()) {
            if(!isOnline(getActivity()))
                getMess(R.string.load_error);
            else
                if(mPage == 1 || mIsChanged) {
                    mIsChanged = false;
                    mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
                }
        }
    }

    private class FetchItemsTask extends AsyncTask<Integer ,Void,List<GalleryItem>> {

        @Override
        protected List<GalleryItem> doInBackground(Integer... params) {
            return new FlickrFetchr().fetchItems(params[0], items, mQuery, mPhotoType, ParamRecyclerView.getWidth());
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            mItems = items;
            mLoading = false;
            setupAdapter();
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder {
        private ImageView mImageView;
        private String url;
        private boolean isPict;
        PhotoHolder(View itemView) {
            super(itemView);
            mImageView = (ImageView) itemView.findViewById(R.id.item_image_view);
            mImageView.getLayoutParams().height = ParamRecyclerView.getHeight(mColChange);
            mImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    long clickTime = System.currentTimeMillis();
                    if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA){
                        tap = false;
                        if(mCanColChange) {
                            if (!mColChange) {
                                mColChange = true;
                            } else {
                                mColChange = false;
                            }
                            reConfigRecyclerView(ParamRecyclerView.getCol(mColChange));
                        }
                    } else{
                        tap = true;
                    }
                    view.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if(tap){
                                if(isPict) {
                                    intent.putExtra("type", mPhotoType);
                                    intent.putExtra("url", url);
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(getActivity()).toBundle());
                                    } else {
                                        startActivity(intent);
                                    }
                                }
                            }
                        }
                    },DOUBLE_CLICK_TIME_DELTA);
                    lastClickTime = clickTime;
                }
            });
        }

        private void setPict(boolean pict) {
            isPict = pict;
        }

        private void setUrl(String url) {
            this.url = url;
        }

        private void bindDrawable(Drawable drawable) {
            mImageView.setImageDrawable(drawable);
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {
        private List<GalleryItem> mGalleryItems;
        private PhotoAdapter(List<GalleryItem> galleryItems) {
            mGalleryItems = galleryItems;
        }
        @Override
        @NonNull
        public PhotoHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.gallery_item, viewGroup, false);
            return new PhotoHolder(view);
        }
        @Override
        public void onBindViewHolder(PhotoHolder photoHolder, int position) {
            GalleryItem galleryItem = mGalleryItems.get(position);
            Drawable placeholder = getResources().getDrawable(R.drawable.brushpencil);
            photoHolder.setPict(false);
            photoHolder.bindDrawable(placeholder);
            if(mPhotoType)
                photoHolder.setUrl(galleryItem.getUrlBig());
            else
                photoHolder.setUrl(galleryItem.getUrlVideo());
            mThumbnailDownloader.queueThumbnail(photoHolder, galleryItem.getUrl());
        }
        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }

        public void setGalleryItems(List<GalleryItem> mGalleryItems) {
            this.mGalleryItems = mGalleryItems;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
    }
}
