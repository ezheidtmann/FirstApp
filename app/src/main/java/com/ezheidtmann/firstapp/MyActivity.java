package com.ezheidtmann.firstapp;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;


public class MyActivity extends AppCompatActivity {

    public final static String EXTRA_MESSAGE = "com.ezheidtmann.firstapp.MESSAGE";

    MyReceiver mReceiver;

    public MyActivity() {
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

//        IntentFilter intentFilter = new IntentFilter(IncrementerService.BROADCAST_ACTION);
//        mReceiver = new MyReceiver();
//        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, intentFilter);
//
//        IncrementerService.startCounting(this, 3);
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
        bindService(new Intent(this, AccelerometerService.class), mConnection, Context.BIND_AUTO_CREATE);
        super.onResume();
    }

    protected void onPause() {
        unbindService(mConnection);
        super.onPause();
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void updateAccelerometerDisplay(float x, float y, float z) {
        TextView tv_x = (TextView) findViewById(R.id.accel_x);
        tv_x.setText(Float.toString(x));

        TextView tv_y = (TextView) findViewById(R.id.accel_y);
        tv_y.setText(Float.toString(y));

        TextView tv_z = (TextView) findViewById(R.id.accel_z);
        tv_z.setText(Float.toString(z));
    }

    class IncomingHandler extends android.os.Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case AccelerometerService.MSG_ACCELEROMETER_UPDATE:
                    Bundle data = msg.getData();
                    updateAccelerometerDisplay(data.getFloat("x"), data.getFloat("y"), data.getFloat("z"));
                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    }

    final Messenger mMessenger = new Messenger(new IncomingHandler());
    Messenger mService = null;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = new Messenger(service);
            try {
                Message msg = Message.obtain(null, AccelerometerService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
                // service crashed or not available . TODO
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };


}
