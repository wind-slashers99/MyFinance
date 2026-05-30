package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import com.example.receiver.FinancialWidgetProvider
import com.example.ui.theme.*
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialApp(
    viewModel: FinancialViewModel,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    isAppLockEnabled: Boolean,
    onToggleAppLock: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf("DASHBOARD") } // "DASHBOARD", "TRANSACTIONS", "BUDGETS", "CHARTS", "AI_CHAT"
    var showAddDialog by remember { mutableStateOf(false) }

    val safeguardPrefs = remember(context) { context.getSharedPreferences("safeguard_prefs", Context.MODE_PRIVATE) }
    var savedPin by remember { mutableStateOf(safeguardPrefs.getString("app_lock_pin", "1234") ?: "1234") }
    var showSecurityMenuDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importDatabaseFromJson(
                context = context,
                jsonUri = uri,
                onSuccess = {
                    Toast.makeText(context, "Data Cadangan berhasil diimpor!", Toast.LENGTH_LONG).show()
                    val intent = Intent(context, FinancialWidgetProvider::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    }
                    val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                        ComponentName(context, FinancialWidgetProvider::class.java)
                    )
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                    context.sendBroadcast(intent)
                },
                onError = { err ->
                    Toast.makeText(context, "Gagal mengimpor cadangan: $err", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val rawAlerts by viewModel.allAlerts.collectAsStateWithLifecycle()
    val unreadAlertsCount = remember(rawAlerts) { rawAlerts.count { !it.isRead } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = "MyFinance",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            )
                            Text(
                                text = "Pencatatan Keuangan Pintar",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Sync Button Indicator
                    val syncContentDescription = when (syncState) {
                        is SyncState.Syncing -> "Sedang menyelaraskan data..."
                        else -> "Data tersimpan di cloud otomatis"
                    }
                    IconButton(
                        onClick = {
                            viewModel.triggerManualSync()
                            Toast.makeText(context, "Sinkronisasi cloud dipicu!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("sync_toolbar_button")
                    ) {
                        when (syncState) {
                            is SyncState.Syncing -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            is SyncState.Success -> {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = syncContentDescription,
                                    tint = ToskaSecondary
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Default.Cloud,
                                    contentDescription = syncContentDescription,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Secure app lock toggle button
                    IconButton(
                        onClick = {
                            if (isAppLockEnabled) {
                                showSecurityMenuDialog = true
                            } else {
                                onToggleAppLock(true)
                            }
                        },
                        modifier = Modifier.testTag("app_lock_safeguard_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Proteksi Keamanan",
                            tint = if (isAppLockEnabled) {
                                if (isDarkTheme) Color(0xFF26A69A) else Color(0xFF00796B)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            }
                        )
                    }

                    // Dark Mode Toggle
                    IconButton(
                        onClick = onThemeToggle,
                        modifier = Modifier.testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Ganti Tema",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets.navigationBars,
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
            ) {
                NavigationBarItem(
                    selected = currentTab == "DASHBOARD",
                    onClick = { currentTab = "DASHBOARD" },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    label = { Text("Mulai", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_dashboard")
                )
                NavigationBarItem(
                    selected = currentTab == "TRANSACTIONS",
                    onClick = { currentTab = "TRANSACTIONS" },
                    icon = { Icon(Icons.Default.FormatListBulleted, contentDescription = null) },
                    label = { Text("Transaksi", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_transactions")
                )
                NavigationBarItem(
                    selected = currentTab == "BUDGETS",
                    onClick = { currentTab = "BUDGETS" },
                    icon = {
                        BadgedBox(badge = {
                            if (unreadAlertsCount > 0) {
                                Badge { Text(unreadAlertsCount.toString()) }
                            }
                        }) {
                            Icon(Icons.Default.Notifications, contentDescription = null)
                        }
                    },
                    label = { Text("Anggaran", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_budgets")
                )
                NavigationBarItem(
                    selected = currentTab == "CHARTS",
                    onClick = { currentTab = "CHARTS" },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                    label = { Text("Laporan", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_reports")
                )
                NavigationBarItem(
                    selected = currentTab == "AI_CHAT",
                    onClick = { currentTab = "AI_CHAT" },
                    icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null) },
                    label = { Text("Asisten AI", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_ai")
                )
            }
        },
        floatingActionButton = {
            if (currentTab == "DASHBOARD" || currentTab == "TRANSACTIONS") {
                ExtendedFloatingActionButton(
                    text = { Text("Tambah", fontWeight = FontWeight.Bold, color = if (isSystemInDarkTheme()) Color(0xFF0B1214) else Color.White) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null, tint = if (isSystemInDarkTheme()) Color(0xFF0B1214) else Color.White) },
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("fab_add_transaction")
                )
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimationContentLayout(
                currentTab = currentTab,
                viewModel = viewModel,
                onNavigateToAllTransactions = { currentTab = "TRANSACTIONS" },
                onNavigateToBudgets = { currentTab = "BUDGETS" }
            )

            if (showAddDialog) {
                AddTransactionDialog(
                    onDismiss = { showAddDialog = false },
                    onConfirm = { title, amt, type, cat, note ->
                        viewModel.addTransaction(
                            title = title,
                            amount = amt,
                            type = type,
                            category = cat,
                            timestamp = System.currentTimeMillis(),
                            description = note
                        )
                        showAddDialog = false
                        Toast.makeText(context, "Transaksi berhasil disimpan!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    if (showSecurityMenuDialog) {
        AlertDialog(
            onDismissRequest = { showSecurityMenuDialog = false },
            title = {
                Text(
                    text = "Proteksi MyFinance",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                )
            },
            text = {
                Text(
                    text = "Atur keamanan aplikasi kamu. Gunakan biometrik sidik jari, wajah, atau ubah PIN Keamanan kamu (Default: 1234).",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            showSecurityMenuDialog = false
                            showChangePinDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Ubah PIN Keamanan", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            showSecurityMenuDialog = false
                            viewModel.exportDatabaseToJson(
                                context = context,
                                onSuccess = { backupFile ->
                                    val fileUri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        backupFile
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/json"
                                        putExtra(Intent.EXTRA_STREAM, fileUri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Bagikan Cadangan MyFinance"))
                                    Toast.makeText(context, "Data berhasil diekspor!", Toast.LENGTH_SHORT).show()
                                },
                                onError = { err ->
                                    Toast.makeText(context, "Gagal mengekspor data: $err", Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Ekspor Cadangan (JSON)", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            showSecurityMenuDialog = false
                            try {
                                importFileLauncher.launch("*/*")
                            } catch (e: Exception) {
                                Toast.makeText(context, "Membuka pemilih file gagal: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Impor Cadangan (JSON)", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            showSecurityMenuDialog = false
                            onToggleAppLock(false)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Nonaktifkan Kunci Aplikasi", fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = { showSecurityMenuDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Batal")
                    }
                }
            },
            dismissButton = {}
        )
    }

    if (showChangePinDialog) {
        var step by remember { mutableStateOf(1) } // 1: Old PIN, 2: New PIN
        var enteredOldPin by remember { mutableStateOf("") }
        var enteredNewPin by remember { mutableStateOf("") }
        var enteredConfirmPin by remember { mutableStateOf("") }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            title = {
                Text(
                    text = if (step == 1) "Verifikasi PIN Lama" else "Masukkan PIN Baru",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (step == 1) {
                        Text(
                            text = "Untuk mengubah PIN, masukkan PIN 4-digit lama kamu terlebih dahulu.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        OutlinedTextField(
                            value = enteredOldPin,
                            onValueChange = { if (it.length <= 4) enteredOldPin = it },
                            label = { Text("PIN Lama (Default: 1234)") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = "Masukkan PIN 4-digit baru kamu dan konfirmasikan.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        OutlinedTextField(
                            value = enteredNewPin,
                            onValueChange = { if (it.length <= 4) enteredNewPin = it },
                            label = { Text("PIN Baru") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = enteredConfirmPin,
                            onValueChange = { if (it.length <= 4) enteredConfirmPin = it },
                            label = { Text("Konfirmasi PIN Baru") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (errorMsg != null) {
                        Text(
                            text = errorMsg ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (step == 1) {
                            if (enteredOldPin == savedPin) {
                                step = 2
                                errorMsg = null
                            } else {
                                errorMsg = "PIN Lama Salah!"
                            }
                        } else {
                            if (enteredNewPin.length != 4) {
                                errorMsg = "PIN baru harus 4 digit!"
                            } else if (enteredNewPin != enteredConfirmPin) {
                                errorMsg = "Konfirmasi PIN baru tidak cocok!"
                            } else {
                                safeguardPrefs.edit().putString("app_lock_pin", enteredNewPin).apply()
                                savedPin = enteredNewPin
                                Toast.makeText(context, "PIN Keamanan Berhasil Diubah!", Toast.LENGTH_SHORT).show()
                                showChangePinDialog = false
                            }
                        }
                    }
                ) {
                    Text(if (step == 1) "Lanjut" else "Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun AnimationContentLayout(
    currentTab: String,
    viewModel: FinancialViewModel,
    onNavigateToAllTransactions: () -> Unit,
    onNavigateToBudgets: () -> Unit
) {
    when (currentTab) {
        "DASHBOARD" -> DashboardScreen(viewModel, onNavigateToAllTransactions, onNavigateToBudgets)
        "TRANSACTIONS" -> TransactionsScreen(viewModel)
        "BUDGETS" -> BudgetsHubScreen(viewModel)
        "CHARTS" -> LiveReportsScreen(viewModel)
        "AI_CHAT" -> AiAdvisorScreen(viewModel)
    }
}

private val cachedLocaleID = Locale("in", "ID")
private val cachedFormatRupiah = NumberFormat.getCurrencyInstance(cachedLocaleID).apply {
    maximumFractionDigits = 0
}

// --- Dynamic Helper Formatter ---
fun formatAmount(amount: Double): String {
    synchronized(cachedFormatRupiah) {
        return cachedFormatRupiah.format(amount)
    }
}

// --- SCREEN 1: DASHBOARD ---
@Composable
fun DashboardScreen(
    viewModel: FinancialViewModel,
    onSeeAllClicked: () -> Unit,
    onBudgetsClicked: () -> Unit
) {
    val context = LocalContext.current
    val summary by viewModel.currentMonthSummary.collectAsStateWithLifecycle()
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    
    var activeHubTab by remember { mutableStateOf("RINGKASAN") }

    val budgetList by viewModel.allBudgets.collectAsStateWithLifecycle()
    val alignList by viewModel.allAlerts.collectAsStateWithLifecycle()
    
    var activeOverlay by remember { mutableStateOf<String?>(null) } // "NABUNG", "SKOR", "TAGIHAN", "REKENING"
    val isDark = isSystemInDarkTheme()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Welcoming Greeting & Cloud Sync status Info banner
        item {
            GreetingSection(syncState)
        }

        // Active Month selector spinner bar
        item {
            MonthSelectionBar(selectedMonth, onMonthChosen = { viewModel.setMonthFilter(it) })
        }

        // Wealth / Financial Stats Card
        item {
            FinancialSummaryCard(income = summary.first, expense = summary.second, balance = summary.third)
        }

        // Modern Quick Actions Row (Services & Features Shortcuts)
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Layanan Keuangan Cerdas",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val actionItems = listOf(
                        Triple("NABUNG", "Target\nNabung", Icons.Default.Star),
                        Triple("SKOR", "Skor &\nSimulasi", Icons.Default.Analytics),
                        Triple("TAGIHAN", "Tagihan\n& Hutang", Icons.Default.CalendarMonth),
                        Triple("REKENING", "Kelola\nRekening", Icons.Default.AccountBalance)
                    )

                    actionItems.forEach { (type, label, icon) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { activeOverlay = type }
                                .padding(vertical = 8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = if (isDark) Color(0xFF132224) else Color(0xFFE8F6F4),
                                border = BorderStroke(
                                    width = 1.dp,
                                    brush = Brush.linearGradient(
                                        colors = if (isDark) {
                                            listOf(Color(0xFF4FD8EB).copy(alpha = 0.3f), Color(0xFF1D2B2E))
                                        } else {
                                            listOf(Color(0xFF2E9B8E).copy(alpha = 0.4f), Color(0xFFD0E6E3))
                                        }
                                    )
                                ),
                                modifier = Modifier.size(56.dp),
                                shadowElevation = if (isDark) 0.dp else 1.dp
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (isDark) Color(0xFF4FD8EB) else Color(0xFF007A87),
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 13.sp,
                                    letterSpacing = 0.2.sp
                                ),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Warning alerts teaser banner if active warnings are present
        val pendingWarning = alignList.firstOrNull { !it.isRead && it.type != "SISTEM" }
        if (pendingWarning != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("warning_banner_teaser"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF3E2723) else Color(0xFFFFF3E0)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isDark) StatPeringatanDark else StatPeringatan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = pendingWarning.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = pendingWarning.message,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        TextButton(onClick = onBudgetsClicked) {
                            Text("Detail", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        // Dashboard Unified Payment Accounts Card
        item {
            DashboardPaymentAccountsList(viewModel)
        }

        // Header for Recent Lists
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transaksi Terbaru Bulan Ini",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onSeeAllClicked) {
                    Text("Lihat Semua", color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Display up to 5 items of recent txs
        if (transactions.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Inbox,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Belum ada catatan di bulan ini",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(transactions.take(5), key = { it.id }) { tx ->
                TransactionRowItem(tx, onDeleteClicked = { viewModel.deleteTransaction(it) })
            }
        }
    }

    // Modern Modal Dialog Overlay Wrapper
    if (activeOverlay != null) {
        Dialog(onDismissRequest = { activeOverlay = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF0F1719) else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, if (isDark) SleekBorderDark else SleekBorderLight)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = when (activeOverlay) {
                                    "NABUNG" -> Icons.Default.Star
                                    "SKOR" -> Icons.Default.Analytics
                                    "TAGIHAN" -> Icons.Default.CalendarMonth
                                    "REKENING" -> Icons.Default.AccountBalance
                                    else -> Icons.Default.Settings
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = when (activeOverlay) {
                                    "NABUNG" -> "Target Nabung Kamu"
                                    "SKOR" -> "Skor & Simulasi Keuangan"
                                    "TAGIHAN" -> "Tagihan & Hutang Bulan Ini"
                                    "REKENING" -> "Kelola Rekening & Dompet"
                                    else -> ""
                                },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                            )
                        }
                        IconButton(onClick = { activeOverlay = null }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup")
                        }
                    }

                    Divider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        thickness = 1.dp
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        when (activeOverlay) {
                            "NABUNG" -> SavingsGoalHubSection(viewModel)
                            "SKOR" -> FinancialHealthScoreSection(viewModel)
                            "TAGIHAN" -> BillsRecurringSection(viewModel)
                            "REKENING" -> PaymentAccountsHubSection(viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GreetingSection(syncState: SyncState) {
    val isDark = isSystemInDarkTheme()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1D2B2E) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
        ),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, if (isDark) SleekBorderDark else SleekBorderLight)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Halo, Yuki Yasa!",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Pantau kondisi arus kas kamu secara berkelanjutan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(30.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (syncState is SyncState.Syncing) Color.Yellow else ToskaSecondary
                        )
                )
                Text(
                    text = when (syncState) {
                        is SyncState.Syncing -> "Metode Cloud: Menyinkronkan..."
                        is SyncState.Success -> "Cloud Sinkron Aktif (Data Aman)"
                        is SyncState.Error -> "Koneksi Cloud Sedang Ditunda"
                        else -> "Keuangan Lokal Terenkripsi"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun MonthSelectionBar(currentMonth: String, onMonthChosen: (String) -> Unit) {
    val monthsList = remember {
        val list = mutableListOf<String>()
        val calendar = Calendar.getInstance()
        val format = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        // Generate current month and past 5 months
        for (i in 0..5) {
            list.add(format.format(calendar.time))
            calendar.add(Calendar.MONTH, -1)
        }
        list
    }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(monthsList) { mStr ->
            val isSelected = mStr == currentMonth
            val monthLabel = remember(mStr) {
                try {
                    val date = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(mStr)
                    SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(date!!)
                } catch (e: Exception) {
                    mStr
                }
            }

            FilterChip(
                selected = isSelected,
                onClick = { onMonthChosen(mStr) },
                label = { Text(monthLabel, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("month_chip_$mStr")
            )
        }
    }
}

@Composable
fun FinancialSummaryCard(income: Double, expense: Double, balance: Double) {
    val isDark = isSystemInDarkTheme()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("summary_card"),
        shape = RoundedCornerShape(32.dp),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = if (isDark) {
                    listOf(Color(0xFF4FD8EB).copy(alpha = 0.4f), Color(0xFF2E9B8E).copy(alpha = 0.1f))
                } else {
                    listOf(Color(0xFF2E9B8E).copy(alpha = 0.3f), Color(0xFF007A87).copy(alpha = 0.1f))
                }
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .drawBehind {
                    // Premium ambient glow orb underneath the text for maximum depth
                    val glowColor = if (isDark) Color(0xFF1F6B62).copy(alpha = 0.25f) else Color(0xFFE8F1F0).copy(alpha = 0.7f)
                    drawCircle(
                        color = glowColor,
                        radius = this.size.width * 0.45f,
                        center = Offset(this.size.width * 0.8f, this.size.height * 0.2f)
                    )
                }
                .background(
                    if (isDark) {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF142426),
                                Color(0xFF0D1618)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFFFFFF),
                                Color(0xFFEAF4F2)
                            )
                        )
                    }
                )
                .padding(24.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SALDO BERSIH BULAN INI",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        ),
                        color = if (isDark) Color(0xFF98F2E1).copy(alpha = 0.7f) else Color(0xFF1F6B62)
                    )
                    
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color(0xFF1D2B2E).copy(alpha = 0.6f) else Color(0xFFD3EBE7),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "Aktif",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isDark) Color(0xFF4FD8EB) else Color(0xFF007A87),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = formatAmount(balance),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 32.sp,
                        letterSpacing = (-0.5).sp,
                        color = if (balance >= 0) {
                            if (isSystemInDarkTheme()) Color(0xFF4FD8EB) else Color(0xFF007A87)
                        } else {
                            if (isSystemInDarkTheme()) Color(0xFFE57373) else Color(0xFFC62828)
                        }
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Pemasukan Container
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isDark) Color(0xFF122C24).copy(alpha = 0.4f) else Color(0xFFEAF5EF)
                            )
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = if (isSystemInDarkTheme()) StatPemasukanDark else StatPemasukan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Pemasukan",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = formatAmount(income),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSystemInDarkTheme()) StatPemasukanDark else StatPemasukan,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Pengeluaran Container
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isDark) Color(0xFF2C1E20).copy(alpha = 0.4f) else Color(0xFFFBECEE)
                            )
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.TrendingDown,
                                    contentDescription = null,
                                    tint = if (isSystemInDarkTheme()) StatPengeluaranDark else StatPengeluaran,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Pengeluaran",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = formatAmount(expense),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSystemInDarkTheme()) StatPengeluaranDark else StatPengeluaran,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

private val cachedTxDateFormat = SimpleDateFormat("dd MMM • HH:mm", Locale("id", "ID"))

fun formatTransactionDate(timestamp: Long): String {
    synchronized(cachedTxDateFormat) {
        return cachedTxDateFormat.format(Date(timestamp))
    }
}

@Composable
fun TransactionRowItem(tx: TransactionEntity, onDeleteClicked: (TransactionEntity) -> Unit) {
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val isDark = isSystemInDarkTheme()
    val isIncome = tx.type == "PEMASUKAN"
    val accentColor = if (isIncome) {
        if (isDark) StatPemasukanDark else StatPemasukan
    } else {
        if (isDark) StatPengeluaranDark else StatPengeluaran
    }

    val iconVector = remember(tx.category) {
        when (tx.category) {
            "Makanan" -> Icons.Default.Restaurant
            "Belanja" -> Icons.Default.ShoppingBag
            "Transportasi" -> Icons.Default.DirectionsCar
            "Hiburan" -> Icons.Default.ConfirmationNumber
            "Kos / Tagihan" -> Icons.Default.HomeWork
            "Investasi" -> Icons.Default.ShowChart
            "Gaji" -> Icons.Default.Payments
            "Usaha" -> Icons.Default.Storefront
            else -> Icons.Default.Category
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDeleteConfirmDialog = true }
            .testTag("transaction_item_${tx.id}"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isSystemInDarkTheme()) SleekBorderDark else SleekBorderLight),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category visual Icon tag
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Details info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = tx.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f))
                    )
                    Text(
                        text = formatTransactionDate(tx.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Value Tag
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (isIncome) "+" else "-") + formatAmount(tx.amount),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = accentColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (tx.syncStatus == "SYNCED") {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = "Tersinkron cloud",
                            tint = ToskaSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text("Cloud", style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp), color = ToskaSecondary)
                    } else {
                        Icon(
                            imageVector = Icons.Default.HourglassEmpty,
                            contentDescription = "Pending cloud sync",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                            modifier = Modifier.size(12.dp)
                        )
                        Text("Antre", style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f))
                    }
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Hapus Transaksi?") },
            text = { Text("Apakah Kak Yuki yakin ingin menghapus catatan \"${tx.title}\"? Tindakan ini tidak dapat dibatalkan ya.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteClicked(tx)
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

// --- SCREEN 2: ALL TRANSACTIONS LIST ---
@Composable
fun TransactionsScreen(viewModel: FinancialViewModel) {
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val typeFilter by viewModel.selectedTypeFilter.collectAsStateWithLifecycle()
    val currentCatFilter by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()

    val categoriesList = remember {
        listOf("SEMUA", "Makanan", "Belanja", "Transportasi", "Hiburan", "Kos / Tagihan", "Investasi", "Gaji", "Usaha", "Lainnya")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Daftar Riwayat Kas",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Month Selection inside trans list
        MonthSelectionBar(selectedMonth, onMonthChosen = { viewModel.setMonthFilter(it) })
        Spacer(modifier = Modifier.height(12.dp))

        // Filter Type Buttons (Semua, Pemasukan, Pengeluaran) Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("SEMUA", "PEMASUKAN", "PENGELUARAN").forEach { mode ->
                Button(
                    onClick = { viewModel.setTypeFilter(mode) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("filter_type_$mode"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (typeFilter == mode) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer.copy(0.4f)
                        },
                        contentColor = if (typeFilter == mode) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        }
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = when (mode) {
                            "SEMUA" -> "Semua"
                            "PEMASUKAN" -> "Pemasukan"
                            "PENGELUARAN" -> "Pengeluaran"
                            else -> mode
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Large Category Filter Horizontal Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categoriesList) { cat ->
                val isSelected = cat == currentCatFilter
                ElevatedFilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setCategoryFilter(cat) },
                    label = { Text(cat, fontSize = 11.sp) },
                    colors = FilterChipDefaults.elevatedFilterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("filter_category_$cat")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display results
        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FilterAltOff,
                        contentDescription = null,
                        modifier = Modifier.size(54.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Tidak ada transaksi yang cocok dengan filter",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(transactions, key = { it.id }) { tx ->
                    TransactionRowItem(tx, onDeleteClicked = { viewModel.deleteTransaction(it) })
                }
            }
        }
    }
}

// --- SCREEN 3: BUDGETS HUB & ALERTS ---
@Composable
fun BudgetsHubScreen(viewModel: FinancialViewModel) {
    val budgets by viewModel.allBudgets.collectAsStateWithLifecycle()
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val alerts by viewModel.allAlerts.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()

    var showSetBudgetDialog by remember { mutableStateOf(false) }

    val expenses = remember(transactions) {
        transactions.filter { it.type == "PENGELUARAN" }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Batas Anggaran Bulanan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = { showSetBudgetDialog = true },
                    modifier = Modifier.testTag("set_budget_trigger_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Atur Limit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Active Month display indicator
        item {
            MonthSelectionBar(selectedMonth, onMonthChosen = { viewModel.setMonthFilter(it) })
        }

        // Display budgets bars progress list
        if (budgets.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = ToskaSecondary)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Kak Yuki belum mengatur batas anggaran nih. Limit anggaran membantu mengendalikan pengeluaran harian dan bulanan secara teratur.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(budgets) { budget ->
                val spent = remember(expenses, budget.category) {
                    if (budget.category == "TOTAL") {
                        expenses.sumOf { it.amount }
                    } else {
                        expenses.filter { it.category == budget.category }.sumOf { it.amount }
                    }
                }
                BudgetProgressBarItem(spent, budget, onDeleteClicked = { viewModel.deleteBudget(it) })
            }
        }

        // Alert History log title
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Log Peringatan Keuangan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (alerts.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearAlerts() }) {
                        Text("Hapus Semua", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // Alert history items
        if (alerts.isEmpty()) {
            item {
                Text(
                    text = "Riwayat peringatan masih kosong. Sistem otomatis memonitor dan memberi notifikasi peringatan harian & bulanan saat pengeluaran mendekati atau melampaui limit anggaran.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        } else {
            items(alerts, key = { it.id }) { alert ->
                AlertHistoryRowItem(alert, onMarkReadClicked = { viewModel.markAlertAsRead(it) })
            }
        }
    }

    if (showSetBudgetDialog) {
        SetBudgetDialog(
            onDismiss = { showSetBudgetDialog = false },
            onConfirm = { cat, amt ->
                viewModel.saveBudget(cat, amt)
                showSetBudgetDialog = false
            }
        )
    }
}

@Composable
fun BudgetProgressBarItem(spent: Double, budget: BudgetEntity, onDeleteClicked: (String) -> Unit) {
    val isOver = spent > budget.amount
    val percentFraction = if (budget.amount > 0) spent / budget.amount else 0.0
    val percent = (percentFraction * 100).toInt()

    val progressColor = when {
        isOver -> if (isSystemInDarkTheme()) StatPengeluaranDark else StatPengeluaran
        percentFraction >= 0.85 -> if (isSystemInDarkTheme()) StatPeringatanDark else StatPeringatan
        else -> if (isSystemInDarkTheme()) ToskaPrimaryDark else ToskaPrimary
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("budget_bar_card_${budget.category}"),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (isSystemInDarkTheme()) SleekBorderDark else SleekBorderLight),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (budget.category == "TOTAL") "BATAS TOTAL PENGELUARAN" else "Kategori: ${budget.category}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${formatAmount(spent)} terpakai dari ${formatAmount(budget.amount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$percent%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = progressColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus Anggaran", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { percentFraction.coerceAtMost(1.0).toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                strokeCap = StrokeCap.Round
            )

            if (isOver) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Error, contentDescription = null, tint = progressColor, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Melebihi anggaran sebesar ${formatAmount(spent - budget.amount)}!",
                        style = MaterialTheme.typography.bodySmall,
                        color = progressColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus Batas Anggaran?") },
            text = { Text("Yakin ingin menghapus batas limit anggaran untuk kategori: ${budget.category}?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteClicked(budget.category)
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun AlertHistoryRowItem(alert: NotificationAlert, onMarkReadClicked: (Int) -> Unit) {
    val isSystem = alert.type == "SISTEM"
    val isDark = isSystemInDarkTheme()

    val tintColor = when {
        isSystem -> ToskaSecondary
        alert.title.contains("Melebihi") || alert.title.contains("Terlampaui") -> if (isDark) StatPengeluaranDark else StatPengeluaran
        else -> if (isDark) StatPeringatanDark else StatPeringatan
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !alert.isRead) { onMarkReadClicked(alert.id) }
            .testTag("alert_history_item_${alert.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (alert.isRead) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)
            }
        ),
        border = BorderStroke(1.dp, if (isSystemInDarkTheme()) SleekBorderDark.copy(alpha = 0.7f) else SleekBorderLight.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isSystem) Icons.Default.CloudDone else Icons.Default.NotificationImportant,
                contentDescription = null,
                tint = tintColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = alert.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (alert.isRead) FontWeight.Bold else FontWeight.ExtraBold
                        ),
                        color = if (alert.isRead) MaterialTheme.colorScheme.onSurface.copy(0.7f) else MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (!alert.isRead) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = alert.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = SimpleDateFormat("dd MMM, HH:mm", Locale("id", "ID")).format(Date(alert.timestamp)),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)
                )
            }
        }
    }
}

// --- SCREEN 4: LIVE GRAPHIC REPORTS & PDF ---
@Composable
fun LiveReportsScreen(viewModel: FinancialViewModel) {
    val context = LocalContext.current
    val summary by viewModel.currentMonthSummary.collectAsStateWithLifecycle()
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()

    val income = summary.first
    val expense = summary.second

    val categoriesSpent = remember(transactions) {
        transactions.filter { it.type == "PENGELUARAN" }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Analisa & Grafik Bulanan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = {
                        val pdfFile = viewModel.getPdfReportFile(context.applicationContext as Application)
                        if (pdfFile != null && pdfFile.exists()) {
                            sharePdfDocument(context, pdfFile)
                        } else {
                            Toast.makeText(context, "Saran: tunggu saran AI termuat terlebih dahulu agar PDF lengkap!", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.testTag("export_pdf_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ToskaPrimary)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ekspor PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            MonthSelectionBar(selectedMonth, onMonthChosen = { viewModel.setMonthFilter(it) })
        }

        // Comparative Bar Chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, if (isSystemInDarkTheme()) SleekBorderDark else SleekBorderLight),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Rasio Arus Kas Bulanan",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    BarChartCanvas(income = income, expense = expense)
                }
            }
        }

        // Categorical Pie list breakdown
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, if (isSystemInDarkTheme()) SleekBorderDark else SleekBorderLight),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Proporsi Belanja Berdasarkan Kategori",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    if (categoriesSpent.isEmpty()) {
                        Text(
                            text = "Belum ada pengeluaran di catatan bulan ini untuk dikelompokkan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontStyle = FontStyle.Italic // Italic
                        )
                    } else {
                        val totalExpense = categoriesSpent.values.sum()
                        categoriesSpent.forEach { (category, amount) ->
                            BreakdownProgressItem(category, amount, totalExpense)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BarChartCanvas(income: Double, expense: Double) {
    val localeID = Locale("in", "ID")
    val rupiah = NumberFormat.getCurrencyInstance(localeID).apply { maximumFractionDigits = 0 }

    val hasData = income > 0 || expense > 0
    val maxVal = maxOf(income, expense, 1.0).toFloat()

    val isDark = isSystemInDarkTheme()
    val incomeColor = if (isDark) StatPemasukanDark else StatPemasukan
    val expenseColor = if (isDark) StatPengeluaranDark else StatPengeluaran

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(bottom = 12.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                // Draw simple horizontal dashed background grid lines
                val numGridLines = 4

                for (i in 0..numGridLines) {
                    val y = (height / numGridLines) * i
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.15f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                if (!hasData) {
                    return@Canvas
                }

                // Bar Layout parameters
                val barWidth = width * 0.15f
                val spacing = width * 0.2f

                // Income Bar coordinate calculations
                val incomeBarHeight = (income.toFloat() / maxVal) * (height - 30.dp.toPx())
                val incomeX = (width / 2) - barWidth - (spacing / 2)
                val incomeY = height - incomeBarHeight

                // Draw Pemasukan
                drawRoundRect(
                    color = incomeColor,
                    topLeft = Offset(incomeX, incomeY),
                    size = Size(barWidth, incomeBarHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )

                // Expense Bar coordinate calculations
                val expenseBarHeight = (expense.toFloat() / maxVal) * (height - 30.dp.toPx())
                val expenseX = (width / 2) + (spacing / 2)
                val expenseY = height - expenseBarHeight

                // Draw Pengeluaran
                drawRoundRect(
                    color = expenseColor,
                    topLeft = Offset(expenseX, expenseY),
                    size = Size(barWidth, expenseBarHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )
            }

            if (!hasData) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Tidak ada riwayat pengeluaran & pemasukan",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Legend details
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(incomeColor))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pemasukan", style = MaterialTheme.typography.bodySmall)
                }
                Text(rupiah.format(income), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = incomeColor)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(expenseColor))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pengeluaran", style = MaterialTheme.typography.bodySmall)
                }
                Text(rupiah.format(expense), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = expenseColor)
            }
        }
    }
}

@Composable
fun BreakdownProgressItem(category: String, amount: Double, totalExpense: Double) {
    val percentFraction = if (totalExpense > 0) amount / totalExpense else 0.0
    val percent = (percentFraction * 100).toInt()

    val accentColor = remember(category) {
        when (category) {
            "Makanan" -> Color(0xFFF76C6C)
            "Belanja" -> Color(0xFFF8DC81)
            "Transportasi" -> Color(0xFF20B2AA)
            "Hiburan" -> Color(0xFFA78BFA)
            "Kos / Tagihan" -> Color(0xFF6366F1)
            "Investasi" -> Color(0xFF10B981)
            "Lainnya" -> Color(0xFF9CA3AF)
            else -> Color(0xFFFF9F1C)
        }
    }

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$category ($percent%)",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = formatAmount(amount),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { percentFraction.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = accentColor,
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
            strokeCap = StrokeCap.Round
        )
    }
}

fun sharePdfDocument(context: Context, file: File) {
    val uri: Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Bagikan Laporan PDF Keuangan"))
}

// --- SCREEN 5: AI FINANCIAL ADVISOR & CHATBOT ---
@Composable
fun AiAdvisorScreen(viewModel: FinancialViewModel) {
    var subTabState by remember { mutableStateOf(0) } // 0 -> "Saran Tips AI", 1 -> "Konsultasi Tanya AI"

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = subTabState) {
            Tab(
                selected = subTabState == 0,
                onClick = { subTabState = 0 },
                text = { Text("Tips Hemat AI", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = subTabState == 1,
                onClick = { subTabState = 1 },
                text = { Text("Tanya MyFinance Chatbot", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            when (subTabState) {
                0 -> TipsAiSubScreen(viewModel)
                1 -> ChatbotAiSubScreen(viewModel)
            }
        }
    }
}

@Composable
fun TipsAiSubScreen(viewModel: FinancialViewModel) {
    val tipsMsg by viewModel.aiRecommendation.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Analisa Hemat Anggaran AI",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Analisis riwayat keuangan riil bulan $selectedMonth",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = { viewModel.generateFinancialAnalysisReport() },
                enabled = !isAnalyzing
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Analisa Ulang", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(0.06f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = ToskaSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Rekomendasi Cerdas Keuangan Kak Yuki:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = tipsMsg,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 22.sp,
                            letterSpacing = 0.1.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun ChatbotAiSubScreen(viewModel: FinancialViewModel) {
    val chatsHistory by viewModel.chatHistory.collectAsStateWithLifecycle()
    val isResponding by viewModel.isChatbotResponding.collectAsStateWithLifecycle()

    var textInput by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    // Keep scrolling list down as new answers populate
    LaunchedEffect(chatsHistory.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        // Chat list area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Initial chatbot welcoming advice
                ChatMessageBubble(
                    text = "Halo Kak Yuki! Aku MyFinance AI, asisten keuangan pribadi kamu yang paling asyik. 😎\n\nKamu bisa tanya apa aja tentang anggaran atau keuanganmu di sini, contohnya:\n- \"Berapa sisa pengeluaran makanan aku?\"\n- \"Bagi tips hemat belanja bulanan dong biar makin irit!\"\n\nYuk, tanyain apa aja, sesantai mungkin juga boleh!",
                    sender = "AI"
                )

                chatsHistory.forEach { chat ->
                    ChatMessageBubble(text = chat.text, sender = chat.sender)
                }

                if (isResponding) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Start)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(0.4f))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                            Text("MyFinance AI lagi mikir dulu ya Kak... 💬", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Text input bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Tanyakan tips kelola uangmu yuk...", fontSize = 13.sp) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_chatbot_input"),
                shape = RoundedCornerShape(24.dp),
                maxLines = 3,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                trailingIcon = {
                    if (textInput.isNotEmpty()) {
                        IconButton(onClick = { textInput = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            )

            FloatingActionButton(
                onClick = {
                    if (textInput.trim().isNotEmpty() && !isResponding) {
                        viewModel.sendMessageToAI(textInput)
                        textInput = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("ai_chatbot_send_button"),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Kirim",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ChatMessageBubble(text: String, sender: String) {
    val isUser = sender == "USER"
    val isDark = isSystemInDarkTheme()

    val backgroundBrush = if (isUser) {
        Brush.horizontalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary,
                ToskaSecondary
            )
        )
    } else {
        if (isSystemInDarkTheme()) {
            Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF1D2B2E),
                    Color(0xFF26393C)
                )
            )
        } else {
            Brush.horizontalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.secondaryContainer,
                    MaterialTheme.colorScheme.secondaryContainer.copy(0.8f)
                )
            )
        }
    }

    val textColor = if (isUser) {
        if (isSystemInDarkTheme()) Color(0xFF0B1214) else MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    val bubbleShape = if (isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(bubbleShape)
                .then(
                    if (!isUser) {
                        Modifier.border(
                            1.dp,
                            if (isSystemInDarkTheme()) SleekBorderDark else SleekBorderLight,
                            bubbleShape
                        )
                    } else {
                        Modifier
                    }
                )
                .background(backgroundBrush)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                color = textColor
            )
        }
    }
}

// --- POPUP DIALOGS ---

// 1. ADD TRANSACTION DIALOG
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, amount: Double, type: String, category: String, description: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("PENGELUARAN") } // "PEMASUKAN" or "PENGELUARAN"
    var category by remember { mutableStateOf("Makanan") }
    var description by remember { mutableStateOf("") }

    val incomeCategories = remember { listOf("Gaji", "Usaha", "Investasi", "Lainnya") }
    val expenseCategories = remember { listOf("Makanan", "Belanja", "Transportasi", "Hiburan", "Kos / Tagihan", "Investasi", "Lainnya") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("add_transaction_dialog_content"),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, if (isSystemInDarkTheme()) SleekBorderDark else SleekBorderLight),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Tambah Catatan Kas Baru",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                // Toggle Tipe: Pemasukan / Pengeluaran
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            type = "PENGELUARAN"
                            category = "Makanan"
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_type_pengeluaran"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "PENGELUARAN") {
                                if (isSystemInDarkTheme()) StatPengeluaranDark else StatPengeluaran
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            contentColor = if (type == "PENGELUARAN") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Pengeluaran", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            type = "PEMASUKAN"
                            category = "Gaji"
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_type_pemasukan"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "PEMASUKAN") {
                                if (isSystemInDarkTheme()) StatPemasukanDark else StatPemasukan
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            contentColor = if (type == "PEMASUKAN") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Pemasukan", fontWeight = FontWeight.Bold)
                    }
                }

                // Input Nominal (Angka) dengan Keypad M-banking Custom
                SleekAmountInputWithKeypad(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = "Jumlah (Rupiah Rp)",
                    placeholder = "Contoh: 50000",
                    modifier = Modifier.testTag("dialog_input_amount")
                )

                // Input Judul
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Judul Transaksi") },
                    placeholder = { Text("Beli makan siang, Gaji bulanan, dsb") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_input_title")
                )

                // Pilih Kategori Row Chips
                Text("Pilih Kategori:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                val catList = if (type == "PEMASUKAN") incomeCategories else expenseCategories
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(catList) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) },
                            modifier = Modifier.testTag("dialog_chip_$cat")
                        )
                    }
                }

                // Deskripsi / Catatan Tambahan
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Catatan Tambahan (Opsional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_input_desc")
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Actions buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal")
                    }

                    Button(
                        onClick = {
                            val amt = amountStr.toDoubleOrNull() ?: 0.0
                            if (amt > 0 && title.isNotBlank()) {
                                onConfirm(title, amt, type, category, description)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_save_button"),
                        enabled = amountStr.isNotEmpty() && title.isNotBlank()
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}

// 2. SET BUDGET LIMIT DIALOG
@Composable
fun SetBudgetDialog(
    onDismiss: () -> Unit,
    onConfirm: (category: String, amount: Double) -> Unit
) {
    var category by remember { mutableStateOf("TOTAL") } // "TOTAL" represents whole spend limit
    var limitAmountStr by remember { mutableStateOf("") }

    val categoriesWithTotal = remember {
        listOf("TOTAL", "Makanan", "Belanja", "Transportasi", "Hiburan", "Kos / Tagihan", "Investasi", "Lainnya")
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("set_budget_dialog_content"),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, if (isSystemInDarkTheme()) SleekBorderDark else SleekBorderLight),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Atur Limit Anggaran",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Pilih kategori untuk diberi batas maksimal pengeluaran bulan ini (atau pilih TOTAL untuk anggaran keseluruhan pengeluaran bulanan):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Choose category
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categoriesWithTotal) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(if (cat == "TOTAL") "TOTAL BULANAN" else cat, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("budget_chip_$cat")
                        )
                    }
                }

                // Nominal Max limit dengan Keypad M-banking Custom
                SleekAmountInputWithKeypad(
                    value = limitAmountStr,
                    onValueChange = { limitAmountStr = it },
                    label = "Batas Maksimal Pengeluaran (Rp)",
                    placeholder = "Contoh: 1500000",
                    modifier = Modifier.testTag("budget_input_limit")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Batal")
                    }

                    Button(
                        onClick = {
                            val amt = limitAmountStr.toDoubleOrNull() ?: 0.0
                            if (amt > 0) {
                                onConfirm(category, amt)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("budget_save_button"),
                        enabled = limitAmountStr.isNotEmpty()
                    ) {
                        Text("Terapkan")
                    }
                }
            }
        }
    }
}

@Composable
fun SleekAmountInputWithKeypad(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "Jumlah (Rupiah Rp)",
    placeholder: String = "Contoh: 50.000",
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    var isKeypadVisible by remember { mutableStateOf(false) }
    var useSystemKeyboard by remember { mutableStateOf(false) }
    
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = if (useSystemKeyboard) value else {
                    if (value.isEmpty()) "" else formatAmount(value.toDoubleOrNull() ?: 0.0)
                },
                onValueChange = { input ->
                    if (useSystemKeyboard) {
                        if (input.all { it.isDigit() }) {
                            onValueChange(input)
                        }
                    }
                },
                label = { Text(label) },
                placeholder = { Text(placeholder) },
                readOnly = !useSystemKeyboard,
                modifier = Modifier.fillMaxWidth(),
                enabled = true,
                prefix = { Text("Rp ") },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            useSystemKeyboard = !useSystemKeyboard
                            if (useSystemKeyboard) {
                                isKeypadVisible = false
                            } else {
                                isKeypadVisible = true
                            }
                        }) {
                            Icon(
                                imageVector = if (useSystemKeyboard) Icons.Default.Dialpad else Icons.Default.Keyboard,
                                contentDescription = "Switch Input Mode",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { isKeypadVisible = !isKeypadVisible }) {
                            Icon(
                                imageVector = if (isKeypadVisible) Icons.Default.Close else Icons.Default.ArrowDropDown,
                                contentDescription = "Toggle Keypad",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )

            if (!useSystemKeyboard) {
                // Safe overlay that captures clicks securely without triggering keyboard or weird focus issues
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable {
                            isKeypadVisible = !isKeypadVisible
                        }
                )
            }
        }
        
        if (!useSystemKeyboard && isKeypadVisible) {
            Text(
                text = "💡 Tip: Sentuh ikon keyboard di atas jika ingin mengetik manual lewat HP.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        
        AnimatedVisibility(
            visible = isKeypadVisible && !useSystemKeyboard,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDark) Color(0xFF131F21) else Color(0xFFF0F5F4), RoundedCornerShape(16.dp))
                    .border(1.dp, if (isDark) SleekBorderDark else SleekBorderLight, RoundedCornerShape(16.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Quick add chips row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val quickAdds = listOf(
                        Pair("+50rb", 50000.0),
                        Pair("+100rb", 100000.0),
                        Pair("+500rb", 500000.0),
                        Pair("+1jt", 1000000.0)
                    )
                    quickAdds.forEach { (lbl, amt) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .background(
                                    if (isDark) Color(0xFF1D2B2E) else Color(0xFFE2EFEF),
                                    RoundedCornerShape(16.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isDark) SleekBorderDark else SleekBorderLight,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    val currentAmt = value.toDoubleOrNull() ?: 0.0
                                    val newAmt = currentAmt + amt
                                    if (newAmt <= 999999999.0) {
                                        onValueChange(newAmt.toLong().toString())
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = lbl,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) ToskaSecondaryDark else ToskaPrimary
                            )
                        }
                    }
                }
                
                // Keyboard Grid
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("000", "0", "⌫")
                )
                
                keys.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { char ->
                            val isAction = char == "⌫" || char == "000"
                            val buttonBaseColor = if (isDark) {
                                if (isAction) Color(0xFF1D2B2E) else Color(0xFF26393C)
                            } else {
                                if (isAction) Color(0xFFE2EFEF) else Color(0xFFFFFFFF)
                            }
                            val contentColor = if (isDark) Color.White else Color(0xFF0B1214)
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .background(buttonBaseColor, RoundedCornerShape(8.dp))
                                    .border(1.dp, if (isDark) SleekBorderDark else SleekBorderLight, RoundedCornerShape(8.dp))
                                    .clickable {
                                        when (char) {
                                            "⌫" -> {
                                                if (value.isNotEmpty()) {
                                                    onValueChange(value.dropLast(1))
                                                }
                                            }
                                            "000" -> {
                                                if (value.isNotEmpty() && value.length < 9) {
                                                    onValueChange(value + "000")
                                                }
                                            }
                                            else -> {
                                                if (value.length < 9) {
                                                    if (value == "0") {
                                                        onValueChange(char)
                                                    } else {
                                                        onValueChange(value + char)
                                                    }
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (char == "⌫") {
                                    Icon(
                                        imageVector = Icons.Default.Backspace,
                                        contentDescription = "Backspace",
                                        modifier = Modifier.size(18.dp),
                                        tint = contentColor
                                    )
                                } else {
                                    Text(
                                        text = char,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = contentColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

