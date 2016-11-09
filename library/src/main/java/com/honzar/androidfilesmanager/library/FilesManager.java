package com.honzar.androidfilesmanager.library;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.support.v4.content.ContextCompat;
import android.util.Xml;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlSerializer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Created by Honza Rychnovský on 3.11.2016.
 * AppsDevTeam
 * honzar@appsdevteam.com
 */

public class FilesManager {

    public static final int DEFAULT = 0;
    public static final int INTERNAL_STORAGE = 1;
    public static final int EXTERNAL_STORAGE = 2;

    private static final int EXTERNAL_TO_INTERNAL_STORAGE_RATIO = 2;

    private static FilesManager instance;
    private static Context mContext;
    private SharedPreferencesManager prefsManager;
    private String externalStorage;
    private String internalStorage;
    private int currentStorageID;
    private String storagesConfiguration;

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
        return instance;
    }

    /**
     * Constructor.
     * @param c context
     */
    private FilesManager(Context c)
    {
        this.mContext = c;
        this.prefsManager = SharedPreferencesManager.getInstance(mContext);

        this.internalStorage = mContext.getFilesDir().getAbsoluteFile() + "/";
        File[] extStorages = ContextCompat.getExternalFilesDirs(mContext, null);
        if (extStorages != null && extStorages.length > 0) {
            this.externalStorage = extStorages[0].getAbsoluteFile() + "/";
        }

        // set up current configuration
        this.storagesConfiguration = prefsManager.getPrefsLastStoragesConfiguration();
        if (storagesConfiguration == null) {
            storagesConfiguration = getCurrentStoragesConfiguration();
        }

        // get optimal storage
        currentStorageID = prefsManager.getSelectedStorage();
        if (currentStorageID == SharedPreferencesManager.PREFS_NONE_NUM) {
            currentStorageID = getOptimalStorage();
        }
    }


    //
    // PRIVATE METHODS
    //

    /**
     * Resolves storage path from storage ID.
     * @param id storage ID
     * @return storage string object
     */
    private String resolveStoragePathByID(int id)
    {
        switch (id) {
            case INTERNAL_STORAGE:
                return internalStorage;
            case EXTERNAL_STORAGE:
                return externalStorage;
            case DEFAULT:
                return resolveStoragePathByID(currentStorageID);
            default:
                return resolveStoragePathByID(currentStorageID);
        }
    }

    /**
     * Returns current storages configuration string.
     * @return current storages configuration string
     */
    private String getCurrentStoragesConfiguration()
    {
        return internalStorage + externalStorage;
    }

    /**
     * Adds slash to end of path string, if they are missing.
     * @param path to be tested.
     * @return path string ending with "/" character
     */
    private String addSlashesToPathIfNeeded(String path)
    {
        if (path != null) {

            if(!path.endsWith("/"))
                path += "/";

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
    private String addStorageDirectoryToPath(String storage, String dir)
    {
        return dir == null ? storage : storage + dir;
    }

    /**
     * Returns storage free space in Bytes.
     * @return storage free space in Bytes if succeed, -1 otherwise
     */
    private long getStorageFreeSpace(String storage)
    {
        long availableSpace = -1L;

        if (storage != null) {
            try {
                StatFs stat = new StatFs(storage);
                stat.restat(storage);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    availableSpace = stat.getAvailableBlocksLong() * stat.getBlockSizeLong();
                } else {
                    availableSpace = (long) (stat.getAvailableBlocks() * stat.getBlockSize());
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return availableSpace;
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
        return getFile(null, fileName, DEFAULT);
    }

    /**
     * Returns File object.
     * @param filePath local path to file
     * @param fileName name of file
     * @return File object
     */
    public File getFile(String filePath, String fileName)
    {
        return getFile(filePath, fileName, DEFAULT);
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

        String storageToBeUsed = resolveStoragePathByID(preferences);

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
        return getAllFilesFromDir(path, DEFAULT);
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

        String storageToBeUsed = resolveStoragePathByID(preferences);

        path = addSlashesToPathIfNeeded(path);
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
        return getAllFilesFromDir(null, EXTERNAL_STORAGE);
    }

    /**
     * Returns all files from internal storage.
     * @return all files from internal storage.
     */
    public ArrayList<File> getAllFileFromInternalStorage()
    {
        return getAllFilesFromDir(null, INTERNAL_STORAGE);
    }

    /**
     * Returns all files from currently chosen storage.
     * @return all files from currently chosen storage.
     */
    public ArrayList<File> getAllFileFromCurrentStorage()
    {
        return getAllFilesFromDir(null, DEFAULT);
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
        return copyFile(fileName, srcDir, destDir, DEFAULT);
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

        String storageToBeUsed = resolveStoragePathByID(preferences);

        srcDir = addStorageDirectoryToPath(storageToBeUsed, srcDir);
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
        path = addStorageDirectoryToPath(resolveStoragePathByID(currentStorageID), path);

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
        return createEmptyFile(filePath, fileName, DEFAULT);
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

        String storageToBeUsed = resolveStoragePathByID(preferences);

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
        path = addStorageDirectoryToPath(resolveStoragePathByID(currentStorageID), path);

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
        return createEmptyDir(path, dirName, DEFAULT);
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

        String storageToBeUsed = resolveStoragePathByID(preferences);

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
        path = addStorageDirectoryToPath(resolveStoragePathByID(currentStorageID), path);

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
        path = addStorageDirectoryToPath(resolveStoragePathByID(currentStorageID), path);

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
    public String getCurrentStorage()
    {
        return resolveStoragePathByID(currentStorageID);
    }

    /**
     * Resolves optimal storage from currently available storages.
     * @return optimal storage ID
     */
    public int getOptimalStorage()
    {
        if (isExternalStorageWritable()) {

            long extFreeSize = getExternalStorageFreeSpace();
            long intFreeSize = getInternalStorageFreeSpace();

            if (intFreeSize == (extFreeSize * EXTERNAL_TO_INTERNAL_STORAGE_RATIO)) {
                return INTERNAL_STORAGE;
            } else {
                return EXTERNAL_STORAGE;
            }
        }

        return INTERNAL_STORAGE;
    }

    /**
     * Deletes whole chosen storage.
     * @param preferences preferred storage to be deleted
     * @return true if succeed, false otherwise.
     */
    public boolean deleteStorage(int preferences)
    {
        String storage = resolveStoragePathByID(preferences);

        try {
            FileUtils.deleteDirectory(new File(storage));
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
        if (currentStorageID == EXTERNAL_STORAGE) {
            if (callbacks != null) {
                callbacks.moveStorageAlreadyDone();
            }
            return;
        }
        MoveStorageTask task = new MoveStorageTask(mContext, EXTERNAL_STORAGE, callbacks);
        task.execute();
    }

    /**
     * Moves files from current storage to internal
     * @param callbacks to inform about result
     */
    public void moveStorageToInternal(OptimalStorageMoveInterface callbacks)
    {
        if (currentStorageID == INTERNAL_STORAGE) {
            if (callbacks != null) {
                callbacks.moveStorageAlreadyDone();
            }
            return;
        }
        MoveStorageTask task = new MoveStorageTask(mContext, INTERNAL_STORAGE, callbacks);
        task.execute();
    }

    /**
     * Moves current storage files to optimal storage, if different.
     * @param callbacks to inform about result
     */
    public void moveStorageToOptimal(OptimalStorageMoveInterface callbacks)
    {
        if (currentStorageID == getOptimalStorage()) {
            if (callbacks != null) {
                callbacks.moveStorageAlreadyDone();
            }
            return;
        }
        MoveStorageTask task = new MoveStorageTask(mContext, getOptimalStorage(), callbacks);
        task.execute();
    }

    /**
     * Checks if optimal storage is used.
     * @return true/false
     */
    public boolean checkOptimalStorageIsUsed()
    {
        if (prefsManager.getPrefsLastStoragesConfiguration() == null) {     // first launch
            currentStorageID = getOptimalStorage();
            prefsManager.saveSelectedStorage(currentStorageID);
            prefsManager.saveLastUserAskedForChangeStorage(System.currentTimeMillis());
            prefsManager.saveStoragesConfiguration(storagesConfiguration);
            return true;

        } else {                                                            // check last configuration
            return !(!storagesConfiguration.equals(prefsManager.getPrefsLastStoragesConfiguration()) || getOptimalStorage() != prefsManager.getSelectedStorage());
        }
    }

    //
    // GENERAL INFORMATIVE METHODS
    //

    /**
     * Returns if external storage is writable.
     * @return true/false
     */
    public boolean isExternalStorageWritable()
    {
        if (externalStorage != null) {
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns if external storage is readable.
     * @return true/false
     */
    public boolean isExternalStorageReadable()
    {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    /**
     * Returns current storage free space in Bytes.
     * @return current storage free space in Bytes if succeed, -1 otherwise
     */
    public long getCurrentStorageFreeSpace()
    {
        return getStorageFreeSpace(resolveStoragePathByID(currentStorageID));
    }

    /**
     * Returns external storage free space in Bytes.
     * @return external storage free space in Bytes if succeed, -1 otherwise
     */
    public long getExternalStorageFreeSpace()
    {
        return getStorageFreeSpace(externalStorage);
    }

    /**
     * Returns internal storage free space in Bytes.
     * @return internal storage free space in Bytes if succeed, -1 otherwise
     */
    public long getInternalStorageFreeSpace()
    {
        return getStorageFreeSpace(internalStorage);
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
     * Writes Xml data to file.
     * @param file
     * @param data
     * @return true if succeed, false otherwise.
     */
    public boolean writeObjectToFile(File file, Document data)
    {
        try {
            FileOutputStream out = new FileOutputStream(file);
            StringWriter writer = new StringWriter();
            XmlSerializer xmlSerializer = Xml.newSerializer();
            TransformerFactory tf = TransformerFactory.newInstance();

            Transformer t = tf.newTransformer();
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.transform(new DOMSource(data), new StreamResult(writer));

            xmlSerializer.setOutput(writer);
            xmlSerializer.flush();

            out.write(writer.toString().getBytes());
            out.close();
            return true;

        } catch (TransformerConfigurationException tce) {
            tce.printStackTrace();
        } catch (TransformerException tce) {
            tce.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
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
    // READ DATA FROM FILE METHODS
    //

    /**
     * Reads String content of file.
     * @param file
     * @return String Object if succeed, null otherwise.
     */
    public String readStringFromFile(File file)
    {
        if (file != null) {

            try {
                return FileUtils.readFileToString(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Reads JSONObject content of file.
     * @param file
     * @return JSONObject if succeed, null otherwise.
     */
    public JSONObject readJSONObjectFromFile(File file)
    {
        String res = readStringFromFile(file);

        try {
            return res != null ? new JSONObject(res) : null;

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Reads Xml content of file.
     * @param file
     * @return Xml if succeed, null otherwise.
     */
    public Document readXmlFromFile(File file)
    {
        String res = readStringFromFile(file);

        if (res != null) {

            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

                factory.setNamespaceAware(true);
                DocumentBuilder builder = factory.newDocumentBuilder();

                return builder.parse(new ByteArrayInputStream(res.getBytes()));

            } catch (IOException ioe) {
                ioe.printStackTrace();
            } catch (ParserConfigurationException pce) {
                pce.printStackTrace();
            } catch (SAXException se) {
                se.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Reads byte array content of file.
     * @param file
     * @return byte[] if succeed, null otherwise.
     */
    public byte[] readByteArrayFromFile(File file)
    {
        if (file != null) {

            try {
                return FileUtils.readFileToByteArray(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    //
    //  INNER CLASS
    //

    /**
     * Asynchronous task to move all storage files to selected storage.
     */
    public class MoveStorageTask extends AsyncTask<Void, Void, Boolean> {

        private Context context;
        private Exception exception;
        private OptimalStorageMoveInterface callbacks;
        private int storageID;
        private String from;
        private String to;

        /**
         * Task constructor.
         * @param context
         * @param storageID
         * @param callbacks
         */
        public MoveStorageTask(Context context, int storageID, OptimalStorageMoveInterface callbacks) {
            this.context = context;
            this.callbacks = callbacks;
            this.storageID = storageID;

            if (storageID == INTERNAL_STORAGE) {
                this.from = externalStorage;
                this.to = internalStorage;
            } else {
                this.from = internalStorage;
                this.to = externalStorage;
            }
        }

        @Override
        protected void onPreExecute() {
            if (callbacks != null) {
                callbacks.moveStorageStarts(this);
            }
        }

        @Override
        protected Boolean doInBackground(final Void... none) {

            try {
                FileUtils.copyDirectory(new File(from), new File(to), true);
                FileUtils.deleteDirectory(new File(from));
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                this.exception = e;
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean res) {

            if (res) {
                if (callbacks != null) {
                    callbacks.moveStorageEndsSuccess(this);

                    currentStorageID = storageID;
                    storagesConfiguration = getCurrentStoragesConfiguration();

                    prefsManager.saveSelectedStorage(storageID);
                    prefsManager.saveStoragesConfiguration(storagesConfiguration);
                    prefsManager.saveLastUserAskedForChangeStorage(System.currentTimeMillis());
                }
            } else {
                if (callbacks != null) {
                    callbacks.moveStorageEndsError(this, exception);
                }
            }
        }
    }

    /**
     * Callbacks for moving storage methods.
     */
    public interface OptimalStorageMoveInterface {
        void moveStorageStarts(MoveStorageTask task);
        void moveStorageEndsSuccess(MoveStorageTask task);
        void moveStorageEndsError(MoveStorageTask task, Exception e);
        void moveStorageAlreadyDone();
    }

}
