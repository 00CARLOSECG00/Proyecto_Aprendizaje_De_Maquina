package com.example.bogotasafe.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bogotasafe.ui.components.GlassButton
import com.example.bogotasafe.ui.theme.AzulCielo
import com.example.bogotasafe.ui.theme.TextoPrincipal
import com.example.bogotasafe.ui.theme.TextoSecundario

val DIAS_SEMANA = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
val MESES = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
)

/** 19 -> "7:00 p. m." */
fun formatoHora(hora: Int): String {
    val h12 = if (hora % 12 == 0) 12 else hora % 12
    val sufijo = if (hora < 12) "a. m." else "p. m."
    return "$h12:00 $sufijo"
}

/** "BARRIO LOS ANDES" -> "Barrio Los Andes" */
fun String.nombrePropio(): String =
    lowercase().split(" ").joinToString(" ") { p -> p.replaceFirstChar { it.titlecase() } }

@Composable
fun TituloSeccion(texto: String) {
    Text(texto, color = AzulCielo, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
}

@Composable
fun TituloPantalla(titulo: String, subtitulo: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(titulo, color = TextoPrincipal, fontWeight = FontWeight.Bold, fontSize = 26.sp)
        if (subtitulo != null) Text(subtitulo, color = TextoSecundario, fontSize = 14.sp)
    }
}

@Composable
fun colorSlider() = SliderDefaults.colors(
    thumbColor = AzulCielo,
    activeTrackColor = AzulCielo,
    inactiveTrackColor = Color.White.copy(alpha = 0.15f),
    activeTickColor = Color.Transparent,
    inactiveTickColor = Color.Transparent
)

/** Selector de edad: botones +/- para ajuste fino y slider para cambios grandes. */
@Composable
fun SelectorEdad(edad: Int, onCambio: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            GlassButton(texto = "−", onClick = { onCambio((edad - 1).coerceAtLeast(1)) })
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$edad", color = TextoPrincipal, fontWeight = FontWeight.Bold, fontSize = 40.sp)
                Text("  años", color = TextoSecundario, fontSize = 15.sp, modifier = Modifier)
            }
            GlassButton(texto = "+", onClick = { onCambio((edad + 1).coerceAtMost(100)) })
        }
        Slider(
            value = edad.toFloat(),
            onValueChange = { onCambio(it.toInt()) },
            valueRange = 1f..100f,
            colors = colorSlider()
        )
    }
}
