package com.example.uber_clone;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;

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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DriverMap extends FragmentActivity implements OnMapReadyCallback, RoutingListener {

    private GoogleMap mMap;
    //GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    private Button logout, mSetting;
    private String customerId ="";
    private Boolean isLoggingOut = false;
    private FusedLocationProviderClient mFusedLocationClient;
    private Switch mWorkingSwitch;
    private LinearLayout mRiderInfo;
    private ImageView mRiderImage;
    private TextView mRiderName, mRiderPhone, mCustomerDestination;
    private DatabaseReference mRiderDatabase;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_map);

        polylines = new ArrayList<>();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        mRiderInfo = (LinearLayout) findViewById(R.id.riderInfo);
        mRiderImage = (ImageView) findViewById(R.id.riderProfileImage);
        mRiderName = (TextView) findViewById(R.id.riderName);
        mRiderPhone = (TextView) findViewById(R.id.riderPhone);
        mCustomerDestination = (TextView) findViewById(R.id.customerDestination);
        mWorkingSwitch = (Switch) findViewById(R.id.work_switch);
        mWorkingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    connectDriver();
                }else{
                    disconnectDriver();
                }
            }
        });




        logout = (Button) findViewById(R.id.logout_button);
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                isLoggingOut = true;
                disconnectDriver();
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(DriverMap.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });


        mSetting = (Button) findViewById(R.id.setting_button);
        mSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(DriverMap.this, DriverSetting.class);
                startActivity(intent);

            }
        });




        getAssignedCustomer();


    }


    private void getAssignedCustomer(){
        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("riderRequest").child("customerRideId");
        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    customerId = dataSnapshot.getValue().toString();
                    getAssignedCustomerPickupLocation();
                    getAssignedCustomerDestination();
                    getAssignedCustomerInfo();


                }else{
                    removePolyline();
                    customerId ="";
                    if(pickupMarker != null){
                        pickupMarker.remove();
                    }
                    if(assignedCustomerPickupLocationListener != null){
                        assignedCustomerPickupLocation.removeEventListener(assignedCustomerPickupLocationListener);

                    }if(mRiderName != null) {
                        mRiderName.setText("");
                    }
                    if(mRiderPhone != null){
                        mRiderPhone.setText("");
                    }
                    if(mRiderInfo != null){
                        mRiderInfo.setVisibility(View.GONE);

                    }else{
                        mCustomerDestination.setText("Destination:-- ");
                    }



                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void getAssignedCustomerDestination(){
        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("riderRequest").child("destination");
        assignedCustomerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    String destination = dataSnapshot.getValue().toString();
                    mCustomerDestination.setText("Destination: "+ destination);
                }else{
                    mCustomerDestination.setText("Destination:-- ");

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }
    Marker pickupMarker;
    private DatabaseReference assignedCustomerPickupLocation;
    private ValueEventListener assignedCustomerPickupLocationListener;
    private void getAssignedCustomerPickupLocation(){
        assignedCustomerPickupLocation = FirebaseDatabase.getInstance().getReference().child("riderRequest").child(customerId).child("l");
        assignedCustomerPickupLocationListener = assignedCustomerPickupLocation.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && !customerId.equals("")){
                   List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLatitude = 0;
                    double loactionLongitude = 0;
                    if(map.get(0) != null){
                        locationLatitude = Double.parseDouble(map.get(0).toString());

                    }
                    if(map.get(1) != null){
                        loactionLongitude = Double.parseDouble(map.get(1).toString());
                    }

                    LatLng pickupLatLng = new LatLng(locationLatitude,loactionLongitude);

                    pickupMarker = mMap.addMarker(new MarkerOptions().position(pickupLatLng).title("pick up location").icon(BitmapDescriptorFactory.fromResource(R.mipmap.map_marker_icon_foreground)));
                    getRouteToMarker(pickupLatLng);
                }
            }




            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void getRouteToMarker(LatLng pickupLatLng) {
        Routing routing = new Routing.Builder()
                .key("AIzaSyDSslf13LTTgLuzpYwNw5lrr1GmOb8Mbjk")
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(false)
                .waypoints(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), pickupLatLng)
                .build();
        routing.execute();
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(500);
        mLocationRequest.setFastestInterval(500);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
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

                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("DriversAvailable");
                    DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("DriversWorking");
                    GeoFire geoFireAvailable = new GeoFire(refAvailable);
                    GeoFire geoFireWorking = new GeoFire(refWorking);


                    switch (customerId){
                        case "": // khong co customer id
                            geoFireWorking.removeLocation(userId);
                            geoFireAvailable.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                            break;

                        default:// Customer ID != null
                            geoFireAvailable.removeLocation(userId);
                            geoFireWorking.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));

                            break;
                    }



                }

            }
        }
    };


    private void getAssignedCustomerInfo(){
        mRiderInfo.setVisibility(View.VISIBLE);
        mRiderDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(customerId);
        mRiderDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()&&dataSnapshot.getChildrenCount()>0){
                    Map<String,Object> map = (Map<String,Object>) dataSnapshot.getValue();
                    if(map.get("name")!= null){

                        mRiderName.setText(map.get("name").toString());

                    }
                    if(map.get("phone")!= null){

                        mRiderPhone.setText(map.get("phone").toString());

                    }
                    if(map.get("profileImageUrl")!= null){
                        //Glide.with(getApplication()).load(profile_image_ulr).into(mprofile_image);
                        Picasso.get().load(map.get("profileImageUrl").toString()).into(mRiderImage);
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void checkLocationPermission() {
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){

                new AlertDialog.Builder(this)
                        .setTitle("Permission Required")
                        .setMessage("Please provide location")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                           ActivityCompat.requestPermissions(DriverMap.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
                            }
                        })
                .create()
                .show();
            }else{
                ActivityCompat.requestPermissions(DriverMap.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
            }
        }
    }

    public void onRequestPermissionsResult(int requestCode,@NonNull String[] permissions,@NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        switch (requestCode){
            case 1: {
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest,mLocationCallback, Looper.myLooper());
                        mMap.setMyLocationEnabled(true);
                    }
                }else{
                    Toast.makeText(getApplicationContext(),"Please provide the permission",Toast.LENGTH_LONG).show();
                }


            }
        }
    }


    public void connectDriver(){
        checkLocationPermission();
        mFusedLocationClient.requestLocationUpdates(mLocationRequest,mLocationCallback, Looper.myLooper());
        mMap.setMyLocationEnabled(true);
    }



    private void disconnectDriver(){
       if(mFusedLocationClient != null){
           mFusedLocationClient.removeLocationUpdates(mLocationCallback);
       }
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("DriversAvailable");
        GeoFire geoFireAvailable = new GeoFire(refAvailable);
        geoFireAvailable.removeLocation(userId);
    }

    protected void onStop(){
        super.onStop();
        if(!isLoggingOut){
           disconnectDriver();
        }


    }

    @Override
    public void onRoutingFailure(RouteException e) {
        if(e != null) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }else {
            Toast.makeText(this, "Something went wrong, Try again", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onRoutingStart() {

    }
    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.primary_dark_material_light};

    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {
        if (polylines.size() > 0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }

        polylines = new ArrayList<>();
        //add route(s) to the map.
        for (int i = 0; i < route.size(); i++) {

            //In case of more than 5 alternative routes
            int colorIndex = i % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);

            Toast.makeText(getApplicationContext(), "Route " + (i + 1) + ": distance - " + route.get(i).getDistanceValue() + ": duration - " + route.get(i).getDurationValue(), Toast.LENGTH_SHORT).show();

        }
    }
    @Override
    public void onRoutingCancelled() {

    }

    private void removePolyline(){
        for(Polyline line : polylines){
            line.remove();

        }
        polylines.clear();
    }
//    protected void onDestroy() {
//        super.onDestroy();
//        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
//        DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("DriversWorking");//remove driver id from driver working database when the driver close the app
//        GeoFire geoFireWorking = new GeoFire(refWorking);
//        geoFireWorking.removeLocation(userId);
//        DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("DriversAvailable");
//        GeoFire geoFireAvailable = new GeoFire(refAvailable);
//        geoFireAvailable.removeLocation(userId);
//        customerId ="";
//        newmMap.remove();
//    }


}
