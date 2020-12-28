package com.example.lsp_client

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ContextAmbient
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.viewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.ui.tooling.preview.Preview
import com.example.lsp_client.server.testConnect
import com.example.lsp_client.ui.Lsp_clientTheme
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException

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

data class ServerResult(
    var loading: Boolean,
    var responseCode: String
)

class ConnViewModel : ViewModel() {
    private val addr: MutableLiveData<String> = MutableLiveData("")
    suspend fun getResult(ip: String): String {
        return connect(ip)
    }

    private suspend fun connect(ip: String): String {
        return try {
            testConnect("http://$ip/health")
        } catch (e: IOException) {
            e.printStackTrace()
            e.toString()
            return "Connection failed"
        }
    }
}


@Composable
fun MyApp(content: @Composable () -> Unit) {
    Lsp_clientTheme {
        Surface(color = Color.Black) {
            content()
        }
    }
}
@Composable
fun Loading(resources: Resources) {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            resources.getString(R.string.msg_loading),
            style = MaterialTheme.typography.h6,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Preview
@Composable
fun ScreenContent() {
    val prompt = "Address"
    var ipAddress by remember { mutableStateOf("") }
    var connecting by remember { mutableStateOf( false) }
    val viewModel: ConnViewModel = viewModel()
    val coroutineScope = rememberCoroutineScope()
    var response by remember { mutableStateOf("")}


    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text("Enter proxy IP address:", color = Color.White)
            OutlinedTextField(value = ipAddress, onValueChange = { ipAddress = it }, label = { Text(prompt) })
            Row(modifier = Modifier.padding(24.dp)) {
                if (ipAddress.isNotEmpty()) {
                    Button(onClick = {
                        coroutineScope.launch {
                            connecting = true;
                            response = viewModel.getResult(ipAddress)
                            connecting = false;
                        }
                    })
                    {
                        Text(text = "Connect")
                    }
                    if (connecting) {
                        CircularProgressIndicator()
                    }
                }
                if (response.isNotEmpty() && ipAddress.isNotEmpty()) {
                    Text(text = "...$response", modifier = Modifier.padding(8.dp))
                    response = ""
                }
            }
        }

    }
}


 fun establishConnection(context: Context, addr: String) {
    Toast.makeText(context, "connecting to.. $addr", Toast.LENGTH_SHORT).show()
}

@Composable
fun DefaultPreview() {
    MyApp {
        ScreenContent()
    }
}