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

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;

/**
 * Sets up a location service with callbacks for interested parties.
 *
 * To use, simply place the following into your ApplicationManifest somewhere in the <application> tag.
 *
 * <service android:name=".service.ConsumerLocationManager"
 *          android:exported="false">
 *
 *          <intent-filter>
 *              <action android:name="com.shiftconnects.android.ACTION_GEOFENCE_TRANSITION"/>
 *          </intent-filter>
 *
 * </service>
 *
 * Created by mattkranzler on 4/28/14. Updated by mattruno.
 */
public class BackgroundLocationService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String ACTION_GEOFENCE_TRANSITION = "com.shiftconnects.android.ACTION_GEOFENCE_TRANSITION";

    private final IBinder mBinder = new LocalBinder();

    public static interface DataCallback {
        void onNewLocation(Location location);
        void onGeofenceError(GeofencingEvent event);
        void onGeofencesSetupSuccessful();
        void onGeofencesSetupUnsuccessful(Status status);
        void onLocationServicesConnectionFailed(ConnectionResult connectionResult);
        void onLocationServicesConnectionSuspended(int cause);
    }

    public static interface LocationCallbacks {
        void onLocationChanged(Location location);
    }

    public static interface ConnectionCallbacks {
        void onLocationServicesConnectionSuccessful();
        void onLocationServicesConnectionFailed(ConnectionResult connectionResult);
    }

    public static interface GeofenceCallbacks {
        void onGeofenceEntered(String geofenceId);
        void onGeofenceDwelled(String geofenceId);
        void onGeofenceExited(String geofenceId);
    }

    private static final String TAG = BackgroundLocationService.class.getSimpleName();

    private GoogleApiClient mGoogleApiClient;

    private DataCallback mDataCallback;
    private List<LocationCallbacks> mLocationCallbacks;
    private List<ConnectionCallbacks> mConnectionCallbacks;
    private List<GeofenceCallbacks> mGeofenceCallbacks;

    private Location mLastLocation;
    private ConnectionResult mFailedConnectionResult;

    @Override public void onCreate() {
        Log.d(TAG, "Service created.");
        super.onCreate();
        mLocationCallbacks = new ArrayList<>();
        mGeofenceCallbacks = new ArrayList<>();
        mConnectionCallbacks = new ArrayList<>();
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        mGoogleApiClient.connect();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleCommand(intent);
        return START_NOT_STICKY;
    }

    private void handleCommand(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            Log.d(TAG, "Received an intent with action [" + action + "]");
            if (TextUtils.equals(ACTION_GEOFENCE_TRANSITION, action)) {
                GeofencingEvent event = GeofencingEvent.fromIntent(intent);
                if (event.hasError()) {
                    Log.w(TAG, "Received a geofence event with an error!");
                    if( null != mDataCallback ){
                        mDataCallback.onGeofenceError(event);
                    }
                } else {
                    switch (event.getGeofenceTransition()) {
                        case Geofence.GEOFENCE_TRANSITION_ENTER:
                            Log.d(TAG, "Received a geofence ENTER event");
                            for (Geofence geofence : event.getTriggeringGeofences()) {
                                notifyCallbacksOnGeofenceEntered(geofence.getRequestId());
                            }
                            break;
                        case Geofence.GEOFENCE_TRANSITION_DWELL:
                            Log.d(TAG, "Received a geofence DWELL event");
                            for (Geofence geofence : event.getTriggeringGeofences()) {
                                notifyCallbacksOnGeofenceDwelled(geofence.getRequestId());
                            }
                            break;
                        case Geofence.GEOFENCE_TRANSITION_EXIT:
                            Log.d(TAG, "Received a geofence EXIT event");
                            for (Geofence geofence : event.getTriggeringGeofences()) {
                                notifyCallbacksOnGeofenceExited(geofence.getRequestId());
                            }
                            break;
                    }
                }
            }
        }
    }

    @Override public void onDestroy() {
        Log.d(TAG, "Service destroyed.");
        super.onDestroy();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
    }

    // region callbacks

    public void setDataCallback(DataCallback callbacks){
        this.mDataCallback = callbacks;
    }

    public boolean addLocationCallbacks(LocationCallbacks callbacks) {
        if (mLastLocation != null) {
            callbacks.onLocationChanged(mLastLocation);
        }
        return mLocationCallbacks.add(callbacks);
    }

    public boolean removeLocationCallbacks(LocationCallbacks callbacks) {
        return mLocationCallbacks.remove(callbacks);
    }

    public boolean addGeofenceCallbacks(GeofenceCallbacks callbacks) {
        return mGeofenceCallbacks.add(callbacks);
    }

    public boolean removeGeofenceCallbacks(GeofenceCallbacks callbacks) {
        return mGeofenceCallbacks.remove(callbacks);
    }

    public boolean addConnectionCallbacks(ConnectionCallbacks callbacks) {
        if (mFailedConnectionResult != null) {
            callbacks.onLocationServicesConnectionFailed(mFailedConnectionResult);
        } else if (isLocationServicesConnected()) {
            callbacks.onLocationServicesConnectionSuccessful();
        }
        return mConnectionCallbacks.add(callbacks);
    }

    public boolean removeConnectionCallbacks(ConnectionCallbacks callbacks) {
        return mConnectionCallbacks.remove(callbacks);
    }

    private void notifyCallbacksOnGeofenceEntered(String geofenceId) {
        for (GeofenceCallbacks callbacks : mGeofenceCallbacks) {
            callbacks.onGeofenceEntered(geofenceId);
        }
    }

    private void notifyCallbacksOnGeofenceDwelled(String geofenceId) {
        for (GeofenceCallbacks callbacks : mGeofenceCallbacks) {
            callbacks.onGeofenceDwelled(geofenceId);
        }
    }

    private void notifyCallbacksOnGeofenceExited(String geofenceId) {
        for (GeofenceCallbacks callbacks : mGeofenceCallbacks) {
            callbacks.onGeofenceExited(geofenceId);
        }
    }

    private void notifyCallbacksOnLocationChanged() {
        for (LocationCallbacks callbacks : mLocationCallbacks) {
            callbacks.onLocationChanged(mLastLocation);
        }
    }

    private void notifyCallbacksOnConnectionFailed(ConnectionResult connectionResult) {
        for (ConnectionCallbacks callbacks : mConnectionCallbacks) {
            callbacks.onLocationServicesConnectionFailed(connectionResult);
        }
    }

    private void notifyCallbacksOnConnectionSuccessful() {
        for (ConnectionCallbacks callbacks : mConnectionCallbacks) {
            callbacks.onLocationServicesConnectionSuccessful();
        }
    }

    // endregion

    public void setupGeofences(List<Geofence> geofences) {
        if (isLocationServicesConnected()) {
            Log.d(TAG, "Setting up geofences [" + geofences + "]...");
            LocationServices.GeofencingApi.addGeofences(
                    getGoogleApiClient(),
                    geofences,
                    getGeofencePendingIntent()
            ).setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(Status status) {
                    if (status.isSuccess()) {
                        Log.d(TAG, "Successfully setup geofences.");
                        if( null != mDataCallback ){
                            mDataCallback.onGeofencesSetupSuccessful();
                        }
                    } else {
                        if( null != mDataCallback ){
                            mDataCallback.onGeofencesSetupUnsuccessful(status);
                        }
                    }
                }
            });
        }
    }

    public void removeGeofences() {
        if (isLocationServicesConnected()) {
            Log.d(TAG, "Removing all geofences...");

            // remove from geofencing api
            LocationServices.GeofencingApi.removeGeofences(getGoogleApiClient(), getGeofencePendingIntent());
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "Connected.");
        mFailedConnectionResult = null;
        notifyCallbacksOnConnectionSuccessful();
    }

    protected void requestUpdates(LocationRequest locationRequest) {
        if (isLocationServicesConnected()) {
            Log.d(TAG, "Requesting updates for [" + locationRequest + "]");
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);
            Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (location != null) {
                onLocationChanged(location);
            }
        }
    }

    public void removeLocationUpdates() {
        if (isLocationServicesConnected()) {
            Log.d(TAG, "Removing location updates.");
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    public void onConnectionResolved() {
        if (mGoogleApiClient != null && !mGoogleApiClient.isConnecting()) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public final void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged [" + location + "]");
        mLastLocation = location;
        notifyCallbacksOnLocationChanged();
        if( null != mDataCallback ){
            mDataCallback.onNewLocation(location);
        }
    }

    @Override
    public final void onConnectionSuspended(int i) {
        Log.w(TAG, "Connection to Google Play Services suspended!");
        if( null != mDataCallback ){
            mDataCallback.onLocationServicesConnectionSuspended(i);
        }
    }

    @Override
    public final void onConnectionFailed(ConnectionResult connectionResult) {
        mFailedConnectionResult = connectionResult;
        Log.w(TAG, "Connection to Google Play Services failed!");
        notifyCallbacksOnConnectionFailed(connectionResult);
        if( null != mDataCallback ){
            mDataCallback.onLocationServicesConnectionFailed(connectionResult);
        }
    }

    private PendingIntent getGeofencePendingIntent() {
        return PendingIntent.getService(
                this,
                0,
                new Intent(ACTION_GEOFENCE_TRANSITION),
                PendingIntent.FLAG_UPDATE_CURRENT
        );
    }

    protected GoogleApiClient getGoogleApiClient() {
        return mGoogleApiClient;
    }

    public boolean isLocationServicesConnected() {
        return mGoogleApiClient != null && mGoogleApiClient.isConnected();
    }

    @Override public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        public BackgroundLocationService getBackgroundLocationService() {
            return BackgroundLocationService.this;
        }
    }
}