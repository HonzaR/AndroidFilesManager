package com.honzar.androidfilesmanager.library;

import android.os.AsyncTask;
import android.os.Build;

/**
 * Created by Honza Rychnovský on 28.7.2016.
 * AppsDevTeam
 * honzar@appsdevteam.com
 */
public abstract class ParallelAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {

    /**
     * Executes the task with the specified parameters. Use instead of execute().
     *
     * @param params The parameters of the task.
     * @return This instance of AsyncTask.
     */
    public final AsyncTask<Params, Progress, Result> parallelExecute(Params... params) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            return executeOnExecutor(THREAD_POOL_EXECUTOR, params);
        } else {
            return execute(params);
        }
    }
}
