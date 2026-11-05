package com.example.swizima.utils

import android.content.Context
import android.content.Intent
import com.example.swizima.data.VehicleRecord

object WhatsAppSender {

    fun formatPhoneNumberForWhatsApp(phone: String, nationality: String): String {
        val cleaned = phone.replace(Regex("[^+\\d]"), "")
        val countryCode = when (nationality.lowercase()) {
            "kenyan", "kenya" -> "+254"
            "tanzanian", "tanzania" -> "+255"
            "rwandan", "rwanda" -> "+250"
            "south sudanese", "south sudan" -> "+211"
            "congolese", "congo" -> "+243"
            "burundian", "burundi" -> "+257"
            "sudanese", "sudan" -> "+249"
            else -> "+256" // Default Ugandan (+256)
        }

        return when {
            cleaned.startsWith("+") -> cleaned
            cleaned.startsWith("0") -> countryCode + cleaned.substring(1)
            cleaned.length >= 9 -> countryCode + cleaned
            else -> countryCode + cleaned
        }
    }

    fun sendTicketToWhatsApp(context: Context, record: VehicleRecord) {
        try {
            val file = PdfTicketGenerator.generatePdfFile(context, record)
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val primaryPhone = record.clientPhoneNumbers.firstOrNull { it.isNotBlank() } ?: ""
            val formattedPhone = formatPhoneNumberForWhatsApp(primaryPhone, record.nationality)
            val cleanJidPhone = formattedPhone.replace("+", "").replace(" ", "")

            val messageText = "Swizima Automotive Receipt Ticket for ${record.numberPlate}"

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, messageText)
                if (cleanJidPhone.isNotBlank()) {
                    putExtra("jid", "$cleanJidPhone@s.whatsapp.net")
                }
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to general Intent chooser for WhatsApp or WhatsApp Business
            try {
                val file = PdfTicketGenerator.generatePdfFile(context, record)
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, "Swizima Automotive Receipt Ticket for ${record.numberPlate}")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Receipt PDF via WhatsApp"))
            } catch (err: Exception) {
                err.printStackTrace()
            }
        }
    }
}
