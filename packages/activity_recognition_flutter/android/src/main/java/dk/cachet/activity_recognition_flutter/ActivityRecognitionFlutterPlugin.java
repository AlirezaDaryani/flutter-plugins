package dk.cachet.activity_recognition_flutter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.HashMap;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.EventChannel;

/**
 * ActivityRecognitionFlutterPlugin
 */
@SuppressLint("LongLogTag")
public class ActivityRecognitionFlutterPlugin implements FlutterPlugin, EventChannel.StreamHandler, ActivityAware, SharedPreferences.OnSharedPreferenceChangeListener {
    private EventChannel channel;
    private EventChannel.EventSink eventSink;
    private Activity androidActivity;
    private Context androidContext;
    public static final String DETECTED_ACTIVITY = "detected_activity";
    public static final String ACTIVITY_RECOGNITION = "activity_recognition_flutter";

    private final String TAG = "activity_recognition_flutter";

    /**
     * The main function for starting activity tracking.
     * Handling events is done inside [ActivityRecognizedService]
     */
    private void startActivityTracking(int appFrequency) {
        // Start the service
        Intent intent = new Intent(androidActivity, ActivityRecognizedBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(androidActivity, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Frequency in milliseconds
        long frequency = appFrequency * 1000;
        Task<Void> task = ActivityRecognition.getClient(androidContext)
                .requestActivityUpdates(frequency, pendingIntent);

        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void e) {
                Log.d(TAG, "ActivityRecognition: onSuccess");
            }
        });
        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "ActivityRecognition: onFailure");
            }
        });
    }

    /**
     * EventChannel.StreamHandler interface below
     */

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        channel = new EventChannel(flutterPluginBinding.getBinaryMessenger(), ACTIVITY_RECOGNITION);
        channel.setStreamHandler(this);
    }

    // Unchecked HashMap cast. Using instanceof does not clear the warning.
    @SuppressWarnings("unchecked")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onListen(Object arguments, EventChannel.EventSink events) {
        HashMap<String, Object> args = (HashMap<String, Object>) arguments;
        /**
         {
         "foreground":true,
         "notification_title":"Some string",
         "notification_desc":"Some string",
         "detection_frequency":10 //in seconds
         }
         */
        Log.d(TAG, "args: " + args);
        boolean fg = false;
        if (args.get("foreground") != null) {
            fg = (boolean) args.get("foreground");
        }
        Log.d(TAG, "foreground: " + fg);
        if (fg) {
            String title = "MonsensoMonitor";
            String desc = "Monsenso Foreground Service";
            if (args.get("notification_title") != null) {
                title = (String) args.get("notification_title");
            }
            if (args.get("notification_desc") != null) {
                desc = (String) args.get("notification_desc");
            }


            startForegroundService(title, desc);
        }
        int appFrequency = 5;
        if (args.get("detection_frequency") != null) {
            appFrequency = (int) args.get("detection_frequency");
        }


        eventSink = events;
        startActivityTracking(appFrequency);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    void startForegroundService(String notificationTitle, String notificationDescription) {
        Intent intent = new Intent(androidActivity, ForegroundService.class);

        // Tell the service we want to start it
        intent.setAction("start");

        // Pass the notification title/text/icon to the service
        intent.putExtra("title", notificationTitle)
                .putExtra("text", notificationDescription)
                .putExtra("icon", R.drawable.common_full_open_on_phone)
                .putExtra("importance", 3)
                .putExtra("id", 10);

        // Start the service
        androidContext.startForegroundService(intent);
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setStreamHandler(null);
    }

    @Override
    public void onCancel(Object arguments) {
        channel.setStreamHandler(null);
    }

    /**
     * ActivityAware interface below
     */
    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        androidActivity = binding.getActivity();
        androidContext = binding.getActivity().getApplicationContext();

        SharedPreferences prefs = androidContext.getSharedPreferences(ACTIVITY_RECOGNITION, Context.MODE_PRIVATE);
        prefs.registerOnSharedPreferenceChangeListener(this);
        Log.d(TAG, "onAttachedToActivity");
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        androidActivity = null;
        androidContext = null;
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        androidActivity = binding.getActivity();
        androidContext = binding.getActivity().getApplicationContext();

    }

    @Override
    public void onDetachedFromActivity() {
        androidActivity = null;
        androidContext = null;
    }

    /**
     * Shared preferences changed, i.e. latest activity
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        String result = sharedPreferences
                .getString(DETECTED_ACTIVITY, "error");
        Log.d("onSharedPreferenceChange", result);
        if (key != null && key.equals(DETECTED_ACTIVITY)) {
            Log.d(TAG, "Detected activity: " + result);
            try {
                eventSink.success(result);
                Intent i = new Intent();
                i.setAction(Intent.ACTION_MAIN);
                i.addCategory(Intent.CATEGORY_HOME);
                androidContext.startActivity(i);
            } catch (Exception e) {
                Log.e(TAG, "onSharedPreferenceChanged: ", e);
            }
        }
    }
  }
