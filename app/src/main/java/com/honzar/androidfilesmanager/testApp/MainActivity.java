package com.honzar.androidfilesmanager.testApp;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.honzar.androidfilesmanager.library.FilesManager;
import com.honzar.androidfilesmanager.library.FilesManagerPreferences;

public class MainActivity extends AppCompatActivity implements AskForStorageChangeDialog.IOnAskStorageMoveDialogListener {

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

                manager.moveStorageToExternal(new FilesManager.OptimalStorageMoveInterface() {
                    @Override
                    public void moveStorageStarts() {
                        Toast.makeText(mContext, getString(R.string.task_move_storage_start), Toast.LENGTH_LONG).show();
                    }
                    @Override
                    public void moveStorageEndsSuccess() {
                        Toast.makeText(mContext, getString(R.string.task_move_storage_end_success), Toast.LENGTH_LONG).show();
                    }
                    @Override
                    public void moveStorageEndsError() {
                        Toast.makeText(mContext, getString(R.string.task_move_storage_end_error), Toast.LENGTH_LONG).show();
                    }
                    @Override
                    public void moveStorageAlreadyDone() {
                        Toast.makeText(mContext, getString(R.string.task_move_storage_already_done), Toast.LENGTH_LONG).show();
                    }
                });

//                manager.writeObjectToFile(manager.getFile("test1"), new byte[] { (byte)0xe0});
//                manager.writeObjectToFile(manager.getFile("test2.txt"), "test");
//                try {
//                    manager.writeObjectToFile(manager.getFile("test3"), new JSONObject().put("juju", 2));
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
            }
        });

        manager = FilesManager.getInstance(mContext);
        if (!manager.checkOptimalStorageIsUsed()) {
            AskForStorageChangeDialog dialog = AskForStorageChangeDialog.newInstance(manager.getOptimalStorage());
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(dialog, AskForStorageChangeDialog.FRAGMENT_TAG);
            ft.commitAllowingStateLoss();
        }

        manager.createEmptyDir("slozka", "slozenka");
        manager.createEmptyFile("slozka/slozenka", "file4");

        Button btnTest1 = (Button) findViewById(R.id.btn_test_1);
        btnTest1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                manager.deleteStorage(FilesManagerPreferences.DEFAULT);
            }
        });

    }

    @Override
    public void onAskStorageMove(int storageId)
    {
        manager.moveStorageToOptimal(new FilesManager.OptimalStorageMoveInterface() {
            @Override
            public void moveStorageStarts() {
                Toast.makeText(mContext, getString(R.string.task_move_storage_start), Toast.LENGTH_LONG).show();
            }
            @Override
            public void moveStorageEndsSuccess() {
                Toast.makeText(mContext, getString(R.string.task_move_storage_end_success), Toast.LENGTH_LONG).show();
            }
            @Override
            public void moveStorageEndsError() {
                Toast.makeText(mContext, getString(R.string.task_move_storage_end_error), Toast.LENGTH_LONG).show();
            }
            @Override
            public void moveStorageAlreadyDone() {
                Toast.makeText(mContext, getString(R.string.task_move_storage_already_done), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
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
