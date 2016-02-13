package com.ezheidtmann.firstapp;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.CountDownLatch;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class TrackingDebuggerUploadIntentService extends IntentService {
    private static final String TAG = "RRUpload";
    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_UPLOAD_FROM_DB = "com.ezheidtmann.firstapp.action.UPLOAD_FROM_DB";

    // TODO: Rename parameters
    private static final String CUTOFF_MILLIS = "com.ezheidtmann.firstapp.extra.CUTOFF_MILLIS";

    public TrackingDebuggerUploadIntentService() {
        super("TrackingDebuggerUploadIntentService");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startUpload(Context context, long timeMillis) {
        Intent intent = new Intent(context, TrackingDebuggerUploadIntentService.class);
        intent.setAction(ACTION_UPLOAD_FROM_DB);
        intent.putExtra(CUTOFF_MILLIS, timeMillis);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_UPLOAD_FROM_DB.equals(action)) {
                final long millis = intent.getLongExtra(CUTOFF_MILLIS, 0L);
                doUploadFromDb(millis);
            }
        }
    }

    private void doUploadFromDb(final long cutoffMillis) {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url ="https://api.ride.report/api/v2/android_debugging_data";

        // Assemble JSON object of information from database up to timeMillis cutoff
        JSONObject content = new JSONObject();

        final TrackingDebuggerDbHelper dbHelper = TrackingDebuggerDbHelper.getInstance(this);

        try {
            Cursor cursor = dbHelper.getSigMotionEventsCursor(cutoffMillis);

            JSONArray sigMotionMillis = new JSONArray();
            try {
                if (cursor.moveToFirst()) {
                    do {
                        sigMotionMillis.put(cursor.getLong(0));
                    } while (cursor.moveToNext());
                }
            }
            finally {
                cursor.close();
            }
            content.put("sigMotionMillis", sigMotionMillis);

            JSONArray detectedActivities = new JSONArray();
            try {
                cursor = dbHelper.getDetectedActivitesCursor(cutoffMillis);
                if (cursor.moveToFirst()) {
                    do {
                        JSONObject entry = new JSONObject();
                        entry.put("timeMillis", cursor.getLong(cursor.getColumnIndexOrThrow("timeMillis")));
                        entry.put("activityType", cursor.getInt(cursor.getColumnIndexOrThrow("activityType")));
                        entry.put("confidence", cursor.getInt(cursor.getColumnIndexOrThrow("confidence")));
                        detectedActivities.put(entry);
                    } while (cursor.moveToNext());
                }
            }
            finally {
                cursor.close();
            }
            content.put("detectedActivities", detectedActivities);

            JSONArray locations = new JSONArray();
            try {
                cursor = dbHelper.getLocationCursor(cutoffMillis);
                if (cursor.moveToFirst()) {
                    do {
                        JSONObject entry = new JSONObject();
                        entry.put("longitude", cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")));
                        entry.put("latitude", cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")));
                        entry.put("timeMillis", cursor.getLong(cursor.getColumnIndexOrThrow("timeMillis")));
                        entry.put("altitude", cursor.getDouble(cursor.getColumnIndexOrThrow("altitude")));
                        entry.put("accuracy", cursor.getDouble(cursor.getColumnIndexOrThrow("accuracy")));
                        entry.put("bearing", cursor.getDouble(cursor.getColumnIndexOrThrow("bearing")));
                        entry.put("speed", cursor.getDouble(cursor.getColumnIndexOrThrow("speed")));
                        entry.put("timestampMillis", cursor.getLong(cursor.getColumnIndexOrThrow("timestampMillis")));
                        entry.put("provider", cursor.getString(cursor.getColumnIndexOrThrow("provider")));
                        entry.put("isMock", cursor.getShort(cursor.getColumnIndexOrThrow("isMock")));
                        locations.put(entry);
                    } while (cursor.moveToNext());
                }
            }
            finally {
                cursor.close();
            }
            content.put("locations", locations);

            content.put("cutoffMillis", cutoffMillis);
        }
        catch (JSONException e) {
            Log.wtf(TAG, "JSONException while building JSON for upload");
            return;
        }

        final Context self = (Context) this;

        final CountDownLatch requestLatch = new CountDownLatch(1);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, content, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                TrackingDebuggerDbHelper dbHelper1 = TrackingDebuggerDbHelper.getInstance(TrackingDebuggerUploadIntentService.this);
                // delete old data from database
                dbHelper1.deleteDataBeforeCutoff(cutoffMillis);
                Toast.makeText(self, "success", Toast.LENGTH_SHORT).show();
                requestLatch.countDown();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Error response from server");
                Toast.makeText(self, "failure", Toast.LENGTH_SHORT).show();
                requestLatch.countDown();
            }
        });

        queue.add(request);

        Toast.makeText(self, "Queued request", Toast.LENGTH_SHORT).show();

        try {
            requestLatch.await();
        }
        catch (InterruptedException e) {
            // do nothing
        }
    }
}

