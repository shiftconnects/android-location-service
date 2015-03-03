package com.shiftconnects.android.location.util;

import android.text.format.DateUtils;

import com.google.android.gms.location.LocationRequest;

/**
 * Various utils to build {@link com.google.android.gms.location.LocationRequest}s
 */
public class LocationRequestUtils {

    public static final float MILES_PER_HOUR_TO_METERS_PER_SECOND = 0.44704f;

    public static LocationRequest byMilesPerHour(float mph, int intervalInSeconds) {

        // convert mph to mps
        final float metersPerSecond = MILES_PER_HOUR_TO_METERS_PER_SECOND * mph;

        // convert to requested interval
        final float metersPerInterval = metersPerSecond * intervalInSeconds;

        // convert seconds to milliseconds
        final long intervalInMillis = intervalInSeconds * DateUtils.SECOND_IN_MILLIS;

        // create the request
        return LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(intervalInMillis)
                .setFastestInterval(intervalInMillis)
                .setSmallestDisplacement(metersPerInterval);
    }

    public static LocationRequest byDisplacement(float smallestDisplacement, long intervalMillis) {
        return LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(intervalMillis)
                .setFastestInterval(intervalMillis)
                .setSmallestDisplacement(smallestDisplacement);
    }
}