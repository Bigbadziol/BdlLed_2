package com.badziol.bdlled_02

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.content.Context
import androidx.core.content.ContextCompat

/*
    Check if got all required permissions to work with bluetooth.
    For Android 12+ are real-time , for Android 11 or less are not.
 */
fun gotBTPerms(context: Context, showTestInLogs :Boolean ): Boolean {
        var gotAllPerms = true
        val permissionsRequired =
            if (Build.VERSION.SDK_INT <= 30) { //android 11 or less
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            } else {
                arrayOf(
                    Manifest.permission.BLUETOOTH, //BUG XIAOMI
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                )
            }
        var permNum = 0
         if (showTestInLogs) {
             val sdk = Build.VERSION.SDK_INT
             Log.d(TAG, "Self test - permissions , testing ${permissionsRequired.size} entries , SDK :  $sdk")
         }
        permissionsRequired.forEach { requiredPermission ->
            permNum++
            if (ContextCompat.checkSelfPermission(
                    context,
                    requiredPermission
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                if (showTestInLogs) {
                    Log.d(TAG, "$permNum ) $requiredPermission -> IS GRANTED")
                }
            } else {
                //if (showTestInLogs) {
                //show always if not granted
                    Log.d(TAG, "$permNum ) $requiredPermission -> NOT GRANTED")
                //}
                gotAllPerms = false
            }
        }
        if (showTestInLogs) {
            Log.d(TAG, "Result of self check permissions : $gotAllPerms")
        }
        return gotAllPerms
    }

/*
 This is a old version , no need any more.
 In fact there were 2 similar functions , doing the same thing.
 gotBTPerms is much more compact.

*/
/*
fun gotBtPerms(context: Context, errorMessage : String) : Boolean {
    var gotPerm = true
    //Android 11 or less
    if (Build.VERSION.SDK_INT <= 30) {

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
    if (Build.VERSION.SDK_INT >= 31) {
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
*/