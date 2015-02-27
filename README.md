A basic base activity and service for managing location in an Android app.
 
Using android-location-activity is as easy as extending our BaseLocationActivity instead of ActionBarActivity from AppCompat.

This base activity will provide a whole suite of useful methods you can override, and provides four abstract methods which you will need to implement.

# To Use
    compile('com.shiftconnects.android:location-activity:1.0.0@aar'){
        transitive=true
    } 
If you do not wish to pull in our version of the Play Services, feel free to ignore the transitive=true flag. If you do this, you will need to provide your own versions of the play services dependencies...

    compile 'com.android.support:appcompat-v7:21.0.3'
    compile 'com.google.android.gms:play-services-location:6.5.87'
    compile 'com.google.android.gms:play-services-base:6.5.87'
    
# And then...
Simply extend BaseLocationActivity for your activities where you'd like location updates. You will want to call bindLocationManager() in onResume, and remember to call unbindLocationManager() when you're done (eg, in onPause).

Useful information will come back in four methods:
### onLocationServicesAvailable()
This method will be called when we've successfully connected to Play Services and we are ready to start location updates. Inside this method, you may want to do something like this:

    LocationRequest request = LocationRequest.create();
    request.setInterval(10000); // Ten seconds
    requestLocationUpdates(request);
The key method there being requestLocationUpdates(LocationRequest). After calling this, you'll get callbacks with updated locations in onLocationChanged().

There are also two methods used to notify users when they have location services disabled. We suggest creating a dialog, or otherwise telling the user to turn the services back on before using the app.

# Background services
android-location-activity also provides a background service, so that you can continue to update locations in a single location while users move around your app. To use this, ensure you have the service defined in your ApplicationManifest as such:

    <service android:name="com.shiftconnects.locationservices.BackgroundLocationService"
             android:exported="false">
        <intent-filter>
            <action android:name="com.shiftconnects.android.ACTION_GEOFENCE_TRANSITION"/>
        </intent-filter>
    </service>
    
# Required Permissions
Since we are using the location, one or both of the location permissions must be declared in your manifest. Choose which one you like, or include both.

    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />