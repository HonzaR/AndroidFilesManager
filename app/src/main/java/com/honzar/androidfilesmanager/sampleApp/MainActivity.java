package com.honzar.androidfilesmanager.sampleApp;

import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.honzar.androidfilesmanager.library.FilesManager;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.StringReader;
import java.util.LinkedList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private Context mContext;
    private FilesManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        this.mContext = MainActivity.this;

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                manager.moveStorageToInternal(new FilesManager.OptimalStorageMoveInterface() {
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

                Log.d("read", "string: " + manager.readStringFromFile(manager.getFile("test2.txt")));
                Log.d("read", "json: " + manager.readJsonFromFile(manager.getFile("test3")));
                Log.d("read", "byte[]: " + manager.readByteArrayFromFile(manager.getFile("test1")));


            }
        });

        manager = FilesManager.getInstance(mContext);
        if (manager.checkIfStoragesChanged() || !manager.isOptimalStorageUsed()) {

            new AlertDialog.Builder(mContext)
                    .setTitle(getString(R.string.dialog_ask_storage_change_title))
                    .setMessage(getString(R.string.dialog_ask_storage_change_msg))
                    .setPositiveButton(R.string.dialog_ask_storage_change_move, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            onAskStorageMove(manager.getOptimalStorage());
                        }
                    })
                    .setNegativeButton(R.string.dialog_ask_storage_change_cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            onAskStorageCancel(manager.getOptimalStorage());
                        }
                    })
                    .show();
        }

        Button btnTest1 = (Button) findViewById(R.id.btn_test_1);
        btnTest1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                LinkedList<File> files = manager.getAllFilesFromDir(null);
                for (File f : files) {
                    Log.d("files", "file: " + f.getName());
                }
            }
        });

        Button btnTest2 = (Button) findViewById(R.id.btn_test_2);
        btnTest2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                manager.readXmlFromFile(manager.getFile("test5.xml"));
            }
        });

        manager.writeByteArrayToFile(manager.getFile("test1"), new byte[] { (byte)0xe0});
        manager.writeStringToFile(manager.getFile("test2.txt"), "test");
        try {
            manager.writeJsonToFile(manager.getFile("test3"), new JSONObject().put("juju", 2));
        } catch (JSONException e) {
            e.printStackTrace();
        }


        String xmlString = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
               "<note>\n" +
                "<to>Tove</to>\n" +
                "<from>Jani</from>\n" +
                "<heading>Reminder</heading>\n" +
                "<body>Don't forget me this weekend!</body>\n" +
                "</note>";
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try
        {
            builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xmlString)));
            manager.writeXmlToFile(manager.getFile("test5.xml"), document);
        } catch (Exception e) {
            e.printStackTrace();
        }

        manager.createEmptyDir(null, "renameDir");
        manager.createEmptyFile("renameDir", "a.txt");
        manager.renameFile("renameDir", "a.txt", "b.txt");

        manager.copyFile(Uri.fromFile(manager.getFile("renameDir", "b.txt")), "b.txt", null);

    }

    public void onAskStorageMove(int storageId)
    {
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
    }

    public void onAskStorageCancel(int storageId)
    {
        // nothing to do
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
