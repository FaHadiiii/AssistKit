package com.publilius.scroller.util

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.publilius.scroller.service.AutoScrollAccessibilityService

data class PermissionStatus(
    val accessibilityEnabled: Boolean,
    val overlayEnabled: Boolean,
) {
    val allGranted: Boolean = accessibilityEnabled && overlayEnabled
}

object PermissionStatusChecker {
    fun read(context: Context): PermissionStatus {
        return PermissionStatus(
            accessibilityEnabled = isAccessibilityServiceEnabled(context),
            overlayEnabled = Settings.canDrawOverlays(context),
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
