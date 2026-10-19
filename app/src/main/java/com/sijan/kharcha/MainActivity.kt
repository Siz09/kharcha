package com.sijan.kharcha

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.IntentCompat
import com.sijan.kharcha.ui.KharchaApp
import com.sijan.kharcha.ui.theme.KharchaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialImageUri: Uri? = if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {
            IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
        } else null

        setContent {
            KharchaTheme {
                KharchaApp(initialImageUri = initialImageUri)
            }
        }
    }
}
