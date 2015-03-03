package com.shiftconnects.android.location;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;

/**
 * Created by mattruno on 3/3/15.
 */
public class MockBackgroundLocationService extends BackgroundLocationService implements BackgroundLocationService.ConnectionCallbacks {

    private static final String TAG = MockBackgroundLocationService.class.getSimpleName();

    private static final long DEFAULT_SEND_INTERVAL = 1000l; // 1 second

    private final IBinder mBinder = new LocalBinder();

    private HandlerThread mWorkThread;
    private Looper mUpdateLooper;
    private UpdateHandler mUpdateHandler;

    private long mSendInterval = DEFAULT_SEND_INTERVAL;
    private float mAccuracy;
    private boolean mTestStarted;

    @Override public void onCreate() {
         super.onCreate();
         Log.d(TAG, "Service created.");
         mWorkThread = new HandlerThread("UpdateThread", android.os.Process.THREAD_PRIORITY_BACKGROUND);
         mWorkThread.start();
         mUpdateLooper = mWorkThread.getLooper();
         mUpdateHandler = new UpdateHandler(mUpdateLooper);
         mTestStarted = false;
        
        addConnectionCallbacks(this);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
         Log.d(TAG, "Service started.");
         return Service.START_STICKY;
    }

    @Override public void onDestroy() {
         super.onDestroy();
         if (getGoogleApiClient() != null && getGoogleApiClient().isConnected()) {
             LocationServices.FusedLocationApi.setMockMode(getGoogleApiClient(), false);
         }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
         return mBinder;
    }

    public void mockLocations(long sendInterval, float accuracy, LatLng... locations) {
         mSendInterval = sendInterval;
         mAccuracy = accuracy;
         Message msg = mUpdateHandler.obtainMessage();
         msg.obj = locations;
         mUpdateHandler.sendMessage(msg);
    }

    public void setAccuracy(float accuracy) {
         mAccuracy = accuracy;
    }

    public void setSendInterval(long sendInterval) {
         mSendInterval = sendInterval;
    }

    @Override
    public void onLocationServicesConnectionSuccessful() {
        mUpdateLooper = mWorkThread.getLooper();
        mUpdateHandler = new UpdateHandler(mUpdateLooper);
        LocationServices.FusedLocationApi.setMockMode(getGoogleApiClient(), true);
    }

    @Override
    public void onLocationServicesConnectionFailed(ConnectionResult connectionResult) {

    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public class UpdateHandler extends Handler {

         public UpdateHandler(Looper looper) {
             super(looper);
         }

         @Override public void handleMessage(Message msg) {
             if (!mTestStarted) {
                 mTestStarted = true;
                 long elapsedTimeNanos;
                 long currentTime;
                 LatLng[] mockLocations = (LatLng[]) msg.obj;
                 if (mockLocations != null && mockLocations.length > 0) {
                     Location mockLocation = new Location("fused");
                     LatLng lastLocation = null;
                     for (LatLng latLng : mockLocations) {
                         if (getGoogleApiClient() == null || !getGoogleApiClient().isConnected()) {
                             break;
                         }
                         currentTime = System.currentTimeMillis();
                         if (Build.VERSION.SDK_INT >= 17) {
                             elapsedTimeNanos = SystemClock.elapsedRealtimeNanos();
                             mockLocation.setElapsedRealtimeNanos(elapsedTimeNanos);
                         }
                         mockLocation.setTime(currentTime);
                         mockLocation.setAccuracy(mAccuracy);
                         mockLocation.setLatitude(latLng.latitude);
                         mockLocation.setLongitude(latLng.longitude);
                         if (lastLocation != null) {
                             mockLocation.setBearing((float) SphericalUtil.computeHeading(lastLocation, latLng));
                         }
                         lastLocation = latLng;
                         LocationServices.FusedLocationApi.setMockLocation(getGoogleApiClient(), mockLocation);

                         // wait the specified interval
                         try {
                             Thread.sleep(mSendInterval);
                         } catch (InterruptedException e) {
                             break;
                         }
                     }
                 }
                 mTestStarted = false;
             }
         }
    }

    public class LocalBinder extends Binder {
         public MockBackgroundLocationService getService() {
             // Return this instance of LocalService so clients can call public methods
             return MockBackgroundLocationService.this;
         }
    }
}
