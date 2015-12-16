package com.ezheidtmann.firstapp;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.provider.SyncStateContract;
import android.support.v4.content.LocalBroadcastManager;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class IncrementerService extends IntentService {
    private static final String ACTION_COUNT_UP = "com.ezheidtmann.firstapp.action.COUNT_UP";

    private static final String STARTING_VALUE = "com.ezheidtmann.firstapp.extra.STARTING_VALUE";

    public static final String BROADCAST_ACTION = "com.ezheidtmann.firstapp.extra.BROADCAST";

    public static final String EXTENDED_DATA_STATUS = "com.ezheidtmann.firstapp.extra.STATUS";

    public static final String EXTRA_CURRENT_VALUE = "com.ezheidtmann.firstapp.extra.CURRENT_VALUE";

    public IncrementerService() {
        super("IncrementerService");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startCounting(Context context, Integer starting_value) {
        Intent intent = new Intent(context, IncrementerService.class);
        intent.setAction(ACTION_COUNT_UP);
        intent.putExtra(STARTING_VALUE, starting_value);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_COUNT_UP.equals(action)) {
                final int starting_value = intent.getIntExtra(STARTING_VALUE, 0);
                handleActionCountUp(starting_value);
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionCountUp(int starting_value) {
        for (int x = starting_value; x < starting_value + 100; ++x) {
            Intent localIntent = new Intent(BROADCAST_ACTION);
            localIntent.putExtra(EXTRA_CURRENT_VALUE, x);
            LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
            try {
                Thread.sleep(1000);
            } catch(InterruptedException e) {
                // pass
            }
        }
    }


}
