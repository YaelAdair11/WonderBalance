package com.example.wonderbalance.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.wonderbalance.datos.entidad.Transaccion
import java.io.File
import java.io.FileOutputStream

object ReportExporter {

    fun compartirCSV(context: Context, datos: List<Transaccion>) {
        try {
            val carpeta = File(context.cacheDir, "reportes")
            if (!carpeta.exists()) carpeta.mkdirs()
            
            val archivo = File(carpeta, "Reporte_Balance.csv")
            val contenido = StringBuilder("Fecha,Concepto,Tipo,Monto\n")
            
            datos.forEach { t ->
                contenido.append("${t.fecha},${t.nota.ifEmpty { "Sin nota" }},${t.tipo},${t.monto}\n")
            }
            
            FileOutputStream(archivo).use { it.write(contenido.toString().toByteArray()) }
            lanzarIntentCompartir(context, archivo, "text/csv")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun compartirPDF(context: Context, datos: List<Transaccion>) {
        val documento = PdfDocument()
        val paginaInfo = PdfDocument.PageInfo.Builder(300, 600, 1).create()
        val pagina = documento.startPage(paginaInfo)
        val canvas: Canvas = pagina.canvas
        val pincel = Paint()

        var y = 40f
        pincel.textSize = 14f
        pincel.isFakeBoldText = true
        canvas.drawText("WonderBalance - Reporte", 50f, y, pincel)
        
        y += 30f
        pincel.textSize = 10f
        pincel.isFakeBoldText = false
        
        datos.take(20).forEach { t ->
            canvas.drawText("${t.fecha} | ${t.tipo}: $${t.monto}", 20f, y, pincel)
            y += 20f
        }

        documento.finishPage(pagina)
        
        val carpeta = File(context.cacheDir, "reportes")
        if (!carpeta.exists()) carpeta.mkdirs()
        val archivo = File(carpeta, "Reporte_Balance.pdf")

        try {
            documento.writeTo(FileOutputStream(archivo))
            lanzarIntentCompartir(context, archivo, "application/pdf")
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            documento.close()
        }
    }

    private fun lanzarIntentCompartir(context: Context, archivo: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            archivo
        )
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir reporte con:"))
    }
}
