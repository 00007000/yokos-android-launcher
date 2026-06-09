package app.lawnchair.bb10hub

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class Bb10HubActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val textView = TextView(this)
        textView.text = "BB10 Hub"
        textView.setTextColor(android.graphics.Color.WHITE)
        textView.setBackgroundColor(android.graphics.Color.BLACK)
        setContentView(textView)
    }
}
