package com.screenpen.overlay

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 28, 28, 28)
            setBackgroundColor(Color.rgb(245, 248, 252))
        }
        root.addView(TextView(this).apply {
            text = "Screen Pen\n\nScreen ke upar kahin bhi likhein"
            textSize = 24f; setTextColor(Color.rgb(20, 55, 90)); gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(Button(this).apply {
            text = "1. Overlay permission dein"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                } else startOverlay()
            }
        }, LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(Button(this).apply {
            text = "2. Screen Pen shuru karein"
            setOnClickListener {
                if (Settings.canDrawOverlays(this@MainActivity)) startOverlay()
                else startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            }
        }, LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(TextView(this).apply {
            text = "Use: Floating ✎ button dabayein, Draw mode on karein. Touch mode se app dobara use kar sakte hain."
            textSize = 15f; setTextColor(Color.DKGRAY); setPadding(0, 24, 0, 0)
        })
        setContentView(root)
    }

    private fun startOverlay() {
        if (!Settings.canDrawOverlays(this)) return
        startService(Intent(this, DrawingOverlayService::class.java))
        finish()
    }
}
