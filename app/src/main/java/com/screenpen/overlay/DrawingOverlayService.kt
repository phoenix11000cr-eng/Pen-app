package com.screenpen.overlay

import android.app.Service
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class DrawingOverlayService : Service() {
    private data class Stroke(val path: Path, val color: Int, val width: Float)
    private lateinit var wm: WindowManager
    private var canvasView: DrawView? = null
    private var toolbar: LinearLayout? = null
    private var floating: TextView? = null
    private var drawing = false
    private var color = Color.RED
    private var width = 7f

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        val floatParams = WindowManager.LayoutParams(-2, -2, type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.END; x = 18; y = 180
        }
        floating = TextView(this).apply {
            text = "✎"; textSize = 24f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(21, 101, 192)); setPadding(18, 10, 18, 10)
            setOnClickListener { showToolbar() }
        }
        wm.addView(floating, floatParams)
        canvasView = DrawView().also { view ->
            val p = WindowManager.LayoutParams(-1, -1, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.START }
            wm.addView(view, p)
        }
    }

    private fun showToolbar() {
        toolbar?.let { wm.removeView(it); toolbar = null; return }
        val bar = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(8, 8, 8, 8); setBackgroundColor(Color.argb(245, 30, 38, 48)) }
        fun button(label: String, action: () -> Unit) {
            bar.addView(Button(this).apply { text = label; textSize = 12f; isAllCaps = false; setOnClickListener { action() } })
        }
        button(if (drawing) "✍ Draw: ON" else "✍ Draw: OFF") { drawing = !drawing; updateTouchMode(); showToolbar() }
        button("🔴 Red pen") { color = Color.RED; width = 7f }
        button("🔵 Blue pen") { color = Color.BLUE; width = 7f }
        button("🟢 Green pen") { color = Color.rgb(0, 150, 60); width = 7f }
        button("🖍 Highlighter") { color = Color.YELLOW; width = 22f }
        button("↩ Undo") { canvasView?.undo() }
        button("Clear all") { canvasView?.clear() }
        button("Close pen toolbar") { toolbar?.let { wm.removeView(it) }; toolbar = null }
        button("Exit Screen Pen") { stopSelf() }
        toolbar = bar
        val p = WindowManager.LayoutParams(-2, -2, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.END; x = 12; y = 250
        }
        wm.addView(bar, p)
        bar.bringToFront()
        updateTouchMode()
    }

    private fun updateTouchMode() {
        canvasView?.let { v ->
            val p = v.layoutParams as WindowManager.LayoutParams
            p.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                (if (!drawing) WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE else 0)
            wm.updateViewLayout(v, p)
            v.drawingEnabled = drawing
        }
    }

    override fun onDestroy() {
        listOfNotNull(toolbar, canvasView, floating).forEach { try { wm.removeView(it) } catch (_: Exception) {} }
        toolbar = null; canvasView = null; floating = null
        super.onDestroy()
    }

    private inner class DrawView : View(this@DrawingOverlayService) {
        private val strokes = mutableListOf<Stroke>()
        private var active: Path? = null
        var drawingEnabled = false
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
        init { setLayerType(View.LAYER_TYPE_HARDWARE, null) }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            strokes.forEach { s -> paint.color = s.color; paint.strokeWidth = s.width; paint.alpha = if (s.color == Color.YELLOW) 105 else 255; canvas.drawPath(s.path, paint) }
            active?.let { paint.color = color; paint.strokeWidth = this@DrawingOverlayService.width; paint.alpha = if (color == Color.YELLOW) 105 else 255; canvas.drawPath(it, paint) }
        }

        private fun isStylus(event: MotionEvent): Boolean {
            return event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS ||
                event.getToolType(0) == MotionEvent.TOOL_TYPE_ERASER
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (!drawingEnabled || !isStylus(event)) return false

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    active = Path().apply { moveTo(event.x, event.y) }
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    active?.lineTo(event.x, event.y)
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    active?.let { strokes.add(Stroke(it, color, this@DrawingOverlayService.width)) }
                    active = null
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_CANCEL -> {
                    active = null
                    invalidate()
                    return true
                }
            }
            return false
        }
        fun undo() { if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex); invalidate() }
        fun clear() { strokes.clear(); active = null; invalidate() }
    }
}