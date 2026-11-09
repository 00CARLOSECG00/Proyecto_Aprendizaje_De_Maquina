package com.example.bogotasafe.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

@Composable
fun InteractiveMapPicker(
    selectedLat: Double?,
    selectedLon: Double?,
    initialCenterLat: Double?,
    initialCenterLon: Double?,
    onPointSelected: (Double, Double) -> Unit
) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(12.dp)),
        factory = { context ->
            Configuration.getInstance().userAgentValue = context.packageName
            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(13.5)

                // Centrar en la ubicación GPS del usuario si existe, o vista general de la sabana
                val startLat = initialCenterLat ?: 4.6486
                val startLon = initialCenterLon ?: -74.0840
                controller.setCenter(GeoPoint(startLat, startLon))

                val receiver = object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                        p?.let {
                            onPointSelected(it.latitude, it.longitude)
                        }
                        return true
                    }

                    override fun longPressHelper(p: GeoPoint?): Boolean = false
                }
                overlays.add(MapEventsOverlay(receiver))
            }
        },
        update = { mapView ->
            // Eliminar marcadores previos y dibujar el nuevo punto seleccionado
            mapView.overlays.removeAll { it is Marker }
            if (selectedLat != null && selectedLon != null) {
                val marker = Marker(mapView).apply {
                    position = GeoPoint(selectedLat, selectedLon)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "Punto a evaluar"
                }
                mapView.overlays.add(marker)
            }
            mapView.invalidate()
        }
    )
}