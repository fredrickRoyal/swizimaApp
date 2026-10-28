# Swizima Automotive Management App

Swizima is a native Android application built with **Kotlin** and **Jetpack Compose (Material 3)** for managing automotive services including Parking, Vehicle Washing, Repairs, Inspections/Reviews, and General Services.

---

## 📱 Features Implemented

### 1. Authentication & Navigation Flow
- **Login Screen (`LoginScreen.kt`):**
  - **Country Code Selector:** Material 3 `ExposedDropdownMenuBox` with country flags and codes (`+256` Uganda by default, `+254` Kenya, `+255` Tanzania, `+250` Rwanda, `+211` South Sudan, `+243` DR Congo, `+257` Burundi, `+249` Sudan, `+1` USA, `+44` UK).
  - **Auto-Scroll & Soft Keyboard IME Padding:** Responsive layout (`imePadding()` + `verticalScroll`) ensuring the form automatically scrolls up above the soft keyboard when typing phone numbers or emails.
  - Dual login options: Switch between **Phone Number** and **Email Address** using Material 3 `PrimaryTabRow`.
  - Real-time input validation for phone numbers and email formats.
- **OTP Verification Screen (`OtpScreen.kt`):**
  - **Auto-Scroll & Keyboard Handling:** Form automatically shifts up when entering the 6-digit OTP code (`imePadding()`).
  - 6-digit PIN input layout with auto-focus movement.
  - Contact badge showing the phone/email receiving the OTP with an edit shortcut.
  - 30-second resend countdown timer.
  - Simulated network validation loading state.
- **App Navigation (`AppNavigation.kt`):**
  - Managed via Jetpack Navigation Compose (`login` → `otp` → `dashboard` → `vehiclesList` → `serviceForm`).
  - Backstack management (`popUpTo`) preventing backward navigation to login after authentication.

### 2. Automotive Dashboard & Service Categories
- **Dashboard Screen (`DashboardScreen.kt`):**
  - Welcome card displaying logged-in user contact info.
  - Live search bar filtering service categories.
  - Interactive grid tiles for primary services:
    1. 🅿️ **Car Park** (`Parking`)
    2. 🧼 **Car Wash General** (`General washing`)
    3. 🔧 **Car Repair** (`Repair`)
    4. 📋 **Car Review and Check General** (`Review`)
  - Modal bottom sheet displaying category overview and quick actions.

### 3. Service Vehicles List & Record Management
- **Vehicles List Screen (`ServiceVehiclesListScreen.kt` & `VehicleRecordDetailBottomSheet.kt`):**
  - Category-filtered vehicle records view displaying all registered vehicles for the selected service.
  - Live search bar filtering by Number Plate, Client Name, or Phone Number.
  - **Record Details View Modal:** Tapping any vehicle card opens a comprehensive detail view sheet displaying all client, vehicle, key, and billing attributes.
  - **Edit Record:** Action button in the detail view launching the pre-filled vehicle edit form (`ServiceFormScreen.kt`) to update records in-place.
  - **Print / PDF Receipt Ticket Generator (`PdfTicketGenerator.kt`):** Generates a professional receipt ticket PDF matching thermal receipt layouts (Business header, Ticket No, Service, Plate, Client Name, ID, Phone, Entry/Exit times, Key status, Assigned staff, and Amount Paid in UGX) and opens/prints via native Android PDF intents.
  - **WhatsApp Ticket Sender (`WhatsAppSender.kt`):** Automatically formats client phone numbers with appropriate regional country codes (Uganda `+256`, Kenya `+254`, Tanzania `+255`, Rwanda `+250`, etc.) and sends formatted service tickets directly to clients via WhatsApp.
  - **Floating Action Button (FAB `+` "Add Vehicle Entry"):** Positioned at the bottom right to launch the form pre-selected to the current service category.

### 4. Vehicle Entry Form with Camera OCR, Barcode/MRZ Scanning & Auto-Capitalization
- **Vehicle Service Form (`ServiceFormScreen.kt`):**
  - **Auto-Capitalization (UPPERCASE):** Automatically converts input to UPPERCASE for `Number Plate`, `Client Name`, `ID / Passport / Licence Number`, and `Person Assigned To`.
  - **Collapsible Form Stacks (`CollapsibleCard.kt`):**
    - **Open by Default (Top):**
      - **Service Details:** Starts with **Phone Number(s)** as the first field, followed by **Number Plate** (with camera OCR scan button), **Service Type** dropdown, **Amount Charged (`UGX`)**, **Person Assigned To**, and **Leaving Key Toggle**.
    - **Closed by Default (Bottom):**
      - **Period Details:** Date, Entry Time, and Expected Exit Time.
      - **Client Details:** Client Nationality, ID Document Type, Camera ID Scan (PDF417 Barcode / MRZ / OCR), optional **Client Name** (`SURNAME, GIVEN NAME`), and optional ID/Passport Number.
      - **General Comment / Notes:** Optional remarks and notes.
    - Each section is built as an expandable/collapsible card with header title, chevron toggle icons (`ExpandLess` / `ExpandMore`), and smooth animated visibility.
  - **Nationality & ID Type Dropdowns (Separate Full-Width Rows):** Material 3 `ExposedDropdownMenuBox` for Client Nationality (`Ugandan` default) and ID Document Type (`National ID` default, `Passport`, `Driving Licence`, `Others`, `None`) positioned on separate full-width rows.
  - **Camera Document Scan BEFORE Client Name:** Photo scan button & thumbnail image preview placed directly before the Client Name field.
  - **Multi-Format Document Scanner (Spatial Bounding-Box Geometry / PDF417 / MRZ / OCR):**
    - Integrated **Google ML Kit Barcode Scanning** (`com.google.mlkit:barcode-scanning`) & **Text Recognition** (`com.google.mlkit:text-recognition`).
    - **Spatial Bounding-Box Geometry Analysis:** Analyzes the 2D bounding boxes (`Rect`) of text lines to read values printed directly below labels (e.g. reading `KASOMA` directly below label `SURNAME`, `FREDRICK` directly below label `GIVEN NAME`, and `CM90061102G2QC` directly below label `NIN`).
    - **PDF417 & 2D Barcode Reader:** Scans 2D barcodes on National IDs and Driving Licences.
    - **MRZ (Machine Readable Zone) Parser:** Parses MRZ zones on Passports & modern IDs (`P<UGAMUKASA<<JOHN<BAPTIST...`).
    - **Smart Name & ID Extraction:** Formats Client Name as `SURNAME, GIVEN NAME` in uppercase.
  - **Dynamic ID Labels:** Field label dynamically adjusts based on selected ID type (`NIN Number`, `Passport Number`, `Licence No.`, `Document Number`, or `ID / Document No.`).
  - **UGX Currency Pricing:** Amount charged input field & vehicle list cards updated with leading **`UGX`** currency indicator.
  - **Service Dropdown:** Material 3 `ExposedDropdownMenuBox` pre-selecting the active service (`Parking`, `General washing`, `Repair`, `Review`, `General Service`).
  - **Auto Pre-filled Date & Time:** Entry Date (`yyyy-MM-dd`) and Entry Time (`HH:mm`) pre-filled from system clock. Optional expected exit time field.
  - **Dynamic Multiple Phone Numbers:** Allows adding multiple client phone numbers via an "Add Another Phone Number" button.
  - **Key & Billing Management:** Switch toggle for vehicle key status (`Leaving Key?`), assigned staff name, billing amount, and general notes.
  - **Submission Confirmation:** Modal dialog summarizing all entry details before returning to the vehicle list.

### 5. Reactive State & Data Management
- **Vehicle Record Repository (`VehicleRecordRepository.kt`):**
  - Centralized in-memory reactive data store (`mutableStateListOf`) managing vehicle records across screens in real-time.

---

## 🛠️ Tech Stack & Libraries

- **Language:** Kotlin `2.2.10`
- **UI Framework:** Jetpack Compose (`Compose BOM 2026.02.01`, Material 3, Material Icons Extended)
- **Navigation:** Jetpack Navigation Compose (`2.8.8`)
- **Camera & AI/OCR:** Google ML Kit Text Recognition (`16.0.1`), Activity Result Contracts (`TakePicturePreview`)
- **Build System:** Gradle AGP `9.4.1` with Kotlin Compose Compiler Plugin

---

## 📌 Project Structure

```text
com.example.swizima
├── MainActivity.kt
├── data
│   └── VehicleRecordRepository.kt
├── navigation
│   └── AppNavigation.kt
└── ui
    ├── screens
    │   ├── DashboardScreen.kt
    │   ├── LoginScreen.kt
    │   ├── OtpScreen.kt
    │   ├── ServiceFormScreen.kt
    │   └── ServiceVehiclesListScreen.kt
    └── theme
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
```
