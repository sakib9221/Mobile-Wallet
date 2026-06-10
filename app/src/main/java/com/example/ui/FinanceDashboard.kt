package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import coil.compose.AsyncImage
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.Transaction
import com.example.ui.theme.LocalThemeState
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.*
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.composed


@Composable
fun isAppDark(): Boolean {
    val theme = LocalThemeState.current
    return theme == "dark" || (theme == "system" && androidx.compose.foundation.isSystemInDarkTheme())
}

fun Modifier.android16Clickable(
    enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape? = null,
    elevation: androidx.compose.ui.unit.Dp = 0.dp,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Highly optimized spring animation with high stiffness to settle in 3 frames, saving enormous CPU/GPU cycles
    val pressProgressState = animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = Spring.StiffnessHigh
        ),
        label = "press_progress"
    )

    val contentModifier = if (shape != null) {
        if (elevation > 0.dp) {
            this.shadow(elevation, shape, clip = true)
        } else {
            this.clip(shape)
        }
    } else {
        this
    }

    contentModifier
        // DEFERRED READ OPTIMIZATION: Reading pressProgressState.value ONLY inside the graphicsLayer block
        // completely prevents any recomposition of the parent composable or item contents during click/gesture animations.
        .graphicsLayer {
            val progress = pressProgressState.value
            val s = 1f - (progress * 0.05f)
            scaleX = s
            scaleY = s
            this.alpha = 1f - (progress * 0.10f)
        }
        .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            enabled = enabled,
            onClick = onClick
        )
}

fun Modifier.android16ScalePress(customInteractionSource: MutableInteractionSource? = null): Modifier = composed {
    val interactionSource = customInteractionSource ?: remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressProgressState = animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = Spring.StiffnessHigh
        ),
        label = "scale_press_progress"
    )
    this.graphicsLayer {
        val progress = pressProgressState.value
        val s = 1f - (progress * 0.04f)
        scaleX = s
        scaleY = s
        this.alpha = 1f - (progress * 0.06f)
    }
}

// Bouncy scale-up and fade entrance animation wrapper for pop-up dialogs and options dropdowns
@Composable
fun AnimatedDialogContent(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var animateTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animateTrigger = true
    }
    val scale by animateFloatAsState(
        targetValue = if (animateTrigger) 1f else 0.85f,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "dialog_scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (animateTrigger) 1f else 0.1f,
        animationSpec = tween(durationMillis = 220),
        label = "dialog_alpha"
    )
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceDashboardScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val currentTab by viewModel.uiTab.collectAsStateWithLifecycle()
    val selectedTheme by viewModel.selectedTheme.collectAsStateWithLifecycle()
    val dailyExpenseLimit by viewModel.dailyExpenseLimit.collectAsStateWithLifecycle()

    val localizedContext = remember(currentLanguage) {
        LocaleHelper.getLocalizedContext(context, currentLanguage)
    }

    // Helper functions for localized text - cached as stable lambda to avoid re-allocation on every frame recompose
    val getStringResource = remember(localizedContext) {
        { id: Int ->
            try {
                localizedContext.getString(id)
            } catch (e: Exception) {
                "Other"
            }
        }
    }

    // Modal dialog trigger states
    var showActionsMenu by remember { mutableStateOf(false) }
    
    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            try {
                val contentBuilder = java.lang.StringBuilder()
                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    java.io.BufferedReader(java.io.InputStreamReader(inputStream, java.nio.charset.StandardCharsets.UTF_8)).use { reader ->
                        var line = reader.readLine()
                        while (line != null) {
                            contentBuilder.append(line)
                            line = reader.readLine()
                        }
                    }
                }
                val jsonStr = contentBuilder.toString()
                
                viewModel.importBackupContent(
                    jsonStr = jsonStr,
                    onSuccess = {
                        Toast.makeText(
                            context,
                            if (currentLanguage == "bn") "ব্যাকআপ সফলভাবে ইমপোর্ট করা হয়েছে!" else "Backup imported successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onError = { errorMsg ->
                        Toast.makeText(
                            context,
                            if (currentLanguage == "bn") "ইমপোর্ট ব্যর্থ হয়েছে: $errorMsg" else "Import failed: $errorMsg",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    if (currentLanguage == "bn") "ফাইল পড়তে ব্যর্থ হয়েছে" else "Failed to read backup file",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var showDeveloperCreditDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showBajarListDialog by remember { mutableStateOf(false) }
    var showDebtListDialog by remember { mutableStateOf(false) }
    var launchUpdateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var showReportPreviewDialog by remember { mutableStateOf(false) }
    var showAllTransactionsDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }
    val bItems by viewModel.bajarItems.collectAsStateWithLifecycle()
    val debtRecords by viewModel.debtRecords.collectAsStateWithLifecycle()

    // Optimize listener callbacks with remember to prevent parent-induced child recompositions at 120Hz
    val onLanguageToggle = remember { { showLanguageDialog = true } }
    val onShowDeveloperCredit = remember { { showDeveloperCreditDialog = true } }
    val onShowAuth = remember { { showAuthDialog = true } }
    val onAddTransactionClick = remember { { showAddDialog = true } }
    val onTabSelect = remember { { tab: String -> viewModel.changeTab(tab) } }
    val onDeleteTransaction = remember {
        { transaction: Transaction ->
            transactionToDelete = transaction
        }
    }
    val onDismissDeveloperCredit = remember { { showDeveloperCreditDialog = false } }
    val onDismissAddDialog = remember { { showAddDialog = false } }
    val onSaveAddDialog = remember(context, getStringResource) {
        { amount: Double, category: String, type: String, note: String, dateLong: Long ->
            viewModel.addTransaction(amount, category, type, note, dateLong)
            showAddDialog = false
            Toast.makeText(context, getStringResource(R.string.transaction_saved), Toast.LENGTH_SHORT).show()
        }
    }
    val onDismissEditDialog = remember { { showEditDialog = false } }
    val onSaveEditDialog = remember(context, getStringResource, currentLanguage) {
        { amount: Double, category: String, type: String, note: String, dateLong: Long ->
            transactionToEdit?.let {
                viewModel.updateTransaction(it.id, amount, category, type, note, dateLong)
            }
            showEditDialog = false
            Toast.makeText(
                context,
                if (currentLanguage == "bn") "লেনদেন সফলভাবে আপডেট করা হয়েছে" else "Transaction updated successfully!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    val onEditTransaction = remember {
        { transaction: Transaction ->
            transactionToEdit = transaction
            showEditDialog = true
        }
    }
    val onThemeChange = remember { { theme: String -> viewModel.setTheme(theme) } }
    val onDismissAuthDialog = remember { { showAuthDialog = false } }
    val onSignInSuccess = remember(context) {
        { email: String ->
            viewModel.loginWithGoogle(email)
            showAuthDialog = false
            Toast.makeText(context, "Google Signed in as $email. Synchronizing...", Toast.LENGTH_LONG).show()
        }
    }
    val onSignOut = remember(context) {
        {
            viewModel.logout()
            showAuthDialog = false
            Toast.makeText(context, "Logged out. Switched to offline guest mode.", Toast.LENGTH_SHORT).show()
        }
    }

    // Ensure initial guest data are seeded for preview/starter and check for updates
    LaunchedEffect(Unit) {
        viewModel.seedGuestDataIfNeeded()
        checkForUpdatesAsync(context) { info, _ ->
            if (info != null && info.hasUpdate) {
                launchUpdateInfo = info
            }
        }
    }

    MeshBackground {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                val isDark = isAppDark()
                val textGradient = remember(isDark) {
                    Brush.linearGradient(
                        colors = if (isDark) {
                            listOf(Color(0xFF34D399), Color(0xFF60A5FA))
                        } else {
                            listOf(Color(0xFF047857), Color(0xFF1D4ED8))
                        }
                    )
                }
                val btnShape = RoundedCornerShape(14.dp)
                val btnSize = 44.dp
                val btnBg = if (isDark) Color(0xFF1E293B).copy(alpha = 0.45f) else Color(0xFFFFFFFF).copy(alpha = 0.85f)
                val btnBorderBrush = remember(isDark) {
                    Brush.linearGradient(
                        colors = if (isDark) {
                            listOf(Color(0xFF334155).copy(alpha = 0.4f), Color(0xFF10B981).copy(alpha = 0.15f))
                        } else {
                            listOf(Color(0xFFE2E8F0).copy(alpha = 0.8f), Color(0xFF10B981).copy(alpha = 0.20f))
                        }
                    )
                }
                val logoGradient = remember {
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF059669), // Emerald
                            Color(0xFF2563EB)  // Electric blue
                        )
                    )
                }
                val settingsBtnGradient = remember {
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF10B981),
                            Color(0xFF3B82F6)
                        )
                    )
                }
                val infoBtnGradient = remember {
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF3B82F6),
                            Color(0xFF6366F1)
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Mobile Wallet Pill (Main Title) - Stretches from left to right
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier
                                .weight(1f)
                                .height(btnSize)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isDark) Color(0xFF1E293B).copy(alpha = 0.45f)
                                    else Color(0xFFFFFFFF).copy(alpha = 0.85f)
                                )
                                .border(
                                    border = BorderStroke(
                                        width = 1.dp,
                                        brush = btnBorderBrush
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .padding(horizontal = 12.dp)
                        ) {
                            // Logo background box (slightly more compact)
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(
                                        brush = logoGradient,
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Wallet,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = getStringResource(R.string.app_name),
                                fontWeight = FontWeight.Black,
                                style = androidx.compose.ui.text.TextStyle(
                                    brush = textGradient,
                                    fontSize = 20.sp,
                                    letterSpacing = 2.5.sp
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            // Three-dimensional active status dot
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF10B981), CircleShape)
                                    .border(1.5.dp, if (isDark) Color(0xFF111827) else Color.White, CircleShape)
                            )
                        }

                        // 1. Settings button (Exactly identical size, icon-only)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .size(btnSize)
                                .clip(btnShape)
                                .background(btnBg)
                                .border(BorderStroke(1.dp, btnBorderBrush), btnShape)
                                .android16Clickable { showSettingsDialog = true }
                                .testTag("settings_header_pill")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        brush = settingsBtnGradient,
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings Icon",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // 2. Info/Developer Credits button (Exactly identical size, icon-only)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .size(btnSize)
                                .clip(btnShape)
                                .background(btnBg)
                                .border(BorderStroke(1.dp, btnBorderBrush), btnShape)
                                .android16Clickable { onShowDeveloperCredit() }
                                .testTag("developer_credit_button")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        brush = infoBtnGradient,
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = "Developer Credit Info",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp)
                        .verticalScroll(androidx.compose.foundation.rememberScrollState())
                ) {
            // Minimalist Monthly balance summary card
            DashboardSummaryCard(
                stats = stats,
                transactions = transactions,
                dailyLimit = dailyExpenseLimit,
                onUpdateLimit = { limit -> viewModel.setDailyExpenseLimit(limit) },
                lang = currentLanguage,
                getString = getStringResource
            )

            // Dynamic grid of 6 Actions & Tools (as requested!)
            DashboardActionsGrid(
                currentLanguage = currentLanguage,
                onAddTransaction = { showAddDialog = true },
                onViewHistory = { showAllTransactionsDialog = true },
                onBajarList = { showBajarListDialog = true },
                onDebtsLoans = { showDebtListDialog = true },
                onDownloadPdf = { showReportPreviewDialog = true },
                onImportBackup = { filePickerLauncher.launch("application/json") }
            )

            Spacer(modifier = Modifier.height(24.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
            Spacer(modifier = Modifier.height(140.dp))
        }
    }
    }
    }

    // Launch automatic update dialog if update exists on startup
    if (launchUpdateInfo != null) {
        AppUpdateDialog(
            updateInfo = launchUpdateInfo!!,
            currentLanguage = currentLanguage,
            onDismiss = { launchUpdateInfo = null }
        )
    }

    // Dialog 0: Developer Credit Dialog
    if (showReportPreviewDialog) {
        ReportPreviewDialog(
            transactions = transactions,
            currentLanguage = currentLanguage,
            onDownloadPdf = {
                val pdfUri = generateFinancePdfReport(context, transactions, currentLanguage)
                if (pdfUri != null) {
                    Toast.makeText(
                        context,
                        if (currentLanguage == "bn") "রিপোর্ট পিডিএফ আকারে সেভ করা হয়েছে!" else "Report printed to Downloads folder as PDF successfully!",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        if (currentLanguage == "bn") "পিডিএফ তৈরি করতে ব্যর্থ হয়েছে" else "Failed to generate PDF Report",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onSharePdf = {
                val pdfUri = generateFinancePdfReport(context, transactions, currentLanguage)
                if (pdfUri != null) {
                    triggerPdfShareIntent(context, pdfUri)
                } else {
                    Toast.makeText(
                        context,
                        if (currentLanguage == "bn") "পিডিএফ তৈরি করতে ব্যর্থ হয়েছে" else "Failed to generate PDF Report",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onDismiss = { showReportPreviewDialog = false }
        )
    }

    if (showDeveloperCreditDialog) {
        DeveloperCreditDialog(
            currentUser = currentUser,
            lang = currentLanguage,
            onDismiss = onDismissDeveloperCredit
        )
    }

    // Dialog 0.4: Transactions History Screen / Dialog
    if (showAllTransactionsDialog) {
        TransactionsHistoryDialog(
            transactions = transactions,
            currentLanguage = currentLanguage,
            getString = getStringResource,
            onDeleteTransaction = onDeleteTransaction,
            onEditTransaction = onEditTransaction,
            onDismiss = { showAllTransactionsDialog = false }
        )
    }

    // Dialog 0.5: Settings Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            currentLanguage = currentLanguage,
            selectedTheme = selectedTheme,
            onThemeChange = onThemeChange,
            onLanguageClick = {
                showLanguageDialog = true
            },
            onImportBackup = {
                showSettingsDialog = false
                filePickerLauncher.launch("application/json")
            },
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = currentLanguage,
            onLanguageSelect = { langCode ->
                viewModel.setLanguage(langCode)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    // Dialog 1: Add Transaction Dialog
    if (showAddDialog) {
        AddTransactionDialog(
            onDismiss = onDismissAddDialog,
            onSave = onSaveAddDialog,
            getString = getStringResource,
            lang = currentLanguage
        )
    }

    // Dialog 1.5: Edit Transaction Dialog
    if (showEditDialog) {
        transactionToEdit?.let { trans ->
            EditTransactionDialog(
                transaction = trans,
                onDismiss = onDismissEditDialog,
                onSave = onSaveEditDialog,
                getString = getStringResource,
                lang = currentLanguage
            )
        }
    }

    // Delete Confirmation Dialog
    if (transactionToDelete != null) {
        DeleteConfirmationDialog(
            transaction = transactionToDelete!!,
            lang = currentLanguage,
            onConfirm = {
                transactionToDelete?.let { trans ->
                    viewModel.deleteTransaction(trans)
                    Toast.makeText(context, getStringResource(R.string.delete_confirm), Toast.LENGTH_SHORT).show()
                }
                transactionToDelete = null
            },
            onDismiss = {
                transactionToDelete = null
            }
        )
    }

    // Dialog 2: Interactive Google Auth Dialog (With Real Feedback Sync)
    if (showAuthDialog) {
        GoogleAuthDialog(
            currentUser = currentUser,
            selectedTheme = selectedTheme,
            onThemeChange = onThemeChange,
            onDismiss = onDismissAuthDialog,
            onSignInSuccess = onSignInSuccess,
            onSignOut = onSignOut,
            getString = getStringResource,
            lang = currentLanguage
        )
    }

    // Active action menus are now displayed as a beautiful, smooth anchored dropdown directly above the FAB to avoid layout overlap issues.

    // Dialog 3: Bajar List Dialog
    if (showBajarListDialog) {
        BajarListDialog(
            bajarItems = bItems,
            lang = currentLanguage,
            onAdd = { name, quantity -> viewModel.addBajarItem(name, quantity) },
            onToggle = { item, isChecked -> viewModel.toggleBajarItemCompletion(item, isChecked) },
            onDelete = { item -> viewModel.deleteBajarItem(item) },
            onFinishShopping = { totalCost, completedList ->
                viewModel.finishBajarShopping(totalCost, completedList)
                showBajarListDialog = false
            },
            onDismiss = { showBajarListDialog = false }
        )
    }

    // Dialog 4: Debts & Lending Tracker Dialog
    if (showDebtListDialog) {
        DebtListDialog(
            debtRecords = debtRecords,
            lang = currentLanguage,
            onAddDebt = { name, amt, dir, note -> viewModel.addDebtRecord(name, amt, dir, note) },
            onSettleDebt = { record, amount -> viewModel.settleDebtRecord(record, amount) },
            onDeleteDebt = { record -> viewModel.deleteDebtRecord(record) },
            onUpdateDebt = { record -> viewModel.updateDebtRecord(record) },
            onDismiss = { showDebtListDialog = false }
        )
    }
}

// User sign-in status indicator banner with a beautiful glass style
@Composable
fun UserSessionBanner(
    currentUser: String?,
    getString: (Int) -> String,
    onSignInClick: () -> Unit
) {
    val isDark = isAppDark()
    val containerBg = if (currentUser != null) {
        if (isDark) Color(0x2660A5FA) else Color(0x5993C5FD) // Light blue glass 35%
    } else {
        if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else Color(0x33FFFFFF) // Standard clear glass 20%
    }
    
    val textColor = if (currentUser != null) {
        if (isDark) Color(0xFF93C5FD) else Color(0xFF1E40AF) // Dark Blue-800
    } else {
        if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF475569) // Slate-600
    }

    val borderColor = if (currentUser != null) {
        if (isDark) Color(0x4060A5FA) else Color(0x6693C5FD)
    } else {
        if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f) else Color(0x4DFFFFFF)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .android16Clickable(shape = RoundedCornerShape(16.dp)) { onSignInClick() },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (currentUser != null) {
                    GoogleAvatar(email = currentUser, sizeDp = 24.dp)
                } else {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = "Sync state icon",
                        tint = textColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (currentUser != null) {
                        val templateStr = getString(R.string.user_google_account)
                        String.format(templateStr, currentUser)
                    } else {
                        getString(R.string.google_guest_account)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (currentUser == null) {
                Text(
                    text = getString(R.string.google_signin),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF2563EB),
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

// High-contrast dashboard balance summary card using beautiful frosted glassmorphism elements
@Composable
fun DashboardSummaryCard(
    stats: Stats,
    transactions: List<Transaction>,
    dailyLimit: Float,
    onUpdateLimit: (Float) -> Unit,
    lang: String,
    getString: (Int) -> String
) {
    val isDark = isAppDark()
    val cardBorder = if (isDark) Color(0xFF475569).copy(alpha = 0.5f) else Color(0xFFCBD5E1).copy(alpha = 0.7f)

    // Premium shimmery background gradient matching global modern design theme
    val premiumGradient = remember(isDark) {
        if (isDark) {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF1E293B), // Premium Slate Grey
                    Color(0xFF0F172A), // Midnight Dark
                    Color(0xFF0F2633)  // Midnight Blue-Teal Tint
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFFFFFF), // Pure Crisp White
                    Color(0xFFF0FDFA)  // Super light soft Teal-Mint Breeze
                )
            )
        }
    }

    // Cache currency formatting strings in remember block to maximize rendering performance
    val formattedTotal = remember(stats.balance, lang) {
        formatCurrency(stats.balance, lang)
    }
    val formattedIncome = remember(stats.totalIncome, lang) {
        formatCurrency(stats.totalIncome, lang)
    }
    val formattedExpense = remember(stats.totalExpense, lang) {
        formatCurrency(stats.totalExpense, lang)
    }

    val incomeBorder = if (isDark) Color(0x4D34D399) else Color(0x4D059669)
    val incomeBg = if (isDark) Color(0x1510B981) else Color(0x1A10B981)
    val incomeIconTint = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
    val incomeLabel = if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857)
    val incomeValue = if (isDark) Color(0xFFA7F3D0) else Color(0xFF064E3B)

    val expenseBorder = if (isDark) Color(0x4DFB7185) else Color(0x4DE11D48)
    val expenseBg = if (isDark) Color(0x15F43F5E) else Color(0x1AF43F5E)
    val expenseIconTint = if (isDark) Color(0xFFFB7185) else Color(0xFFDC2626)
    val expenseLabel = if (isDark) Color(0xFFFDA4AF) else Color(0xFFB91C1C)
    val expenseValue = if (isDark) Color(0xFFFECDD3) else Color(0xFF7F1D1D)

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val cardPadding = if (screenWidthDp < 360) 14.dp else 24.dp
    val dynamicFontSize = if (screenWidthDp < 340) 24.sp else if (screenWidthDp < 380) 32.sp else 40.sp
    val spaceGap = if (screenWidthDp < 360) 12.dp else 24.dp
    val incomeExpenseGap = if (screenWidthDp < 360) 8.dp else 16.dp

    // Calculate Today's Cost details safely
    val todayCost = remember(transactions) {
        transactions
            .filter { it.type == "EXPENSE" && isSameDay(it.dateLong, System.currentTimeMillis()) }
            .sumOf { it.amount }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(premiumGradient, RoundedCornerShape(32.dp))
            .testTag("dashboard_summary_card"),
        shape = RoundedCornerShape(32.dp),
        border = BorderStroke(1.dp, cardBorder),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(cardPadding)
        ) {
            // Remaining Balance Section
            Text(
                text = getString(R.string.remaining_balance_label).uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF0D9488)
                )
            )
            Spacer(modifier = Modifier.height(4.dp))

            val hasDecimals = formattedTotal.contains(".") && lang != "bn"

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Start
            ) {
                if (hasDecimals) {
                    val wholePart = formattedTotal.substringBefore(".")
                    val decimalPart = formattedTotal.substringAfter(".")
                    Text(
                        text = wholePart,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = dynamicFontSize,
                            letterSpacing = (-1).sp
                        ),
                        color = if (stats.balance >= 0) MaterialTheme.colorScheme.onSurface else Color(0xFFE11D48)
                    )
                    Text(
                        text = ".$decimalPart",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = if (screenWidthDp < 360) 16.sp else 20.sp
                        ),
                        color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF475569),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                } else {
                    Text(
                        text = formattedTotal,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = dynamicFontSize,
                            letterSpacing = (-1).sp
                        ),
                        color = if (stats.balance >= 0) MaterialTheme.colorScheme.onSurface else Color(0xFFE11D48)
                    )
                }
            }

            // Daily cost and expense progress bar, placed under Remaining Balance as requested!
            val isBn = lang == "bn"
            val formattedTodayCost = remember(todayCost, lang) {
                formatCurrency(todayCost, lang)
            }
            val formattedLimit = remember(dailyLimit, lang) {
                if (lang == "bn") {
                    convertDigitsToBengali(dailyLimit.toInt().toString())
                } else {
                    dailyLimit.toInt().toString()
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    1.dp, 
                    if (isDark) Color(0xFF334155).copy(alpha = 0.4f) else Color(0xFFCBD5E1).copy(alpha = 0.5f)
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF0F172A).copy(alpha = 0.40f) else Color(0xFFF1F5F9).copy(alpha = 0.60f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isBn) "দৈনিক খরচ (আজ)" else "TODAY'S COST",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = formattedTodayCost,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = if (todayCost >= dailyLimit && dailyLimit > 0) Color(0xFFEF4444) else if (isDark) Color.White else Color(0xFF0F172A)
                                )
                            )
                        }

                        // Daily limit config input
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isBn) "খরচের সীমা (লিমিট)" else "DAILY LIMIT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))

                            var isEditing by remember { mutableStateOf(false) }
                            var limitInputText by remember(dailyLimit) { mutableStateOf(dailyLimit.toInt().toString()) }

                            if (isEditing) {
                                BasicTextField(
                                    value = limitInputText,
                                    onValueChange = { newValue ->
                                        // Allow only digits
                                        val filtered = newValue.filter { it.isDigit() }
                                        limitInputText = filtered
                                    },
                                    textStyle = TextStyle(
                                        color = if (isDark) Color.White else Color(0xFF0F172A),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.End
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    modifier = Modifier
                                        .width(70.dp)
                                        .background(
                                            color = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 3.dp),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            isEditing = false
                                            val cleanEng = limitInputText.trim()
                                            val newLimit = cleanEng.toFloatOrNull() ?: 150f
                                            onUpdateLimit(newLimit)
                                        }
                                    ),
                                    singleLine = true
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .android16Clickable(shape = RoundedCornerShape(6.dp)) {
                                            isEditing = true
                                        }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isBn) "৳$formattedLimit" else "$$formattedLimit",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Limit",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val limitSafe = if (dailyLimit <= 0) 150f else dailyLimit
                    val progress = (todayCost / limitSafe).toFloat().coerceIn(0f, 1f)

                    val progressColor = when {
                        todayCost >= limitSafe -> Color(0xFFEF4444) // alarm red
                        todayCost >= limitSafe * 0.8f -> Color(0xFFF59E0B) // amber warning
                        else -> Color(0xFF10B981) // safe green
                    }

                    val animatedProgress by animateFloatAsState(
                        targetValue = progress,
                        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow),
                        label = "daily_expense_progress"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(
                                color = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                                shape = CircleShape
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress)
                                .fillMaxHeight()
                                .background(color = progressColor, shape = CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val percentageStr = remember(todayCost, limitSafe, lang) {
                            val pct = ((todayCost / limitSafe) * 100).toInt()
                            if (lang == "bn") {
                                convertDigitsToBengali("$pct%")
                            } else {
                                "$pct%"
                            }
                        }
                        Text(
                            text = if (todayCost >= limitSafe) {
                                if (isBn) "সীমা অতিক্রম করেছেন!" else "Limit Exceeded!"
                            } else {
                                if (isBn) "বাজেটের $percentageStr খরচ হয়েছে" else "Spent $percentageStr of budget"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = progressColor,
                                fontSize = 11.sp
                            )
                        )

                        val remainingAmount = kotlin.math.max(0.0, limitSafe - todayCost)
                        val formattedRemaining = remember(remainingAmount, lang) {
                            if (lang == "bn") {
                                convertDigitsToBengali(remainingAmount.toInt().toString())
                            } else {
                                remainingAmount.toInt().toString()
                            }
                        }

                        Text(
                            text = if (todayCost >= limitSafe) {
                                if (isBn) "০ টাকা বাকি" else "0 left"
                            } else {
                                if (isBn) "বাকি ৳$formattedRemaining" else "Left $$formattedRemaining"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Black,
                                color = if (todayCost >= limitSafe) Color(0xFFEF4444) else if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(spaceGap))

            // Income / Expense Side-by-Side Panels with tinted background borders matching HTML
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(incomeExpenseGap)
            ) {
                // Income Column (Emerald theme)
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, incomeBorder),
                    colors = CardDefaults.cardColors(containerColor = incomeBg)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Income Arrow",
                                tint = incomeIconTint,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = getString(R.string.total_income_label).uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = incomeLabel
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formattedIncome,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = incomeValue
                        )
                    }
                }

                // Expense Column (Rose theme)
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, expenseBorder),
                    colors = CardDefaults.cardColors(containerColor = expenseBg)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Expense Arrow",
                                tint = expenseIconTint,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = getString(R.string.total_expense_label).uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = expenseLabel
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formattedExpense,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = expenseValue
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionsHeaderRow(
    currentLanguage: String,
    getString: (Int) -> String,
    onSeeAllClick: () -> Unit
) {
    val isDark = isAppDark()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.ReceiptLong,
                contentDescription = null,
                tint = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF2563EB),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = getString(R.string.recent_transactions_header),
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // See All history button
        TextButton(
            onClick = onSeeAllClick,
            modifier = Modifier.testTag("see_all_transactions_button")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF2563EB)
                )
                Text(
                    text = if (currentLanguage == "bn") "সব দেখুন" else "See All",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF2563EB)
                )
            }
        }
    }
}

fun generateFinancePdfReport(
    context: android.content.Context,
    transactions: List<Transaction>,
    lang: String
): android.net.Uri? {
    try {
        val pdfDocument = android.graphics.pdf.PdfDocument()
        val paint = android.graphics.Paint()
        
        val pageWidth = 595
        val pageHeight = 842
        
        val totalIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val netBalance = totalIncome - totalExpense
        
        val itemsPerPage = 22
        val totalPages = if (transactions.isEmpty()) 1 else ((transactions.size - 1) / itemsPerPage) + 1
        
        val dateFmt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val generatedTime = dateFmt.format(java.util.Date())
        
        for (pageIndex in 0 until totalPages) {
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            
            paint.style = android.graphics.Paint.Style.STROKE
            paint.color = android.graphics.Color.LTGRAY
            paint.strokeWidth = 1f
            canvas.drawRect(20f, 20f, (pageWidth - 20).toFloat(), (pageHeight - 20).toFloat(), paint)
            
            var yPos = 45f
            
            if (pageIndex == 0) {
                paint.style = android.graphics.Paint.Style.FILL
                paint.color = android.graphics.Color.rgb(37, 99, 235)
                paint.textSize = 20f
                paint.isFakeBoldText = true
                val title = if (lang == "bn") "অর্থ ব্যবস্থাপক প্রতিবেদন" else "Finance Manager Report"
                canvas.drawText(title, 40f, yPos, paint)
                
                yPos += 20f
                paint.textSize = 10f
                paint.isFakeBoldText = false
                paint.color = android.graphics.Color.GRAY
                val subtitle = if (lang == "bn") "তৈরির সময়: $generatedTime" else "Generated: $generatedTime"
                canvas.drawText(subtitle, 40f, yPos, paint)
                
                yPos += 30f
                
                paint.style = android.graphics.Paint.Style.FILL
                paint.color = android.graphics.Color.rgb(243, 244, 246)
                canvas.drawRoundRect(40f, yPos, (pageWidth - 40).toFloat(), yPos + 60f, 8f, 8f, paint)
                
                paint.style = android.graphics.Paint.Style.STROKE
                paint.color = android.graphics.Color.rgb(209, 213, 219)
                canvas.drawRoundRect(40f, yPos, (pageWidth - 40).toFloat(), yPos + 60f, 8f, 8f, paint)
                
                yPos += 20f
                paint.style = android.graphics.Paint.Style.FILL
                paint.textSize = 11f
                paint.isFakeBoldText = true
                
                paint.color = android.graphics.Color.rgb(5, 150, 105)
                val incomeStr = if (lang == "bn") "মোট আয়: ৳${String.format("%.2f", totalIncome)}" else "Total Income: \$${String.format("%.2f", totalIncome)}"
                canvas.drawText(incomeStr, 60f, yPos, paint)
                
                paint.color = android.graphics.Color.rgb(220, 38, 38)
                val expenseStr = if (lang == "bn") "মোট ব্যয়: ৳${String.format("%.2f", totalExpense)}" else "Total Expense: \$${String.format("%.2f", totalExpense)}"
                canvas.drawText(expenseStr, 240f, yPos, paint)
                
                paint.color = if (netBalance >= 0) android.graphics.Color.rgb(37, 99, 235) else android.graphics.Color.rgb(220, 38, 38)
                val balStr = if (lang == "bn") "অবশিষ্ট ব্যালেন্স: ৳${String.format("%.2f", netBalance)}" else "Net Balance: \$${String.format("%.2f", netBalance)}"
                canvas.drawText(balStr, 410f, yPos, paint)
                
                yPos += 40f
            } else {
                yPos = 40f
            }
            
            paint.style = android.graphics.Paint.Style.FILL
            paint.color = android.graphics.Color.rgb(71, 85, 105)
            canvas.drawRect(40f, yPos, (pageWidth - 40).toFloat(), yPos + 22f, paint)
            
            paint.color = android.graphics.Color.WHITE
            paint.textSize = 10f
            paint.isFakeBoldText = true
            
            val colDate = if (lang == "bn") "তারিখ" else "Date"
            val colType = if (lang == "bn") "ধরণ" else "Type"
            val colCat = if (lang == "bn") "ক্যাটাগরি" else "Category"
            val colNote = if (lang == "bn") "মন্তব্য" else "Note"
            val colAmt = if (lang == "bn") "পরিমাণ" else "Amount"
            
            canvas.drawText(colDate, 50f, yPos + 15f, paint)
            canvas.drawText(colType, 130f, yPos + 15f, paint)
            canvas.drawText(colCat, 190f, yPos + 15f, paint)
            canvas.drawText(colNote, 290f, yPos + 15f, paint)
            canvas.drawText(colAmt, 490f, yPos + 15f, paint)
            
            yPos += 22f
            
            paint.isFakeBoldText = false
            paint.color = android.graphics.Color.BLACK
            
            val startIndex = pageIndex * itemsPerPage
            val endIndex = minOf(startIndex + itemsPerPage, transactions.size)
            
            val simpleDateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            
            for (i in startIndex until endIndex) {
                val transaction = transactions[i]
                
                if (i % 2 == 0) {
                    paint.style = android.graphics.Paint.Style.FILL
                    paint.color = android.graphics.Color.rgb(249, 250, 251)
                    canvas.drawRect(40f, yPos, (pageWidth - 40).toFloat(), yPos + 20f, paint)
                }
                
                paint.style = android.graphics.Paint.Style.FILL
                paint.color = android.graphics.Color.BLACK
                paint.textSize = 9f
                
                val dateStr = simpleDateFormat.format(java.util.Date(transaction.dateLong))
                canvas.drawText(dateStr, 50f, yPos + 14f, paint)
                
                val typeStr = transaction.type
                paint.color = if (typeStr == "INCOME") android.graphics.Color.rgb(5, 150, 105) else android.graphics.Color.rgb(220, 38, 38)
                paint.isFakeBoldText = true
                val cleanType = if (typeStr == "INCOME") (if (lang == "bn") "আয়" else "INCOME") else (if (lang == "bn") "ব্যয়" else "EXPENSE")
                canvas.drawText(cleanType, 130f, yPos + 14f, paint)
                paint.isFakeBoldText = false
                paint.color = android.graphics.Color.BLACK
                
                val catRawVal = transaction.category
                val catIdInt = catRawVal.toIntOrNull()
                val categoryName = if (catIdInt != null) {
                    try {
                        context.getString(catIdInt)
                    } catch (e: Exception) {
                        "Other"
                    }
                } else {
                    catRawVal
                }
                val safeCat = if (categoryName.length > 15) categoryName.take(13) + ".." else categoryName
                canvas.drawText(safeCat, 190f, yPos + 14f, paint)
                
                val noteDisplay = if (transaction.note.isNotBlank()) transaction.note else "-"
                val safeNote = if (noteDisplay.length > 25) noteDisplay.take(22) + ".." else noteDisplay
                canvas.drawText(safeNote, 290f, yPos + 14f, paint)
                
                paint.isFakeBoldText = true
                val amtStr = if (lang == "bn") "৳${String.format("%.2f", transaction.amount)}" else "\$${String.format("%.2f", transaction.amount)}"
                paint.color = if (typeStr == "INCOME") android.graphics.Color.rgb(5, 150, 105) else android.graphics.Color.rgb(220, 38, 38)
                canvas.drawText(amtStr, 490f, yPos + 14f, paint)
                paint.isFakeBoldText = false
                paint.color = android.graphics.Color.BLACK
                
                paint.style = android.graphics.Paint.Style.STROKE
                paint.color = android.graphics.Color.rgb(229, 231, 235)
                canvas.drawLine(40f, yPos + 20f, (pageWidth - 40).toFloat(), yPos + 20f, paint)
                
                yPos += 20f
            }
            
            paint.style = android.graphics.Paint.Style.FILL
            paint.color = android.graphics.Color.GRAY
            paint.textSize = 9f
            val footerStr = "${pageIndex + 1} / $totalPages"
            canvas.drawText(footerStr, (pageWidth / 2f) - 10f, (pageHeight - 30).toFloat(), paint)
            
            pdfDocument.finishPage(page)
        }
        
        val contentResolver = context.contentResolver
        val pdfDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "Finance_Report_$pdfDate.pdf")
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
        }
        
        val pdfUri = contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        if (pdfUri != null) {
            contentResolver.openOutputStream(pdfUri)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            pdfDocument.close()
            return pdfUri
        }
        
        pdfDocument.close()
    } catch (e: Exception) {
        android.util.Log.e("PDF_GEN", "Failed to compile/write PDF report: ${e.message}", e)
    }
    return null
}

fun triggerPdfShareIntent(context: android.content.Context, uri: android.net.Uri) {
    try {
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Finance Report PDF"))
    } catch (e: Exception) {
        android.util.Log.e("PDF_SHARE", "Failed to start share sheet: ${e.message}")
    }
}

// Tab 1: Transaction List View Content
@Composable
fun TransactionsTabContent(
    transactions: List<Transaction>,
    currentLanguage: String,
    getString: (Int) -> String,
    onDeleteTransaction: (Transaction) -> Unit,
    onEditTransaction: (Transaction) -> Unit
) {
    val isDark = isAppDark()
    if (transactions.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Wallet,
                    contentDescription = "Empty list graphic",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = getString(R.string.empty_transactions_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }
    } else {
        val listState = androidx.compose.foundation.lazy.rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 84.dp, top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = transactions,
                key = { it.id },
                contentType = { "transaction_item" }
            ) { transaction ->
                TransactionListItem(
                    transaction = transaction,
                    lang = currentLanguage,
                    getString = getString,
                    isDark = isDark,
                    onDelete = onDeleteTransaction,
                    onEdit = onEditTransaction
                )
            }
        }
    }
}

// Single Transaction List Row
// Thread-safe cached static gradients and colors to avoid GC allocation stutter during scrolling lists
private val OptDarkExpenseBrush = Brush.linearGradient(
    colors = listOf(Color(0xFF1E293B).copy(alpha = 0.55f), Color(0xFF271313).copy(alpha = 0.45f))
)
private val OptDarkIncomeBrush = Brush.linearGradient(
    colors = listOf(Color(0xFF1E293B).copy(alpha = 0.55f), Color(0xFF0B2921).copy(alpha = 0.45f))
)
private val OptLightExpenseBrush = Brush.linearGradient(
    colors = listOf(Color.White.copy(alpha = 0.85f), Color(0xFFFFF1F2).copy(alpha = 0.70f))
)
private val OptLightIncomeBrush = Brush.linearGradient(
    colors = listOf(Color.White.copy(alpha = 0.85f), Color(0xFFF0FDF4).copy(alpha = 0.70f))
)
private val OptDarkBorderColor = Color(0xFF334155).copy(alpha = 0.5f)
private val OptLightBorderColor = Color(0xFFE2E8F0).copy(alpha = 0.7f)
private val OptCardShape = RoundedCornerShape(20.dp)

@Composable
fun TransactionListItem(
    transaction: Transaction,
    lang: String,
    getString: (Int) -> String,
    isDark: Boolean,
    onDelete: (Transaction) -> Unit,
    onEdit: (Transaction) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val isExpense = remember(transaction.type) { transaction.type == "EXPENSE" }
    val categoryName = remember(transaction.category, lang) {
        val resId = transaction.category.toIntOrNull()
        if (resId != null) getString(resId) else transaction.category
    }

    // Map categories to modern design colors for icons
    val categoryColor = remember(transaction.category) {
        val resId = transaction.category.toIntOrNull()
        when (resId) {
            R.string.category_salary -> Color(0xFF10B981) // Emerald Green
            R.string.category_food -> Color(0xFFF59E0B) // Golden Amber
            R.string.category_groceries -> Color(0xFF84CC16) // Lime Green
            R.string.category_utilities -> Color(0xFF6366F1) // Indigo Blue
            R.string.category_entertainment -> Color(0xFFEC4899) // Hot Pink
            R.string.category_transport -> Color(0xFF06B6D4) // Cyan
            R.string.category_freelance -> Color(0xFF3B82F6) // Electric Blue
            else -> Color(0xFF64748B) // Slate Gray
        }
    }

    // Cache formatted timestamp and amount strings as stable remembered instances for butter-smooth 120Hz scrolling
    val formattedDate = remember(transaction.dateLong, lang) {
        formatDate(transaction.dateLong, lang)
    }
    val formattedAmount = remember(transaction.amount, lang, isExpense) {
        "${if (isExpense) "-" else "+"}${formatCurrency(transaction.amount, lang)}"
    }

    val itemBg = remember(isDark, isExpense, categoryColor) {
        if (isDark) {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF1E293B).copy(alpha = 0.45f), // Slate gradient
                    categoryColor.copy(alpha = 0.08f)    // Subtle contextual glow of category color
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFFFFFF).copy(alpha = 0.85f), // Solid white base
                    categoryColor.copy(alpha = 0.05f)    // Light contextual glow
                )
            )
        }
    }
    val itemBorder = remember(isDark, categoryColor) {
        if (isDark) {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF334155).copy(alpha = 0.4f),
                    categoryColor.copy(alpha = 0.20f)
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFFE2E8F0).copy(alpha = 0.8f),
                    categoryColor.copy(alpha = 0.15f)
                )
            )
        }
    }
    val titleColor = remember(isDark) { if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A) }
    val noteColor = remember(isDark) { if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569) }
    val dateColor = remember(isDark) { if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B) }
    val amountColor = remember(isDark, isExpense) { if (isExpense) Color(0xFFEF4444) else (if (isDark) Color(0xFF14B8A6) else Color(0xFF0D9488)) }

    val categoryIcon = remember(transaction.category) {
        val resId = transaction.category.toIntOrNull()
        when (resId) {
            R.string.category_salary -> Icons.Default.AccountBalanceWallet
            R.string.category_food -> Icons.Default.Restaurant
            R.string.category_groceries -> Icons.Default.ShoppingBasket
            R.string.category_utilities -> Icons.Default.FlashOn
            R.string.category_entertainment -> Icons.Default.Movie
            R.string.category_transport -> Icons.Default.DirectionsCar
            R.string.category_freelance -> Icons.Default.LaptopMac
            else -> Icons.Default.Category
        }
    }

    val iconBoxBg = remember(categoryColor) { categoryColor.copy(alpha = 0.15f) }
    val iconBoxShape = remember { RoundedCornerShape(14.dp) }

    val radialBrush = remember(categoryColor) {
        Brush.radialGradient(
            colors = listOf(categoryColor.copy(alpha = 0.22f), categoryColor.copy(alpha = 0.04f))
        )
    }
    val linearBorderBrush = remember(categoryColor) {
        Brush.linearGradient(
            colors = listOf(categoryColor.copy(alpha = 0.45f), Color.Transparent)
        )
    }

    val cardShape = OptCardShape
    val cardBorder = remember(itemBorder) { BorderStroke(1.dp, itemBorder) }
    val cardInteractionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(itemBg, cardShape)
            .border(cardBorder, cardShape)
            .android16Clickable(
                shape = cardShape,
                onClick = { expanded = !expanded }
            )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Modern vertical accent pill with glowing gradient representing transaction type
                val accentGradient = remember(amountColor) {
                    Brush.verticalGradient(
                        colors = listOf(amountColor, amountColor.copy(alpha = 0.35f))
                    )
                }
                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .height(32.dp)
                        .clip(CircleShape)
                        .background(accentGradient)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Glowing circular ring category emblem for elite modern finish
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            brush = radialBrush,
                            shape = CircleShape
                        )
                        .border(
                            width = 1.dp,
                            brush = linearBorderBrush,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = categoryName,
                        tint = categoryColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Text section
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = categoryName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = titleColor
                    )
                    if (transaction.note.isNotBlank()) {
                        Text(
                            text = transaction.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = noteColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = dateColor
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Cost / Income receipt-style pill badge and expand indicator
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    val amountBg = remember(isDark, isExpense, categoryColor) {
                        if (isExpense) {
                            Color(0xFFEF4444).copy(alpha = 0.08f)
                        } else {
                            if (isDark) Color(0xFF14B8A6).copy(alpha = 0.08f) else Color(0xFF0D9488).copy(alpha = 0.08f)
                        }
                    }
                    val amountBorder = remember(isDark, isExpense) {
                        if (isExpense) {
                            Color(0xFFEF4444).copy(alpha = 0.15f)
                        } else {
                            if (isDark) Color(0xFF14B8A6).copy(alpha = 0.15f) else Color(0xFF0D9488).copy(alpha = 0.15f)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(amountBg)
                            .border(1.dp, amountBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = formattedAmount,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                            fontWeight = FontWeight.Black,
                            color = amountColor,
                            letterSpacing = 0.3.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier
                            .size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand Options",
                            tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = tween(durationMillis = 180)) + fadeIn(animationSpec = tween(180)),
                exit = shrinkVertically(animationSpec = tween(durationMillis = 150)) + fadeOut(animationSpec = tween(150))
            ) {
                    // Expandable panel with quick options
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.padding(bottom = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = if (lang == "bn") "লেনদেনের তথ্য ও অপশনসমূহ" else "Transaction Details & Options",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        if (transaction.note.isNotBlank()) {
                            Text(
                                text = "${if (lang == "bn") "মন্তব্য / নোট:" else "Note / Description:"} ${transaction.note}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF334155)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val context = LocalContext.current
                            if (transaction.note.isNotBlank()) {
                                val copyInt = remember { MutableInteractionSource() }
                                Button(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("Transaction Note", transaction.note)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, if (lang == "bn") "নোট কপি করা হয়েছে" else "Note copied to clipboard", Toast.LENGTH_SHORT).show()
                                    },
                                    interactionSource = copyInt,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .android16ScalePress(copyInt)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (lang == "bn") "সব তথ্য কপি" else "Copy Note", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            val editBtnInt = remember { MutableInteractionSource() }
                            Button(
                                onClick = { onEdit(transaction) },
                                interactionSource = editBtnInt,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .android16ScalePress(editBtnInt)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (lang == "bn") "সম্পাদনা" else "Edit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            val deleteBtnInt = remember { MutableInteractionSource() }
                            Button(
                                onClick = { onDelete(transaction) },
                                interactionSource = deleteBtnInt,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .android16ScalePress(deleteBtnInt)
                            ) {
                                 Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (lang == "bn") "মুছে ফেলুন" else "Delete", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

// Tab 2: Category wise summarize statistics
@Composable
fun SummaryTabContent(
    transactions: List<Transaction>,
    currentLanguage: String,
    getString: (Int) -> String
) {
    val isDark = isAppDark()
    // Categorize transactions and sum expenses / income
    val summaryData = remember(transactions, currentLanguage) {
        val incomeCategories = mutableMapOf<Int, Double>()
        val expenseCategories = mutableMapOf<Int, Double>()

        for (t in transactions) {
            val resId = t.category.toIntOrNull() ?: R.string.category_other
            val amount = t.amount
            if (t.type == "INCOME") {
                incomeCategories[resId] = (incomeCategories[resId] ?: 0.0) + amount
            } else {
                expenseCategories[resId] = (expenseCategories[resId] ?: 0.0) + amount
            }
        }

        Pair(
            incomeCategories.toList().sortedByDescending { it.second },
            expenseCategories.toList().sortedByDescending { it.second }
        )
    }

    val (incomeSummaries, expenseSummaries) = summaryData
    var activeFilter by remember { mutableStateOf("EXPENSE") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Toggle filtering for summaries (Show spent category stats vs revenue category stats)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { activeFilter = "EXPENSE" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeFilter == "EXPENSE") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (activeFilter == "EXPENSE") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text(getString(R.string.expense_type), fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { activeFilter = "INCOME" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeFilter == "INCOME") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (activeFilter == "INCOME") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text(getString(R.string.income_type), fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val activeList = if (activeFilter == "EXPENSE") expenseSummaries else incomeSummaries
        
        if (activeList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getString(R.string.empty_transactions_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            val totalSum = remember(activeList) { activeList.sumOf { it.second } }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 84.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(
                    items = activeList,
                    key = { it.first },
                    contentType = { "category_summary" }
                ) { (categoryResId, totalAmount) ->
                    val percentage = if (totalSum > 0) (totalAmount / totalSum * 100) else 0.0
                    CategorySummaryItem(
                        categoryResId = categoryResId,
                        amount = totalAmount,
                        percentage = percentage,
                        lang = currentLanguage,
                        getString = getString,
                        isDark = isDark,
                        isExpense = activeFilter == "EXPENSE"
                    )
                }
            }
        }
    }
}

// Individual categorized statistics item
@Composable
fun CategorySummaryItem(
    categoryResId: Int,
    amount: Double,
    percentage: Double,
    lang: String,
    getString: (Int) -> String,
    isDark: Boolean,
    isExpense: Boolean
) {
    val categoryName = remember(categoryResId, lang) { getString(categoryResId) }

    // Cache formatting computations inside remember states to guarantee stable 120Hz scrolling frames
    val formattedAmount = remember(amount, lang) {
        formatCurrency(amount, lang)
    }
    val formattedPercentage = remember(percentage) {
        String.format(Locale.US, "%.1f%%", percentage)
    }

    val colorAccent = remember(categoryResId) {
        when (categoryResId) {
            R.string.category_salary -> Color(0xFF10B981) // Emerald Green
            R.string.category_food -> Color(0xFFF59E0B) // Golden Amber
            R.string.category_groceries -> Color(0xFF84CC16) // Lime Green
            R.string.category_utilities -> Color(0xFF6366F1) // Indigo Blue
            R.string.category_entertainment -> Color(0xFFEC4899) // Hot Pink
            R.string.category_transport -> Color(0xFF06B6D4) // Cyan
            R.string.category_freelance -> Color(0xFF3B82F6) // Electric Blue
            else -> Color(0xFF64748B) // Slate Gray
        }
    }

    val itemBg = remember(isDark) { if (isDark) Color(0xFF1E293B).copy(alpha = 0.55f) else Color.White.copy(alpha = 0.72f) }
    val itemBorder = remember(isDark) { if (isDark) Color(0xFF475569).copy(alpha = 0.5f) else Color(0xFFCBD5E1).copy(alpha = 0.7f) }
    val titleColor = remember(isDark) { if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A) }
    val amountColor = remember(isDark, isExpense) { if (isExpense) Color(0xFFEF4444) else { if (isDark) Color(0xFF34D399) else Color(0xFF0D9488) } }
    val progressTrackBg = remember(isDark) { if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0) }
    val percentTextColor = remember(isDark) { if (isDark) Color(0xFF94A3B8) else Color(0xFF475569) }

    Card(
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, itemBorder), // white 35% border
        colors = CardDefaults.cardColors(containerColor = itemBg), // white 60% glass background
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(colorAccent)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = categoryName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = titleColor
                    )
                }

                Text(
                    text = formattedAmount,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleMedium,
                    color = amountColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Percentage track line
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Animated percentage bar tracker
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(progressTrackBg) // Subtle elegant track
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = (percentage / 100).toFloat())
                            .clip(CircleShape)
                            .background(colorAccent)
                    )
                }

                Text(
                    text = formattedPercentage,
                    style = MaterialTheme.typography.labelMedium,
                    color = percentTextColor,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.width(44.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

private data class CalendarDay(
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean,
    val isPreviousMonth: Boolean = false,
    val isNextMonth: Boolean = false,
    val timestamp: Long
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun BeautifulInteractiveCalendarDialog(
    initialSelectedDateMillis: Long,
    lang: String,
    onDismissRequest: () -> Unit,
    onDateSelected: (Long) -> Unit
) {
    var selectedDateMillis by remember { mutableStateOf(initialSelectedDateMillis) }
    
    val baseCalendar = java.util.Calendar.getInstance()
    baseCalendar.timeInMillis = selectedDateMillis
    
    // Viewed month & year
    var displayedMonth by remember { mutableStateOf(baseCalendar.get(java.util.Calendar.MONTH)) }
    var displayedYear by remember { mutableStateOf(baseCalendar.get(java.util.Calendar.YEAR)) }
    
    // For smooth sliding animation of month & grid
    var slideDirectionRight by remember { mutableStateOf(true) }
    // Combined state for month and year that triggers transition in AnimatedContent
    val displayedPeriod = remember(displayedMonth, displayedYear) { Pair(displayedMonth, displayedYear) }

    val isDark = isAppDark()
    val dialogSurfaceBg = if (isDark) Color(0xFF1E293B).copy(alpha = 0.92f) else Color.White.copy(alpha = 0.95f)
    val dialogBorder = if (isDark) Color(0xFF475569).copy(alpha = 0.5f) else Color(0xFFCBD5E1).copy(alpha = 0.7f)

    val englishMonths = remember {
        listOf(
            "January", "February", "March", "April", "May", "June", 
            "July", "August", "September", "October", "November", "December"
        )
    }

    val bengaliMonths = remember {
        listOf(
            "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন", 
            "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
        )
    }

    Dialog(onDismissRequest = onDismissRequest) {
        AnimatedDialogContent {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 16.dp)
                    .testTag("custom_calendar_dialog"),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, dialogBorder),
                colors = CardDefaults.cardColors(
                    containerColor = dialogSurfaceBg,
                    contentColor = if (isDark) Color.White else Color(0xFF0F172A)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    // --- HEADER SECTION ---
                    Text(
                        text = if (lang == "bn") "তারিখ নির্বাচন করুন" else "SELECT DATE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Selected Date Header
                    Text(
                        text = formatDate(selectedDateMillis, lang),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = dialogBorder.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // --- MONTH / YEAR NAVIGATION WITH Horizontally Sliding ANIMATION ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous Month Button
                        IconButton(
                            onClick = {
                                slideDirectionRight = false
                                if (displayedMonth == 0) {
                                    displayedMonth = 11
                                    displayedYear -= 1
                                } else {
                                    displayedMonth -= 1
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Previous Month",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Month Year Text sliding content
                        AnimatedContent(
                            targetState = displayedPeriod,
                            transitionSpec = {
                                if (slideDirectionRight) {
                                    (slideInHorizontally { w -> w } + fadeIn()).togetherWith(slideOutHorizontally { w -> -w } + fadeOut())
                                } else {
                                    (slideInHorizontally { w -> -w } + fadeIn()).togetherWith(slideOutHorizontally { w -> w } + fadeOut())
                                } using SizeTransform(clip = false)
                            },
                            label = "month_year_animation"
                        ) { period ->
                            val m = period.first
                            val y = period.second
                            val monthName = if (lang == "bn") bengaliMonths[m] else englishMonths[m]
                            val yearNameStr = if (lang == "bn") convertDigitsToBengali(y.toString()) else y.toString()
                            
                            Text(
                                text = "$monthName $yearNameStr",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Next Month Button
                        IconButton(
                            onClick = {
                                slideDirectionRight = true
                                if (displayedMonth == 11) {
                                    displayedMonth = 0
                                    displayedYear += 1
                                } else {
                                    displayedMonth += 1
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Next Month",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- CALENDAR GRID ROWS ---
                    // Week Days Headers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        val daysHeader = if (lang == "bn") {
                            listOf("রবি", "সোম", "মঙ্গল", "বুধ", "বৃহঃ", "শুক্র", "শনি")
                        } else {
                            listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
                        }
                        daysHeader.forEach { header ->
                            Box(
                                modifier = Modifier.size(36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = header,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Days Grid within AnimatedContent to simulate elegant swipe transition
                    AnimatedContent(
                        targetState = displayedPeriod,
                        transitionSpec = {
                            if (slideDirectionRight) {
                                (slideInHorizontally { w -> w } + fadeIn()).togetherWith(slideOutHorizontally { w -> -w } + fadeOut())
                            } else {
                                (slideInHorizontally { w -> -w } + fadeIn()).togetherWith(slideOutHorizontally { w -> w } + fadeOut())
                            } using SizeTransform(clip = false)
                        },
                        label = "calendar_days_animation"
                    ) { period ->
                        val m = period.first
                        val y = period.second
                        
                        val tempCal = java.util.Calendar.getInstance().apply {
                            clear()
                            set(java.util.Calendar.DAY_OF_MONTH, 1)
                            set(java.util.Calendar.MONTH, m)
                            set(java.util.Calendar.YEAR, y)
                        }
                        val firstDayOfWeek = tempCal.get(java.util.Calendar.DAY_OF_WEEK)
                        val maxDaysInMonth = tempCal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
                        
                        val prevMonthCal = java.util.Calendar.getInstance().apply {
                            clear()
                            set(java.util.Calendar.DAY_OF_MONTH, 1)
                            set(java.util.Calendar.MONTH, if (m == 0) 11 else m - 1)
                            set(java.util.Calendar.YEAR, if (m == 0) y - 1 else y)
                        }
                        val maxDaysInPrevMonth = prevMonthCal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
                        
                        val emptyPreSlots = firstDayOfWeek - 1
                        val daysList = mutableListOf<CalendarDay>()
                        val cellCal = java.util.Calendar.getInstance().apply {
                            clear()
                            set(java.util.Calendar.HOUR_OF_DAY, 12)
                            set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }

                        // Fill previous month trailing days
                        for (i in emptyPreSlots - 1 downTo 0) {
                            val d = maxDaysInPrevMonth - i
                            val pm = if (m == 0) 11 else m - 1
                            val py = if (m == 0) y - 1 else y
                            cellCal.set(py, pm, d)
                            daysList.add(CalendarDay(d, isCurrentMonth = false, isPreviousMonth = true, timestamp = cellCal.timeInMillis))
                        }

                        // Fill current month days
                        for (d in 1..maxDaysInMonth) {
                            cellCal.set(y, m, d)
                            daysList.add(CalendarDay(d, isCurrentMonth = true, timestamp = cellCal.timeInMillis))
                        }

                        // Fill next month leading days
                        val remaining = 42 - daysList.size
                        for (d in 1..remaining) {
                            val nm = if (m == 11) 0 else m + 1
                            val ny = if (m == 11) y + 1 else y
                            cellCal.set(ny, nm, d)
                            daysList.add(CalendarDay(d, isCurrentMonth = false, isNextMonth = true, timestamp = cellCal.timeInMillis))
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (row in 0 until 6) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    for (col in 0 until 7) {
                                        val index = row * 7 + col
                                        val day = daysList[index]
                                        
                                        val isSelected = isSameDay(day.timestamp, selectedDateMillis)
                                        val isToday = isSameDay(day.timestamp, System.currentTimeMillis())
                                        
                                        val scaleFactor by animateFloatAsState(
                                            targetValue = if (isSelected) 1.15f else 1.0f,
                                            animationSpec = spring(
                                                dampingRatio = 0.6f,
                                                stiffness = Spring.StiffnessMediumLow
                                            ),
                                            label = "day_cell_scale"
                                        )

                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .graphicsLayer {
                                                    scaleX = scaleFactor
                                                    scaleY = scaleFactor
                                                }
                                                .background(
                                                    color = when {
                                                        isSelected -> MaterialTheme.colorScheme.primary
                                                        isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                                        else -> Color.Transparent
                                                    },
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .border(
                                                    width = if (isToday && !isSelected) 1.5.dp else 0.dp,
                                                    color = if (isToday && !isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .android16Clickable(shape = RoundedCornerShape(10.dp)) {
                                                    selectedDateMillis = day.timestamp
                                                    val clickedCal = java.util.Calendar.getInstance().apply {
                                                        timeInMillis = day.timestamp
                                                    }
                                                    displayedMonth = clickedCal.get(java.util.Calendar.MONTH)
                                                    displayedYear = clickedCal.get(java.util.Calendar.YEAR)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val dayStr = if (lang == "bn") convertDigitsToBengali(day.dayOfMonth.toString()) else day.dayOfMonth.toString()
                                            
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = dayStr,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                                                    color = when {
                                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                                        !day.isCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                                        isToday -> MaterialTheme.colorScheme.primary
                                                        else -> MaterialTheme.colorScheme.onSurface
                                                    }
                                                )
                                                if (isToday && isSelected) {
                                                    Spacer(modifier = Modifier.height(1.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.dp)
                                                            .background(MaterialTheme.colorScheme.onPrimary, RoundedCornerShape(50))
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- ACTION FOOTER BUTTONS ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismissRequest,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                        ) {
                            Text(
                                text = if (lang == "bn") "বাতিল" else "Cancel",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Button(
                            onClick = {
                                onDateSelected(selectedDateMillis)
                                onDismissRequest()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (lang == "bn") "ঠিক আছে" else "OK",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun isSameDay(millis1: Long, millis2: Long): Boolean {
    val cal1 = java.util.Calendar.getInstance().apply { timeInMillis = millis1 }
    val cal2 = java.util.Calendar.getInstance().apply { timeInMillis = millis2 }
    return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
           cal1.get(java.util.Calendar.MONTH) == cal2.get(java.util.Calendar.MONTH) &&
           cal1.get(java.util.Calendar.DAY_OF_MONTH) == cal2.get(java.util.Calendar.DAY_OF_MONTH)
}

private fun convertDigitsToBengali(input: String): String {
    val sb = java.lang.StringBuilder()
    for (i in 0 until input.length) {
        val char = input[i]
        if (char in '0'..'9') {
            sb.append(bengaliDigits[char - '0'])
        } else {
            sb.append(char)
        }
    }
    return sb.toString()
}

// Form logic to record new transactions
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onSave: (amount: Double, category: String, type: String, note: String, dateLong: Long) -> Unit,
    getString: (Int) -> String,
    lang: String
) {
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("EXPENSE") }
    var customCategoryText by remember { mutableStateOf("") }

    val categories = remember {
        listOf(
            R.string.category_food,
            R.string.category_groceries,
            R.string.category_salary,
            R.string.category_freelance,
            R.string.category_utilities,
            R.string.category_entertainment,
            R.string.category_transport,
            R.string.category_other
        )
    }

    var selectedCategory by remember { mutableStateOf(categories[0]) }
    var dateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    val formattedDate = remember(dateMillis, lang) {
        formatDate(dateMillis, lang)
    }
    var showDatePicker by remember { mutableStateOf(false) }

    var labelErrorText by remember { mutableStateOf("") }

    val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp
    Dialog(onDismissRequest = onDismiss) {
        AnimatedDialogContent {
            val isDark = isAppDark()
            val dialogSurfaceBg = if (isDark) Color(0xFF1E293B).copy(alpha = 0.92f) else Color.White.copy(alpha = 0.95f)
            val dialogBorder = if (isDark) Color(0xFF475569).copy(alpha = 0.5f) else Color(0xFFCBD5E1).copy(alpha = 0.7f)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
                    .heightIn(max = screenHeight - 64.dp)
                    .padding(horizontal = 8.dp)
                    .testTag("add_transaction_dialog_surface"),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, dialogBorder),
                colors = CardDefaults.cardColors(
                    containerColor = dialogSurfaceBg,
                    contentColor = if (isDark) Color.White else Color(0xFF0F172A)
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                        .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                Text(
                    text = getString(R.string.add_transaction_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                // Select Transaction Type (Income VS Expense tabs block)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp)
                ) {
                    val expenseSelected = selectedType == "EXPENSE"
                    val isDark = isAppDark()
                    
                    val expenseColor = if (expenseSelected) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    } else {
                        ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val incomeColor = if (!expenseSelected) {
                        ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0x3334D399) else Color(0xFFE8F5E9), // Light green container
                            contentColor = if (isDark) Color(0xFF34D399) else Color(0xFF1B5E20) // Dark green text
                        )
                    } else {
                        ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            selectedType = "EXPENSE"
                            // Set suitable default category matching expense type
                            selectedCategory = R.string.category_food
                        },
                        colors = expenseColor,
                        modifier = Modifier
                            .weight(1f)
                            .android16ScalePress()
                            .testTag("type_expense_tab"),
                        shape = RoundedCornerShape(8.dp),
                        elevation = null
                    ) {
                        Text(getString(R.string.expense_type), fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            selectedType = "INCOME"
                            // Set suitable default category matching income type
                            selectedCategory = R.string.category_salary
                        },
                        colors = incomeColor,
                        modifier = Modifier
                            .weight(1f)
                            .android16ScalePress()
                            .testTag("type_income_tab"),
                        shape = RoundedCornerShape(8.dp),
                        elevation = null
                    ) {
                        Text(getString(R.string.income_type), fontWeight = FontWeight.Bold)
                    }
                }

                // Amount Field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        if (it.isEmpty() || it.toDoubleOrNull() != null || it.matches(Regex("^\\d+\\.?\\d{0,2}$"))) {
                            amountText = it
                            labelErrorText = ""
                        }
                    },
                    label = { Text(getString(R.string.amount_label)) },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .android16ScalePress()
                        .testTag("amount_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Render error helper text if any
                if (labelErrorText.isNotBlank()) {
                    Text(
                        text = labelErrorText,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Category selection buttons mapping the list
                Column {
                    Text(
                        text = getString(R.string.category_label) + ":",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Simple wrapping grid items
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        maxItemsInEachRow = 3
                    ) {
                        categories.forEach { categoryId ->
                            val isChosen = selectedCategory == categoryId
                            FilterChip(
                                selected = isChosen,
                                onClick = { selectedCategory = categoryId },
                                label = { Text(getString(categoryId), fontSize = 12.sp) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("cat_chip_$categoryId")
                            )
                        }
                    }

                    if (selectedCategory == R.string.category_other) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customCategoryText,
                            onValueChange = { customCategoryText = it },
                            label = { Text(if (lang == "bn") "কাস্টম ক্যাটাগরি লিখুন" else "Enter custom category") },
                            placeholder = { Text(if (lang == "bn") "যেমন: উপহার, বোনাস ইত্যাদি" else "e.g., Gift, Bonus, etc.") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .android16ScalePress()
                                .testTag("custom_category_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Note (Optional)
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text(getString(R.string.note_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .android16ScalePress()
                        .testTag("note_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Date Picker row info (Uses M3 DatePickerDialog to choose correct date)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .android16Clickable {
                            showDatePicker = true
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Calendar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = getString(R.string.date_label),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (showDatePicker) {
                    BeautifulInteractiveCalendarDialog(
                        initialSelectedDateMillis = dateMillis,
                        lang = lang,
                        onDismissRequest = { showDatePicker = false },
                        onDateSelected = { selectedDate ->
                            dateMillis = selectedDate
                        }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Actions Button Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val cancelInt = remember { MutableInteractionSource() }
                    TextButton(
                        onClick = onDismiss,
                        interactionSource = cancelInt,
                        modifier = Modifier
                            .weight(1f)
                            .android16ScalePress(cancelInt)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold)
                    }

                    val saveInt = remember { MutableInteractionSource() }
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull()
                            if (amt == null || amt <= 0) {
                                labelErrorText = getString(R.string.error_empty_amount)
                            } else {
                                val finalCategory = if (selectedCategory == R.string.category_other && customCategoryText.isNotBlank()) {
                                    customCategoryText.trim()
                                } else {
                                    selectedCategory.toString()
                                }
                                onSave(amt, finalCategory, selectedType, noteText, dateMillis)
                            }
                        },
                        interactionSource = saveInt,
                        modifier = Modifier
                            .weight(1.5f)
                            .android16ScalePress(saveInt)
                            .testTag("save_transaction_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(getString(R.string.add_button), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionDialog(
    transaction: com.example.data.Transaction,
    onDismiss: () -> Unit,
    onSave: (amount: Double, category: String, type: String, note: String, dateLong: Long) -> Unit,
    getString: (Int) -> String,
    lang: String
) {
    var amountText by remember { mutableStateOf(transaction.amount.toString()) }
    var noteText by remember { mutableStateOf(transaction.note) }
    var selectedType by remember { mutableStateOf(transaction.type) }

    val categories = remember {
        listOf(
            R.string.category_food,
            R.string.category_groceries,
            R.string.category_salary,
            R.string.category_freelance,
            R.string.category_utilities,
            R.string.category_entertainment,
            R.string.category_transport,
            R.string.category_other
        )
    }

    var customCategoryText by remember(transaction.category) {
        val catId = transaction.category.toIntOrNull()
        mutableStateOf(if (catId == null) transaction.category else "")
    }

    var selectedCategory by remember(transaction.category) {
        val catId = transaction.category.toIntOrNull()
        mutableStateOf(catId ?: R.string.category_other)
    }
    var dateMillis by remember { mutableStateOf(transaction.dateLong) }
    val formattedDate = remember(dateMillis, lang) {
        formatDate(dateMillis, lang)
    }
    var showDatePicker by remember { mutableStateOf(false) }

    var labelErrorText by remember { mutableStateOf("") }

    val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp
    Dialog(onDismissRequest = onDismiss) {
        AnimatedDialogContent {
            val isDark = isAppDark()
            val dialogSurfaceBg = if (isDark) Color(0xFF1E293B).copy(alpha = 0.92f) else Color.White.copy(alpha = 0.95f)
            val dialogBorder = if (isDark) Color(0xFF475569).copy(alpha = 0.5f) else Color(0xFFCBD5E1).copy(alpha = 0.7f)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
                    .heightIn(max = screenHeight - 64.dp)
                    .padding(horizontal = 8.dp)
                    .testTag("edit_transaction_dialog_surface"),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, dialogBorder),
                colors = CardDefaults.cardColors(
                    containerColor = dialogSurfaceBg,
                    contentColor = if (isDark) Color.White else Color(0xFF0F172A)
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                        .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (lang == "bn") "লেনদেন সম্পাদনা" else getString(R.string.edit_transaction_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    // Select Transaction Type (Income VS Expense tabs block)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(4.dp)
                    ) {
                        val expenseSelected = selectedType == "EXPENSE"
                        val isDark = isAppDark()
                        
                        val expenseColor = if (expenseSelected) {
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        } else {
                            ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        val incomeColor = if (!expenseSelected) {
                            ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color(0x3334D399) else Color(0xFFE8F5E9), // Light green container
                                contentColor = if (isDark) Color(0xFF34D399) else Color(0xFF1B5E20) // Dark green text
                            )
                        } else {
                            ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = {
                                selectedType = "EXPENSE"
                                selectedCategory = R.string.category_food
                            },
                            colors = expenseColor,
                            modifier = Modifier
                                .weight(1f)
                                .android16ScalePress()
                                .testTag("edit_type_expense_tab"),
                            shape = RoundedCornerShape(8.dp),
                            elevation = null
                        ) {
                            Text(getString(R.string.expense_type), fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                selectedType = "INCOME"
                                selectedCategory = R.string.category_salary
                            },
                            colors = incomeColor,
                            modifier = Modifier
                                .weight(1f)
                                .android16ScalePress()
                                .testTag("edit_type_income_tab"),
                            shape = RoundedCornerShape(8.dp),
                            elevation = null
                        ) {
                            Text(getString(R.string.income_type), fontWeight = FontWeight.Bold)
                        }
                    }

                    // Amount Field
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = {
                            if (it.isEmpty() || it.toDoubleOrNull() != null || it.matches(Regex("^\\d+\\.?\\d{0,2}$"))) {
                                amountText = it
                                labelErrorText = ""
                            }
                        },
                        label = { Text(getString(R.string.amount_label)) },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .android16ScalePress()
                            .testTag("edit_amount_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Render error helper text if any
                    if (labelErrorText.isNotBlank()) {
                        Text(
                            text = labelErrorText,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Category selection buttons mapping the list
                    Column {
                        Text(
                            text = getString(R.string.category_label) + ":",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        // Simple wrapping grid items
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            maxItemsInEachRow = 3
                        ) {
                            categories.forEach { categoryId ->
                                val isChosen = selectedCategory == categoryId
                                FilterChip(
                                    selected = isChosen,
                                    onClick = { selectedCategory = categoryId },
                                    label = { Text(getString(categoryId), fontSize = 12.sp) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("edit_cat_chip_$categoryId")
                                )
                            }
                        }

                        if (selectedCategory == R.string.category_other) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = customCategoryText,
                                onValueChange = { customCategoryText = it },
                                label = { Text(if (lang == "bn") "কাস্টম ক্যাটাগরি লিখুন" else "Enter custom category") },
                                placeholder = { Text(if (lang == "bn") "যেমন: উপহার, বোনাস ইত্যাদি" else "e.g., Gift, Bonus, etc.") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .android16ScalePress()
                                    .testTag("edit_custom_category_input"),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    // Note (Optional)
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text(getString(R.string.note_label)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .android16ScalePress()
                            .testTag("edit_note_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Date Picker row info
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .android16Clickable {
                                showDatePicker = true
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Calendar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = getString(R.string.date_label),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (showDatePicker) {
                        BeautifulInteractiveCalendarDialog(
                            initialSelectedDateMillis = dateMillis,
                            lang = lang,
                            onDismissRequest = { showDatePicker = false },
                            onDateSelected = { selectedDate ->
                                dateMillis = selectedDate
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Actions Button Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val editCancelInt = remember { MutableInteractionSource() }
                        TextButton(
                            onClick = onDismiss,
                            interactionSource = editCancelInt,
                            modifier = Modifier
                                .weight(1f)
                                .android16ScalePress(editCancelInt)
                        ) {
                            Text(if (lang == "bn") "বাতিল" else "Cancel", fontWeight = FontWeight.Bold)
                        }

                        val editSaveInt = remember { MutableInteractionSource() }
                        Button(
                            onClick = {
                                val amt = amountText.toDoubleOrNull()
                                if (amt == null || amt <= 0) {
                                    labelErrorText = getString(R.string.error_empty_amount)
                                } else {
                                    val finalCategory = if (selectedCategory == R.string.category_other && customCategoryText.isNotBlank()) {
                                        customCategoryText.trim()
                                    } else {
                                        selectedCategory.toString()
                                    }
                                    onSave(amt, finalCategory, selectedType, noteText, dateMillis)
                                }
                            },
                            interactionSource = editSaveInt,
                            modifier = Modifier
                                .weight(1.5f)
                                .android16ScalePress(editSaveInt)
                                .testTag("edit_save_transaction_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (lang == "bn") "পরিবর্তন সংরক্ষণ" else getString(R.string.edit_button), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GoogleLogoIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sizePx = size.width
        val strokeWidth = sizePx * 0.18f
        val radius = (sizePx - strokeWidth) / 2f
        val cx = sizePx / 2f
        val cy = sizePx / 2f

        // Draw 4 gorgeous segments: Red (top), Yellow (left), Green (bottom), Blue (right with horizontal bar)
        // 1. Red (top): 180 to 315
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = 180f,
            sweepAngle = 135f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
        )
        // 2. Yellow (left): 90 to 180
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 90f,
            sweepAngle = 90f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
        )
        // 3. Green (bottom): 0 to 90
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 0f,
            sweepAngle = 90f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
        )
        // 4. Blue (right segment with the horizontal inside bar): 270 to 360/0
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = 270f,
            sweepAngle = 90f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
        )

        // Horizontal bar inside for the 'G'
        drawRect(
            color = Color(0xFF4285F4),
            topLeft = androidx.compose.ui.geometry.Offset(cx, cy - strokeWidth / 2f),
            size = androidx.compose.ui.geometry.Size(cx, strokeWidth)
        )
    }
}

// Google Authentication Flow Simulator (Provides immersive Interactive syncing)
@Composable
fun GoogleAuthDialog(
    currentUser: String?,
    selectedTheme: String,
    onThemeChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSignInSuccess: (email: String) -> Unit,
    onSignOut: () -> Unit,
    getString: (Int) -> String,
    lang: String
) {
    var isVerifyingAccount by remember { mutableStateOf<String?>(null) }
    var emailInput by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf(false) }
    var isChoosingAccount by remember { mutableStateOf(false) }
    var isEnteringCustomEmail by remember { mutableStateOf(false) }

    LaunchedEffect(isVerifyingAccount) {
        val email = isVerifyingAccount
        if (email != null) {
            kotlinx.coroutines.delay(1200)
            onSignInSuccess(email)
            isVerifyingAccount = null
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        AnimatedDialogContent {
            val isDark = isAppDark()
            val dialogSurfaceBg = if (isDark) Color(0xFF1E293B).copy(alpha = 0.92f) else Color.White.copy(alpha = 0.95f)
            val dialogBorder = if (isDark) Color(0xFF475569).copy(alpha = 0.5f) else Color(0xFFCBD5E1).copy(alpha = 0.7f)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .testTag("google_auth_dialog_surface"),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, dialogBorder),
                colors = CardDefaults.cardColors(
                    containerColor = dialogSurfaceBg,
                    contentColor = if (isDark) Color.White else Color(0xFF0F172A)
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Secure login header",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Google Identity Services",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Secure your financial records across all Android platforms using your Google identity.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (isVerifyingAccount != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(36.dp))
                            Text(
                                text = "Establishing secure connection...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        if (currentUser == null) {
                            if (isChoosingAccount) {
                                // Google high-fidelity Account Selector popup within dialog bounds
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (lang == "bn") "একটি অ্যাকাউন্ট বেছে নিন" else "Choose an account",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (lang == "bn") "Finance Manager-এ সাইন-ইন করতে" else "to continue to Finance Manager",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        thickness = 1.dp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Primary pre-configured real user account option (Sakib Islam as from metadata)
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .android16Clickable(shape = RoundedCornerShape(12.dp)) {
                                                isVerifyingAccount = "sakibislam94433@gmail.com"
                                                isChoosingAccount = false
                                            }
                                            .testTag("google_account_sakib_islam"),
                                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            GoogleAvatar(email = "sakibislam94433@gmail.com", sizeDp = 40.dp)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Sakib Islam",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "sakibislam94433@gmail.com",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Secondary account selection card
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .android16Clickable(shape = RoundedCornerShape(12.dp)) {
                                                isChoosingAccount = false
                                                isEnteringCustomEmail = true
                                            }
                                            .testTag("google_account_custom"),
                                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = if (lang == "bn") "অন্য গুগল অ্যাকাউন্ট ব্যবহার করুন" else "Use another Google Account",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        thickness = 1.dp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = if (lang == "bn") "ফাইন্যান্স ডাটা ক্লাউডে সুরক্ষিত রাখতে গুগল আপনার ছবি, ইমেইল এড্রেস এবং গুগল ড্রাইভে সুরক্ষিত ব্যাকআপ ফোল্ডারের এক্সেস শেয়ার করবে।" else "Google will securely share your profile details and private AppData storage spaces in Google Drive to auto-sync your wallet transactions.",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                        textAlign = TextAlign.Center,
                                        lineHeight = 15.sp
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    TextButton(
                                        onClick = { isChoosingAccount = false },
                                        modifier = Modifier.testTag("google_auth_back")
                                    ) {
                                        Text(
                                            text = if (lang == "bn") "বাতিল করুন" else "Cancel",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            } else if (isEnteringCustomEmail) {
                                // High-Polished Manual Email TextInput fallback option
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = if (lang == "bn") "গুগল অ্যাকাউন্ট ইমেইল" else "Enter Google Email",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )

                                    OutlinedTextField(
                                        value = emailInput,
                                        onValueChange = {
                                            emailInput = it
                                            emailError = false
                                        },
                                        label = { Text(if (lang == "bn") "গুগল ইমেইল এড্রেস" else "Google Email Account") },
                                        placeholder = { Text("example@gmail.com") },
                                        isError = emailError,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("google_email_input"),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Email
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    if (emailError) {
                                        Text(
                                            text = if (lang == "bn") "অনুগ্রহ করে একটি সঠিক গুগল ইমেইল এড্রেস প্রবেশ করান।" else "Please enter a valid Google email address.",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier
                                                .align(Alignment.Start)
                                                .padding(start = 4.dp, top = 2.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { 
                                                isEnteringCustomEmail = false
                                                isChoosingAccount = true
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(if (lang == "bn") "ফিরে যান" else "Back")
                                        }

                                        Button(
                                            onClick = {
                                                val trimmed = emailInput.trim()
                                                if (trimmed.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()) {
                                                    isVerifyingAccount = trimmed
                                                    isEnteringCustomEmail = false
                                                } else {
                                                    emailError = true
                                                }
                                            },
                                            modifier = Modifier
                                                .weight(1.5f)
                                                .height(48.dp)
                                                .testTag("google_signin_submit"),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary
                                            )
                                        ) {
                                            Text(if (lang == "bn") "সাইন-ইন" else "Sign In", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else {
                                // Default Google Brand Sign-In Button (Official Spec styled representation)
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    val googleBtnBg = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White
                                    val googleBtnBorder = if (isDark) Color.Transparent else MaterialTheme.colorScheme.outlineVariant
                                    val googleBtnTextColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF1F2937)

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .android16Clickable(shape = RoundedCornerShape(26.dp)) {
                                                isChoosingAccount = true
                                            }
                                            .testTag("google_brand_signin_button"),
                                        shape = RoundedCornerShape(26.dp),
                                        border = BorderStroke(1.dp, googleBtnBorder),
                                        colors = CardDefaults.cardColors(containerColor = googleBtnBg),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            GoogleLogoIcon(modifier = Modifier.size(22.dp))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = if (lang == "bn") "গুগল অ্যাকাউন্ট দিয়ে সাইন-ইন করুন" else "Sign In with Google Account",
                                                fontWeight = FontWeight.Black,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = googleBtnTextColor
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = if (lang == "bn") "নিরাপদ ক্লাউড ব্যাকআপ সক্রিয় করতে গুগল অ্যাকাউন্ট ব্যবহার করুন" else "Uses your Google identity to trigger automatic Google Drive appdata backup.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        } else {
                            // If already logged in, show account and provide "Sign Out" option
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        contentAlignment = Alignment.BottomEnd,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    ) {
                                        GoogleAvatar(email = currentUser, sizeDp = 72.dp)
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF2E7D32))
                                                .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Active",
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Connected Successfully",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF1B5E20)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = currentUser,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = onSignOut,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signout_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(getString(R.string.google_signout), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Theme Selection Block - Bilingual
                    Text(
                        text = getString(R.string.theme_section_title),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val themes = listOf(
                        Triple("system", getString(R.string.theme_system), Icons.Default.Settings),
                        Triple("light", getString(R.string.theme_light), Icons.Default.WbSunny),
                        Triple("dark", getString(R.string.theme_dark), Icons.Default.NightsStay)
                    )

                    themes.forEach { (themeId, label, icon) ->
                        val isSelected = selectedTheme == themeId
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .android16Clickable(shape = RoundedCornerShape(12.dp)) { onThemeChange(themeId) }
                                .testTag("theme_option_$themeId"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                }
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onDismiss) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
}

private val bnLocale = Locale("bn", "BD")
private val enLocale = Locale.ENGLISH
private val hiLocale = Locale("hi", "IN")
private val arLocale = Locale("ar", "SA")
private val esLocale = Locale("es", "ES")
private val sdfMap = java.util.concurrent.ConcurrentHashMap<String, SimpleDateFormat>()
private val bengaliDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
private val devanagariDigits = charArrayOf('०', '१', '२', '३', '४', '५', '६', '७', '८', '९')
private val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

// Decimal and date formatters helper
fun formatDate(timestamp: Long, lang: String): String {
    val date = Date(timestamp)
    val sdf = sdfMap.getOrPut(lang) {
        val locale = when (lang) {
            "bn" -> bnLocale
            "hi" -> hiLocale
            "ar" -> arLocale
            "es" -> esLocale
            else -> enLocale
        }
        SimpleDateFormat("dd MMM, yyyy", locale)
    }
    return sdf.format(date)
}

fun formatCurrency(amount: Double, lang: String): String {
    val formatted = String.format(Locale.US, "%,.2f", amount)
    return when (lang) {
        "bn" -> {
            val sb = StringBuilder(formatted.length + 2)
            sb.append("৳")
            for (i in 0 until formatted.length) {
                val char = formatted[i]
                if (char in '0'..'9') {
                    sb.append(bengaliDigits[char - '0'])
                } else {
                    sb.append(char)
                }
            }
            sb.toString()
        }
        "hi" -> {
            val sb = StringBuilder(formatted.length + 2)
            sb.append("₹")
            for (i in 0 until formatted.length) {
                val char = formatted[i]
                if (char in '0'..'9') {
                    sb.append(devanagariDigits[char - '0'])
                } else {
                    sb.append(char)
                }
            }
            sb.toString()
        }
        "ar" -> {
            val sb = StringBuilder(formatted.length + 6)
            for (i in 0 until formatted.length) {
                val char = formatted[i]
                if (char in '0'..'9') {
                    sb.append(arabicDigits[char - '0'])
                } else {
                    sb.append(char)
                }
            }
            sb.append(" د.إ")
            sb.toString()
        }
        "es" -> {
            "€$formatted"
        }
        else -> {
            "$$formatted"
        }
    }
}

// Support extension for picking hex values cleanly
fun Color.Companion.fromHex(hex: String): Color {
    val cleanHex = hex.removePrefix("#")
    val argb = when (cleanHex.length) {
        6 -> "FF$cleanHex"
        8 -> cleanHex
        else -> "FFFFFFFF"
    }
    return Color(android.graphics.Color.parseColor("#$argb"))
}

// Visual layout support for even padding density
fun symmetricPadding(horizontal: androidx.compose.ui.unit.Dp, vertical: androidx.compose.ui.unit.Dp) = PaddingValues(horizontal, vertical, horizontal, vertical)

// Soft, beautiful and highly performant background
@Composable
fun MeshBackground(content: @Composable () -> Unit) {
    val isDark = isAppDark()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isDark) {
                        listOf(
                            Color(0xFF090D1A), // Ultra deep midnight dark
                            Color(0xFF0F172A), // Rich midnight slate
                            Color(0xFF020617)  // Deep deep blue
                        )
                    } else {
                        listOf(
                            Color.White,       // Pure crisp white light mode background
                            Color(0xFFF8FAFC)  // Clean soft slate gray light mode base
                        )
                    }
                )
            )
    ) {
        content()
    }
}

// Beautiful Solid Dialogue / Card Developer Credit section
internal fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

private data class ConfettiParticle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val size: Float,
    val shapeType: Int,
    val rotation: Float,
    val vr: Float,
    val alpha: Float = 1.0f
)

@Composable
private fun ConfettiShower(
    modifier: Modifier = Modifier,
    trigger: Boolean,
    durationMs: Long = 4000
) {
    if (!trigger) return

    val particles = remember { mutableStateListOf<ConfettiParticle>() }
    val random = remember { java.util.Random() }
    val density = androidx.compose.ui.platform.LocalDensity.current

    // Colors representing celebration including bKash Pink and Nagad Orange
    val particleColors = remember {
        listOf(
            Color(0xFFE2125B), // bKash Pink
            Color(0xFFF35F22), // Nagad Orange
            Color(0xFFFFD700), // Gold
            Color(0xFF00E5FF), // Cyan
            Color(0xFF00FF87), // Bright Green
            Color(0xFFFF007F)  // Pink/Magneta
        )
    }

    LaunchedEffect(trigger) {
        particles.clear()
        val count = 90
        val tempParticles = mutableListOf<ConfettiParticle>()
        
        // Spawn particles with shooting-up forces from bottom-left & bottom-right
        for (i in 0 until count) {
            val fromLeft = i % 2 == 0
            val startX = if (fromLeft) 50f else 950f // will adjust dynamically on canvas draw
            val startY = 1600f

            // Vector angle towards center-up
            val angleDeg = if (fromLeft) {
                -35f - random.nextFloat() * 45f // SHOOT UP RIGHT
            } else {
                -100f - random.nextFloat() * 45f // SHOOT UP LEFT
            }
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val speed = 30f + random.nextFloat() * 25f

            val vx = (Math.cos(angleRad) * speed).toFloat()
            val vy = (Math.sin(angleRad) * speed).toFloat()

            tempParticles.add(
                ConfettiParticle(
                    x = startX,
                    y = startY,
                    vx = vx,
                    vy = vy,
                    color = particleColors[random.nextInt(particleColors.size)],
                    size = 14f + random.nextFloat() * 18f,
                    shapeType = random.nextInt(3),
                    rotation = random.nextFloat() * 360f,
                    vr = -12f + random.nextFloat() * 24f
                )
            )
        }
        particles.addAll(tempParticles)

        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < durationMs) {
            val elapsed = System.currentTimeMillis() - startTime
            val progress = elapsed.toFloat() / durationMs
            
            val updatedList = mutableListOf<ConfettiParticle>()
            for (p in particles) {
                val newX = p.x + p.vx
                val newY = p.y + p.vy
                val newVy = p.vy + 1.1f // Gravity pull
                val newVx = p.vx * 0.96f // Wind drag
                val newRotation = p.rotation + p.vr
                val newAlpha = (1.0f - progress).coerceIn(0f, 1f)
                
                updatedList.add(
                    p.copy(
                        x = newX,
                        y = newY,
                        vx = newVx,
                        vy = newVy,
                        rotation = newRotation,
                        alpha = newAlpha
                    )
                )
            }
            particles.clear()
            particles.addAll(updatedList)
            kotlinx.coroutines.delay(16)
        }
        particles.clear()
    }

    BoxWithConstraints(modifier = modifier) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        // Re-align starting position once actual screen bounds are verified
        LaunchedEffect(width, height) {
            if (particles.isNotEmpty()) {
                for (index in particles.indices) {
                    val p = particles[index]
                    // If left side, spawn from near 5-10% of screen width. If right side, spawn from near 90-95%
                    val sideLeft = index % 2 == 0
                    val originalSpawnPointX = if (sideLeft) width * 0.1f else width * 0.9f
                    val originalSpawnPointY = height * 0.9f
                    
                    // Simple adjustment to give the relative initial offset
                    if (p.y > height) {
                        particles[index] = p.copy(x = originalSpawnPointX, y = originalSpawnPointY)
                    }
                }
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            particles.forEach { p ->
                if (p.alpha > 0f) {
                    rotate(p.rotation, pivot = androidx.compose.ui.geometry.Offset(p.x, p.y)) {
                        val paintColor = p.color.copy(alpha = p.alpha)
                        when (p.shapeType) {
                            0 -> { // Premium rectangle streamer
                                drawRect(
                                    color = paintColor,
                                    topLeft = androidx.compose.ui.geometry.Offset(p.x - p.size / 2, p.y - p.size / 4),
                                    size = androidx.compose.ui.geometry.Size(p.size, p.size / 2)
                                )
                            }
                            1 -> { // Shiny circle dot
                                drawCircle(
                                    color = paintColor,
                                    radius = p.size / 2.5f,
                                    center = androidx.compose.ui.geometry.Offset(p.x, p.y)
                                )
                            }
                            else -> { // Stylish Square paper
                                drawRect(
                                    color = paintColor,
                                    topLeft = androidx.compose.ui.geometry.Offset(p.x - p.size / 2.5f, p.y - p.size / 2.5f),
                                    size = androidx.compose.ui.geometry.Size(p.size, p.size)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SupportDeveloperDonationDialog(
    lang: String,
    onDismiss: () -> Unit
) {
    val isDark = isAppDark()
    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    
    var triggerConfetti by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        triggerConfetti = true
    }

    var copiedBkash by remember { mutableStateOf(false) }
    var copiedNagad by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ConfettiShower(
                modifier = Modifier.fillMaxSize(),
                trigger = triggerConfetti,
                durationMs = 4500
            )

            AnimatedDialogContent {
                val dialogSurfaceBg = if (isDark) Color(0xFF1E293B).copy(alpha = 0.92f) else Color.White.copy(alpha = 0.95f)
                val dialogBorder = if (isDark) Color(0xFF475569).copy(alpha = 0.5f) else Color(0xFFCBD5E1).copy(alpha = 0.7f)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .testTag("support_donation_dialog_surface"),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, dialogBorder),
                    colors = CardDefaults.cardColors(
                        containerColor = dialogSurfaceBg,
                        contentColor = if (isDark) Color.White else Color(0xFF0F172A)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        var iconAnimate by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            iconAnimate = true
                        }
                        val animatedScale by animateFloatAsState(
                            targetValue = if (iconAnimate) 1.2f else 0.8f,
                            animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
                            label = "gift_icon_scale"
                        )

                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = animatedScale
                                    scaleY = animatedScale
                                }
                                .size(68.dp)
                                .background(
                                    color = if (isDark) Color(0x33FCD34D) else Color(0xFFFEF3C7),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🎉",
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (lang == "bn") "ডেভেলপারকে সাপোর্ট করুন" else "Support the Developer",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (lang == "bn") {
                                "অ্যাপটিকে নিয়মিত উন্নত করার জন্য আপনার যেকোনো সহযোগিতা বা উপহার সাদরে গ্রহণ করা হবে। নিচে বিকাশ ও নগদ নম্বর দেওয়া হলো:"
                            } else {
                                "To support continuous updates and premium features, any donation is deeply appreciated. Copy details below:"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // BKASH SELECTION CARD
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .android16Clickable(shape = RoundedCornerShape(16.dp)) {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString("+8801788-884161"))
                                    copiedBkash = true
                                    copiedNagad = false
                                    Toast.makeText(context, if (lang == "bn") "বিকাশ নম্বর কপি করা হয়েছে!" else "bKash number copied!", Toast.LENGTH_SHORT).show()
                                },
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.5.dp, Color(0xFFE2125B).copy(alpha = 0.4f)),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) Color(0xFF221116) else Color(0xFFFFF0F5)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(Color(0xFFE2125B), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("ব", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = if (lang == "bn") "বিকাশ (Personal)" else "bKash (Personal)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFE2125B)
                                        )
                                        Text(
                                            text = if (lang == "bn") convertDigitsToBengali("+8801788-884161") else "+8801788-884161",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString("+8801788-884161"))
                                        copiedBkash = true
                                        copiedNagad = false
                                        Toast.makeText(context, if (lang == "bn") "বিকাশ নম্বর কপি করা হয়েছে!" else "bKash number copied!", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (copiedBkash) Icons.Default.Check else Icons.Default.ContentCopy,
                                        contentDescription = "Copy bKash Number",
                                        tint = if (copiedBkash) Color(0xFF10B981) else Color(0xFFE2125B)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // NAGAD SELECTION CARD
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .android16Clickable(shape = RoundedCornerShape(16.dp)) {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString("+8801788-884161"))
                                    copiedNagad = true
                                    copiedBkash = false
                                    Toast.makeText(context, if (lang == "bn") "নগদ নম্বর কপি করা হয়েছে!" else "Nagad number copied!", Toast.LENGTH_SHORT).show()
                                },
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.5.dp, Color(0xFFF35F22).copy(alpha = 0.4f)),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) Color(0xFF221411) else Color(0xFFFFF2EE)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(Color(0xFFF35F22), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("ন", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = if (lang == "bn") "নগদ (Personal)" else "Nagad (Personal)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFF35F22)
                                        )
                                        Text(
                                            text = if (lang == "bn") convertDigitsToBengali("+8801788-884161") else "+8801788-884161",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString("+8801788-884161"))
                                        copiedNagad = true
                                        copiedBkash = false
                                        Toast.makeText(context, if (lang == "bn") "নগদ নম্বর কপি করা হয়েছে!" else "Nagad number copied!", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (copiedNagad) Icons.Default.Check else Icons.Default.ContentCopy,
                                        contentDescription = "Copy Nagad Number",
                                        tint = if (copiedNagad) Color(0xFF10B981) else Color(0xFFF35F22)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("close_support_button")
                        ) {
                            Text(
                                text = if (lang == "bn") "বন্ধ করুন" else "Close",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AboutAppDialog(
    lang: String = "en",
    onDismiss: () -> Unit
) {
    val isDark = isAppDark()
    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        AnimatedDialogContent {
            val dialogSurfaceBg = if (isDark) Color(0xFF1E293B).copy(alpha = 0.92f) else Color.White.copy(alpha = 0.95f)
            val dialogBorder = if (isDark) Color(0xFF475569).copy(alpha = 0.5f) else Color(0xFFCBD5E1).copy(alpha = 0.7f)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
                    .fillMaxHeight(0.85f)
                    .padding(horizontal = 4.dp)
                    .testTag("about_app_dialog_surface"),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, dialogBorder),
                colors = CardDefaults.cardColors(
                    containerColor = dialogSurfaceBg,
                    contentColor = if (isDark) Color.White else Color(0xFF0F172A)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = if (isDark) Color(0x2210B981) else Color(0x112563EB),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF10B981) else Color(0xFF2563EB),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (lang == "bn") "অ্যাপ্লিকেশন সম্পর্কে" else "About Application",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Mobile Wallet v3.5",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Scrollable content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(scrollState)
                            .padding(end = 4.dp)
                    ) {
                        // Why use this app card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) Color(0x1210B981) else Color(0x0A2563EB)
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isDark) Color(0x3310B981) else Color(0x222563EB)
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = if (lang == "bn") "কেন এই অ্যাপটি ব্যবহার করবেন?" else "Why Use This App?",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) Color(0xFF10B981) else Color(0xFF2563EB)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (lang == "bn") {
                                        "আমাদের প্রতিদিনের আয়ের হিসাব, ব্যয়ের হিসাব, দেনা-পাওনা এবং বাজারের ফর্দ মনে রাখা অত্যন্ত কঠিন। \"Mobile Wallet\" এই সকল হিসাব জটমুক্ত ও সহজ করতে একটি সামগ্রিক অফলাইন সমাধান। এটি আপনার সম্পূর্ণ ব্যক্তিগত ফাইন্যান্স ম্যানেজার হিসেবে দ্বিমুখী ভাষা সুরক্ষাসহ কাজ করে।"
                                    } else {
                                        "Tracking earnings, personal spending, debts, and grocery lists dynamically can be overwhelming. \"Mobile Wallet\" is a comprehensive local-first toolkit that simplifies financial tracking under beautiful modern layouts."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Features Header
                        Text(
                            text = if (lang == "bn") "প্রধান জাদুকরী ফিচারসমূহ" else "Core Premium Features",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Features List
                        val features = listOf(
                            FeatureItem(
                                emoji = "💸",
                                titleBn = "দৈনিক আয়-ব্যয় ট্র্যাকার",
                                titleEn = "Income & Expense Tracker",
                                descBn = "সহজে দৈনিক আয়-ব্যয় কাস্টম ক্যাটাগরি, নোট ও সঠিক তারিখ দিয়ে লিখে রাখুন। ঝামেলাহীনভাবে বাজেট কন্ট্রোল করুন।",
                                descEn = "Record daily transactions with specific dates, labels, and notes. Control your budget easily."
                            ),
                            FeatureItem(
                                emoji = "📊",
                                titleBn = "রিয়েল-টাইম চার্ট ও ইনসাইট",
                                titleEn = "Visual Analytics & Insights",
                                descBn = "ইন্টারেক্টিভ পাই-চার্ট, ক্যাটাগরি ভিত্তিক খরচের হার এবং সম্পূর্ণ আর্থিক হিসেবের চমৎকার ভিজ্যুয়াল রিপোর্ট।",
                                descEn = "Interactive pie charts and category percentage ratios provide dynamic visual cash flow reports."
                            ),
                            FeatureItem(
                                emoji = "🛒",
                                titleBn = "ডিজিটাল বাজার তালিকা",
                                titleEn = "Digital Shopping (Bajar) List",
                                descBn = "বাজারে যাওয়ার আগে ফর্দ তৈরি করুন, প্রয়োজনীয় পরিমাণ লিখে রাখুন এবং কেনা সম্পন্ন হলে টিক দিয়ে দিন বা ট্র্যাশে পাঠান।",
                                descEn = "Frame future market needs by registering items with quantities, checking items of interest off as you shop."
                            ),
                            FeatureItem(
                                emoji = "🤝",
                                titleBn = "দেনা-পাওনা ও ঋণ ট্র্যাকার",
                                titleEn = "Debts & Loans Tracker",
                                descBn = "বন্ধুবান্ধব ও পরিবারের সাথে লেনদেনের বিস্তারিত হিসাব রাখুন। পরিশোধিত হলে এক ক্লিকে 'Settle' করে সম্পূর্ণ রেকর্ড সংরক্ষণ করুন।",
                                descEn = "Identify exact balances owed to or by you in a clear dashboard. Finish historical loans in a single tap."
                            ),
                            FeatureItem(
                                emoji = "💾",
                                titleBn = "স্মার্ট অটো-রিপ্লেস রিস্টোর ব্যাকআপ",
                                titleEn = "Auto-Overwrite Local Backup",
                                descBn = "ডাউনলোডস ফোল্ডারে ম্যানুয়াল JSON ব্যাকআপ এক্সপোর্ট করুন। নতুন ব্যাকআপ ফাইলটি অটোমেটিক পুরনোটিকে রিপ্লেস করে ফোনে অপ্রয়োজনীয় ডুপ্লিকেট ফাইল জমে থাকা বন্ধ করে!",
                                descEn = "Export manual JSON formatted file into your custom Downloads folder. Subsequent backups replace previous files to avoid digital clutter."
                            ),
                            FeatureItem(
                                emoji = "🌍",
                                titleBn = "ইনস্ট্যান্ট দ্রুত ভাষা পরিবর্তন",
                                titleEn = "Instant Language Toggle",
                                descBn = "অ্যাপের সম্পূর্ণ ইন্টারফেস খাঁটি বাংলা অথবা প্রফেশনাল ইংরেজি ভাষায় এক ক্লিকে পরিবর্তন করুন হেডার থেকে।",
                                descEn = "Instantly toggle the workspace templates between beautiful Bengali and professional English from the app header."
                            ),
                            FeatureItem(
                                emoji = "🎨",
                                titleBn = "প্রিমিয়াম গ্লাস-ইউআই ও ডার্ক মোড",
                                titleEn = "Premium Soft Glass UI & Theme",
                                descBn = "চোখের সুরক্ষার জন্য আরামদায়ক অন্ধকার ডার্ক থিম বা পরিচ্ছন্ন লাইট থিম। সাথে প্রিমিয়াম গ্লাস-মরফিজম ভিজ্যুয়াল ফিনিশ!",
                                descEn = "Dive into seamless, distraction-free modern UI styled elegantly with rich dark theme aesthetics and dynamic ripple effects."
                            )
                        )

                        features.forEach { item ->
                            FeatureItemRow(item = item, lang = lang, isDark = isDark)
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Recent Update Log Card
                        Text(
                            text = if (lang == "bn") "সাম্প্রতিক আপডেটসমূহ (Changelog)" else "Recent Change Log",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) Color(0xFF1E293B).copy(alpha = 0.6f) else Color(0xFFF1F5F9).copy(alpha = 0.8f)
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                // Update Group 1: Header styling
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(Color(0xFF3B82F6).copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Wallet,
                                            contentDescription = null,
                                            tint = Color(0xFF3B82F6),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (lang == "bn") "হেডার ও টাইটেল অপটিমাইজেশন" else "Header & Title Redesign",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (lang == "bn") {
                                                "টাইটেল 'Mobile Wallet' লেখাটি চমৎকার ২৫ পয়েন্টের অক্ষরের ব্যবধানে দীর্ঘায়িত করে স্টাইলিশভাবে ব্যাকগ্রাউন্ড বক্সে ফিট করা হয়েছে যাতে উপরের কোনো ফাঁকা জায়গা না থাকে। এছাড়া সেটিংস ও ইনফো বাটন দুটি থেকে অতিরিক্ত লেখা মুছে ফেলে সম্পূর্ণ আইকন-অনলি ও নিখুঁত সমান সাইজের (৪০dp) কম্প্যাক্ট রিভলভিং পিল কন্ট্রোলে পরিণত করা হয়েছে।"
                                            } else {
                                                "Rebuilt the primary 'Mobile Wallet' branding text with an expanded 2.5sp letter spacing to stretch elegantly across the background panel, eliminating vertical gaps. Re-engineered the System Settings and About buttons into identical icon-only circular pill elements of absolute equal size, stripping raw texts."
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }

                                // Update Group 2: Lists smoothness
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(Color(0xFF10B981).copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (lang == "bn") "লেনদেন তালিকার কার্যক্ষমতা ও স্মুথনেস" else "Smooth Transaction Ledgers",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (lang == "bn") {
                                                "লেনদেনের ইতিহাস স্ক্রলিং ১০০% ল্যাগ-ফ্রি ও মসৃণ করতে ক্যাটাগরিভিত্তিক এম্বিয়েন্ট রঙিন গ্লো ব্যাকগ্রাউন্ড যুক্ত করা হয়েছে। একই সাথে চমৎকার প্রেন্টিং-পিল ডিজাইনে ইনকাম এবং এক্সপেন্স ব্যাকগ্রাউন্ড রেন্ডার করা হয়েছে এবং অতিরিক্ত স্ক্রল-হেজি কাটানোর জন্য স্পর্শ সংবেদনশীল ফার্স্ট-হ্যান্ড টাচ ট্রানজিশন দেওয়া হয়েছে।"
                                            } else {
                                                "Optimized transaction ledgers for buttery smooth 120Hz scrolling by replacing solid blocks with lightweight contextual ambient glows mapped to categories. Implemented vertical tactile type pillars (Green glow for Income, Red for Expense) and premium receipt-style amount badges with zero click latency."
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }

                                // Update Group 3: Grids alignment
                                Row(
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(Color(0xFF8B5CF6).copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = Color(0xFF8B5CF6),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (lang == "bn") "সুষম লে-আউট ও অ্যালাইনমেন্ট সমন্বয়" else "Symmetrical Grid & Alignment",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isDark) Color(0xFFA78BFA) else Color(0xFF7C3AED)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (lang == "bn") {
                                                "বাকী ব্যালেন্স ও আয়ের ড্যাশবোর্ড বক্সগুলোকে একদম পরিমাপমত সোজা কাঠামোতে সাজানো হয়েছে, যার অ্যালাইনমেন্ট উপরের মূল হেডার সেকশন এবং টগলগুলোর সাথে নিখুঁতভাবে সারিবদ্ধ রয়েছে।"
                                            } else {
                                                "Ensured meticulous layout alignment, matching the remaining balance card's four-cornered shape boundaries exactly with the upper core header and action controls to present a unified straight visual layout."
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (lang == "bn") "পড়া হয়েছে" else "Got It",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private data class FeatureItem(
    val emoji: String,
    val titleBn: String,
    val titleEn: String,
    val descBn: String,
    val descEn: String
)

@Composable
private fun FeatureItemRow(
    item: FeatureItem,
    lang: String,
    isDark: Boolean
) {
    val cardBg = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f) else Color(0xFFFFFFFF).copy(alpha = 0.1f)
    val cardBorder = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f) else Color(0xFFFFFFFF).copy(alpha = 0.25f))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = cardBorder
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = if (isDark) Color(0x1F94A3B8) else Color(0x0F475569),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = item.emoji, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = if (lang == "bn") item.titleBn else item.titleEn,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = if (lang == "bn") item.descBn else item.descEn,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun SettingsDialog(
    currentLanguage: String,
    selectedTheme: String,
    onThemeChange: (String) -> Unit,
    onLanguageClick: () -> Unit,
    onImportBackup: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = isAppDark()
    var showAboutAppDialog by remember { mutableStateOf(false) }

    if (showAboutAppDialog) {
        AboutAppDialog(lang = currentLanguage, onDismiss = { showAboutAppDialog = false })
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
        val containerBg = if (isDark) Color(0xFF1E293B).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.85f)
        val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
        val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = bgColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Elite Header Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .background(
                            color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.45f) else Color.White.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isDark) Color(0xFF334155).copy(alpha = 0.50f) else Color(0xFFE2E8F0),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color(0xFF3B82F6), Color(0xFF10B981))
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (currentLanguage == "bn") "সেটিংস উইন্ডো" 
                                       else if (currentLanguage == "hi") "सेटिंग्स मेनू"
                                       else if (currentLanguage == "ar") "الإعدادات الذكية" 
                                       else if (currentLanguage == "es") "Ajustes del Sistema" 
                                       else "Personal Settings",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = textPrimary
                            )
                            Text(
                                text = if (currentLanguage == "bn") "পার্সোনালাইজেশন ও কন্ট্রোল" else "Personalization & control",
                                style = MaterialTheme.typography.labelSmall,
                                color = textSecondary
                            )
                        }
                    }

                    // Done / Close pill
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                            .android16Clickable { onDismiss() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (currentLanguage == "bn") "সম্পন্ন" else if (currentLanguage == "hi") "पूर्ण" else if (currentLanguage == "ar") "تم" else if (currentLanguage == "es") "Hecho" else "Done",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Local Backup Status Card (First thing, as previously implemented)
                    LocalBackupStatusCard(
                        currentLanguage = currentLanguage,
                        onImportClick = onImportBackup
                    )

                    // Theme Section Header with Title and Details
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = containerBg,
                                shape = RoundedCornerShape(24.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(0xFF8B5CF6).copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WbSunny,
                                    contentDescription = null,
                                    tint = Color(0xFF8B5CF6),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (currentLanguage == "bn") "অ্যাপ থিম নির্বাচন করুন" 
                                       else if (currentLanguage == "hi") "ऐप थीम चुनें"
                                       else if (currentLanguage == "ar") "اختر مظهر التطبيق"
                                       else if (currentLanguage == "es") "Seleccionar tema de la app"
                                       else "Select App Theme",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                color = textPrimary
                            )
                        }

                        // Modern Horizontal 3-Column Themes Segment Card
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val themes = listOf(
                                Triple("system", Pair("সিস্টেম", "System"), Icons.Default.Settings),
                                Triple("light", Pair("লাইট", "Light"), Icons.Default.WbSunny),
                                Triple("dark", Pair("ডার্ক", "Dark"), Icons.Default.NightsStay)
                            )

                            themes.forEach { (themeId, labels, icon) ->
                                val isSelected = selectedTheme == themeId
                                val label = if (currentLanguage == "bn") labels.first 
                                            else if (currentLanguage == "hi") {
                                                if (themeId == "system") "सिस्टम" else if (themeId == "light") "लाइट" else "डार्क"
                                            } else if (currentLanguage == "ar") {
                                                if (themeId == "system") "النظام" else if (themeId == "light") "فاتح" else "داكن"
                                            } else if (currentLanguage == "es") {
                                                if (themeId == "system") "Sistema" else if (themeId == "light") "Claro" else "Oscuro"
                                            } else labels.second

                                val cardBg = remember(isSelected, isDark) {
                                    if (isSelected) {
                                        if (isDark) Color(0xFF1E293B).copy(alpha = 0.9f) else Color(0xFFEFF6FF)
                                    } else {
                                        if (isDark) Color(0xFF0F172A).copy(alpha = 0.3f) else Color(0xFFF8FAFC)
                                    }
                                }

                                val cardBorderColor = remember(isSelected, isDark) {
                                    if (isSelected) {
                                        if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB)
                                    } else {
                                        if (isDark) Color(0xFF334155).copy(alpha = 0.4f) else Color(0xFFE2E8F0)
                                    }
                                }

                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(96.dp)
                                        .android16Clickable(shape = RoundedCornerShape(16.dp)) { onThemeChange(themeId) }
                                        .testTag("theme_sel_$themeId"),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = cardBorderColor
                                    ),
                                    colors = CardDefaults.cardColors(containerColor = cardBg)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(
                                                    color = if (isSelected) {
                                                        (if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB)).copy(alpha = 0.15f)
                                                    } else {
                                                        if (isDark) Color(0xFF1E293B) else Color(0xFFEDF2F7)
                                                    },
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = label,
                                                tint = if (isSelected) {
                                                    if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB)
                                                } else {
                                                    if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                                },
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .background(if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB), CircleShape)
                                                )
                                            }
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                                color = if (isSelected) (if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB)) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Preferences & App Integration Category Block
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = containerBg,
                                shape = RoundedCornerShape(24.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Language Selector Option
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .android16Clickable(shape = RoundedCornerShape(14.dp)) {
                                    onLanguageClick()
                                }
                                .testTag("settings_language_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) Color(0xFF1E293B).copy(alpha = 0.35f) else Color(0xFFF8FAFC)
                            ),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF334155).copy(alpha = 0.3f) else Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            color = Color(0xFF3B82F6).copy(alpha = 0.12f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Translate,
                                        contentDescription = "Language Option",
                                        tint = Color(0xFF3B82F6),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (currentLanguage == "bn") "ভাষা পরিবর্তন করুন" 
                                               else if (currentLanguage == "hi") "भाषा बदलें"
                                               else if (currentLanguage == "ar") "تغيير اللغة"
                                               else if (currentLanguage == "es") "Cambiar idioma"
                                               else "Change App Language",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary
                                    )
                                    val langLabel = remember(currentLanguage) {
                                        when (currentLanguage) {
                                            "bn" -> "🇧🇩 বাংলা (Bengali)"
                                            "hi" -> "🇮🇳 हिन्दी (Hindi)"
                                            "ar" -> "🇸🇦 العربية (Arabic)"
                                            "es" -> "🇪🇸 Español (Spanish)"
                                            else -> "🇺🇸 English"
                                        }
                                    }
                                    Text(
                                        text = langLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = textSecondary
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = textSecondary.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Check for updates UI Card Checked over Raw Github release JSON
                        var isCheckingForUpdates by remember { mutableStateOf(false) }
                        var settingsUpdateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
                        val context = LocalContext.current

                        if (settingsUpdateInfo != null) {
                            AppUpdateDialog(
                                updateInfo = settingsUpdateInfo!!,
                                currentLanguage = currentLanguage,
                                onDismiss = { settingsUpdateInfo = null }
                            )
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .android16Clickable(
                                    shape = RoundedCornerShape(14.dp),
                                    enabled = !isCheckingForUpdates
                                ) {
                                    isCheckingForUpdates = true
                                    checkForUpdatesAsync(context) { info, error ->
                                        isCheckingForUpdates = false
                                        if (error != null) {
                                            Toast.makeText(context, if (currentLanguage == "bn") "নেটওয়ার্ক সমস্যা, অনুগ্রহ করে পুনরায় চেষ্টা করুন" else "Network issue, please try again", Toast.LENGTH_SHORT).show()
                                        } else if (info != null) {
                                            if (info.hasUpdate) {
                                                settingsUpdateInfo = info
                                            } else {
                                                Toast.makeText(context, if (currentLanguage == "bn") "আপনার অ্যাপটি ইতিমধ্যে সর্বশেষ সংস্করণে রয়েছে!" else "Your app is already up to date!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) Color(0xFF1E293B).copy(alpha = 0.35f) else Color(0xFFF8FAFC)
                            ),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF334155).copy(alpha = 0.3f) else Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            color = Color(0xFF10B981).copy(alpha = 0.12f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isCheckingForUpdates) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = Color(0xFF10B981)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.SystemUpdate,
                                            contentDescription = "Update Check Icon",
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (currentLanguage == "bn") "আপডেট চেক করুন" else "Check for Updates",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary
                                    )
                                    Text(
                                        text = if (isCheckingForUpdates) {
                                            if (currentLanguage == "bn") "আপডেটের হিসাব খোঁজা হচ্ছে..." else "Looking for recent build releases..."
                                        } else {
                                            if (currentLanguage == "bn") "সর্বশেষ সংস্করণ কোড চেক করুন" else "Check latest release code"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = textSecondary
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = textSecondary.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // About App Button Inside settings
                    Button(
                        onClick = { showAboutAppDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) MaterialTheme.colorScheme.secondaryContainer else Color(0xFFE2E8F0),
                            contentColor = if (isDark) MaterialTheme.colorScheme.onSecondaryContainer else Color(0xFF334155)
                        ),
                        border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f) else Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About App Icon",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (currentLanguage == "bn") "অ্যাপ সম্পর্কে জানুন" else "About App & Features",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Close Settings Button inside scrollable area to prevent overlap/clipping on any screen size or bottom home gesture bar
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF2563EB),
                            contentColor = if (isDark) MaterialTheme.colorScheme.onPrimary else Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("settings_close_button")
                    ) {
                        Text(
                            text = if (currentLanguage == "bn") "বন্ধ করুন" else "Close Settings",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black)
                        )
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
fun DeveloperCreditDialog(
    currentUser: String?,
    lang: String = "en",
    onDismiss: () -> Unit
) {
    val isDark = isAppDark()
    var showContactDialog by remember { mutableStateOf(false) }

    if (showContactDialog) {
        DeveloperContactDialog(onDismiss = { showContactDialog = false })
    }

    Dialog(onDismissRequest = onDismiss) {
        AnimatedDialogContent {
            val dialogSurfaceBg = if (isDark) Color(0xFF0F172A) else Color.White
            val dialogBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .testTag("developer_credit_dialog_surface"),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, dialogBorderColor),
                colors = CardDefaults.cardColors(
                    containerColor = dialogSurfaceBg,
                    contentColor = if (isDark) Color.White else Color(0xFF1E293B)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Modern Abstract Design Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = if (isDark) {
                                        listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF311042))
                                    } else {
                                        listOf(Color(0xFFEFF6FF), Color(0xFFE0E7FF), Color(0xFFF3E8FF))
                                    }
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Futuristic code patterns/lines styled elegantly
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val lineColor = if (isDark) Color(0xFF10B981).copy(alpha = 0.08f) else Color(0xFF2563EB).copy(alpha = 0.05f)
                            drawCircle(
                                color = lineColor,
                                radius = size.minDimension * 0.6f,
                                center = androidx.compose.ui.geometry.Offset(size.width * 0.8f, size.height * 0.2f)
                            )
                            drawCircle(
                                color = lineColor,
                                radius = size.minDimension * 0.4f,
                                center = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.8f)
                            )
                        }

                        // Floating dynamic icon circle
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    color = if (isDark) Color(0x3310B981) else Color(0x222563EB),
                                    shape = CircleShape
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = if (isDark) Color(0xFF10B981).copy(alpha = 0.6f) else Color(0xFF2563EB).copy(alpha = 0.5f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF10B981) else Color(0xFF2563EB),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .padding(horizontal = 20.dp, vertical = 18.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (lang == "bn") "ডেভেলপার ক্রেডিট" else "Developer Credit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )

                        Text(
                            text = if (lang == "bn") "অ্যাপ্লিকেশন আর্কিটেক্ট এবং ক্রিয়েটর" else "Application Architect & Creator",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF10B981) else Color(0xFF2563EB)
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Unified Cards display
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CreditItemCard(
                                isDark = isDark,
                                icon = Icons.Default.Person,
                                accentColor = Color(0xFF10B981),
                                title = if (lang == "bn") "নাম" else "NAME",
                                value = "Md. Robiul Islam Sakib"
                            )

                            CreditItemCard(
                                isDark = isDark,
                                icon = Icons.Default.Computer,
                                accentColor = Color(0xFFA855F7),
                                title = if (lang == "bn") "ডিপার্টমেন্ট" else "DEPARTMENT",
                                value = "Computer Science and Technology"
                            )

                            CreditItemCard(
                                isDark = isDark,
                                icon = Icons.Default.School,
                                accentColor = Color(0xFF3B82F6),
                                title = if (lang == "bn") "ইনস্টিটিউট" else "INSTITUTE",
                                value = "Park Polytechnic Institute"
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        var showDonationDialog by remember { mutableStateOf(false) }

                        if (showDonationDialog) {
                            SupportDeveloperDonationDialog(
                                lang = lang,
                                onDismiss = { showDonationDialog = false }
                            )
                        }

                        // Primary Action: Elegant support button
                        Button(
                            onClick = { showDonationDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color(0xFFDB2777) else Color(0xFFFCE7F3),
                                contentColor = if (isDark) Color.White else Color(0xFF9D174D)
                            ),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFFF472B6) else Color(0xFFFBCFE8)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("support_developer_donation_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Support Icon",
                                modifier = Modifier.size(18.dp),
                                tint = if (isDark) Color.White else Color(0xFFEC4899)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (lang == "bn") "ডেভেলপারকে সাপোর্ট করুন (বিকাশ / নগদ)" else "Support Developer (bKash / Nagad)",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Secondary actions layout
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showContactDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                    contentColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                                ),
                                border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(46.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (lang == "bn") "যোগাযোগ" else "Contact",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            Button(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                                    contentColor = if (isDark) Color.White else Color(0xFF1E293B)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            ) {
                                Text(
                                    text = if (lang == "bn") "বন্ধ করুন" else "Close",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreditItemCard(
    isDark: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    title: String,
    value: String
) {
    val cardBg = if (isDark) Color(0xFF1E293B).copy(alpha = 0.5f) else Color(0xFFF8FAFC)
    val cardBorder = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, cardBorder),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = accentColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontSize = 10.sp),
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color.White else Color(0xFF1E293B)
                )
            }
        }
    }
}

// Beautiful Contact Dialog displaying Telegram, Facebook, and WhatsApp
@Composable
fun DeveloperContactDialog(
    onDismiss: () -> Unit
) {
    val isDark = isAppDark()
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    Dialog(onDismissRequest = onDismiss) {
        AnimatedDialogContent {
            val dialogSurfaceBg = if (isDark) Color(0xFF1E293B).copy(alpha = 0.92f) else Color.White.copy(alpha = 0.95f)
            val dialogBorder = if (isDark) Color(0xFF475569).copy(alpha = 0.5f) else Color(0xFFCBD5E1).copy(alpha = 0.7f)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .testTag("developer_contact_dialog_surface"),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, dialogBorder),
                colors = CardDefaults.cardColors(
                    containerColor = dialogSurfaceBg,
                    contentColor = if (isDark) Color.White else Color(0xFF0F172A)
                )
            ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Circular decorated message icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            if (isDark) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            else Color(0xFFEFF6FF)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AlternateEmail,
                        contentDescription = "Contact Developer Icon",
                        tint = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF2563EB),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Let's Connect",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Select any platform below to get in touch.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Contact list
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Telegram Option
                    ContactOptionItem(
                        title = "Telegram",
                        handle = "@sakib_9221",
                        icon = Icons.Default.Send,
                        brandColor = Color(0xFF0088CC),
                        onClick = {
                            try {
                                uriHandler.openUri("https://t.me/sakib_9221")
                            } catch (e: Exception) {
                                // Handled gracefully
                            }
                        }
                    )

                    // WhatsApp Option
                    ContactOptionItem(
                        title = "WhatsApp",
                        handle = "01788884161",
                        icon = Icons.Default.Phone,
                        brandColor = Color(0xFF25D366),
                        onClick = {
                            try {
                                uriHandler.openUri("https://wa.me/8801788884161")
                            } catch (e: Exception) {
                            }
                        }
                    )

                    // Facebook Option
                    ContactOptionItem(
                        title = "Facebook",
                        handle = "Mohammad Sakib Sordar",
                        icon = Icons.Default.Public,
                        brandColor = Color(0xFF1877F2),
                        onClick = {
                            try {
                                uriHandler.openUri("https://www.facebook.com/sakibislam94433")
                            } catch (e: Exception) {
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF2563EB),
                        contentColor = if (isDark) MaterialTheme.colorScheme.onPrimary else Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Close",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
}

@Composable
fun ContactOptionItem(
    title: String,
    handle: String,
    icon: ImageVector,
    brandColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .android16Clickable(shape = RoundedCornerShape(14.dp), onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(brandColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "$title Icon",
                    tint = brandColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = handle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Navigate to platform",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}


@Composable
fun GoogleDriveSyncBlock(
    viewModel: FinanceViewModel,
    currentUser: String?,
    getString: (Int) -> String
) {
    val isDark = isAppDark()
    val context = androidx.compose.ui.platform.LocalContext.current

    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackupToFile(context, uri)
        }
    }

    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importBackupFromFile(context, uri)
        }
    }
    
    // Standard visual feedback variables
    val syncState by viewModel.driveSyncState.collectAsStateWithLifecycle()
    val syncMessage by viewModel.driveLastSyncMessage.collectAsStateWithLifecycle()
    val onlineCount by viewModel.driveOnlineCount.collectAsStateWithLifecycle()
    val autoSync by viewModel.autoSyncOnChanges.collectAsStateWithLifecycle()
    
    val lastSyncTime = remember(currentUser, syncState) {
        viewModel.getDriveLastSyncTime()
    }
    
    val displayOnlineCount = remember(currentUser, syncState) {
        viewModel.getDriveOnlineCount()
    }

    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("google_drive_sync_card"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp, 
            if (currentUser != null) {
                if (isDark) Color(0x3310B981) else Color(0x66A7F3D0) // Emerald/Mint border for synced users
            } else {
                if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f) else Color(0x19FFFFFF)
            }
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (currentUser != null) {
                if (isDark) Color(0x1F10B981) else Color(0x0F10B981) // Soft green transparent background
            } else {
                if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f) else Color(0x0FFFFFFF)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Expandable controls
            Row(
                modifier = Modifier
                     .fillMaxWidth()
                     .android16Clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = "Cloud Icon",
                        tint = if (currentUser != null) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = getString(R.string.drive_sync_title),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (currentUser == null) getString(R.string.drive_unavailable_guest) else "Google Account Active",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = if (currentUser != null) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
                
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Expanded body with Sync Controls
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Text(
                        text = getString(R.string.drive_sync_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (currentUser == null) {
                        // User cannot use sync in guest mode
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                        ) {
                            Text(
                                text = "⚠ " + getString(R.string.drive_unavailable_guest),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // Logged-in user: show synced stats and control panel
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Status indicators
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = getString(R.string.drive_status_label),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                val statusText: String
                                val statusColor: Color
                                when (syncState) {
                                    com.example.data.GoogleDriveManager.SyncState.IDLE -> {
                                        statusText = "Idle"
                                        statusColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                    com.example.data.GoogleDriveManager.SyncState.SYNCING_BACKUP -> {
                                        statusText = "Backing up..."
                                        statusColor = MaterialTheme.colorScheme.primary
                                    }
                                    com.example.data.GoogleDriveManager.SyncState.SYNCING_RESTORE -> {
                                        statusText = "Restoring..."
                                        statusColor = MaterialTheme.colorScheme.secondary
                                    }
                                    com.example.data.GoogleDriveManager.SyncState.SUCCESS_BACKUP -> {
                                        statusText = "Backup success ✓"
                                        statusColor = Color(0xFF10B981)
                                    }
                                    com.example.data.GoogleDriveManager.SyncState.SUCCESS_RESTORE -> {
                                        statusText = "Restore success ✓"
                                        statusColor = Color(0xFF10B981)
                                    }
                                    com.example.data.GoogleDriveManager.SyncState.ERROR -> {
                                        statusText = "Sync failed ⚠"
                                        statusColor = MaterialTheme.colorScheme.error
                                    }
                                }
                                
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor
                                )
                            }

                            // Dynamic loading indicators
                            if (syncState == com.example.data.GoogleDriveManager.SyncState.SYNCING_BACKUP ||
                                syncState == com.example.data.GoogleDriveManager.SyncState.SYNCING_RESTORE) {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Sync Messages / Prompt log
                            if (syncMessage.isNotEmpty()) {
                                Text(
                                    text = syncMessage,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }

                            // Metadata: Last synced
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = getString(R.string.drive_last_sync_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = lastSyncTime,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Metadata: Online count
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = getString(R.string.drive_online_count_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$displayOnlineCount records",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Auto sync switch
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .android16Clickable { viewModel.toggleAutoSync() }
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = getString(R.string.drive_auto_sync_label),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Saves automatically on add/delete",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                                Switch(
                                    checked = autoSync,
                                    onCheckedChange = { viewModel.toggleAutoSync() },
                                    thumbContent = {
                                        Icon(
                                            imageVector = Icons.Default.Sync,
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize)
                                        )
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF10B981)
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Backup & Restore actions buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.backupToDrive() },
                                    modifier = Modifier.weight(1f)
                                        .testTag("drive_backup_now_button"),
                                    contentPadding = PaddingValues(vertical = 8.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF10B981),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = "Upload",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = getString(R.string.drive_backup_btn),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }

                                Button(
                                    onClick = { viewModel.restoreFromDrive() },
                                    modifier = Modifier.weight(1f)
                                        .testTag("drive_restore_now_button"),
                                    contentPadding = PaddingValues(vertical = 8.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary,
                                        contentColor = MaterialTheme.colorScheme.onSecondary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDownload,
                                        contentDescription = "Download",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = getString(R.string.drive_restore_btn),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    // Local Backup & Restore Block
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    Text(
                        text = "Offline File Backup (.json)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Save details to a file or import them back later. Persists even after uninstalling the app!",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                                exportLauncher.launch("finance_backup_$timestamp.json")
                            },
                            modifier = Modifier.weight(1f)
                                .testTag("file_export_button"),
                            contentPadding = PaddingValues(vertical = 8.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Export File",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Export File",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                importLauncher.launch(arrayOf("application/json"))
                            },
                            modifier = Modifier.weight(1f)
                                .testTag("file_import_button"),
                            contentPadding = PaddingValues(vertical = 8.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = "Import File",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Import File",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GoogleAvatar(
    email: String?,
    modifier: Modifier = Modifier,
    sizeDp: androidx.compose.ui.unit.Dp = 36.dp
) {
    if (email == null) return
    val initial = remember(email) {
        val trimmed = email.trim()
        if (trimmed.isNotEmpty()) trimmed[0].uppercaseChar().toString() else "U"
    }

    // Choose a consistent high-contrast gradient based on character content
    val gradientColors = remember(initial) {
        val hash = (initial.hashCode() and 0x7FFFFFFF) % 5
        when (hash) {
            0 -> listOf(Color(0xFFEF4444), Color(0xFFF87171)) // Red
            1 -> listOf(Color(0xFF10B981), Color(0xFF34D399)) // Green
            2 -> listOf(Color(0xFF3B82F6), Color(0xFF60A5FA)) // Blue
            3 -> listOf(Color(0xFFF59E0B), Color(0xFFFBBF24)) // Amber
            else -> listOf(Color(0xFF8B5CF6), Color(0xFFA78BFA)) // Purple
        }
    }

    Box(
        modifier = modifier
            .size(sizeDp)
            .clip(CircleShape)
            .background(Brush.linearGradient(gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        var isError by remember(email) { mutableStateOf(false) }
        var isLoading by remember(email) { mutableStateOf(true) }

        if (!isError) {
            val avatarUrl = remember(email) {
                "https://www.google.com/s2/photos/profile/${email.trim().lowercase()}?sz=250"
            }
            AsyncImage(
                model = avatarUrl,
                contentDescription = "Google profile picture",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onError = {
                    isError = true
                    isLoading = false
                },
                onSuccess = {
                    isLoading = false
                    isError = false
                }
            )
        }

        if (isError || isLoading) {
            Text(
                text = initial,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    fontSize = (sizeDp.value * 0.45f).sp
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BajarListDialog(
    bajarItems: List<com.example.data.BajarItem>,
    lang: String,
    onAdd: (name: String, quantity: String) -> Unit,
    onToggle: (com.example.data.BajarItem, Boolean) -> Unit,
    onDelete: (com.example.data.BajarItem) -> Unit,
    onFinishShopping: (totalCost: Double, completedItemsList: String) -> Unit,
    onDismiss: () -> Unit
) {
    var nameInput by remember { mutableStateOf("") }
    var qtyInput by remember { mutableStateOf("") }
    var isFinishingShopping by remember { mutableStateOf(false) }
    var totalCostInput by remember { mutableStateOf("") }

    val isDark = isAppDark()

    val titleText = if (lang == "bn") "বাজার তালিকা" else "Bajar (Shopping) List"
    val itemPlaceholder = if (lang == "bn") "বাজারের নাম (যেমন: আলু)" else "Item Name (e.g., Potato)"
    val qtyPlaceholder = if (lang == "bn") "পরিমাণ (যেমন: ২ কেজি, ৪টি)" else "Quantity (e.g., 2 kg, 4 pcs)"
    val addButtonText = if (lang == "bn") "যোগ করুন" else "Add Item"
    val emptyText = if (lang == "bn") "কোন বাজারের তালিকা নেই!" else "No items in Bajar list!"
    val closeButtonText = if (lang == "bn") "বন্ধ করুন" else "Close"

    val finishButtonText = if (lang == "bn") "বাজার শেষ করুন" else "Finish Shopping"

    Dialog(onDismissRequest = onDismiss) {
        AnimatedDialogContent {
            val dialogSurfaceBg = if (isDark) Color(0xFF1E293B).copy(alpha = 0.92f) else Color.White.copy(alpha = 0.95f)
            val dialogBorder = if (isDark) Color(0xFF475569).copy(alpha = 0.5f) else Color(0xFFCBD5E1).copy(alpha = 0.7f)
            val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
                    .heightIn(max = screenHeight - 48.dp)
                    .padding(16.dp)
                    .testTag("bajar_list_dialog_card"),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, dialogBorder),
                colors = CardDefaults.cardColors(
                    containerColor = dialogSurfaceBg,
                    contentColor = if (isDark) Color.White else Color(0xFF0F172A)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                if (!isFinishingShopping) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = onDismiss, modifier = Modifier.testTag("bajar_dismiss_icon_button")) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Input form to add a new Bajar item
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text(itemPlaceholder) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("bajar_name_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = qtyInput,
                            onValueChange = { qtyInput = it },
                            label = { Text(qtyPlaceholder) },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("bajar_cost_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Button(
                            onClick = {
                                val name = nameInput.trim()
                                val qty = qtyInput.trim()
                                if (name.isNotEmpty()) {
                                    val finalQty = if (qty.isEmpty()) "1" else qty
                                    onAdd(name, finalQty)
                                    nameInput = ""
                                    qtyInput = ""
                                }
                            },
                            modifier = Modifier.testTag("bajar_add_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981),
                                contentColor = Color.White
                            )
                        ) {
                            Text(addButtonText, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // List of items
                    Text(
                        text = if (lang == "bn") "তালিকাভুক্ত পণ্যসমূহ" else "Items",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                    ) {
                        if (bajarItems.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = emptyText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(
                                    items = bajarItems,
                                    key = { it.id },
                                    contentType = { "bajar_item" }
                                ) { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isDark) Color(0xFF334155).copy(alpha = 0.5f)
                                                else Color(0xFFF1F5F9)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Checkbox with click listener
                                        Checkbox(
                                            checked = item.isCompleted,
                                            onCheckedChange = { isChecked ->
                                                onToggle(item, isChecked)
                                            },
                                            modifier = Modifier.testTag("bajar_checkbox_${item.id}"),
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = Color(0xFF10B981)
                                            )
                                        )

                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = 8.dp)
                                        ) {
                                            Text(
                                                text = item.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    textDecoration = if (item.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                                                ),
                                                fontWeight = if (item.isCompleted) FontWeight.Normal else FontWeight.Bold,
                                                color = if (item.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                        else MaterialTheme.colorScheme.onSurface
                                            )

                                            Text(
                                                text = if (lang == "bn") "পরিমাণ: ${item.quantity}" else "Qty: ${item.quantity}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (item.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                        else Color(0xFF10B981)
                                            )
                                        }

                                        // Delete button
                                        IconButton(
                                            onClick = { onDelete(item) },
                                            modifier = Modifier.size(36.dp).testTag("bajar_delete_button_${item.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Item",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val completedItems = bajarItems.filter { it.isCompleted }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (completedItems.isNotEmpty()) {
                            Button(
                                onClick = { isFinishingShopping = true },
                                modifier = Modifier.testTag("bajar_finish_shopping_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF10B981),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Complete",
                                    modifier = Modifier.padding(end = 4.dp).size(16.dp)
                                )
                                Text(finishButtonText, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("bajar_dialog_close_button")
                        ) {
                            Text(closeButtonText, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Finishing shopping session form (ask money spent)
                    val completedItems = bajarItems.filter { it.isCompleted }
                    val completedNames = completedItems.joinToString(", ") { it.name }

                    val finishPromptTitle = if (lang == "bn") "বাজার সম্পন্ন করুন" else "Complete Shopping"
                    val finishPromptSub = if (lang == "bn") {
                        "নিচের সম্পন্নকৃত পণ্যগুলোর জন্য খরচ যোগ করা হবে:"
                    } else {
                        "The following completed items will be logged as an expense:"
                    }
                    val amountLabel = if (lang == "bn") "কত টাকা খরচ হলো?" else "How much money did you spend?"
                    val doneBtnText = if (lang == "bn") "সম্পন্ন" else "Done"
                    val backBtnText = if (lang == "bn") "পেছনে" else "Back"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = finishPromptTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { isFinishingShopping = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = finishPromptSub,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = completedNames,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isDark) Color(0xFF334155).copy(alpha = 0.3f)
                                else Color(0xFFF1F5F9)
                            )
                            .padding(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = totalCostInput,
                        onValueChange = { totalCostInput = it },
                        label = { Text(amountLabel) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("bajar_total_cost_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { isFinishingShopping = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(backBtnText, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val amount = totalCostInput.trim().toDoubleOrNull()
                                if (amount != null && amount >= 0.0) {
                                    onFinishShopping(amount, completedNames)
                                }
                            },
                            enabled = totalCostInput.trim().toDoubleOrNull() != null,
                            modifier = Modifier.weight(1.5f).testTag("bajar_finish_shopping_confirm_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(doneBtnText, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
}

// Dialog 4: Debts & Lending Tracker Dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtListDialog(
    debtRecords: List<com.example.data.DebtRecord>,
    lang: String,
    onAddDebt: (personName: String, amount: Double, direction: String, note: String) -> Unit,
    onSettleDebt: (com.example.data.DebtRecord, Double) -> Unit,
    onDeleteDebt: (com.example.data.DebtRecord) -> Unit,
    onUpdateDebt: (com.example.data.DebtRecord) -> Unit,
    onDismiss: () -> Unit
) {
    var nameInput by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }
    var selectedDirection by remember { mutableStateOf("PAYABLE") } // "PAYABLE" or "RECEIVABLE"

    var editingDebt by remember { mutableStateOf<com.example.data.DebtRecord?>(null) }
    var partialSettleTarget by remember { mutableStateOf<com.example.data.DebtRecord?>(null) }
    var partialSettleAmount by remember { mutableStateOf("") }

    val isDark = isAppDark()

    val titleText = if (lang == "bn") "দেন-পাওনা ট্র্যাকার" else "Debts & Loans Tracker"
    val namePlaceholder = if (lang == "bn") "নাম (যেমন: আবির)" else "Name (e.g., Abir)"
    val amountPlaceholder = if (lang == "bn") "টাকা (যেমন: ৫০০)" else "Amount (e.g., 500)"
    val notePlaceholder = if (lang == "bn") "নোট / কেন দেয়া হয়েছিল (ঐচ্ছিক)" else "Note (Optional)"
    val isEditing = editingDebt != null
    val addButtonText = if (isEditing) {
        if (lang == "bn") "হালনাগাদ করুন" else "Update Record"
    } else {
        if (lang == "bn") "যোগ করুন" else "Add Record"
    }
    val emptyText = if (lang == "bn") "কোনো দেন-পাওনার রেকর্ড নেই" else "No active debt records"

    Dialog(onDismissRequest = onDismiss) {
        AnimatedDialogContent {
            val dialogSurfaceBg = if (isDark) Color(0xFF1E293B).copy(alpha = 0.92f) else Color.White.copy(alpha = 0.95f)
            val dialogBorder = if (isDark) Color(0xFF475569).copy(alpha = 0.5f) else Color(0xFFCBD5E1).copy(alpha = 0.7f)
            val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
                    .heightIn(max = screenHeight - 48.dp)
                    .padding(16.dp)
                    .testTag("debt_list_dialog_card"),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, dialogBorder),
                colors = CardDefaults.cardColors(
                    containerColor = dialogSurfaceBg,
                    contentColor = if (isDark) Color.White else Color(0xFF0F172A)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("debt_dismiss_icon_button")) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Input form to add a new Debt record
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text(namePlaceholder) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("debt_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text(amountPlaceholder) },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        modifier = Modifier.weight(1f).testTag("debt_amount_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Direction Selector Button
                    Button(
                        onClick = {
                            selectedDirection = if (selectedDirection == "PAYABLE") "RECEIVABLE" else "PAYABLE"
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedDirection == "PAYABLE") Color(0xFFEF4444) else Color(0xFF10B981)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.android16Clickable(shape = RoundedCornerShape(12.dp)) {
                            selectedDirection = if (selectedDirection == "PAYABLE") "RECEIVABLE" else "PAYABLE"
                        }
                    ) {
                        Text(
                            text = if (selectedDirection == "PAYABLE") {
                                if (lang == "bn") "আমি দেবো" else "I owe"
                            } else {
                                if (lang == "bn") "আমি পাবো" else "I get"
                            },
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text(notePlaceholder) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("debt_note_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val name = nameInput.trim()
                        val amt = amountInput.toDoubleOrNull()
                        if (name.isNotEmpty() && amt != null && amt > 0.0) {
                            if (isEditing && editingDebt != null) {
                                val updated = editingDebt!!.copy(
                                    personName = name,
                                    amount = amt,
                                    direction = selectedDirection,
                                    note = noteInput.trim()
                                )
                                onUpdateDebt(updated)
                                editingDebt = null
                            } else {
                                onAddDebt(name, amt, selectedDirection, noteInput.trim())
                            }
                            nameInput = ""
                            amountInput = ""
                            noteInput = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth().android16Clickable(shape = RoundedCornerShape(12.dp)) {
                        val name = nameInput.trim()
                        val amt = amountInput.toDoubleOrNull()
                        if (name.isNotEmpty() && amt != null && amt > 0.0) {
                            if (isEditing && editingDebt != null) {
                                val updated = editingDebt!!.copy(
                                    personName = name,
                                    amount = amt,
                                    direction = selectedDirection,
                                    note = noteInput.trim()
                                )
                                onUpdateDebt(updated)
                                editingDebt = null
                            } else {
                                onAddDebt(name, amt, selectedDirection, noteInput.trim())
                            }
                            nameInput = ""
                            amountInput = ""
                            noteInput = ""
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isEditing) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                        contentColor = if (isEditing) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(addButtonText, fontWeight = FontWeight.Bold)
                }

                if (isEditing) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            editingDebt = null
                            nameInput = ""
                            amountInput = ""
                            noteInput = ""
                            selectedDirection = "PAYABLE"
                        },
                        modifier = Modifier.fillMaxWidth().android16Clickable(shape = RoundedCornerShape(12.dp)) {
                            editingDebt = null
                            nameInput = ""
                            amountInput = ""
                            noteInput = ""
                            selectedDirection = "PAYABLE"
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (lang == "bn") "বাতিল" else "Cancel",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // List of items
                Text(
                    text = if (lang == "bn") "সক্রিয় দেন-পাওনা সমূহ" else "Active Debt Records",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                ) {
                    val activeDebts = remember(debtRecords) { debtRecords.filter { !it.isSettled } }
                    if (activeDebts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = emptyText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(
                                items = activeDebts,
                                key = { it.id },
                                contentType = { "debt_item" }
                            ) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isDark) Color(0xFF334155).copy(alpha = 0.5f)
                                            else Color(0xFFF1F5F9)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = item.personName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            
                                            // Badge for direction
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(
                                                        if (item.direction == "PAYABLE") Color(0x22EF4444) else Color(0x2210B981)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (item.direction == "PAYABLE") {
                                                        if (lang == "bn") "আমি দেবো" else "I Owe"
                                                    } else {
                                                        if (lang == "bn") "পাবো" else "Get"
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (item.direction == "PAYABLE") Color(0xFFEF4444) else Color(0xFF10B981)
                                                )
                                            }
                                        }

                                        if (item.note.isNotEmpty()) {
                                            Text(
                                                text = item.note,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Text(
                                            text = formatDate(item.timestamp, lang),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = formatCurrency(item.amount, lang),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (item.direction == "PAYABLE") Color(0xFFEF4444) else Color(0xFF10B981)
                                        )

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Settle button
                                            Button(
                                                onClick = { 
                                                    partialSettleTarget = item
                                                    partialSettleAmount = item.amount.toString()
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (item.direction == "PAYABLE") Color(0xFFEF4444) else Color(0xFF10B981)
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier
                                                    .height(28.dp)
                                                    .android16Clickable(shape = RoundedCornerShape(8.dp)) { 
                                                        partialSettleTarget = item
                                                        partialSettleAmount = item.amount.toString()
                                                    }
                                            ) {
                                                Text(
                                                    text = if (lang == "bn") "শোধ" else "Settle",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }

                                            // Edit icon button
                                            IconButton(
                                                onClick = {
                                                    editingDebt = item
                                                    nameInput = item.personName
                                                    amountInput = item.amount.toString()
                                                    noteInput = item.note
                                                    selectedDirection = item.direction
                                                },
                                                modifier = Modifier.size(28.dp).android16Clickable(shape = CircleShape) {
                                                    editingDebt = item
                                                    nameInput = item.personName
                                                    amountInput = item.amount.toString()
                                                    noteInput = item.note
                                                    selectedDirection = item.direction
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit record",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            // Delete icon button
                                            IconButton(
                                                onClick = { onDeleteDebt(item) },
                                                modifier = Modifier.size(28.dp).android16Clickable(shape = CircleShape) { onDeleteDebt(item) }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete record",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
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
        }
    }

    if (partialSettleTarget != null) {
        val target = partialSettleTarget!!
        Dialog(onDismissRequest = { partialSettleTarget = null }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (lang == "bn") "দেন-পাওনা সমন্বয়" else "Settle Debt/Credit",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    val directionText = if (target.direction == "PAYABLE") {
                        if (lang == "bn") "পাওনাদার: ${target.personName}" else "Creditor: ${target.personName}"
                    } else {
                        if (lang == "bn") "খাতক: ${target.personName}" else "Debtor: ${target.personName}"
                    }
                    
                    Column {
                        Text(
                            text = directionText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (lang == "bn") "মোট পরিমাণ: ৳${target.amount}" else "Total Amount: ৳${target.amount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }

                    // Input field for partial payment
                    OutlinedTextField(
                        value = partialSettleAmount,
                        onValueChange = { partialSettleAmount = it },
                        label = { 
                            Text(
                                text = if (lang == "bn") "পরিশোধের পরিমাণ লিখুন" else "Settle Amount"
                            )
                        },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text(target.amount.toString()) }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { partialSettleTarget = null }) {
                           Text(text = if (lang == "bn") "বাতিল" else "Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val amt = partialSettleAmount.toDoubleOrNull() ?: target.amount
                                if (amt > 0.0) {
                                    val cappedAmt = if (amt > target.amount) target.amount else amt
                                    onSettleDebt(target, cappedAmt)
                                    partialSettleTarget = null
                                    partialSettleAmount = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (target.direction == "PAYABLE") Color(0xFFEF4444) else Color(0xFF10B981)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (lang == "bn") "সমন্বয় করুন" else "Settle",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun LocalBackupStatusCard(
    currentLanguage: String,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppDark()
    val containerBg = if (isDark) {
        Color(0x1F10B981) // Green transparent hue
    } else {
        Color(0x1210B981) // Highly optimized, soft transparent green
    }
    val textColor = if (isDark) Color(0xFF34D399) else Color(0xFF065F46) // Clean high-contrast emerald green for light mode
    val borderColor = if (isDark) Color(0x3310B981) else Color(0x4010B981)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Backup,
                        contentDescription = "Backup Active",
                        tint = textColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (currentLanguage == "bn") "অটো ব্যাকআপ সক্রিয়" else "Auto-Backup Active",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (currentLanguage == "bn") 
                        "ডাটা পরিবর্তন হলে ডাউনলোড ফোল্ডারে ব্যাকআপ আপডেট হয়।" 
                    else 
                        "Local backup updates in your Downloads folder when data changes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.85f),
                    lineHeight = 15.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = textColor
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .android16Clickable(shape = RoundedCornerShape(8.dp)) {
                        onImportClick()
                    }
                    .height(30.dp)
                    .testTag("auto_backup_import_button"),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (currentLanguage == "bn") "ইমপোর্ট" else "Import",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun DropdownItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    title: String,
    desc: String,
    isDark: Boolean,
    onClick: () -> Unit,
    isLarge: Boolean = false
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isNarrow = screenWidthDp < 360

    val cornerRadius = if (isLarge) 18.dp else 12.dp
    val containerColor = if (isLarge) {
        if (isDark) Color(0xFF1E293B).copy(alpha = 0.55f) else Color.White.copy(alpha = 0.72f)
    } else {
        if (isDark) Color(0xFF1E293B).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.72f)
    }
    val borderColor = if (isLarge) {
        if (isDark) Color(0x736366F1) else Color(0x4D0D9488)
    } else {
        if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)
    }
    val borderWidth = if (isLarge) 2.dp else 1.dp

    val outerPadding = if (isNarrow) 10.dp else if (isLarge) 15.dp else 10.dp
    val dynamicIconBoxSize = if (isNarrow) 32.dp else if (isLarge) 46.dp else 32.dp
    val dynamicIconSize = if (isNarrow) 16.dp else if (isLarge) 24.dp else 16.dp
    val spacerWidth = if (isNarrow) 10.dp else if (isLarge) 14.dp else 10.dp

    val titleStyle = if (isNarrow) {
        MaterialTheme.typography.bodyMedium
    } else if (isLarge) {
        MaterialTheme.typography.titleMedium
    } else {
        MaterialTheme.typography.bodyMedium
    }

    val descStyle = if (isNarrow) {
        MaterialTheme.typography.labelSmall
    } else if (isLarge) {
        MaterialTheme.typography.bodySmall
    } else {
        MaterialTheme.typography.labelSmall
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .android16Clickable(shape = RoundedCornerShape(cornerRadius)) { onClick() },
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(outerPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(dynamicIconBoxSize)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(dynamicIconSize)
                )
            }
            Spacer(modifier = Modifier.width(spacerWidth))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = titleStyle,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )
                Text(
                    text = desc,
                    style = descStyle,
                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

data class AppUpdateInfo(
    val hasUpdate: Boolean,
    val latestVersionName: String,
    val latestVersionCode: Int,
    val changelog: String,
    val downloadUrl: String
)

fun checkForUpdatesAsync(
    context: android.content.Context,
    onResult: (AppUpdateInfo?, String?) -> Unit
) {
    val currentVersionCode = try {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            pInfo.longVersionCode.toInt()
        } else {
            pInfo.versionCode
        }
    } catch (e: Exception) {
        1
    }

    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
        var connection: java.net.HttpURLConnection? = null
        try {
            val url = java.net.URL("https://raw.githubusercontent.com/sakib9221/Mobile-Wallet/main/update.json")
            connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.useCaches = false
            
            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = org.json.JSONObject(responseText)
                val latestVersionCode = json.optInt("versionCode", 1)
                val latestVersionName = json.optString("versionName", "1.0")
                val changelog = json.optString("changelog", "")
                val downloadUrl = json.optString("downloadUrl", "https://github.com/sakib9221/Mobile-Wallet/releases")
                
                val hasUpdate = latestVersionCode > currentVersionCode
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(
                        AppUpdateInfo(
                            hasUpdate = hasUpdate,
                            latestVersionName = latestVersionName,
                            latestVersionCode = latestVersionCode,
                            changelog = changelog,
                            downloadUrl = downloadUrl
                        ),
                        null
                    )
                }
            } else {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(null, "Response code: ${connection.responseCode}")
                }
            }
        } catch (e: Exception) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onResult(null, e.localizedMessage ?: "Network error")
            }
        } finally {
            connection?.disconnect()
        }
    }
}

class AppUpdaterDownloader(
    private val context: android.content.Context,
    private val downloadUrl: String,
    private val onProgress: (Float, Long, Long) -> Unit, // progress, downloaded, total
    private val onCompleted: (java.io.File) -> Unit,
    private val onError: (String) -> Unit
) {
    private var job: kotlinx.coroutines.Job? = null

    fun start() {
        job = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            var connection: java.net.HttpURLConnection? = null
            var input: java.io.BufferedInputStream? = null
            var output: java.io.FileOutputStream? = null
            try {
                val apkFile = java.io.File(context.cacheDir, "MobileWallet_update.apk")
                if (apkFile.exists()) {
                    apkFile.delete()
                }

                val url = java.net.URL(downloadUrl)
                connection = url.openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.connect()

                if (connection.responseCode != java.net.HttpURLConnection.HTTP_OK) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onError("Server returned response code " + connection.responseCode)
                    }
                    return@launch
                }

                val fileLength = connection.contentLengthLong
                input = java.io.BufferedInputStream(connection.inputStream)
                output = java.io.FileOutputStream(apkFile)

                val data = ByteArray(4096)
                var total: Long = 0
                var count: Int
                var lastProgressUpdate = 0L

                while (input.read(data).also { count = it } != -1) {
                    if (!isActive) {
                        apkFile.delete()
                        return@launch
                    }
                    total += count.toLong()
                    val currentProgress = if (fileLength > 0) total.toFloat() / fileLength.toFloat() else -1f
                    
                    val now = System.currentTimeMillis()
                    if (now - lastProgressUpdate > 50 || total == fileLength) {
                        lastProgressUpdate = now
                        val currentDownloaded = total
                        val currentLength = fileLength
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            onProgress(currentProgress, currentDownloaded, currentLength)
                        }
                    }
                    output.write(data, 0, count)
                }

                output.flush()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onCompleted(apkFile)
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onError(e.localizedMessage ?: "Unknown download error")
                }
            } finally {
                try { output?.close() } catch (e: Exception) {}
                try { input?.close() } catch (e: Exception) {}
                connection?.disconnect()
            }
        }
    }

    fun cancel() {
        job?.cancel()
    }
}

fun installApk(context: android.content.Context, apkFile: java.io.File) {
    try {
        if (!apkFile.exists()) {
            android.widget.Toast.makeText(context, "APK file not found", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val authority = "${context.packageName}.provider"
        val apkUri = androidx.core.content.FileProvider.getUriForFile(context, authority, apkFile)
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Installation failed: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
    }
}

@Composable
fun AppUpdateDialog(
    updateInfo: AppUpdateInfo,
    currentLanguage: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isDark = isAppDark()
    
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var downloadedBytes by remember { mutableStateOf(0L) }
    var totalBytes by remember { mutableStateOf(0L) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    var activeDownloader by remember { mutableStateOf<AppUpdaterDownloader?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            activeDownloader?.cancel()
        }
    }
    
    Dialog(
        onDismissRequest = {
            if (!isDownloading) {
                activeDownloader?.cancel()
                onDismiss()
            }
        }
    ) {
        AnimatedDialogContent {
            val dialogBg = if (isDark) Color(0xFF1E293B).copy(alpha = 0.92f) else Color.White.copy(alpha = 0.95f)
            val borderColor = if (isDark) Color(0xFF475569).copy(alpha = 0.5f) else Color(0xFFCBD5E1).copy(alpha = 0.7f)
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, borderColor),
                colors = CardDefaults.cardColors(
                    containerColor = dialogBg,
                    contentColor = if (isDark) Color.White else Color(0xFF0F172A)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                if (isDownloading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = if (currentLanguage == "bn") "আপডেট ডাউনলোড হচ্ছে" else "Downloading update",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color.White else Color(0xFF1E293B)
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Progress Bar
                        val animatedProgress by animateFloatAsState(
                            targetValue = if (downloadProgress >= 0f) downloadProgress else 0.0f,
                            animationSpec = spring(stiffness = Spring.StiffnessLow),
                            label = "download_progress"
                        )
                        
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF10B981),
                            trackColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Format dynamic counts, e.g. "81.5 / 107.1 MB"
                        val downloadedText = remember(downloadedBytes, totalBytes) {
                            val downloadedMB = downloadedBytes.toDouble() / (1024 * 1024)
                            val totalMB = totalBytes.toDouble() / (1024 * 1024)
                            if (totalBytes > 0) {
                                String.format(java.util.Locale.US, "%.1f / %.1f MB", downloadedMB, totalMB)
                            } else {
                                String.format(java.util.Locale.US, "%.1f MB", downloadedMB)
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = downloadedText,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                fontWeight = FontWeight.Bold
                            )
                            
                            // Show percentage on the right, e.g., "76%"
                            val pct = if (totalBytes > 0) "${(downloadProgress * 100).toInt()}%" else ""
                            Text(
                                text = pct,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                fontWeight = FontWeight.Black
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Cancel button on bottom-right
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    activeDownloader?.cancel()
                                    isDownloading = false
                                }
                            ) {
                                Text(
                                    text = if (currentLanguage == "bn") "বাতিল" else "Cancel",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFFDC2626)
                                )
                            }
                        }
                    }
                } else if (downloadError != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (currentLanguage == "bn") "ডাউনলোড ব্যর্থ হয়েছে" else "Download Failed",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = downloadError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    downloadError = null
                                    isDownloading = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (currentLanguage == "bn") "বন্ধ করুন" else "Close")
                            }
                            
                            Button(
                                onClick = {
                                    downloadError = null
                                    isDownloading = true
                                    val downloader = AppUpdaterDownloader(
                                        context = context,
                                        downloadUrl = updateInfo.downloadUrl,
                                        onProgress = { p, downloaded, total ->
                                            downloadProgress = p
                                            downloadedBytes = downloaded
                                            totalBytes = total
                                        },
                                        onCompleted = { file ->
                                            isDownloading = false
                                            installApk(context, file)
                                            onDismiss()
                                        },
                                        onError = { err ->
                                            downloadError = err
                                            isDownloading = false
                                        }
                                    )
                                    activeDownloader = downloader
                                    downloader.start()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                            ) {
                                Text(if (currentLanguage == "bn") "আবার চেষ্টা করুন" else "Retry")
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    color = if (isDark) Color(0x2410B981) else Color(0x1610B981),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = if (currentLanguage == "bn") "নতুন আপডেট এসেছে!" else "New Update Available!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color.White else Color(0xFF1E293B)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = if (currentLanguage == "bn") "ভার্সন: ${updateInfo.latestVersionName}" else "Version: ${updateInfo.latestVersionName}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (updateInfo.changelog.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) Color(0xFF0F172A).copy(alpha = 0.5f) else Color(0xFFF1F5F9).copy(alpha = 0.70f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFE2E8F0).copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = if (currentLanguage == "bn") "কী পরিবর্তন করা হয়েছে:" else "What's New:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = updateInfo.changelog,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            ) {
                                Text(
                                    text = if (currentLanguage == "bn") "পরে করব" else "Maybe Later",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                                    maxLines = 1,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                            
                            Button(
                                onClick = {
                                    isDownloading = true
                                    val downloader = AppUpdaterDownloader(
                                        context = context,
                                        downloadUrl = updateInfo.downloadUrl,
                                        onProgress = { p, downloaded, total ->
                                            downloadProgress = p
                                            downloadedBytes = downloaded
                                            totalBytes = total
                                        },
                                        onCompleted = { file ->
                                            isDownloading = false
                                            installApk(context, file)
                                            onDismiss()
                                        },
                                        onError = { err ->
                                            downloadError = err
                                            isDownloading = false
                                        }
                                    )
                                    activeDownloader = downloader
                                    downloader.start()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF10B981),
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(46.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (currentLanguage == "bn") "এখনই আপডেট" else "Update Now",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    maxLines = 1,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportPreviewDialog(
    transactions: List<Transaction>,
    currentLanguage: String,
    onDownloadPdf: () -> Unit,
    onSharePdf: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = isAppDark()
    val dialogBg = if (isDark) Color(0xFF1E293B).copy(alpha = 0.92f) else Color.White.copy(alpha = 0.95f)
    val borderColor = if (isDark) Color(0xFF475569).copy(alpha = 0.5f) else Color(0xFFCBD5E1).copy(alpha = 0.7f)

    val totalIncome = remember(transactions) { transactions.filter { it.type == "INCOME" }.sumOf { it.amount } }
    val totalExpense = remember(transactions) { transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount } }
    val netBalance = remember(totalIncome, totalExpense) { totalIncome - totalExpense }

    val formattedGenerationTime = remember(currentLanguage) {
        val dateFmt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val formatted = dateFmt.format(java.util.Date())
        if (currentLanguage == "bn") "তৈরি করার সময়: $formatted" else "Generated: $formatted"
    }

    Dialog(onDismissRequest = onDismiss) {
        AnimatedDialogContent {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, borderColor),
                colors = CardDefaults.cardColors(
                    containerColor = dialogBg,
                    contentColor = if (isDark) Color.White else Color(0xFF0F172A)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Title Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (currentLanguage == "bn") "রিপোর্ট প্রিভিউ" else "Report Preview",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF1E293B)
                            )
                        }
                        
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Report Paper-Style Sheet (Scrollable Preview)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(
                                color = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp)
                    ) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                // Report Title Header
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = if (currentLanguage == "bn") "অর্থসংক্রান্ত প্রতিবেদন" else "Personal Finance Report",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Black,
                                        color = if (isDark) Color.White else Color(0xFF1E293B)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = formattedGenerationTime,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                                    )
                                }
                            }

                            item {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                                )
                            }

                            // Summary Figures block
                            item {

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.5f) else Color.White,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (currentLanguage == "bn") "মোট ব্যালেন্স" else "Net Balance",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                        )
                                        Text(
                                            text = "৳ ${"%,.2f".format(netBalance)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (netBalance >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9))
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (currentLanguage == "bn") "মোট আয়" else "Total Income",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                            )
                                            Text(
                                                text = "৳ ${"%,.2f".format(totalIncome)}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF10B981)
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .height(30.dp)
                                                .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9))
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (currentLanguage == "bn") "মোট ব্যয়" else "Total Expense",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                            )
                                            Text(
                                                text = "৳ ${"%,.2f".format(totalExpense)}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFEF4444)
                                            )
                                        }
                                    }
                                }
                            }

                            item {
                                Text(
                                    text = if (currentLanguage == "bn") "লেনদেন বিবরণী" else "Transactions List",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            if (transactions.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (currentLanguage == "bn") "কোন লেনদেন পাওয়া যায়নি" else "No transactions found",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isDark) Color(0xFF475569) else Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            } else {
                                items(
                                    items = transactions,
                                    key = { "preview_${it.id}" },
                                    contentType = { "report_preview_item" }
                                ) { tx ->
                                    PreviewTransactionRow(tx = tx, currentLanguage = currentLanguage, isDark = isDark)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Buttons Panel at bottom of Dialog
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onSharePdf,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .weight(1.5f)
                                .height(46.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isDark) Color.White else Color(0xFF475569)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (currentLanguage == "bn") "শেয়ার করুন" else "Share Report",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isDark) Color.White else Color(0xFF475569),
                                maxLines = 1
                            )
                        }

                        Button(
                            onClick = onDownloadPdf,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF2563EB),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1.8f)
                                .height(46.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (currentLanguage == "bn") "ডাউনলোড ও সেভ" else "Download & Save",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// REDESIGNED ACTION GRID & TOOLS (3 COLUMNS)
// ==========================================
@Composable
fun DashboardActionsGrid(
    currentLanguage: String,
    onAddTransaction: () -> Unit,
    onViewHistory: () -> Unit,
    onBajarList: () -> Unit,
    onDebtsLoans: () -> Unit,
    onDownloadPdf: () -> Unit,
    onImportBackup: () -> Unit
) {
    val isDark = isAppDark()
    val cardBg = if (isDark) Color(0xFF1E293B).copy(alpha = 0.55f) else Color.White.copy(alpha = 0.72f)
    val cardBorderColor = if (isDark) Color(0xFF475569).copy(alpha = 0.5f) else Color(0xFFCBD5E1).copy(alpha = 0.7f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, cardBorderColor),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (currentLanguage == "bn") "কুইক অ্যাকশন ও টুলস" else "Quick Actions & Tools",
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.labelMedium,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFFCBD5E1)
            )

            // 5 Vertical Actions & Tools as requested
            DropdownItem(
                icon = Icons.Default.Add,
                iconBg = if (isDark) Color(0xFF4F46E5) else Color(0xFF6366F1),
                title = if (currentLanguage == "bn") "লেনদেন যুক্ত করুন" else "Add Transactions",
                desc = if (currentLanguage == "bn") "নতুন আয় বা ব্যয়ের হিসাব লিখুন" else "Record new income or expense",
                isDark = isDark,
                onClick = onAddTransaction,
                isLarge = true
            )

            DropdownItem(
                icon = Icons.Default.History,
                iconBg = Color(0xFF2563EB),
                title = if (currentLanguage == "bn") "লেনদেনের ইতিহাস" else "Transactions History",
                desc = if (currentLanguage == "bn") "আগের সকল লেনদেন পর্যবেক্ষণ করুন" else "View and search previous transactions",
                isDark = isDark,
                onClick = onViewHistory
            )

            DropdownItem(
                icon = Icons.Default.ShoppingBasket,
                iconBg = Color(0xFF10B981),
                title = if (currentLanguage == "bn") "বাজারের তালিকা" else "Bajar (Shopping list)",
                desc = if (currentLanguage == "bn") "কেনাকাটার প্রয়োজনীয় তালিকা" else "Manage items to scan/buy",
                isDark = isDark,
                onClick = onBajarList
            )

            DropdownItem(
                icon = Icons.Default.People,
                iconBg = Color(0xFFF59E0B),
                title = if (currentLanguage == "bn") "দেনা-পাওনা / লোন" else "Debts & Loans",
                desc = if (currentLanguage == "bn") "কারো থেকে ঋণ গ্রহণ বা প্রদানের হিসাব" else "Track active loans or lendings",
                isDark = isDark,
                onClick = onDebtsLoans
            )

            DropdownItem(
                icon = Icons.Default.PictureAsPdf,
                iconBg = Color(0xFFEF4444),
                title = if (currentLanguage == "bn") "পিডিএফ রিপোর্ট" else "PDF Report",
                desc = if (currentLanguage == "bn") "পিডিএফ স্টেটমেন্ট তৈরি ও ডাউনলোড" else "Generate, preview, or download PDF statement",
                isDark = isDark,
                onClick = onDownloadPdf
            )
        }
    }
}

@Composable
fun QuickActionItem(
    icon: ImageVector,
    bgColor: Color,
    iconColor: Color,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppDark()
    val subtitleColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF0F172A)

    Card(
        modifier = modifier
            .height(84.dp)
            .android16Clickable(shape = RoundedCornerShape(16.dp)) { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E293B).copy(alpha = 0.55f) else Color.White.copy(alpha = 0.72f)
        ),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF475569).copy(alpha = 0.5f) else Color(0xFFCBD5E1).copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(bgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp
                ),
                color = subtitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PreviewTransactionRow(tx: Transaction, currentLanguage: String, isDark: Boolean) {
    val formattedDate = remember(tx.dateLong, currentLanguage) {
        formatDate(tx.dateLong, currentLanguage)
    }
    val prefix = remember(tx.type) { if (tx.type == "INCOME") "+" else "-" }
    val tint = remember(tx.type) { if (tx.type == "INCOME") Color(0xFF10B981) else Color(0xFFEF4444) }
    val formattedAmount = remember(tx.amount) { "%,.2f".format(tx.amount) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.3f) else Color.White,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tx.category,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color(0xFF1E293B)
            )
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.labelSmall,
                color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
            )
        }
        
        Text(
            text = "$prefix৳ $formattedAmount",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = tint
        )
    }
}

// ===============================================
// EXTREMELY SMOOTH TRANSACTIONS HISTORY WINDOW
// ===============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsHistoryDialog(
    transactions: List<Transaction>,
    currentLanguage: String,
    getString: (Int) -> String,
    onDeleteTransaction: (Transaction) -> Unit,
    onEditTransaction: (Transaction) -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = isAppDark()
    val dialogBg = if (isDark) Color(0xFF0F172A) else Color.White
    val isBn = currentLanguage == "bn"

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isTablet = screenWidthDp >= 600

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight(if (isTablet) 0.85f else 1f)
                    .fillMaxWidth(if (isTablet) 0.85f else 1f)
                    .widthIn(max = 600.dp)
                    .clickable(enabled = false, onClick = {}),
                shape = if (isTablet) RoundedCornerShape(28.dp) else RoundedCornerShape(0.dp),
                color = dialogBg,
                contentColor = if (isDark) Color.White else Color(0xFF0F172A),
                border = if (isTablet) BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)) else null
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .navigationBarsPadding()
                ) {
                // Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBn) "লেনদেন ইতিহাস" else "Transactions History",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                var searchQuery by remember { mutableStateOf("") }
                var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "INCOME", "EXPENSE"

                // Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = if (isBn) "লেনদেন খুঁজুন..." else "Search transactions...",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                        unfocusedContainerColor = if (isDark) Color(0xFF1E293B).copy(alpha = 0.6f) else Color(0xFFF8FAFC),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                        unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                )

                // Premium Segmented Pill Filter Control
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(
                            color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val filters = listOf("ALL", "INCOME", "EXPENSE")
                    filters.forEach { filter ->
                        val label = when (filter) {
                            "ALL" -> if (isBn) "সব" else "All"
                            "INCOME" -> if (isBn) "আয়" else "Income"
                            else -> if (isBn) "ব্যয়" else "Expense"
                        }
                        val isSelected = selectedFilter == filter
                        val tabInteractionSource = remember { MutableInteractionSource() }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                )
                                .android16ScalePress(tabInteractionSource)
                                .clickable(
                                    interactionSource = tabInteractionSource,
                                    indication = null
                                ) { selectedFilter = filter },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                                }
                            )
                        }
                    }
                }

                // Filter Logic
                val filteredList = remember(transactions, searchQuery, selectedFilter, currentLanguage) {
                    val query = searchQuery.trim().lowercase()
                    if (query.isEmpty() && selectedFilter == "ALL") {
                        transactions
                    } else {
                        transactions.filter { transaction ->
                            val categoryName = {
                                val resId = transaction.category.toIntOrNull()
                                if (resId != null) getString(resId) else transaction.category
                            }()
                            val matchesSearch = if (query.isEmpty()) true else {
                                transaction.note.lowercase().contains(query) ||
                                        categoryName.lowercase().contains(query)
                            }
                            val matchesType = when (selectedFilter) {
                                "INCOME" -> transaction.type == "INCOME"
                                "EXPENSE" -> transaction.type == "EXPENSE"
                                else -> true
                            }
                            matchesSearch && matchesType
                        }
                    }
                }

                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "No items matched",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = if (isBn) "কোন লেনদেন পাওয়া যায়নি" else "No transactions found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 24.dp, top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = filteredList,
                            key = { it.id },
                            contentType = { "transaction_item" }
                        ) { tx ->
                            TransactionListItem(
                                transaction = tx,
                                lang = currentLanguage,
                                getString = getString,
                                isDark = isDark,
                                onDelete = onDeleteTransaction,
                                onEdit = onEditTransaction
                            )
                        }
                    }
                }
            }
        }
    }
}
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionDialog(
    currentLanguage: String,
    onLanguageSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = isAppDark()
    val dialogBg = if (isDark) Color(0xFF0F172A) else Color.White

    val languages = listOf(
        Triple("en", "🇺🇸  English", "Default English"),
        Triple("bn", "🇧🇩  বাংলা", "Bengali"),
        Triple("hi", "🇮🇳  हिन्दी", "Hindi"),
        Triple("ar", "🇸🇦  العربية", "Arabic"),
        Triple("es", "🇪🇸  Español", "Spanish")
    )

    val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = screenHeight - 48.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = dialogBg),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF3B82F6), Color(0xFF10B981))
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = when(currentLanguage) {
                        "bn" -> "ভাষা নির্বাচন করুন"
                        "hi" -> "भाषा चुनें"
                        "ar" -> "اختر اللغة"
                        "es" -> "Seleccionar idioma"
                        else -> "Select Language"
                    },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = when(currentLanguage) {
                        "bn" -> "আপনার পছন্দের অ্যাপ্লিকেশন ভাষা চয়ন করুন"
                        "hi" -> "अपनी पसंदीदा स्थानीय भाषा चुनें"
                        "ar" -> "اختر لغتك المفضلة للتطبيق"
                        "es" -> "Selecciona tu idioma de preferencia"
                        else -> "Choose your preferred application language"
                    },
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    languages.forEach { (code, name, subtitle) ->
                        val isSelected = currentLanguage == code
                        val optionBg = if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        } else {
                            if (isDark) Color(0xFF1E293B).copy(alpha = 0.4f) else Color(0xFFF8FAFC)
                        }
                        val optionBorder = if (isSelected) {
                            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                        } else {
                            BorderStroke(1.dp, if (isDark) Color(0xFF334155).copy(alpha = 0.3f) else Color(0xFFE2E8F0))
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(optionBg, RoundedCornerShape(16.dp))
                                .border(optionBorder, RoundedCornerShape(16.dp))
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onLanguageSelect(code) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Close Button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = when(currentLanguage) {
                            "bn" -> "বন্ধ করুন"
                            "hi" -> "बंद करें"
                            "ar" -> "إغلاق"
                            "es" -> "Cerrar"
                            else -> "Close"
                        },
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    transaction: Transaction,
    lang: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = isAppDark()
    val containerBg = if (isDark) Color(0xFF1E293B).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.95f)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    // Localized language strings
    val title = when (lang) {
        "bn" -> "লেনদেন মুছে ফেলা নিশ্চিত করুন"
        "hi" -> "लेनदेन हटाने की पुष्टि करें"
        "ar" -> "تأكيد حذف المعاملة"
        "es" -> "Confirmar eliminación"
        else -> "Confirm Delete"
    }

    val message = when (lang) {
        "bn" -> "আপনি কি নিশ্চিতভাবে এই লেনদেনটি স্থায়ীভাবে মুছে ফেলতে চান? এটি আর স্বয়ংক্রিয়ভাবে ফিরে পাওয়া যাবে না।"
        "hi" -> "क्या आप वाकई इस लेनदेन को स्थायी रूप से हटाना चाहते हैं? इसे वापस नहीं लाया जा सकता।"
        "ar" -> "هل أنت متأكد من رغبتك في حذف هذه المعاملة نهائيًا؟ لا يمكن التراجع عن هذا الإجراء."
        "es" -> "¿Está seguro de que desea eliminar permanentemente esta transacción? No se puede deshacer."
        else -> "Are you sure you want to permanently delete this transaction record? This action cannot be undone."
    }

    val cancelText = when (lang) {
        "bn" -> "বাতিল করুন"
        "hi" -> "रद्द करें"
        "ar" -> "إلغاء"
        "es" -> "Cancelar"
        else -> "Cancel"
    }

    val deleteText = when (lang) {
        "bn" -> "মুছে ফেলুন"
        "hi" -> "हटाएं"
        "ar" -> "حذف"
        "es" -> "Eliminar"
        else -> "Delete"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("delete_confirmation_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = containerBg
            ),
            border = BorderStroke(
                width = 1.dp,
                color = if (isDark) Color(0xFF334155).copy(alpha = 0.8f) else Color(0xFFE2E8F0)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Bin Icon Block
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xFFEF4444).copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Icon",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    ),
                    color = textPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Amount details if available
                val formattedAmount = remember(transaction) {
                    val symbol = if (transaction.type == "income") "+" else "-"
                    val prefix = if (lang == "bn") "পরিমাণ: " else "Amount: "
                    "$prefix$symbol$${transaction.amount}"
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (isDark) Color(0xFF0F172A).copy(alpha = 0.4f) else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = formattedAmount,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (transaction.type == "income") Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall.copy(
                        lineHeight = 18.sp
                    ),
                    color = textSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Actions Button Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cancel button
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFE2E8F0),
                            contentColor = textPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(
                            text = cancelText,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black)
                        )
                    }

                    // Delete button
                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF4444),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(
                            text = deleteText,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
