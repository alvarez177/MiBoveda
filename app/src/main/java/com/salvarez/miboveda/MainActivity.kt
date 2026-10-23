package com.salvarez.miboveda

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.salvarez.miboveda.presentation.navigation.MiBovedaNavHost
import com.salvarez.miboveda.presentation.theme.MiBovedaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MiBovedaTheme {
                MiBovedaNavHost()
            }
        }
    }
}
