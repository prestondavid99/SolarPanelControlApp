package org.example.project

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import org.jetbrains.compose.ui.tooling.preview.Preview


@Composable
@Preview
fun App() {
    MaterialTheme {
        ColumnLayout()
    }
}

@Composable
fun Mqtt() {
    val scope = rememberCoroutineScope()
    val mqttManager = remember { MqttManager() }
    val payload by remember { mutableStateOf(mqttManager.payload) }
    var connectionStatus by remember { mutableStateOf("Disconnected") }
    val fileSystem = FileSystem.SYSTEM
    val path = fileSystem.canonicalize("/".toPath())
    println(path)

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                mqttManager.init()
                while (mqttManager.client == null) delay(100)
                println("Client connected!")
                connectionStatus = "Connected"
                mqttManager.subscribe("/home")
            } catch (e: Exception) {
                connectionStatus = "Connection Failed: ${e.message}"
                println(e.printStackTrace())
            }
        }
    }

    Column {
        Text("Status: $connectionStatus")
        Button(
            onClick = {
                scope.launch {
                    try {
                        mqttManager.publish("/home","Hello from KMP!")
                    } catch (e: Exception) {
                        connectionStatus = "Publish Failed: ${e.message}"
                    }
                }
            },
            enabled = connectionStatus == "Connected"
        ) {
            Text("Send Message")
        }
        Text("Payload: " )
        Text(payload)
    }
}

@Composable
fun ColumnLayout() {
    var isExtended by remember { (mutableStateOf(false)) }
    var isLifted by remember { (mutableStateOf(false)) }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TwoOptions(isExtended) { isExtended = !isExtended }
        TwoOptions(isLifted) { isLifted = !isLifted }
        Mqtt()
    }
}

@Composable
fun TwoOptions(boolean: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CustomRadioButton(selected = boolean) { onClick() }
        CustomRadioButton(selected = !boolean) { onClick() }
    }
}

@Composable
fun CustomRadioButton(
    selected: Boolean = false,
//    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(21.dp))
            .clickable(onClick = onClick)
            .size(300.dp)
            .padding(50.dp),
        color = if (selected)
            MaterialTheme.colors.primary
        else MaterialTheme.colors.surface,
        border = BorderStroke(1.dp, MaterialTheme.colors.primary)
    ) {

    }
}