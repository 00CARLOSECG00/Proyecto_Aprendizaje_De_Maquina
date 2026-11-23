package com.example.bogotasafe



import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
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
import com.example.bogotasafe.ui.components.*
import com.example.bogotasafe.ui.screens.PantallaMiUbicacion
import com.example.bogotasafe.ui.screens.PantallaSimular
import com.example.bogotasafe.ui.screens.SimulacionState
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.bogotasafe.ui.theme.*
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
                run {
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
    var perfilGuardado by remember { mutableStateOf(prefsRepo.obtenerPerfilGuardado()) }
    var editandoPerfil by remember { mutableStateOf(false) }
    var tabSeleccionada by rememberSaveable { mutableIntStateOf(0) }
    // El estado del asistente vive aquí para conservar el progreso al cambiar de pestaña
    val simulacion = remember { SimulacionState(engine.obtenerTiempoActualDelSistema()) }

    FondoLiquido {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text("BogotáSafe", color = TextoPrincipal, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    actions = {
                        if (perfilGuardado != null && !editandoPerfil) {
                            GlassButton(
                                texto = "${perfilGuardado!!.sexo}, ${perfilGuardado!!.edad}",
                                onClick = {
                                    editandoPerfil = true
                                    tabSeleccionada = 0
                                },
                                liquido = true,
                                modifier = Modifier.padding(end = 12.dp)
                            )
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
                // Selector de modo: control segmentado de vidrio
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .vidrioLiquido(forma = RoundedCornerShape(50))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Mi ubicación", "Simular").forEachIndexed { indice, etiqueta ->
                        GlassButton(
                            texto = etiqueta,
                            onClick = { tabSeleccionada = indice },
                            destacado = tabSeleccionada == indice,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    if (tabSeleccionada == 0) {
                        PantallaMiUbicacion(
                            engine = engine,
                            perfil = perfilGuardado,
                            editandoPerfil = editandoPerfil,
                            tienePermisoGps = tienePermisoGps,
                            obteniendoGps = obteniendoGps,
                            errorGps = errorGps,
                            gpsLat = gpsLatReal,
                            gpsLon = gpsLonReal,
                            onGuardarPerfil = { sexo, edad ->
                                prefsRepo.guardarPerfil(sexo, edad)
                                perfilGuardado = UserProfile(sexo, edad)
                                editandoPerfil = false
                            },
                            onCancelarEdicion = { editandoPerfil = false },
                            onSolicitarPermisoGps = onSolicitarPermisoGps,
                            onRefrescarGps = onRefrescarGps
                        )
                    } else {
                        PantallaSimular(
                            engine = engine,
                            estado = simulacion,
                            perfilGuardado = perfilGuardado,
                            gpsLat = gpsLatReal,
                            gpsLon = gpsLonReal
                        )
                    }
                }
            }
        }
    }
}
