package edu.sdsmt.thereyetjohnsonna;

/*
 * Tutorial 7 Grading

Complete the following checklist. If you partially completed an item, put a note how I can check what is working for partial credit.


__X__  35 	Tutorial complete

__X__	20 	Layout (-3pt for each minor error)

__X__	10 	setUI() function (-5pt for each minor error)

__X__	10 	Computing distance between two coordinates (-5pt for each minor error)

__X__	10 	Saving new address to preferences (-5pt for each minor error)

__X__	5 	Adding your home coordinates to the program

__X__	10 	Getting the call to newLocation() to work (-5pt for each minor error)

_N/A_	10 	CSC 576 ONLY:  "is GPS active?" task



The grade you compute is the starting point for course staff, who reserve the right to change the grade if they disagree with your assessment and to deduct points for other issues they may encounter, such as errors in the submission process, naming issues, etc.

 */


import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.location.provider.ProviderProperties;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.location.LocationListenerCompat;

import android.Manifest;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private LocationManager locationManager = null;
    private final static String TO = "to";
    private final static String TOLAT = "tolat";
    private final static String TOLONG = "tolong";


    private double latitude = 0;
    private double longitude = 0;
    private boolean valid = false;

    private double toLatitude = 0;
    private double toLongitude = 0;
    private String to = "";

    private TextView viewDistance;
    private TextView viewLatitude;
    private TextView viewLongitude;
    private TextView viewTo;
    private final ActiveListener activeListener = new ActiveListener();
    private static final int NEED_PERMISSIONS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences settings = getSharedPreferences(getPackageName() + "_preferences", Context.MODE_PRIVATE);
        to = settings.getString(TO, "McLaury Building");
        toLatitude = Double.parseDouble(settings.getString(TOLAT, "44.075104"));
        toLongitude = Double.parseDouble(settings.getString(TOLONG, "-103.206819"));

        viewDistance = findViewById(R.id.textDistance);
        viewLatitude = findViewById(R.id.textLatitude);
        viewLongitude = findViewById(R.id.textLongitude);
        viewTo = findViewById(R.id.textTo);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Force the screen to say on and bright
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }


    /**
     * Called when this application becomes foreground again.
     */
    @Override
    protected void onStart() {
        super.onStart();

        TextView viewProvider = findViewById(R.id.textProvider);
        viewProvider.setText("");

        setUI();
        registerListeners();
    }

    /**
     * Called when this application is no longer the foreground application.
     */
    @Override
    protected void onStop() {
        super.onStop();
        unregisterListeners();
    }

    /**
     * Set all user interface components to the current state
     */
    private void setUI() {
        viewTo.setText(to);
        if (valid) {
            viewLatitude.setText(String.format("%.7f", latitude));
            viewLongitude.setText(String.format("%.7f", longitude));

            double distance = calculateDistance(latitude, longitude, toLatitude, toLongitude);

            viewDistance.setText(String.format(Locale.getDefault(), "%1$6.1fm", distance));
        } else {
            viewLatitude.setText("");
            viewLongitude.setText("");
            viewDistance.setText("");
        }
    }

    private double calculateDistance(double latitude, double longitude, double toLatitude, double toLongitude) {
        final double EARTH_RADIUS_METERS = 6371000.0;

        double lat1 = Math.toRadians(latitude);
        double lon1 = Math.toRadians(longitude);
        double lat2 = Math.toRadians(toLatitude);
        double lon2 = Math.toRadians(toLongitude);

        double dlon = lon2 - lon1;
        double dlat = lat2 - lat1;
        double a = Math.pow(Math.sin(dlat / 2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dlon / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = EARTH_RADIUS_METERS * c;

        return Math.round(distance * 10.0) / 10.0;
    }

    private void registerListeners() {
        unregisterListeners();

        //register if permitted, and request permission if not
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            //base selection on best accurcy, and nothing else
            List<String> providers = locationManager.getProviders(true);
            String bestAvailable = providers.get(0);
            for (int i = providers.size() - 1; i > 0; i--) {

                //use LocationProvider is under android S, and provider properties if over to check accuracy
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S) {
                    LocationProvider loc = locationManager.getProvider(providers.get(i));
                    if (Objects.requireNonNull(loc).getAccuracy() < Objects.requireNonNull(locationManager.getProvider(bestAvailable)).getAccuracy()) {
                        bestAvailable = providers.get(i);
                    }
                } else {
                    ProviderProperties loc = locationManager.getProviderProperties(providers.get(i));
                    if (bestAvailable.equals(LocationManager.PASSIVE_PROVIDER) || Objects.requireNonNull(loc).getAccuracy() < Objects.requireNonNull(locationManager.getProviderProperties(bestAvailable)).getAccuracy()) {
                        bestAvailable = providers.get(i);
                    }
                }
            }
            TextView viewProvider = findViewById(R.id.textProvider);
            viewProvider.setText(bestAvailable);

            //if location is availble request locaiton updates
            if (!bestAvailable.equals(LocationManager.PASSIVE_PROVIDER)) {
                locationManager.requestLocationUpdates(bestAvailable, 500, 1, activeListener);
                Location location = locationManager.getLastKnownLocation(bestAvailable);
                onLocation(location);
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, NEED_PERMISSIONS);
        }
    }

    private void unregisterListeners() {
        locationManager.removeUpdates(activeListener);
    }

    /**
     * Handle when a new location if found by updating the latitude and longitude
     *
     * @param location the current device location
     */
    private void onLocation(Location location) {
        if (location == null) {
            return;
        }

        latitude = location.getLatitude();
        longitude = location.getLongitude();
        valid = true;

        setUI();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NEED_PERMISSIONS) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // permission was granted, yay! Try registering again
                registerListeners();

            } else {

                // permission denied, boo! Tell the users the app won't work now
                Toast.makeText(getApplicationContext(), R.string.denied, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class ActiveListener implements LocationListenerCompat {

        @Override
        public void onLocationChanged(@NonNull Location location) {
            SharedPreferences.Editor editor = getPreferences(Context.MODE_PRIVATE).edit();

            editor.putLong(TOLAT, (long) location.getLatitude());
            editor.putLong(TOLONG, (long) location.getLongitude());
            editor.apply();

            onLocation(location);
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
            registerListeners();
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            registerListeners();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;

    }

    /**
     * Handle an options menu selection
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.itemGrubby) {
            newTo(getString(R.string.grubby), 44.074834, -103.207562);
            return true;

        } else if (id == R.id.itemHome) {
            newTo(getString(R.string.home), 44.068220965293044, -103.28124661176633);
            return true;

        } else if (id == R.id.itemMcLaury) {
            newTo(getString(R.string.mclaury), 44.075104, -103.206819);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Handle setting a new "to" location.
     *
     * @param address Address to display
     * @param lat     latitude
     * @param lon     longitude
     */
    private void newTo(String address, double lat, double lon) {
        to = address;
        toLatitude = lat;
        toLongitude = lon;

        setUI();
    }

    /**
     * Starts the process of finding the coordinates of a new a dress
     *
     * @param view not used
     */
    public void onNew(View view) {
        EditText location = findViewById(R.id.editLocation);
        final String address = location.getText().toString().trim();
        newAddress(address);
    }

    private void newAddress(final String address) {
        if (address.isEmpty()) {
            // Don't do anything if the address is blank
            return;
        }

        new Thread(() -> lookupAddress(address)).start();
    }

    /**
     * Look up the provided address. This works in a thread!
     *
     * @param address Address we are looking up
     */
    private void lookupAddress(String address) {
        Geocoder geocoder = new Geocoder(MainActivity.this, Locale.US);
        boolean exception = false;
        List<Address> locations;
        try {
            Log.i("Emily", "about to go in...");
            locations = geocoder.getFromLocationName(address, 1);
            Log.i("Emily", "made it");
        } catch (Exception ex) {
            // Failed due to I/O exception
            locations = null;
            exception = true;
            Log.e("Emily", "Failed");
        }

        final boolean finalException = exception;
        final List<Address> finalLocations = locations;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                newLocation(address, finalException, finalLocations);
            }
        });
    }


    /**
     * Takes an user input address, and the location address associated with the address, and
     * processes them to display it. If there was an exception, it should toast a failure message
     *
     * @param address   the user input address
     * @param exception was there an exception when trying to find the GPS address
     * @param locations the location address than had the latitude and longitude
     */
    private void newLocation(String address, boolean exception, List<Address> locations) {
        Log.i("Emily", "Made it to newLocation");
        if (exception) {
            Toast.makeText(MainActivity.this, R.string.exception, Toast.LENGTH_SHORT).show();
        } else {
            if (locations == null || locations.isEmpty()) {
                Toast.makeText(this, R.string.couldnotfind, Toast.LENGTH_SHORT).show();
                return;
            }

            EditText location = findViewById(R.id.editLocation);
            location.setText("");

            // We have a valid new location
            Address a = locations.get(0);
            newTo(address, a.getLatitude(), a.getLongitude());
        }
    }
}