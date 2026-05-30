package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.*
import kotlin.math.pow

// --- SLEEK HUB MAIN TAB NAVIGATION ---
@Composable
fun MyFinanceHubTabRow(
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val tabs = listOf(
            Triple("RINGKASAN", "Ringkasan", Icons.Default.ReceiptLong),
            Triple("REKENING", "Akun & E-Wallet", Icons.Default.AccountBalanceWallet),
            Triple("TABUNGAN", "Target Nabung", Icons.Default.Star), // Fits saving goal nicely
            Triple("HEALTH", "Skor & Simulasi", Icons.Default.Analytics),
            Triple("TAGIHAN", "Tagihan & Hutang", Icons.Default.CalendarMonth)
        )

        tabs.forEach { (tabId, label, icon) ->
            val isSelected = selectedTab == tabId
            FilterChip(
                selected = isSelected,
                onClick = { onTabSelected(tabId) },
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isSelected) {
                                if (isDark) Color(0xFF0B1214) else Color.White
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = if (isDark) Color(0xFF0B1214) else Color.White,
                    containerColor = if (isDark) Color(0xFF1D2B2E) else Color(0xFFE8F1F0)
                ),
                border = BorderStroke(1.dp, if (isDark) SleekBorderDark else SleekBorderLight),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

// ==========================================
// OPSIONAL 1: SAVING GOAL TRACKER (TARGET MENABUNG)
// ==========================================

@Composable
fun SavingsGoalHubSection(viewModel: FinancialViewModel) {
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    val savingGoals by viewModel.allSavingGoals.collectAsStateWithLifecycle()

    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var goalToModify by remember { mutableStateOf<SavingGoalEntity?>(null) }
    var modifyType by remember { mutableStateOf("") } // "DEPOSIT" or "WITHDRAW"

    // Math for total savings
    val totalSaved = remember(savingGoals) { savingGoals.sumOf { it.currentAmount } }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Savings Header Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF1D2B2E) else Color(0xFFE2EFEF)
            ),
            border = BorderStroke(1.dp, if (isDark) SleekBorderDark else SleekBorderLight)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Apresiasi Tabungan",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) ToskaSecondaryDark else ToskaPrimary
                        )
                        Text(
                            text = formatRupiahHelper(totalSaved),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Button(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mimpi Baru", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = if (isDark) Color(0xFF0B1214) else Color.White)
                    }
                }
            }
        }

        // List of Saving Goals
        if (savingGoals.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, if (isDark) SleekBorderDark else SleekBorderLight)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Belum ada rencana keuangan.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Set gol baru seperti liburan, beli gadget, atau menyiapkan dana darurat.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            savingGoals.forEach { goal ->
                val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).coerceIn(0.0..1.0) else 0.0
                val percentText = "${(progress * 100).toInt()}%"

                val categoryIcon = when (goal.category) {
                    "GADGET" -> Icons.Default.Laptop
                    "LIBURAN" -> Icons.Default.Flight
                    "KENDARAAN" -> Icons.Default.DirectionsCar
                    "DANA_DARURAT" -> Icons.Default.Shield
                    else -> Icons.Default.Diamond
                }

                val categoryColor = when (goal.category) {
                    "GADGET" -> Color(0xFF29B6F6)
                    "LIBURAN" -> Color(0xFFAB47BC)
                    "KENDARAAN" -> Color(0xFFFF7043)
                    "DANA_DARURAT" -> Color(0xFF66BB6A)
                    else -> Color(0xFF26A69A)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, if (isDark) SleekBorderDark else SleekBorderLight)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(categoryColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = categoryIcon,
                                    contentDescription = null,
                                    tint = categoryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = goal.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Target: ${goal.targetDate}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = { viewModel.deleteSavingGoal(goal) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Hapus",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Visual Progress Info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${formatRupiahHelper(goal.currentAmount)} / ${formatRupiahHelper(goal.targetAmount)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = percentText,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (progress >= 1.0) Color(0xFF66BB6A) else MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Gradient Sleek Progress Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(if (isDark) Color(0xFF0F1A1B) else Color(0xFFECEFF1))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progress.toFloat())
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                ToskaSecondary,
                                                if (progress >= 1.0) Color(0xFF66BB6A) else ToskaPrimaryDark
                                            )
                                        )
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Action buttons: Deposit / Withdraw
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    goalToModify = goal
                                    modifyType = "WITHDRAW"
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ambil", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }

                            Button(
                                onClick = {
                                    goalToModify = goal
                                    modifyType = "DEPOSIT"
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDark) ToskaTertiaryDark else ToskaSecondary
                                ),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Nabung", style = MaterialTheme.typography.bodySmall, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Add Goal Dialog ---
    if (showAddDialog) {
        AddSavingGoalDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, target, current, date, cat ->
                viewModel.insertSavingGoal(title, target, current, date, cat)
                Toast.makeText(context, "Gol Impian '$title' berhasil dibuat!", Toast.LENGTH_SHORT).show()
                showAddDialog = false
            }
        )
    }

    // --- Modify Savings Amount Dialog ---
    if (goalToModify != null) {
        DepositSavingGoalDialog(
            goal = goalToModify!!,
            type = modifyType,
            onDismiss = { goalToModify = null },
            onConfirm = { amount ->
                val currentGoal = goalToModify!!
                val newAmount = if (modifyType == "DEPOSIT") {
                    currentGoal.currentAmount + amount
                } else {
                    (currentGoal.currentAmount - amount).coerceAtLeast(0.0)
                }
                viewModel.updateSavingGoalAmount(currentGoal.id, newAmount)
                Toast.makeText(context, "Saldo tabungan diperbarui!", Toast.LENGTH_SHORT).show()
                goalToModify = null
            }
        )
    }
}

// ==========================================
// OPSIONAL 2: SKOR KESEHATAN & SIMULATOR MANDIRI
// ==========================================

@Composable
fun FinancialHealthScoreSection(viewModel: FinancialViewModel) {
    val isDark = isSystemInDarkTheme()
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val budgets by viewModel.allBudgets.collectAsStateWithLifecycle()

    // 1. Math Analysis Ratios
    val summary = remember(transactions) {
        val income = transactions.filter { it.type == "PEMASUKAN" }.sumOf { it.amount }
        val expense = transactions.filter { it.type == "PENGELUARAN" }.sumOf { it.amount }
        val savingRate = if (income > 0) ((income - expense) / income * 100).coerceIn(0.0..100.0) else 0.0
        val expenseRate = if (income > 0) (expense / income * 100).coerceIn(0.0..100.0) else 0.0
        Triple(income, expense, Pair(savingRate, expenseRate))
    }

    // Dynamic Financial Health Score computation
    val score = remember(summary, budgets) {
        val income = summary.first
        val expense = summary.second
        val savingR = summary.third.first
        val expenseR = summary.third.second

        if (income == 0.0 && expense == 0.0) {
            75 // default healthy starting point
        } else {
            var rawScore = 40
            // Contribution from saving rate
            if (savingR >= 30.0) rawScore += 30
            else if (savingR >= 15.0) rawScore += 20
            else if (savingR > 0.0) rawScore += 10

            // Contribution from expense limits
            if (expenseR <= 50.0) rawScore += 30
            else if (expenseR <= 75.0) rawScore += 15
            else if (expenseR <= 90.0) rawScore += 5

            // Budget limits discipline
            val exceededBudgetCount = budgets.count { b ->
                if (b.category == "TOTAL") {
                    expense > b.amount
                } else {
                    val catExpense = transactions.filter { it.category == b.category && it.type == "PENGELUARAN" }.sumOf { it.amount }
                    catExpense > b.amount
                }
            }
            if (exceededBudgetCount == 0 && budgets.isNotEmpty()) {
                rawScore += 10
            } else if (exceededBudgetCount > 0) {
                rawScore -= (exceededBudgetCount * 5)
            }

            rawScore.coerceIn(10..100)
        }
    }

    @Composable
    fun getScoreTextLabel(score: Int): Pair<String, Color> {
        return when {
            score >= 80 -> Pair("Sangat Sehat 🌱", Color(0xFF66BB6A))
            score >= 55 -> Pair("Cukup Stabil 🌤️", Color(0xFFFFB74D))
            else -> Pair("Warning Darurat ⚠️", Color(0xFFEF5350))
        }
    }

    val scoreLabel = getScoreTextLabel(score)

    // --- Interactive Compound Interest / Dream Planner Sliders state ---
    var sliderTargetJuta by remember { mutableStateOf(10f) } // 1 to 250 (means 1 Juta to 250 Juta)
    var sliderYears by remember { mutableStateOf(3f) } // 1 to 15 years
    var sliderInterest by remember { mutableStateOf(6f) } // 0% to 15% annual yield

    // Calculations for Dream planner
    val targetAmount = sliderTargetJuta * 1000000.0
    val months = sliderYears * 12.0
    val monthlyRate = (sliderInterest / 100.0) / 12.0

    val calculatedDeposit = remember(targetAmount, months, monthlyRate) {
        if (monthlyRate > 0.0) {
            (targetAmount * monthlyRate) / ((1.0 + monthlyRate).pow(months) - 1.0)
        } else {
            targetAmount / months
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Health gauge representation card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, if (isDark) SleekBorderDark else SleekBorderLight)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Asesmen Kesehatan Keuangan AI",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Gauge Drawing Canvas in central container
                Box(
                    modifier = Modifier.size(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 14.dp.toPx()
                        val diameter = size.minDimension - strokeWidth
                        val radius = diameter / 2
                        val rectSize = Size(diameter, diameter)
                        val offset = Offset(strokeWidth / 2, strokeWidth / 2)

                        // Gray Background Arc (240 degrees sweep, starting from 150)
                        drawArc(
                            color = if (isDark) Color(0xFF1E2829) else Color(0xFFECEFF1),
                            startAngle = 150f,
                            sweepAngle = 240f,
                            useCenter = false,
                            topLeft = offset,
                            size = rectSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // Dynamic Colored Arc
                        val sweepValue = (score / 100f) * 240f
                        drawArc(
                            brush = Brush.horizontalGradient(
                                colors = listOf(ToskaSecondary, ToskaPrimaryDark)
                            ),
                            startAngle = 150f,
                            sweepAngle = sweepValue,
                            useCenter = false,
                            topLeft = offset,
                            size = rectSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    // Score Display Text inside Ring
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = score.toString(),
                            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                            color = scoreLabel.second
                        )
                        Text(
                            text = "/ 100",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Health Label Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(scoreLabel.second.copy(alpha = 0.15f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = scoreLabel.first,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = scoreLabel.second
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Actionable AI Tips description based on the computed score
                Text(
                    text = when {
                        score >= 80 -> "Wah luar biasa! Keuangan Kak Yuki sehat banget nih! Rasio tabungannya solid dan batas anggaran terjaga aman. Pertahankan performa prima ini ya!"
                        score >= 55 -> "Keuangan Kak Yuki cukup aman nih, tapi batas anggaran bulanan tetap harus dijaga biar gak jebol. Coba kurangi jajan santai/makan mewah dulu biar target tabungan makin cepat tercapai!"
                        else -> "Waduh, gawat nih Kak Yuki! Pengeluaran kamu masuk zona merah atau udah ngelebihi batas aman. Yuk tanyain ke asisten AI buat dapetin tips hemat taktis!"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // COMPOUND CALCULATOR SIMULATOR CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, if (isDark) SleekBorderDark else SleekBorderLight)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Kalkulator Simulasi Dana Impian",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Hitung berapa investasi bulanan dengan imbal hasil majemuk agar target mimpimu terwujud.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Numeric Labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Target Dana", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Text(
                        formatRupiahHelper(targetAmount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Slider(
                    value = sliderTargetJuta,
                    onValueChange = { sliderTargetJuta = it },
                    valueRange = 1f..200f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Jangka Waktu", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Text(
                        "${sliderYears.toInt()} Tahun",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Slider(
                    value = sliderYears,
                    onValueChange = { sliderYears = it },
                    valueRange = 1f..15f,
                    steps = 14,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Asumsi Imbal Hasil (P.A.)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Text(
                        "${sliderInterest.toInt()}% per tahun",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Slider(
                    value = sliderInterest,
                    onValueChange = { sliderInterest = it },
                    valueRange = 0f..15f,
                    steps = 15,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Result Box Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) Color(0xFF1D2B2E) else Color(0xFFF0F7F6))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Tabungan Bulanan yang Dibutuhkan:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${formatRupiahHelper(calculatedDeposit)} / bulan",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = ToskaSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (sliderInterest > 0) "*Sudah menghitung sistem bunga majemuk instan" else "*Simulasi menabung fisik reguler",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontStyle = FontStyle.Italic),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// OPSIONAL 3: TAGIHAN RUTIN & HUTANG PIUTANG TRACKER
// ==========================================

@Composable
fun BillsRecurringSection(viewModel: FinancialViewModel) {
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    val allBills by viewModel.allBillsRecurring.collectAsStateWithLifecycle()

    var showAddDialog by rememberSaveable { mutableStateOf(false) }

    // Math obligations
    val totalDebt = remember(allBills) { allBills.filter { it.type == "HUTANG" && !it.isPaid }.sumOf { it.amount } }
    val totalReceivable = remember(allBills) { allBills.filter { it.type == "PIUTANG" && !it.isPaid }.sumOf { it.amount } }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Balances breakdown card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, if (isDark) SleekBorderDark else SleekBorderLight)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Total Hutang Saya", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        formatRupiahHelper(totalDebt),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFEF5350)
                    )
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(if (isDark) SleekBorderDark else SleekBorderLight)
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                ) {
                    Text("Total Piutang (Klaim)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        formatRupiahHelper(totalReceivable),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF66BB6A)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Daftar Tagihan & Kewajiban",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Button(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Tambah", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = if (isDark) Color(0xFF0B1214) else Color.White)
            }
        }

        // Bills Display
        if (allBills.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, if (isDark) SleekBorderDark else SleekBorderLight)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Belum ada tagihan terdaftar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            allBills.forEach { bill ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (bill.isPaid) {
                            if (isDark) Color(0xFF132022) else Color(0xFFF1F8F6)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    border = BorderStroke(1.dp, if (isDark) SleekBorderDark else SleekBorderLight)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = bill.isPaid,
                            onCheckedChange = { isChecked ->
                                viewModel.updateBillPaidStatus(bill.id, isChecked)
                                val msg = if (isChecked) "Kewajiban ditandakan LUNAS!" else "Diperbarui ke belum lunas."
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = bill.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontStyle = if (bill.isPaid) FontStyle.Italic else FontStyle.Normal
                                    ),
                                    color = if (bill.isPaid) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when (bill.type) {
                                                "HUTANG" -> Color(0xFFEF5350).copy(alpha = 0.15f)
                                                "PIUTANG" -> Color(0xFF66BB6A).copy(alpha = 0.15f)
                                                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            }
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = bill.type,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = when (bill.type) {
                                            "HUTANG" -> Color(0xFFEF5350)
                                            "PIUTANG" -> Color(0xFF66BB6A)
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                    )
                                }
                            }

                            Text(
                                text = "Nominal: ${formatRupiahHelper(bill.amount)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = if (bill.isPaid) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                            )

                            if (bill.notes.isNotEmpty()) {
                                Text(
                                    text = bill.notes,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontStyle = FontStyle.Italic),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = "Jatuh Tempo: ${bill.dueDate}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                color = if (bill.isPaid) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else Color(0xFFFF7043)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.deleteBillRecurring(bill) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Hapus",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Add Bill Dialog ---
    if (showAddDialog) {
        AddBillRecurringDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, amount, type, date, notes ->
                viewModel.insertBillRecurring(title, amount, type, date, notes)
                Toast.makeText(context, "$type '$title' berhasil terdaftar!", Toast.LENGTH_SHORT).show()
                showAddDialog = false
            }
        )
    }
}

// ==========================================
// INNER DIALOG COMPOSABLES FOR CORE ACTIONS
// ==========================================

@Composable
fun AddSavingGoalDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Double, String, String) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    var title by remember { mutableStateOf("") }
    var targetStr by remember { mutableStateOf("") }
    var initialStr by remember { mutableStateOf("") }
    var targetDate by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("GADGET") } // default

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, if (isDark) SleekBorderDark else SleekBorderLight),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Buat Target Tabungan Baru",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nama Impian / Sasaran") },
                    placeholder = { Text("e.g. Beli Laptop Kerja, Liburan Bali") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                SleekAmountInputWithKeypad(
                    value = targetStr,
                    onValueChange = { targetStr = it },
                    label = "Target Dana (Rp)",
                    placeholder = "e.g. 5000000"
                )

                Spacer(modifier = Modifier.height(8.dp))

                SleekAmountInputWithKeypad(
                    value = initialStr,
                    onValueChange = { initialStr = it },
                    label = "Mulai Celengan Pertama (Rp)",
                    placeholder = "e.g. 200000"
                )

                OutlinedTextField(
                    value = targetDate,
                    onValueChange = { targetDate = it },
                    label = { Text("Target Waktu Pencapaian") },
                    placeholder = { Text("e.g., Desember 2026, 6 Bulan") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Category selector
                Text("Kategori Rencana", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                val cats = listOf("GADGET", "LIBURAN", "KENDARAAN", "DANA_DARURAT", "LAINNYA")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    cats.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val targetVal = targetStr.toDoubleOrNull() ?: 0.0
                            val currentVal = initialStr.toDoubleOrNull() ?: 0.0
                            if (title.isNotEmpty() && targetVal > 0.0) {
                                onConfirm(title, targetVal, currentVal, targetDate.ifEmpty { "Fleksibel" }, category)
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Aktifkan Gol", color = if (isDark) Color(0xFF0B1214) else Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun DepositSavingGoalDialog(
    goal: SavingGoalEntity,
    type: String, // "DEPOSIT" or "WITHDRAW"
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    var amountStr by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, if (isDark) SleekBorderDark else SleekBorderLight),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (type == "DEPOSIT") "Tambahkan ke Tabungan" else "Ambil dari Tabungan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Target: ${goal.title}\nSaldo Saat Ini: ${formatRupiahHelper(goal.currentAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                SleekAmountInputWithKeypad(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = "Nominal Uang (Rp)",
                    placeholder = "e.g. 100000"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = amountStr.toDoubleOrNull() ?: 0.0
                            if (amt > 0.0) {
                                onConfirm(amt)
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Konfirmasi", color = if (isDark) Color(0xFF0B1214) else Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun AddBillRecurringDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, String, String) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("TAGIHAN") } // "TAGIHAN", "HUTANG", "PIUTANG"
    var dueDate by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, if (isDark) SleekBorderDark else SleekBorderLight),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Daftarkan Kewajiban Finansial Baru",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Type select buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val types = listOf("TAGIHAN", "HUTANG", "PIUTANG")
                    types.forEach { t ->
                        val selected = type == t
                        FilterChip(
                            selected = selected,
                            onClick = { type = t },
                            label = { Text(t, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(
                        text = when (type) {
                            "HUTANG" -> "Nama Pemberi Pinjaman"
                            "PIUTANG" -> "Nama Peminjam Uang"
                            else -> "Nama Layanan / Tagihan"
                        }
                    ) },
                    placeholder = { Text("e.g. Netflix Premium, Kosan, Pinjaman Andi") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                SleekAmountInputWithKeypad(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = "Nominal Uang (Rp)",
                    placeholder = "e.g. 100000"
                )

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Tanggal Jatuh Tempo") },
                    placeholder = { Text("e.g. Setiap Tanggal 15, 12 Juni 2026") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan Tambahan") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = amountStr.toDoubleOrNull() ?: 0.0
                            if (title.isNotEmpty() && amt > 0.0) {
                                onConfirm(title, amt, type, dueDate.ifEmpty { "Segera" }, notes)
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Tambahkan", color = if (isDark) Color(0xFF0B1214) else Color.White)
                    }
                }
            }
        }
    }
}

private val sleekLocaleID = Locale("in", "ID")
private val sleekFormatRupiah = NumberFormat.getCurrencyInstance(sleekLocaleID).apply {
    maximumFractionDigits = 0
}

// Private NumberFormat Formatter
fun formatRupiahHelper(amount: Double): String {
    synchronized(sleekFormatRupiah) {
        return sleekFormatRupiah.format(amount)
    }
}

// ==========================================
// OPSIONAL 4: FINANCIAL ACCOUNTS (BANK & E-WALLET) MANAGER
// ==========================================

@Composable
fun PaymentAccountsHubSection(viewModel: FinancialViewModel) {
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    val accounts by viewModel.allPaymentAccounts.collectAsStateWithLifecycle()

    var showAddDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Upper banner card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF1D2B2E) else Color(0xFFE2EFEF)
            ),
            border = BorderStroke(1.dp, if (isDark) SleekBorderDark else SleekBorderLight)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Kelola Akun Pembayaran",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) ToskaSecondaryDark else ToskaPrimary
                    )
                    Text(
                        text = "${accounts.size} Akun Terdaftar",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Akun Baru", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = if (isDark) Color(0xFF0B1214) else Color.White)
                }
            }
        }

        // Empty state
        if (accounts.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, if (isDark) SleekBorderDark else SleekBorderLight)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Belum ada Rekening atau E-Wallet.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Tambahkan rekening bank atau akun e-wallet Kak Yuki di sini biar pengecekan dan pencatatan transaksi jadi jauh lebih gampang!",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            accounts.forEach { acc ->
                val isBank = acc.accountType == "BANK"
                // Sleek Gradient for Cards
                val bgGradient = if (isBank) {
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF0D47A1), Color(0xFF1976D2)) // Premium Bank Blue
                    )
                } else {
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF00796B), Color(0xFF00B0FF)) // Teal & Cyan E-Wallet Neon
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334E52) else Color(0xFFB0BEC5))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bgGradient)
                            .padding(16.dp)
                    ) {
                        Column {
                            // Header: Bank / E-Wallet Name & Chip
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isBank) Icons.Default.AccountBalance else Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = acc.institutionName,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (isBank) "BANK" else "E-WALLET",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 9.sp,
                                            color = Color.White
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Account Number / ID
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .clickable {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("No Rekening", acc.accountNumber)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Nomor Rekening / ID disalin: ${acc.accountNumber}", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Nomor Rekening / ID (Sentuh untuk salin):",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = acc.accountNumber,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.5.sp
                                        ),
                                        color = Color.White
                                    )
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.2f))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Salin Nomor",
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "SALIN NO",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 9.sp
                                            ),
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Details (Owner & Delete action)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    Text(
                                        text = "NAMA PEMILIK",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = acc.ownerName.uppercase(),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = { viewModel.movePaymentAccountUp(acc) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = "Ke Atas",
                                            tint = Color.White.copy(alpha = 0.9f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.movePaymentAccountDown(acc) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDownward,
                                            contentDescription = "Ke Bawah",
                                            tint = Color.White.copy(alpha = 0.9f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                            val clip = android.content.ClipData.newPlainText("Info Rekening", "${acc.institutionName}, ${acc.ownerName}, ${acc.accountNumber}")
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Rekening disalin: ${acc.institutionName}, ${acc.ownerName}, ${acc.accountNumber}", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Salin Info Rekening",
                                            tint = Color.White.copy(alpha = 0.9f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    if (acc.currentBalance > 0.0) {
                                        Text(
                                            text = formatRupiahHelper(acc.currentBalance),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            viewModel.deletePaymentAccount(acc)
                                            Toast.makeText(context, "${acc.institutionName} berhasil dihapus.", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Hapus",
                                            tint = Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
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

    if (showAddDialog) {
        AddPaymentAccountDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { instName, type, accNum, owner, bal ->
                viewModel.insertPaymentAccount(instName, type, accNum, owner, bal)
                showAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPaymentAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, Double) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    var isBank by remember { mutableStateOf(true) }
    var selectedInstitution by remember { mutableStateOf("Bank Central Asia (BCA)") }
    var accountNumber by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var balanceStr by remember { mutableStateOf("") }

    // Dropdown list
    val bankOptions = listOf(
        "Bank Central Asia (BCA)",
        "Bank Rakyat Indonesia (BRI)",
        "Bank Mandiri",
        "Bank Negara Indonesia (BNI)",
        "Bank Syariah Indonesia (BSI)",
        "Bank Tabungan Negara (BTN)",
        "Bank CIMB Niaga",
        "Bank Danamon",
        "Bank Permata",
        "Bank Mega",
        "Bank Jago",
        "SeaBank",
        "Allo Bank",
        "Blu by BCA Digital",
        "Neobank (BNC)",
        "Digibank by DBS",
        "Jenius (BTPN)"
    )

    val walletOptions = listOf(
        "GoPay",
        "OVO",
        "Dana",
        "ShopeePay",
        "LinkAja",
        "AstraPay"
    )

    // Sync selected institution automatically when toggling bank/wallet
    LaunchedEffect(isBank) {
        selectedInstitution = if (isBank) bankOptions.first() else walletOptions.first()
    }

    var dropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, if (isDark) SleekBorderDark else SleekBorderLight)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Tambah Akun Pembayaran",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                // Tab Selector for Bank vs E-Wallet
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val activeColor = MaterialTheme.colorScheme.primary
                    val inactiveColor = if (isDark) Color(0xFF1D2B2E) else Color(0xFFECEFF1)

                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { isBank = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isBank) activeColor else inactiveColor
                        )
                    ) {
                        Text(
                            "BANK",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isBank) (if (isDark) Color(0xFF0B1214) else Color.White) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { isBank = false },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isBank) activeColor else inactiveColor
                        )
                    ) {
                        Text(
                            "E-WALLET",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (!isBank) (if (isDark) Color(0xFF0B1214) else Color.White) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Drop-down selector or list
                Text(
                    text = "Pilih " + (if (isBank) "Bank" else "E-Wallet") + ":",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                )

                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedInstitution,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors()
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        val options = if (isBank) bankOptions else walletOptions
                        options.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    selectedInstitution = option
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = { accountNumber = it },
                    label = { Text(if (isBank) "Nomor Rekening" else "No HP / ID Akun") },
                    placeholder = { Text(if (isBank) "e.g., 5220304859" else "e.g., 081298765432") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text("Nama Pemilik Rekening / Akun") },
                    placeholder = { Text("e.g., Yuki Yasa") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                SleekAmountInputWithKeypad(
                    value = balanceStr,
                    onValueChange = { balanceStr = it },
                    label = "Estimasi Saldo (Opsional, Rp)",
                    placeholder = "e.g. 500000"
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (selectedInstitution.isNotEmpty() && accountNumber.isNotEmpty() && ownerName.isNotEmpty()) {
                                onConfirm(
                                    selectedInstitution,
                                    if (isBank) "BANK" else "EWALLET",
                                    accountNumber,
                                    ownerName,
                                    balanceStr.toDoubleOrNull() ?: 0.0
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Simpan", color = if (isDark) Color(0xFF0B1214) else Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardPaymentAccountsList(viewModel: FinancialViewModel) {
    val context = LocalContext.current
    val accounts by viewModel.allPaymentAccounts.collectAsStateWithLifecycle()
    val isDark = isSystemInDarkTheme()
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddPaymentAccountDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { instName, type, accNum, owner, bal ->
                viewModel.insertPaymentAccount(instName, type, accNum, owner, bal)
                showAddDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Akun & Saldo Pembayaran",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onBackground
            )
            IconButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_payment_account_shortcut_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AddCircle,
                    contentDescription = "Tambah Rekening / E-Wallet Baru",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF1D2B2E) else Color(0xFFF1F7F6)
            ),
            border = BorderStroke(1.dp, if (isDark) SleekBorderDark else SleekBorderLight)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (accounts.isEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Belum ada Rekening/E-Wallet terdaftar.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    accounts.forEachIndexed { index, acc ->
                        val isBank = acc.accountType == "BANK"
                        val iconColor = if (isBank) Color(0xFF0D47A1) else Color(0xFF00796B)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = iconColor.copy(alpha = 0.15f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = if (isBank) Icons.Default.AccountBalance else Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = iconColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("No Rekening", acc.accountNumber)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Nomor Rekening / ID disalin: ${acc.accountNumber}", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = acc.institutionName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "${acc.accountNumber} • ${acc.ownerName.uppercase()}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Salin",
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isBank) Color(0xFF0D47A1).copy(alpha = 0.1f) else Color(0xFF00796B).copy(alpha = 0.1f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isBank) "BANK" else "DOMPET",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 8.sp,
                                        color = if (isBank) Color(0xFF0D47A1) else Color(0xFF00796B)
                                    )
                                )
                            }
                        }

                        if (index < accounts.size - 1) {
                            Divider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

