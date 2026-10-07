package com.smartjoy.app.ui.screens

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartjoy.app.R
import com.smartjoy.app.model.BleDevice
import com.smartjoy.app.model.ProximityLevel
import com.smartjoy.app.model.TrackingUiState
import com.smartjoy.app.viewmodel.ChildTrackingViewModel

private fun requiredPermissions(): Array<String> {
    val permissions = mutableListOf<String>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        permissions += Manifest.permission.BLUETOOTH_SCAN
        permissions += Manifest.permission.BLUETOOTH_CONNECT
    } else {
        permissions += Manifest.permission.ACCESS_FINE_LOCATION
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions += Manifest.permission.POST_NOTIFICATIONS
    }
    return permissions.toTypedArray()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildTrackingScreen(
    onNavigateBack: () -> Unit,
    viewModel: ChildTrackingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var hasPermissions by remember {
        mutableStateOf(
            requiredPermissions().all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    var permissionLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>? = null

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.startScan()
        }
    }

    val requestScanOrEnableBt: () -> Unit = {
        if (!hasPermissions) {
            permissionLauncher?.launch(requiredPermissions())
        } else {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = bluetoothManager?.adapter
            if (adapter != null && !adapter.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                enableBluetoothLauncher.launch(enableBtIntent)
            } else {
                viewModel.startScan()
            }
        }
    }

    val actualPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermissions = results.values.all { it }
        if (hasPermissions) {
            requestScanOrEnableBt()
        }
    }
    permissionLauncher = actualPermissionLauncher

    LaunchedEffect(Unit) {
        requestScanOrEnableBt()
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.stopScan() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.device_pairing_topbar), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back_desc))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp)
        ) {
            val paired = uiState.pairedDevice
            if (paired == null) {
                PairingSection(
                    uiState = uiState,
                    onScanRequest = {
                        if (uiState.isScanning) {
                            viewModel.stopScan()
                        } else {
                            requestScanOrEnableBt()
                        }
                    },
                    onPair = { viewModel.pairDevice(it) }
                )
            } else {
                MonitoringSection(
                    device = paired,
                    proximity = uiState.proximity,
                    isScanning = uiState.isScanning,
                    onToggleMonitoring = {
                        if (uiState.isScanning) {
                            viewModel.stopScan()
                        } else {
                            requestScanOrEnableBt()
                        }
                    },
                    onUnpair = { viewModel.unpairDevice() }
                )
            }

            uiState.errorMessage?.let {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.padding(top = 16.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PairingSection(
    uiState: TrackingUiState,
    onScanRequest: () -> Unit,
    onPair: (BleDevice) -> Unit
) {
    Spacer(modifier = Modifier.height(16.dp))
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = if (uiState.isScanning) 1.2f else 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(if (uiState.isScanning) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.BluetoothSearching,
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .graphicsLayer(scaleX = scale, scaleY = scale),
                    tint = if (uiState.isScanning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            
            Text(text = stringResource(id = R.string.nearby_safety_devices_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = if (uiState.isScanning) stringResource(id = R.string.searching_wearables) else stringResource(id = R.string.start_scan_wearable),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onScanRequest,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isScanning) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primary,
                    contentColor = if (uiState.isScanning) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(if (uiState.isScanning) stringResource(id = R.string.stop_scanning_btn) else stringResource(id = R.string.scan_devices_btn))
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(text = stringResource(id = R.string.discovered_devices_title), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(modifier = Modifier.height(12.dp))

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(uiState.nearbyDevices, key = { it.address }) { device ->
            DeviceRow(device, onPair)
        }
    }
}

@Composable
fun DeviceRow(device: BleDevice, onPair: (BleDevice) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Watch, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = device.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(text = device.address, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = { onPair(device) }) {
                Text(stringResource(id = R.string.pair_btn))
            }
        }
    }
}

@Composable
private fun MonitoringSection(
    device: BleDevice,
    proximity: ProximityLevel,
    isScanning: Boolean,
    onToggleMonitoring: () -> Unit,
    onUnpair: () -> Unit
) {
    Spacer(modifier = Modifier.height(16.dp))
    
    val statusColor = if (isScanning) {
        when (proximity) {
            ProximityLevel.NEAR -> MaterialTheme.colorScheme.primary
            ProximityLevel.SAFE -> Color(0xFF16A34A)
            ProximityLevel.FAR -> Color(0xFFF59E0B)
            ProximityLevel.OUT_OF_RANGE -> Color(0xFFDC2626)
        }
    } else MaterialTheme.colorScheme.outline

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(statusColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isScanning) Icons.Default.ChildCare else Icons.Default.BluetoothDisabled,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = statusColor
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(text = device.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = stringResource(id = R.string.paired_linked_text), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(32.dp))

            val proximityText = if (isScanning) stringResource(id = proximity.labelResId) 
                               else stringResource(id = R.string.monitoring_paused_text)

            Text(
                text = proximityText,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = statusColor
            )
            
            if (isScanning) {
                val rssiStr = if (device.rssi == ChildTrackingViewModel.UNKNOWN_RSSI) {
                    stringResource(id = R.string.rssi_calculating)
                } else {
                    stringResource(id = R.string.rssi_dbm_format, device.rssi)
                }
                Text(
                    text = stringResource(id = R.string.signal_strength_text, rssiStr),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(32.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Button(
            onClick = onToggleMonitoring,
            modifier = Modifier.weight(1f).height(52.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(if (isScanning) stringResource(id = R.string.pause_btn) else stringResource(id = R.string.resume_btn))
        }
        OutlinedButton(
            onClick = onUnpair,
            modifier = Modifier.weight(1f).height(52.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(stringResource(id = R.string.unpair_btn))
        }
    }
}

private fun formatRssi(rssi: Int): String =
    if (rssi == ChildTrackingViewModel.UNKNOWN_RSSI) "calculating..." else "$rssi dBm"
