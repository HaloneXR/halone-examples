package dev.halone.plugin.sample

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import dev.halone.plugin.PanelProviderService
import dev.halone.plugin.PanelSession
import dev.halone.plugin.PluginApp
import dev.halone.plugin.StatusItem

/** Two apps that exercise the plugin contract, one in Surface mode and one in View mode, and a status item in the dock. */
class SampleProvider : PanelProviderService() {
    private var level = 2

    override fun onCreate() {
        super.onCreate()
        showLevel()
    }

    /** A signal meter, as a network plugin would show one; each click steps it. */
    override fun onStatusItemClicked(id: String) {
        level = (level + 1) % (BARS + 1)
        showLevel()
    }

    private fun showLevel() = setStatusItem(
        StatusItem(METER, meter(level), label = "Sample plugin: $level of $BARS bars. Click to change.", text = "S$level"),
    )

    private fun meter(filled: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(METER_PX, METER_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val gap = METER_PX / 16f
        val width = (METER_PX - gap * (BARS - 1)) / BARS
        for (i in 0 until BARS) {
            paint.color = Color.argb(if (i < filled) 255 else 70, 255, 255, 255)
            val left = i * (width + gap)
            canvas.drawRoundRect(left, METER_PX * (1 - (i + 1f) / BARS), left + width, METER_PX.toFloat(), gap, gap, paint)
        }
        return bitmap
    }

    override fun apps() = listOf(
        PluginApp(PATTERN, "Test pattern", "Surface mode: drawn with a Canvas, echoes input", 1024, 640, icon = R.drawable.ic_pattern, color = 0xFF5B2A86.toInt()),
        PluginApp(VIEWS, "Views demo", "View mode: ordinary Android views", 900, 760),
    )

    override fun createSession(appId: String, args: Bundle): PanelSession? = when (appId) {
        PATTERN -> PatternSession()
        VIEWS -> ViewsSession(this, args.getInt(ViewsSession.INDEX, 1))
        else -> null
    }

    companion object {
        const val PATTERN = "pattern"
        const val VIEWS = "views"
        private const val METER = "meter"
        private const val METER_PX = 64
        private const val BARS = 4
    }
}
