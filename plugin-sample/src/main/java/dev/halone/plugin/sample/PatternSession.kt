package dev.halone.plugin.sample

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.os.SystemClock
import android.view.Choreographer
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Surface
import dev.halone.plugin.KeyboardRequest
import dev.halone.plugin.KeyboardState
import dev.halone.plugin.PanelSession
import dev.halone.plugin.TextInputEvent

/**
 * A moving test pattern drawn with a hardware Canvas, showing the input it
 * receives. A tap on the dark strip brings up the phone's keyboard, whose
 * edits this session applies itself, as a Surface-mode plugin must.
 */
class PatternSession : PanelSession(), Choreographer.FrameCallback {
    private var surface: Surface? = null
    private val trail = ArrayList<PointF>()
    private var hover: PointF? = null
    private var wheel = 0f
    private var lastKey = "none"
    private var lastEvent = "none"
    private var frames = 0
    private var fpsSince = 0L
    private var fps = 0
    private var typed = ""
    private var composingStart = -1
    private var composingEnd = -1
    private var keyboardUp = false

    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fill = Paint()

    override fun onAttach(surface: Surface) {
        this.surface = surface
        fpsSince = SystemClock.uptimeMillis()
        frames = 0
        Choreographer.getInstance().removeFrameCallback(this)
        Choreographer.getInstance().postFrameCallback(this)
        host.setTitle("Test pattern")
    }

    override fun onDetach() = stop()

    override fun onClose() = stop()

    private fun stop() {
        Choreographer.getInstance().removeFrameCallback(this)
        surface = null
    }

    override fun onMotionEvent(event: MotionEvent) {
        val tool = when (event.getToolType(0)) {
            MotionEvent.TOOL_TYPE_FINGER -> "finger"
            MotionEvent.TOOL_TYPE_MOUSE -> "mouse"
            MotionEvent.TOOL_TYPE_STYLUS -> "stylus"
            else -> "unknown tool"
        }
        lastEvent = "${MotionEvent.actionToString(event.actionMasked)} (source 0x${Integer.toHexString(event.source)}, $tool)"
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                trail.clear()
                trail += PointF(event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> trail += PointF(event.x, event.y)
            MotionEvent.ACTION_UP -> {
                trail += PointF(event.x, event.y)
                if (trail.size <= 2 && event.y > heightPx * STRIP) toggleKeyboard()
            }
            MotionEvent.ACTION_HOVER_MOVE -> hover = PointF(event.x, event.y)
            MotionEvent.ACTION_SCROLL -> wheel += event.getAxisValue(MotionEvent.AXIS_VSCROLL)
        }
        if (trail.size > MAX_TRAIL) trail.subList(0, trail.size - MAX_TRAIL).clear()
    }

    override fun onKeyEvent(event: KeyEvent) {
        if (event.action != KeyEvent.ACTION_DOWN) return
        val char = event.unicodeChar
        lastKey = if (char > 32) "'${char.toChar()}'" else KeyEvent.keyCodeToString(event.keyCode).removePrefix("KEYCODE_")
        when {
            event.keyCode == KeyEvent.KEYCODE_DEL -> replace(maxOf(0, typed.length - 1), typed.length, "")
            char >= 32 -> replace(typed.length, typed.length, char.toChar().toString())
            else -> return
        }
        composingStart = -1
        sync()
    }

    private fun toggleKeyboard() {
        keyboardUp = !keyboardUp
        if (keyboardUp) host.showKeyboard(KeyboardRequest(hint = "Type on the test pattern", state = KeyboardState(typed))) else host.hideKeyboard()
    }

    /** A one-line editor with the cursor kept at the end: enough to show the events, not a real field. */
    override fun onTextInput(event: TextInputEvent) {
        lastEvent = event::class.simpleName ?: "text"
        when (event) {
            is TextInputEvent.Commit -> {
                replaceComposing(event.text)
                composingStart = -1
            }
            is TextInputEvent.Compose -> {
                val start = replaceComposing(event.text)
                composingStart = start
                composingEnd = start + event.text.length
            }
            is TextInputEvent.ComposingRegion -> {
                composingStart = event.start.coerceIn(0, typed.length)
                composingEnd = event.end.coerceIn(composingStart, typed.length)
            }
            TextInputEvent.FinishComposing -> composingStart = -1
            is TextInputEvent.DeleteSurrounding -> replace(maxOf(0, typed.length - event.before), typed.length, "")
            is TextInputEvent.Select -> Unit
            is TextInputEvent.EditorAction -> {
                keyboardUp = false
                host.hideKeyboard()
            }
            TextInputEvent.Dismissed -> keyboardUp = false
        }
    }

    private fun replaceComposing(text: String): Int {
        val start = if (composingStart >= 0) composingStart else typed.length
        val end = if (composingStart >= 0) composingEnd else typed.length
        replace(start, end, text)
        return start
    }

    private fun replace(start: Int, end: Int, text: String) {
        typed = (typed.substring(0, start) + text + typed.substring(end)).takeLast(MAX_TYPED)
    }

    private fun sync() {
        if (keyboardUp) host.updateKeyboard(KeyboardState(typed))
    }

    override fun onContextMenu(x: Float, y: Float) {
        lastEvent = "context menu at ${x.toInt()}, ${y.toInt()}"
    }

    override fun doFrame(frameTimeNanos: Long) {
        val target = surface ?: return
        val canvas = runCatching { target.lockHardwareCanvas() }.getOrNull()
        if (canvas != null) {
            draw(canvas, frameTimeNanos / 1_000_000)
            target.unlockCanvasAndPost(canvas)
        }
        frames++
        val now = SystemClock.uptimeMillis()
        if (now - fpsSince >= 1000) {
            fps = (frames * 1000 / (now - fpsSince)).toInt()
            frames = 0
            fpsSince = now
        }
        Choreographer.getInstance().postFrameCallback(this)
    }

    private fun draw(canvas: Canvas, timeMs: Long) {
        val w = widthPx.toFloat()
        val h = heightPx.toFloat()
        val barWidth = w / BARS.size
        BARS.forEachIndexed { i, colour ->
            fill.color = colour
            canvas.drawRect(i * barWidth, 0f, (i + 1) * barWidth, h * STRIP, fill)
        }
        fill.color = Color.rgb(0x18, 0x1a, 0x20)
        canvas.drawRect(0f, h * STRIP, w, h, fill)

        line.color = Color.argb(70, 255, 255, 255)
        line.strokeWidth = 1f
        var gx = 0f
        while (gx < w) { canvas.drawLine(gx, 0f, gx, h, line); gx += GRID }
        var gy = 0f
        while (gy < h) { canvas.drawLine(0f, gy, w, gy, line); gy += GRID }

        val sweep = (timeMs % SWEEP_MS) / SWEEP_MS.toFloat() * w
        fill.color = Color.argb(160, 0, 0, 0)
        canvas.drawRect(sweep - 6f, 0f, sweep + 6f, h * STRIP, fill)

        line.color = Color.WHITE
        line.strokeWidth = 3f
        canvas.drawRect(1.5f, 1.5f, w - 1.5f, h - 1.5f, line)

        text.textSize = 30f
        val left = 32f
        var y = h * STRIP + 52f
        listOf(
            "Halone plugin · Surface mode",
            "${widthPx} × ${heightPx} at ${densityDpi} dpi · $fps fps",
            "Last event: $lastEvent · wheel ${"%.1f".format(wheel)}",
            "Last key: $lastKey",
            if (keyboardUp || typed.isNotEmpty()) "Typed: $typed▏" else "Tap here for the phone keyboard",
        ).forEach {
            canvas.drawText(it, left, y, text)
            y += 44f
        }

        if (trail.size > 1) {
            val path = Path().apply {
                moveTo(trail[0].x, trail[0].y)
                trail.drop(1).forEach { lineTo(it.x, it.y) }
            }
            line.color = Color.rgb(0xff, 0x45, 0x8a)
            line.strokeWidth = 10f
            canvas.drawPath(path, line)
        }
        hover?.let {
            line.color = Color.rgb(0x7f, 0xd1, 0xae)
            line.strokeWidth = 3f
            canvas.drawCircle(it.x, it.y, 18f, line)
        }
    }

    private companion object {
        const val MAX_TRAIL = 400
        const val MAX_TYPED = 60
        const val STRIP = 0.58f
        const val GRID = 64f
        const val SWEEP_MS = 3000L
        val BARS = intArrayOf(
            Color.rgb(0xe8, 0xe8, 0xe8), Color.rgb(0xe8, 0xd4, 0x4d), Color.rgb(0x4d, 0xc8, 0xe8), Color.rgb(0x5b, 0xd4, 0x5b),
            Color.rgb(0xd4, 0x4d, 0xd4), Color.rgb(0xe0, 0x4d, 0x4d), Color.rgb(0x4d, 0x5b, 0xe0),
        )
    }
}
