package com.example.uber_clone;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RiderMap extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    private Button logout, request,setting;
    private LatLng pickup_location;
    private Boolean request_check = false;
    private Marker pickup_marker;
    private FusedLocationProviderClient mFusedLocationClient;
    private String destination;
    private LinearLayout mDriverInfo;
    private ImageView mDriverImage;
    private TextView mDriverName, mDriverPhone, mDriverCar;
    private DatabaseReference mDriverDatabase;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider_map);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        logout = (Button) findViewById(R.id.logout_button);
        request = (Button) findViewById(R.id.request_button);
        setting = (Button) findViewById(R.id.setting_button);



        mDriverInfo = (LinearLayout) findViewById(R.id.driverInfo);
        mDriverImage = (ImageView) findViewById(R.id.driverProfileImage);
        mDriverName = (TextView) findViewById(R.id.driverName);
        mDriverPhone = (TextView) findViewById(R.id.driverPhone);
        mDriverCar = (TextView) findViewById(R.id.driverCar);




        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(RiderMap.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });


        request.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(request_check){
                    request_check = false;
                    geoQuery.removeAllListeners();
                    if(driverLocationRef!=null) {
                        driverLocationRef.removeEventListener(driverLocationRefListener);
                    }
                    if(driverFoundId != null){
                        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId).child("riderRequest");
                        driverRef.removeValue();
                        driverFoundId = null;
                        driverFound = false;
                    }
                    driverFound = false;
                    radius = 1;
                    String userId = FirebaseAuth.getInstance().getUid();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("riderRequest");
                    GeoFire geoFire = new GeoFire(ref);
                    geoFire.removeLocation(userId);
                    if(driver_marker != null){
                        driver_marker.remove();
                    }

                    if(pickup_marker != null){
                        pickup_marker.remove();
                    }
                    request.setText("Call Uber");


                    if(mDriverName != null) {
                        mDriverName.setText("");
                    }
                    if(mDriverPhone != null){
                        mDriverPhone.setText("");
                    }
                    if(mDriverInfo != null){
                        mDriverInfo.setVisibility(View.GONE);
                    }
                    if(mDriverCar != null){
                        mDriverCar.setText("");

                    }

                }else {
                    request_check = true;
                    String userId = FirebaseAuth.getInstance().getUid();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("riderRequest");

                    GeoFire geoFire = new GeoFire(ref);
                    geoFire.setLocation(userId, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()));

                    pickup_location = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());

                    pickup_marker = mMap.addMarker(new MarkerOptions().position(pickup_location).title("Pickup Here").icon(BitmapDescriptorFactory.fromResource(R.mipmap.map_marker_icon_foreground)));

                    request.setText("Getting your Driver....");

                    findClosestDriver();

                }

            }
        });

        setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    Intent intent = new Intent(RiderMap.this,RiderSetting.class);
                    startActivity(intent);

            }
        });

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        }

        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);


        assert autocompleteFragment != null;
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME));


        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                // TODO: Get info about the selected place.
                destination = place.getName();

            }
            @Override
            public void onError(@NonNull Status status) {
                // TODO: Handle the error.
                Log.i("Places", "An error occurred: $status");
            }
        });


    }


    private int radius = 1;
    private Boolean driverFound = false;
    private String driverFoundId;
    GeoQuery geoQuery;

    private void findClosestDriver(){
        DatabaseReference driverLocation = FirebaseDatabase.getInstance().getReference().child("DriversAvailable");
        GeoFire geoFire = new GeoFire(driverLocation);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(pickup_location.latitude,pickup_location.longitude),radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                 if(!driverFound && request_check) {
                     driverFound = true;
                     driverFoundId = key;
                     DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId).child("riderRequest");
                     String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                     HashMap map = new HashMap();
                     map.put("customerRideId",customerId);
                     map.put("destination",destination);
                     driverRef.updateChildren(map);

                     getDriverLocation();
                     getDriverInfo();
                     request.setText("Looking for Driver Location");

                 }

            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                    if(!driverFound)
                    {
                        radius++;
                        findClosestDriver();
                    }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private Marker driver_marker;
    private  DatabaseReference driverLocationRef;
    private ValueEventListener driverLocationRefListener;
    private void getDriverLocation(){
        driverLocationRef = FirebaseDatabase.getInstance().getReference().child("DriversWorking").child(driverFoundId).child("l");
        driverLocationRefListener =  driverLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && request_check){
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLatitude = 0;
                    double loactionLongitude = 0;
                    request.setText("Driver Found");
                    if(map.get(0) != null){
                        locationLatitude = Double.parseDouble(map.get(0).toString());

                    }
                    if(map.get(1) != null){
                        loactionLongitude = Double.parseDouble(map.get(1).toString());
                    }

                    LatLng driverLatlng = new LatLng(locationLatitude,loactionLongitude);
                    if(driver_marker != null){
                        driver_marker.remove();
                    }

                    Location location1 = new Location("");
                    location1.setLatitude(pickup_location.latitude);
                    location1.setLongitude(pickup_location.longitude);

                    Location location2 = new Location("");
                    location2.setLatitude(driverLatlng.latitude);
                    location2.setLongitude(driverLatlng.longitude);

                    float distance = location1.distanceTo(location2);
                    if(distance<100) {
                        request.setText("Your driver is almost here ");
                    }else{
                        request.setText("Driver Found: "+ String.valueOf(distance));
                    }

                    driver_marker= mMap.addMarker(new MarkerOptions().position(driverLatlng).title("your driver").icon(BitmapDescriptorFactory.fromResource(R.mipmap.car_icon_foreground)));


                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }


    private void getDriverInfo(){
        mDriverInfo.setVisibility(View.VISIBLE);
        mDriverDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId);
        mDriverDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()&&dataSnapshot.getChildrenCount()>0){
                    Map<String,Object> map = (Map<String,Object>) dataSnapshot.getValue();
                    if(map.get("name")!= null){

                        mDriverName.setText(map.get("name").toString());

                    }
                    if(map.get("phone")!= null){

                        mDriverPhone.setText(map.get("phone").toString());

                    }
                    if(map.get("car")!= null){

                        mDriverCar.setText(map.get("car").toString());

                    }
                    if(map.get("profileImageUrl")!= null){
                        //Glide.with(getApplication()).load(profile_image_ulr).into(mprofile_image);
                        Picasso.get().load(map.get("profileImageUrl").toString()).into(mDriverImage);
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                mMap.setMyLocationEnabled(true);
            }else{
                checkLocationPermission();
            }
        }



    }

    LocationCallback mLocationCallback = new LocationCallback(){
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for(Location location : locationResult.getLocations()){
                if(getApplicationContext() != null) {
                    mLastLocation = location;
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(16));

                }

            }
        }
    };



    private void checkLocationPermission() {
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                new android.app.AlertDialog.Builder(this)
                        .setTitle("give permission")
                        .setMessage("give permission message")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(RiderMap.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                            }
                        })
                        .create()
                        .show();
            }
            else{
                ActivityCompat.requestPermissions(RiderMap  .this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode){
            case 1:{
                if(grantResults.length >0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        mMap.setMyLocationEnabled(true);
                    }
                } else{
                    Toast.makeText(getApplicationContext(), "Please provide the permission", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }









    protected void onStop(){
        super.onStop();
    }


}

