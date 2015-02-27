
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

package com.shiftconnects.sample;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationRequest;
import com.shiftconnects.locationservice.BackgroundLocationService;


public class MainActivity extends ActionBarActivity implements GpsStatus.Listener, BackgroundLocationService.ConnectionCallbacks, BackgroundLocationService.LocationCallbacks {
    private static final String TAG = MainActivity.class.getName();

    // Google play services stuff..
    private boolean mIsInResolution;
    private boolean mShouldRetryConnecting;

    // Request code for auto Google Play Services error resolution.
    protected static final int REQUEST_CODE_RESOLUTION = 1;

    // Use the location manger to track if location is enabled or not.
    private LocationManager mLocationManager;
    private boolean mLocationEnabled;

    // Our background service, and the callback setup.
    private BackgroundLocationService mBackgroundLocationService;
    private ServiceConnection mLocationManagerConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "Background location services connected");
            mBackgroundLocationService = ((BackgroundLocationService.LocalBinder)service).getBackgroundLocationService();
            mBackgroundLocationService.addConnectionCallbacks(MainActivity.this);
            mBackgroundLocationService.addLocationCallbacks(MainActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBackgroundLocationService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkLocationEnabled();

        if( mLocationEnabled ){
            bindService(new Intent(this, BackgroundLocationService.class), mLocationManagerConnection, Context.BIND_AUTO_CREATE);
        } else {
            Toast.makeText(this, "Location is disabled :(", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkLocationEnabled() {
        mLocationEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    @Override
    public void onLocationChanged(Location location) {
        Toast.makeText(this, "New location! " + location.toString(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        // When you're done with locations, be sure you remember to remove the service!
        if (mBackgroundLocationService != null) {
            mBackgroundLocationService.removeLocationUpdates();
            mBackgroundLocationService.removeConnectionCallbacks(this);
            mBackgroundLocationService.removeLocationCallbacks(this);
            unbindService(mLocationManagerConnection);
            mBackgroundLocationService = null;
        }

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocationManager.removeGpsStatusListener(this);
    }

    @Override
    public final void onConnectionSuspended(int i) {
        Log.w(TAG, "Connection to Google Play Services suspended!");
    }

    @Override
    public void onLocationServicesConnectionSuccessful() {
        LocationRequest request = LocationRequest.create();
        request.setInterval(5000); // Five seconds

        mBackgroundLocationService.requestUpdates(request);
    }

    @Override
    public void onLocationServicesConnectionFailed(ConnectionResult result) {
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
        if (mBackgroundLocationService != null) {
            mBackgroundLocationService.onConnectionResolved();
        }
    }

    @Override public void onGpsStatusChanged(int event) {
        switch (event) {
            case GpsStatus.GPS_EVENT_STARTED:
                Log.d(TAG, "GPS has started.");
                break;
            case GpsStatus.GPS_EVENT_STOPPED:
                Log.d(TAG, "GPS has stopped.");
                checkLocationEnabled();
                if (mBackgroundLocationService.getGoogleApiClient().isConnected() && !mLocationEnabled) {
                    Log.d(TAG, "Disconnecting location client");
                    Toast.makeText(this, "Location disabled.", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
}
