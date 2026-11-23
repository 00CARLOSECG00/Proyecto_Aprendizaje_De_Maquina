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
import org.osmdroid.tileprovider.tilesource.XYTileSource
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
    onPointSelected: (Double, Double) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth().height(280.dp)
) {
    AndroidView(
        modifier = modifier.clip(RoundedCornerShape(20.dp)),
        // Libera el mapa (hilos de descarga de teselas y caché) cuando sale de la pantalla
        onRelease = { mapView -> mapView.onDetach() },
        factory = { context ->
            // Cargar configuración de osmdroid
            Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", android.content.Context.MODE_PRIVATE))
            // OpenStreetMap bloquea los User-Agent genéricos (como los que empiezan por com.example).
            // Se debe colocar uno personalizado.
            Configuration.getInstance().userAgentValue = "BogotaSafe/1.0"
            
            MapView(context).apply {
                // Usar MapTiler con API Key extraída de local.properties a través de BuildConfig
                val mapTilerKey = com.example.bogotasafe.BuildConfig.MAPTILER_API_KEY
                
                val mapTilerSource = object : XYTileSource(
                    "MapTiler",
                    0, 20, 256, ".png",
                    arrayOf("https://api.maptiler.com/maps/streets-v2/256/")
                ) {
                    override fun getTileURLString(pMapTileIndex: Long): String {
                        return super.getTileURLString(pMapTileIndex) + "?key=$mapTilerKey"
                    }
                }
                
                setTileSource(mapTilerSource)
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