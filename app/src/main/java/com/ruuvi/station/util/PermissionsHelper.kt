package com.ruuvi.station.util

import android.Manifest
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ruuvi.station.BuildConfig
import com.ruuvi.station.R
import timber.log.Timber

class PermissionsHelper(private val activity: AppCompatActivity) {
    private val requiredPermissions = mutableListOf(
        ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    ).also {
        if (BuildConfig.FILE_LOGS_ENABLED) it.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    fun arePermissionsGranted(): Boolean = getRequiredPermissions().isEmpty()

    fun requestPermissions() {
        val neededPermissions = getRequiredPermissions()
        if (neededPermissions.isNotEmpty()) {
            if (neededPermissions.size == 1 && neededPermissions.first() == Manifest.permission.ACCESS_FINE_LOCATION) {
                showPermissionDialog(activity)
            } else {
                val alertDialog = AlertDialog.Builder(activity).create()
                alertDialog.setTitle(activity.getString(R.string.permission_dialog_title))
                alertDialog.setMessage(activity.getString(R.string.permission_dialog_request_message))
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, activity.getString(R.string.ok)
                ) { dialog, _ -> dialog.dismiss() }
                alertDialog.setOnDismissListener { showPermissionDialog(activity) }
                alertDialog.show()
            }
        } else {
            enableBluetooth()
        }
    }

    private fun enableBluetooth(): Boolean {
        if (isBluetoothEnabled()) {
            return true
        }
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        activity.startActivityForResult(enableBtIntent, REQUEST_CODE_BLUETOOTH)
        return false
    }

    private fun getRequiredPermissions(): List<String> {
        return requiredPermissions.mapNotNull { permission ->
            val isPermissionGranted = ContextCompat.checkSelfPermission(activity, permission) == PERMISSION_GRANTED

            if (!isPermissionGranted) {
                Timber.d("$permission required")
                permission
            } else {
                null
            }
        }
    }

    private fun showPermissionDialog(activity: AppCompatActivity): Boolean {
        val requiredPermissions = getRequiredPermissions()

        if (requiredPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, requiredPermissions.toTypedArray(), REQUEST_CODE_PERMISSIONS)
        }

        return requiredPermissions.isNotEmpty()
    }

    private fun isBluetoothEnabled(): Boolean {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled
    }

    companion object {
        const val REQUEST_CODE_PERMISSIONS = 10
        const val REQUEST_CODE_BLUETOOTH = 87
    }
}