package dk.cachet.activity_recognition_flutter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

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
    public static final String DETECTED_ACTIVITY_CURRENT = "detected_activity_current";
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

    private static Intent getLaunchIntent(Context context) {
        String packageName = context.getPackageName();
        PackageManager packageManager = context.getPackageManager();
        return packageManager.getLaunchIntentForPackage(packageName);
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
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        //channel.setStreamHandler(null);
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        androidActivity = binding.getActivity();
        androidContext = binding.getActivity().getApplicationContext();

    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        //androidActivity = null;
        //androidContext = null;
    }

    @Override
    public void onDetachedFromActivity() {
        //androidActivity = null;
        //androidContext = null;
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "NotiPark";
            String description = "NotiPark Reminders";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("notipark_channel", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = androidContext.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private static int getDrawableResourceId(Context context, String name) {
        return context.getResources().getIdentifier(name, "drawable", context.getPackageName());
    }

    void notifyNotification(boolean isStopped) {
        createNotificationChannel();
        Intent intent = getLaunchIntent(androidContext);
        intent.setAction("SELECT_NOTIFICATION");
        intent.putExtra("payload", isStopped ? "STOPPED" : "MOVING");
        PendingIntent pendingIntent = PendingIntent.getActivity(androidContext, 909090, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(androidContext, "notipark_channel")
                .setSmallIcon(getDrawableResourceId(androidContext, "app_icon"))
                .setContentTitle("Parking")
                .setContentText(isStopped ? "You just stopped" : "You start moving")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_MAX);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(androidContext);

// notificationId is a unique int for each notification that you must define
        notificationManager.notify(909090, builder.build());


    }

    void sendMessage(String data) {
        String[] tokens = data.split(",");
        //todo now work for walk and stop
        if (tokens.length == 2) {
            if (tokens[0].equals("STILL")) {
                notifyNotification(true);
            } else if (tokens[0].equals("IN_VEHICLE")) {
                notifyNotification(false);
            }
        }


    }


    /**
     * Shared preferences changed, i.e. latest activity
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        String result = sharedPreferences
                .getString(DETECTED_ACTIVITY, "error");
        //String last = sharedPreferences
        //.getString(DETECTED_ACTIVITY_CURRENT, "");
        Log.d("onSharedPreferenceChange", result);
//        Log.e(TAG, "onSharedPreferenceChanged: " + result);
//        Log.e(TAG, "onSharedPreferenceChanged: " + last);
//
//        //todo compare new with latest and if there are different, show new one
//        if (last.equals("") || !result.equals(last)) {
//            Log.e(TAG, "onSharedPreferenceChanged: We will show this and save it again");
//            sharedPreferences.edit().putString(DETECTED_ACTIVITY_CURRENT, result).apply();
//        }
        if (key != null && key.equals(DETECTED_ACTIVITY)) {
            Log.d(TAG, "Detected activity: " + result);
            try {
                eventSink.success(result);
                // sendMessage(result);
            } catch (Exception e) {
                Log.e(TAG, "onSharedPreferenceChanged: ", e);
            }
        }
    }
  }
