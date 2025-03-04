package org.example.project.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.model.Payload
import org.example.project.viewmodel.AppViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview


@Composable
@Preview
fun App() {
    MaterialTheme {
        CenterAlign()
    }
}

@Composable
fun Mqtt(viewModel: AppViewModel = remember { AppViewModel() }) {
    val payload by viewModel.payload.collectAsState()
    val isConnected by viewModel.connectionState.collectAsState()
    val raiseArray by viewModel.raiseArray.collectAsState()
    println("MQTT: raiseArray = $raiseArray")

    Column {
        TwoOptions(
            enabled = isConnected,
            selected = raiseArray,
            onOptionSelected = { newSelection ->
                try {
                    viewModel.publish(
                         Payload(
                            raise_array = newSelection.compareTo(false)
                        )
                    )
                } catch (e: Exception) {
                    println("Publish Failed: ${e.message}")
                }
            }
        )
        Text("Payload Received:", fontSize = 30.sp)
        Text("raise_array: ${payload.raise_array}", fontSize = 20.sp)
    }
}

@Composable
fun CenterAlign() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Mqtt()
    }
}

@Composable
fun TwoOptions(
    selected: Boolean,
    enabled: Boolean,
    onOptionSelected: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // When option "true" is selected, set state to true
        CustomRadioButton(enabled = enabled && !selected, selected = selected, icon = Icons.Filled.KeyboardArrowUp) { onOptionSelected(true) }
        // When option "false" is selected, set state to false
        CustomRadioButton(enabled = enabled && selected, selected = !selected, icon = Icons.Filled.KeyboardArrowDown) { onOptionSelected(false) }
    }
}


@Composable
fun CustomRadioButton(
    selected: Boolean = false,
//    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(21.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .size(300.dp)
            .padding(50.dp),
        color = if (selected)
            MaterialTheme.colors.primary
        else MaterialTheme.colors.surface,
        border = BorderStroke(1.dp, MaterialTheme.colors.primary)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = if (selected) "Selected option" else "Unselected option",
            modifier = Modifier.size(48.dp),
            tint = if (selected) MaterialTheme.colors.onPrimary
            else MaterialTheme.colors.primary
        )
    }
}