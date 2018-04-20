package com.example.testfusedlocationproviderclient

import android.Manifest.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    private val REQUEST_PERMISSION = 1000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d("MainActivity", "onCreate()");

        if(Build.VERSION.SDK_INT >= 23){
            checkPermission()
        }
        else{
            locationActivity()
        }
    }

    // 位置情報許可の確認
    private fun checkPermission() {
        val permissionCheck = ActivityCompat.checkSelfPermission(this,
                permission.ACCESS_FINE_LOCATION)
        // 既に許可している
        if (permissionCheck == PackageManager.PERMISSION_GRANTED
        ) {
            locationActivity()
        }
        else {
            requestLocationPermission()
        }
    }

    // 許可要請
    private fun requestLocationPermission() {
        val requestPermissionRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,
                permission.ACCESS_FINE_LOCATION)

        if (requestPermissionRationale) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(permission.ACCESS_FINE_LOCATION),
                    REQUEST_PERMISSION)
        }
        else {
            Toast.makeText(this, "許可しないと実行できないよ", Toast.LENGTH_LONG).show()
            ActivityCompat.requestPermissions(this,
                    arrayOf(permission.ACCESS_FINE_LOCATION),
                    REQUEST_PERMISSION)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationActivity()
            }
            else {
                Toast.makeText(this, "これ以上何もできまへん", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun locationActivity() {
        startActivity(Intent(application, LocationActivity::class.java))
    }
}
