package com.honzar.androidfilesmanager.library;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.channels.FileChannel;
import java.util.LinkedList;

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

    public static final int DEFAULT_STORAGE = 0;
    public static final int INTERNAL_STORAGE = 1;
    public static final int EXTERNAL_STORAGE = 2;

    private static final int EXTERNAL_TO_INTERNAL_STORAGE_RATIO = 2;

    private static FilesManager instance;
    private static Context mContext;
    private SharedPreferencesManager prefsManager;
    private String externalStoragePath;
    private String internalStoragePath;
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

        this.internalStoragePath = resolveInternalStorageString();
        this.externalStoragePath = resolveExternalStorageString();

        // set up current configuration
        this.storagesConfiguration = prefsManager.getPrefsLastStoragesConfiguration();
        if (storagesConfiguration == null) {
            storagesConfiguration = getStoragesConfiguration();
        }

        // get optimal storage
        currentStorageID = prefsManager.getSelectedStorage();
        if (currentStorageID == SharedPreferencesManager.PREFS_NONE_NUM) {
            currentStorageID = getOptimalStorage();
            prefsManager.saveSelectedStorage(currentStorageID);
            prefsManager.saveLastUserAskedForChangeStorage(System.currentTimeMillis());
            prefsManager.saveStoragesConfiguration(getStoragesConfiguration());
        }
    }


    //
    // PRIVATE METHODS
    //

    /**
     * Resolves current internal storage string.
     * @return storage string
     */
    private String resolveInternalStorageString()
    {
        return mContext.getFilesDir().getAbsoluteFile() + "/";
    }

    /**
     * Resolves current external storage string.
     * @return storage string
     */
    private String resolveExternalStorageString()
    {
        String storage = null;
        File[] extStorages = ContextCompat.getExternalFilesDirs(mContext, null);
        if (extStorages != null && extStorages.length > 0 && extStorages[0] != null) {
            storage = extStorages[0].getAbsoluteFile() + "/";
        }
        return storage;
    }

    /**
     * Resolves storage path from storage ID.
     * @param storageId storage ID
     * @return storage string object
     */
    private String getStoragePath(int storageId)
    {
        switch (storageId) {
            case INTERNAL_STORAGE:
                return internalStoragePath;
            case EXTERNAL_STORAGE:
                return externalStoragePath;
            default:
                return getStoragePath(currentStorageID);
        }
    }

    /**
     * Returns current storages configuration string.
     * @return current storages configuration string
     */
    private String getStoragesConfiguration()
    {
        return internalStoragePath + externalStoragePath;
    }

    /**
     * Resolves and returns most current storages configuration.
     * @return storages string
     */
    private String getMostCurrentStoragesConfigurations()
    {
        String internal = resolveInternalStorageString();
        String external = resolveExternalStorageString();
        return internal + external;
    }

    /**
     * Adds slash to end of path string, if they are missing.
     * @param path to be tested.
     * @return path string ending with "/" character
     */
    private String addSlashToPathIfNeeded(String path)
    {
        if (path != null) {

            if (!path.endsWith("/"))
                path += "/";

            return path;
        }
        return null;
    }

    /**
     * Adds storage path at the beginning of dir
     * @param storagePath
     * @param dir
     * @return path string starting with storage path
     */
    private String addDirectoryToStoragePath(String storagePath, String dir)
    {
        return (dir == null) ? storagePath : storagePath + dir;
    }

    /**
     * Returns storage free space in Bytes.
     * @param storagePath
     * @return storage free space in Bytes if succeed, -1 otherwise
     */
    private long getStorageFreeSpace(String storagePath)
    {
        long availableSpace = -1L;

        if (storagePath != null) {
            try {
                StatFs stat = new StatFs(storagePath);
                stat.restat(storagePath);
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
        return getFile(null, fileName, DEFAULT_STORAGE);
    }

    /**
     * Returns File object.
     * @param filePath local path to file
     * @param fileName name of file
     * @return File object
     */
    public File getFile(String filePath, String fileName)
    {
        return getFile(filePath, fileName, DEFAULT_STORAGE);
    }

    /**
     * Returns File object.
     * @param fileName name of file
     * @param storageId preferred storage
     * @return File object
     */
    public File getFile(String fileName, int storageId)
    {
        return getFile(null, fileName, storageId);
    }

    /**
     * Returns File object.
     * @param filePath local path to file
     * @param fileName name of file
     * @param storageId preferred storage
     * @return File object
     */
    public File getFile(String filePath, String fileName, int storageId)
    {
        filePath = addSlashToPathIfNeeded(filePath);

        String storageToBeUsed = getStoragePath(storageId);

        filePath = addDirectoryToStoragePath(storageToBeUsed, filePath);

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
    public LinkedList<File> getAllFilesFromDir(String path)
    {
        return getAllFilesFromDir(path, DEFAULT_STORAGE);
    }

    /**
     * Returns all files from directory.
     * @param path local directory path
     * @param storageId preferred storage
     * @return all files from directory.
     */
    public LinkedList<File> getAllFilesFromDir(String path, int storageId)
    {
        LinkedList<File> inFiles = new LinkedList<>();

        String storageToBeUsed = getStoragePath(storageId);

        path = (path != null) ? addSlashToPathIfNeeded(path) : "";
        path = addDirectoryToStoragePath(storageToBeUsed, path);

        File dir = new File(path);
        if (dir.exists()) {
            inFiles = (LinkedList<File>) FileUtils.listFiles(new File(path), null, true);
        }

        return inFiles;
    }

    /**
     * Returns all files from external storage.
     * @return all files from external storage.
     */
    public LinkedList<File> getAllFileFromExternalStorage()
    {
        return getAllFilesFromDir(null, EXTERNAL_STORAGE);
    }

    /**
     * Returns all files from internal storage.
     * @return all files from internal storage.
     */
    public LinkedList<File> getAllFilesFromInternalStorage()
    {
        return getAllFilesFromDir(null, INTERNAL_STORAGE);
    }

    /**
     * Returns all files from currently chosen storage.
     * @return all files from currently chosen storage.
     */
    public LinkedList<File> getAllFileFromCurrentStorage()
    {
        return getAllFilesFromDir(null, DEFAULT_STORAGE);
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
        return copyFile(fileName, srcDir, destDir, DEFAULT_STORAGE);
    }

    /**
     * Copies file from one directory to another.
     * @param fileName
     * @param srcDir
     * @param destDir
     * @param storageId
     * @return true if succeed, false otherwise.
     */
    public boolean copyFile(String fileName, String srcDir, String destDir, int storageId)
    {
        srcDir = (srcDir != null) ? addSlashToPathIfNeeded(srcDir) : "";
        destDir = (destDir != null) ? addSlashToPathIfNeeded(destDir) : "";

        String storageToBeUsed = getStoragePath(storageId);

        srcDir = addDirectoryToStoragePath(storageToBeUsed, srcDir);
        destDir = addDirectoryToStoragePath(storageToBeUsed, destDir);

        try {
            FileUtils.copyFile(new File(srcDir, fileName), new File(destDir, fileName), true);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Copies file from one directory to absolute path.
     * @param fileName
     * @param srcDir
     * @param destAbsolutePath
     * @return true if succeed, false otherwise.
     */
    public boolean copyFileToAbsolutePath(String fileName, String srcDir, String destAbsolutePath)
    {
        srcDir = (srcDir != null) ? addSlashToPathIfNeeded(srcDir) : "";
        srcDir = addDirectoryToStoragePath(getStoragePath(currentStorageID), srcDir);

        try {
            FileUtils.copyFile(new File(srcDir, fileName), new File(destAbsolutePath, fileName), true);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Copies file from one source to file on selected path persisting photo exif data.
     * @param uri
     * @param fileName
     * @param destDir
     * @return true if succeed, false otherwise.
     */
    public boolean copyFilePersistingExifData(Uri uri, String fileName, String destDir)
    {
        ExifInterface exifData = null;
        String exifOrientation = null;

        String path;
        try {
            path = getRealPathFromURI(uri);
        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
            return false;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        }
        File srcFile = new File(path);

        try {   // persist exif photo data
            exifData = new ExifInterface(srcFile.getAbsolutePath());
            exifOrientation = exifData.getAttribute(ExifInterface.TAG_ORIENTATION);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (copyFile(srcFile, fileName, destDir, DEFAULT_STORAGE)) {

            if (exifOrientation != null) {
                try {
                    ExifInterface newExif = new ExifInterface(getFile(destDir, fileName).getAbsolutePath());
                    newExif.setAttribute(ExifInterface.TAG_ORIENTATION, exifOrientation);
                    newExif.saveAttributes();
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (UnsupportedOperationException e) { // caused by .png files, which don't work with exif
                    e.printStackTrace();
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Copies file from one source URI to file on selected path.
     * @param srcFile
     * @param fileName
     * @param destDir
     * @param storageId
     * @return true if succeed, false otherwise.
     */
    public boolean copyFile(File srcFile, String fileName, String destDir, int storageId)
    {
        destDir = (destDir != null) ? addSlashToPathIfNeeded(destDir) : "";
        String storageToBeUsed = getStoragePath(storageId);
        destDir = addDirectoryToStoragePath(storageToBeUsed, destDir);

        try {
            FileUtils.copyFile(srcFile, new File(destDir, fileName), true);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Copies file from one source URI to file on selected path.
     * @param sourceUri
     * @param fileName
     * @param destDir
     * @return true if succeed, false otherwise.
     */
    public boolean copyFile(Uri sourceUri, String fileName, String destDir)
    {
        destDir = (destDir != null) ? addSlashToPathIfNeeded(destDir) : "";
        destDir = addDirectoryToStoragePath(getStoragePath(currentStorageID), destDir);

        try {
            File source = new File(sourceUri.getPath());
            FileChannel src = new FileInputStream(source).getChannel();
            FileChannel dst = new FileOutputStream(new File(destDir, fileName)).getChannel();
            dst.transferFrom(src, 0, src.size());
            src.close();
            dst.close();
            return true;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    //
    //  FILE METHODS
    //

    /**
     * Checks if file exists.
     * @param file
     * @return true if file exists, false otherwise.
     */
    public boolean checkFileExists(File file)
    {
        try {
            if (file.exists())
                return true;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Checks if file exists on selected path.
     * @param fileName
     * @param path
     * @return true if file exists, false otherwise.
     */
    public boolean checkFileExists(String fileName, String path)
    {
        path = (path != null) ? addSlashToPathIfNeeded(path) : "";
        path = addDirectoryToStoragePath(getStoragePath(currentStorageID), path);

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
        return createEmptyFile(filePath, fileName, DEFAULT_STORAGE);
    }

    /**
     * Creates empty file with selected name on selected path.
     * @param filePath
     * @param fileName
     * @param storageId
     * @return empty File object on success, null otherwise.
     */
    public File createEmptyFile(String filePath, String fileName, int storageId)
    {
        filePath = (filePath != null) ? addSlashToPathIfNeeded(filePath) : "";

        String storageToBeUsed = getStoragePath(storageId);

        filePath = addDirectoryToStoragePath(storageToBeUsed, filePath);

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
        path = (path != null) ? addSlashToPathIfNeeded(path) : "";
        path = addDirectoryToStoragePath(getStoragePath(currentStorageID), path);

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

    /**
     * Renames file on selected path with old name to new name.
     * @param path
     * @param oldName
     * @param newName
     * @return renamed File object if succeed, null otherwise.
     */
    public File renameFile(String path, String oldName, String newName)
    {
        path = (path != null) ? addSlashToPathIfNeeded(path) : "";
        path = addDirectoryToStoragePath(getStoragePath(currentStorageID), path);

        if (new File(path, oldName).renameTo(new File(path, newName))) {
            return new File(path, newName);
        }

        return null;
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
        return createEmptyDir(path, dirName, DEFAULT_STORAGE);
    }

    /**
     * Creates new empty directory with name, on selected path and to preferred storage.
     * @param path
     * @param dirName
     * @param storageId
     * @return newly created File object, null otherwise.
     */
    public File createEmptyDir(String path, String dirName, int storageId)
    {
        path = (path != null) ? addSlashToPathIfNeeded(path) : "";

        String storageToBeUsed = getStoragePath(storageId);

        path = addDirectoryToStoragePath(storageToBeUsed, path);

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
        path = (path != null) ? addSlashToPathIfNeeded(path) : "";
        path = addDirectoryToStoragePath(getStoragePath(currentStorageID), path);

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
        path = (path != null) ? addSlashToPathIfNeeded(path) : "";
        path = addDirectoryToStoragePath(getStoragePath(currentStorageID), path);

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
        return getStoragePath(currentStorageID);
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

            if (intFreeSize >= (extFreeSize * EXTERNAL_TO_INTERNAL_STORAGE_RATIO)) {
                return INTERNAL_STORAGE;
            } else {
                return EXTERNAL_STORAGE;
            }
        }

        return INTERNAL_STORAGE;
    }

    /**
     * Deletes whole chosen storage.
     * @param storageId preferred storage to be deleted
     * @return true if succeed, false otherwise.
     */
    public boolean deleteStorage(int storageId)
    {
        String storage = getStoragePath(storageId);

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
     * Check if there is some changes in storages configuration
     * @return true/false
     */
    public boolean checkIfStoragesChanged()
    {
        if (prefsManager.getPrefsLastStoragesConfiguration() == null) { // first launch
            currentStorageID = getOptimalStorage();
            prefsManager.saveSelectedStorage(currentStorageID);
            prefsManager.saveLastUserAskedForChangeStorage(System.currentTimeMillis());
            prefsManager.saveStoragesConfiguration(getStoragesConfiguration());
            return false;
        }
        return !getMostCurrentStoragesConfigurations().equals(prefsManager.getPrefsLastStoragesConfiguration());
    }

    /**
     * Checks if optimal storage is used.
     * @return true/false
     */
    public boolean isOptimalStorageUsed()
    {
        return (getOptimalStorage() == prefsManager.getSelectedStorage());
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
        if (externalStoragePath != null) {
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
        return getStorageFreeSpace(getStoragePath(currentStorageID));
    }

    /**
     * Returns external storage free space in Bytes.
     * @return external storage free space in Bytes if succeed, -1 otherwise
     */
    public long getExternalStorageFreeSpace()
    {
        return getStorageFreeSpace(externalStoragePath);
    }

    /**
     * Returns internal storage free space in Bytes.
     * @return internal storage free space in Bytes if succeed, -1 otherwise
     */
    public long getInternalStorageFreeSpace()
    {
        return getStorageFreeSpace(internalStoragePath);
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
    public boolean writeStringToFile(File file, String data)
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
     * Writes Uri data to file.
     * @param file
     * @param data
     * @return true if succeed, false otherwise.
     */
    public boolean writeDataFromUriToFile(File file, Uri data)
    {
        InputStream initialStream = null;
        try {
            initialStream = mContext.getContentResolver().openInputStream(data);
            byte[] buffer = new byte[initialStream.available()];
            initialStream.read(buffer);
            OutputStream outStream = new FileOutputStream(file);
            outStream.write(buffer);
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
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
    public boolean writeJsonToFile(File file, JSONObject data)
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
    public boolean writeXmlToFile(File file, Document data)
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
    public boolean writeByteArrayToFile(File file, byte[] data)
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
    public JSONObject readJsonFromFile(File file)
    {
        String res = readStringFromFile(file);

        try {
            return (res != null) ? new JSONObject(res) : null;

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
        String content = readStringFromFile(file);

        if (content != null) {

            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

                factory.setNamespaceAware(true);
                DocumentBuilder builder = factory.newDocumentBuilder();

                return builder.parse(new ByteArrayInputStream(content.getBytes()));

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


    /**
     * Returns real string path from URI
     * @param contentUri
     * @return String file path.
     */
    public static String getRealPathFromURI(Uri contentUri) throws ArrayIndexOutOfBoundsException, IllegalArgumentException
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return getPathForV19AndUp(contentUri);
        } else {
            return getPathForPreV19(contentUri);
        }
    }


    public static String getPathForPreV19(Uri contentUri) throws ArrayIndexOutOfBoundsException, IllegalArgumentException
    {
        String res = null;

        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = mContext.getContentResolver().query(contentUri, proj, null, null, null);

        try {
            if (cursor != null && cursor.moveToFirst()) {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                res = cursor.getString(column_index);
            }
            if (cursor != null) {
                cursor.close();
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return res;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static String getPathForV19AndUp(Uri contentUri) throws ArrayIndexOutOfBoundsException, IllegalArgumentException
    {
        String wholeID = DocumentsContract.getDocumentId(contentUri);

        // Split at colon, use second item in the array
        String id = wholeID.split(":")[1];
        String[] column = { MediaStore.Images.Media.DATA };

        // where id is equal to
        String sel = MediaStore.Images.Media._ID + "=?";
        Cursor cursor = mContext.getContentResolver().
                query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        column, sel, new String[]{ id }, null);

        String filePath = "";
        int columnIndex = cursor.getColumnIndex(column[0]);
        if (cursor.moveToFirst()) {
            filePath = cursor.getString(columnIndex);
        }

        cursor.close();
        return filePath;
    }



    //
    //  INNER CLASS
    //

    /**
     * Asynchronous task to move all storage files to selected storage.
     */
    public class MoveStorageTask extends AsyncTask<Void, Void, Boolean> {

        private Context context;
        private Exception lastException;
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
                this.from = externalStoragePath;
                this.to = internalStoragePath;
            } else {
                this.from = internalStoragePath;
                this.to = externalStoragePath;
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
                this.lastException = e;
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {

            if (result) {
                if (callbacks != null) {
                    callbacks.moveStorageEndsSuccess(this);

                    currentStorageID = storageID;

                    internalStoragePath = resolveInternalStorageString();
                    externalStoragePath = resolveExternalStorageString();

                    prefsManager.saveSelectedStorage(storageID);
                    prefsManager.saveStoragesConfiguration(getStoragesConfiguration());
                    prefsManager.saveLastUserAskedForChangeStorage(System.currentTimeMillis());
                }
            } else {
                if (callbacks != null) {
                    callbacks.moveStorageEndsError(this, lastException);
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
