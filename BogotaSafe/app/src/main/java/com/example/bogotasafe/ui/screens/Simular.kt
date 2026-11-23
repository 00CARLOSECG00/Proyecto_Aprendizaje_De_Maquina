package com.example.bogotasafe.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bogotasafe.data.RiskInferenceEngine
import com.example.bogotasafe.data.SystemTimeData
import com.example.bogotasafe.data.UserProfile
import com.example.bogotasafe.ui.components.FormaCampoVidrio
import com.example.bogotasafe.ui.components.GlassButton
import com.example.bogotasafe.ui.components.GlassCard
import com.example.bogotasafe.ui.components.InteractiveMapPicker
import com.example.bogotasafe.ui.components.glassTextFieldColors
import com.example.bogotasafe.ui.theme.AzulCielo
import com.example.bogotasafe.ui.theme.TextoPrincipal
import com.example.bogotasafe.ui.theme.TextoSecundario

private const val FASE_LUGAR = 0
private const val FASE_PERFIL = 1
private const val FASE_MOMENTO = 2
private const val FASE_RESULTADO = 3
private val NOMBRES_FASE = listOf("Lugar", "Perfil", "Momento", "Resultado")

/**
 * Estado del asistente. Se crea en la raíz de la app para que el progreso
 * se conserve al cambiar de pestaña.
 */
@Stable
class SimulacionState(tiempo: SystemTimeData) {
    var fase by mutableIntStateOf(FASE_LUGAR)
    var lat by mutableStateOf<Double?>(null)
    var lon by mutableStateOf<Double?>(null)
    var sexo by mutableStateOf<String?>(null)
    var edad by mutableIntStateOf(25)
    var mes by mutableIntStateOf(tiempo.mes)
    var dia by mutableStateOf(tiempo.diaSemana)
    var hora by mutableIntStateOf(tiempo.hora)
    /** true cuando el usuario vino desde el resultado a editar una sola fase. */
    var editandoDesdeResultado by mutableStateOf(false)

    fun faseValida(f: Int): Boolean = when (f) {
        FASE_LUGAR -> lat != null && lon != null
        FASE_PERFIL -> sexo != null
        else -> true
    }

    fun continuar() {
        fase = if (editandoDesdeResultado) FASE_RESULTADO else fase + 1
        if (fase == FASE_RESULTADO) editandoDesdeResultado = false
    }

    fun editar(f: Int) {
        editandoDesdeResultado = true
        fase = f
    }

    fun reiniciar(t: SystemTimeData) {
        fase = FASE_LUGAR; lat = null; lon = null; sexo = null; edad = 25
        mes = t.mes; dia = t.diaSemana; hora = t.hora
        editandoDesdeResultado = false
    }
}

@Composable
fun PantallaSimular(
    engine: RiskInferenceEngine,
    estado: SimulacionState,
    perfilGuardado: UserProfile?,
    gpsLat: Double?,
    gpsLon: Double?
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        IndicadorProgreso(estado.fase)
        Spacer(Modifier.height(12.dp))

        Box(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = estado.fase,
                transitionSpec = {
                    val haciaAdelante = targetState > initialState
                    (slideInHorizontally(tween(220)) { if (haciaAdelante) it / 6 else -it / 6 } + fadeIn(tween(220)))
                        .togetherWith(fadeOut(tween(120)))
                },
                label = "fases"
            ) { fase ->
                when (fase) {
                    FASE_LUGAR -> FaseLugar(engine, estado, gpsLat, gpsLon)
                    FASE_PERFIL -> FasePerfil(estado, perfilGuardado)
                    FASE_MOMENTO -> FaseMomento(engine, estado)
                    else -> FaseResultado(engine, estado)
                }
            }
        }

        BarraAcciones(engine, estado)
    }
}

@Composable
private fun IndicadorProgreso(fase: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            repeat(4) { i ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (i <= fase) AzulCielo else Color.White.copy(alpha = 0.15f))
                )
            }
        }
        Text("Paso ${fase + 1} de 4 · ${NOMBRES_FASE[fase]}", color = TextoSecundario, fontSize = 12.sp)
    }
}

@Composable
private fun BarraAcciones(engine: RiskInferenceEngine, estado: SimulacionState) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
    ) {
        if (estado.fase == FASE_RESULTADO) {
            GlassButton(
                texto = "Nueva simulación",
                onClick = { estado.reiniciar(engine.obtenerTiempoActualDelSistema()) },
                destacado = true,
                modifier = Modifier.weight(1f)
            )
        } else {
            if (estado.fase > FASE_LUGAR && !estado.editandoDesdeResultado) {
                GlassButton(texto = "Atrás", onClick = { estado.fase-- }, modifier = Modifier.weight(1f))
            }
            if (estado.editandoDesdeResultado) {
                GlassButton(
                    texto = "Cancelar",
                    onClick = { estado.editandoDesdeResultado = false; estado.fase = FASE_RESULTADO },
                    modifier = Modifier.weight(1f)
                )
            }
            GlassButton(
                texto = if (estado.editandoDesdeResultado || estado.fase == FASE_MOMENTO) "Ver resultado" else "Continuar",
                onClick = { estado.continuar() },
                destacado = true,
                habilitado = estado.faseValida(estado.fase),
                modifier = Modifier.weight(if (estado.fase == FASE_LUGAR && !estado.editandoDesdeResultado) 1f else 1.4f)
            )
        }
    }
}

// ---------------------------------------------------------------- Fase 1: Lugar

@Composable
private fun FaseLugar(engine: RiskInferenceEngine, estado: SimulacionState, gpsLat: Double?, gpsLon: Double?) {
    var mostrarCoordenadas by remember { mutableStateOf(false) }
    var latTexto by remember { mutableStateOf(estado.lat?.toString() ?: "") }
    var lonTexto by remember { mutableStateOf(estado.lon?.toString() ?: "") }
    val lat = estado.lat
    val lon = estado.lon
    val nombreSector = remember(lat, lon) {
        if (lat != null && lon != null) engine.nombreSectorCercano(lat, lon).nombrePropio() else null
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TituloPantalla("¿A dónde vas?", "Toca el mapa para marcar el lugar.")

        GlassCard(modifier = Modifier.fillMaxWidth().weight(1f), padding = 6.dp) {
            InteractiveMapPicker(
                selectedLat = lat,
                selectedLon = lon,
                initialCenterLat = lat ?: gpsLat,
                initialCenterLon = lon ?: gpsLon,
                onPointSelected = { la, lo ->
                    estado.lat = la; estado.lon = lo
                    latTexto = "%.5f".format(la); lonTexto = "%.5f".format(lo)
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    nombreSector ?: "Aún no has marcado un lugar",
                    color = if (nombreSector != null) TextoPrincipal else TextoSecundario,
                    fontWeight = if (nombreSector != null) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 16.sp
                )
                if (nombreSector != null) Text("Sector seleccionado", color = TextoSecundario, fontSize = 12.sp)
            }
            GlassButton(
                texto = if (mostrarCoordenadas) "Ocultar" else "Coordenadas",
                onClick = { mostrarCoordenadas = !mostrarCoordenadas }
            )
        }
        if (mostrarCoordenadas) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = latTexto,
                    onValueChange = { t -> latTexto = t; t.toDoubleOrNull()?.let { estado.lat = it } },
                    label = { Text("Latitud") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true,
                    shape = FormaCampoVidrio,
                    colors = glassTextFieldColors(),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = lonTexto,
                    onValueChange = { t -> lonTexto = t; t.toDoubleOrNull()?.let { estado.lon = it } },
                    label = { Text("Longitud") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true,
                    shape = FormaCampoVidrio,
                    colors = glassTextFieldColors(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Fase 2: Perfil

@Composable
private fun FasePerfil(estado: SimulacionState, perfilGuardado: UserProfile?) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TituloPantalla("¿Quién va?", "El sexo y la edad ajustan el riesgo.")

        if (perfilGuardado != null) {
            val usandoPropio = estado.sexo == perfilGuardado.sexo && estado.edad == perfilGuardado.edad
            GlassButton(
                texto = "Usar mi perfil (${perfilGuardado.sexo}, ${perfilGuardado.edad} años)",
                onClick = { estado.sexo = perfilGuardado.sexo; estado.edad = perfilGuardado.edad },
                destacado = usandoPropio,
                modifier = Modifier.fillMaxWidth()
            )
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            TituloSeccion("Sexo")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("Hombre", "Mujer").forEach { opcion ->
                    GlassButton(
                        texto = opcion,
                        onClick = { estado.sexo = opcion },
                        destacado = estado.sexo == opcion,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            TituloSeccion("Edad")
            Spacer(Modifier.height(6.dp))
            SelectorEdad(estado.edad) { estado.edad = it }
        }
    }
}

// ---------------------------------------------------------------- Fase 3: Momento

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FaseMomento(engine: RiskInferenceEngine, estado: SimulacionState) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TituloPantalla("¿Cuándo?", "El día y la hora cambian mucho el riesgo.")

        // Atajos rápidos
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            GlassButton("Ahora", onClick = {
                val t = engine.obtenerTiempoActualDelSistema()
                estado.mes = t.mes; estado.dia = t.diaSemana; estado.hora = t.hora
            })
            GlassButton("Esta noche", onClick = {
                val t = engine.obtenerTiempoActualDelSistema()
                estado.mes = t.mes; estado.dia = t.diaSemana; estado.hora = 22
            })
            GlassButton("Sábado en la noche", onClick = { estado.dia = "Sábado"; estado.hora = 22 })
            GlassButton("Lunes en la mañana", onClick = { estado.dia = "Lunes"; estado.hora = 7 })
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            TituloSeccion("Día")
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DIAS_SEMANA.forEach { d ->
                    GlassButton(texto = d.take(3), onClick = { estado.dia = d }, destacado = estado.dia == d)
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TituloSeccion("Hora")
                Spacer(Modifier.weight(1f))
                Text(formatoHora(estado.hora), color = TextoPrincipal, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            }
            Slider(
                value = estado.hora.toFloat(),
                onValueChange = { estado.hora = it.toInt() },
                valueRange = 0f..23f,
                steps = 22,
                colors = colorSlider()
            )

            Spacer(Modifier.height(12.dp))
            TituloSeccion("Mes")
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MESES.forEachIndexed { i, m ->
                    GlassButton(texto = m.take(3), onClick = { estado.mes = i + 1 }, destacado = estado.mes == i + 1)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Fase 4: Resultado

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FaseResultado(engine: RiskInferenceEngine, estado: SimulacionState) {
    val lat = estado.lat ?: return
    val lon = estado.lon ?: return
    val sexo = estado.sexo ?: return
    // Se calcula una sola vez por combinación de entradas
    val res = remember(lat, lon, sexo, estado.edad, estado.mes, estado.dia, estado.hora) {
        engine.evaluarRiesgo(lat, lon, sexo, estado.edad, estado.mes, estado.dia, estado.hora)
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Toca un dato para cambiarlo", color = TextoSecundario, fontSize = 12.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassButton(res.nombreBarrio.nombrePropio(), onClick = { estado.editar(FASE_LUGAR) })
            GlassButton("$sexo, ${estado.edad}", onClick = { estado.editar(FASE_PERFIL) })
            GlassButton("${estado.dia.take(3)} ${formatoHora(estado.hora)} · ${MESES[estado.mes - 1].take(3)}", onClick = { estado.editar(FASE_MOMENTO) })
        }
        ResultadoRiesgo(res, perfilPropio = false)
        Spacer(Modifier.height(4.dp))
    }
}
