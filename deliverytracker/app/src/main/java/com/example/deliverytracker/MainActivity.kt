package com.example.deliverytracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.location.LocationListener
import okhttp3.*
import java.io.IOException

class MainActivity : AppCompatActivity(), LocationListener {

    private lateinit var locationManager: LocationManager
    private val client = OkHttpClient()
    private var isTracking = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        val shareLocationButton = findViewById<Button>(R.id.shareLocationButton)
        shareLocationButton.setOnClickListener {
            if (!isTracking) {
                if (checkLocationPermission()) {
                    if (isGPSEnabled()) {
                        startLocationUpdates()
                        isTracking = true
                        shareLocationButton.text = "Stop Sharing Location"
                        Toast.makeText(this, "Location tracking started", Toast.LENGTH_SHORT).show()
                    } else {
                        showGPSDisabledAlert()
                    }
                } else {
                    requestLocationPermission()
                }
            } else {
                stopLocationUpdates()
                isTracking = false
                shareLocationButton.text = "Start Sharing Location"
                Toast.makeText(this, "Location tracking stopped", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isGPSEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun showGPSDisabledAlert() {
        AlertDialog.Builder(this)
            .setMessage("GPS is disabled. Would you like to enable it?")
            .setCancelable(false)
            .setPositiveButton("Yes") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.cancel()
                Toast.makeText(this, "GPS is required for location tracking", Toast.LENGTH_SHORT).show()
            }
            .create()
            .show()
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun startLocationUpdates() {
        if (checkLocationPermission()) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000L,
                10f,
                this
            )
        } else {
            Log.e("MainActivity", "Location permission not granted")
        }
    }

    private fun stopLocationUpdates() {
        locationManager.removeUpdates(this)
    }

    private fun sendLocationToServer(location: Location) {
        val url = "http://192.168.0.180:3000/update-location"
        // val url = "http://your-computer-ip:3000/update-location" // Use this when testing with a physical device
        val requestBody = FormBody.Builder()
            .add("latitude", location.latitude.toString())
            .add("longitude", location.longitude.toString())
            .build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("MainActivity", "Failed to send location", e)
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Failed to send location: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val responseBody = response.body?.string()
                    Log.d("MainActivity", "Response: $responseBody")
                    if (!response.isSuccessful) {
                        Log.e("MainActivity", "Unexpected code $response")
                    } else {
                        Log.d("MainActivity", "Location sent successfully")
                    }
                }
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (isGPSEnabled()) {
                    startLocationUpdates()
                    isTracking = true
                    findViewById<Button>(R.id.shareLocationButton).text = "Stop Sharing Location"
                    Toast.makeText(this, "Location tracking started", Toast.LENGTH_SHORT).show()
                } else {
                    showGPSDisabledAlert()
                }
            } else {
                Log.e("MainActivity", "Location permission denied")
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onLocationChanged(location: Location) {
        sendLocationToServer(location)
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
}