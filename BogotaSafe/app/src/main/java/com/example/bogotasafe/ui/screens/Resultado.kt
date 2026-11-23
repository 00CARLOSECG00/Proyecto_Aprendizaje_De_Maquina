package com.example.bogotasafe.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bogotasafe.data.RiskResult
import com.example.bogotasafe.ui.components.GlassCard
import com.example.bogotasafe.ui.theme.AzulCielo
import com.example.bogotasafe.ui.theme.TextoPrincipal
import com.example.bogotasafe.ui.theme.TextoSecundario

private enum class Efecto(val etiqueta: String) { SUBE("Aumenta"), BAJA("Reduce"), NEUTRO("Neutral") }

private class Motivo(val titulo: String, val texto: String, val efecto: Efecto)

private fun efectoDe(factor: Double): Efecto = when {
    factor >= 1.08 -> Efecto.SUBE
    factor <= 0.92 -> Efecto.BAJA
    else -> Efecto.NEUTRO
}

private fun nivelCorto(score: Double): String = when {
    score >= 70.0 -> "Riesgo alto"
    score >= 45.0 -> "Riesgo moderado"
    else -> "Riesgo bajo"
}

/** Traduce los factores del modelo a frases entendibles. */
private fun motivos(res: RiskResult, perfilPropio: Boolean): List<Motivo> {
    val zona = when {
        res.scoreBaseCluster >= 60 -> Motivo("Zona", "Sector con alta incidencia delictiva histórica.", Efecto.SUBE)
        res.scoreBaseCluster >= 35 -> Motivo("Zona", "Sector con incidencia delictiva media.", Efecto.NEUTRO)
        else -> Motivo("Zona", "Sector con baja incidencia delictiva histórica.", Efecto.BAJA)
    }.let {
        Motivo(it.titulo, "${it.texto} ${res.hurtosHistoricos} hurtos, ${res.lesionesHistoricas} lesiones y ${res.homicidiosHistoricos} homicidios registrados (2023-2025).", it.efecto)
    }

    val efectoMomento = efectoDe(res.multTemporal)
    val momento = Motivo(
        "Momento",
        "${res.diaSemana} a las ${formatoHora(res.hora)}: " + when (efectoMomento) {
            Efecto.SUBE -> "hay más incidentes de lo habitual."
            Efecto.BAJA -> "hay menos incidentes de lo habitual."
            Efecto.NEUTRO -> "un momento promedio."
        },
        efectoMomento
    )

    val efectoPerfil = efectoDe(res.multDemografico)
    val sujeto = if (perfilPropio) "Tu perfil" else "Este perfil"
    val rango = res.cicloVital.substringBefore(")").removePrefix("(").replace(" a ", " a ")
    val perfil = Motivo(
        "Perfil",
        "$sujeto (${res.sexo.lowercase()}, $rango años) " + when (efectoPerfil) {
            Efecto.SUBE -> "aparece con más frecuencia entre las víctimas."
            Efecto.BAJA -> "aparece con menos frecuencia entre las víctimas."
            Efecto.NEUTRO -> "tiene una exposición promedio."
        },
        efectoPerfil
    )
    return listOf(zona, momento, perfil)
}

/**
 * Resultado con jerarquía: 1) respuesta y acción, 2) por qué, 3) detalle técnico plegado.
 */
@Composable
fun ResultadoRiesgo(res: RiskResult, perfilPropio: Boolean, mostrarSector: Boolean = true) {
    val colorRiesgo = Color(res.colorHex)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // 1. Respuesta + acción
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            tinte = colorRiesgo.copy(alpha = 0.22f),
            bordeColor = colorRiesgo,
            liquido = true
        ) {
            Text(nivelCorto(res.scoreFinal), color = colorRiesgo, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${res.scoreFinal}", color = colorRiesgo, fontWeight = FontWeight.ExtraBold, fontSize = 60.sp)
                Text(" / 100", color = TextoSecundario, fontSize = 16.sp, modifier = Modifier.padding(bottom = 12.dp))
            }
            if (mostrarSector) {
                Text(res.nombreBarrio.nombrePropio(), color = TextoPrincipal, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
            Spacer(Modifier.height(14.dp))
            Text("Qué hacer", color = TextoSecundario, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text(res.recomendacion, color = TextoPrincipal, fontSize = 15.sp, lineHeight = 21.sp)
        }

        // 2. Por qué
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            TituloSeccion("Por qué")
            Spacer(Modifier.height(10.dp))
            motivos(res, perfilPropio).forEachIndexed { i, m ->
                if (i > 0) {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    Spacer(Modifier.height(10.dp))
                }
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(m.titulo, color = TextoPrincipal, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(m.texto, color = TextoSecundario, fontSize = 13.sp, lineHeight = 18.sp)
                    }
                    EtiquetaEfecto(m.efecto)
                }
            }
        }

        // 3. Detalle técnico, plegado por defecto
        DetalleTecnico(res)
    }
}

@Composable
private fun EtiquetaEfecto(efecto: Efecto) {
    val fondo = when (efecto) {
        Efecto.SUBE -> Color.White.copy(alpha = 0.16f)
        Efecto.BAJA -> Color.White.copy(alpha = 0.06f)
        Efecto.NEUTRO -> Color.White.copy(alpha = 0.10f)
    }
    Text(
        efecto.etiqueta,
        color = TextoPrincipal,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(fondo)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
private fun DetalleTecnico(res: RiskResult) {
    var abierto by rememberSaveable { mutableStateOf(false) }
    GlassCard(modifier = Modifier.fillMaxWidth().animateContentSize(tween(200))) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { abierto = !abierto }
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TituloSeccion("Ver cómo se calculó")
            Text(if (abierto) "Ocultar" else "Mostrar", color = AzulCielo, fontSize = 13.sp)
        }
        if (abierto) {
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FilaTecnica(
                    "Ubicación",
                    "${"%.5f".format(res.latitud)}, ${"%.5f".format(res.longitud)}\nSector catastral ${res.codigoSector}, a ${res.distanciaMetros} m de su centroide"
                )
                FilaTecnica(
                    "Zona · K-Means (k = 7)",
                    "Clúster ${res.clusterId} de 6 · Puntaje base ${res.scoreBaseCluster}"
                )
                FilaTecnica(
                    "Momento · Regresión de Poisson",
                    "×${res.multTemporal} = Mes ${res.mes} (${res.factorMes}) × ${res.diaSemana} (${res.factorDia}) × ${res.hora} h (${res.factorHora})"
                )
                FilaTecnica(
                    "Perfil · Naive Bayes",
                    "×${res.multDemografico} = 0.55 × ${res.sexo} (${res.factorSexo}) + 0.45 × ${res.cicloVital} (${res.factorEdad})"
                )
                FilaTecnica(
                    "Fórmula",
                    "${res.scoreBaseCluster} × (0.65 × ${res.multTemporal} + 0.35 × ${res.multDemografico}) = ${res.scoreFinal}\nLimitado al rango 5 - 99"
                )
            }
        }
    }
}

@Composable
private fun FilaTecnica(titulo: String, valor: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(titulo, color = TextoPrincipal, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text(valor, color = TextoSecundario, fontSize = 12.sp, lineHeight = 17.sp)
    }
}
