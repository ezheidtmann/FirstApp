package com.ezheidtmann.firstapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.util.Log;

/**
 * Created by evan on 2/8/16.
 */
public class TrackingDebuggerDbHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 3;
    public static final String DATABASE_NAME = "TrackingDebugger.db";
    public static final String TAG = "RRTrackingDB";

    public static final String SQL_CREATE_SIGMOTION_EVENT_2 = "" +
            "CREATE TABLE SigMotionEvent (" +
            "  timeMillis INTEGER NOT NULL" +
            ")";

    public static final String SQL_CREATE_DETECTED_ACTIVITY_2 = "" +
            "CREATE TABLE DetectedActivity (" +
            "  timeMillis INTEGER NOT NULL," +
            "  activityType INTEGER NOT NULL," +
            "  confidence INTEGER NOT NULL" +
            ")";

    public static final String SQL_CREATE_LOCATION_3 = "" +
            "CREATE TABLE Location (" +
            "  timeMillis INTEGER NOT NULL," +
            "  longitude REAL," +
            "  latitude REAL," +
            "  accuracy REAL," +
            "  altitude REAL," +
            "  bearing REAL," +
            "  speed REAL," +
            "  provider TEXT," +
            "  timestampMillis INTEGER," +
            "  isMock INTEGER" +
            ")";

    public static final String SQL_DROP_SIGMOTION_EVENT = "DROP TABLE IF EXISTS SigMotionEvent";
    public static final String SQL_DROP_DETECTED_ACTIVITY = "DROP TABLE IF EXISTS DetectedActivity";
    public static final String SQL_DROP_LOCATION = "DROP TABLE IF EXISTS Location";


    private static TrackingDebuggerDbHelper mInstance;

    public static TrackingDebuggerDbHelper getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new TrackingDebuggerDbHelper(context.getApplicationContext());
        }
        return mInstance;
    }

    private TrackingDebuggerDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_SIGMOTION_EVENT_2);
        db.execSQL(SQL_CREATE_DETECTED_ACTIVITY_2);
        db.execSQL(SQL_CREATE_LOCATION_3);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 2 && newVersion == 3) {
            db.execSQL(SQL_CREATE_LOCATION_3);
        }
        else {
            db.execSQL(SQL_DROP_SIGMOTION_EVENT);
            db.execSQL(SQL_DROP_DETECTED_ACTIVITY);
            db.execSQL(SQL_DROP_LOCATION);
            onCreate(db);
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public void addSignificantMotionEvent(long timeNanos) {
        long timeInMillis = System.currentTimeMillis()
                + (timeNanos - System.nanoTime()) / 1000000L;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("timeMillis", timeInMillis);
        db.insert("SigMotionEvent", null, values);
        Log.i(TAG, "sig motion event");
    }

    public void addDetectedActivity(long timeMillis, int activityType, int confidence) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("timeMillis", timeMillis);
        values.put("activityType", activityType);
        values.put("confidence", confidence);
        db.insert("DetectedActivity", null, values);
        Log.i(TAG, "detected activity: " + activityType + ", " + confidence + "%");
    }

    public void addLocation(long timeMillis, Location location) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("timeMillis", timeMillis);
        values.put("longitude", location.getLongitude());
        values.put("latitude", location.getLatitude());
        values.put("altitude", location.getAltitude());
        values.put("accuracy", location.getAccuracy());
        values.put("bearing", location.getBearing());
        values.put("speed", location.getSpeed());
        values.put("timestampMillis", location.getTime());
        values.put("provider", location.getProvider());
        values.put("isMock", location.isFromMockProvider() ? 1 : 0);
        db.insert("Location", null, values);
        Log.i(TAG, "location updated: " + location.getProvider());
    }

    public Cursor getSigMotionEventsCursor(long beforeCutoffMillis) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] fields = { "timeMillis" };
        String whereClause = "timeMillis <= ?";
        String[] whereArgs = { Long.toString(beforeCutoffMillis) };
        return db.query("SigMotionEvent", fields, whereClause, whereArgs, null, null, null);
    }

    public Cursor getDetectedActivitesCursor(long beforeCutoffMillis) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] fields = {"timeMillis", "activityType", "confidence"};
        String whereClause = "timeMillis <= ?";
        String[] whereArgs = {Long.toString(beforeCutoffMillis)};
        return db.query("DetectedActivity", fields, whereClause, whereArgs, null, null, null);
    }

    public Cursor getLocationCursor(long beforeCutoffMillis) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] fields = {
                "timeMillis",
                "longitude",
                "latitude",
                "altitude",
                "accuracy",
                "bearing",
                "speed",
                "timestampMillis",
                "provider",
                "isMock"
        };
        String whereClause = "timeMillis <= ?";
        String[] whereArgs = { Long.toString(beforeCutoffMillis) };
        return db.query("Location", fields, whereClause, whereArgs, null, null, null);
    }

    public void deleteDataBeforeCutoff(long cutoffMillis) {
        SQLiteDatabase db = this.getWritableDatabase();
        String[] args = { Long.toString(cutoffMillis) };
        db.delete("DetectedActivity", "timeMillis <= ?", args);
        db.delete("SigMotionEvent", "timeMillis <= ?", args);
        db.delete("Location", "timeMillis <= ?", args);
    }
}
