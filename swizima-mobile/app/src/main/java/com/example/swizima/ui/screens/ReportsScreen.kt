package com.example.swizima.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.swizima.data.VehicleRecordRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class DateRangeOption(val label: String) {
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    CUSTOM("Customize")
}

fun showNativeDatePicker(
    context: android.content.Context,
    currentDateStr: String,
    onDatePicked: (String) -> Unit
) {
    val cal = Calendar.getInstance()
    if (currentDateStr.isNotBlank()) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val d = sdf.parse(currentDateStr)
            if (d != null) cal.time = d
        } catch (_: Exception) {}
    }
    val y = cal.get(Calendar.YEAR)
    val m = cal.get(Calendar.MONTH)
    val day = cal.get(Calendar.DAY_OF_MONTH)

    val dialog = android.app.DatePickerDialog(
        context,
        { _, year, monthOfYear, dayOfMonth ->
            val formatted = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, monthOfYear + 1, dayOfMonth)
            onDatePicked(formatted)
        },
        y,
        m,
        day
    )
    dialog.show()
}

@Composable
fun ReportsScreen() {
    var selectedRange by remember { mutableStateOf(DateRangeOption.TODAY) }
    var customFromDate by remember { mutableStateOf("") }
    var customToDate by remember { mutableStateOf("") }
    var showCustomDateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        VehicleRecordRepository.fetchRecordsFromApi()
    }

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    val filteredRecords = remember(VehicleRecordRepository.records.size, selectedRange, customFromDate, customToDate) {
        val rawList = VehicleRecordRepository.records
        when (selectedRange) {
            DateRangeOption.TODAY -> rawList.filter { it.date == todayStr }
            DateRangeOption.THIS_WEEK -> rawList.filter { isDateInCurrentWeek(it.date) }
            DateRangeOption.THIS_MONTH -> rawList.filter { isDateInCurrentMonth(it.date) }
            DateRangeOption.CUSTOM -> rawList.filter { record ->
                val matchesFrom = customFromDate.isBlank() || record.date >= customFromDate
                val matchesTo = customToDate.isBlank() || record.date <= customToDate
                matchesFrom && matchesTo
            }
        }
    }

    val totalTxCount = filteredRecords.size
    val totalRevenue = filteredRecords.sumOf { it.amountCharged.replace(",", "").toDoubleOrNull() ?: 0.0 }
    val paidCount = filteredRecords.count { it.paymentStatus.equals("PAID", ignoreCase = true) }
    val clearedCount = filteredRecords.count { it.clearanceStatus.equals("CLEARED", ignoreCase = true) || it.clearanceStatus.equals("RELEASED", ignoreCase = true) }

    val formatter = java.text.NumberFormat.getNumberInstance(Locale.US)

    val activeRangeSubtitle = when (selectedRange) {
        DateRangeOption.TODAY -> "Showing data for Today ($todayStr)"
        DateRangeOption.THIS_WEEK -> "Showing data for This Week"
        DateRangeOption.THIS_MONTH -> "Showing data for This Month"
        DateRangeOption.CUSTOM -> if (customFromDate.isNotBlank() && customToDate.isNotBlank()) {
            "Showing data from $customFromDate to $customToDate"
        } else if (customFromDate.isNotBlank()) {
            "Showing data from $customFromDate onwards"
        } else if (customToDate.isNotBlank()) {
            "Showing data up to $customToDate"
        } else {
            "Showing data for Custom Date Range"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Reports & Analytics",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        // Date Range Filter Chips
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DateRangeOption.entries.forEach { option ->
                    FilterChip(
                        selected = selectedRange == option,
                        onClick = {
                            selectedRange = option
                            if (option == DateRangeOption.CUSTOM) {
                                showCustomDateDialog = true
                            }
                        },
                        label = {
                            Text(
                                text = if (option == DateRangeOption.CUSTOM && customFromDate.isNotBlank() && customToDate.isNotBlank()) {
                                    "$customFromDate to $customToDate"
                                } else {
                                    option.label
                                },
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Text(
                text = activeRangeSubtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 2.dp)
            )
        }

        // KPI Cards Row 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(imageVector = Icons.Default.DirectionsCar, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Total Volume", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$totalTxCount", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(imageVector = Icons.Default.AttachMoney, contentDescription = null, tint = Color(0xFF2E7D32))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Total Revenue (UGX)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32))
                    Text(formatter.format(totalRevenue), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                }
            }
        }

        // KPI Cards Row 2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF1565C0))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Paid Transactions", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1565C0))
                    Text("$paidCount / $totalTxCount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1))
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(imageVector = Icons.Default.Assessment, contentDescription = null, tint = Color(0xFFE65100))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Cleared Vehicles", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE65100))
                    Text("$clearedCount / $totalTxCount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                }
            }
        }

        // Service Breakdown Section
        Text(
            text = "Category Breakdown",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        val categories = listOf("Car park", "Car wash", "Repair", "Maintenance")
        categories.forEach { cat ->
            val catCount = filteredRecords.count { it.serviceType.contains(cat, ignoreCase = true) || cat.contains(it.serviceType, ignoreCase = true) }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(cat, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Text("$catCount records", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }
        }
    }

    if (showCustomDateDialog) {
        var tempFrom by remember { mutableStateOf(customFromDate) }
        var tempTo by remember { mutableStateOf(customToDate) }
        val context = LocalContext.current

        AlertDialog(
            onDismissRequest = { showCustomDateDialog = false },
            title = { Text("Select Custom Date Range", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Tap fields below to pick dates:", style = MaterialTheme.typography.bodyMedium)

                    OutlinedTextField(
                        value = tempFrom,
                        onValueChange = { tempFrom = it },
                        readOnly = true,
                        label = { Text("From Date") },
                        placeholder = { Text("Tap to select start date") },
                        trailingIcon = {
                            IconButton(onClick = { showNativeDatePicker(context, tempFrom) { tempFrom = it } }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Pick Start Date")
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showNativeDatePicker(context, tempFrom) { tempFrom = it } }
                    )

                    OutlinedTextField(
                        value = tempTo,
                        onValueChange = { tempTo = it },
                        readOnly = true,
                        label = { Text("To Date") },
                        placeholder = { Text("Tap to select end date") },
                        trailingIcon = {
                            IconButton(onClick = { showNativeDatePicker(context, tempTo) { tempTo = it } }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Pick End Date")
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showNativeDatePicker(context, tempTo) { tempTo = it } }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        customFromDate = tempFrom.trim()
                        customToDate = tempTo.trim()
                        showCustomDateDialog = false
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Apply Range")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun isDateInCurrentWeek(dateStr: String): Boolean {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdf.parse(dateStr) ?: return false
        val cal = Calendar.getInstance()
        val currentWeek = cal.get(Calendar.WEEK_OF_YEAR)
        val currentYear = cal.get(Calendar.YEAR)
        cal.time = date
        cal.get(Calendar.WEEK_OF_YEAR) == currentWeek && cal.get(Calendar.YEAR) == currentYear
    } catch (_: Exception) {
        false
    }
}

private fun isDateInCurrentMonth(dateStr: String): Boolean {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdf.parse(dateStr) ?: return false
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)
        cal.time = date
        cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
    } catch (_: Exception) {
        false
    }
}
