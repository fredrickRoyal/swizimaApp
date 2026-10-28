package com.example.swizima.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VpnKey
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.swizima.ui.theme.SwizimaTheme
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val availableServices = listOf(
    "Parking",
    "General washing",
    "Repair",
    "Review",
    "General Service"
)

val availableNationalities = listOf(
    "Ugandan",
    "Kenyan",
    "Tanzanian",
    "Rwandan",
    "Burundian",
    "South Sudanese",
    "Sudanese",
    "Congolese",
    "Other"
)

val availableIdTypes = listOf(
    "National ID",
    "Passport",
    "Driving Licence",
    "Others",
    "None"
)

private fun getCurrentFormattedDate(): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return formatter.format(Date())
}

private fun getCurrentFormattedTime(): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date())
}

fun extractNumberPlateFromText(rawText: String): String {
    if (rawText.isBlank()) return ""
    val plateRegex = Regex("[A-Z]{2,3}\\s*-?\\s*\\d{3,4}\\s*-?\\s*[A-Z]?", RegexOption.IGNORE_CASE)
    val match = plateRegex.find(rawText)
    if (match != null) {
        return match.value.replace("-", " ").replace("\\s+".toRegex(), " ").trim().uppercase()
    }
    val lineMatch = rawText.lines()
        .map { it.trim().uppercase() }
        .firstOrNull { line -> (line.length in 4..12) && line.any { it.isDigit() } && line.any { it.isLetter() } }
    return lineMatch ?: rawText.trim().uppercase().take(12)
}

data class ExtractedIdentity(
    val fullName: String,
    val idOrPassportNumber: String
)

fun extractIdentityFromMrz(rawText: String): ExtractedIdentity? {
    val lines = rawText.lines().map { it.trim().uppercase() }.filter { it.contains("<") }
    val mrzLines = lines.filter { it.length in 28..45 }
    if (mrzLines.isEmpty()) return null

    var surname = ""
    var givenNames = ""
    var docNumber = ""

    val nameLine = mrzLines.firstOrNull { it.startsWith("P<") || it.startsWith("I<") || it.startsWith("A<") || it.startsWith("C<") }
    if (nameLine != null && nameLine.contains("<<")) {
        val nameSection = nameLine.substring(5).trimEnd('<')
        val parts = nameSection.split("<<")
        if (parts.isNotEmpty()) {
            surname = parts[0].replace("<", " ").trim()
        }
        if (parts.size > 1) {
            givenNames = parts[1].replace("<", " ").trim()
        }
    }

    val docLine = mrzLines.getOrNull(1)
    if (docLine != null && docLine.length >= 9) {
        val possibleDoc = docLine.substring(0, 9).replace("<", "").trim()
        if (possibleDoc.length >= 6) {
            docNumber = possibleDoc
        }
    }

    if (surname.isNotBlank() || givenNames.isNotBlank() || docNumber.isNotBlank()) {
        val formattedName = when {
            surname.isNotBlank() && givenNames.isNotBlank() -> "$surname, $givenNames"
            surname.isNotBlank() -> surname
            else -> givenNames
        }
        return ExtractedIdentity(fullName = formattedName.uppercase(), idOrPassportNumber = docNumber.uppercase())
    }

    return null
}

fun extractIdentityFromDocumentText(rawText: String, idType: String): ExtractedIdentity {
    if (rawText.isBlank()) return ExtractedIdentity("", "")

    val mrzResult = extractIdentityFromMrz(rawText)
    if (mrzResult != null && (mrzResult.fullName.isNotBlank() || mrzResult.idOrPassportNumber.isNotBlank())) {
        return mrzResult
    }

    val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }

    var surname = ""
    var givenNames = ""
    var documentId = ""

    for (i in lines.indices) {
        val line = lines[i]
        val upperLine = line.uppercase()

        if (surname.isEmpty()) {
            when {
                upperLine.contains("SURNAME") || upperLine.contains("FAMILY NAME") || upperLine.contains("NOM") || upperLine.contains("S/NAME") -> {
                    val value = line.substringAfter(":", "").substringAfter("SURNAME", "").substringAfter("NOM", "").trim()
                    if (value.length > 1 && !value.startsWith("SURNAME", ignoreCase = true)) {
                        surname = value
                    } else if (i + 1 < lines.size) {
                        surname = lines[i + 1].trim()
                    }
                }
            }
        }

        if (givenNames.isEmpty()) {
            when {
                upperLine.contains("GIVEN") || upperLine.contains("NAMES") || upperLine.contains("PRENOM") || upperLine.contains("OTHER NAME") -> {
                    val value = line.substringAfter(":", "").substringAfter("NAMES", "").substringAfter("GIVEN", "").trim()
                    if (value.length > 1 && !value.startsWith("GIVEN", ignoreCase = true)) {
                        givenNames = value
                    } else if (i + 1 < lines.size) {
                        givenNames = lines[i + 1].trim()
                    }
                }
            }
        }

        if (documentId.isEmpty()) {
            when {
                idType == "National ID" && (upperLine.contains("NIN") || upperLine.contains("ID NO") || upperLine.contains("CARD NO")) -> {
                    val value = line.substringAfter(":", "").substringAfter("NIN", "").trim()
                    if (value.length >= 6) documentId = value
                    else if (i + 1 < lines.size && lines[i + 1].length >= 6) documentId = lines[i + 1].trim()
                }
                idType == "Driving Licence" && (upperLine.contains("LICENCE") || upperLine.contains("PERMIT") || upperLine.contains("DL NO") || upperLine.contains("NO.")) -> {
                    val value = line.substringAfter(":", "").substringAfter("NO", "").trim()
                    if (value.length >= 5) documentId = value
                    else if (i + 1 < lines.size && lines[i + 1].length >= 5) documentId = lines[i + 1].trim()
                }
                idType == "Passport" && (upperLine.contains("PASSPORT") || upperLine.contains("DOC NO") || upperLine.contains("NO.")) -> {
                    val value = line.substringAfter(":", "").substringAfter("NO", "").trim()
                    if (value.length >= 6) documentId = value
                    else if (i + 1 < lines.size && lines[i + 1].length >= 6) documentId = lines[i + 1].trim()
                }
            }
        }
    }

    if (documentId.isEmpty()) {
        when (idType) {
            "National ID" -> {
                val ugandaNinRegex = Regex("(CM|CF)[0-9A-Z]{12}", RegexOption.IGNORE_CASE)
                val genericIdRegex = Regex("\\b[0-9A-Z]{7,16}\\b")
                documentId = ugandaNinRegex.find(rawText)?.value ?: genericIdRegex.find(rawText)?.value ?: ""
            }
            "Passport" -> {
                val passportRegex = Regex("[A-Z]{1,2}[0-9]{6,8}", RegexOption.IGNORE_CASE)
                documentId = passportRegex.find(rawText)?.value ?: ""
            }
            "Driving Licence" -> {
                val licenceRegex = Regex("\\b[0-9A-Z]{6,12}\\b")
                documentId = licenceRegex.find(rawText)?.value ?: ""
            }
        }
    }

    val formattedName = when {
        surname.isNotBlank() && givenNames.isNotBlank() -> "$surname, $givenNames".uppercase().trim()
        surname.isNotBlank() -> surname.uppercase().trim()
        givenNames.isNotBlank() -> givenNames.uppercase().trim()
        else -> {
            val fallbackLine = lines.firstOrNull { l ->
                l.length in 5..35 &&
                        !l.contains("REPUBLIC", ignoreCase = true) &&
                        !l.contains("NATIONAL", ignoreCase = true) &&
                        !l.contains("PASSPORT", ignoreCase = true) &&
                        l.all { char -> char.isLetter() || char == ' ' || char == ',' || char == '\'' || char == '-' } &&
                        l.contains(" ")
            } ?: ""
            if (fallbackLine.contains(",")) {
                fallbackLine.uppercase().trim()
            } else if (fallbackLine.contains(" ")) {
                val parts = fallbackLine.split(" ", limit = 2)
                "${parts[0]}, ${parts.getOrElse(1) { "" }}".uppercase().trim()
            } else {
                fallbackLine.uppercase().trim()
            }
        }
    }

    return ExtractedIdentity(
        fullName = formattedName,
        idOrPassportNumber = documentId.uppercase().trim()
    )
}

fun extractIdentityFromVisionText(visionText: Text, idType: String): ExtractedIdentity {
    val mrzResult = extractIdentityFromMrz(visionText.text)
    if (mrzResult != null && (mrzResult.fullName.isNotBlank() || mrzResult.idOrPassportNumber.isNotBlank())) {
        return mrzResult
    }

    val allLines = visionText.textBlocks.flatMap { it.lines }
    if (allLines.isEmpty()) {
        return extractIdentityFromDocumentText(visionText.text, idType)
    }

    var surname = ""
    var givenNames = ""
    var documentId = ""

    val surnameLabelLine = allLines.firstOrNull {
        val t = it.text.uppercase()
        t == "SURNAME" || t.contains("SURNAME") || t.contains("FAMILY NAME") || t.contains("NOM")
    }
    val givenNameLabelLine = allLines.firstOrNull {
        val t = it.text.uppercase()
        t == "GIVEN NAME" || t.contains("GIVEN NAME") || t.contains("PRENOM") || t.contains("FIRST NAME")
    }
    val ninLabelLine = allLines.firstOrNull {
        val t = it.text.uppercase()
        t == "NIN" || t == "CARD NO" || t.contains("NIN") || t.contains("ID NO") || t.contains("PASSPORT NO") || t.contains("PERMIT NO")
    }

    fun findLineBelow(labelLine: Text.Line?): String? {
        if (labelLine == null) return null
        val labelBox = labelLine.boundingBox ?: return null

        return allLines
            .filter { line ->
                val box = line.boundingBox
                if (box != null && line != labelLine) {
                    val isBelow = box.top >= (labelBox.bottom - 15) && box.top <= (labelBox.bottom + 140)
                    val horizontalOverlap = box.right >= (labelBox.left - 80) && box.left <= (labelBox.right + 200)
                    val lineTextUpper = line.text.uppercase()
                    isBelow && horizontalOverlap &&
                            !lineTextUpper.contains("SURNAME") &&
                            !lineTextUpper.contains("GIVEN") &&
                            !lineTextUpper.contains("NATIONALITY") &&
                            !lineTextUpper.contains("SEX") &&
                            !lineTextUpper.contains("DATE OF") &&
                            !lineTextUpper.contains("EXPIRY") &&
                            !lineTextUpper.contains("REPUBLIC") &&
                            !lineTextUpper.contains("NATIONAL ID") &&
                            !lineTextUpper.contains("NIN")
                } else false
            }
            .minByOrNull { it.boundingBox?.top ?: Int.MAX_VALUE }
            ?.text?.trim()
    }

    surname = findLineBelow(surnameLabelLine) ?: ""
    givenNames = findLineBelow(givenNameLabelLine) ?: ""
    documentId = findLineBelow(ninLabelLine) ?: ""

    if (documentId.contains("NIN", ignoreCase = true)) {
        documentId = documentId.substringAfter("NIN").trim()
    }

    if (documentId.isBlank() || documentId.length < 6) {
        val ugandaNinRegex = Regex("(CM|CF)[0-9A-Z]{12}", RegexOption.IGNORE_CASE)
        val passportRegex = Regex("[A-Z]{1,2}[0-9]{6,8}", RegexOption.IGNORE_CASE)
        val idNumberRegex = Regex("\\b[0-9]{7,10}\\b")

        val rawFullText = visionText.text
        documentId = ugandaNinRegex.find(rawFullText)?.value
            ?: passportRegex.find(rawFullText)?.value
            ?: idNumberRegex.find(rawFullText)?.value ?: ""
    }

    if (surname.isBlank() || givenNames.isBlank()) {
        val fallback = extractIdentityFromDocumentText(visionText.text, idType)
        if (surname.isBlank()) surname = fallback.fullName.substringBefore(",", "").trim()
        if (givenNames.isBlank()) givenNames = fallback.fullName.substringAfter(",", "").trim()
        if (documentId.isBlank()) documentId = fallback.idOrPassportNumber
    }

    val formattedName = when {
        surname.isNotBlank() && givenNames.isNotBlank() -> "$surname, $givenNames"
        surname.isNotBlank() -> surname
        else -> givenNames
    }.uppercase().trim()

    return ExtractedIdentity(
        fullName = formattedName,
        idOrPassportNumber = documentId.uppercase().trim()
    )
}

@Composable
fun CollapsibleCard(
    title: String,
    initiallyExpanded: Boolean = true,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceFormScreen(
    initialService: String = "Parking",
    recordIdToEdit: String? = null,
    onBack: () -> Unit,
    onSubmitSuccess: () -> Unit
) {
    val existingRecord = remember(recordIdToEdit) {
        if (!recordIdToEdit.isNullOrBlank()) {
            com.example.swizima.data.VehicleRecordRepository.getRecordById(recordIdToEdit)
        } else null
    }

    // Form States
    val clientPhoneNumbers = remember {
        val phones = existingRecord?.clientPhoneNumbers ?: listOf("")
        mutableStateListOf(*(if (phones.isEmpty()) listOf("") else phones).toTypedArray())
    }

    var numberPlate by remember { mutableStateOf(existingRecord?.numberPlate ?: "") }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isOcrProcessing by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            isOcrProcessing = true
            val image = InputImage.fromBitmap(bitmap, 0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    isOcrProcessing = false
                    val extracted = extractNumberPlateFromText(visionText.text)
                    if (extracted.isNotBlank()) {
                        numberPlate = extracted.uppercase()
                    }
                }
                .addOnFailureListener {
                    isOcrProcessing = false
                }
        }
    }

    val defaultService = existingRecord?.serviceType ?: when {
        initialService.contains("wash", ignoreCase = true) -> "General washing"
        initialService.contains("repair", ignoreCase = true) -> "Repair"
        initialService.contains("review", ignoreCase = true) || initialService.contains("check", ignoreCase = true) -> "Review"
        else -> "Parking"
    }

    var selectedService by remember { mutableStateOf(defaultService) }
    var serviceDropdownExpanded by remember { mutableStateOf(false) }

    var dateInput by remember { mutableStateOf(existingRecord?.date ?: getCurrentFormattedDate()) }
    var entryTimeInput by remember { mutableStateOf(existingRecord?.entryTime ?: getCurrentFormattedTime()) }
    var expectedExitTimeInput by remember { mutableStateOf(existingRecord?.expectedExitTime ?: "") }

    // Client Details
    var clientName by remember { mutableStateOf(existingRecord?.clientName ?: "") }
    var selectedNationality by remember { mutableStateOf(existingRecord?.nationality ?: "Ugandan") }
    var nationalityDropdownExpanded by remember { mutableStateOf(false) }

    var selectedIdType by remember { mutableStateOf(existingRecord?.idType ?: "National ID") }
    var idTypeDropdownExpanded by remember { mutableStateOf(false) }

    var idPassportNumber by remember { mutableStateOf(existingRecord?.idPassportNumber ?: "") }
    var idDocBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isIdOcrProcessing by remember { mutableStateOf(false) }

    val idDocCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            idDocBitmap = bitmap
            isIdOcrProcessing = true
            val image = InputImage.fromBitmap(bitmap, 0)

            val barcodeScanner = com.google.mlkit.vision.barcode.BarcodeScanning.getClient()
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodeList ->
                    var barcodeFound = false
                    for (barcode in barcodeList) {
                        val rawValue = barcode.rawValue
                        if (!rawValue.isNullOrBlank()) {
                            barcodeFound = true
                            val identity = extractIdentityFromDocumentText(rawValue, selectedIdType)
                            if (identity.fullName.isNotBlank()) clientName = identity.fullName.uppercase()
                            if (identity.idOrPassportNumber.isNotBlank()) idPassportNumber = identity.idOrPassportNumber.uppercase()
                            break
                        }
                    }

                    if (!barcodeFound) {
                        val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                        textRecognizer.process(image)
                            .addOnSuccessListener { visionText ->
                                isIdOcrProcessing = false
                                val identity = extractIdentityFromVisionText(visionText, selectedIdType)
                                if (identity.fullName.isNotBlank()) clientName = identity.fullName.uppercase()
                                if (identity.idOrPassportNumber.isNotBlank()) idPassportNumber = identity.idOrPassportNumber.uppercase()
                            }
                            .addOnFailureListener {
                                isIdOcrProcessing = false
                            }
                    } else {
                        isIdOcrProcessing = false
                    }
                }
                .addOnFailureListener {
                    val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    textRecognizer.process(image)
                        .addOnSuccessListener { visionText ->
                            isIdOcrProcessing = false
                            val identity = extractIdentityFromVisionText(visionText, selectedIdType)
                            if (identity.fullName.isNotBlank()) clientName = identity.fullName.uppercase()
                            if (identity.idOrPassportNumber.isNotBlank()) idPassportNumber = identity.idOrPassportNumber.uppercase()
                        }
                        .addOnFailureListener {
                            isIdOcrProcessing = false
                        }
                }
        }
    }

    var isLeavingKey by remember { mutableStateOf(existingRecord?.isLeavingKey ?: false) }
    var assignedToPerson by remember { mutableStateOf(existingRecord?.assignedToPerson ?: "") }
    var generalComment by remember { mutableStateOf(existingRecord?.generalComment ?: "") }
    var amountCharged by remember { mutableStateOf(existingRecord?.amountCharged ?: "") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    if (clientPhoneNumbers.isEmpty()) {
        clientPhoneNumbers.add("")
    }

    val dynamicIdLabel = when (selectedIdType) {
        "National ID" -> "NIN Number (Optional)"
        "Driving Licence" -> "Licence No. (Optional)"
        "Passport" -> "Passport Number (Optional)"
        "Others" -> "Document Number (Optional)"
        "None" -> "ID / Document No. (Optional)"
        else -> "ID / Document No. (Optional)"
    }

    fun validateAndSubmit() {
        val plate = numberPlate.trim().uppercase()
        val name = clientName.trim().uppercase()
        val assigned = assignedToPerson.trim().uppercase()
        val validPhones = clientPhoneNumbers.filter { it.isNotBlank() }

        if (validPhones.isEmpty()) {
            errorMessage = "Please enter at least one client phone number"
            return
        }
        if (plate.isEmpty()) {
            errorMessage = "Please enter or scan the vehicle number plate"
            return
        }

        if (existingRecord != null) {
            val updatedRecord = existingRecord.copy(
                numberPlate = plate,
                serviceType = selectedService,
                date = dateInput.ifBlank { getCurrentFormattedDate() },
                entryTime = entryTimeInput.ifBlank { getCurrentFormattedTime() },
                expectedExitTime = expectedExitTimeInput.trim(),
                clientName = name,
                nationality = selectedNationality,
                idType = selectedIdType,
                idPassportNumber = idPassportNumber.trim().uppercase(),
                clientPhoneNumbers = validPhones,
                isLeavingKey = isLeavingKey,
                assignedToPerson = assigned,
                generalComment = generalComment.trim(),
                amountCharged = amountCharged.trim()
            )
            com.example.swizima.data.VehicleRecordRepository.updateRecord(updatedRecord)
        } else {
            val newRecord = com.example.swizima.data.VehicleRecord(
                numberPlate = plate,
                serviceType = selectedService,
                date = dateInput.ifBlank { getCurrentFormattedDate() },
                entryTime = entryTimeInput.ifBlank { getCurrentFormattedTime() },
                expectedExitTime = expectedExitTimeInput.trim(),
                clientName = name,
                nationality = selectedNationality,
                idType = selectedIdType,
                idPassportNumber = idPassportNumber.trim().uppercase(),
                clientPhoneNumbers = validPhones,
                isLeavingKey = isLeavingKey,
                assignedToPerson = assigned,
                generalComment = generalComment.trim(),
                amountCharged = amountCharged.trim(),
                status = "Active"
            )
            com.example.swizima.data.VehicleRecordRepository.addRecord(newRecord)
        }

        errorMessage = null
        showSuccessDialog = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (existingRecord != null) "Edit Vehicle Record" else "Vehicle Entry Form",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Group 1: Service Details (Open by Default)
            CollapsibleCard(title = "Service Details", initiallyExpanded = true) {
                // 1. Phone Number(s) - FIRST FIELD!
                Text(
                    text = "Phone Number(s) *",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                clientPhoneNumbers.forEachIndexed { index, phone ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { newValue ->
                                clientPhoneNumbers[index] = newValue
                                errorMessage = null
                            },
                            label = { Text("Phone ${index + 1}") },
                            placeholder = { Text("+256 700 123 456") },
                            leadingIcon = {
                                Icon(Icons.Default.Phone, contentDescription = null)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )

                        if (clientPhoneNumbers.size > 1) {
                            IconButton(
                                onClick = { clientPhoneNumbers.removeAt(index) },
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove Phone",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = { clientPhoneNumbers.add("") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Another Phone Number")
                }

                // 2. Number Plate
                OutlinedTextField(
                    value = numberPlate,
                    onValueChange = {
                        numberPlate = it.uppercase()
                        errorMessage = null
                    },
                    label = { Text("Number Plate (CAPITAL) *") },
                    placeholder = { Text("e.g. KBA 123X") },
                    leadingIcon = {
                        Icon(Icons.Default.DirectionsCar, contentDescription = null)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Next
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = { cameraLauncher.launch(null) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan Number Plate with Camera (OCR)")
                }

                if (isOcrProcessing) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Extracting number plate text...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }

                capturedBitmap?.let { bitmap ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Captured Plate",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Plate Photo Captured", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                Text(text = "Number plate extracted via OCR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { capturedBitmap = null }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove Photo", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                // 3. Service Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = serviceDropdownExpanded,
                    onExpandedChange = { serviceDropdownExpanded = !serviceDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedService,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Service Type *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = serviceDropdownExpanded) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().menuAnchor(androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )

                    ExposedDropdownMenu(
                        expanded = serviceDropdownExpanded,
                        onDismissRequest = { serviceDropdownExpanded = false }
                    ) {
                        availableServices.forEach { service ->
                            DropdownMenuItem(
                                text = { Text(service) },
                                onClick = {
                                    selectedService = service
                                    serviceDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Amount Charged (UGX)
                OutlinedTextField(
                    value = amountCharged,
                    onValueChange = { amountCharged = it },
                    label = { Text("Amount Charged (UGX)") },
                    placeholder = { Text("e.g. 50,000") },
                    leadingIcon = {
                        Text(
                            text = "UGX",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = assignedToPerson,
                    onValueChange = { assignedToPerson = it.uppercase() },
                    label = { Text("Person Assigned To (CAPITAL)") },
                    placeholder = { Text("e.g. MARK ATTENDANT") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Next),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.VpnKey, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = "Leaving Key?", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text(text = if (isLeavingKey) "Key left with attendant" else "Client keeps the key", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Switch(
                        checked = isLeavingKey,
                        onCheckedChange = { isLeavingKey = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // Group 2: Period Details (Closed by Default)
            CollapsibleCard(title = "Period Details", initiallyExpanded = false) {
                // Date & Entry Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = dateInput,
                        onValueChange = { dateInput = it },
                        label = { Text("Date") },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = entryTimeInput,
                        onValueChange = { entryTimeInput = it },
                        label = { Text("Entry Time") },
                        leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Expected Exit Time
                OutlinedTextField(
                    value = expectedExitTimeInput,
                    onValueChange = { expectedExitTimeInput = it },
                    label = { Text("Expected Exit Time (Optional)") },
                    placeholder = { Text("e.g. 17:00") },
                    leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Group 3: Client Details (Closed by Default)
            CollapsibleCard(title = "Client Details", initiallyExpanded = false) {
                // Nationality Dropdown
                ExposedDropdownMenuBox(
                    expanded = nationalityDropdownExpanded,
                    onExpandedChange = { nationalityDropdownExpanded = !nationalityDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedNationality,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Client Nationality") },
                        leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = nationalityDropdownExpanded) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().menuAnchor(androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )

                    ExposedDropdownMenu(
                        expanded = nationalityDropdownExpanded,
                        onDismissRequest = { nationalityDropdownExpanded = false }
                    ) {
                        availableNationalities.forEach { nat ->
                            DropdownMenuItem(
                                text = { Text(nat) },
                                onClick = {
                                    selectedNationality = nat
                                    nationalityDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // ID Document Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = idTypeDropdownExpanded,
                    onExpandedChange = { idTypeDropdownExpanded = !idTypeDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedIdType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("ID Document Type") },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = idTypeDropdownExpanded) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().menuAnchor(androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )

                    ExposedDropdownMenu(
                        expanded = idTypeDropdownExpanded,
                        onDismissRequest = { idTypeDropdownExpanded = false }
                    ) {
                        availableIdTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    selectedIdType = type
                                    idTypeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Photo Scan BEFORE Client Name
                OutlinedButton(
                    onClick = { idDocCameraLauncher.launch(null) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan $selectedNationality $selectedIdType (OCR / Barcode / MRZ)")
                }

                if (isIdOcrProcessing) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Scanning MRZ, Barcode & Text OCR...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }

                idDocBitmap?.let { bitmap ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Captured ID",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "$selectedNationality $selectedIdType Captured", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                Text(text = "Name & ID extracted automatically", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { idDocBitmap = null }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                // Client Name (Optional)
                OutlinedTextField(
                    value = clientName,
                    onValueChange = { clientName = it.uppercase() },
                    label = { Text("Client Name (SURNAME, GIVEN NAME) (Optional)") },
                    placeholder = { Text("e.g. KASOMA, FREDRICK") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Next),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // ID / Passport / Licence Number (Optional)
                OutlinedTextField(
                    value = idPassportNumber,
                    onValueChange = { idPassportNumber = it.uppercase() },
                    label = { Text(dynamicIdLabel) },
                    placeholder = { Text("e.g. CM90061102G2QC") },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Next),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Group 4: General Comments / Notes (Closed by Default)
            CollapsibleCard(title = "General Comment / Notes", initiallyExpanded = false) {
                OutlinedTextField(
                    value = generalComment,
                    onValueChange = { generalComment = it },
                    label = { Text("General Comment / Notes (Optional)") },
                    placeholder = { Text("Enter any special instructions or remarks...") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = null) },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            AnimatedVisibility(visible = errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            // Submit Button
            Button(
                onClick = { validateAndSubmit() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = if (existingRecord != null) "Update Vehicle Record" else "Submit Vehicle Entry", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Success Confirmation Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            icon = {
                Box(
                    modifier = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                }
            },
            title = {
                Text(
                    text = if (existingRecord != null) "Record Updated Successfully!" else "Vehicle Entry Recorded!",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("• Phone(s): ${clientPhoneNumbers.filter { it.isNotBlank() }.joinToString(", ")}")
                    Text("• Number Plate: $numberPlate")
                    Text("• Service: $selectedService")
                    if (clientName.isNotBlank()) {
                        Text("• Client Name: $clientName")
                    }
                    Text("• Nationality: $selectedNationality")
                    Text("• Document: $selectedIdType ${if (idPassportNumber.isNotBlank()) "($idPassportNumber)" else ""}")
                    Text("• Entry Time: $dateInput $entryTimeInput")
                    if (expectedExitTimeInput.isNotBlank()) {
                        Text("• Expected Exit: $expectedExitTimeInput")
                    }
                    Text("• Key Left: ${if (isLeavingKey) "Yes" else "No"}")
                    if (assignedToPerson.isNotBlank()) {
                        Text("• Assigned To: $assignedToPerson")
                    }
                    if (amountCharged.isNotBlank()) {
                        Text("• Amount Charged: UGX $amountCharged")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onSubmitSuccess()
                    }
                ) {
                    Text("Done & Back to List")
                }
            }
        )
    }
}
