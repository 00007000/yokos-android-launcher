package com.yokos.bb10launcher.launcher

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.yokos.bb10launcher.ui.theme.Bb10Theme
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

/** What the home screen should do after the Home button or the peek gesture brings it forward. */
enum class HomeCommand { GoHome, OpenHub }

class LauncherActivity : ComponentActivity() {
    private val viewModel: LauncherViewModel by viewModels()
    private val commands = Channel<HomeCommand>(Channel.CONFLATED)
    private val commandFlow = commands.receiveAsFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            Bb10Theme {
                LauncherRoot(commands = commandFlow, viewModel = viewModel)
            }
        }
        handleIntent(intent, fromNewIntent = false)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent, fromNewIntent = true)
    }

    override fun onStart() {
        super.onStart()
        viewModel.widgets.host.startListening()
    }

    override fun onStop() {
        super.onStop()
        viewModel.widgets.host.stopListening()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    private fun handleIntent(intent: Intent, fromNewIntent: Boolean) {
        when {
            intent.getBooleanExtra(EXTRA_OPEN_HUB, false) -> commands.trySend(HomeCommand.OpenHub)
            fromNewIntent && intent.hasCategory(Intent.CATEGORY_HOME) -> commands.trySend(HomeCommand.GoHome)
        }
    }

    companion object {
        const val EXTRA_OPEN_HUB = "com.yokos.bb10launcher.extra.OPEN_HUB"

        fun hubIntent(context: Context): Intent = Intent(context, LauncherActivity::class.java)
            .putExtra(EXTRA_OPEN_HUB, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
    }
}
