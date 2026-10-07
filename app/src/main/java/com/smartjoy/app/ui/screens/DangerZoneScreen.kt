package com.smartjoy.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartjoy.app.R
import com.smartjoy.app.model.Zone
import com.smartjoy.app.model.ZoneProximityStatus
import com.smartjoy.app.model.ZoneStatus
import com.smartjoy.app.model.ZoneType
import com.smartjoy.app.model.zooZones
import com.smartjoy.app.ui.components.MapLegend
import com.smartjoy.app.ui.map.ZooMapView
import com.smartjoy.app.viewmodel.DangerZoneViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DangerZoneScreen(
    onNavigateBack: () -> Unit,
    viewModel: DangerZoneViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var recenterTrigger by remember { mutableIntStateOf(0) }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) viewModel.startMonitoring()
    }

    LaunchedEffect(Unit) {
        if (hasPermission) viewModel.startMonitoring()
        else permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.stopMonitoring() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.safety_zones_topbar), style = MaterialTheme.typography.titleLarge) },
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
        ) {
            StatusBanner(status = uiState.currentZoneStatus, activeZone = uiState.activeZone)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.2f)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(20.dp))
            ) {
                ZooMapView(
                    modifier = Modifier.fillMaxSize(),
                    recenterTrigger = recenterTrigger,
                    userLocation = uiState.currentLocation,
                    zones = zooZones,
                    zoom = 18.0
                )
                
                IconButton(
                    onClick = { recenterTrigger++ },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), CircleShape),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Home, contentDescription = stringResource(id = R.string.recenter_desc), modifier = Modifier.size(20.dp))
                }

                MapLegend(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.nearby_safety_zones_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        IconButton(
                            onClick = {
                                if (uiState.isMonitoring) viewModel.stopMonitoring()
                                else viewModel.startMonitoring()
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (uiState.isMonitoring) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primary,
                                contentColor = if (uiState.isMonitoring) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                if (uiState.isMonitoring) Icons.Default.LocationDisabled else Icons.Default.Radar,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(uiState.zoneStatuses, key = { it.zone.id }) { status ->
                            ZoneStatusCard(status = status)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBanner(status: ZoneStatus, activeZone: Zone?) {
    val (color, icon, textResId) = when (status) {
        ZoneStatus.DANGER -> Triple(MaterialTheme.colorScheme.error, Icons.Default.Warning, R.string.status_danger)
        ZoneStatus.CAUTION -> Triple(Color(0xFFF59E0B), Icons.Default.Info, R.string.status_caution)
        ZoneStatus.SAFE -> Triple(Color(0xFF16A34A), Icons.Default.CheckCircle, R.string.status_safe)
        ZoneStatus.UNDEFINED -> Triple(MaterialTheme.colorScheme.outline, Icons.Default.Radar, R.string.status_scanning)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = stringResource(id = textResId),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
                Text(
                    text = if (activeZone != null) stringResource(id = R.string.located_in_zone, activeZone.name) else stringResource(id = R.string.monitoring_child_position),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ZoneStatusCard(status: ZoneProximityStatus) {
    val color = when (status.zone.type) {
        ZoneType.SAFE -> Color(0xFF16A34A)
        ZoneType.CAUTION -> Color(0xFFF59E0B)
        ZoneType.DANGER -> Color(0xFFDC2626)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = status.zone.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(text = stringResource(id = status.zone.type.labelResId), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text = if (status.isInside) stringResource(id = R.string.zone_inside) else stringResource(id = R.string.distance_meters_text, status.distanceMeters.toInt()),
            style = MaterialTheme.typography.labelLarge,
            color = if (status.isInside) color else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}
