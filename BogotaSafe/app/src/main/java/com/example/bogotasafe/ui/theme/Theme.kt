package com.example.bogotasafe.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Esquema fijo (sin colores dinámicos del sistema) para que la regla 60/30/10
// se mantenga igual en cualquier dispositivo.
private val BogotaSafeColorScheme = darkColorScheme(
    primary = AzulCielo,
    onPrimary = SobreAcento,
    secondary = AzulCielo,
    onSecondary = SobreAcento,
    background = AzulNoche,
    onBackground = TextoPrincipal,
    surface = Pizarra,
    onSurface = TextoPrincipal,
    surfaceVariant = Pizarra,
    onSurfaceVariant = TextoSecundario,
    surfaceContainer = Pizarra,
    surfaceContainerHigh = PizarraBorde,
    outline = PizarraBorde,
    outlineVariant = PizarraBorde,
    error = RiesgoAlto,
    onError = TextoPrincipal
)

@Composable
fun BogotaSafeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BogotaSafeColorScheme,
        typography = Typography,
        content = content
    )
}
