package com.myexample.photogallery;

import android.content.Context;
import android.preference.PreferenceManager;

class QueryPreferences {
    private static final String PREF_SEARCH_QUERY = "searchQuery";
    static String getStoredQuery(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREF_SEARCH_QUERY, null);
    }
    static void setStoredQuery(Context context, String query) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(PREF_SEARCH_QUERY, query)
                .apply();
    }
}
