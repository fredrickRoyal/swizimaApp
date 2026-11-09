package com.example.swizima.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.swizima.data.ApiBillingWallet
import com.example.swizima.data.ApiClientStaff
import com.example.swizima.data.ApiServiceCatalogItem
import com.example.swizima.data.ApiCreditLoad
import com.example.swizima.data.ApiCreditRequest
import com.example.swizima.data.ClientServiceRepository
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var showCreditRequestDialog by remember { mutableStateOf(false) }
    var requestedAmount by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf("MOBILE_MONEY") }
    var methodDropdownExpanded by remember { mutableStateOf(false) }
    var paymentReference by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf(ClientServiceRepository.activeClientPhone) }
    var requestNotes by remember { mutableStateOf("") }
    var requestError by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    var showTopUpHistoryDialog by remember { mutableStateOf(false) }
    var historyTab by remember { mutableStateOf("REQUESTS") }

    // Add Service Dialog States
    var showAddServiceDialog by remember { mutableStateOf(false) }
    var newServiceName by remember { mutableStateOf("") }
    var newServiceCategory by remember { mutableStateOf("Car wash") }
    var serviceCatDropdownExpanded by remember { mutableStateOf(false) }
    var newServicePrice by remember { mutableStateOf("") }
    var addServiceError by remember { mutableStateOf<String?>(null) }
    var isAddingService by remember { mutableStateOf(false) }

    // Add Staff Dialog States
    var showAddStaffDialog by remember { mutableStateOf(false) }
    var newStaffName by remember { mutableStateOf("") }
    var newStaffRole by remember { mutableStateOf("ATTENDANT") }
    var newStaffPhone by remember { mutableStateOf("") }
    var addStaffError by remember { mutableStateOf<String?>(null) }
    var isAddingStaff by remember { mutableStateOf(false) }

    var walletData by remember { mutableStateOf(ApiBillingWallet()) }
    var serviceCatalogList by remember { mutableStateOf<List<ApiServiceCatalogItem>>(emptyList()) }
    var staffList by remember { mutableStateOf<List<ApiClientStaff>>(emptyList()) }
    var creditRequestsList by remember { mutableStateOf<List<ApiCreditRequest>>(emptyList()) }
    var creditLoadsList by remember { mutableStateOf<List<ApiCreditLoad>>(emptyList()) }

    fun refreshBillingData() {
        scope.launch {
            val clientId = ClientServiceRepository.activeClientId
            walletData = ClientServiceRepository.fetchBillingWallet(clientId)
            serviceCatalogList = ClientServiceRepository.fetchServiceCatalogs(clientId)
            staffList = ClientServiceRepository.fetchClientStaff(clientId)
            creditRequestsList = ClientServiceRepository.fetchCreditRequests(clientId)
            creditLoadsList = ClientServiceRepository.fetchCreditLoads(clientId)
        }
    }

    LaunchedEffect(Unit) {
        refreshBillingData()
    }

    val formatter = NumberFormat.getNumberInstance(Locale.US)
    val quickAmounts = listOf(10000, 25000, 50000, 100000, 250000)
    val paymentMethods = listOf(
        "MOBILE_MONEY" to "Mobile Money",
        "CASH" to "Cash",
        "CARD" to "Card",
        "BANK" to "Bank Transfer"
    )
    val serviceCategories = listOf("Car park", "Car wash", "Repair", "Maintenance")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Billing & Wallet",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        // Web Parity Billing KPI Cards Grid (Row 1)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Credit Balance Card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Credit balance", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    Text(
                        text = "${formatter.format(walletData.balance.toLong())} ${walletData.currency}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Text(
                        text = "Alert at ${formatter.format(walletData.lowBalanceThreshold.toLong())} ${walletData.currency}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            // 2. Rate Per Transaction Card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Rate per tx", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Text(
                        text = "${walletData.percentageRate.toInt()}%",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE8F5E9)
                    ) {
                        Text(
                            text = "• Active",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Web Parity Billing KPI Cards Grid (Row 2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 3. Total Loaded Card (Clickable to view Requests & Direct Top-ups)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { showTopUpHistoryDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Total loaded", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32))
                    }

                    Text(
                        text = "${formatter.format(walletData.totalCredited.toLong())} ${walletData.currency}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1B5E20)
                    )

                    Text(
                        text = "All-time top-ups (Tap to view)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            // 4. Total Charged Card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Total charged", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE65100))
                    }

                    Text(
                        text = "${formatter.format(walletData.totalCharged.toLong())} ${walletData.currency}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFE65100)
                    )

                    Text(
                        text = "Net of refunds",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFE65100)
                    )
                }
            }
        }

        // Action: Request Credit Top-Up
        Button(
            onClick = {
                requestError = null
                requestedAmount = ""
                paymentReference = ""
                contactPhone = ClientServiceRepository.activeClientPhone
                requestNotes = ""
                showCreditRequestDialog = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Request Credit Top-Up", fontWeight = FontWeight.Bold)
        }

        // Support & Contact Persons Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.HeadsetMic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Customer Support & Contact",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "For account assistance, credit top-ups, or system inquiries, reach out to our dedicated support contact:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+256700408387"))
                            ctx.startActivity(intent)
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = Color(0xFF2E7D32))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Phone Support", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("+256 700 408 387", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable {
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:kfredrick35@gmail.com"))
                            ctx.startActivity(intent)
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = Color(0xFF1565C0))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Email Support", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("kfredrick35@gmail.com", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Service Catalog Header with Add Option
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Service Catalog (${serviceCatalogList.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            TextButton(
                onClick = {
                    newServiceName = ""
                    newServicePrice = ""
                    addServiceError = null
                    showAddServiceDialog = true
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Service", fontWeight = FontWeight.Bold)
            }
        }

        if (serviceCatalogList.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Text("No service catalog items registered yet.", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            serviceCatalogList.forEach { catalogItem ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.ListAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(catalogItem.serviceName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text("Category: ${catalogItem.category}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFE8F5E9)) {
                            Text("${formatter.format(catalogItem.price.toLong())} ${catalogItem.currency}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }
            }
        }

        // Client Staff Header with Add Option
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Client Staff Members (${staffList.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            TextButton(
                onClick = {
                    newStaffName = ""
                    newStaffPhone = ""
                    addStaffError = null
                    showAddStaffDialog = true
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Staff", fontWeight = FontWeight.Bold)
            }
        }

        if (staffList.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Text("No staff members registered for this client.", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            staffList.forEach { staffMember ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(staffMember.fullName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text("Role: ${staffMember.position ?: "Staff Member"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        if (!staffMember.phoneNumber.isNullOrBlank()) {
                            Text(staffMember.phoneNumber, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }

    // Add Service Catalog Item Dialog
    if (showAddServiceDialog) {
        AlertDialog(
            onDismissRequest = { if (!isAddingService) showAddServiceDialog = false },
            title = { Text("Add Service Catalog Item", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newServiceName,
                        onValueChange = { newServiceName = it },
                        label = { Text("Service Name *") },
                        placeholder = { Text("e.g. Standard Car Wash") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    ExposedDropdownMenuBox(
                        expanded = serviceCatDropdownExpanded,
                        onExpandedChange = { serviceCatDropdownExpanded = !serviceCatDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = newServiceCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Service Category *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = serviceCatDropdownExpanded) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        )

                        ExposedDropdownMenu(
                            expanded = serviceCatDropdownExpanded,
                            onDismissRequest = { serviceCatDropdownExpanded = false }
                        ) {
                            serviceCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat, fontWeight = FontWeight.SemiBold) },
                                    onClick = {
                                        newServiceCategory = cat
                                        serviceCatDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newServicePrice,
                        onValueChange = { newServicePrice = it },
                        label = { Text("Price (UGX) *") },
                        placeholder = { Text("e.g. 15000") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (addServiceError != null) {
                        Text(addServiceError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanAmt = newServicePrice.replace(",", "").toDoubleOrNull() ?: 0.0
                        if (newServiceName.isBlank()) {
                            addServiceError = "Please enter a service name"
                            return@Button
                        }
                        if (cleanAmt <= 0) {
                            addServiceError = "Please enter a valid price"
                            return@Button
                        }
                        isAddingService = true
                        scope.launch {
                            val success = ClientServiceRepository.addServiceCatalog(
                                clientId = ClientServiceRepository.activeClientId,
                                serviceName = newServiceName,
                                category = newServiceCategory,
                                price = cleanAmt
                            )
                            isAddingService = false
                            if (success) {
                                showAddServiceDialog = false
                                refreshBillingData()
                                android.widget.Toast.makeText(ctx, "Service catalog item added!", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                addServiceError = "Failed to add service catalog item"
                            }
                        }
                    },
                    enabled = !isAddingService,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isAddingService) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Add Service")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddServiceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Staff Member Dialog
    if (showAddStaffDialog) {
        AlertDialog(
            onDismissRequest = { if (!isAddingStaff) showAddStaffDialog = false },
            title = { Text("Add Staff Member", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newStaffName,
                        onValueChange = { newStaffName = it },
                        label = { Text("Full Name *") },
                        placeholder = { Text("e.g. JOSEPH AGABA") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newStaffRole,
                        onValueChange = { newStaffRole = it },
                        label = { Text("Role / Position") },
                        placeholder = { Text("e.g. ATTENDANT or MANAGER") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newStaffPhone,
                        onValueChange = { newStaffPhone = it },
                        label = { Text("Phone Number") },
                        placeholder = { Text("e.g. +256 700 123 456") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (addStaffError != null) {
                        Text(addStaffError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newStaffName.isBlank()) {
                            addStaffError = "Please enter staff full name"
                            return@Button
                        }
                        isAddingStaff = true
                        scope.launch {
                            val success = ClientServiceRepository.addClientStaff(
                                clientId = ClientServiceRepository.activeClientId,
                                fullName = newStaffName,
                                position = newStaffRole.ifBlank { "ATTENDANT" },
                                phoneNumber = newStaffPhone.ifBlank { null }
                            )
                            isAddingStaff = false
                            if (success) {
                                showAddStaffDialog = false
                                refreshBillingData()
                                android.widget.Toast.makeText(ctx, "Staff member added successfully!", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                addStaffError = "Failed to add staff member"
                            }
                        }
                    },
                    enabled = !isAddingStaff,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isAddingStaff) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Add Staff")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStaffDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Request Credit Top-Up Modal (Mirrors Web App Form)
    if (showCreditRequestDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showCreditRequestDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Request credit top-up", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Text(ClientServiceRepository.activeClientName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { showCreditRequestDialog = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Submit your credit top-up request below. A Swizima administrator will review your request, confirm the actual credit amount, and top up your wallet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Quick Amount Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickAmounts.forEach { amt ->
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable {
                                    requestedAmount = formatter.format(amt.toLong())
                                    requestError = null
                                }
                            ) {
                                Text(
                                    text = "+ ${formatter.format(amt.toLong())} UGX",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Amount Requested
                    OutlinedTextField(
                        value = requestedAmount,
                        onValueChange = {
                            val clean = it.replace(",", "").replace(".", "")
                            requestedAmount = if (clean.isNotBlank() && clean.all { c -> c.isDigit() }) {
                                formatter.format(clean.toLong())
                            } else {
                                clean
                            }
                            requestError = null
                        },
                        label = { Text("Amount requested *") },
                        placeholder = { Text("50,000") },
                        trailingIcon = { Text("UGX", fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 12.dp)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Payment Method Dropdown
                    ExposedDropdownMenuBox(
                        expanded = methodDropdownExpanded,
                        onExpandedChange = { methodDropdownExpanded = !methodDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = paymentMethods.find { it.first == selectedPaymentMethod }?.second ?: "Mobile Money",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Payment method *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodDropdownExpanded) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        )

                        ExposedDropdownMenu(
                            expanded = methodDropdownExpanded,
                            onDismissRequest = { methodDropdownExpanded = false }
                        ) {
                            paymentMethods.forEach { (code, label) ->
                                DropdownMenuItem(
                                    text = { Text(label, fontWeight = FontWeight.SemiBold) },
                                    onClick = {
                                        selectedPaymentMethod = code
                                        methodDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Payment / MoMo reference
                    OutlinedTextField(
                        value = paymentReference,
                        onValueChange = { paymentReference = it },
                        label = { Text("Payment / MoMo reference") },
                        placeholder = { Text("Receipt or MoMo txn ID") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Contact phone
                    OutlinedTextField(
                        value = contactPhone,
                        onValueChange = { contactPhone = it },
                        label = { Text("Contact phone") },
                        placeholder = { Text("+256 700 123 456") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Notes
                    OutlinedTextField(
                        value = requestNotes,
                        onValueChange = { requestNotes = it },
                        label = { Text("Notes") },
                        placeholder = { Text("Optional details for the billing administrator") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (requestError != null) {
                        Text(requestError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val cleanAmt = requestedAmount.replace(",", "").toDoubleOrNull() ?: 0.0
                            if (cleanAmt <= 0) {
                                requestError = "Enter a valid amount greater than zero"
                                return@Button
                            }
                            isSubmitting = true
                            scope.launch {
                                val success = ClientServiceRepository.submitCreditRequest(
                                    clientId = ClientServiceRepository.activeClientId,
                                    amount = cleanAmt,
                                    paymentMethod = selectedPaymentMethod,
                                    paymentReference = paymentReference.ifBlank { null },
                                    phone = contactPhone.ifBlank { null },
                                    notes = requestNotes.ifBlank { null }
                                )
                                isSubmitting = false
                                if (success) {
                                    showCreditRequestDialog = false
                                    refreshBillingData()
                                    android.widget.Toast.makeText(ctx, "Credit request submitted successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    requestError = ClientServiceRepository.apiErrorMessage ?: "Failed to submit credit request. Please try again."
                                }
                            }
                        },
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Submit request", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val cleanAmt = requestedAmount.replace(",", "").toDoubleOrNull() ?: 0.0
                            if (cleanAmt <= 0) {
                                requestError = "Enter a valid amount greater than zero"
                                return@OutlinedButton
                            }
                            isSubmitting = true
                            scope.launch {
                                ClientServiceRepository.submitCreditRequest(
                                    clientId = ClientServiceRepository.activeClientId,
                                    amount = cleanAmt,
                                    paymentMethod = selectedPaymentMethod,
                                    paymentReference = paymentReference.ifBlank { null },
                                    phone = contactPhone.ifBlank { null },
                                    notes = requestNotes.ifBlank { null }
                                )
                                isSubmitting = false
                                showCreditRequestDialog = false
                                refreshBillingData()

                                val msg = buildString {
                                    append("Hello Swizima Admin, I would like to request a credit top-up:\n")
                                    append("• Business: ${ClientServiceRepository.activeClientName}\n")
                                    append("• Amount: UGX ${formatter.format(cleanAmt.toLong())}\n")
                                    append("• Method: ${selectedPaymentMethod.replace("_", " ")}\n")
                                    if (paymentReference.isNotBlank()) append("• Reference: ${paymentReference.trim()}\n")
                                    if (contactPhone.isNotBlank()) append("• Contact Phone: ${contactPhone.trim()}\n")
                                    if (requestNotes.isNotBlank()) append("• Notes: ${requestNotes.trim()}\n")
                                }
                                try {
                                    val encoded = URLEncoder.encode(msg, "UTF-8")
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=+256700408387&text=$encoded"))
                                    ctx.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                        },
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF25D366))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Submit & notify via WhatsApp", fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = { showCreditRequestDialog = false },
                        enabled = !isSubmitting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel")
                    }
                }
            },
            dismissButton = {}
        )
    }

    // Top-Up History & Credit Requests Modal
    if (showTopUpHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showTopUpHistoryDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("All-Time Top-Ups & Requests", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = { showTopUpHistoryDialog = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = historyTab == "REQUESTS",
                            onClick = { historyTab = "REQUESTS" },
                            label = { Text("Credit Requests (${creditRequestsList.size})", fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(12.dp)
                        )
                        FilterChip(
                            selected = historyTab == "DIRECT",
                            onClick = { historyTab = "DIRECT" },
                            label = { Text("Direct Top-Ups (${creditLoadsList.size})", fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    if (historyTab == "REQUESTS") {
                        if (creditRequestsList.isEmpty()) {
                            Text("No credit requests found.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            creditRequestsList.forEach { req ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("UGX ${formatter.format(req.requestedAmount.toLong())}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            Text("Method: ${req.paymentMethod.replace("_", " ")} | ${req.createdAt}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            if (!req.paymentReference.isNullOrBlank()) {
                                                Text("Ref: ${req.paymentReference}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }

                                        val (statusBg, statusColor) = when (req.status.uppercase()) {
                                            "APPROVED" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
                                            "REJECTED" -> Color(0xFFFFEBEE) to Color(0xFFC62828)
                                            else -> Color(0xFFFFF3E0) to Color(0xFFE65100)
                                        }

                                        Surface(shape = RoundedCornerShape(8.dp), color = statusBg) {
                                            Text(req.status, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = statusColor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        if (creditLoadsList.isEmpty()) {
                            Text("No direct top-ups found.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            creditLoadsList.forEach { load ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("UGX ${formatter.format(load.amount.toLong())}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            Text("Method: ${load.paymentMethod.replace("_", " ")} | ${load.loadedAt}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            if (!load.paymentReference.isNullOrBlank()) {
                                                Text("Ref: ${load.paymentReference}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }

                                        Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFE8F5E9)) {
                                            Text("Loaded", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showTopUpHistoryDialog = false }, shape = RoundedCornerShape(12.dp)) {
                    Text("Close")
                }
            }
        )
    }
}
