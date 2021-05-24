package com.randa.yourlocation;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Home extends AppCompatActivity implements OnMapReadyCallback {

    private FusedLocationProviderClient client;
    private SupportMapFragment mapFragment;
    private final int REQUEST_CODE = 111, REQUST_CHECKING_SETTINGS=1001;
    private PlacesClient placeClient;
    private GoogleMap map;
    private AutocompleteSupportFragment autocompleteSupportFragment;
    private  LocationRequest locationRequest;;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);


// this for search places and add marker
        String api=getString(R.string.google_place);
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), api);
        }
        placeClient= Places.createClient(this);
        autocompleteSupportFragment = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.autoPlace);
        autocompleteSupportFragment.setPlaceFields(Arrays.asList(Place.Field.ADDRESS,Place.Field.NAME,Place.Field.ID,Place.Field.LAT_LNG));
        autocompleteSupportFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
               String locationPlace=place.getAddress();
                List<Address> addressList=null;
                if(locationPlace!=null || locationPlace.equals("")){
                    Geocoder geocoder=new Geocoder(Home.this);
                    try {
                        addressList=geocoder.getFromLocationName(locationPlace,1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Address address=addressList.get(0);
                    LatLng latLng= new LatLng(address.getLatitude(),address.getLongitude());
                    map.addMarker(new MarkerOptions().position(latLng).title(place.getName()));

                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,14));

                }
            }
            @Override
            public void onError( Status status) {
                 Log.i("Places","An error occurred: " + status);
            }
        });
        mapFragment.getMapAsync(Home.this);
    }

    private void getCurrentLocation() {
        checkPermission();
        Task<Location> task = client.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if(location!=null){

                    LatLng latLng=new LatLng(location.getLatitude(),location.getLongitude());
                    map.addMarker(new MarkerOptions().position(latLng).title("You are here."));

                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,14));

                }

            }
        });

    }


    private void checkPermission(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }else{
            ActivityCompat.requestPermissions(Home.this,
                    new String[]  {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},REQUEST_CODE);
        }
    }


    @Override
   public void onRequestPermissionsResult(int requestCode,  String[] permissions,  int[] grantResults) {
        if(requestCode==REQUEST_CODE){
            if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                //getCurrentLocation();
            }
        }else {
            Toast.makeText(this,"Permission Denied",Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map=googleMap;


// this for auto location map
        checkPermission();
        checkLocationServices();
        map.setMyLocationEnabled(true);
        map.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                checkPermission();
                client = LocationServices.getFusedLocationProviderClient(Home.this);
                getCurrentLocation();
                return false;
            }
        });

    }
    // method to check the location devices is on or off
    private void checkLocationServices(){
        locationRequest=LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(2000);

        LocationSettingsRequest.Builder builder=new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        Task<LocationSettingsResponse> result=LocationServices.getSettingsClient(getApplicationContext())
                .checkLocationSettings(builder.build());

        result.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete( Task<LocationSettingsResponse> task) {

                LocationSettingsResponse response;
                try {
                    response = task.getResult(ApiException.class);
                   // Toast.makeText(Home.this,"Your Location is on ",Toast.LENGTH_LONG).show();
                } catch (ApiException e) {
                    e.printStackTrace();
                    Toast.makeText(Home.this,"Your Location is Off ",Toast.LENGTH_LONG).show();
                    switch (e.getStatusCode()){
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            try {
                                ResolvableApiException resolvableApiException=(ResolvableApiException)e;
                                resolvableApiException.startResolutionForResult(Home.this,REQUST_CHECKING_SETTINGS);
                            } catch (IntentSender.SendIntentException sendIntentException) {
                                sendIntentException.printStackTrace();
                            }
                            break;
                        case  LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            break;
                    }

                }
            }
        });

    }
    //method to confirm if user check no or yes for location decvice is on

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==REQUST_CHECKING_SETTINGS){
            switch (resultCode){
                case Activity.RESULT_OK:
                    Toast.makeText(Home.this,"GPS is Turned on ", Toast.LENGTH_LONG).show();
                    break;
                case Activity.RESULT_CANCELED:
                    Toast.makeText(Home.this,"GPS is required to be on  ", Toast.LENGTH_LONG).show();

            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}