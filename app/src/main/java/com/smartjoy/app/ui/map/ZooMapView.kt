package com.smartjoy.app.ui.map

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.smartjoy.app.model.Attraction
import com.smartjoy.app.model.PathNode
import com.smartjoy.app.model.Zone
import com.smartjoy.app.model.ZooLocation
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline

@Composable
fun ZooMapView(
    modifier: Modifier = Modifier,
    center: GeoPoint = GeoPoint(34.017306, 71.489722),
    zoom: Double = 18.0,
    recenterTrigger: Int = 0,
    userLocation: ZooLocation? = null,
    zones: List<Zone> = emptyList(),
    attractions: List<Attraction> = emptyList(),
    path: List<PathNode> = emptyList(),
    onAttractionClick: (Attraction) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    remember {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = context.packageName
        true
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(zoom)
            controller.setCenter(center)
        }
    }

    LaunchedEffect(recenterTrigger) {
        if (recenterTrigger > 0) {
            mapView.controller.animateTo(center)
            mapView.controller.setZoom(zoom)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { view ->
            view.overlays.clear()
            addParkBoundary(view, zooBoundaryPoints)
            zones.forEach { zone -> addZone(view, zone) }
            if (path.isNotEmpty()) {
                drawPath(view, path)
            }
            attractions.forEach { attraction ->
                val marker = Marker(view)
                marker.position = GeoPoint(attraction.location.latitude, attraction.location.longitude)
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                marker.title = attraction.name
                marker.snippet = attraction.description
                marker.setOnMarkerClickListener { m, _ ->
                    onAttractionClick(attraction)
                    m.showInfoWindow()
                    true
                }
                view.overlays.add(marker)
            }
            userLocation?.let {
                addMarker(view, GeoPoint(it.latitude, it.longitude), "My Location", isUser = true)
            }
            view.invalidate()
        }
    )
}

fun addMarker(mapView: MapView, geoPoint: GeoPoint, title: String, isUser: Boolean = false) {
    val marker = Marker(mapView)
    marker.position = geoPoint
    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
    marker.title = title
    if (isUser) {
        marker.icon = mapView.context.getDrawable(android.R.drawable.presence_online)
    }
    mapView.overlays.add(marker)
}

fun addZone(mapView: MapView, zone: Zone) {
    val circle = Polygon(mapView)
    val center = GeoPoint(zone.center.latitude, zone.center.longitude)
    circle.points = Polygon.pointsAsCircle(center, zone.radiusMeters)
    circle.fillPaint.color = zone.type.fillColor
    circle.outlinePaint.color = zone.type.borderColor
    circle.outlinePaint.strokeWidth = 2f
    mapView.overlays.add(circle)
}

fun addParkBoundary(mapView: MapView, boundaryPoints: List<GeoPoint>) {
    val boundary = Polygon(mapView)
    boundary.points = boundaryPoints
    boundary.outlinePaint.color = android.graphics.Color.BLACK
    boundary.outlinePaint.strokeWidth = 5f
    boundary.fillPaint.color = android.graphics.Color.argb(15, 0, 0, 0)
    mapView.overlays.add(boundary)
}

fun drawPath(mapView: MapView, path: List<PathNode>) {
    val geoPoints = path.map { GeoPoint(it.location.latitude, it.location.longitude) }
    
    // Outer shadow/border line for a polished look
    val outerLine = Polyline(mapView).apply {
        setPoints(geoPoints)
        outlinePaint.color = android.graphics.Color.argb(160, 29, 78, 216) // Deep blue shadow
        outlinePaint.strokeWidth = 16f
        outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
        outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
    }
    mapView.overlays.add(outerLine)

    // Inner vibrant navigation line
    val innerLine = Polyline(mapView).apply {
        setPoints(geoPoints)
        outlinePaint.color = android.graphics.Color.rgb(37, 99, 235) // Professional navigation blue
        outlinePaint.strokeWidth = 10f
        outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
        outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
    }
    mapView.overlays.add(innerLine)
}

val zooBoundaryPoints = listOf(
    GeoPoint(34.0189, 71.4879),
    GeoPoint(34.0189, 71.4897),
    GeoPoint(34.0189, 71.4915),
    GeoPoint(34.0173, 71.4915),
    GeoPoint(34.0157, 71.4915),
    GeoPoint(34.0157, 71.4897),
    GeoPoint(34.0157, 71.4879),
    GeoPoint(34.0173, 71.4879)
)
