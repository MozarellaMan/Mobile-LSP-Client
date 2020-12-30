package com.example.lsp_client.server

import androidx.compose.material.Text
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.viewModel
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch

@Preview
@Composable
fun ScreenContent() {
    val prompt = "Address"
    var ipAddress by remember { mutableStateOf("10.0.2.2:8001") }
    var connecting by remember { mutableStateOf( false) }
    val connectionViewModel: ConnViewModel = viewModel()
    val coroutineScope = rememberCoroutineScope()
    var response by remember { mutableStateOf("") }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text("Enter proxy IP address:", color = Color.White)
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
                    if (connecting) {
                        CircularProgressIndicator()
                    }
                }
            }
            Spacer(Modifier.preferredSize(16.dp))
            if (response.isNotEmpty() && ipAddress.isNotEmpty() && !connecting)  {
                Text(response)
            }

        }

    }
}