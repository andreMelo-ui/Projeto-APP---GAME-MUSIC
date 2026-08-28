package com.desafiomusical.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.desafiomusical.app.ui.navigation.DesafioMusicalNavHost
import com.desafiomusical.app.ui.theme.DesafioMusicalTheme

class MainActivity : ComponentActivity() {
    private val container by lazy { (application as DesafioMusicalApp).container }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DesafioMusicalTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DesafioMusicalNavHost(container = container)
                }
            }
        }
    }
}
