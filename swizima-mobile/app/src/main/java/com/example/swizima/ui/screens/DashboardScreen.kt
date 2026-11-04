package com.example.swizima.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.LocalCarWash
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.swizima.ui.theme.SwizimaTheme

data class DashboardTaskTile(
    val id: String,
    val title: String,
    val description: String,
    val tag: String,
    val icon: ImageVector,
    val containerColor: Color,
    val iconColor: Color,
    val activeTasksCount: Int = 0
)

fun mapTileToServiceType(tileTitle: String): String {
    return when {
        tileTitle.contains("park", ignoreCase = true) -> "Parking"
        tileTitle.contains("wash", ignoreCase = true) -> "General washing"
        tileTitle.contains("repair", ignoreCase = true) -> "Repair"
        tileTitle.contains("review", ignoreCase = true) || tileTitle.contains("check", ignoreCase = true) -> "Review"
        else -> "General Service"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    userContact: String,
    onLogout: () -> Unit,
    onNavigateToVehiclesList: (serviceType: String) -> Unit = {}
) {
    var orgName by remember { mutableStateOf("Swizima Motors & Parking Ltd") }
    var orgAddress by remember { mutableStateOf("Plot 12, Lugogo Bypass, Kampala") }
    var orgContact by remember { mutableStateOf("Contact: +256 414 123 456") }

    LaunchedEffect(Unit) {
        val clients = com.example.swizima.data.ClientServiceRepository.fetchClients()
        if (clients.isNotEmpty()) {
            val client = clients.first()
            orgName = client.clientName
            orgAddress = client.address.ifBlank { "Kampala, Uganda" }
            orgContact = "Code: ${client.clientCode} ${if (client.phone.isNotBlank()) "| Phone: ${client.phone}" else ""}"
        }
    }

    val taskTiles = remember {
        listOf(
            DashboardTaskTile(
                id = "car_park",
                title = "Car Park",
                description = "Parking space booking, valet services & spot management",
                tag = "Parking",
                icon = Icons.Default.LocalParking,
                containerColor = Color(0xFFE8F5E9),
                iconColor = Color(0xFF2E7D32),
                activeTasksCount = 2
            ),
            DashboardTaskTile(
                id = "car_wash",
                title = "Car Wash General",
                description = "Exterior washing, interior vacuum, foam detail & polish",
                tag = "Wash & Detail",
                icon = Icons.Default.LocalCarWash,
                containerColor = Color(0xFFE1F5FE),
                iconColor = Color(0xFF0288D1),
                activeTasksCount = 1
            ),
            DashboardTaskTile(
                id = "car_repair",
                title = "Car Repair",
                description = "Engine diagnostic, brakes, electrical & mechanical repair",
                tag = "Maintenance",
                icon = Icons.Default.Build,
                containerColor = Color(0xFFFFF3E0),
                iconColor = Color(0xFFE65100),
                activeTasksCount = 3
            ),
            DashboardTaskTile(
                id = "car_review_check",
                title = "Car Review and Check General",
                description = "Comprehensive multi-point vehicle inspection & health checks",
                tag = "Inspection",
                icon = Icons.Default.FactCheck,
                containerColor = Color(0xFFF3E5F5),
                iconColor = Color(0xFF7B1FA2),
                activeTasksCount = 0
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Swizima Dashboard",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* Handle Notifications */ }) {
                        BadgedBox(
                            badge = { Badge { Text("2") } }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications"
                            )
                        }
                    }
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // User Greeting Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "User Avatar",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = orgName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "📍 $orgAddress",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "📞 $orgContact | User: $userContact",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "Active",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // --- Metrics Dashboard ---
            val records = com.example.swizima.data.VehicleRecordRepository.records
            val todayDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            val todaysRecords = records.filter { it.date == todayDate }
            val totalTxCount = todaysRecords.size
            val totalCharged = todaysRecords.sumOf { it.amountCharged.replace(",", "").toDoubleOrNull() ?: 0.0 }
            val totalCollected = todaysRecords.filter { it.paymentStatus.equals("PAID", ignoreCase = true) }.sumOf { it.amountCharged.replace(",", "").toDoubleOrNull() ?: 0.0 }
            val totalOutstanding = totalCharged - totalCollected

            val formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale.US)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Total Transactions
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Today's Txns", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(totalTxCount.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
                // Collected
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Collected (UGX)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(formatter.format(totalCollected), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                    }
                }
                // Outstanding
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Outstanding", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE65100))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(formatter.format(totalOutstanding), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Task Tiles Grid (Sleek Redesigned Cards)
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(taskTiles, key = { it.id }) { tile ->
                    TaskTileCard(
                        tile = tile,
                        onClick = {
                            val serviceType = mapTileToServiceType(tile.title)
                            onNavigateToVehiclesList(serviceType)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TaskTileCard(
    tile: DashboardTaskTile,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(155.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Icon Badge & Category Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(tile.containerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = tile.icon,
                        contentDescription = tile.title,
                        tint = tile.iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = tile.containerColor
                ) {
                    Text(
                        text = tile.tag,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = tile.iconColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Bottom Section: Title & Description
            Column {
                Text(
                    text = tile.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = tile.description,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    SwizimaTheme {
        DashboardScreen(
            userContact = "+1 (555) 019-2834",
            onLogout = {}
        )
    }
}
