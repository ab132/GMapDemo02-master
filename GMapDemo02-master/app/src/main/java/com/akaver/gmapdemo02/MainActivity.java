package com.akaver.gmapdemo02;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener {

    private final static String TAG = "MainActivity";

    private GoogleMap mGoogleMap;
    private Menu mOptionsMenu;

    private LocationManager locationManager;

    private String provider;

    private int markerCount = 0;
    private Location locationPrevious;
    private double totalDistance;

    private Polyline mPolyline;
    private PolylineOptions mPolylineOptions;


    private TextView textViewWPCount;


    private TextView textViewTotalDistance;
    private double textViewTotalDistanceNumber;

    private Location locationPrevious2;

    double speed;
    private TextView textViewSpeed;
    //private TextView tVSpeed;

    private TextView textViewWpDistance;
    double  textViewWpDistanceNumber;
    //private double distanceFromLastWP;

    public Marker mrk;

    private Location locationCurrent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        Criteria criteria = new Criteria();

        // get the location provider (GPS/CEL-towers, WIFI)
        provider = locationManager.getBestProvider(criteria, false);

        //Log.d(TAG, provider);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No COARSE location permissions!");

        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No FINE location permissions!");

        }

        locationPrevious = locationManager.getLastKnownLocation(provider);

       /* if (locationPrevious != null) {
            // do something with initial position?
        }
        */

        textViewWPCount = (TextView) findViewById(R.id.textview_wpcount);
        textViewSpeed = (TextView) findViewById(R.id.textview_speed);

        textViewTotalDistance = (TextView) findViewById(R.id.textview_total_distance);

        textViewWpDistance = (TextView) findViewById(R.id.textview_wp_distance);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mOptionsMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case R.id.menu_mylocation:
                item.setChecked(!item.isChecked());
                updateMyLocation();
                return true;
            case R.id.menu_trackposition:
                item.setChecked(!item.isChecked());
                updateTrackPosition();
                return true;
            case R.id.menu_keepmapcentered:
                item.setChecked(!item.isChecked());
                return true;
            case R.id.menu_map_type_hybrid:
            case R.id.menu_map_type_none:
            case R.id.menu_map_type_normal:
            case R.id.menu_map_type_satellite:
            case R.id.menu_map_type_terrain:
                item.setChecked(true);
                updateMapType();
                return true;

            case R.id.menu_map_zoom_10:
            case R.id.menu_map_zoom_15:
            case R.id.menu_map_zoom_20:
            case R.id.menu_map_zoom_in:
            case R.id.menu_map_zoom_out:
            case R.id.menu_map_zoom_fittrack:
                updateMapZoomLevel(item.getItemId());
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }


    }


    private void updateMapZoomLevel(int itemId) {
        if (!checkReady()) {
            return;
        }

        switch (itemId) {
            case R.id.menu_map_zoom_10:
                mGoogleMap.moveCamera(CameraUpdateFactory.zoomTo(10));
                break;
            case R.id.menu_map_zoom_15:
                mGoogleMap.moveCamera(CameraUpdateFactory.zoomTo(15));
                break;
            case R.id.menu_map_zoom_20:
                mGoogleMap.moveCamera(CameraUpdateFactory.zoomTo(20));
                break;
            case R.id.menu_map_zoom_in:
                mGoogleMap.moveCamera(CameraUpdateFactory.zoomIn());
                break;
            case R.id.menu_map_zoom_out:
                mGoogleMap.moveCamera(CameraUpdateFactory.zoomOut());
                break;
            case R.id.menu_map_zoom_fittrack:
                updateMapZoomFitTrack();
                break;
        }
    }

    private void updateMapZoomFitTrack() {
        if (mPolyline == null) {
            return;
        }

        List<LatLng> points = mPolyline.getPoints();

        if (points.size() <= 1) {
            return;
        }
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng point : points) {
            builder.include(point);
        }
        LatLngBounds bounds = builder.build();
        int padding = 0; // offset from edges of the map in pixels
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));

    }

    private void updateTrackPosition() {
        if (!checkReady()) {
            return;
        }

        if (mOptionsMenu.findItem(R.id.menu_trackposition).isChecked()) {
            mPolylineOptions = new PolylineOptions().width(5).color(Color.RED);
            mPolyline = mGoogleMap.addPolyline(mPolylineOptions);
        }


    }

    private void updateMapType() {
        if (!checkReady()) {
            return;
        }

        if (mOptionsMenu.findItem(R.id.menu_map_type_normal).isChecked()) {
            mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        } else if (mOptionsMenu.findItem(R.id.menu_map_type_hybrid).isChecked()) {
            mGoogleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        } else if (mOptionsMenu.findItem(R.id.menu_map_type_none).isChecked()) {
            mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NONE);
        } else if (mOptionsMenu.findItem(R.id.menu_map_type_satellite).isChecked()) {
            mGoogleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        } else if (mOptionsMenu.findItem(R.id.menu_map_type_terrain).isChecked()) {
            mGoogleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        }

    }

    private boolean checkReady() {
        if (mGoogleMap == null) {
            Toast.makeText(this, R.string.map_not_ready, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void updateMyLocation() {
        if (mOptionsMenu.findItem(R.id.menu_mylocation).isChecked()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mGoogleMap.setMyLocationEnabled(true);
            return;
        }

        mGoogleMap.setMyLocationEnabled(false);
    }

    public void buttonAddWayPointClicked(View view){
        if (locationPrevious==null){
            return;
        }

        markerCount++;

        mGoogleMap.addMarker(new MarkerOptions().position(new LatLng(locationPrevious.getLatitude(), locationPrevious.getLongitude())).title(Integer.toString(markerCount)));
        textViewWPCount.setText(Integer.toString(markerCount));

    }

    public void buttonCResetClicked(View view){
        markerCount = 0;

        textViewWPCount.setText(Integer.toString(markerCount));


    }

    // Removes the markers from the map, but keeps them in the array.
    public void clearMarkers() {
        mGoogleMap= null;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;

        //mGoogleMap.setMyLocationEnabled(false);

        //LatLng latLngITK = new LatLng(59.3954789, 24.6621282);
        //mGoogleMap.addMarker(new MarkerOptions().position(latLngITK).title("ITK"));
        //mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngITK, 17));

        // set zoom level to 15 - street
        mGoogleMap.moveCamera(CameraUpdateFactory.zoomTo(17));

        // if there was initial location received, move map to it
        if (locationPrevious != null) {
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(locationPrevious.getLatitude(), locationPrevious.getLongitude())));
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
    }

    @Override
    public void onLocationChanged(Location location) {

        LatLng newPoint = new LatLng(location.getLatitude(), location.getLongitude());

        if (mGoogleMap==null) return;

        if (mOptionsMenu.findItem(R.id.menu_keepmapcentered).isChecked() || locationPrevious == null) {
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(newPoint));
        }

        if (mOptionsMenu.findItem(R.id.menu_trackposition).isChecked()) {
            List<LatLng> points = mPolyline.getPoints();
            points.add(newPoint);
            mPolyline.setPoints(points);
        }

        locationPrevious = location;

        textViewTotalDistanceNumber = totalDistance++;
        textViewTotalDistance.setText(Double.toString(textViewTotalDistanceNumber));

        // speed
        speed = 1000 / (locationPrevious.distanceTo(location) / 0.5);
        double min = speed / 60;
        double sec = speed % 60;
        textViewSpeed.setText(String.format("%s:%s", Math.round(min), Math.round(sec)) + " min:km");

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }


    @Override
    protected void onResume(){
        super.onResume();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No COARSE location permissions!");
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No FINE location permissions!");
        }

        if (locationManager!=null){
            locationManager.requestLocationUpdates(provider, 500, 1, this);
        }

        //notifWithControls();
    }


    @Override
    protected void onPause(){
        super.onPause();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No COARSE location permissions!");
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No FINE location permissions!");
        }

        if (locationManager!=null){
            locationManager.removeUpdates(this);
        }
    }


}
