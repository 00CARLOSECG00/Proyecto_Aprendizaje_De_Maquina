package com.example.bogotasafe.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bogotasafe.data.RiskInferenceEngine
import com.example.bogotasafe.data.UserProfile
import com.example.bogotasafe.ui.components.GlassButton
import com.example.bogotasafe.ui.components.GlassCard
import com.example.bogotasafe.ui.theme.AzulCielo
import com.example.bogotasafe.ui.theme.RiesgoAlto
import com.example.bogotasafe.ui.theme.TextoPrincipal
import com.example.bogotasafe.ui.theme.TextoSecundario

@Composable
fun PantallaMiUbicacion(
    engine: RiskInferenceEngine,
    perfil: UserProfile?,
    editandoPerfil: Boolean,
    tienePermisoGps: Boolean,
    obteniendoGps: Boolean,
    errorGps: String?,
    gpsLat: Double?,
    gpsLon: Double?,
    onGuardarPerfil: (String, Int) -> Unit,
    onCancelarEdicion: () -> Unit,
    onSolicitarPermisoGps: () -> Unit,
    onRefrescarGps: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when {
            !tienePermisoGps -> EstadoMensaje(
                titulo = "Activa tu ubicación",
                texto = "Para decirte qué tan seguro es el lugar donde estás, necesitamos acceder al GPS. Tu ubicación no sale del teléfono.",
                accion = "Conceder permiso",
                onAccion = onSolicitarPermisoGps
            )

            perfil == null || editandoPerfil -> FormularioPerfil(
                titulo = if (perfil == null) "Cuéntanos de ti" else "Editar perfil",
                subtitulo = "Tu sexo y edad ajustan el cálculo de riesgo. Se guardan solo en este teléfono.",
                sexoInicial = perfil?.sexo,
                edadInicial = perfil?.edad ?: 25,
                onConfirmar = onGuardarPerfil,
                onCancelar = if (perfil != null) onCancelarEdicion else null
            )

            obteniendoGps && gpsLat == null -> Box(
                modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator(color = AzulCielo)
                    Text("Buscando tu ubicación...", color = TextoSecundario, fontSize = 14.sp)
                }
            }

            errorGps != null && gpsLat == null -> EstadoMensaje(
                titulo = "No pudimos ubicarte",
                texto = errorGps,
                accion = "Intentar de nuevo",
                onAccion = onRefrescarGps,
                colorBorde = RiesgoAlto.copy(alpha = 0.7f)
            )

            gpsLat != null && gpsLon != null -> {
                val p = perfil!!
                // Se recalcula SOLO cuando cambian la ubicación, el perfil o el usuario pide actualizar
                var actualizaciones by remember { mutableIntStateOf(0) }
                val tiempo = remember(gpsLat, gpsLon, actualizaciones) { engine.obtenerTiempoActualDelSistema() }
                val resultado = remember(gpsLat, gpsLon, p, tiempo) {
                    engine.evaluarRiesgo(gpsLat, gpsLon, p.sexo, p.edad, tiempo.mes, tiempo.diaSemana, tiempo.hora)
                }

                // Hora con minutos reales para el encabezado (el modelo solo usa la hora entera)
                val horaExacta = remember(tiempo) {
                    val minutos = java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE)
                    formatoHora(tiempo.hora).replace(":00", ":%02d".format(minutos))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Estás en", color = TextoSecundario, fontSize = 13.sp)
                        Text(resultado.nombreBarrio.nombrePropio(), color = TextoPrincipal, fontWeight = FontWeight.Bold, fontSize = 26.sp)
                        Text("${tiempo.diaSemana}, $horaExacta", color = TextoSecundario, fontSize = 13.sp)
                    }
                    GlassButton(
                        texto = if (obteniendoGps) "Buscando..." else "Actualizar",
                        onClick = {
                            actualizaciones++
                            onRefrescarGps()
                        },
                        habilitado = !obteniendoGps
                    )
                }
                ResultadoRiesgo(resultado, perfilPropio = true, mostrarSector = false)
            }
        }
    }
}

@Composable
private fun EstadoMensaje(
    titulo: String,
    texto: String,
    accion: String,
    onAccion: () -> Unit,
    colorBorde: androidx.compose.ui.graphics.Color? = null
) {
    Spacer(Modifier.height(40.dp))
    GlassCard(modifier = Modifier.fillMaxWidth(), bordeColor = colorBorde) {
        Text(titulo, color = TextoPrincipal, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(8.dp))
        Text(texto, color = TextoSecundario, fontSize = 14.sp, lineHeight = 20.sp)
        Spacer(Modifier.height(20.dp))
        GlassButton(texto = accion, onClick = onAccion, destacado = true, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun FormularioPerfil(
    titulo: String,
    subtitulo: String,
    sexoInicial: String?,
    edadInicial: Int,
    onConfirmar: (String, Int) -> Unit,
    onCancelar: (() -> Unit)?
) {
    var sexo by remember { mutableStateOf(sexoInicial) }
    var edad by remember { mutableIntStateOf(edadInicial) }

    TituloPantalla(titulo, subtitulo)
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        TituloSeccion("Sexo")
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("Hombre", "Mujer").forEach { opcion ->
                GlassButton(
                    texto = opcion,
                    onClick = { sexo = opcion },
                    destacado = sexo == opcion,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        TituloSeccion("Edad")
        Spacer(Modifier.height(6.dp))
        SelectorEdad(edad) { edad = it }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        if (onCancelar != null) {
            GlassButton(texto = "Cancelar", onClick = onCancelar, modifier = Modifier.weight(1f))
        }
        GlassButton(
            texto = "Guardar",
            onClick = { sexo?.let { onConfirmar(it, edad) } },
            destacado = true,
            habilitado = sexo != null,
            modifier = Modifier.weight(1f)
        )
    }
}
