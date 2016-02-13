package com.ezheidtmann.firstapp;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class SpeakerIntentService extends IntentService {

    public static final String TAG = "RRSpeaker";

    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_SPEAK = "com.ezheidtmann.firstapp.action.SPEAK";
    private static final String ACTION_SPEECH_ENABLE = "com.ezheidtmann.firstapp.action.SPEECH_ENABLE";

    // TODO: Rename parameters
    private static final String SPEAK_TEXT = "com.ezheidtmann.firstapp.extra.SPEAK_TEXT";

    public SpeakerIntentService() {
        super("SpeakerIntentService");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startSpeakAction(Context context, String text) {
        SharedPreferences prefs = context.getSharedPreferences(TAG, Context.MODE_PRIVATE);

        if (!prefs.getBoolean("speechEnabled", true)) {
            return;
        }

        Intent intent = new Intent(context, SpeakerIntentService.class);
        intent.setAction(ACTION_SPEAK);
        intent.putExtra(SPEAK_TEXT, text);
        context.startService(intent);
    }

    public static void setSpeechEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(TAG, Context.MODE_PRIVATE);
        if (prefs.getBoolean("speechEnabled", true) != enabled) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("speechEnabled", enabled);
            editor.apply();
            //Log.d(TAG, "set speechEnabled: " + (enabled ? "true", "false"));
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_SPEAK.equals(action)) {
                final String text = intent.getStringExtra(SPEAK_TEXT);
                handleSpeakAction(text);
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleSpeakAction(String text) {
        final boolean[] ttsOk = new boolean[1];
        final CountDownLatch ttsInitLatch = new CountDownLatch(1);

        TextToSpeech tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                ttsOk[0] = (status == TextToSpeech.SUCCESS);
                ttsInitLatch.countDown();
            }
        });

        try {
            ttsInitLatch.await();
            if (!ttsOk[0]) {
                return;
            }

            final CountDownLatch ttsSpeechLatch = new CountDownLatch(1);
            String language = "en";
            Locale speechLocale = new Locale(language);
            int langStatus = tts.setLanguage(speechLocale);
            // TODO: handle langStatus
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    // Ignore
                }

                @Override
                public void onDone(String utteranceId) {
                    ttsSpeechLatch.countDown();
                }

                @Override
                public void onError(String utteranceId) {
                    Log.e(TAG, "tts.onError()");
                    ttsSpeechLatch.countDown();
                }
            });

            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utterance");
            ttsSpeechLatch.await();
        }
        catch (InterruptedException e) {
            Log.wtf(TAG, "Interrupted while trying to speak");
        }
        catch (Exception e) {
            Log.wtf(TAG, "Other exception while trying to speak");
        }
        finally {
            // This is required to avoid leaks
            tts.shutdown();
        }
    }

}
