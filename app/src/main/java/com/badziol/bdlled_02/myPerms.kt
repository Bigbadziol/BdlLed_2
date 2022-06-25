package com.badziol.bdlled_02

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import android.content.Context
/*

*/

fun gotBtPerms(context: Context, errorMessage : String) : Boolean{
    var gotPerm  = true
    //Android 11 or less
    if (Build.VERSION.SDK_INT <=30) {

        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            gotPerm = false
            Log.d(TAG, "$errorMessage - no  permission (API) <=30 , BLUETOOTH")
        }

        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_ADMIN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            gotPerm = false
            Log.d(TAG, "$errorMessage - no  permission (API) <=30 , BLUETOOTH_ADMIN")
        }
    }
    //Android 12+
    if (Build.VERSION.SDK_INT >=31) {
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            gotPerm = false
            Log.d(TAG, "$errorMessage  - no  permission (API) >= 31 , BLUETOOTH (FOR XIAOMI BUG) ")
        }

        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            gotPerm = false
            Log.d(TAG, "$errorMessage  - no  permission (API) >= 31 , BLUETOOTH_SCAN ")
        }

        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            gotPerm = false
            Log.d(TAG, "$errorMessage  - no  permission (API) >= 31 , BLUETOOTH_CONNECT ")
        }
    }
    return gotPerm
}