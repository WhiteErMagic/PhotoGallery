package com.myexample.photogallery;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class FlickrFetchr {

    private static final String TAG = "Pixabay";
    private static final String API_KEY = "17682329-2f73d4afdd32935166bee8eb2";
    //private static final String API_KEY = "21d2954f387c18f6d4ee035a5068dbb5";
    private boolean mPhotoType;
    private int mVideoWidth;

    public byte[] getUrlBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage() +
                        ": with " +
                        urlSpec);
            }
            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return out.toByteArray();
        } finally {
            connection.disconnect();
        }
    }

    String getUrlString(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }

    List<GalleryItem> fetchItems(Integer arg, List<GalleryItem> items, String mQuery, boolean mPhotoType, int mVideoWidth) {
        this.mPhotoType = mPhotoType;
        this.mVideoWidth = mVideoWidth;
        try {
            Uri.Builder uri = Uri.parse("https://pixabay.com/api/" + (mPhotoType?"":"videos"))
                    .buildUpon()
                    .appendQueryParameter("key", API_KEY)
                    .appendQueryParameter("pretty", "true")
                    .appendQueryParameter("image_type", mPhotoType?"photo":"video")
                    .appendQueryParameter("per_page", "100")
                    .appendQueryParameter("page", arg.toString());
                    if(mQuery != null){
                        uri.appendQueryParameter("q", mQuery.replace(" ", "+"));
                    }
            String url = uri.toString();
            String jsonString = getUrlString(url);
            JSONObject jsonBody = new JSONObject(jsonString);
            parseItems(items, jsonBody);
        } catch (IOException ioe) {
            return null;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        return items;
    }

    private void parseItems(List<GalleryItem> items, JSONObject jsonBody)
            throws IOException, JSONException {
        JSONArray photoJsonArray = jsonBody.getJSONArray("hits");
        for (int i = 0; i < photoJsonArray.length(); i++) {
            JSONObject photoJsonObject = photoJsonArray.getJSONObject(i);
            GalleryItem item = new GalleryItem();
            item.setId(photoJsonObject.getString("id"));
            if(mPhotoType) {
                item.setUrl(photoJsonObject.getString("previewURL"));
                item.setUrlBig(photoJsonObject.getString("largeImageURL"));
            }else{
                item.setUrl("https://i.vimeocdn.com/video/" + photoJsonObject.getString("picture_id")
                        + "_200x150.jpg");
                JSONObject videoObject = photoJsonObject.getJSONObject("videos");
                String url = "";
                if (mVideoWidth > 4000)
                    url = videoObject.getJSONObject("large").get("url").toString();
//                else if (mVideoWidth <= 1280)
//                    url = videoObject.getJSONObject("medium").get("url").toString();
//                else if (mVideoWidth <= 960)
//                    url = videoObject.getJSONObject("small").get("url").toString();
                else
                    url = videoObject.getJSONObject("tiny").get("url").toString();

                item.setUrlVideo(url);
            }

            items.add(item);
        }
    }
}
