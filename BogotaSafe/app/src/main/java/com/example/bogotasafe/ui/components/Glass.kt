package com.example.bogotasafe.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bogotasafe.ui.theme.AzulCielo
import com.example.bogotasafe.ui.theme.AzulNoche
import com.example.bogotasafe.ui.theme.SobreAcento
import com.example.bogotasafe.ui.theme.TextoPrincipal
import com.example.bogotasafe.ui.theme.TextoSecundario
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.LocalHazePerformanceMode
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.hazeGlass
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/*
 * Estilo "Liquid Glass" con la librería Haze (módulo haze-glass).
 * - Fondo (60 %): ilustración nocturna de Bogotá, que el vidrio refracta y desenfoca.
 * - Vidrio (30 %): tarjetas y controles con refracción real, brillo especular y borde de luz.
 * - Acento (10 %): azul cielo, usado como tinte del vidrio en acciones y selección.
 */

/** Estado compartido entre la fuente (el fondo) y todos los elementos de vidrio. */
val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

/** Fondo de la app: ilustración de Bogotá registrada como fuente del vidrio. */
@Composable
fun FondoLiquido(content: @Composable BoxScope.() -> Unit) {
    val hazeState = rememberHazeState()
    Box(modifier = Modifier.fillMaxSize().background(AzulNoche)) {
        FondoBogota(modifier = Modifier.fillMaxSize().hazeSource(hazeState))
        CompositionLocalProvider(
            LocalHazeState provides hazeState,
            // El fondo ya es difuso: a baja resolución el vidrio se ve igual y cuesta mucho menos GPU
            LocalHazePerformanceMode provides HazePerformanceMode.Fixed(0.15f)
        ) {
            content()
        }
    }
}

/** Estilo de vidrio oscuro: legible sobre el fondo y con un tinte opcional. */
private fun estiloVidrio(forma: RoundedCornerShape, tinte: Color): GlassStyle = GlassStyle.regular.then {
    shape(forma)
    tint(tinte)
    backgroundColor(AzulNoche.copy(alpha = 0.35f))
}

/**
 * Aplica vidrio líquido real (refracción + desenfoque) usando Haze.
 * Si no hay fuente disponible, cae en una versión translúcida simple.
 */
@Composable
fun Modifier.vidrioLiquido(
    forma: RoundedCornerShape = RoundedCornerShape(28.dp),
    tinte: Color = AzulNoche.copy(alpha = 0.30f),
    bordeColor: Color? = null
): Modifier {
    val estado = LocalHazeState.current ?: return this.vidrio(forma, bordeColor = bordeColor)
    val estilo = remember(forma, tinte) { estiloVidrio(forma, tinte) }
    val conBorde = this
        .hazeGlass(input = HazeInput.Sources(estado), style = estilo)
        .clip(forma)
    return if (bordeColor != null) conBorde.border(1.5.dp, Brush.verticalGradient(listOf(bordeColor, bordeColor.copy(alpha = 0.4f))), forma)
    else conBorde
}

/** Vidrio translúcido simple (sin refracción). Se usa en controles dentro de las tarjetas. */
fun Modifier.vidrio(
    shape: Shape = RoundedCornerShape(24.dp),
    tinte: Color = Color.White,
    intensidad: Float = 1f,
    bordeColor: Color? = null,
    bordeAncho: Dp = 1.dp
): Modifier = this
    .clip(shape)
    .background(
        Brush.verticalGradient(
            listOf(
                tinte.copy(alpha = 0.14f * intensidad),
                tinte.copy(alpha = 0.05f * intensidad)
            )
        )
    )
    .border(
        width = bordeAncho,
        brush = if (bordeColor != null) Brush.verticalGradient(
            listOf(bordeColor, bordeColor.copy(alpha = 0.45f))
        ) else Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.38f), Color.White.copy(alpha = 0.06f))
        ),
        shape = shape
    )

/** Tarjeta de vidrio líquido. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    bordeColor: Color? = null,
    tinte: Color = AzulNoche.copy(alpha = 0.30f),
    padding: Dp = 20.dp,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    liquido: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val forma = RoundedCornerShape(28.dp)
    val superficie = if (liquido) {
        modifier.vidrioLiquido(forma = forma, tinte = tinte, bordeColor = bordeColor)
    } else {
        // Vidrio simple: velo oscuro + brillo + borde de luz. Sin shaders, casi gratis para la GPU.
        modifier
            .clip(forma)
            .background(AzulNoche.copy(alpha = 0.62f))
            .vidrio(shape = forma, intensidad = 0.8f, bordeColor = bordeColor, bordeAncho = if (bordeColor != null) 1.5.dp else 1.dp)
    }
    Column(
        modifier = superficie
            .padding(padding),
        horizontalAlignment = horizontalAlignment,
        content = content
    )
}

/**
 * Botón de vidrio.
 * - destacado = true: vidrio tintado con el acento (acción principal o selección activa).
 * - liquido = true: vidrio líquido real; usar solo para botones que flotan sobre el fondo.
 */
@Composable
fun GlassButton(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    destacado: Boolean = false,
    habilitado: Boolean = true,
    liquido: Boolean = false
) {
    val forma = RoundedCornerShape(50)
    val fondo = when {
        destacado && habilitado -> modifier
            .clip(forma)
            .background(Brush.verticalGradient(listOf(AzulCielo.copy(alpha = 0.95f), AzulCielo.copy(alpha = 0.75f))))
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.1f))), forma)
        liquido -> modifier.vidrioLiquido(forma = forma)
        else -> modifier.vidrio(shape = forma)
    }
    Box(
        modifier = fondo
            .clickable(enabled = habilitado, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = texto,
            color = when {
                !habilitado -> TextoSecundario
                destacado -> SobreAcento
                else -> TextoPrincipal
            },
            fontWeight = if (destacado) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}

/** Colores para los campos de texto sobre vidrio. */
@Composable
fun glassTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.White.copy(alpha = 0.08f),
    unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
    focusedBorderColor = AzulCielo,
    unfocusedBorderColor = Color.White.copy(alpha = 0.18f),
    focusedLabelColor = AzulCielo,
    unfocusedLabelColor = TextoSecundario,
    cursorColor = AzulCielo,
    focusedTextColor = TextoPrincipal,
    unfocusedTextColor = TextoPrincipal
)

/** Forma estándar de los campos de texto de vidrio. */
val FormaCampoVidrio = RoundedCornerShape(16.dp)
