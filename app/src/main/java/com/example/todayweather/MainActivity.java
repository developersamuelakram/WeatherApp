package com.example.todayweather;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.Image;
import android.os.AsyncTask;
import android.provider.Settings;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String URL = "http://api.openweathermap.org/data/2.5/weather";
    private static final int LOCATION_PERMISSION_REQUEST = 0;
    private static final int SMALL_DISPLACEMENT = 20;
    private static final int REFRESH_MAX_INTERVAL = 10000;
    private static final int REFRESH_MIN_INTERVAL = 1000;
    private String lastrequestURL;
    EditText cityname;
    Button submit;
    FloatingActionButton refresh;
    View outbox;
    ProgressDialog loader;
    private GoogleApiClient mGoogleApiclient;
    private LocationRequest locationRequest;
    private Location location;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cityname = findViewById(R.id.cityName);
        refresh = findViewById(R.id.refresh);
        submit = findViewById(R.id.submit);
        outbox = findViewById(R.id.output);

        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadandbinddata();
            }
        });

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (cityname.getText().length() == 0) {
                    cityname.setError("enter complete city name");
                    return;
                }

                generateWeatherbycity();
                loadandbinddata();


            }
        });

        if (mGoogleApiclient == null) {
            mGoogleApiclient = new GoogleApiClient.Builder
                    (this).addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
        }

        createlocationrequest();
        loader = ProgressDialog.show(this, "LOADING", "Waiting on GPS", true);
    }

    private void generateWeatherbycity() {
        lastrequestURL = URL + "?q=" + cityname.getText() + "&lang=en&units=metric&appid=" + getString(R.string.API_KEY);
    }

    private void generateweatherbylocation() {

        lastrequestURL = URL + "?lat=" + location.getLatitude() + "&lon=" + location.getLongitude() + "&lang=en&units=metric&appid=" + getString(R.string.API_KEY);


    }

    @SuppressLint("StaticFieldLeak")
    private void loadandbinddata() {
        loader = ProgressDialog.show(this, "Loading", "WAITING ON GPS", true);

        new AsyncTask<Void, Void, Pair<CurrentWeatherdata, String>>() {


            @Override
            protected Pair<CurrentWeatherdata, String> doInBackground(Void... voids) {
                return loaddata();
            }

            @Override
            protected  void onPostExecute(Pair<CurrentWeatherdata, String> result) {

                switch (result.second) {

                    case "OK":
                        bindtooutbox(result.first);
                        break;

                    case "CITY NOT FOUND":
                        showcitynotfounddialong();
                        break;

                    case "WRONG KEY":
                        Toast.makeText(MainActivity.this, "KEY IS WRONG", Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        showDialogProblemwithwifi();
                        break;

                }

                loader.dismiss();

            }
        }.execute();
    }

    private Pair<CurrentWeatherdata, String> loaddata() {
        Pair<CurrentWeatherdata, String> result = new Pair(null, "ERROR");

        InputStream inputStream = null;
        try {
            URL url = new URL(lastrequestURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(10000);
            conn.setDoInput(true);
            conn.connect();

            switch (conn.getResponseCode()) {

                case HttpURLConnection.HTTP_OK:
                    inputStream = new BufferedInputStream(conn.getInputStream());
                    result = new Pair(CurrentWeatherdata.serialize(IOUtils.toString(inputStream, "UTF-8")), "OK");
                    break;

                case HttpURLConnection.HTTP_UNAUTHORIZED:
                    result = new Pair<>(null, "NOT AUTHORIZED");
                    break;

                case HttpURLConnection.HTTP_NOT_FOUND:
                    result = new Pair<>(null, "NOT FOUND");
                    break;

            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {

            if (inputStream!=null) {

                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return result;
    }

    private void showDialogProblemwithwifi() {
        new AlertDialog.Builder(this).setTitle("Notice").setCancelable(true).setNegativeButton(null, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            }
        }).setPositiveButton("Cancel", null).show();

    }


    private void showcitynotfounddialong() {
        new AlertDialog.Builder(this).
                setTitle("Notice").setMessage("City not found").
                setCancelable(true).setPositiveButton("cancel", null )
                .show();
    }

    private void bindtooutbox(CurrentWeatherdata first) {

        outbox.setVisibility(View.VISIBLE);
        TextView name  = findViewById(R.id.areaName);
        TextView temp = findViewById(R.id.temp);
        TextView desc = findViewById(R.id.description);
        ImageView imageView = findViewById(R.id.weatherimage);

        name.setText(first.getPlacename());
        desc.setText(first.getDesc());
        temp.setText(String.format("%d °C", first.getTemp()));

        if (first.getTemp() < 8) {
            temp.setTextColor(ContextCompat.getColor(this, R.color.cold));

        } else if (first.getTemp() < 24) {
            temp.setTextColor(ContextCompat.getColor(this, R.color.normal));
        } else {
            temp.setTextColor(ContextCompat.getColor(this, R.color.hot));

        }

        if (first.getClouds() < 20) {
            imageView.setImageResource(R.drawable.white_balance_sunny);

        } else if (first.getClouds() < 40){
            imageView.setImageResource(R.drawable.weather_partlycloudy);

        } else if (first.getClouds() < 60) {
            imageView.setImageResource(R.drawable.cloud_outline);
        } else {
            imageView.setImageResource(R.drawable.weather_lightning_rainy);


        }
    }

    private void createlocationrequest() {
        locationRequest  = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(REFRESH_MIN_INTERVAL);
        locationRequest.setFastestInterval(REFRESH_MAX_INTERVAL);
        locationRequest.setSmallestDisplacement(SMALL_DISPLACEMENT);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        startLocationRequest();

    }

    private void startLocationRequest() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST);
            return;

        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiclient, locationRequest, this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[]permission, int[]grantresults) {

        super.onRequestPermissionsResult(requestCode, permission, grantresults);
        if (LOCATION_PERMISSION_REQUEST == requestCode) {
            startLocationRequest();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {

        this.location = location;
        loader.dismiss();
        loadandbinddata();
        generateweatherbylocation();
    }

    protected void onStart() {
        mGoogleApiclient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiclient.disconnect();
        super.onStop();
    }
}