package com.example.lsp_client.server

import androidx.navigation.compose.navigate
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@Composable
fun StartupScreen(navController: NavController) {
    val prompt = "Address"
    var ipAddress by remember { mutableStateOf("10.0.2.2:8001") }
    var connecting by remember { mutableStateOf( false) }
    val connectionViewModel: ConnViewModel = viewModel()
    val coroutineScope = rememberCoroutineScope()
    var response by remember { mutableStateOf("") }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text("Enter proxy IP address:", color = Color.White, style = MaterialTheme.typography.h6)
            OutlinedTextField(value = ipAddress, onValueChange = { ipAddress = it }, label = { Text(prompt) })
            Row(modifier = Modifier.padding(24.dp)) {
                if (ipAddress.isNotEmpty()) {
                    Button(onClick = {
                        coroutineScope.launch {
                            connecting = true
                            response = connectionViewModel.getResult(ipAddress)
                            connecting = false
                        }
                    })
                    {
                        Text(text = "Connect")
                    }
                }
            }
        }
        Row {
            if (response.isNotEmpty() && ipAddress.isNotEmpty() && !connecting)  {
                Text(response)
                if (response == "OK ✅") navController.navigate("editor/$ipAddress")
            } else if (connecting) {
                CircularProgressIndicator(modifier = Modifier.scale(0.7F))
            }
        }
    }
}