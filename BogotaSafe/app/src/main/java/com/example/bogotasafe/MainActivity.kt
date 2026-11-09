package com.example.bogotasafe



import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.bogotasafe.data.RiskInferenceEngine
import com.example.bogotasafe.data.RiskResult
import com.example.bogotasafe.data.UserPreferencesRepository
import com.example.bogotasafe.data.UserProfile
import com.example.bogotasafe.ui.components.InteractiveMapPicker
import com.example.bogotasafe.ui.theme.BogotaSafeTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

class MainActivity : ComponentActivity() {

    private lateinit var inferenceEngine: RiskInferenceEngine
    private lateinit var prefsRepository: UserPreferencesRepository
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var tienePermisoGps by mutableStateOf(false)
    private var obteniendoGps by mutableStateOf(false)
    private var errorGps by mutableStateOf<String?>(null)
    private var gpsLatReal by mutableStateOf<Double?>(null)
    private var gpsLonReal by mutableStateOf<Double?>(null)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val concedido = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        tienePermisoGps = concedido
        if (concedido) {
            capturarUbicacionGpsReal()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inferenceEngine = RiskInferenceEngine(this)
        prefsRepository = UserPreferencesRepository(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        verificarPermisosExistentes()

        setContent {
            BogotaSafeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigationRoot(
                        engine = inferenceEngine,
                        prefsRepo = prefsRepository,
                        tienePermisoGps = tienePermisoGps,
                        obteniendoGps = obteniendoGps,
                        errorGps = errorGps,
                        gpsLatReal = gpsLatReal,
                        gpsLonReal = gpsLonReal,
                        onSolicitarPermisoGps = {
                            requestPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        onRefrescarGps = { capturarUbicacionGpsReal() }
                    )
                }
            }
        }
    }

    private fun verificarPermisosExistentes() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        tienePermisoGps = (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED)
        if (tienePermisoGps) {
            capturarUbicacionGpsReal()
        }
    }

    @SuppressLint("MissingPermission")
    private fun capturarUbicacionGpsReal() {
        if (!tienePermisoGps) return
        obteniendoGps = true
        errorGps = null

        val tokenSource = CancellationTokenSource()
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.token)
            .addOnSuccessListener { location ->
                obteniendoGps = false
                if (location != null) {
                    gpsLatReal = location.latitude
                    gpsLonReal = location.longitude
                } else {
                    errorGps = "El sensor GPS no devolvió coordenadas. Verifica que la Ubicación del teléfono esté encendida."
                }
            }
            .addOnFailureListener { e ->
                obteniendoGps = false
                errorGps = "Error leyendo hardware GPS: ${e.localizedMessage}"
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigationRoot(
    engine: RiskInferenceEngine,
    prefsRepo: UserPreferencesRepository,
    tienePermisoGps: Boolean,
    obteniendoGps: Boolean,
    errorGps: String?,
    gpsLatReal: Double?,
    gpsLonReal: Double?,
    onSolicitarPermisoGps: () -> Unit,
    onRefrescarGps: () -> Unit
) {
    // 0. Leer si existe un perfil guardado (nulo si no existe)
    var perfilGuardado by remember { mutableStateOf(prefsRepo.obtenerPerfilGuardado()) }
    var mostrarEditorPerfil by remember { mutableStateOf(false) }

    // Pestaña activa: 0 = Mi Estado Actual (GPS + Perfil), 1 = Simular Estado Posible (Mapa + Inputs)
    var tabSeleccionada by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BogotáSafe - Inferencia IA", color = Color.White, fontSize = 18.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A1A1A)),
                actions = {
                    if (perfilGuardado != null && tabSeleccionada == 0) {
                        TextButton(onClick = { mostrarEditorPerfil = true }) {
                            Text("Mi Perfil (${perfilGuardado!!.sexo}, ${perfilGuardado!!.edad}a)", color = Color(0xFF80CBC4))
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Selector Superior de Modo
            SecondaryTabRow(
                selectedTabIndex = tabSeleccionada,
                containerColor = Color(0xFF262626),
                contentColor = Color.White
            ) {
                Tab(
                    selected = tabSeleccionada == 0,
                    onClick = { tabSeleccionada = 0 },
                    text = { Text("📍 1. Mi Estado Actual") }
                )
                Tab(
                    selected = tabSeleccionada == 1,
                    onClick = { tabSeleccionada = 1 },
                    text = { Text("🗺️ 2. Evaluar Estado Posible") }
                )
            }

            // Contenido según modo
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                if (tabSeleccionada == 0) {
                    VistaMiEstadoActual(
                        engine = engine,
                        perfil = perfilGuardado,
                        forzarEdicion = mostrarEditorPerfil,
                        tienePermisoGps = tienePermisoGps,
                        obteniendoGps = obteniendoGps,
                        errorGps = errorGps,
                        gpsLatReal = gpsLatReal,
                        gpsLonReal = gpsLonReal,
                        onGuardarPerfil = { nuevoSexo, nuevaEdad ->
                            prefsRepo.guardarPerfil(nuevoSexo, nuevaEdad)
                            perfilGuardado = UserProfile(nuevoSexo, nuevaEdad)
                            mostrarEditorPerfil = false
                        },
                        onSolicitarPermisoGps = onSolicitarPermisoGps,
                        onRefrescarGps = onRefrescarGps
                    )
                } else {
                    VistaSimuladorEstadoPosible(
                        engine = engine,
                        gpsLatReferencia = gpsLatReal,
                        gpsLonReferencia = gpsLonReal
                    )
                }
            }
        }
    }
}

@Composable
fun VistaMiEstadoActual(
    engine: RiskInferenceEngine,
    perfil: UserProfile?,
    forzarEdicion: Boolean,
    tienePermisoGps: Boolean,
    obteniendoGps: Boolean,
    errorGps: String?,
    gpsLatReal: Double?,
    gpsLonReal: Double?,
    onGuardarPerfil: (String, Int) -> Unit,
    onSolicitarPermisoGps: () -> Unit,
    onRefrescarGps: () -> Unit
) {
    // PASO A: Si no hay permisos de GPS, exigirlos antes de cualquier cosa
    if (!tienePermisoGps) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFB71C1C))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(" Permiso de Ubicación Requerido", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "Para calcular tu estado actual no asumimos ninguna coordenada por defecto. Debes autorizar el acceso al GPS del dispositivo.",
                    color = Color.White,
                    fontSize = 13.sp
                )
                Button(
                    onClick = onSolicitarPermisoGps,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Text("Conceder Permiso de GPS", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    // PASO B: Si no existe perfil guardado (o el usuario pidió editarlo), preguntar Género y Edad
    if (perfil == null || forzarEdicion) {
        FormularioCapturaPerfil(
            titulo = "Configurar Mi Perfil Demográfico",
            subtitulo = "Estos datos se guardarán en tu dispositivo para evaluar tu riesgo personal con el modelo Naive Bayes.",
            sexoInicial = perfil?.sexo,
            edadInicial = perfil?.edad?.toString() ?: "",
            onConfirmar = onGuardarPerfil
        )
        return
    }

    // PASO C: Verificar que el hardware GPS ya haya entregado coordenadas reales
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (gpsLatReal != null) "🛰️ GPS: ${"%.5f".format(gpsLatReal)}, ${"%.5f".format(gpsLonReal)}"
                else "🛰️ Esperando señal GPS...",
                fontSize = 12.sp,
                color = Color.Gray
            )
            OutlinedButton(onClick = onRefrescarGps, enabled = !obteniendoGps) {
                Text(if (obteniendoGps) "Buscando..." else "Actualizar GPS y Hora", fontSize = 12.sp)
            }
        }

        if (obteniendoGps) {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (errorGps != null) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF4E342E))) {
                Text(errorGps, color = Color.White, modifier = Modifier.padding(16.dp), fontSize = 13.sp)
            }
        } else if (gpsLatReal != null && gpsLonReal != null) {
            // Todo verificado: Coordenada real del GPS + Perfil guardado + Hora real del sistema
            val tiempoSistema = engine.obtenerTiempoActualDelSistema()
            val resultado = engine.evaluarRiesgo(
                latitud = gpsLatReal,
                longitud = gpsLonReal,
                sexo = perfil.sexo,
                edad = perfil.edad,
                mes = tiempoSistema.mes,
                diaSemana = tiempoSistema.diaSemana,
                hora = tiempoSistema.hora
            )
            TarjetaDesgloseRiesgo(resultado)
        }
    }
}

@Composable
fun VistaSimuladorEstadoPosible(
    engine: RiskInferenceEngine,
    gpsLatReferencia: Double?,
    gpsLonReferencia: Double?
) {
    // Ningún dato se asume por defecto; el usuario los selecciona explícitamente
    var sexoSimulado by remember { mutableStateOf<String?>(null) }
    var edadSimuladaStr by remember { mutableStateOf("") }
    var latSeleccionadaMapa by remember { mutableStateOf<Double?>(null) }
    var lonSeleccionadaMapa by remember { mutableStateOf<Double?>(null) }

    // Para el tiempo del estado posible: carga inicialmente el reloj real mediante función, pero permite cambiarlo
    val tiempoInicial = remember { engine.obtenerTiempoActualDelSistema() }
    var mesSimuladoStr by remember { mutableStateOf(tiempoInicial.mes.toString()) }
    var diaSimulado by remember { mutableStateOf(tiempoInicial.diaSemana) }
    var horaSimuladaStr by remember { mutableStateOf(tiempoInicial.hora.toString()) }

    val diasSemana = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF252525)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("1. Toca el mapa para elegir la ubicación a evaluar", color = Color(0xFF80CBC4), fontWeight = FontWeight.Bold, fontSize = 14.sp)

                InteractiveMapPicker(
                    selectedLat = latSeleccionadaMapa,
                    selectedLon = lonSeleccionadaMapa,
                    initialCenterLat = gpsLatReferencia,
                    initialCenterLon = gpsLonReferencia,
                    onPointSelected = { lat, lon ->
                        latSeleccionadaMapa = lat
                        lonSeleccionadaMapa = lon
                    }
                )

                Text(
                    text = if (latSeleccionadaMapa != null)
                        "📌 Punto marcado: Lat ${"%.5f".format(latSeleccionadaMapa)}, Lon ${"%.5f".format(lonSeleccionadaMapa)}"
                    else "⚠️ Aún no has tocado ningún punto en el mapa.",
                    fontSize = 12.sp,
                    color = if (latSeleccionadaMapa != null) Color(0xFFA5D6A7) else Color(0xFFFFAB91)
                )

                HorizontalDivider(color = Color.DarkGray)

                Text("2. Ingresa el Perfil a evaluar", color = Color(0xFF80CBC4), fontWeight = FontWeight.Bold, fontSize = 14.sp)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("Hombre", "Mujer").forEach { opcion ->
                        Button(
                            onClick = { sexoSimulado = opcion },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (sexoSimulado == opcion) Color(0xFF1976D2) else Color(0xFF3A3A3A)
                            )
                        ) {
                            Text(opcion, color = Color.White)
                        }
                    }
                }

                OutlinedTextField(
                    value = edadSimuladaStr,
                    onValueChange = { edadSimuladaStr = it.filter { c -> c.isDigit() } },
                    label = { Text("Edad a evaluar (años)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(color = Color.DarkGray)

                Text("3. Condición Temporal (Mes, Día y Hora)", color = Color(0xFF80CBC4), fontWeight = FontWeight.Bold, fontSize = 14.sp)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = mesSimuladoStr,
                        onValueChange = { mesSimuladoStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Mes (1-12)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = horaSimuladaStr,
                        onValueChange = { horaSimuladaStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Hora (0-23)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Selector rápido de día de la semana
                SecondaryScrollableTabRow(
                    selectedTabIndex = diasSemana.indexOf(diaSimulado).coerceAtLeast(0),
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent
                ) {
                    diasSemana.forEach { d ->
                        Tab(
                            selected = diaSimulado == d,
                            onClick = { diaSimulado = d },
                            text = { Text(d, fontSize = 12.sp) }
                        )
                    }
                }
            }
        }

        // Validación estricta de todos los campos antes de calcular
        val edadInt = edadSimuladaStr.toIntOrNull()
        val mesInt = mesSimuladoStr.toIntOrNull()
        val horaInt = horaSimuladaStr.toIntOrNull()

        val datosCompletos = sexoSimulado != null &&
                edadInt != null && edadInt in 1..110 &&
                latSeleccionadaMapa != null && lonSeleccionadaMapa != null &&
                mesInt != null && mesInt in 1..12 &&
                horaInt != null && horaInt in 0..23

        if (datosCompletos) {
            val resultadoSimulado = engine.evaluarRiesgo(
                latitud = latSeleccionadaMapa!!,
                longitud = lonSeleccionadaMapa!!,
                sexo = sexoSimulado!!,
                edad = edadSimuladaStr.toInt(),
                mes = mesSimuladoStr.toInt(),
                diaSemana = diaSimulado,
                hora = horaSimuladaStr.toInt()
            )
            TarjetaDesgloseRiesgo(resultadoSimulado)
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF37474F))
            ) {
                Text(
                    text = "ℹ️ Selecciona un punto en el mapa, elige el sexo y escribe una edad válida para ejecutar el cálculo.",
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun FormularioCapturaPerfil(
    titulo: String,
    subtitulo: String,
    sexoInicial: String?,
    edadInicial: String,
    onConfirmar: (String, Int) -> Unit
) {
    var sexoSeleccionado by remember { mutableStateOf(sexoInicial) }
    var edadTexto by remember { mutableStateOf(edadInicial) }
    var mensajeError by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF262626))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(titulo, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(subtitulo, color = Color.LightGray, fontSize = 12.sp)

            Text("Selecciona tu Sexo:", color = Color.White, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("Hombre", "Mujer").forEach { sexo ->
                    Button(
                        onClick = { sexoSeleccionado = sexo },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (sexoSeleccionado == sexo) Color(0xFF1976D2) else Color(0xFF424242)
                        )
                    ) {
                        Text(sexo, color = Color.White)
                    }
                }
            }

            OutlinedTextField(
                value = edadTexto,
                onValueChange = { edadTexto = it.filter { char -> char.isDigit() } },
                label = { Text("Ingresa tu Edad exacta (años)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            if (mensajeError != null) {
                Text(mensajeError!!, color = Color(0xFFFF8A80), fontSize = 12.sp)
            }

            Button(
                onClick = {
                    val edadNum = edadTexto.toIntOrNull()
                    when {
                        sexoSeleccionado == null -> mensajeError = "Debes seleccionar un sexo."
                        edadNum == null || edadNum !in 1..110 -> mensajeError = "Ingresa una edad válida entre 1 y 110 años."
                        else -> {
                            mensajeError = null
                            onConfirmar(sexoSeleccionado!!, edadNum)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))
            ) {
                Text("Guardar Perfil y Calcular Riesgo", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TarjetaDesgloseRiesgo(res: RiskResult) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Tarjeta Principal de Score
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(res.colorHex))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(res.nivelAlerta, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("${res.scoreFinal}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 52.sp)
                Text("Índice de Riesgo Combinado (0 - 100)", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
            }
        }

        // Tarjeta Espacial (K-Means k=7)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF262626))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("📍 Componente Espacial (K-Means k=7)", color = Color(0xFF80CBC4), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Sector Catastral: ${res.nombreBarrio} (${res.codigoSector})", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text("Clúster Tipológico: ${res.clusterId} de 6 | Score Base: ${res.scoreBaseCluster}", color = Color.LightGray, fontSize = 12.sp)
                Text("Distancia al centroide: ${res.distanciaMetros} m", color = Color.Gray, fontSize = 12.sp)
                Text(
                    "Historial (2023-2025): Hurtos=${res.hurtosHistoricos} | Lesiones=${res.lesionesHistoricas} | Homicidios=${res.homicidiosHistoricos}",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        }

        // Tarjeta Temporal y Demográfica (Poisson + Naive Bayes)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF262626))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("🧮 Multiplicadores del Motor", color = Color(0xFF80CBC4), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Factor Temporal (Poisson): ×${res.multTemporal}", color = Color.White, fontSize = 13.sp)
                Text(res.detalleTiempo, color = Color.Gray, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Factor Demográfico (Naive Bayes): ×${res.multDemografico}", color = Color.White, fontSize = 13.sp)
                Text(res.detallePerfil, color = Color.Gray, fontSize = 11.sp)
            }
        }

        // Recomendación
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E352F))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("💡 Directriz Preventiva:", color = Color(0xFFA5D6A7), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(res.recomendacion, color = Color.White, fontSize = 13.sp)
            }
        }
    }
}