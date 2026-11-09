package com.example.bogotasafe.data

import android.content.Context
import com.example.bogotasafe.R
import org.json.JSONObject
import java.util.Calendar
import kotlin.math.*

data class SystemTimeData(
    val mes: Int,
    val diaSemana: String,
    val hora: Int
)

data class RiskResult(
    val scoreFinal: Double,
    val nivelAlerta: String,
    val colorHex: Long,
    val nombreBarrio: String,
    val codigoSector: String,
    val clusterId: Int,
    val scoreBaseCluster: Double,
    val distanciaMetros: Long,
    val multTemporal: Double,
    val multDemografico: Double,
    val detalleTiempo: String,
    val detallePerfil: String,
    val hurtosHistoricos: Int,
    val homicidiosHistoricos: Int,
    val lesionesHistoricas: Int,
    val recomendacion: String
)
class RiskInferenceEngine(context: Context) {

    private val jsonObject: JSONObject

    init {
        // Carga el archivo JSON desde res/raw de forma local y 100% offline
        val inputStream = context.resources.openRawResource(R.raw.modelo_seguridad_bogota)
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        jsonObject = JSONObject(jsonString)
    }

    fun obtenerTiempoActualDelSistema(): SystemTimeData {
        val calendar = Calendar.getInstance()
        val mes = calendar.get(Calendar.MONTH) + 1
        val hora = calendar.get(Calendar.HOUR_OF_DAY)
        val diaSemana = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Lunes"
            Calendar.TUESDAY -> "Martes"
            Calendar.WEDNESDAY -> "Miércoles"
            Calendar.THURSDAY -> "Jueves"
            Calendar.FRIDAY -> "Viernes"
            Calendar.SATURDAY -> "Sábado"
            Calendar.SUNDAY -> "Domingo"
            else -> throw IllegalStateException("Día de calendario inválido")
        }
        return SystemTimeData(mes = mes, diaSemana = diaSemana, hora = hora)
    }
    private fun calcularDistanciaHaversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val radioTierraKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return radioTierraKm * 2 * asin(sqrt(a))
    }

    fun mapearEdadACicloVital(edad: Int): String {
        return when {
            edad <= 5 -> "(00 a 05) Primera Infancia"
            edad <= 11 -> "(06 a 11) Infancia"
            edad <= 17 -> "(12 a 17) Adolescencia"
            edad <= 28 -> "(18 a 28) Juventud"
            edad <= 59 -> "(29 a 59) Adultez"
            else -> "(Más de 60) Adulto Mayor"
        }
    }

    fun evaluarRiesgo(
        latitud: Double,
        longitud: Double,
        sexo: String,
        edad: Int,
        mes: Int,
        diaSemana: String,
        hora: Int
    ): RiskResult {
        val sectores = jsonObject.getJSONArray("sectores_urbanos")

        var sectorMasCercano: JSONObject? = null
        var distanciaMinimaKm = Double.MAX_VALUE

        for (i in 0 until sectores.length()) {
            val s = sectores.getJSONObject(i)
            val dist = calcularDistanciaHaversineKm(
                latitud, longitud,
                s.getDouble("lat"), s.getDouble("lon")
            )
            if (dist < distanciaMinimaKm) {
                distanciaMinimaKm = dist
                sectorMasCercano = s
            }
        }

        val sector = requireNotNull(sectorMasCercano) { "El JSON no contiene sectores válidos." }

        val scoreBase = sector.getDouble("score_base")
        val nombreBarrio = sector.getString("nombre_barrio")
        val idSector = sector.getString("id_sector")
        val clusterId = sector.getInt("cluster_id")
        val metricas = sector.getJSONObject("metricas_historicas")

        // 1. Inferencia Temporal (Regresión de Poisson)
        val modeloTemp = jsonObject.getJSONObject("modelo_temporal")
        val fMes = modeloTemp.getJSONObject("factor_mes").getDouble(mes.toString())
        val fDia = modeloTemp.getJSONObject("factor_dia").getDouble(diaSemana)
        val fHora = modeloTemp.getJSONObject("factor_hora").getDouble(hora.toString())
        val multTemporal = fMes * fDia * fHora

        // 2. Inferencia Demográfica (Naive Bayes)
        val modeloDemo = jsonObject.getJSONObject("modelo_demografico")
        val cicloVital = mapearEdadACicloVital(edad)
        val fSexo = modeloDemo.getJSONObject("multiplicador_sexo").getDouble(sexo)
        val fEdad = modeloDemo.getJSONObject("multiplicador_ciclo_vital").getDouble(cicloVital)
        val multDemografico = (0.55 * fSexo) + (0.45 * fEdad)

        // 3. Ensamble del Score Final
        val factorCombinado = (0.65 * multTemporal) + (0.35 * multDemografico)
        val scoreFinal = max(5.0, min(99.0, round(scoreBase * factorCombinado * 10.0) / 10.0))

        val (nivel, colorHex, recomendacion) = when {
            scoreFinal >= 70.0 -> Triple(
                "RIESGO ALTO / CRÍTICO",
                0xFFD32F2F,
                "Alerta máxima: Sector con alta densidad delictiva exacerbada por la franja temporal. Evite transitar a pie o exponer objetos de valor."
            )
            scoreFinal >= 45.0 -> Triple(
                "RIESGO MODERADO",
                0xFFF57C00,
                "Precaución activa: Manténgase sobre vías principales iluminadas y evite paraderos solitarios."
            )
            else -> Triple(
                "RIESGO BAJO",
                0xFF388E3C,
                "Condiciones favorables: El sector catastral y el horario presentan baja incidencia histórica."
            )
        }

        return RiskResult(
            scoreFinal = scoreFinal,
            nivelAlerta = nivel,
            colorHex = colorHex,
            nombreBarrio = nombreBarrio,
            codigoSector = idSector,
            clusterId = clusterId,
            scoreBaseCluster = scoreBase,
            distanciaMetros = round(distanciaMinimaKm * 1000).toLong(),
            multTemporal = round(multTemporal * 1000.0) / 1000.0,
            multDemografico = round(multDemografico * 1000.0) / 1000.0,
            detalleTiempo = "Mes($mes)=$fMes × $diaSemana=$fDia × Hora(${hora}h)=$fHora",
            detallePerfil = "$sexo=$fSexo | $cicloVital=$fEdad",
            hurtosHistoricos = metricas.getInt("hurtos"),
            homicidiosHistoricos = metricas.getInt("homicidios"),
            lesionesHistoricas = metricas.getInt("lesiones"),
            recomendacion = recomendacion
        )
    }


}