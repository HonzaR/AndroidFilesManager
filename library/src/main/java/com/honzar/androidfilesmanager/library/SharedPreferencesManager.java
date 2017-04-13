package com.honzar.androidfilesmanager.library;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by Honza Rychnovský on 1.1.2016.
 * AppsDevTeam
 * honzar@appsdevteam.com
 */

class SharedPreferencesManager {

    public static final String PREFS_NONE = "none";
    public static final int PREFS_NONE_NUM = -1;
    public static final String PREFS_SELECTED_STORAGE = "selected_storage";
    public static final String PREFS_LAST_STORAGES_CONFIGURATION = "last_storages_configuration";
    private static final String PREFS_LAST_USER_ASKED_FOR_CHANGE_STORAGE = "last_user_asked_for_change_storage";

    private static SharedPreferences preferences;
    private static SharedPreferencesManager manager;
    private Context context;

    //
    // INNER METHODS
    //

    private SharedPreferencesManager(Context context) {
        this.context = context;
    }

    public static SharedPreferencesManager getInstance(Context context) {
        if (manager == null) {
            manager = new SharedPreferencesManager(context);
        }
        return manager;
    }

    private SharedPreferences getPreferences() {
        if (preferences == null) {
            preferences = PreferenceManager.getDefaultSharedPreferences(context);
        }
        return preferences;
    }

    // LAST STORAGES CONFIGURATION

    public void saveStoragesConfiguration(String configuration)
    {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(PREFS_LAST_STORAGES_CONFIGURATION, configuration);
        editor.apply();
    }

    public String getPrefsLastStoragesConfiguration()
    {
        String configuration = getPreferences().getString(PREFS_LAST_STORAGES_CONFIGURATION, PREFS_NONE);

        if (configuration.equals(PREFS_NONE)) {
            return null;
        } else {
            return configuration;
        }
    }

    // SELECTED STORAGE

    public void saveSelectedStorage(int selectedStorageId)
    {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putInt(PREFS_SELECTED_STORAGE, selectedStorageId);
        editor.apply();
    }

    public int getSelectedStorage()
    {
        return getPreferences().getInt(PREFS_SELECTED_STORAGE, PREFS_NONE_NUM);
    }

    // LAST GO TO BACKGROUND TIME

    public void saveLastUserAskedForChangeStorage(long time)
    {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putLong(PREFS_LAST_USER_ASKED_FOR_CHANGE_STORAGE, time);
        editor.apply();
    }

    public long getLastUserAskedForChangeStorage()
    {
        return getPreferences().getLong(PREFS_LAST_USER_ASKED_FOR_CHANGE_STORAGE, PREFS_NONE_NUM);
    }

}
