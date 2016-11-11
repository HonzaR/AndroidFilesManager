# Android Files Manager

[![](https://jitpack.io/v/HonzaR/AndroidFilesManager.svg)](https://jitpack.io/#HonzaR/AndroidFilesManager)

Android Library for work with storages. Work with Internal and primary External storages are transparent with this library.
Library also contains bunch of public methods for work with files on storages:

    getInstance
    getFile
    getFile
    getFile
    getFile
    getAllFilesFromDir
    getAllFilesFromDir
    getAllFileFromExternalStorage
    getAllFilesFromInternalStorage
    getAllFileFromCurrentStorage
    copyFile
    copyFile
    checkFileExists
    createEmptyFile
    createEmptyFile
    deleteFile
    deleteFile
    createEmptyDir
    createEmptyDir
    deleteDir
    renameDirectory
    getCurrentStorage
    getOptimalStorage
    deleteStorage
    moveStorageToExternal
    moveStorageToInternal
    moveStorageToOptimal
    checkIfStoragesChanged
    isOptimalStorageUsed
    isExternalStorageWritable
    isExternalStorageReadable
    getCurrentStorageFreeSpace
    getExternalStorageFreeSpace
    getInternalStorageFreeSpace
    writeStringToFile
    writeJsonToFile
    writeXmlToFile
    writeByteArrayToFile
    readStringFromFile
    readJsonFromFile
    readXmlFromFile
    readByteArrayFromFile


## How to Use
```
        AdroidFilesManager manager = FilesManager.getInstance(mContext);

        manager.writeByteArrayToFile(manager.getFile("test1"), new byte[] { (byte)0xe0});
        manager.writeStringToFile(manager.getFile("test2.txt"), "test");

        manager.moveStorageToOptimal(new FilesManager.OptimalStorageMoveInterface() {
            @Override
            public void moveStorageStarts(FilesManager.MoveStorageTask task) {
                Toast.makeText(mContext, getString(R.string.task_move_storage_start), Toast.LENGTH_LONG).show();
            }
            @Override
            public void moveStorageEndsSuccess(FilesManager.MoveStorageTask task) {
                Toast.makeText(mContext, getString(R.string.task_move_storage_end_success), Toast.LENGTH_LONG).show();
            }
            @Override
            public void moveStorageEndsError(FilesManager.MoveStorageTask task, Exception e) {
                Toast.makeText(mContext, getString(R.string.task_move_storage_end_error), Toast.LENGTH_LONG).show();
            }
            @Override
            public void moveStorageAlreadyDone() {
               Toast.makeText(mContext, getString(R.string.task_move_storage_already_done), Toast.LENGTH_LONG).show();
            }
        });
```

## Integration
This library is hosted by jitpack.io.

Root level gradle:
```
allprojects {
 repositories {
    jcenter()
    maven { url "https://jitpack.io" }
 }
}
```

Application level gradle:
```
dependencies {
    compile 'com.github.HonzaR:AndroidFilesManager:x.y.z'
}
```
Note: do not add the jitpack.io repository under buildscript

## Proguard
```
-keep class com.honzar.androidfilesmanager.library.** {*;}
```
