package com.concordium.wallet.ui.onramp

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

class PermissionManager(
    private val activity: Activity,
    private val permissionResultLauncher: ActivityResultLauncher<Array<String>>
) {
    fun arePermissionsGranted(): Boolean {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    fun requestPermissions() {
        permissionResultLauncher.launch(
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        )
    }
}