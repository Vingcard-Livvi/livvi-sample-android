package com.vingcard.livvi.sample

import android.Manifest
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.MaterialTheme.typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vingcard.livvi.sample.lk.LKScanResult
import com.vingcard.livvi.sample.lk.LKUnlockResult
import com.vingcard.livvi.sample.ui.theme.ComposableSampleTheme
import com.google.accompanist.permissions.*
import com.vingcard.livvi.sample.lk.LKScanner

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scanner = LKScanner(applicationContext)
        this.viewModel = MainViewModel(scanner)

        setContent {
            ComposableSampleTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                    Content(this.viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun Content(viewModel: MainViewModel)
{
    val bluetoothPermissions = mutableListOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION)

    bluetoothPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
    bluetoothPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)

    val multiplePermissionState = rememberMultiplePermissionsState(
        permissions = bluetoothPermissions
    )

    if (multiplePermissionState.allPermissionsGranted) {
        viewModel.onPermissionsGranted()
        RenderDevicesList(viewModel)
    } else {
        RequestPermissions(viewModel = viewModel, multiplePermissionState)
    }
}


@OptIn(ExperimentalPermissionsApi::class)
@Composable fun RequestPermissions(viewModel: MainViewModel, permissionState: MultiplePermissionsState)
{
    Column {
        val textToShow = if (permissionState.shouldShowRationale) {
            // If the user has denied the permission but the rationale can be shown,
            // then gently explain why the app requires this permission
            "The location permissions are important for this app. Please grant the permissions."
        } else {
            // If it's the first time the user lands on this feature, or the user
            // doesn't want to be asked again for this permission, explain that the
            // permission is required
            "Location permissions required for this feature to be available. " +
                    "Please grant the permissions"
        }

        Text(textToShow)

        Row {
            Button(onClick = { permissionState.launchMultiplePermissionRequest() }) {
                Text("Request permissions")
            }
        }
    }
}

@Composable
fun RenderDevicesList(viewModel: MainViewModel)
{
    val context = LocalContext.current
    val devices: List<LKScanResult> = remember { viewModel.renderedDevices }

    // Handle unlock result with toast
    LaunchedEffect(viewModel.unlockResult.value) {
        viewModel.unlockResult.value?.let { result ->
            val message = when (result) {
                LKUnlockResult.success -> "Door unlocked successfully!"
                LKUnlockResult.signalError -> "Signal error - please try again"
                LKUnlockResult.syncError -> "Sync error - please try again"
                LKUnlockResult.timeout -> "Operation timed out"
                LKUnlockResult.permissionError -> "Permission denied"
                LKUnlockResult.doorUnavailable -> "Door unavailable"
                LKUnlockResult.messageSignError -> "Message sign error"
                LKUnlockResult.serverCommError -> "Server communication error"
                LKUnlockResult.lockDataMissing -> "Lock data missing"
                LKUnlockResult.unknown -> "Unknown error occurred"
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearUnlockResult()
        }
    }

    Box {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(devices, itemContent = { item ->
                VisibleDeviceItem(device = item, viewModel = viewModel)
            })
        }

        // Loading overlay
        if (viewModel.isLoading.value) {
            Dialog(
                onDismissRequest = { }, // Prevent dismissing while loading
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.padding(16.dp),
                        elevation = 8.dp,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Unlocking door...")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VisibleDeviceItem(device: LKScanResult, viewModel: MainViewModel)
{
    Card(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .fillMaxWidth(),
        elevation = 2.dp,
        shape = RoundedCornerShape(corner = CornerSize(16.dp))

    ) {
        Row(horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically){
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(0.7f)
                    .align(Alignment.CenterVertically)) {
                Text(text = device.name ?: "Bluetooth Device", style = typography.h6)
                device.serial?.let {
                    Text(text = it, style = typography.caption)
                }
                device.macAddress?.let {
                    Text(text = it, style = typography.caption)
                }
            }

            TextButton(onClick = { viewModel.unlockTapped(device) }, colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent)) {
                Text(text = "Unlock")
            }
        }
    }
}
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ComposableSampleTheme {
        // Preview with mock data
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
            Text("Livvi Sample App", modifier = Modifier.padding(16.dp))
        }
    }
}