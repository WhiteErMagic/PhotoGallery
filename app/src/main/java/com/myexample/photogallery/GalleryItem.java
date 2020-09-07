package com.myexample.photogallery;

public class GalleryItem {
    //private String mCaption;
    private String mId;
    private String mUrl;
    private String mUrlBig;
    private String mUrlVideo;

    public String getId() {
        return mId;
    }

    public void setId(String mId) {
        this.mId = mId;
    }

    String getUrl() {
        return mUrl;
    }

    void setUrl(String mUrl) {
        this.mUrl = mUrl;
    }

    String getUrlBig() {
        return mUrlBig;
    }

    String getUrlVideo() {
        return mUrlVideo;
    }

    void setUrlVideo(String mUrlVideo) {
        this.mUrlVideo = mUrlVideo;
    }

    void setUrlBig(String mUrlBig) {
        this.mUrlBig = mUrlBig;
    }
}
