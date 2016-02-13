package com.ezheidtmann.firstapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;


public class MyActivity extends AppCompatActivity {

    public final static String EXTRA_MESSAGE = "com.ezheidtmann.firstapp.MESSAGE";

    MyReceiver mReceiver;

    SharedPreferences mPrefs;

    public MyActivity() {
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPrefs = this.getPreferences(Context.MODE_PRIVATE);

        setContentView(R.layout.activity_my);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Do a thing with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

            }
        });

        CheckBox checkBoxSpeech = (CheckBox) findViewById(R.id.checkBoxSpeech);
        checkBoxSpeech.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SpeakerIntentService.setSpeechEnabled(buttonView.getContext(), isChecked);
            }
        });

        Button uploadButton = (Button) findViewById(R.id.buttonSync);
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TrackingDebuggerUploadIntentService.startUpload(v.getContext(), System.currentTimeMillis());
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_my, menu);
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

    public class MyReceiver extends BroadcastReceiver {
        public MyReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
//            int value = intent.getIntExtra(IncrementerService.EXTRA_CURRENT_VALUE, 0);
//            TextView tv = (TextView) findViewById(R.id.counter_display);
//            tv.setText(Integer.toString(value));
        }
    }

    protected void onResume() {
        startService(new Intent(this, TrackingService.class));
        super.onResume();
    }

    protected void onPause() {
        super.onPause();
    }
}
