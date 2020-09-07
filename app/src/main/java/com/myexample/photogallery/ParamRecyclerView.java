package com.myexample.photogallery;

import android.app.Activity;
import android.view.Display;

class ParamRecyclerView {
    private static int mHeight;
    private static int mNewHeight;
    private static int mCol;
    private static int mNewCol;
    private static Display display;

    static void reConfigDisplay(Activity ctx) {
        display = ctx.getWindowManager().getDefaultDisplay();
        if (display.getWidth() <= 300) {
            mCol = 2;
        } else {
            mCol = display.getWidth() / 200;
        }
        if(mCol > 2)
            mNewCol = (int) (mCol / 1.5);
        else
            mNewCol = mCol;

        mNewHeight = display.getWidth() / mNewCol;
        mHeight = display.getWidth() / mCol;
    }

    static int getCol(boolean mColChange){
        if(mColChange)
            return mNewCol;
        else
            return mCol;
    }

    static int getHeight(boolean mColChange){
        if(mColChange)
            return mNewHeight;
        else
            return mHeight;
    }

    static int getWidth(){
        return display.getWidth();
    }
}
