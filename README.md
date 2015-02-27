A basic base activity and service for managing location in an Android app.
 
Using android-location-activity is as easy as including and starting the background service in the library.

The ```BackgroundLocationService``` provides a whole suite of useful methods (via callbacks).

[ ![Download](https://api.bintray.com/packages/inktomi/maven/com.shiftconnects.android.location/images/download.svg) ](https://bintray.com/inktomi/maven/com.shiftconnects.android.location/_latestVersion)

# To Use / Dependencies
    compile('com.shiftconnects.android.location:location-service:1.3.0'){
        transitive=true
    } 
If you do not wish to pull in our version of the Play Services, feel free to ignore the transitive=true flag. If you do this, you will need to provide your own versions of the play services dependencies...

    compile 'com.google.android.gms:play-services-location:6.5.87'
    compile 'com.google.android.gms:play-services-base:6.5.87'
    
    
# Background services
android-location-activity a background service so that you can continue to update locations in a single location while users move around your app. To use this, ensure you have the service defined in your ApplicationManifest as such:

    <service android:name="com.shiftconnects.android.location.BackgroundLocationService"
             android:exported="false">
        <intent-filter>
            <action android:name="com.shiftconnects.android.ACTION_GEOFENCE_TRANSITION"/>
        </intent-filter>
    </service>
    
# Getting Location Data
In order to retrieve location data, several callbacks exist on the service class. You will set these up after starting the service.

    bindService(new Intent(this, BackgroundLocationService.class), mLocationManagerConnection, Context.BIND_AUTO_CREATE);
    
Inside your ```ServiceConnection``` object, you'll have access to the service in order to set callbacks.
 
    private BackgroundLocationService mBackgroundLocationService;
    private ServiceConnection mLocationManagerConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "Background location services connected");
            mBackgroundLocationService = ((BackgroundLocationService.LocalBinder)service).getBackgroundLocationService();
            mBackgroundLocationService.addConnectionCallbacks(LocationActivity.this);
            mBackgroundLocationService.addLocationCallbacks(LocationActivity.this);
        }
    
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBackgroundLocationService = null;
        }
    };
    
Once you're notified via ```onLocationServicesConnectionSuccessful``` that the play services are connected in the connection callbacks, you can request location updates on the service by calling ```requestUpdates()``` and passing in your ```LocationRequest```. Updated locations will arrive in ```onNewLocation()```.
# Required Permissions
Since we are using the location, one or both of the location permissions must be declared in your manifest. Choose which one you like, or include both.

    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />