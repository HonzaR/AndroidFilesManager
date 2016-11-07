package com.honzar.androidfilesmanager.library;

import android.content.Context;

/**
 * Created by Honza Rychnovský on 3.11.2016.
 * AppsDevTeam
 * honzar@appsdevteam.com
 */

public class FilesManagerPreferences {

    public static final int DEFAULT = 0;
    public static final int INTERNAL_STORAGE = 1;
    public static final int EXTERNAL_STORAGE = 2;

    private int currStoragePreferences;

    public int getCurrStoragePreferences() {
        return currStoragePreferences;
    }

    public void setCurrStoragePreferences(int currStoragePreferences) {
        this.currStoragePreferences = currStoragePreferences;
    }

    public static String resolveStorageName(Context c, int storageId)
    {
        switch (storageId) {
            case INTERNAL_STORAGE:
                return c.getString(R.string.storage_internal);
            case EXTERNAL_STORAGE:
                return c.getString(R.string.storage_external);
            default:
                return "";
        }
    }
}
