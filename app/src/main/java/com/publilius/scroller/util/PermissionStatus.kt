package com.publilius.scroller.util

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat
import com.publilius.scroller.service.AutoScrollAccessibilityService

data class PermissionStatus(
    val accessibilityEnabled: Boolean,
    val overlayEnabled: Boolean,
    val microphoneEnabled: Boolean,
) {
    val allGranted: Boolean = accessibilityEnabled && overlayEnabled
}

object PermissionStatusChecker {
    fun read(context: Context): PermissionStatus {
        return PermissionStatus(
            accessibilityEnabled = isAccessibilityServiceEnabled(context),
            overlayEnabled = Settings.canDrawOverlays(context),
            microphoneEnabled = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK,
        )
        val expectedId = ComponentName(context, AutoScrollAccessibilityService::class.java).flattenToString()
        return enabledServices.any { it.resolveInfo.serviceInfo?.let { info ->
            ComponentName(info.packageName, info.name).flattenToString() == expectedId
        } == true }
    }
}
