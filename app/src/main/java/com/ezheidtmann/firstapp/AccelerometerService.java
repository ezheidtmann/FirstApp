package com.ezheidtmann.firstapp;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;

import java.util.ArrayList;

public class AccelerometerService extends Service implements SensorEventListener {
    public final Messenger mMessenger = new Messenger(new IncomingHandler());
    static final int MSG_REGISTER_CLIENT = 1;
    static final int MSG_UNREGISTER_CLIENT = 2;
    static final int MSG_ACCELEROMETER_UPDATE = 2;

    ArrayList<Messenger> mClients = new ArrayList<Messenger>();
    long mLastUpdateMillis = 0;

    SensorManager mSensorManager;
    Sensor mAccelerometer;

    public AccelerometerService() {
    }

    @Override
    public void onCreate() {
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public void onDestroy() {
        mSensorManager.unregisterListener(this, mAccelerometer);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor != mAccelerometer) {
            return;
        }
        if (mClients.size() == 0) {
            return;
        }
        long currentMillis = SystemClock.elapsedRealtime();
        if (currentMillis - mLastUpdateMillis < 1000.) {
            return;
        }
        mLastUpdateMillis = currentMillis;
        Bundle bundle = new Bundle();
        bundle.putFloat("x", event.values[0]);
        bundle.putFloat("y", event.values[1]);
        bundle.putFloat("z", event.values[2]);

        Message msg = Message.obtain(null, MSG_ACCELEROMETER_UPDATE);
        msg.setData(bundle);

        for (int i = mClients.size() - 1; i >= 0; --i) {
            try {
                mClients.get(i).send(msg);
            } catch (RemoteException e) {
                // dead client; safe to remove because we are decrementing i
                mClients.remove(i);
            }
        }
    }
}
