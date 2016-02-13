package com.ezheidtmann.firstapp;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;

public class TrackingService extends Service
        implements ConnectionCallbacks, OnConnectionFailedListener, LocationListener {
    public final Messenger mMessenger = new Messenger(new IncomingHandler());
    static final int MSG_REGISTER_CLIENT = 1;
    static final int MSG_UNREGISTER_CLIENT = 2;
    static final int MSG_DETECTED_ACTIVITY = 3;
    static final String TAG = "RRTrackingService";

    static final String ACTION_DETECTED_ACTIVITY = "com.ezheidtmann.firstapp.action.DETECTED_ACTIVITY";

    static final long ACTIVITY_DETECTION_DELAY_MILLIS = 10000L; // 10 seconds

    static final int MINIMUM_BIKING_CONFIDENCE = 20;
    static final long MAXIMUM_NOT_BIKING_MILLIS = 1200000L;
    static final long MINIMUM_NOT_BIKING_FOR_NOTIFICATION = 60000L;

    ArrayList<Messenger> mClients = new ArrayList<Messenger>();

    TrackingDebuggerDbHelper mDbHelper;

    private GoogleApiClient mGoogleApiClient;

    LocationRequest mLocationRequest;

    boolean mLocationTrackingEnabled = false;

    int mRecentBikingConfidence = 0;
    long mRecentBikingMillis = 0;

    SensorManager mSensorManager;
    Sensor mSigMotionSensor;
    TriggerEventListener mSigMotionListener = new TriggerEventListener() {
        @Override
        public void onTrigger(TriggerEvent event) {
            onSignificantMotionEvent(event);
        }
    };

    public TrackingService() {
        super();
    }

    @Override
    public void onCreate() {
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSigMotionSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION);
        if (mSigMotionSensor != null) {
            mSensorManager.requestTriggerSensor(mSigMotionListener, mSigMotionSensor);
        }

        mDbHelper = TrackingDebuggerDbHelper.getInstance(this);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(ActivityRecognition.API)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();

        Log.i(TAG, "created service");
    }

    @Override
    public void onDestroy() {
        mSensorManager.cancelTriggerSensor(mSigMotionListener, mSigMotionSensor);
        Log.i(TAG, "destroying service");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        // Connected to Google Play Services
        // TODO: it's possible that we don't have permission to monitor activities
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(
                mGoogleApiClient,
                ACTIVITY_DETECTION_DELAY_MILLIS,
                getActivityDetectionPendingIntent()
        );

        ensureLocationTrackingAsAppropriate();

        Log.i(TAG, "connected to play services, requesting updates");
    }

    public void startLocationTracking() {
        if (mLocationRequest == null) {
            mLocationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(1000)
                    .setFastestInterval(1000);
        }

        if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, "android.permission.ACCESS_FINE_LOCATION")) {
            Log.i(TAG, "startLocationTracking(): requesting location updates");
            // TODO: handle denied location permission properly
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient,
                    mLocationRequest,
                    this
            );
            mLocationTrackingEnabled = true;

        }
        else {
            Log.i(TAG, "startLocationTracking(): not requesting location updates");
        }
    }

    public void stopLocationTracking() {
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            Log.i(TAG, "stopLocationTracking(): stopping location tracking");
            mLocationTrackingEnabled = false;
        }
        else {
            Log.i(TAG, "stopLocationTracking(): not connected");
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection has been interrupted.
        // Disable any UI components that depend on Google APIs
        // until onConnected() is called.
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // This callback is important for handling errors that
        // may occur while attempting to connect with Google.
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_DETECTED_ACTIVITY.equals(action)) {
                handleDetectedActivity(intent);
            }
        }
        return START_STICKY; // keep the service alive
    }

    public void handleDetectedActivity(Intent intent) {
        ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
        ArrayList<DetectedActivity> activities = (ArrayList) result.getProbableActivities();

        TrackingDebuggerDbHelper dbHelper = TrackingDebuggerDbHelper.getInstance(this);
        long timestampMillis = System.currentTimeMillis();

        for (DetectedActivity activity: activities) {
            dbHelper.addDetectedActivity(timestampMillis, activity.getType(), activity.getConfidence());
        }

        ensureLocationTrackingAsAppropriate();
    }

    /**
     * Check to see if activity detection indicates we should either start location tracking
     * or we should stop tracking.
     */
    public void ensureLocationTrackingAsAppropriate() {
        long millis = System.currentTimeMillis();
        boolean isBiking = getIsBiking(millis);
        if (isBiking && !mLocationTrackingEnabled) {
            startLocationTracking();
        }
        else if (!isBiking && mLocationTrackingEnabled) {
            stopLocationTracking();
        }
    }

    public void updateIsBiking(long timeMillis, DetectedActivity activity) {
        if (activity.getType() == DetectedActivity.ON_BICYCLE && activity.getConfidence() >= MINIMUM_BIKING_CONFIDENCE) {
            mRecentBikingConfidence = activity.getConfidence();
            mRecentBikingMillis = timeMillis;
        }
    }

    public boolean getIsBiking(long timeMillis) {
        if (mRecentBikingMillis == 0) {
            // TODO: load from prefs, in the case that we are just loaded again. maybe do this in onCreate.
        }

        boolean isBiking = (getBikingAge(timeMillis)) <= MAXIMUM_NOT_BIKING_MILLIS;
        Log.i(TAG, "isBiking: " + (isBiking ? "true" : "false"));
        return isBiking;
    }

    public long getBikingAge(long timeMillis) {
        return timeMillis - mRecentBikingMillis;
    }

    /**
     * Get a PendingIntent that calls back to ourselves.
     *
     * @return
     */
    public PendingIntent getActivityDetectionPendingIntent() {
        Intent intent = new Intent(this, TrackingService.class);
        intent.setAction(ACTION_DETECTED_ACTIVITY);

        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public PendingIntent getLocationUpdatesPendingIntent() {
        Intent intent = new Intent(this, TrackingService.class);

        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public void onSignificantMotionEvent(TriggerEvent event) {
        mDbHelper.addSignificantMotionEvent(event.timestamp);
        //SpeakerIntentService.startSpeakAction(this, "significant motion event");

        // Re-attach so we get the next one
        mSensorManager.requestTriggerSensor(mSigMotionListener, mSigMotionSensor);
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(TAG, "got location from gplay");
        mDbHelper.addLocation(System.currentTimeMillis(), location);

        ensureLocationTrackingAsAppropriate();
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
                    break;
            }
        }
    }
}
