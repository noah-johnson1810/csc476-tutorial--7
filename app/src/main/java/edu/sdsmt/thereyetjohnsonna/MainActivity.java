package edu.sdsmt.thereyetjohnsonna;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.location.provider.ProviderProperties;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.location.LocationListenerCompat;

import android.Manifest;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private LocationManager locationManager = null;
    private SharedPreferences settings = null;
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

        settings = getSharedPreferences(getPackageName() + "_preferences", Context.MODE_PRIVATE);
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
            viewLatitude.setText(String.valueOf(latitude));
            viewLongitude.setText(String.valueOf(longitude));

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
            for (int i = 1; i < providers.size(); i++) {

                //use LocationProvider is under android S, and provider properties if over to check accuracy
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S) {
                    LocationProvider loc = locationManager.getProvider(providers.get(i));
                    if (loc.getAccuracy() < locationManager.getProvider(bestAvailable).getAccuracy()) {
                        bestAvailable = providers.get(i);
                    }
                } else {
                    ProviderProperties loc = locationManager.getProviderProperties(providers.get(i));
                    if (loc.getAccuracy() < locationManager.getProviderProperties(bestAvailable).getAccuracy()) {
                        bestAvailable = providers.get(i);
                    }
                }
            }

            //if location is availble request locaiton updates
            if (!bestAvailable.equals(LocationManager.PASSIVE_PROVIDER)) {
                locationManager.requestLocationUpdates(bestAvailable, 500, 1, activeListener);
                TextView viewProvider = findViewById(R.id.textProvider);
                viewProvider.setText(bestAvailable);
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
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case NEED_PERMISSIONS:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Try registering again
                    registerListeners();

                } else {

                    // permission denied, boo! Tell the users the app won't work now
                    Toast.makeText(getApplicationContext(), R.string.denied, Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private class ActiveListener implements LocationListenerCompat {

        @Override
        public void onLocationChanged(@NonNull Location location) {
            onLocation(location);
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
            registerListeners();
        }

        @Override
        public void onProviderDisabled(String provider) {
            registerListeners();
        }
    }
}