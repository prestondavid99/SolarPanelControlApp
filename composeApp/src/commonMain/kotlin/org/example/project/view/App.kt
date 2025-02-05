package org.example.project.view

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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import org.example.project.MqttManager
import org.example.project.model.Status
import org.example.project.viewmodel.AppViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview


@Composable
@Preview
fun App() {
    MaterialTheme {
        ColumnLayout()
    }
}

@Composable
fun Mqtt(viewModel: AppViewModel = AppViewModel()) {

    val scope = rememberCoroutineScope()
    val mqttManager = remember { MqttManager() }
    val status by viewModel.status.collectAsState()
    val client by viewModel.client.collectAsState()
    var connectionStatus by remember { mutableStateOf("Disconnected") }
    val fileSystem = FileSystem.SYSTEM
    val path = fileSystem.canonicalize("/".toPath())
    println(path)

    LaunchedEffect(Unit) {
        viewModel.subscribe("/home")
        viewModel.status.collect { newStatus ->
            // Update connection status based on newStatus if needed

        }
    }

    Column {
        Button(
            onClick = {
                try {
                    viewModel.publish("/home",Status(
                        isExtended = true,
                        isLifted = true,
                        windSpeed = 999
                    ))
                } catch (e: Exception) {
                    connectionStatus = "Publish Failed: ${e.message}"
                }
            },
            enabled = client != null
        ) {
            Text("Send Message")
        }
        Text("Status:", fontSize = 20.sp)
        Text("Is Extended: ${status.isExtended}", fontSize = 18.sp)
        Text("Is Lifted: ${status.isLifted}", fontSize = 18.sp)
        Text("Wind Speed: ${status.windSpeed}", fontSize = 18.sp)
    }
}

@Composable
fun ColumnLayout() {
    var isExtended by remember { mutableStateOf(false) }
    var isLifted by remember { mutableStateOf(false) }
    var windSpeed by remember { mutableStateOf(0) }
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