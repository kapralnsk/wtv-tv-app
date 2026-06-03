package tv.wtv.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import tv.wtv.app.navigation.AppNavigation
import tv.wtv.app.ui.theme.WtvTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WtvTheme {
                AppNavigation()
            }
        }
    }
}
