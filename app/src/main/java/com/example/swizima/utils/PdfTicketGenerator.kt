package com.example.swizima.utils

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.swizima.data.VehicleRecord
import java.io.File
import java.io.FileOutputStream

object PdfTicketGenerator {

    fun generateAndOpenPdf(context: Context, record: VehicleRecord) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(350, 620, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            isAntiAlias = true
        }

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val centerPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 11f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val boldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val rightPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }

        val rightBoldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }

        var y = 35f

        // Business Header
        canvas.drawText("Swizima Automotive", 175f, y, titlePaint)
        y += 18f
        canvas.drawText("Automotive & Parking Services", 175f, y, centerPaint)
        y += 15f
        canvas.drawText("Kampala, Uganda | Tel: +256 700 123 456", 175f, y, centerPaint)
        y += 20f

        // Divider Line
        canvas.drawLine(20f, y, 330f, y, paint)
        y += 18f

        // Ticket No
        canvas.drawText("Ticket No.: ${record.id.take(8).uppercase()}", 20f, y, boldPaint)
        y += 18f

        // Divider Line
        canvas.drawLine(20f, y, 330f, y, paint)
        y += 22f

        // Table Rows Function
        fun drawRow(label: String, value: String, isBoldValue: Boolean = false) {
            canvas.drawText(label, 20f, y, paint)
            canvas.drawText(value, 330f, y, if (isBoldValue) rightBoldPaint else rightPaint)
            y += 20f
        }

        drawRow("Service Type", record.serviceType, isBoldValue = true)
        drawRow("Number Plate", record.numberPlate, isBoldValue = true)
        drawRow("Client Name", record.clientName)
        drawRow("Nationality", record.nationality)
        if (record.idPassportNumber.isNotBlank()) {
            drawRow(record.idType, record.idPassportNumber)
        }
        drawRow("Phone", record.clientPhoneNumbers.firstOrNull() ?: "-")
        drawRow("Date", record.date)
        drawRow("Time In", record.entryTime)
        drawRow("Time Out", record.expectedExitTime.ifBlank { "Pending / Active" })
        drawRow("Key Status", if (record.isLeavingKey) "Key Left" else "Key with Client")
        if (record.assignedToPerson.isNotBlank()) {
            drawRow("Assigned To", record.assignedToPerson)
        }
        drawRow("Paid (UGX)", "UGX ${record.amountCharged.ifBlank { "0" }}", isBoldValue = true)

        y += 10f
        // Divider Line
        canvas.drawLine(20f, y, 330f, y, paint)
        y += 25f

        // Footer Message
        val footerPaint = Paint().apply {
            color = Color.BLACK
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("Thank You and Drive Safely !!!", 175f, y, footerPaint)

        pdfDocument.finishPage(page)

        // Save PDF
        val file = File(context.cacheDir, "Swizima_Ticket_${record.numberPlate}.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()

            // Open or Print Intent
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Open or Print Ticket PDF"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
