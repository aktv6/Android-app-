package com.example.gps;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener, GoogleMap.OnMyLocationClickListener {

    private GoogleMap mMap;
    private UiSettings mUiSettings;
    private LocationManager mlocationManager;
    private SupportMapFragment mapFragment;

    private static final String TAG = "demo";
    private static final int LOCATION_REQUEST_CODE = 101;

    private final OkHttpClient okHttpClient = new OkHttpClient();
    private final String directionsURL = "https://maps.googleapis.com/maps/api/directions/json?";
    private final String api_key = "%%API_KEY%%";

    CardView cardViewSearch, cardViewInfo;
    View startFrag, endFrag;
    Button buttonStartRoute, buttonEndRoute;
    TextView textViewDistance, textViewDuration;
    String responseBody, distance, duration;
    List<LatLng> polylinePoints = new ArrayList<>();
    Boolean locationPermission;
    Place startPlace, endPlace;
    LatLng startPoint, endPoint;
    LatLng currentPoint = new LatLng(37.0902, 95.7129);
    //^^^ prevents any potential issues with getting current location


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        Places.initialize(getApplicationContext(), BuildConfig.MAPS_API_KEY);

        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMaxZoomPreference(20f);
        mMap.setMinZoomPreference(0f);
        mUiSettings = mMap.getUiSettings();
        mUiSettings.setZoomControlsEnabled(true);
        mUiSettings.setZoomGesturesEnabled(true);

        getLocationPermission();
        currentPoint = setLocation();

        cardViewSearch = findViewById(R.id.cardViewSearch);
        cardViewInfo = findViewById(R.id.cardViewInfo);
        startFrag = findViewById(R.id.autocompleteStart_fragment);
        endFrag = findViewById(R.id.autocompleteEnd_fragment);

        textViewDistance = findViewById(R.id.textViewDistance);
        textViewDuration = findViewById(R.id.textViewDuration);

        buttonEndRoute = findViewById(R.id.buttonEndRoute);
        buttonStartRoute = findViewById(R.id.buttonStartRoute);

        searchLocation(startFrag.getId());
        searchLocation(endFrag.getId());

        buttonStartRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (endPoint != null) {
                    requestDirections();
                    cardViewSearch.setVisibility(View.INVISIBLE);
                    buttonStartRoute.setVisibility(View.INVISIBLE);

                    cardViewInfo.setVisibility(View.VISIBLE);
                    buttonEndRoute.setVisibility(View.VISIBLE);
                }
                else {
                    Toast.makeText(MapsActivity.this, "To start route please select your destination point", Toast.LENGTH_LONG);
                }
            }
        });

        buttonEndRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cardViewSearch.setVisibility(View.VISIBLE);
                buttonEndRoute.setVisibility(View.INVISIBLE);
                cardViewInfo.setVisibility(View.INVISIBLE);
                buttonStartRoute.setVisibility(View.VISIBLE);
                getLocationPoints();
                textViewDistance.setText("");
                textViewDuration.setText("");
            }
        });
    }


    @SuppressLint("MissingPermission")
    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermission = true;
        }
        else {
            ActivityCompat.requestPermissions(this, new String[] {
                    Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
    }


    @SuppressLint("MissingPermission")
    public LatLng setLocation() {

        if (locationPermission == true) {

            mMap.setMyLocationEnabled(true);
            mMap.setOnMyLocationButtonClickListener(this);
            mMap.setOnMyLocationClickListener(this);
            mUiSettings.setMyLocationButtonEnabled(true);

            //Display the myLocationButton in the left bottom corner.
            @SuppressLint("ResourceType")
            View myLocationButton = mapFragment.getView().findViewById(0x2);
            if (myLocationButton != null && myLocationButton.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
                RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) myLocationButton.getLayoutParams();
                rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                rlp.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
                rlp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
                rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
                rlp.addRule(RelativeLayout.ALIGN_PARENT_END, 0);
                rlp.addRule(RelativeLayout.ALIGN_END, 0);
                rlp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                rlp.setMargins(30, 0, 10, 40);
            }

            //Get user's location and set it to currentPoint.
            mlocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            mlocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 10, new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    currentPoint = new LatLng(location.getLatitude(), location.getLongitude());
                    CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(currentPoint, 15f);
                    mMap.moveCamera(cu);
                    mMap.animateCamera(cu);
                }
            });
        }
        else {
            Toast.makeText(this, "Please, enable your location", Toast.LENGTH_LONG);
        }
        return currentPoint;
    }


    //Search for places by using autocomplete fragment(s) search bars.
    public void searchLocation(Integer i) {
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(i);

        autocompleteFragment.setCountry("us");
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                if (i == findViewById(R.id.autocompleteStart_fragment).getId()) {
                    startPlace = place;
                    startPoint = place.getLatLng();
                    getLocationPoints();
                    autocompleteFragment.getView().findViewById(R.id.places_autocomplete_clear_button)
                            .setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    autocompleteFragment.setText("");
                                    startPoint = null;
                                    getLocationPoints();
                                }
                            });
                }
                else {
                    endPlace = place;
                    endPoint = place.getLatLng();
                    getLocationPoints();
                    autocompleteFragment.getView().findViewById(R.id.places_autocomplete_clear_button)
                            .setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    autocompleteFragment.setText("");
                                    endPoint = null;
                                    getLocationPoints();
                                }
                            });
                }
            }

            @Override
            public void onError(@NonNull Status status) {
                if (status.isCanceled() == true) {
                }
                else {
                    Toast.makeText(MapsActivity.this, "Oops, unexpected error. Please try again.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }


    //Set points and camera position on the map
    private void getLocationPoints() {
        String startTitle = null;
        String endTitle = null;
        mMap.clear();

        if (endPoint != null) {

            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            LatLngBounds bounds;

            if (startPoint == null) {
                mMap.addMarker(new MarkerOptions().position(currentPoint).title("My Location"));
                builder.include(currentPoint);
            }
            else {
                startTitle = startPlace.getName();
                mMap.addMarker(new MarkerOptions().position(startPoint).title(startTitle));
                builder.include(startPoint);
            }

            endTitle = endPlace.getName();
            mMap.addMarker(new MarkerOptions().position(endPoint).title(endTitle));
            builder.include(endPoint);
            bounds = builder.build();
            int width = getResources().getDisplayMetrics().widthPixels;
            int height = getResources().getDisplayMetrics().heightPixels;
            int padding = (int) (width * 0.20);
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding);
            mMap.animateCamera(cu);

        }
        else {
            if (startPoint != null) {
                startTitle = startPlace.getName();
                mMap.addMarker(new MarkerOptions().position(startPoint).title(startTitle));
                CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(startPoint, 15f);
                mMap.moveCamera(cu);
                mMap.animateCamera(cu);
            }
            else {
                CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(currentPoint, 15f);
                mMap.moveCamera(cu);
                mMap.animateCamera(cu);

            }
        }
    }


    @Override
    public boolean onMyLocationButtonClick() {return false;}
    @Override
    public void onMyLocationClick(@NonNull Location location) {}


    private void requestDirections() {
        if (endPoint != null && startPoint != null) {
            HttpUrl httpUrl = HttpUrl.parse(directionsURL)
                    .newBuilder()
                    .addQueryParameter("destination", "side_of_road:place_id:"+endPlace.getId())
                    .addQueryParameter("origin", "place_id:"+startPlace.getId())
                    .addQueryParameter("key", api_key)
                    .build();

            Request request = new Request.Builder()
                    .url(httpUrl)
                    .build();

            okHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    e.printStackTrace();
                }
                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String rBody = response.body().string();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                responseBody = rBody;
                                setDirections(responseBody);
                                getLocationPoints();
                                PolylineOptions polylineOptions = new PolylineOptions();
                                polylineOptions.addAll(polylinePoints);
                                polylineOptions.width(6);
                                polylineOptions.color(Color.argb(60, 60, 45,60));
                                for (int i=0; i<polylinePoints.size(); i++) {
                                    mMap.addPolyline(polylineOptions);
                                }
                                textViewDistance.setText(distance);
                                textViewDuration.setText(duration);
                            }
                        });
                    }
                    else {
                        Log.d(TAG, "onResponse: " + responseBody);
                    }
                }
            });
        }
    }


    private void setDirections(String bodyResponse) {
        if (bodyResponse != null) {
            try {
                JSONObject json = new JSONObject(bodyResponse);
                JSONObject jsonRoutes = json.getJSONArray("routes").getJSONObject(0);
                JSONObject jsonOverview = jsonRoutes.getJSONObject("overview_polyline");
                JSONObject jsonLegs = jsonRoutes.getJSONArray("legs").getJSONObject(0);
                JSONObject jsonDistance = jsonLegs.getJSONObject("distance");
                JSONObject jsonDuration = jsonLegs.getJSONObject("duration");

                distance = jsonDistance.getString("text");
                duration = jsonDuration.getString("text");
                String overview = jsonOverview.getString("points");
                polylinePoints = PolyUtil.decode(overview);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}