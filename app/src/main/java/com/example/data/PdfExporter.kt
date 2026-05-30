package com.example.data

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object PdfExporter {

    fun exportToPdf(
        context: Context,
        monthString: String,
        transactions: List<TransactionEntity>,
        budgets: Map<String, Double>,
        aiTips: String
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Paints
        val titlePaint = Paint().apply {
            color = Color.parseColor("#008B8B") // Toska Primary
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val subtitlePaint = Paint().apply {
            color = Color.GRAY
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }

        val headingPaint = Paint().apply {
            color = Color.parseColor("#006666")
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val tableHeaderPaint = Paint().apply {
            color = Color.parseColor("#112625")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val normalPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
        }

        val incomePaint = Paint().apply {
            color = Color.parseColor("#2E7D32") // Green
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val expensePaint = Paint().apply {
            color = Color.parseColor("#C62828") // Red
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        val fillPaint = Paint().apply {
            color = Color.parseColor("#F4FBFB") // Soft background
            style = Paint.Style.FILL
        }

        val localeID = Locale("in", "ID")
        val formatRupiah = NumberFormat.getCurrencyInstance(localeID).apply {
            maximumFractionDigits = 0
        }
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", localeID)

        var yPos = 50f

        // Draw Header
        canvas.drawText("LAPORAN KEUANGAN BULANAN", 50f, yPos, titlePaint)
        yPos += 20f
        canvas.drawText("Periode: $monthString | Diunduh pada: ${SimpleDateFormat("dd MMMM yyyy HH:mm", localeID).format(Date())}", 50f, yPos, subtitlePaint)
        yPos += 30f

        // Financial Summary
        val totalIncome = transactions.filter { it.type == "PEMASUKAN" }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == "PENGELUARAN" }.sumOf { it.amount }
        val balance = totalIncome - totalExpense

        // Draw summary card
        canvas.drawRect(50f, yPos, 545f, yPos + 80f, fillPaint)
        canvas.drawRect(50f, yPos, 545f, yPos + 80f, Paint().apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#008B8B")
            strokeWidth = 1f
        })

        val cardY = yPos + 25f
        canvas.drawText("Total Pemasukan", 70f, cardY, headingPaint)
        canvas.drawText(formatRupiah.format(totalIncome), 70f, cardY + 20f, incomePaint)

        canvas.drawText("Total Pengeluaran", 230f, cardY, headingPaint)
        canvas.drawText(formatRupiah.format(totalExpense), 230f, cardY + 20f, expensePaint)

        canvas.drawText("Saldo Bersih", 390f, cardY, headingPaint)
        val balancePaint = if (balance >= 0) incomePaint else expensePaint
        canvas.drawText(formatRupiah.format(balance), 390f, cardY + 20f, balancePaint)

        yPos += 110f

        // Draw Table Header
        canvas.drawText("Daftar Transaksi:", 50f, yPos, headingPaint)
        yPos += 20f

        canvas.drawText("Tanggal", 50f, yPos, tableHeaderPaint)
        canvas.drawText("Judul / Kategori", 130f, yPos, tableHeaderPaint)
        canvas.drawText("Tipe", 350f, yPos, tableHeaderPaint)
        canvas.drawText("Jumlah", 450f, yPos, tableHeaderPaint)

        yPos += 8f
        canvas.drawLine(50f, yPos, 545f, yPos, linePaint)
        yPos += 18f

        // Draw Rows
        val maxRows = 20
        val displayTransactions = transactions.take(maxRows)
        for (tx in displayTransactions) {
            if (yPos > 650f) {
                // Warning if too many transactions (keep simple for high-performance single page first)
                canvas.drawText("...dan ${transactions.size - displayTransactions.indexOf(tx)} transaksi lainnya.", 50f, yPos, subtitlePaint)
                yPos += 20f
                break
            }
            canvas.drawText(dateFormat.format(Date(tx.timestamp)), 50f, yPos, normalPaint)
            
            // Draw title and category
            canvas.drawText("${tx.title} (${tx.category})", 130f, yPos, normalPaint)
            
            // Draw Type
            val isIncome = tx.type == "PEMASUKAN"
            canvas.drawText(
                if (isIncome) "PEMASUKAN" else "PENGELUARAN",
                350f,
                yPos,
                if (isIncome) incomePaint else expensePaint
            )

            // Draw Amount
            canvas.drawText(
                formatRupiah.format(tx.amount),
                450f,
                yPos,
                if (isIncome) incomePaint else expensePaint
            )

            yPos += 12f
            canvas.drawLine(50f, yPos, 545f, yPos, Paint().apply {
                color = Color.parseColor("#E0E0E0")
                strokeWidth = 0.5f
            })
            yPos += 15f
        }

        // Draw AI Suggestions Section at footer
        yPos += 15f
        if (yPos < 780f) {
            canvas.drawText("Rekomendasi Hemat AI:", 50f, yPos, headingPaint)
            yPos += 20f

            // Format Tips to wrap text
            val wrappedTips = wrapText(aiTips, 85)
            for (line in wrappedTips.take(4)) {
                if (yPos > 810f) break
                canvas.drawText(line, 50f, yPos, normalPaint)
                yPos += 14f
            }
        }

        pdfDocument.finishPage(page)

        // Write PDF to cache directory
        val fileName = "Laporan_Keuangan_$monthString.pdf"
        val storageDir = context.getExternalFilesDir(null) ?: context.cacheDir
        val pdfFile = File(storageDir, fileName)

        try {
            FileOutputStream(pdfFile).use { fos ->
                pdfDocument.writeTo(fos)
            }
            pdfDocument.close()
            return pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            return null
        }
    }

    private fun wrapText(text: String, charsPerLine: Int): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            if (currentLine.length + word.length + 1 > charsPerLine) {
                lines.add(currentLine)
                currentLine = word
            } else {
                currentLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }
        return lines
    }
}
