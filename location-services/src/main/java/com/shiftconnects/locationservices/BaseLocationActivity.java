/*
 * Copyright (C) 2015 P100 OG, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.shiftconnects.locationservices;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.graphics.Rect;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationManager;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.LocationRequest;

import java.util.List;


/**
 * Base activity that should be extended if sub activities want location data. This activity handles
 * binding to Google Play Services and also manages the LocationClient. To make location requests,
 * subclasses should call #onLocationServicesConnected passing in a {@link com.google.android.gms.location.LocationRequest}.
 * Subclasses will then receive location updates in #onLocationChanged
 */
public abstract class BaseLocationActivity extends ActionBarActivity implements GpsStatus.Listener,
        BackgroundLocationService.ConnectionCallbacks, BackgroundLocationService.LocationCallbacks,
        BackgroundLocationService.DataCallback {

    private static final String TAG = BaseLocationActivity.class.getSimpleName();
    private static final String KEY_IN_RESOLUTION = "is_in_resolution";

    /**
     * Request code for auto Google Play Services error resolution.
     */
    protected static final int REQUEST_CODE_RESOLUTION = 1;

    private LocationManager mLocationManager;
    private boolean mLocationEnabled;

    private BackgroundLocationService mBackgroundLocationManager;
    private boolean mIsInResolution;
    private boolean mShouldRetryConnecting;

    private ServiceConnection mLocationManagerConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "Background location services connected");
            mBackgroundLocationManager = ((BackgroundLocationService.LocalBinder)service).getBackgroundLocationService();
            mBackgroundLocationManager.addConnectionCallbacks(BaseLocationActivity.this);
            mBackgroundLocationManager.addLocationCallbacks(BaseLocationActivity.this);
            mBackgroundLocationManager.setDataCallback(BaseLocationActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBackgroundLocationManager = null;
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mIsInResolution = savedInstanceState.getBoolean(KEY_IN_RESOLUTION, false);
        }
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_IN_RESOLUTION, mIsInResolution);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkLocationEnabled();
        if (!mLocationEnabled) {
            showLocationDisabledDialogFragment();
        }
    }

    public void onNewLocation(Location location) {
        Log.i(TAG, "New location.");
        onLocationChanged(location);
    }

    public void onGeofenceError(GeofencingEvent event) {
        Log.i(TAG, "Geofence error.");
    }

    public void onGeofencesSetupSuccessful() {
        Log.i(TAG, "Geofence setup.");
    }
    public void onGeofencesSetupUnsuccessful(Status status) {
        Log.i(TAG, "Geofence setup failed.");
    }
    public void onLocationServicesConnectionSuspended(int cause) {
        Log.i(TAG, "Location services connection suspended.");
    }

    protected void requestLocationUpdates(LocationRequest request) {
        mBackgroundLocationManager.requestUpdates(request);
    }

    /**
     * This method will set up the background location tracking service.
     * Normally, this can be called in onResume, for example.
     */
    protected void bindLocationManager() {
        bindService(new Intent(this, BackgroundLocationService.class), mLocationManagerConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * This method will remove the location updates. Call it when you no longer care about location tracking.
     * Normally this can be called in onPause.
     */
    protected void unbindLocationManager() {
        if (mBackgroundLocationManager != null) {
            mBackgroundLocationManager.removeLocationUpdates();
            mBackgroundLocationManager.removeConnectionCallbacks(this);
            mBackgroundLocationManager.removeLocationCallbacks(this);
            unbindService(mLocationManagerConnection);
            mBackgroundLocationManager = null;
        }
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        mLocationManager.removeGpsStatusListener(this);
    }

    /**
     * Called when the location client has successfully connected. When this is called it is safe to request
     * location updates.
     */
    public abstract void onLocationServicesAvailable();

    @Override
    public abstract void onLocationChanged(Location location);

    private void checkLocationEnabled() {
        mLocationEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public abstract void showLocationDisabledDialogFragment();

    public abstract void removeLocationDisabledDialogFragment();

    @Override public void onGpsStatusChanged(int event) {
        switch (event) {
            case GpsStatus.GPS_EVENT_STARTED:
                Log.d(TAG, "GPS has started.");
                removeLocationDisabledDialogFragment();
                break;
            case GpsStatus.GPS_EVENT_STOPPED:
                Log.d(TAG, "GPS has stopped.");
                checkLocationEnabled();
                if (mBackgroundLocationManager.getGoogleApiClient().isConnected() && !mLocationEnabled) {
                    Log.d(TAG, "Disconnecting location client");
                    showLocationDisabledDialogFragment();
                }
                break;
        }
    }

    @Override
    public final void onLocationServicesConnectionSuccessful() {
        onLocationServicesAvailable();
    }

    @Override
    public final void onLocationServicesConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
        if (!result.hasResolution()) {
            mShouldRetryConnecting = true;
            // Show a localized error dialog.
            GooglePlayServicesUtil.getErrorDialog(
                    result.getErrorCode(), this, 0, new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            retryConnecting();
                        }
                    }).show();
            return;
        }
        // If there is an existing resolution error being displayed or a resolution
        // activity has started before, do nothing and wait for resolution
        // progress to be completed.
        if (mIsInResolution) {
            return;
        }
        mIsInResolution = true;
        try {
            result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
            retryConnecting();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mShouldRetryConnecting) {
            retryConnecting();
        } else {
            switch (requestCode) {
                case REQUEST_CODE_RESOLUTION:
                    retryConnecting();
                    break;
            }
        }
    }

    private void retryConnecting() {
        mIsInResolution = false;
        mShouldRetryConnecting = false;
        if (mBackgroundLocationManager != null) {
            mBackgroundLocationManager.onConnectionResolved();
        }
    }
}