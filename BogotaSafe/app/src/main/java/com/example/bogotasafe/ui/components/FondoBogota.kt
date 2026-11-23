package com.example.bogotasafe.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.sin
import kotlin.random.Random

/*
 * Fondo ilustrado: Bogotá de noche.
 * Cielo, luna, los cerros orientales con Monserrate y una ciudad iluminada.
 * Se dibuja completamente en código (sin imágenes externas) y es la fuente
 * que el vidrio refracta y desenfoca.
 */

private val CieloAlto = Color(0xFF070B1E)
private val CieloMedio = Color(0xFF14204A)
private val CieloBajo = Color(0xFF2A2463)
private val CerroLejano = Color(0xFF1B2452)
private val CerroCercano = Color(0xFF111834)
private val Edificio = listOf(Color(0xFF0C1230), Color(0xFF101840), Color(0xFF0E1538))
private val LuzCalida = Color(0xFFFDE7A6)
private val LuzFria = Color(0xFFBFDBFE)
private val LuzColpatria = listOf(Color(0xFF60A5FA), Color(0xFFA78BFA), Color(0xFF22D3EE))

/**
 * La escena se renderiza UNA sola vez en un bitmap (por tamaño de pantalla) y después
 * solo se copia esa imagen. Así el fondo no vuelve a dibujar cientos de figuras en cada frame.
 */
@Composable
fun FondoBogota(modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier.drawWithCache {
            val ancho = size.width.toInt()
            val alto = size.height.toInt()
            if (ancho <= 0 || alto <= 0) return@drawWithCache onDrawBehind { }
            val imagen = ImageBitmap(ancho, alto)
            CanvasDrawScope().draw(this, layoutDirection, Canvas(imagen), size) {
                dibujarEscena()
            }
            onDrawBehind { drawImage(imagen) }
        }
    )
}

private fun DrawScope.dibujarEscena() {
    run {
        val w = size.width
        val h = size.height
        val azar = Random(2506)

        // Cielo
        drawRect(Brush.verticalGradient(listOf(CieloAlto, CieloMedio, CieloBajo), endY = h * 0.75f))

        // Halos de luz (se ven a través del vidrio)
        halo(Offset(w * 0.1f, h * 0.18f), w * 0.9f, Color(0xFF60A5FA), 0.35f)
        halo(Offset(w * 0.95f, h * 0.45f), w * 0.8f, Color(0xFF8B5CF6), 0.30f)
        halo(Offset(w * 0.5f, h * 0.72f), w * 0.9f, Color(0xFFF59E0B), 0.10f)

        // Estrellas
        repeat(90) {
            val x = azar.nextFloat() * w
            val y = azar.nextFloat() * h * 0.45f
            drawCircle(Color.White.copy(alpha = 0.25f + azar.nextFloat() * 0.6f), radius = 1f + azar.nextFloat() * 2.2f, center = Offset(x, y))
        }

        // Luna
        val luna = Offset(w * 0.78f, h * 0.13f)
        halo(luna, w * 0.28f, Color(0xFFE0E7FF), 0.35f)
        drawCircle(Color(0xFFF1F5F9), radius = w * 0.07f, center = luna)
        drawCircle(Color(0xFFCBD5E1).copy(alpha = 0.5f), radius = w * 0.015f, center = luna + Offset(-w * 0.02f, -w * 0.015f))
        drawCircle(Color(0xFFCBD5E1).copy(alpha = 0.4f), radius = w * 0.01f, center = luna + Offset(w * 0.025f, w * 0.02f))

        // Cerros orientales (dos capas) y Monserrate
        val baseCerros = h * 0.58f
        cerro(baseCerros, h * 0.14f, CerroLejano, fase = 0.4f, frecuencia = 2.3f)
        val cimaMonserrate = Offset(w * 0.32f, baseCerros - h * 0.2f)
        val cercano = Path().apply {
            moveTo(0f, baseCerros + h * 0.04f)
            lineTo(w * 0.12f, baseCerros - h * 0.06f)
            lineTo(cimaMonserrate.x, cimaMonserrate.y)
            lineTo(w * 0.5f, baseCerros - h * 0.09f)
            lineTo(w * 0.68f, baseCerros - h * 0.13f)
            lineTo(w * 0.85f, baseCerros - h * 0.05f)
            lineTo(w, baseCerros - h * 0.08f)
            lineTo(w, h); lineTo(0f, h); close()
        }
        drawPath(cercano, CerroCercano)
        // Santuario de Monserrate iluminado en la cima
        halo(cimaMonserrate, w * 0.08f, LuzCalida, 0.7f)
        drawRect(LuzCalida, topLeft = cimaMonserrate + Offset(-6f, -10f), size = Size(12f, 10f))
        // Teleférico
        drawLine(LuzCalida.copy(alpha = 0.35f), cimaMonserrate, Offset(w * 0.45f, baseCerros + h * 0.05f), strokeWidth = 2f)

        // Ciudad
        val baseCiudad = h * 0.66f
        drawRect(Color(0xFF0A0F28), topLeft = Offset(0f, baseCiudad), size = Size(w, h - baseCiudad))
        var x = -10f
        while (x < w) {
            val ancho = w * (0.05f + azar.nextFloat() * 0.08f)
            val alto = h * (0.03f + azar.nextFloat() * 0.12f)
            edificio(x, baseCiudad + h * 0.06f, ancho, alto, Edificio[azar.nextInt(Edificio.size)], azar)
            x += ancho + w * 0.008f
        }

        // Torre Colpatria con sus luces de colores
        val anchoTorre = w * 0.075f
        val altoTorre = h * 0.25f
        val xTorre = w * 0.6f
        val yTorre = baseCiudad + h * 0.06f - altoTorre
        val colorTorre = LuzColpatria[azar.nextInt(LuzColpatria.size)]
        halo(Offset(xTorre + anchoTorre / 2, yTorre + altoTorre * 0.3f), w * 0.3f, colorTorre, 0.4f)
        drawRect(Brush.verticalGradient(listOf(colorTorre, Color(0xFF1E1B4B)), startY = yTorre, endY = yTorre + altoTorre),
            topLeft = Offset(xTorre, yTorre), size = Size(anchoTorre, altoTorre))
        drawRect(Color.White.copy(alpha = 0.6f), topLeft = Offset(xTorre + anchoTorre * 0.48f, yTorre - h * 0.02f), size = Size(3f, h * 0.02f))

        // Primer plano: otra fila de edificios más cercanos
        var x2 = -20f
        while (x2 < w) {
            val ancho = w * (0.08f + azar.nextFloat() * 0.1f)
            val alto = h * (0.04f + azar.nextFloat() * 0.1f)
            edificio(x2, h * 0.82f, ancho, alto, Color(0xFF080C22), azar, densidad = 0.25f)
            x2 += ancho + w * 0.01f
        }
        drawRect(Color(0xFF060918), topLeft = Offset(0f, h * 0.82f), size = Size(w, h * 0.18f))

        // Avenida con luces de vehículos
        val yAvenida = h * 0.86f
        repeat(60) {
            val px = azar.nextFloat() * w
            val roja = azar.nextBoolean()
            val c = if (roja) Color(0xFFF87171) else LuzCalida
            drawCircle(c.copy(alpha = 0.8f), radius = 2.5f, center = Offset(px, yAvenida + (if (roja) 0f else 14f)))
            halo(Offset(px, yAvenida + (if (roja) 0f else 14f)), 18f, c, 0.25f)
        }
        // Velo inferior para que el contenido siga siendo legible
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF050816).copy(alpha = 0.7f)), startY = h * 0.75f, endY = h))
    }
}

private fun DrawScope.halo(centro: Offset, radio: Float, color: Color, alfa: Float) {
    drawCircle(
        brush = Brush.radialGradient(listOf(color.copy(alpha = alfa), Color.Transparent), center = centro, radius = radio),
        radius = radio,
        center = centro
    )
}

private fun DrawScope.cerro(base: Float, amplitud: Float, color: Color, fase: Float, frecuencia: Float) {
    val w = size.width
    val p = Path().apply {
        moveTo(0f, size.height)
        var x = 0f
        while (x <= w) {
            val t = x / w
            val y = base - amplitud * (0.55f + 0.3f * sin((t * frecuencia + fase) * 6.28f) + 0.15f * sin((t * 7.1f + fase) * 6.28f))
            lineTo(x, y)
            x += w / 60f
        }
        lineTo(w, size.height); close()
    }
    drawPath(p, color)
}

private fun DrawScope.edificio(
    x: Float, base: Float, ancho: Float, alto: Float, color: Color, azar: Random, densidad: Float = 0.45f
) {
    val top = base - alto
    drawRect(color, topLeft = Offset(x, top), size = Size(ancho, alto))
    val tam = 5f
    val sep = 11f
    var wy = top + 8f
    while (wy < base - 8f) {
        var wx = x + 6f
        while (wx < x + ancho - 8f) {
            if (azar.nextFloat() < densidad) {
                val c = if (azar.nextFloat() < 0.75f) LuzCalida else LuzFria
                drawRect(c.copy(alpha = 0.5f + azar.nextFloat() * 0.45f), topLeft = Offset(wx, wy), size = Size(tam, tam))
            }
            wx += sep
        }
        wy += sep + 3f
    }
}
