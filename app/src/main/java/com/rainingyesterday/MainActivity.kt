package com.rainingyesterday

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rainingyesterday.ui.navigation.RainingYesterdayNavHost
import com.rainingyesterday.ui.theme.RainingYesterdayTheme
import dagger.hilt.android.AndroidEntryPoint

/** 唯一 Activity：挂 Compose 根；导航骨架见 ui/navigation。 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RainingYesterdayTheme {
                RainingYesterdayNavHost()
            }
        }
    }
}