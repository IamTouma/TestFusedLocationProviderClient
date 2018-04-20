package com.example.testfusedlocationproviderclient

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.widget.Toast
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.text.DateFormat
import java.util.*


/**
 * Created by ohmi on 2018/03/27.
 */
class LocationActivity : Activity(), LocationListener {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var settingsClient: SettingsClient
    private lateinit var locationSettingsRequest: LocationSettingsRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private lateinit var location: Location

    private var priority: Int = 0
    private lateinit var lastUpdateTime: String
    private var requestingLocationUpdates: Boolean = false

    private lateinit var textLog: String
    private lateinit var localStorage :LocalStorage



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        settingsClient = LocationServices.getSettingsClient(this)

        createLocationCallback()
        createLocationRequest()
        buildLocationSettingsRequest()

        localStorage = LocalStorage(File(filesDir, "log.txt").path)

        textLog = "onCreate()\n"
        text_view.text = textLog
        localStorage.Write(textLog)

        button_start.setOnClickListener {
            startLocationUpdates()
        }

        button_stop.setOnClickListener {
            stopLocationUpdates()
        }

       button_clear.setOnClickListener {
            textLog = ""
           text_view.text = textLog
           localStorage.Write(textLog)
        }

        button_priority.setOnClickListener {
            priority = editText.text.toString().toInt()
        }
    }

    private fun createLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult?) {
                super.onLocationResult(p0)

                location = p0!!.lastLocation
                lastUpdateTime = DateFormat.getInstance().format(Date())
                updateLocationUI()
            }
        }
    }

    private fun updateLocationUI() {
        if (location != null) {
            var fusedName = arrayOf("Latitude", "Longitude", "Accuracy",
                    "Altitude", "Speed", "Bearing")

            var fusedData = arrayOf(location.latitude,
                    location.longitude,
                    location.accuracy,
                    location.altitude,
                    location.speed,
                    location.bearing)

            val strBuf = StringBuilder("---------- UpdateLocation ---------- \n")

            for (i in fusedName.indices) {
                strBuf.append("${fusedName[i]} = ${fusedData[i]}\n")
            }

            strBuf.append("Time = $lastUpdateTime\n")

            textLog += strBuf
            text_view.text = textLog
            localStorage.Write(textLog)
        }
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest.create()

        when (priority) {
            0 -> // 高い精度の位置情報を取得したい場合
                // インターバルを例えば5000msecに設定すれば
                // マップアプリのようなリアルタイム測位となる
                // 主に精度重視のためGPSが優先的に使われる
                locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            1 -> // バッテリー消費を抑えたい場合、精度は100mと悪くなる
                // 主にwifi,電話網での位置情報が主となる
                // この設定の例としては　setInterval(1時間)、setFastestInterval(1分)
                locationRequest.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
            2 -> // バッテリー消費を抑えたい場合、精度は10kmと悪くなる
                locationRequest.priority = LocationRequest.PRIORITY_LOW_POWER
            else -> // 受け身的な位置情報取得でアプリが自ら測位せず、
                // 他のアプリで得られた位置情報は入手できる
                locationRequest.priority = LocationRequest.PRIORITY_NO_POWER
        }

        // アップデートのインターバル期間設定
        // このインターバルは測位データがない場合はアップデートしません
        // また状況によってはこの時間よりも長くなることもあり
        // 必ずしも正確な時間ではありません
        // 他に同様のアプリが短いインターバルでアップデートしていると
        // それに影響されインターバルが短くなることがあります。
        // 単位：msec
        locationRequest.interval = 60000
        // このインターバル時間は正確です。これより早いアップデートはしません。
        // 単位：msec
        locationRequest.fastestInterval = 5000

    }

    private fun buildLocationSettingsRequest() {
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(locationRequest)
        locationSettingsRequest = builder.build()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            0x1 -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        Log.i("debug", "User agreed to make required location settings changes.");
                        // Nothing to do. startLocationupdates() gets called in onResume again.
                    }

                    Activity.RESULT_CANCELED -> {
                        Log.i("debug", "User chose not to make required location settings changes.");
                        requestingLocationUpdates = false
                    }
                }
            }
        }
    }

    private fun startLocationUpdates() {
        settingsClient.checkLocationSettings(locationSettingsRequest)
                .addOnSuccessListener {
                    Log.i("debug", "All location settings are satisfied.")

                    val fineLocation = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    val coarseLocation = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                    if (fineLocation != PackageManager.PERMISSION_GRANTED
                    && coarseLocation != PackageManager.PERMISSION_GRANTED) {
                        return@addOnSuccessListener
                    }
                    fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
                }
                .addOnFailureListener {
                    when ((it as ApiException).statusCode) {
                        LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                            Log.i("debug", "Location settings are not satisfied. Attempting to upgrade " +
                                    "location settings ");

                            try {
                                (it as ResolvableApiException).startResolutionForResult(this, 0x01)
                            }
                            catch (sie: IntentSender.SendIntentException) {
                                Log.i("debug", "PendingIntent unable to execute request.")
                            }
                        }

                        LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                            val errMsg: String = "Location settings are inadequate, and cannot be fixed here. Fix in Settings."
                            Log.e("debug", errMsg)
                            Toast.makeText(this, errMsg, Toast.LENGTH_LONG).show()

                            requestingLocationUpdates = false
                        }
                    }
                }
        requestingLocationUpdates = true
    }

    private fun stopLocationUpdates() {
        textLog += "onStop()\n"
        text_view.text = textLog
        localStorage.Write(textLog)

        if (!requestingLocationUpdates) {
            Log.d("debug", "stopLocationUpdates: updates never requested, no-op.");
            return
        }

        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
                .addOnCompleteListener {
                    requestingLocationUpdates = false
                }
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    override fun onLocationChanged(p0: Location?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
