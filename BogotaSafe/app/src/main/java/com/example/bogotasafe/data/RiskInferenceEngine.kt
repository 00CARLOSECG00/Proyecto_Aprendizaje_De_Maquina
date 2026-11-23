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
    val recomendacion: String,
    // Factores individuales (para la explicación en lenguaje humano)
    val factorMes: Double = 1.0,
    val factorDia: Double = 1.0,
    val factorHora: Double = 1.0,
    val factorSexo: Double = 1.0,
    val factorEdad: Double = 1.0,
    val cicloVital: String = "",
    // Entradas usadas (para la sección técnica)
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    val mes: Int = 0,
    val diaSemana: String = "",
    val hora: Int = 0,
    val sexo: String = "",
    val edad: Int = 0
)

/** Sector catastral precargado en memoria (evita recorrer el JSON en cada cálculo). */
private class Sector(
    val lat: Double,
    val lon: Double,
    val latRad: Double,
    val cosLat: Double,
    val scoreBase: Double,
    val nombre: String,
    val id: String,
    val clusterId: Int,
    val hurtos: Int,
    val homicidios: Int,
    val lesiones: Int
)
class RiskInferenceEngine(context: Context) {

    private val sectores: Array<Sector>
    private val factorMes: Map<String, Double>
    private val factorDia: Map<String, Double>
    private val factorHora: Map<String, Double>
    private val multSexo: Map<String, Double>
    private val multCicloVital: Map<String, Double>

    init {
        // Carga el archivo JSON desde res/raw de forma local y 100% offline.
        // Se parsea UNA sola vez y se guarda en arreglos y mapas para que cada cálculo sea barato.
        val inputStream = context.resources.openRawResource(R.raw.modelo_seguridad_bogota)
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(jsonString)

        val arr = json.getJSONArray("sectores_urbanos")
        sectores = Array(arr.length()) { i ->
            val s = arr.getJSONObject(i)
            val lat = s.getDouble("lat")
            val m = s.getJSONObject("metricas_historicas")
            Sector(
                lat = lat,
                lon = s.getDouble("lon"),
                latRad = Math.toRadians(lat),
                cosLat = cos(Math.toRadians(lat)),
                scoreBase = s.getDouble("score_base"),
                nombre = s.getString("nombre_barrio"),
                id = s.getString("id_sector"),
                clusterId = s.getInt("cluster_id"),
                hurtos = m.getInt("hurtos"),
                homicidios = m.getInt("homicidios"),
                lesiones = m.getInt("lesiones")
            )
        }
        val temp = json.getJSONObject("modelo_temporal")
        factorMes = aMapa(temp.getJSONObject("factor_mes"))
        factorDia = aMapa(temp.getJSONObject("factor_dia"))
        factorHora = aMapa(temp.getJSONObject("factor_hora"))
        val demo = json.getJSONObject("modelo_demografico")
        multSexo = aMapa(demo.getJSONObject("multiplicador_sexo"))
        multCicloVital = aMapa(demo.getJSONObject("multiplicador_ciclo_vital"))
    }

    private fun aMapa(o: JSONObject): Map<String, Double> =
        o.keys().asSequence().associateWith { o.getDouble(it) }

    /** Sector más cercano a una coordenada (distancia de Haversine). */
    private fun buscarSector(latitud: Double, longitud: Double): Pair<Sector, Double> {
        val latRad = Math.toRadians(latitud)
        val cosLat = cos(latRad)
        var mejor = sectores[0]
        var mejorA = Double.MAX_VALUE
        // Se compara el término "a" de Haversine (monótono con la distancia) y solo
        // al final se calcula la distancia real, evitando asin/sqrt en cada sector.
        for (s in sectores) {
            val dLat = s.latRad - latRad
            val dLon = Math.toRadians(s.lon - longitud)
            val sLat = sin(dLat / 2)
            val sLon = sin(dLon / 2)
            val a = sLat * sLat + cosLat * s.cosLat * sLon * sLon
            if (a < mejorA) {
                mejorA = a
                mejor = s
            }
        }
        val distKm = 6371.0 * 2 * asin(sqrt(mejorA))
        return mejor to distKm
    }

    /** Nombre del sector catastral más cercano (para mostrarlo al marcar un punto en el mapa). */
    fun nombreSectorCercano(latitud: Double, longitud: Double): String =
        buscarSector(latitud, longitud).first.nombre

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
        val (sector, distanciaMinimaKm) = buscarSector(latitud, longitud)
        val scoreBase = sector.scoreBase

        // 1. Inferencia Temporal (Regresión de Poisson)
        val fMes = factorMes.getValue(mes.toString())
        val fDia = factorDia.getValue(diaSemana)
        val fHora = factorHora.getValue(hora.toString())
        val multTemporal = fMes * fDia * fHora

        // 2. Inferencia Demográfica (Naive Bayes)
        val cicloVital = mapearEdadACicloVital(edad)
        val fSexo = multSexo.getValue(sexo)
        val fEdad = multCicloVital.getValue(cicloVital)
        val multDemografico = (0.55 * fSexo) + (0.45 * fEdad)

        // 3. Ensamble del Score Final
        val factorCombinado = (0.65 * multTemporal) + (0.35 * multDemografico)
        val scoreFinal = max(5.0, min(99.0, round(scoreBase * factorCombinado * 10.0) / 10.0))

        val (nivel, colorHex, recomendacion) = when {
            scoreFinal >= 70.0 -> Triple(
                "RIESGO ALTO / CRÍTICO",
                0xFFEF4444,
                "Alerta máxima: Sector con alta densidad delictiva exacerbada por la franja temporal. Evite transitar a pie o exponer objetos de valor."
            )
            scoreFinal >= 45.0 -> Triple(
                "RIESGO MODERADO",
                0xFFF59E0B,
                "Precaución activa: Manténgase sobre vías principales iluminadas y evite paraderos solitarios."
            )
            else -> Triple(
                "RIESGO BAJO",
                0xFF22C55E,
                "Condiciones favorables: El sector catastral y el horario presentan baja incidencia histórica."
            )
        }

        return RiskResult(
            scoreFinal = scoreFinal,
            nivelAlerta = nivel,
            colorHex = colorHex,
            nombreBarrio = sector.nombre,
            codigoSector = sector.id,
            clusterId = sector.clusterId,
            scoreBaseCluster = scoreBase,
            distanciaMetros = round(distanciaMinimaKm * 1000).toLong(),
            multTemporal = round(multTemporal * 1000.0) / 1000.0,
            multDemografico = round(multDemografico * 1000.0) / 1000.0,
            detalleTiempo = "Mes($mes)=$fMes × $diaSemana=$fDia × Hora(${hora}h)=$fHora",
            detallePerfil = "$sexo=$fSexo | $cicloVital=$fEdad",
            hurtosHistoricos = sector.hurtos,
            homicidiosHistoricos = sector.homicidios,
            lesionesHistoricas = sector.lesiones,
            recomendacion = recomendacion,
            factorMes = fMes,
            factorDia = fDia,
            factorHora = fHora,
            factorSexo = fSexo,
            factorEdad = fEdad,
            cicloVital = cicloVital,
            latitud = latitud,
            longitud = longitud,
            mes = mes,
            diaSemana = diaSemana,
            hora = hora,
            sexo = sexo,
            edad = edad
        )
    }


}