package com.westly.snakes

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.westly.snakes.data.Prefs
import com.westly.snakes.game.GameMode
import com.westly.snakes.ui.GameScreen
import com.westly.snakes.ui.SplashScreen
import com.westly.snakes.ui.StartScreen

/** The two screens behind the splash: the Start screen and a game in one mode. */
private sealed class AppScreen {
    object Start : AppScreen()
    class Game(val mode: GameMode) : AppScreen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        setContent {
            val prefs = remember { Prefs(applicationContext) }
            var showSplash by remember { mutableStateOf(true) }
            var screen by remember { mutableStateOf<AppScreen>(AppScreen.Start) }

            // Back on the Start screen closes the app (during a game the game screen asks first)
            BackHandler(enabled = screen is AppScreen.Start) { finish() }

            Box(modifier = Modifier.fillMaxSize()) {
                when (val current = screen) {
                    is AppScreen.Start -> StartScreen(
                        prefs = prefs,
                        onStart = { mode -> screen = AppScreen.Game(mode) },
                        onClose = { finish() }
                    )
                    is AppScreen.Game -> GameScreen(
                        mode = current.mode,
                        prefs = prefs,
                        onLeave = { screen = AppScreen.Start }
                    )
                }
                // the splash plays on top and fades out to reveal the Start screen
                if (showSplash) {
                    SplashScreen(onFinished = { showSplash = false })
                }
            }
        }
    }
}
