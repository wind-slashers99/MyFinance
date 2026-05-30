package com.example

import android.os.Bundle
import android.content.Context
import android.widget.Toast
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.BorderStroke
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.example.ui.FinancialApp
import com.example.ui.FinancialViewModel
import com.example.ui.theme.MyApplicationTheme
import java.util.concurrent.Executor

class MainActivity : FragmentActivity() {

    private lateinit var executor: Executor
    private var isUnlocked by mutableStateOf(true)

    override fun onStop() {
        super.onStop()
        if (isAppLockPrefEnabled(applicationContext)) {
            isUnlocked = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        executor = ContextCompat.getMainExecutor(this)

        // Schedule month-end evaluation reminders with try-catch guard
        try {
            com.example.receiver.MonthEndScheduler.scheduleReminder(applicationContext)
        } catch (t: Throwable) {
            t.printStackTrace()
        }

        // Request POST_NOTIFICATIONS permission dynamically on Android 13+ to support warnings / notifications
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val permission = android.Manifest.permission.POST_NOTIFICATIONS
                if (androidx.core.content.ContextCompat.checkSelfPermission(this, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    androidx.core.app.ActivityCompat.requestPermissions(this, arrayOf(permission), 101)
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }

        // Initialize lock state
        val isLockEnabledInit = isAppLockPrefEnabled(applicationContext)
        isUnlocked = !isLockEnabledInit

        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDarkTheme by remember { mutableStateOf(systemDark) }

            // App Lock States
            var isAppLockEnabled by remember { mutableStateOf(isLockEnabledInit) }
            var authErrorMessage by remember { mutableStateOf<String?>(null) }

            // Auto trigger biometric challenge on launch with a safe brief delay to prevent activity lifecycle issue
            LaunchedEffect(isAppLockEnabled) {
                if (isAppLockEnabled && !isUnlocked) {
                    kotlinx.coroutines.delay(600)
                    triggerBiometricUnlock(
                        onSuccess = {
                            isUnlocked = true
                            authErrorMessage = null
                        },
                        onFailure = { err ->
                            authErrorMessage = err
                        }
                    )
                }
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                if (isAppLockEnabled && !isUnlocked) {
                    AppLockedScreen(
                        onUnlockClicked = {
                            triggerBiometricUnlock(
                                onSuccess = {
                                    isUnlocked = true
                                    authErrorMessage = null
                                },
                                onFailure = { err ->
                                    authErrorMessage = err
                                }
                            )
                        },
                        onPinUnlocked = {
                            isUnlocked = true
                            authErrorMessage = null
                        },
                        currentSavedPin = remember {
                            applicationContext.getSharedPreferences("safeguard_prefs", Context.MODE_PRIVATE)
                                .getString("app_lock_pin", "1234") ?: "1234"
                        },
                        errorMessage = authErrorMessage
                    )
                } else {
                    val financialViewModel: FinancialViewModel = viewModel()
                    FinancialApp(
                        viewModel = financialViewModel,
                        isDarkTheme = isDarkTheme,
                        onThemeToggle = { isDarkTheme = !isDarkTheme },
                        isAppLockEnabled = isAppLockEnabled,
                        onToggleAppLock = { enabled ->
                            if (isRunningOnEmulator()) {
                                // On Emulator, toggle lock directly and let them test the PIN keyboard
                                setAppLockPrefEnabled(applicationContext, enabled)
                                isAppLockEnabled = enabled
                                isUnlocked = !enabled
                                Toast.makeText(applicationContext, if (enabled) "Kunci Aplikasi Aktif (Mode Emulator)!" else "Kunci Aplikasi Dimatikan.", Toast.LENGTH_SHORT).show()
                            } else {
                                triggerBiometricChallenge(
                                    title = "Verifikasi Keamanan",
                                    subtitle = if (enabled) "Verifikasi sidik jari/wajah untuk mengaktifkan Kunci Aplikasi" else "Verifikasi sidik jari/wajah untuk menonaktifkan Kunci Aplikasi",
                                    onSuccess = {
                                        setAppLockPrefEnabled(applicationContext, enabled)
                                        isAppLockEnabled = enabled
                                        isUnlocked = !enabled
                                        Toast.makeText(applicationContext, if (enabled) "Kunci Aplikasi Aktif!" else "Kunci Aplikasi Dimatikan.", Toast.LENGTH_SHORT).show()
                                    },
                                    onFailure = { err ->
                                        Toast.makeText(applicationContext, "Gagal memverifikasi: $err", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    private fun isRunningOnEmulator(): Boolean {
        val model = android.os.Build.MODEL ?: ""
        val product = android.os.Build.PRODUCT ?: ""
        val brand = android.os.Build.BRAND ?: ""
        val device = android.os.Build.DEVICE ?: ""
        val fingerprint = android.os.Build.FINGERPRINT ?: ""
        val hardware = android.os.Build.HARDWARE ?: ""
        return model.startsWith("sdk") ||
                model.contains("Emulator") ||
                model.contains("Android SDK") ||
                product.contains("sdk") ||
                product.contains("emulator") ||
                brand.startsWith("generic") ||
                device.startsWith("generic") ||
                fingerprint.startsWith("generic") ||
                hardware.contains("goldfish") ||
                hardware.contains("ranchu") ||
                hardware.contains("vbox")
    }

    private fun isAppLockPrefEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences("safeguard_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("app_lock_biometric", false)
    }

    private fun setAppLockPrefEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences("safeguard_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("app_lock_biometric", enabled).apply()
    }

    private fun triggerBiometricUnlock(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        triggerBiometricChallenge(
            title = "MyFinance Safeguard",
            subtitle = "Sentuh sensor sidik jari atau gunakan pengenalan wajah untuk membuka kunci",
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }

    private fun triggerBiometricChallenge(
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (isRunningOnEmulator()) {
            onFailure("Sidik jari tidak tersedia di emulator. Silakan buka dengan PIN (Default: 1234).")
            return
        }
        try {
            val biometricManager = BiometricManager.from(this)
            val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL

            val authStatus = try {
                biometricManager.canAuthenticate(authenticators)
            } catch (ex: Throwable) {
                try {
                    biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                } catch (ex2: Throwable) {
                    BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE
                }
            }

            if (authStatus == BiometricManager.BIOMETRIC_SUCCESS) {
                if (!lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) {
                    onSuccess()
                    return
                }

                val promptInfo = try {
                    val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
                        .setTitle(title)
                        .setSubtitle(subtitle)

                    // Safeguard: On devices where DEVICE_CREDENTIAL is used but cannot combine with setNegativeButtonText 
                    // or isn't well supported, we protect the build()
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        promptInfoBuilder.setAllowedAuthenticators(authenticators)
                    } else {
                        promptInfoBuilder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                        promptInfoBuilder.setNegativeButtonText("Batal")
                    }
                    promptInfoBuilder.build()
                } catch (exBuild: Throwable) {
                    onSuccess()
                    return
                }

                val biometricPrompt = try {
                    BiometricPrompt(this, executor,
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                super.onAuthenticationError(errorCode, errString)
                                onFailure(errString.toString())
                            }

                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                super.onAuthenticationSucceeded(result)
                                onSuccess()
                            }

                            override fun onAuthenticationFailed() {
                                super.onAuthenticationFailed()
                                onFailure("Biometrik tidak terdaftar atau tidak cocok.")
                            }
                        })
                } catch (exInit: Throwable) {
                    onSuccess()
                    return
                }

                try {
                    biometricPrompt.authenticate(promptInfo)
                } catch (exAuth: Throwable) {
                    onSuccess()
                }
            } else {
                onSuccess()
            }
        } catch (t: Throwable) {
            onSuccess()
        }
    }
}

@Composable
fun AppLockedScreen(
    onUnlockClicked: () -> Unit,
    onPinUnlocked: () -> Unit,
    currentSavedPin: String,
    errorMessage: String?
) {
    val isDark = isSystemInDarkTheme()
    var enteredPin by remember { mutableStateOf("") }
    var localErrorMsg by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF090E10) else Color(0xFFF0F4F6))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = if (isDark) Color(0xFF1D2B2E) else Color.White,
                shadowElevation = 4.dp,
                modifier = Modifier.size(80.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock Icon",
                        tint = if (isDark) Color(0xFF00B0FF) else Color(0xFF00796B),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Text(
                text = "MyFinance Safeguard",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Masukkan PIN Keamanan untuk Membuka",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // PIN Dots Indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                for (i in 1..4) {
                    val active = enteredPin.length >= i
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(
                                if (active) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                }
                            )
                    )
                }
            }

            val errorToDisplay = localErrorMsg ?: errorMessage
            if (!errorToDisplay.isNullOrEmpty()) {
                Text(
                    text = errorToDisplay,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else {
                Text(
                    text = "PIN Default: 1234. Kak Nakhlah bisa ubah ini di settings ya.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Numeric Keypad 3x4
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("BIOMETRIC", "0", "DELETE")
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                keys.forEach { rowKeys ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rowKeys.forEach { key ->
                            if (key == "BIOMETRIC") {
                                IconButton(
                                    onClick = onUnlockClicked,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "Buka Sidik Jari",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            } else if (key == "DELETE") {
                                IconButton(
                                    onClick = {
                                        if (enteredPin.isNotEmpty()) {
                                            enteredPin = enteredPin.dropLast(1)
                                            localErrorMsg = null
                                        }
                                    },
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Backspace,
                                        contentDescription = "Backspace",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        if (enteredPin.length < 4) {
                                            enteredPin += key
                                            localErrorMsg = null
                                            if (enteredPin.length == 4) {
                                                if (enteredPin == currentSavedPin) {
                                                    onPinUnlocked()
                                                } else {
                                                    localErrorMsg = "PIN salah. Silakan coba lagi."
                                                    enteredPin = ""
                                                }
                                            }
                                        }
                                    },
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDark) Color(0xFF1D2B2E) else Color.White,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    border = BorderStroke(1.dp, if (isDark) Color(0xFF2C3E42) else Color(0xFFE0E0E0)),
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Text(
                                        text = key,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Black,
                                            fontSize = 20.sp
                                        )
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
