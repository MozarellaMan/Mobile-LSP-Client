package com.example.lsp_client

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.setContent
import com.example.lsp_client.server.ScreenContent
import com.example.lsp_client.ui.LSPClientTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApp {
                ScreenContent()
            }
        }
    }
}

@Composable
fun MyApp(content: @Composable () -> Unit) {
    LSPClientTheme {
        Surface(color = Color.Black) {
            content()
        }
    }
}

@Composable
fun DefaultPreview() {
    MyApp {
        ScreenContent()
    }
}