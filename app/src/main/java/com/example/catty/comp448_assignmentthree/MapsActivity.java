package com.example.catty.comp448_assignmentthree;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    private Context context;

    private boolean mLocationPermissionGranted;
    private Location mLastKnownLocation;

    // Keys for storing activity state.
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

    //default location is set to sydney if location can't be found
    private final LatLng mDefaultLocation = new LatLng(-33.8523341, 151.2106085);
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private FusedLocationProviderClient mFusedLocationProviderClient;

    private List<Marker> markerList;
    private RecyclerView.Adapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        //Adds a customised toolbar to activity
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        if(savedInstanceState != null){
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
        }
        markerList = new ArrayList<>();
        context = this;
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyAdapter(markerList);
        recyclerView.setAdapter(adapter);
        recyclerView.addOnItemTouchListener(new MyRecyclerTouchListener(this, recyclerView, new MyRecyclerTouchListener.ClickListener() {
                    @Override
                    public void onClick(View view, int position) {
                        Marker marker = markerList.get(position);
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                marker.getPosition(), DEFAULT_ZOOM
                        ));

                    }
                    @Override
                    public void onLongClick(View view, int position) {
                        Marker marker = markerList.get(position);
                        deleteMarker(marker);
                    }
                }));

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menubar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //Display info about app when pressed
        if(id == R.id.info){
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
            alertDialog.setTitle("Information")
                    .setMessage(R.string.info_text)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //do nothing just close dialog box
                        }
                    })
                    .show();
        }
        return super.onOptionsItemSelected(item);
    }


    private void getDeviceLocation() {
        try {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            //Check if gps or network is enabled, if not then make user enable it
            if(!gpsEnabled && !networkEnabled) {
                //Enable gps or network if not enabled
                final AlertDialog alertdialog = new AlertDialog.Builder(this)
                        .setMessage("GPS not enabled")
                        .setPositiveButton(R.string.enable_gps_settings, null)
                        .setNegativeButton(R.string.close, null)
                        .create();
                alertdialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        Button postiveButton = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE);
                        postiveButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivity(intent);
                                alertdialog.dismiss();
                            }
                        });
                        Button negativeButton = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE);
                        negativeButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                //nothing
                                alertdialog.dismiss();
                            }
                        });
                    }
                });
                alertdialog.show();
            }else {
                //If we have the users location update accordingly
                if (mLocationPermissionGranted) {
                    Task locationResult = mFusedLocationProviderClient.getLastLocation();
                    locationResult.addOnCompleteListener(this, new OnCompleteListener() {
                        @Override
                        public void onComplete(@NonNull Task task) {
                            //Move map to location otherwise move to default location
                            if (task.isSuccessful()) {
                                mLastKnownLocation = (Location) task.getResult();
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        new LatLng(mLastKnownLocation.getLatitude(),
                                                mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                            } else {
                                Log.d("MAP", "Current location is null. Using defaults.");
                                Log.e("MAP", "Exception: %s", task.getException());
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                            }
                        }
                    });
                }
            }
        } catch(SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    //Check if location permission is granted if not request it
    private void getLocationPermission(){
        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED){
            mLocationPermissionGranted = true;
        }else{
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch(requestCode){
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                if(grantResults.length >0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    mLocationPermissionGranted =true;
                }else{
                    Toast.makeText(this, "Locations denied", Toast.LENGTH_LONG).show();
                }
            }
        }
        updateLocationUI();
        getDeviceLocation();
    }

    //Updating user interface
    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            getLocationPermission();
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, mLastKnownLocation);
            super.onSaveInstanceState(outState);
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnMapLongClickListener(this);
        mMap.setOnMarkerClickListener(this);

        updateLocationUI();
        getDeviceLocation();
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        getDeviceLocation();
        final LatLng latLng1 = latLng;
        final View view = getLayoutInflater().inflate(R.layout.marker_dialog, null);

        final AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle("New Marker")
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();


                alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        Button positiveButton = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE);
                        positiveButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                //Get marker name from edit text if no name is given force user to enter one and return
                                EditText markerName = (EditText) view.findViewById(R.id.markerName);
                                if(markerName.getText().toString().equals("")){
                                    Snackbar.make(view, "Marker needs a name", Snackbar.LENGTH_LONG).show();
                                    markerName.requestFocus();
                                    return;
                                }
                                //Calculate distance between current location and selected point
                                Location newLocation = new Location("location");
                                newLocation.setLatitude(latLng1.latitude);
                                newLocation.setLongitude(latLng1.longitude);
                                String distance = calcDistance(mLastKnownLocation, newLocation);

                                //Add marker to map and list, with marker name and distance attached
                                markerList.add(mMap.addMarker(new MarkerOptions().position(latLng1).title(markerName.getText().toString()).snippet(distance)));
                                adapter.notifyItemInserted(markerList.size()-1);
                                alertDialog.dismiss();
                            }
                        });

                        Button negativeButton = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE);
                        negativeButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                //If cancel is selected don't add marker
                                alertDialog.dismiss();
                            }
                        });
                    }
                });
                alertDialog.show();
    }

    //Calculate the distance to between to locations
    private String calcDistance(Location locationA, Location locationB){
        int distance = Math.round(locationA.distanceTo(locationB));
        String units;
        if(distance < 5000){
            units = "m";
        }else{
            distance = distance /1000;
            units = "km";
        }
        String output = Integer.toString(distance) + units;
        return output;

    }

    //If marker is clicked zoom to it
    @Override
    public boolean onMarkerClick(Marker marker) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                marker.getPosition(), DEFAULT_ZOOM
        ));
        return false;
    }

    void deleteMarker(Marker marker){
        final Marker toDeleteMarker = marker;
        final AlertDialog alertdialog = new  AlertDialog.Builder(this)
                .setTitle("Delete this marker")
                .setPositiveButton(android.R.string.ok,null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        //Check user wants to delete marker
        alertdialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button positiveButton = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE);
                positiveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //Delete marker if requested otherwise do nothing
                        int index = markerList.indexOf(toDeleteMarker);
                        toDeleteMarker.remove();
                        markerList.remove(toDeleteMarker);
                        adapter.notifyItemRemoved(index);
                        alertdialog.dismiss();
                    }
                });

                Button negativeButton = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE);
                negativeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        alertdialog.dismiss();
                    }
                });
            }
        });

        alertdialog.show();
    }
}
