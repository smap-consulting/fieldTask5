/*
 * Copyright (C) 2017 Smap Consulting Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package au.smap.fieldTask.services;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.odk.collect.android.R;
import org.odk.collect.android.application.Collect;
import org.odk.collect.settings.keys.ProjectKeys;
import au.smap.fieldTask.receivers.LocationReceiver;
import au.smap.fieldTask.notifications.SmapNotificationChannels;
import org.odk.collect.android.utilities.ApplicationConstants;

import androidx.core.app.NotificationCompat;
import timber.log.Timber;

/**
 * Created by neilpenman on 2018-01-11.
 */

/*
 * Get locations
 */
public class LocationService extends Service implements GoogleApiClient.ConnectionCallbacks {

    private Handler mHandler = new Handler(Looper.getMainLooper());  // smap - Handler for periodic checks
    private LocationRequest locationRequest;
    private FusedLocationProviderClient fusedLocationClient;
    private boolean isRecordingLocation = false;
    private static final long CHECK_INTERVAL_MS = 60000;  // smap - Check every 60 seconds
    private static final int LOCATION_SERVICE_NOTIFICATION_ID = 1;

    public LocationService() {
        super();
    }

    // smap - Runnable for periodic settings check (replaces Timer)
    private final Runnable checkSettingsRunnable = new Runnable() {
        @Override
        public void run() {
            Timber.i("=================== Periodic check for user settings");
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(Collect.getInstance());
            isRecordingLocation = sharedPreferences.getBoolean(ProjectKeys.KEY_SMAP_ENABLE_GEOFENCE, true);

            // Restart location monitoring - in case permission was disabled and then re-enabled
            stopLocationUpdates();
            requestLocationUpdates();

            // Schedule next check
            mHandler.postDelayed(this, CHECK_INTERVAL_MS);
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        super.onStartCommand(intent, flags, startId);
        Timber.i("======================= Start Location Service");

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(Collect.getInstance());
        isRecordingLocation = sharedPreferences.getBoolean(ProjectKeys.KEY_SMAP_ENABLE_GEOFENCE, true);

        // smap - Start periodic checks using Handler (replaces Timer)
        mHandler.post(checkSettingsRunnable);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createLocationRequest();
        requestLocationUpdates();

        // smap - CRITICAL FIX: Start foreground notification on API 26+ (was API 29+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Notification channel already created in Collect.onCreate() by SmapNotificationChannels
            Notification notification = new NotificationCompat.Builder(this, SmapNotificationChannels.LOCATION_TRACKING_CHANNEL_ID)
                    .setContentTitle(getString(org.odk.collect.strings.R.string.app_name))
                    .setContentText(getString(org.odk.collect.strings.R.string.location_tracking_active))
                    .setSmallIcon(org.odk.collect.icons.R.drawable.ic_baseline_location_on_24)
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .build();

            startForeground(LOCATION_SERVICE_NOTIFICATION_ID, notification, FOREGROUND_SERVICE_TYPE_LOCATION);
            Timber.i("Foreground service started for location tracking");
        }

        return START_STICKY;
    }

    @Override
    public void onConnected(Bundle bundle) {
        Timber.i("++++++++++Connected to provider");
        stopLocationUpdates();
        requestLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Timber.i("+++++++++++ Connection Suspended");
        stopLocationUpdates();
    }

    @Override
    public void onDestroy() {
        Timber.i("======================= Destroy Location Service");
        // smap - Stop periodic checks
        mHandler.removeCallbacks(checkSettingsRunnable);
        stopLocationUpdates();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /*
     * Methods to support location broadcast receiver
     */
    private void createLocationRequest() {
        locationRequest = new LocationRequest.Builder(ApplicationConstants.GPS_INTERVAL)
                .setMinUpdateIntervalMillis(ApplicationConstants.GPS_INTERVAL / 2)
                .setPriority( Priority.PRIORITY_HIGH_ACCURACY)
                .build();
    }

    private void requestLocationUpdates() {
        if(isRecordingLocation) {
            try {
                Timber.i("+++++++ Requesting location updates");
                fusedLocationClient.requestLocationUpdates(locationRequest, getPendingIntent());
            } catch (SecurityException e) {
                Timber.i("%%%%%%%%%%%%%%%%%%%% location recording not permitted: ");
            }
        } else {
            Timber.i("+++++++ Location updates disabled");
        }
    }

    private void stopLocationUpdates() {
        Timber.i("=================== Location Recording turned off");
        if(fusedLocationClient != null) {
            fusedLocationClient.removeLocationUpdates(getPendingIntent());
        }
    }

    private PendingIntent getPendingIntent() {
        Intent intent = new Intent(this, LocationReceiver.class);
        intent.setAction(LocationReceiver.ACTION_PROCESS_UPDATES);
        return PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

}
