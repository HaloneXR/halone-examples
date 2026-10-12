package dev.halone.plugin.sample

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Process
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import dev.halone.plugin.ViewPanelSession

/** Ordinary Android views in a window: buttons, a text field for a physical keyboard, the window calls, a long list to scroll. */
class ViewsSession(private val provider: SampleProvider, private val index: Int) : ViewPanelSession() {
    private lateinit var size: TextView
    private var count = 0
    private var pinned = false
    private var backs = 0
    private lateinit var backLabel: TextView

    override fun onCreateView(context: Context): View {
        host.setTitle("Views demo $index")
        host.setBackButton(true)
        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(PADDING, PADDING, PADDING, PADDING)
        }
        column.addView(label(context, "Views demo $index", 30f))
        size = label(context, "", 18f).also(column::addView)
        backLabel = label(context, "The title bar's Back button counts here: 0", 16f).also(column::addView)
        column.addView(label(context, "Views built at ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.ROOT).format(java.util.Date())}; a resize that rebuilt them would change this", 16f))
        updateSize()

        column.addView(row(
            context,
            button(context, "Count: 0") { count++; (it as Button).text = "Count: $count" },
            button(context, "Wider") { host.requestResize(widthPx + STEP, heightPx) },
            button(context, "Narrower") { host.requestResize(widthPx - STEP, heightPx) },
        ))

        val field = EditText(context).apply {
            hint = "Type here with a keyboard"
            inputType = InputType.TYPE_CLASS_TEXT
            setSingleLine()
        }
        column.addView(field, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        column.addView(row(
            context,
            button(context, "Use as title") { host.setTitle(field.text.toString().ifBlank { "Views demo $index" }) },
            button(context, "Pin") {
                pinned = !pinned
                host.setPinned(pinned)
                (it as Button).text = if (pinned) "Unpin" else "Pin"
            },
        ))

        column.addView(row(
            context,
            button(context, "New window") { provider.openPanel(SampleProvider.VIEWS, Bundle().apply { putInt(INDEX, index + 1) }) },
            button(context, "Test pattern") { provider.launchApp(SampleProvider.PATTERN) },
            button(context, "Close") { host.close() },
            button(context, "Stop plugin") { Process.killProcess(Process.myPid()) },
        ))

        val pressMe = label(context, "Long-press or ask for a context menu here", 18f).apply {
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.rgb(0x2b, 0x3a, 0x55))
            setOnLongClickListener {
                text = "Context menu asked for"
                true
            }
        }
        column.addView(pressMe, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = 16 })

        (1..LIST_ROWS).forEach { column.addView(label(context, "Row $it of $LIST_ROWS: scroll to see the rest", 18f).apply { setPadding(0, 14, 0, 14) }) }
        return ScrollView(context).apply {
            setBackgroundColor(Color.rgb(0x16, 0x18, 0x1d))
            addView(column)
        }
    }

    override fun onBack() {
        backs++
        backLabel.text = "The title bar's Back button counts here: $backs"
    }

    override fun onResize() {
        super.onResize()
        updateSize()
    }

    private fun updateSize() {
        if (::size.isInitialized) size.text = "$widthPx × $heightPx at $densityDpi dpi"
    }

    private fun label(context: Context, text: String, sp: Float) = TextView(context).apply {
        this.text = text
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
    }

    private fun button(context: Context, text: String, onClick: (View) -> Unit) = Button(context).apply {
        this.text = text
        isAllCaps = false
        setOnClickListener(onClick)
    }

    private fun row(context: Context, vararg views: View) = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        views.forEach { addView(it, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = 12 }) }
    }

    companion object {
        const val INDEX = "index"
        private const val PADDING = 32
        private const val STEP = 200
        private const val LIST_ROWS = 30
    }
}
