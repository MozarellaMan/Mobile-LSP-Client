package com.example.lsp_client

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.lsp_client.editor.ui.Editor
import com.example.lsp_client.server.StartupScreen
import com.example.lsp_client.ui.LSPClientTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppEntry()
        }
    }
}

@Composable
fun AppEntry() {
    LSPClientTheme {
        val navController = rememberNavController()
        Surface(color = Color.Black) {
            NavHost(navController = navController, startDestination = "startup") {
                composable("startup") { StartupScreen(navController) }
                composable("editor/{ipAddress}/{rootUri}") { navBackStackEntry ->
                    Editor(navBackStackEntry.arguments?.getString("ipAddress")!!, navBackStackEntry.arguments?.getString("rootUri")!!)
                }
            }
        }
    }
}
