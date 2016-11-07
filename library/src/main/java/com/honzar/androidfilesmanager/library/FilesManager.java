package com.honzar.androidfilesmanager.library;

import android.app.Activity;
import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Honza Rychnovský on 3.11.2016.
 * AppsDevTeam
 * honzar@appsdevteam.com
 */

public class FilesManager {

    private static final int EXTERNAL_TO_INTERNAL_STORAGE_RATIO = 2;

    private static FilesManager instance;
    private static Context mContext;
    private static AppCompatActivity mActivity;
    private SharedPreferencesManager prefsManager;
    private File[] externalStorages;
    private File internalStorage;
    private File currentStorage;
    private String storagesConfiguration;
    private FilesManagerPreferences filesPrefs;
    private int optimalStorage;

    /**
     * Singleton method.
     * @param c context
     * @return FilesManager instance
     */
    public static FilesManager getInstance(Context c)
    {
        if (instance == null) {
            instance = new FilesManager(c);
        }
        mContext = c;
        mActivity = (AppCompatActivity) c;
        return instance;
    }

    /**
     * Constructor.
     * @param c context
     */
    private FilesManager(Context c)
    {
        mContext = c;
        this.filesPrefs = new FilesManagerPreferences();
        this.prefsManager = SharedPreferencesManager.getInstance(mContext);

        this.internalStorage = mContext.getFilesDir();
        this.externalStorages = ContextCompat.getExternalFilesDirs(mContext, null);

        // set up current configuration
        this.storagesConfiguration = getCurrentStoragesConfiguration();

        // check optimal storage
        optimalStorage = resolveOptimalStorage();
        currentStorage = optimalStorage == FilesManagerPreferences.INTERNAL_STORAGE ? internalStorage : externalStorages[0];
    }


    //
    // PRIVATE METHODS
    //

    /**
     * Resolves storage file from storage ID.
     * @param id storage ID
     * @return storage file ovject
     */
    private File resolveStorageFileByID(int id)
    {
        switch (id) {
            case FilesManagerPreferences.INTERNAL_STORAGE:
                return internalStorage;
            case FilesManagerPreferences.EXTERNAL_STORAGE:
                return externalStorages[0];
            case FilesManagerPreferences.DEFAULT:
                return currentStorage;
            default:
                return currentStorage;
        }
    }

    /**
     * Returns current storages configuration string.
     * @return current storages configuration string
     */
    private String getCurrentStoragesConfiguration()
    {
        String config = internalStorage.getAbsolutePath();
        for (File f : externalStorages) {
            config += f.getAbsolutePath();
        }
        return config;
    }

    /**
     * Resolves optimal storage from currently available storages.
     * @return optimal storage ID
     */
    private int resolveOptimalStorage()
    {
        if (Utils.isExternalStorageWritable()) {

            long extFreeSize = Utils.getExternalStorageAvailableSpace();
            long intFreeSize = Utils.getInternalStorageAvailableSpace();

            if (intFreeSize == (extFreeSize * EXTERNAL_TO_INTERNAL_STORAGE_RATIO)) {
                return FilesManagerPreferences.INTERNAL_STORAGE;
            } else {
                return FilesManagerPreferences.EXTERNAL_STORAGE;
            }
        }

        return FilesManagerPreferences.INTERNAL_STORAGE;
    }

    /**
     * Adds slashes to start and end of path string, if they are missing.
     * @param path to be tested.
     * @return path string starting and ending with "/" character
     */
    private String addSlashesToPathIfNeeded(String path)
    {
        if (path != null) {

            if(!path.endsWith("/"))
                path += "/";

            if (!path.startsWith("/"))
                path = "/" + path;

            return path;
        }
        return null;
    }

    /**
     * Adds storage path at the beginning of dir
     * @param storage
     * @param dir
     * @return path string starting with storage path
     */
    private String addStorageDirectoryToPath(File storage, String dir)
    {
        return dir == null ? storage.getAbsolutePath() : storage.getAbsolutePath() + dir;
    }


    //
    //  GET FILES METHODS
    //

    /**
     * Returns File object.
     * @param fileName name of file
     * @return File object
     */
    public File getFile(String fileName)
    {
        return getFile(null, fileName, FilesManagerPreferences.DEFAULT);
    }

    /**
     * Returns File object.
     * @param filePath local path to file
     * @param fileName name of file
     * @return File object
     */
    public File getFile(String filePath, String fileName)
    {
        return getFile(filePath, fileName, FilesManagerPreferences.DEFAULT);
    }

    /**
     * Returns File object.
     * @param fileName name of file
     * @param preferences preferred storage
     * @return File object
     */
    public File getFile(String fileName, int preferences)
    {
        return getFile(null, fileName, preferences);
    }

    /**
     * Returns File object.
     * @param filePath local path to file
     * @param fileName name of file
     * @param preferences preferred storage
     * @return File object
     */
    public File getFile(String filePath, String fileName, int preferences)
    {
        filePath = addSlashesToPathIfNeeded(filePath);
        File storageToBeUsed = currentStorage;

        if (preferences != FilesManagerPreferences.DEFAULT) {

            switch (preferences) {
                case FilesManagerPreferences.INTERNAL_STORAGE:
                    storageToBeUsed = internalStorage;
                    break;
                case FilesManagerPreferences.EXTERNAL_STORAGE:
                    storageToBeUsed = externalStorages[0];
                    break;
            }
        }

        filePath = addStorageDirectoryToPath(storageToBeUsed, filePath);

        if (fileName != null) {
            return new File(filePath, fileName);
        }

        return null;
    }


    //
    //  GET ALL FILES METHODS
    //

    /**
     * Returns all files from directory.
     * @param path local directory path
     * @return all files from directory.
     */
    public ArrayList<File> getAllFilesFromDir(String path)
    {
        return getAllFilesFromDir(path, FilesManagerPreferences.DEFAULT);
    }

    /**
     * Returns all files from directory.
     * @param path local directory path
     * @param preferences preferred storage
     * @return all files from directory.
     */
    public ArrayList<File> getAllFilesFromDir(String path, int preferences)
    {
        ArrayList<File> inFiles = new ArrayList<>();
        File storageToBeUsed = currentStorage;

        path = addSlashesToPathIfNeeded(path);

        if (preferences != FilesManagerPreferences.DEFAULT) {

            switch (preferences) {
                case FilesManagerPreferences.INTERNAL_STORAGE:
                    storageToBeUsed = internalStorage;
                    break;
                case FilesManagerPreferences.EXTERNAL_STORAGE:
                    storageToBeUsed = externalStorages[0];
                    break;
            }
        }

        path = addStorageDirectoryToPath(storageToBeUsed, path);

        inFiles = (ArrayList<File>) FileUtils.listFiles(new File(path), null, true);

        return inFiles;
    }

    /**
     * Returns all files from external storage.
     * @return all files from external storage.
     */
    public ArrayList<File> getAllFileFromExternalStorage()
    {
        return getAllFilesFromDir(null, FilesManagerPreferences.EXTERNAL_STORAGE);
    }

    /**
     * Returns all files from internal storage.
     * @return all files from internal storage.
     */
    public ArrayList<File> getAllFileFromInternalStorage()
    {
        return getAllFilesFromDir(null, FilesManagerPreferences.INTERNAL_STORAGE);
    }

    /**
     * Returns all files from currently chosen storage.
     * @return all files from currently chosen storage.
     */
    public ArrayList<File> getAllFileFromCurrentStorage()
    {
        return getAllFilesFromDir(null, FilesManagerPreferences.DEFAULT);
    }


    //
    //  GET ALL FILES METHODS
    //

    /**
     * Copies file from one directory to another.
     * @param fileName
     * @param srcDir
     * @param destDir
     * @return true if succeed, false otherwise.
     */
    public boolean copyFile(String fileName, String srcDir, String destDir)
    {
        return copyFile(fileName, srcDir, destDir, FilesManagerPreferences.DEFAULT);
    }

    /**
     * Copies file from one directory to another.
     * @param fileName
     * @param srcDir
     * @param destDir
     * @param preferences
     * @return true if succeed, false otherwise.
     */
    public boolean copyFile(String fileName, String srcDir, String destDir, int preferences)
    {
        srcDir = addSlashesToPathIfNeeded(srcDir);
        destDir = addSlashesToPathIfNeeded(destDir);

        File storageToBeUsed = currentStorage;

        if (preferences != FilesManagerPreferences.DEFAULT) {

            switch (preferences) {
                case FilesManagerPreferences.INTERNAL_STORAGE:
                    storageToBeUsed = internalStorage;
                    break;
                case FilesManagerPreferences.EXTERNAL_STORAGE:
                    storageToBeUsed = externalStorages[0];
                    break;
            }
        }

        srcDir = addStorageDirectoryToPath(currentStorage, srcDir);
        destDir = addStorageDirectoryToPath(storageToBeUsed, destDir);

        try {
            FileUtils.copyFile(new File(srcDir, fileName), new File(destDir, fileName), true);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }


    //
    //  FILE METHODS
    //

    /**
     * Checks if file exists on selected path.
     * @param fileName
     * @param path
     * @return true if file exists, false otherwise.
     */
    public boolean checkFileExists(String fileName, String path)
    {
        path = addSlashesToPathIfNeeded(path);
        path = addStorageDirectoryToPath(currentStorage, path);

        try {
            File file = new File(path, fileName);

            if (file.exists())
                return true;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Creates empty file with selected name on selected path.
     * @param filePath
     * @param fileName
     * @return empty File object on success, null otherwise.
     */
    public File createEmptyFile(String filePath, String fileName)
    {
        return createEmptyFile(filePath, fileName, FilesManagerPreferences.DEFAULT);
    }

    /**
     * Creates empty file with selected name on selected path.
     * @param filePath
     * @param fileName
     * @param preferences
     * @return empty File object on success, null otherwise.
     */
    public File createEmptyFile(String filePath, String fileName, int preferences)
    {
        filePath = addSlashesToPathIfNeeded(filePath);

        File storageToBeUsed = currentStorage;

        if (preferences != FilesManagerPreferences.DEFAULT) {

            switch (preferences) {
                case FilesManagerPreferences.INTERNAL_STORAGE:
                    storageToBeUsed = internalStorage;
                    break;
                case FilesManagerPreferences.EXTERNAL_STORAGE:
                    storageToBeUsed = externalStorages[0];
                    break;
            }
        }

        filePath = addStorageDirectoryToPath(storageToBeUsed, filePath);

        try {

            File file = new File(filePath, fileName);

            if (file.createNewFile() && file.exists())
                return file;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Deletes file with selected name on selected path.
     * @param path
     * @param fileName
     * @return true if succeed, false otherwise.
     */
    public boolean deleteFile(String path, String fileName)
    {
        path = addSlashesToPathIfNeeded(path);
        path = addStorageDirectoryToPath(currentStorage, path);

        return (new File(path, fileName)).delete();
    }

    /**
     * Deletes selected file.
     * @param file
     * @return true if succeed, false otherwise.
     */
    public boolean deleteFile(File file)
    {
        return file.delete();
    }

    //
    //  DIRECTORY METHODS
    //

    /**
     * Creates new empty directory with name and on selected path.
     * @param path
     * @param dirName
     * @return newly created File object, null otherwise.
     */
    public File createEmptyDir(String path, String dirName)
    {
        return createEmptyDir(path, dirName, FilesManagerPreferences.DEFAULT);
    }

    /**
     * Creates new empty directory with name, on selected path and to preferred storage.
     * @param path
     * @param dirName
     * @param preferences
     * @return newly created File object, null otherwise.
     */
    public File createEmptyDir(String path, String dirName, int preferences)
    {
        path = addSlashesToPathIfNeeded(path);

        File storageToBeUsed = currentStorage;

        if (preferences != FilesManagerPreferences.DEFAULT) {

            switch (preferences) {
                case FilesManagerPreferences.INTERNAL_STORAGE:
                    storageToBeUsed = internalStorage;
                    break;
                case FilesManagerPreferences.EXTERNAL_STORAGE:
                    storageToBeUsed = externalStorages[0];
                    break;
            }
        }

        path = addStorageDirectoryToPath(storageToBeUsed, path);

        File folder = new File(path, dirName);

        if (!folder.exists()) {

            if (folder.mkdirs())
                return folder;

        } else {
            return folder;
        }

        return null;
    }

    /**
     * Deletes whole directory with its content.
     * @param path
     * @return true if succeed, false otherwise.
     */
    public boolean deleteDir(String path)
    {
        path = addSlashesToPathIfNeeded(path);
        path = addStorageDirectoryToPath(currentStorage, path);

        try {
            FileUtils.deleteDirectory(new File(path));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * Renames directory on selected path with old name to new name.
     * @param path
     * @param oldName
     * @param newName
     * @return renamed directory File object if succeed, null otherwise.
     */
    public File renameDirectory(String path, String oldName, String newName)
    {
        path = addSlashesToPathIfNeeded(path);
        path = addStorageDirectoryToPath(currentStorage, path);

        if (new File(path, oldName).renameTo(new File(path, newName))) {
            return new File(path, newName);
        }

        return null;
    }

    //
    //  STORAGE METHODS
    //

    /**
     * Returns current storage File object.
     * @return current storage File object.
     */
    public File getCurrentStorage()
    {
        return currentStorage;
    }

    /**
     * Returns actually most optimal storage ID.
     * @return actually most optimal storage ID.
     */
    public int getOptimalStorage()
    {
        this.optimalStorage = resolveOptimalStorage();
        return this.optimalStorage;
    }

    /**
     * Deletes whole chosen storage.
     * @param preferences preferred storage to be deleted
     * @return true if succeed, false otherwise.
     */
    public boolean deleteStorage(int preferences)
    {
        File storage = currentStorage;

        if (preferences != FilesManagerPreferences.DEFAULT) {

            switch (preferences) {
                case FilesManagerPreferences.INTERNAL_STORAGE:
                    storage = internalStorage;
                    break;
                case FilesManagerPreferences.EXTERNAL_STORAGE:
                    storage = externalStorages[0];
                    break;
            }
        }

        try {
            FileUtils.deleteDirectory(storage);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * Moves files from current storage to external
     * @param callbacks to inform about result
     */
    public void moveStorageToExternal(OptimalStorageMoveInterface callbacks)
    {
        if (filesPrefs.getCurrStoragePreferences() == FilesManagerPreferences.EXTERNAL_STORAGE) {
            if (callbacks != null) {
                callbacks.moveStorageAlreadyDone();
            }
            return;
        }
        MoveStorageTask task = new MoveStorageTask(mContext, FilesManagerPreferences.EXTERNAL_STORAGE, callbacks);
        task.parallelExecute();
    }

    /**
     * Moves files from current storage to internal
     * @param callbacks to inform about result
     */
    public void moveStorageToInternal(OptimalStorageMoveInterface callbacks)
    {
        if (filesPrefs.getCurrStoragePreferences() == FilesManagerPreferences.INTERNAL_STORAGE) {
            if (callbacks != null) {
                callbacks.moveStorageAlreadyDone();
            }
            return;
        }
        MoveStorageTask task = new MoveStorageTask(mContext, FilesManagerPreferences.INTERNAL_STORAGE, callbacks);
        task.parallelExecute();
    }

    /**
     * Moves current storage files to optimal storage, if different.
     * @param callbacks to inform about result
     */
    public void moveStorageToOptimal(OptimalStorageMoveInterface callbacks)
    {
        if (filesPrefs.getCurrStoragePreferences() == resolveOptimalStorage()) {
            if (callbacks != null) {
                callbacks.moveStorageAlreadyDone();
            }
            return;
        }
        MoveStorageTask task = new MoveStorageTask(mContext, resolveOptimalStorage(), callbacks);
        task.parallelExecute();
    }

    /**
     * Checks if optimal storage is used.
     * @return true/false
     */
    public boolean checkOptimalStorageIsUsed()
    {
        this.optimalStorage = resolveOptimalStorage();

        if (prefsManager.getPrefsLastStoragesConfiguration() == null) {     // first launch
            filesPrefs.setCurrStoragePreferences(optimalStorage);
            prefsManager.saveSelectedStorage(filesPrefs.getCurrStoragePreferences());
            prefsManager.saveLastUserAskedForChangeStorage(System.currentTimeMillis());
            prefsManager.saveStoragesConfiguration(storagesConfiguration);
            return true;

        } else {                                                            // check last configuration
            return !(!storagesConfiguration.equals(prefsManager.getPrefsLastStoragesConfiguration()) || optimalStorage != prefsManager.getSelectedStorage());
        }
    }

    //
    // GENERAL INFORMATIVE METHODS
    //

    /**
     * Returns if external storage is writable.
     * @return
     */
    public boolean isExternalStorageWritable()
    {
        return Utils.isExternalStorageWritable();
    }

    /**
     * Returns if external storage is readable.
     * @return
     */
    public boolean isExternalStorageReadable()
    {
        return Utils.isExternalStorageReadable();
    }

    /**
     * Returns external storage free space in Bytes.
     * @return
     */
    public long getExternalStorageFreeSpace()
    {
        return Utils.getExternalStorageAvailableSpace();
    }

    /**
     * Returns internal storage free space in Bytes.
     * @return
     */
    public long getInternalStorageFreeSpace()
    {
        return Utils.getInternalStorageAvailableSpace();
    }


    //
    // WRITE OBJECT TO FILE METHODS
    //

    /**
     * Writes string data to file.
     * @param file
     * @param data
     * @return true if succeed, false otherwise.
     */
    public boolean writeObjectToFile(File file, String data)
    {
        try {
            FileOutputStream out = new FileOutputStream(file);
            out.write(data.getBytes());
            out.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Writes Json data to file.
     * @param file
     * @param data
     * @return true if succeed, false otherwise.
     */
    public boolean writeObjectToFile(File file, JSONObject data)
    {
        try {
            FileOutputStream out = new FileOutputStream(file);
            out.write((data.toString()).getBytes());
            out.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Writes byte[] data to file.
     * @param file
     * @param data
     * @return true if succeed, false otherwise.
     */
    public boolean writeObjectToFile(File file, byte[] data)
    {
        try {
            FileOutputStream out = new FileOutputStream(file);
            out.write(data);
            out.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    //
    //  INNER CLASS
    //

    /**
     * Asynchronous task to move all storage files to selected storage.
     */
    public class MoveStorageTask extends ParallelAsyncTask<Void, Void, Boolean> {

        Context context;
        Activity activity;
        OptimalStorageMoveInterface callbacks;
        int storageID;
        File from;
        File to;

        /**
         * Task constructor.
         * @param context
         * @param storageID
         * @param callbacks
         */
        public MoveStorageTask(Context context, int storageID, OptimalStorageMoveInterface callbacks) {
            this.context = context;
            this.activity = (Activity) context;
            this.callbacks = callbacks;
            this.storageID = storageID;

            if (storageID == FilesManagerPreferences.INTERNAL_STORAGE) {
                this.from = externalStorages[0];
                this.to = internalStorage;
            } else {
                this.from = internalStorage;
                this.to = externalStorages[0];
            }
        }

        @Override
        protected void onPreExecute() {
            if (callbacks != null) {
                callbacks.moveStorageStarts();
            }
        }

        @Override
        protected Boolean doInBackground(final Void... none) {

            try {
                FileUtils.copyDirectory(from, to, true);
                FileUtils.deleteDirectory(from);
                return true;
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }

            return false;
        }

        @Override
        protected void onPostExecute(Boolean res) {

            if (res) {
                if (callbacks != null) {
                    callbacks.moveStorageEndsSuccess();

                    currentStorage = resolveStorageFileByID(storageID);
                    storagesConfiguration = getCurrentStoragesConfiguration();
                    filesPrefs.setCurrStoragePreferences(storageID);

                    prefsManager.saveSelectedStorage(storageID);
                    prefsManager.saveStoragesConfiguration(storagesConfiguration);
                    prefsManager.saveLastUserAskedForChangeStorage(System.currentTimeMillis());

                    optimalStorage = resolveOptimalStorage();
                }
            } else {
                if (callbacks != null) {
                    callbacks.moveStorageEndsError();
                }
            }
        }
    }

    /**
     * Callbacks for moving storage methods.
     */
    public interface OptimalStorageMoveInterface {
        void moveStorageStarts();
        void moveStorageEndsSuccess();
        void moveStorageEndsError();
        void moveStorageAlreadyDone();
    }

}
