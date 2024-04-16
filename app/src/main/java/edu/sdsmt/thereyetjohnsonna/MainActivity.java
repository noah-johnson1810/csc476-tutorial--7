package edu.sdsmt.thereyetjohnsonna;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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

        latitude = 44.076708;
        longitude = -103.210499;
        valid = true;
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
    }

    /**
     * Called when this application is no longer the foreground application.
     */
    @Override
    protected void onStop() {
        super.onStop();
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
}