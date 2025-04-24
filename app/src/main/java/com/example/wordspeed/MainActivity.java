package com.example.wordspeed;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.view.View;
import android.widget.Button;

import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // Constants
    private static final int LOCATION_PERMISSION_CODE = 101;
    private static final long MIN_TIME_MS = 1000;
    private static final float MIN_DISTANCE_M = 1;

    // UI Components
    private SpeedometerView speedometerPortrait, speedometerLandscape;
    private Button btnStartPortrait, btnStartLandscape;
    private FloatingActionButton btnRotatePortrait, btnRotateLandscape;
    private TextView txtTripInfo;

    // Location & Tracking
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Location lastLocation;
    private Handler updateHandler;
    private Runnable updateRunnable;

    // Trip Data
    private float totalDistance = 0;
    private float maxSpeed = 0;
    private boolean isTracking = false;
    private boolean isLandscape = false;
    private long startTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeUI();
        setupLocationSystem();
        setupEventHandlers();
    }

    private void initializeUI() {
        // Portrait Components
        speedometerPortrait = findViewById(R.id.speedometerViewPortrait);
        btnStartPortrait = findViewById(R.id.startButtonPortrait);
        btnRotatePortrait = findViewById(R.id.rotateButtonPortrait);

        // Landscape Components
        speedometerLandscape = findViewById(R.id.speedometerViewLandscape);
        btnStartLandscape = findViewById(R.id.startButtonLandscape);
        btnRotateLandscape = findViewById(R.id.rotateButtonLandscape);
        txtTripInfo = findViewById(R.id.tripInfoLandscape);

        // Validate components
        if (speedometerPortrait == null || speedometerLandscape == null ||
                btnStartPortrait == null || btnStartLandscape == null ||
                btnRotatePortrait == null || btnRotateLandscape == null) {
            Toast.makeText(this, "Lỗi khởi tạo giao diện!", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void setupLocationSystem() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        createLocationListener();
        checkLocationPermission();
    }

    private void createLocationListener() {
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                handleNewLocation(location);
            }

            @Override
            public void onProviderEnabled(String provider) {
                updateSpeedometer(0);
            }

            @Override
            public void onProviderDisabled(String provider) {
                updateSpeedometer(0);
            }
        };
    }

    private void setupEventHandlers() {
        // Start/Stop Buttons
        btnStartPortrait.setOnClickListener(v -> toggleTracking());
        btnStartLandscape.setOnClickListener(v -> toggleTracking());

        // Rotation Buttons
        btnRotatePortrait.setOnClickListener(v -> toggleOrientation());
        btnRotateLandscape.setOnClickListener(v -> toggleOrientation());
    }

    private void toggleTracking() {
        if (isTracking) {
            stopTracking();
        } else {
            startTracking();
        }
        updateButtonStates();
    }

    private void startTracking() {
        if (!checkLocationPermission()) return;

        resetTripData();
        isTracking = true;
        startTime = System.currentTimeMillis();
        startLocationUpdates();
        startSpeedometerAnimation();
        startInfoUpdates();
    }

    private void stopTracking() {
        isTracking = false;
        stopLocationUpdates();
        stopInfoUpdates();
        showTripSummary();
        updateSpeedometer(0);
    }

    private void updateButtonStates() {
        String label = isTracking ? "■ DỪNG" : "▶ BẮT ĐẦU";
        btnStartPortrait.setText(label);
        btnStartLandscape.setText(label);
    }

    private void toggleOrientation() {
        isLandscape = !isLandscape;
        findViewById(R.id.portraitLayout).setVisibility(isLandscape ? View.GONE : View.VISIBLE);
        findViewById(R.id.landscapeLayout).setVisibility(isLandscape ? View.VISIBLE : View.GONE);
        syncSpeedometerState();
    }

    private void syncSpeedometerState() {
        SpeedometerView active = getActiveSpeedometer();
        active.setSpeed(isTracking ? getCurrentSpeed() : 0);
        active.setDistance(totalDistance);
    }

    private void handleNewLocation(Location location) {
        if (lastLocation != null && location != null) {
            float distance = lastLocation.distanceTo(location);
            totalDistance += distance;
        }

        float speed = location.getSpeed() * 3.6f; // m/s to km/h
        maxSpeed = Math.max(maxSpeed, speed);

        updateSpeedometer(speed);
        lastLocation = location;
    }

    private void updateSpeedometer(float speed) {
        getActiveSpeedometer().setSpeed(speed);
        getActiveSpeedometer().setDistance(totalDistance);
    }

    private void startSpeedometerAnimation() {
        SpeedometerView speedometer = getActiveSpeedometer();
        AnimatorSet animSet = new AnimatorSet();
        ObjectAnimator anim = ObjectAnimator.ofFloat(speedometer, "speed", 0, 240, 0);
        anim.setDuration(2000);
        animSet.play(anim);
        animSet.start();
    }

    private void startInfoUpdates() {
        updateHandler = new Handler(Looper.getMainLooper());
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (isTracking && isLandscape) {
                    updateTripInfoDisplay();
                    updateHandler.postDelayed(this, 500);
                }
            }
        };
        updateHandler.post(updateRunnable);
    }

    private void updateTripInfoDisplay() {
        String info = String.format(Locale.getDefault(),
                "⚡ Tốc độ: %.1f km/h\n" +
                        "📏 Quãng đường: %.2f km\n" +
                        "⏱ Thời gian: %s\n" +
                        "🚀 Max: %.1f km/h\n" +
                        "🛰 Độ chính xác: %.1fm",
                getCurrentSpeed(),
                totalDistance / 1000,
                formatDuration(System.currentTimeMillis() - startTime),
                maxSpeed,
                lastLocation != null ? lastLocation.getAccuracy() : 0.0f);

        txtTripInfo.setText(info);
    }

    private void stopInfoUpdates() {
        if (updateHandler != null) {
            updateHandler.removeCallbacks(updateRunnable);
        }
    }

    private void showTripSummary() {
        long duration = System.currentTimeMillis() - startTime;
        float avgSpeed = totalDistance > 0 ? (totalDistance / 1000) / (duration / 3600000f) : 0;

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM", Locale.getDefault());
        String summary = String.format(Locale.getDefault(),
                "⏱ Thời gian: %s\n" +
                        "📏 Quãng đường: %.2f km\n" +
                        "📈 Tốc độ TB: %.1f km/h\n" +
                        "🚀 Tốc độ tối đa: %.1f km/h\n\n" +
                        "Bắt đầu: %s\n" +
                        "Kết thúc: %s",
                formatDuration(duration),
                totalDistance / 1000,
                avgSpeed,
                maxSpeed,
                sdf.format(new Date(startTime)),
                sdf.format(new Date(System.currentTimeMillis())));

        new AlertDialog.Builder(this)
                .setTitle("Thống kê chuyến đi")
                .setMessage(summary)
                .setPositiveButton("OK", null)
                .show();
    }

    private String formatDuration(long millis) {
        return String.format(Locale.getDefault(), "%02d:%02d:%02d",
                (millis / 3600000),
                (millis / 60000) % 60,
                (millis / 1000) % 60);
    }

    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_CODE);
            return false;
        }
        return true;
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_TIME_MS,
                    MIN_DISTANCE_M,
                    locationListener
            );
        }
    }

    private void stopLocationUpdates() {
        locationManager.removeUpdates(locationListener);
    }

    private void resetTripData() {
        totalDistance = 0;
        maxSpeed = 0;
        lastLocation = null;
        getActiveSpeedometer().reset();
    }

    private SpeedometerView getActiveSpeedometer() {
        return isLandscape ? speedometerLandscape : speedometerPortrait;
    }

    private float getCurrentSpeed() {
        return getActiveSpeedometer().getSpeed();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (isTracking) startLocationUpdates();
            } else {
                Toast.makeText(this, "Cần quyền truy cập vị trí để hoạt động!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isTracking) {
            stopLocationUpdates();
            stopInfoUpdates();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isTracking) {
            startLocationUpdates();
            startInfoUpdates();
        }
    }
}