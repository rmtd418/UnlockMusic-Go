package dev.unlockmusic.android.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.unlockmusic.android.app.theme.UnlockMusicTheme
import dev.unlockmusic.android.app.ui.UnlockApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            UnlockMusicTheme {
                UnlockApp()
            }
        }
    }
}

