package com.myexample.photogallery;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.net.Uri;
import android.widget.MediaController;
import android.widget.VideoView;

import java.io.IOException;

public class PhotoGalleryBigItem extends AppCompatActivity {
    private boolean mPhotoType;
    private View view;
    private int dX = 0;
    private int dY = 0;
    private VideoView vidView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        Intent intent = getIntent();
        mPhotoType = intent.getBooleanExtra("type", true);
        if(mPhotoType) {
            setContentView(R.layout.gallery_item_big);
            view = findViewById(R.id.item_image_view_big);
        }else {
            setContentView(R.layout.video_view);
            view =  findViewById(R.id.my_video);
        }

        if(mPhotoType) {
            FetchItemsTask mTack = new FetchItemsTask();
            mTack.execute(intent.getStringExtra("url"));
        }else{
            vidView = (VideoView)findViewById(R.id.my_video);
            MediaController controls = new MediaController(this){
                @Override
                public void hide() {
                    super.hide();
                }
            };
            controls.setAnchorView(vidView);
            vidView.setMediaController(controls);
            Uri vidUri = Uri.parse(intent.getStringExtra("url"));
            vidView.setVideoURI(vidUri);
            vidView.start();
        }

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                dX = (int) event.getRawX();
                dY = (int) event.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                if(Math.abs(dX - event.getRawX()) > 100
                    || Math.abs(dY - event.getRawY()) > 100)
                    finish();
                break;
        }

        return true;

    }

    private class FetchItemsTask extends AsyncTask<String ,Void, byte[]> {
        @Override
        protected byte[] doInBackground(String... params) {

            try {

                return new FlickrFetchr().getUrlBytes(params[0]);
            } catch (IOException e) {
                e.printStackTrace();
                return new byte[0];
            }
        }

        @Override
        protected void onPostExecute(byte[] item) {
            Bitmap mBitMap = BitmapFactory.decodeByteArray(item, 0, item.length);
            ImageView iv = (ImageView) findViewById(R.id.item_image_view_big);
            iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            iv.setAdjustViewBounds(true);
            if(mBitMap == null) {
                ((TextView)findViewById(R.id.textView)).setText(R.string.load_error);
            }else
                findViewById(R.id.textView).setVisibility(View.INVISIBLE);

            iv.setImageBitmap(mBitMap);
        }
    }
}
