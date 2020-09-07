package com.myexample.photogallery;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.ViewTreeObserver;
import android.widget.Toast;

public class PhotoGalleryActivity extends SingleFragmentActivity{

    @Override
    protected Fragment createFragment() {
        return PhotoGalleryFragment.newInstance();
    }
}
