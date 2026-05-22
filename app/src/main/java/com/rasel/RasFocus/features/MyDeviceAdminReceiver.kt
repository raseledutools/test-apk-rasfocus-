package com.rasel.RasFocus.features

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class MyDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, "Rasfocus Admin Enabled! Uninstallation is now protected.", Toast.LENGTH_SHORT).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        // যখন কেউ অ্যাডমিন অফ করতে যাবে, তখন এই মেসেজটা স্ক্রিনে ভাসবে
        return "Warning: Disabling this will break your focus streak and allow uninstallation!"
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(context, "Admin Disabled. Protection removed.", Toast.LENGTH_SHORT).show()
    }
}
