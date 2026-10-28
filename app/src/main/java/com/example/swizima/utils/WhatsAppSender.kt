package com.example.swizima.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.swizima.data.VehicleRecord
import java.net.URLEncoder

object WhatsAppSender {

    fun formatPhoneNumberForWhatsApp(phone: String, nationality: String): String {
        val cleaned = phone.replace(Regex("[^+\\d]"), "")
        val countryCode = when (nationality.lowercase()) {
            "kenyan", "kenya" -> "+254"
            "tanzanian", "tanzania" -> "+255"
            "rwandan", "rwanda" -> "+250"
            "south Sudanese", "south sudan" -> "+211"
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
        val primaryPhone = record.clientPhoneNumbers.firstOrNull { it.isNotBlank() } ?: ""
        if (primaryPhone.isBlank()) return

        val formattedPhone = formatPhoneNumberForWhatsApp(primaryPhone, record.nationality)

        val message = buildString {
            append("*SWIZIMA AUTOMOTIVE SERVICES*\n")
            append("🚗 *Vehicle Service Ticket*\n\n")
            append("• *Ticket No:* ${record.id.take(8).uppercase()}\n")
            append("• *Service:* ${record.serviceType}\n")
            append("• *Number Plate:* ${record.numberPlate}\n")
            if (record.clientName.isNotBlank()) {
                append("• *Client:* ${record.clientName}\n")
            }
            append("• *Date:* ${record.date}\n")
            append("• *Time In:* ${record.entryTime}\n")
            if (record.expectedExitTime.isNotBlank()) {
                append("• *Expected Exit:* ${record.expectedExitTime}\n")
            }
            append("• *Key Status:* ${if (record.isLeavingKey) "Key Left with Attendant" else "Key with Client"}\n")
            if (record.assignedToPerson.isNotBlank()) {
                append("• *Assigned To:* ${record.assignedToPerson}\n")
            }
            if (record.amountCharged.isNotBlank()) {
                append("• *Amount Charged:* UGX ${record.amountCharged}\n")
            }
            append("\n_Thank You and Drive Safely !!!_")
        }

        try {
            val encodedMessage = URLEncoder.encode(message, "UTF-8")
            val url = "https://api.whatsapp.com/send?phone=$formattedPhone&text=$encodedMessage"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
