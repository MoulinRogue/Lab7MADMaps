package com.example.mad_maps;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;


import java.util.ArrayList;
import java.util.Arrays;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationClient;
    private String TAG = "MapAct";
    private String[] Permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,};
    private LocationCallback locationCallback;
    private LocationRequest mLocationRequest;
    private ArrayList<LatLng> points;
    Polyline line;
    private static final float SMALLEST_DISPLACEMENT = 0.5F;
    private int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 345;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        //create location client object
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (!hasPermissions(this, Permissions)) {
            ActivityCompat.requestPermissions(this, Permissions, REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
            Log.d(TAG, "In onMapReady required Permissions not granted, Asking Permissions");
        } else {
            if (!mFusedLocationClient.getLastLocation().isSuccessful()) {
                getLastLocation();
            }
        }
    }

    // Get results from permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS && grantResults.length == 2) {
            boolean fineLocationPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;
            boolean coarseLocationPermission = grantResults[1] == PackageManager.PERMISSION_GRANTED;
            Log.d(TAG, permissions[0] + " " + fineLocationPermission);
            Log.d(TAG, permissions[1] + " " + coarseLocationPermission);
            if (fineLocationPermission && coarseLocationPermission) {
                Toast.makeText(this, "Permission Granted for both coarse & fine location", Toast.LENGTH_LONG).show();
                getLastLocation();
            } else {
                Toast.makeText(this, "Permission Denied for required permissions", Toast.LENGTH_LONG).show();
            }
        }
    }

    @SuppressLint("MissingPermission")
    //get initial location
    private void getLastLocation(){
        mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new
                OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        LatLng loc = new LatLng(location.getLatitude(),
                                location.getLongitude());
                        //focus map to location
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 19)); // zoom

                    }
                });

        // to hold our location readings
        points = new ArrayList<LatLng>();

        // on location updates
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        LatLng latLng = new LatLng(latitude, longitude);
                        points.add(latLng);
                        //as we have added a new location to the location arraylist draw the line again
                        redrawLine();
                        //refocus the map to the new location
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    }
                }
            }
        };
        //create ongoing location request
        createLocationRequest(); // set up the location tracking
    }

    @SuppressLint("MissingPermission")
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(2000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setSmallestDisplacement(SMALLEST_DISPLACEMENT);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                locationCallback, this.getMainLooper());
    }

    private void redrawLine() {
        mMap.clear(); //clears all overlays
        PolylineOptions options = new PolylineOptions().width(5).color(Color.BLUE).geodesic(true);
        for (int i = 0; i < points.size(); i++) {
            LatLng point = points.get(i);
            options.add(point);
        }
        mMap.addMarker(new MarkerOptions()
                .position(points.get(points.size() - 1))
                .title("I'm a moving bullet"));
        line = mMap.addPolyline(options); //adds Polyline
    }

    //helper to check if permissions are all granted
    public boolean hasPermissions(Context context, String... permissions) {
        boolean passed = true;
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "has permission check failed for:" + permission);
                    passed = false;
                }
            }
        }
        Log.d(TAG, "has permission check passed for:" + Arrays.toString(permissions));
        return passed;
    }
}