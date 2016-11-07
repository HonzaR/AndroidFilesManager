package com.honzar.androidfilesmanager.library;

import android.os.Build;
import android.os.Environment;
import android.os.StatFs;

/**
 * Created by Honza Rychnovský on 3.11.2016.
 * AppsDevTeam
 * honzar@appsdevteam.com
 */

public class Utils {

    /* Checks if external storage is available for read and write */
    public static boolean isExternalStorageWritable()
    {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public static boolean isExternalStorageReadable()
    {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    /* Get external storage available free space */
    public static long getExternalStorageAvailableSpace()
    {
        long availableSpace = -1L;

        try {
            StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
            stat.restat(Environment.getExternalStorageDirectory().getPath());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                availableSpace = stat.getAvailableBlocksLong() * stat.getBlockSizeLong();
            } else {
                availableSpace = (long) (stat.getAvailableBlocks() * stat.getBlockSize());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return availableSpace;
    }

    /* Get internal storage available free space */
    public static long getInternalStorageAvailableSpace()
    {
        long availableSpace = -1L;

        try {
            StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
            stat.restat(Environment.getDataDirectory().getPath());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                availableSpace = stat.getAvailableBlocksLong() * stat.getBlockSizeLong();
            } else {
                availableSpace = (long) (stat.getAvailableBlocks() * stat.getBlockSize());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return availableSpace;
    }
}
