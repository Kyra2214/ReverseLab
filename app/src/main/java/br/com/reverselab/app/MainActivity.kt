package br.com.reverselab.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import br.com.reverselab.app.ui.ReverseLabApp
import br.com.reverselab.app.ui.theme.ReverseLabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ReverseLabTheme { ReverseLabApp() }
        }
    }
}
