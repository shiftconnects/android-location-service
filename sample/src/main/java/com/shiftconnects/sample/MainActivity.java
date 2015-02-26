
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

import android.location.Location;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.location.LocationRequest;
import com.shiftconnects.locationservices.BaseLocationActivity;


public class MainActivity extends BaseLocationActivity {

    Toast mToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindLocationManager();
    }

    @Override
    public void onLocationServicesAvailable() {
        LocationRequest request = LocationRequest.create();
        request.setInterval(1000); // One second

        requestLocationUpdates(request);
    }

    @Override
    public void onLocationChanged(Location location) {
        Toast.makeText(this, "New location! " + location.toString(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showLocationDisabledDialogFragment() {
        mToast = Toast.makeText(this, "Location disabled!", Toast.LENGTH_LONG);
        mToast.show();
    }

    @Override
    public void removeLocationDisabledDialogFragment() {
        mToast.cancel();
        mToast = null;
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
        unbindLocationManager();

        super.onPause();
    }
}
