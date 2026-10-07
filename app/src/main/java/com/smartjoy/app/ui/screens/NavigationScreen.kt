package com.smartjoy.app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartjoy.app.R
import com.smartjoy.app.logic.LocationUtils
import com.smartjoy.app.logic.Pathfinder
import com.smartjoy.app.model.Attraction
import com.smartjoy.app.model.PathNode
import com.smartjoy.app.model.sampleAttractions
import com.smartjoy.app.model.zooPathGraph
import com.smartjoy.app.ui.components.AttractionInfoCard
import com.smartjoy.app.ui.components.MapLegend
import com.smartjoy.app.ui.map.ZooMapView
import com.smartjoy.app.viewmodel.LocationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationScreen(
    onNavigateBack: () -> Unit,
    locationViewModel: LocationViewModel = viewModel()
) {
    val hasPermission by locationViewModel.hasLocationPermission.collectAsState()
    val currentLocation by locationViewModel.currentLocation.collectAsState()
    
    var selectedAttraction by remember { mutableStateOf<Attraction?>(null) }
    var recenterTrigger by remember { mutableIntStateOf(0) }
    
    var selectedDestinationNode by remember { mutableStateOf<PathNode?>(null) }
    var currentPath by remember { mutableStateOf<List<PathNode>>(emptyList()) }
    var pathDistance by remember { mutableStateOf(0.0) }

    val namedNodes = remember { zooPathGraph.nodes.filter { it.name != null } }
    var showRoutePicker by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> locationViewModel.onPermissionResult(granted) }

    LaunchedEffect(Unit) {
        if (hasPermission) {
            locationViewModel.fetchLastLocation()
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    LaunchedEffect(currentLocation, selectedDestinationNode) {
        val loc = currentLocation
        val dest = selectedDestinationNode
        if (loc != null && dest != null) {
            val startNode = LocationUtils.findNearestNode(zooPathGraph, loc)
            val path = Pathfinder.findShortestPath(zooPathGraph, startNode.id, dest.id)
            currentPath = path
            var dist = 0.0
            for (i in 0 until path.size - 1) {
                dist += LocationUtils.calculateDistance(path[i].location, path[i + 1].location)
            }
            pathDistance = dist.toDouble()
        } else {
            currentPath = emptyList()
            pathDistance = 0.0
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.map_nav_topbar), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back_desc))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(
                    onClick = { recenterTrigger++ },
                    modifier = Modifier.padding(bottom = 12.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Home, contentDescription = stringResource(id = R.string.recenter_zoo_desc))
                }
                
                FloatingActionButton(
                    onClick = {
                        if (hasPermission) locationViewModel.fetchLastLocation()
                        else permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = stringResource(id = R.string.update_location_desc))
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            ZooMapView(
                modifier = Modifier.fillMaxSize(),
                recenterTrigger = recenterTrigger,
                userLocation = currentLocation,
                attractions = sampleAttractions,
                path = currentPath,
                onAttractionClick = { selectedAttraction = it }
            )

            MapLegend(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            )

            // Destination Selector Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .clickable { showRoutePicker = !showRoutePicker }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedDestinationNode?.name ?: stringResource(id = R.string.search_destination_placeholder),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (selectedDestinationNode != null && pathDistance > 0) {
                            Text(
                                text = stringResource(id = R.string.walking_route_text, pathDistance.toInt()),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (selectedDestinationNode != null) {
                        IconButton(onClick = { selectedDestinationNode = null }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(id = R.string.clear_desc), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Route Picker Overlay
            if (showRoutePicker) {
                Surface(
                    modifier = Modifier.fillMaxSize().clickable { showRoutePicker = false },
                    color = Color.Black.copy(alpha = 0.3f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Card(
                            modifier = Modifier.fillMaxWidth(0.85f).heightIn(max = 400.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(stringResource(id = R.string.select_destination_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(12.dp))
                                LazyColumn {
                                    items(namedNodes) { node ->
                                        DestinationItem(node) {
                                            selectedDestinationNode = it
                                            showRoutePicker = false
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            selectedAttraction?.let { attraction ->
                AttractionInfoCard(
                    attraction = attraction,
                    onDismiss = { selectedAttraction = null },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
                )
            }
        }
    }
}

@Composable
fun DestinationItem(node: PathNode, onClick: (PathNode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(node) }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon = when {
            node.name?.contains("Food", true) == true -> Icons.Default.Restaurant
            node.name?.contains("Entrance", true) == true -> Icons.Default.MeetingRoom
            node.name?.contains("Restroom", true) == true -> Icons.Default.Wc
            else -> Icons.Default.Pets
        }
        
        Box(
            modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(node.name ?: "", style = MaterialTheme.typography.bodyMedium)
    }
}
