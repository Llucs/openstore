package com.llucs.openstore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.llucs.openstore.ui.OpenStoreRoot
import com.llucs.openstore.ui.theme.OpenStoreTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            OpenStoreTheme {
                OpenStoreRoot()
            }
        }
    }
}
