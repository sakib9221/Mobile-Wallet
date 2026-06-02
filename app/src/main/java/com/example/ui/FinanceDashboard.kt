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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.Transaction
import com.example.ui.theme.LocalThemeState
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
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.45f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "click_scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isPressed) 0.82f else 1.0f,
        animationSpec = spring(
            stiffness = Spring.StiffnessHigh
        ),
        label = "click_alpha"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
        .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            enabled = enabled,
            onClick = onClick
        )
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
    var showAddDialog by remember { mutableStateOf(false) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var showDeveloperCreditDialog by remember { mutableStateOf(false) }
    var showBajarListDialog by remember { mutableStateOf(false) }
    var showDebtListDialog by remember { mutableStateOf(false) }
    val bItems by viewModel.bajarItems.collectAsStateWithLifecycle()
    val debtRecords by viewModel.debtRecords.collectAsStateWithLifecycle()

    // Optimize listener callbacks with remember to prevent parent-induced child recompositions at 120Hz
    val onLanguageToggle = remember { { viewModel.toggleLanguage() } }
    val onShowDeveloperCredit = remember { { showDeveloperCreditDialog = true } }
    val onShowAuth = remember { { showAuthDialog = true } }
    val onAddTransactionClick = remember { { showAddDialog = true } }
    val onTabSelect = remember { { tab: String -> viewModel.changeTab(tab) } }
    val onDeleteTransaction = remember(context, getStringResource) {
        { transaction: Transaction ->
            viewModel.deleteTransaction(transaction)
            Toast.makeText(context, getStringResource(R.string.delete_confirm), Toast.LENGTH_SHORT).show()
        }
    }
    val onDismissDeveloperCredit = remember { { showDeveloperCreditDialog = false } }
    val onDismissAddDialog = remember { { showAddDialog = false } }
    val onSaveAddDialog = remember(context, getStringResource) {
        { amount: Double, categoryId: Int, type: String, note: String, dateLong: Long ->
            viewModel.addTransaction(amount, categoryId, type, note, dateLong)
            showAddDialog = false
            Toast.makeText(context, getStringResource(R.string.transaction_saved), Toast.LENGTH_SHORT).show()
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

    // Ensure initial guest data are seeded for preview/starter
    LaunchedEffect(Unit) {
        viewModel.seedGuestDataIfNeeded()
    }

    MeshBackground {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = getStringResource(R.string.app_name),
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleLarge.copy(
                                letterSpacing = (-0.5).sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    },
                    navigationIcon = {
                        // Language Switch Glass Pill Button
                        val isDark = isAppDark()
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color(0x66FFFFFF)
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f) else Color(0x80FFFFFF)
                            ),
                            shape = CircleShape,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .android16Clickable { onLanguageToggle() }
                                .testTag("language_toggle_button"),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Translate,
                                    contentDescription = "Language",
                                    tint = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF475569),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (currentLanguage == "en") "বাংলা" else "English",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF475569)
                                )
                            }
                        }
                    },
                    actions = {
                        // Developer Credit/About App info button
                        val isDark = isAppDark()
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color(0x66FFFFFF))
                                .android16Clickable { onShowDeveloperCredit() }
                                .testTag("developer_credit_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = "Developer Credit Info",
                                tint = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF475569),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Google Auth Button inside a Glass circle
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color(0x66FFFFFF))
                                .android16Clickable { onShowAuth() }
                                .testTag("auth_profile_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (currentUser != null) {
                                GoogleAvatar(email = currentUser, sizeDp = 36.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.AccountCircle,
                                    contentDescription = "Profile Inactive",
                                    tint = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF475569),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            floatingActionButton = {
                val debtsButtonText = if (currentLanguage == "bn") "দেন-পাওনা" else "Debts"
                val bajarListButtonText = if (currentLanguage == "bn") "বাজার" else "Bajar"
                Row(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FloatingActionButton(
                        onClick = { showDebtListDialog = true },
                        containerColor = Color(0xFFF59E0B),
                        contentColor = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("floating_debt_list_button")
                            .android16Clickable { showDebtListDialog = true }
                    ) {
                        Icon(Icons.Default.People, contentDescription = "Debts")
                    }

                    FloatingActionButton(
                        onClick = { showBajarListDialog = true },
                        containerColor = Color(0xFF10B981),
                        contentColor = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("floating_bajar_list_button")
                            .android16Clickable { showBajarListDialog = true }
                    ) {
                        Icon(Icons.Default.ShoppingBasket, contentDescription = "Bajar List")
                    }

                    ExtendedFloatingActionButton(
                        onClick = onAddTransactionClick,
                        icon = { Icon(Icons.Filled.Add, contentDescription = "Add transaction") },
                        text = { Text(getStringResource(R.string.add_transaction_title)) },
                        containerColor = Color(0xFF2563EB),
                        contentColor = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("floating_add_button")
                            .android16Clickable(onClick = onAddTransactionClick)
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
            // Live banner indicating Google Auth State
            UserSessionBanner(
                currentUser = currentUser,
                getString = getStringResource,
                onSignInClick = onShowAuth
            )

            // Interactive Google Drive Sync block
            GoogleDriveSyncBlock(
                viewModel = viewModel,
                currentUser = currentUser,
                getString = getStringResource
            )

            // Minimalist Monthly balance summary card
            DashboardSummaryCard(
                stats = stats,
                lang = currentLanguage,
                getString = getStringResource
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Navigation selector for transactions list or monthly category summary
            TabNavigationRow(
                currentTab = currentTab,
                onTabSelect = onTabSelect,
                getString = getStringResource
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Content matching the active tab with premium horizontal swipe transition
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    if (targetState == "summary") {
                        // Slide in from right (to left), together with slide out to left
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        // Slide in from left (to right), together with slide out to right
                        slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> width } + fadeOut()
                    }
                },
                label = "tab_transition",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { targetTab ->
                if (targetTab == "dashboard") {
                    TransactionsTabContent(
                        transactions = transactions,
                        currentLanguage = currentLanguage,
                        getString = getStringResource,
                        onDeleteTransaction = onDeleteTransaction
                    )
                } else {
                    SummaryTabContent(
                        transactions = transactions,
                        currentLanguage = currentLanguage,
                        getString = getStringResource
                    )
                }
            }
        }
    }
    }

    // Dialog 0: Developer Credit Dialog
    if (showDeveloperCreditDialog) {
        DeveloperCreditDialog(
            currentUser = currentUser,
            lang = currentLanguage,
            onDismiss = onDismissDeveloperCredit
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

    // Dialog 2: Interactive Google Auth Dialog (With Real Feedback Sync)
    if (showAuthDialog) {
        GoogleAuthDialog(
            currentUser = currentUser,
            selectedTheme = selectedTheme,
            onThemeChange = onThemeChange,
            onDismiss = onDismissAuthDialog,
            onSignInSuccess = onSignInSuccess,
            onSignOut = onSignOut,
            getString = getStringResource
        )
    }

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
            onSettleDebt = { record -> viewModel.settleDebtRecord(record) },
            onDeleteDebt = { record -> viewModel.deleteDebtRecord(record) },
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
            .android16Clickable { onSignInClick() },
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
    lang: String,
    getString: (Int) -> String
) {
    val isDark = isAppDark()
    val cardBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White
    val cardBorder = MaterialTheme.colorScheme.outlineVariant

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

    val incomeBorder = if (isDark) Color(0x4D34D399) else Color(0x4D10B981)
    val incomeBg = if (isDark) Color(0x1510B981) else Color(0x1A10B981)
    val incomeIconTint = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
    val incomeLabel = if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857)
    val incomeValue = if (isDark) Color(0xFFA7F3D0) else Color(0xFF064E3B)

    val expenseBorder = if (isDark) Color(0x4DFB7185) else Color(0x4DF43F5E)
    val expenseBg = if (isDark) Color(0x15F43F5E) else Color(0x1AF43F5E)
    val expenseIconTint = if (isDark) Color(0xFFFB7185) else Color(0xFFE11D48)
    val expenseLabel = if (isDark) Color(0xFFFDA4AF) else Color(0xFFBE123C)
    val expenseValue = if (isDark) Color(0xFFFECDD3) else Color(0xFF881337)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("dashboard_summary_card"),
        shape = RoundedCornerShape(32.dp),
        border = BorderStroke(1.dp, cardBorder),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Remaining Balance Section
            Text(
                text = getString(R.string.remaining_balance_label).uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF2563EB)
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
                            fontSize = 40.sp,
                            letterSpacing = (-1).sp
                        ),
                        color = if (stats.balance >= 0) MaterialTheme.colorScheme.onSurface else Color(0xFFE11D48)
                    )
                    Text(
                        text = ".$decimalPart",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF94A3B8),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                } else {
                    Text(
                        text = formattedTotal,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 40.sp,
                            letterSpacing = (-1).sp
                        ),
                        color = if (stats.balance >= 0) MaterialTheme.colorScheme.onSurface else Color(0xFFE11D48)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Income / Expense Side-by-Side Panels with tinted background borders matching HTML
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
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

// Custom Switch Tab Row for Transactions / Summary selection with glass styling
@Composable
fun TabNavigationRow(
    currentTab: String,
    onTabSelect: (String) -> Unit,
    getString: (Int) -> String
) {
    val isDark = isAppDark()
    val tabRowBg = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.5f) else Color(0x33FFFFFF)
    val tabRowBorder = if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f) else Color(0x66FFFFFF)
    val activePaneBg = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xE6FFFFFF)
    val activeTextColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF2563EB)
    val inactiveTextColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF64748B)

    // Smoothly animated weights for expanding the active tab
    val dWeight by animateFloatAsState(
        targetValue = if (currentTab == "dashboard") 1.4f else 1.0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "dashboard_weight"
    )
    val sWeight by animateFloatAsState(
        targetValue = if (currentTab == "summary") 1.4f else 1.0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "summary_weight"
    )

    // Smoothly animated background card colors
    val dBackGroundColor by animateColorAsState(
        targetValue = if (currentTab == "dashboard") activePaneBg else Color.Transparent,
        animationSpec = tween(durationMillis = 220),
        label = "dashboard_bg"
    )
    val sBackGroundColor by animateColorAsState(
        targetValue = if (currentTab == "summary") activePaneBg else Color.Transparent,
        animationSpec = tween(durationMillis = 220),
        label = "summary_bg"
    )

    // Smoothly animated text colors
    val dTextColor by animateColorAsState(
        targetValue = if (currentTab == "dashboard") activeTextColor else inactiveTextColor,
        animationSpec = tween(durationMillis = 220),
        label = "dashboard_text"
    )
    val sTextColor by animateColorAsState(
        targetValue = if (currentTab == "summary") activeTextColor else inactiveTextColor,
        animationSpec = tween(durationMillis = 220),
        label = "summary_text"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, tabRowBorder),
        colors = CardDefaults.cardColors(containerColor = tabRowBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Tab 1: Dashboard & Transactions list
            Box(
                modifier = Modifier
                    .weight(dWeight)
                    .clip(RoundedCornerShape(16.dp))
                    .background(dBackGroundColor)
                    .android16Clickable(enabled = currentTab != "dashboard") { onTabSelect("dashboard") }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getString(R.string.recent_transactions_header),
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.3.sp),
                    color = dTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Tab 2: Categorized Summary info
            Box(
                modifier = Modifier
                    .weight(sWeight)
                    .clip(RoundedCornerShape(16.dp))
                    .background(sBackGroundColor)
                    .android16Clickable(enabled = currentTab != "summary") { onTabSelect("summary") }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getString(R.string.monthly_summary_header),
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.3.sp),
                    color = sTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// Tab 1: Transaction List View Content
@Composable
fun TransactionsTabContent(
    transactions: List<Transaction>,
    currentLanguage: String,
    getString: (Int) -> String,
    onDeleteTransaction: (Transaction) -> Unit
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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 84.dp, top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(transactions, key = { it.id }) { transaction ->
                TransactionListItem(
                    transaction = transaction,
                    lang = currentLanguage,
                    getString = getString,
                    isDark = isDark,
                    onDelete = { onDeleteTransaction(transaction) }
                )
            }
        }
    }
}

// Single Transaction List Row
@Composable
fun TransactionListItem(
    transaction: Transaction,
    lang: String,
    getString: (Int) -> String,
    isDark: Boolean,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val isExpense = transaction.type == "EXPENSE"
    val categoryName = remember(transaction.category, lang) {
        val resId = transaction.category.toIntOrNull()
        if (resId != null) getString(resId) else transaction.category
    }

    // Cache formatted timestamp and amount strings as stable remembered instances for butter-smooth 120Hz scrolling
    val formattedDate = remember(transaction.dateLong, lang) {
        formatDate(transaction.dateLong, lang)
    }
    val formattedAmount = remember(transaction.amount, lang, isExpense) {
        "${if (isExpense) "-" else "+"}${formatCurrency(transaction.amount, lang)}"
    }

    val itemBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White
    val itemBorder = MaterialTheme.colorScheme.outlineVariant
    val titleColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF1E293B)
    val noteColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF64748B)
    val dateColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f) else Color(0xFF94A3B8)
    val amountColor = if (isExpense) Color(0xFFE11D48) else { if (isDark) Color(0xFF34D399) else Color(0xFF10B981) }

    // Map categories to modern design colors for icons
    val categoryColor = remember(transaction.category) {
        val resId = transaction.category.toIntOrNull()
        when (resId) {
            R.string.category_salary -> Color(0xFF00C853) // Green
            R.string.category_food -> Color(0xFFFF9100) // Orange
            R.string.category_groceries -> Color(0xFFFFD600) // Yellow
            R.string.category_utilities -> Color(0xFFD500F9) // Purple
            R.string.category_entertainment -> Color(0xFFF50057) // Pink
            R.string.category_transport -> Color(0xFF00E5FF) // Cyan
            R.string.category_freelance -> Color(0xFF2979FF) // Blue
            else -> Color(0xFF9E9E9E) // Others
        }
    }

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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .android16Clickable { expanded = !expanded },
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, itemBorder), // white/40 as in design HTML
        colors = CardDefaults.cardColors(containerColor = itemBg), // white/60 backdrop glass
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rounded color emblem for Category
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp)) // rounded-2xl style icon backgrounds
                        .background(categoryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = categoryName,
                        tint = categoryColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Text section
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = categoryName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
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
                        color = dateColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Cost / Income badge and delete button
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = formattedAmount,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = amountColor
                    )
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
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = shrinkVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) + fadeOut()
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
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Transaction Note", transaction.note)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, if (lang == "bn") "নোট কপি করা হয়েছে" else "Note copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .android16Clickable {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("Transaction Note", transaction.note)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, if (lang == "bn") "নোট কপি করা হয়েছে" else "Note copied to clipboard", Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (lang == "bn") "সব তথ্য কপি" else "Copy Note", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = onDelete,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .android16Clickable { onDelete() }
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (lang == "bn") "মুছে ফেলুন" else "Delete Record", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                items(activeList, key = { it.first }) { (categoryResId, totalAmount) ->
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
    val categoryName = getString(categoryResId)

    // Cache formatting computations inside remember states to guarantee stable 120Hz scrolling frames
    val formattedAmount = remember(amount, lang) {
        formatCurrency(amount, lang)
    }
    val formattedPercentage = remember(percentage) {
        String.format(Locale.US, "%.1f%%", percentage)
    }

    val colorAccent = when (categoryResId) {
        R.string.category_salary -> Color(0xFF00C853)
        R.string.category_food -> Color(0xFFFF9100)
        R.string.category_groceries -> Color(0xFFFFD600)
        R.string.category_utilities -> Color(0xFFD500F9)
        R.string.category_entertainment -> Color(0xFFF50057)
        R.string.category_transport -> Color(0xFF00E5FF)
        R.string.category_freelance -> Color(0xFF2979FF)
        else -> Color(0xFF9E9E9E)
    }

    val itemBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White
    val itemBorder = MaterialTheme.colorScheme.outlineVariant
    val titleColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF1E293B)
    val amountColor = if (isExpense) Color(0xFFE11D48) else { if (isDark) Color(0xFF34D399) else Color(0xFF10B981) }
    val progressTrackBg = if (isDark) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f) else Color(0x14000000)
    val percentTextColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF64748B)

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
    val dialogSurfaceBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White
    val dialogBorder = MaterialTheme.colorScheme.outlineVariant

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
                colors = CardDefaults.cardColors(containerColor = dialogSurfaceBg),
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
                                                .android16Clickable {
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
    onSave: (amount: Double, categoryId: Int, type: String, note: String, dateLong: Long) -> Unit,
    getString: (Int) -> String,
    lang: String
) {
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("EXPENSE") }

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

    Dialog(onDismissRequest = onDismiss) {
        AnimatedDialogContent {
            val isDark = isAppDark()
            val dialogSurfaceBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White
            val dialogBorder = MaterialTheme.colorScheme.outlineVariant
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .testTag("add_transaction_dialog_surface"),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, dialogBorder),
            colors = CardDefaults.cardColors(containerColor = dialogSurfaceBg)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
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
                }

                // Note (Optional)
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text(getString(R.string.note_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
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
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull()
                            if (amt == null || amt <= 0) {
                                labelErrorText = getString(R.string.error_empty_amount)
                            } else {
                                onSave(amt, selectedCategory, selectedType, noteText, dateMillis)
                            }
                        },
                        modifier = Modifier
                            .weight(1.5f)
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

// Google Authentication Flow Simulator (Provides immersive Interactive syncing)
@Composable
fun GoogleAuthDialog(
    currentUser: String?,
    selectedTheme: String,
    onThemeChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSignInSuccess: (email: String) -> Unit,
    onSignOut: () -> Unit,
    getString: (Int) -> String
) {
    var isVerifyingAccount by remember { mutableStateOf<String?>(null) }
    var emailInput by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf(false) }

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
            val dialogSurfaceBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White
            val dialogBorder = MaterialTheme.colorScheme.outlineVariant
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .testTag("google_auth_dialog_surface"),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, dialogBorder),
            colors = CardDefaults.cardColors(containerColor = dialogSurfaceBg)
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
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = {
                                emailInput = it
                                emailError = false
                            },
                            label = { Text("Google Email Account") },
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
                                text = "Please enter a valid Google email address.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier
                                    .align(Alignment.Start)
                                    .padding(start = 4.dp, top = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val trimmed = emailInput.trim()
                                if (trimmed.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()) {
                                    isVerifyingAccount = trimmed
                                } else {
                                    emailError = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("google_signin_submit"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Sign In"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(getString(R.string.google_signin), fontWeight = FontWeight.Bold)
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
                                .android16Clickable { onThemeChange(themeId) }
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
private val sdfMap = java.util.concurrent.ConcurrentHashMap<String, SimpleDateFormat>()
private val bengaliDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

// Decimal and date formatters helper
fun formatDate(timestamp: Long, lang: String): String {
    val date = Date(timestamp)
    val sdf = sdfMap.getOrPut(lang) {
        val locale = if (lang == "bn") bnLocale else enLocale
        SimpleDateFormat("dd MMM, yyyy", locale)
    }
    return sdf.format(date)
}

fun formatCurrency(amount: Double, lang: String): String {
    val formatted = String.format(Locale.US, "%,.2f", amount)
    if (lang == "bn") {
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
        return sb.toString()
    } else {
        return "$$formatted"
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

    val bgModifier = if (isDark) {
        Modifier.background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0F172A), // Slate 900
                    Color(0xFF020617)  // Slate 950
                )
            )
        )
    } else {
        Modifier.background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFF8FAFC), // Slate 50
                    Color(0xFFF1F5F9)  // Slate 100
                )
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(bgModifier)
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
            
            for (index in particles.indices) {
                val p = particles[index]
                val newX = p.x + p.vx
                val newY = p.y + p.vy
                val newVy = p.vy + 1.1f // Gravity pull
                val newVx = p.vx * 0.96f // Wind drag
                val newRotation = p.rotation + p.vr
                val newAlpha = (1.0f - progress).coerceIn(0f, 1f)
                
                particles[index] = p.copy(
                    x = newX,
                    y = newY,
                    vx = newVx,
                    vy = newVy,
                    rotation = newRotation,
                    alpha = newAlpha
                )
            }
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
                val dialogSurfaceBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                val dialogBorder = MaterialTheme.colorScheme.outlineVariant
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .testTag("support_donation_dialog_surface"),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, dialogBorder),
                    colors = CardDefaults.cardColors(containerColor = dialogSurfaceBg),
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
                                .android16Clickable {
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
                                .android16Clickable {
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
            val dialogSurfaceBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White
            val dialogBorder = MaterialTheme.colorScheme.outlineVariant
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(horizontal = 4.dp)
                    .testTag("about_app_dialog_surface"),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, dialogBorder),
                colors = CardDefaults.cardColors(containerColor = dialogSurfaceBg),
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
                                text = "Mobile Wallet v1.4.0",
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
                                titleEn = "Income & Expense Ledger",
                                descBn = "প্রতিদিনের আয় এবং ব্যয় সহজে ক্যাটাগরি, নোট ও সঠিক তারিখ সহ সংরক্ষণ করুন। কোন ঝামেলা ছাড়াই নিখুঁত বাজেট বজায় রাখুন।",
                                descEn = "Effortlessly catalog daily cash flow with customized categories, remarks, and custom transaction dates."
                            ),
                            FeatureItem(
                                emoji = "📊",
                                titleBn = "বিশদ পরিসংখ্যান ও চার্ট",
                                titleEn = "Visual Financial Analytics",
                                descBn = "রিয়েল-টাইম সুন্দর পাই-চার্ট ও পরিমাণের উপর ভিত্তি করে ক্যাটাগরি অনুযায়ী খরচের পার্সেন্টেজ ও তুলনামূলক গ্রাফস দেখুন।",
                                descEn = "Examine budget patterns instantly using graphical distribution cards, and real-time category ratios."
                            ),
                            FeatureItem(
                                emoji = "🛒",
                                titleBn = "ডিজিটাল বাজার তালিকা",
                                titleEn = "Shopping List (Bajar)",
                                descBn = "বাজারে যাওয়ার আগে ফর্দ তৈরি করুন, প্রয়োজনীয় পরিমাণ লিখে রাখুন। কেনা সম্পন্ন হলে টিক দিয়ে দিন বা ট্র্যাশে পাঠান।",
                                descEn = "Frame future market needs by registering items with quantities, checking items of interest off as you shopping."
                            ),
                            FeatureItem(
                                emoji = "🤝",
                                titleBn = "দেনা-পাওনা (Debt) ট্র্যাকার",
                                titleEn = "Debts & Loans Tracker",
                                descBn = "বন্ধুবান্ধব ও পরিবারের সাথে লেনদেনের বিস্তারিত হিসাব রাখুন। পরিশোধিত হলে এক ক্লিকে 'Settle' করে সম্পূর্ণ রেকর্ড সংরক্ষণ করুন।",
                                descEn = "Identify exact balances owed to or by you in a clear dashboard. Finish historical loans in a single tap."
                            ),
                            FeatureItem(
                                emoji = "🌐",
                                titleBn = "অফলাইন ফার্স্ট ও ক্লাউড সিঙ্ক",
                                titleEn = "Offline First & Sync Engine",
                                descBn = "আপনার সকল ডেটা ফোনেই সুরক্ষিত থাকে, তাই ইন্টারনেট ছাড়াই চলে। আবার ডেটা হারানোর ভয় এড়াতে ক্লাউড ব্যাকআপ নেওয়ার চমৎকার সুবিধা আছে।",
                                descEn = "Operates completely internet-free for high security, with automated triggers to backup/restore securely anytime."
                            ),
                            FeatureItem(
                                emoji = "🌍",
                                titleBn = "সম্পূর্ণ দ্বি-ভাষিক সুবিধা",
                                titleEn = "Dual Language Experience",
                                descBn = "বাঙালিদের জন্য শতভাগ খাঁটি বাংলা ভাষা এবং বৈশ্বিক ব্যবহারের জন্য ইংরেজি ভাষায় সহজেই এক ক্লিকে পরিবর্তন যোগ্য ইন্টারফেস।",
                                descEn = "Instantly toggle between authentic Bengali and professional English language templates from the app header."
                            ),
                            FeatureItem(
                                emoji = "🎨",
                                titleBn = "চোখধাঁধানো গ্লাস-ইউআই ও ডার্ক মোড",
                                titleEn = "Premium Theme & Dark Mode",
                                descBn = "চোখের সুরক্ষার জন্য আরামদায়ক অন্ধকার ডার্ক থিম বা পরিচ্ছন্ন লাইট থিম। সাথে প্রিমিয়াম গ্লাস-মরফিজম ভিজ্যুয়াল ফিনিশ!",
                                descEn = "Dive into seamless, distraction-free modern UI styled elegantly with rich dark theme aesthetics."
                            )
                        )

                        features.forEach { item ->
                            FeatureItemRow(item = item, lang = lang, isDark = isDark)
                            Spacer(modifier = Modifier.height(10.dp))
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
    val cardBg = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f) else Color(0xFFF1F5F9)
    val cardBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

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
fun DeveloperCreditDialog(
    currentUser: String?,
    lang: String = "en",
    onDismiss: () -> Unit
) {
    val isDark = isAppDark()
    var showContactDialog by remember { mutableStateOf(false) }
    var showAboutAppDialog by remember { mutableStateOf(false) }

    if (showContactDialog) {
        DeveloperContactDialog(onDismiss = { showContactDialog = false })
    }

    if (showAboutAppDialog) {
        AboutAppDialog(lang = lang, onDismiss = { showAboutAppDialog = false })
    }

    Dialog(onDismissRequest = onDismiss) {
        AnimatedDialogContent {
            val dialogSurfaceBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White
            val dialogBorder = MaterialTheme.colorScheme.outlineVariant
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .testTag("developer_credit_dialog_surface"),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, dialogBorder),
            colors = CardDefaults.cardColors(containerColor = dialogSurfaceBg)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Glowing Code symbol container
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = if (isDark) listOf(Color(0x3310B981), Color.Transparent) else listOf(Color(0x223B82F6), Color.Transparent)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                color = if (isDark) Color(0x1F10B981) else Color(0x123B82F6),
                                shape = CircleShape
                            )
                            .border(
                                width = 1.dp,
                                color = if (isDark) Color(0x5010B981) else Color(0x403B82F6),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "Code badge",
                            tint = if (isDark) Color(0xFF10B981) else Color(0xFF2563EB),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Developer Credit",
                    style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 1.5.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF0F172A)
                )

                Text(
                    text = "Application Architect & Creator",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.5.sp),
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFF10B981) else Color(0xFF2563EB)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Detail display list cards with elegant layout and representative icons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    val valueColor = MaterialTheme.colorScheme.onSurface
                    val blockBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    val blockBg = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))

                    // Name Info block
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        border = blockBorder,
                        colors = blockBg
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        color = if (isDark) Color(0x2910B981) else Color(0x1A2563EB),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF10B981) else Color(0xFF2563EB),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "NAME",
                                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = labelColor
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Md. Robiul Islam Sakib",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = valueColor
                                )
                            }
                        }
                    }

                    // Department Info block
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        border = blockBorder,
                        colors = blockBg
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        color = if (isDark) Color(0x24A855F7) else Color(0x16A855F7),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Computer,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFFA855F7) else Color(0xFF9333EA),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "DEPARTMENT",
                                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = labelColor
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Computer Science and Technology",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = valueColor
                                )
                            }
                        }
                    }

                    // Institute Info block
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        border = blockBorder,
                        colors = blockBg
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        color = if (isDark) Color(0x243B82F6) else Color(0x163B82F6),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF3B82F6) else Color(0xFF1D4ED8),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "INSTITUTE",
                                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = labelColor
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Park Polytechnic Institute",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = valueColor
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Support Developer options
                var showDonationDialog by remember { mutableStateOf(false) }

                if (showDonationDialog) {
                    SupportDeveloperDonationDialog(
                        lang = lang,
                        onDismiss = { showDonationDialog = false }
                    )
                }

                // New Premium Single Support Button
                Button(
                    onClick = { showDonationDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFFEC4899) else Color(0xFFFCE7F3),
                        contentColor = if (isDark) Color.White else Color(0xFF9D174D)
                    ),
                    border = BorderStroke(1.5.dp, if (isDark) Color(0xFFDB2777) else Color(0xFFFBCFE8)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("support_developer_donation_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Support Icon",
                        modifier = Modifier.size(20.dp),
                        tint = if (isDark) Color.White else Color(0xFFDB2777)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (lang == "bn") "ডেভেলপারকে সাপোর্ট করুন (বিকাশ / নগদ)" else "Support Developer (bKash / Nagad)",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Contact Me Button
                Button(
                    onClick = { showContactDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) MaterialTheme.colorScheme.primaryContainer else Color(0xFFEFF6FF),
                        contentColor = if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else Color(0xFF1D4ED8)
                    ),
                    border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color(0xFFBFDBFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "Contact Icon",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (lang == "bn") "যোগাযোগ করুন" else "Contact Me",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // About App Button
                Button(
                    onClick = { showAboutAppDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) MaterialTheme.colorScheme.secondaryContainer else Color(0xFFF1F5F9),
                        contentColor = if (isDark) MaterialTheme.colorScheme.onSecondaryContainer else Color(0xFF475569)
                    ),
                    border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f) else Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "About App Icon",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (lang == "bn") "অ্যাপ সম্পর্কে জানুন" else "About App & Features",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

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
                        text = if (lang == "bn") "বন্ধ করুন" else "Close",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
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
            val dialogSurfaceBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White
            val dialogBorder = MaterialTheme.colorScheme.outlineVariant
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .testTag("developer_contact_dialog_surface"),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, dialogBorder),
            colors = CardDefaults.cardColors(containerColor = dialogSurfaceBg)
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
            .android16Clickable(onClick = onClick),
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
            val dialogSurfaceBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White
            val dialogBorder = MaterialTheme.colorScheme.outlineVariant
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("bajar_list_dialog_card"),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, dialogBorder),
            colors = CardDefaults.cardColors(containerColor = dialogSurfaceBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
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
                                items(bajarItems, key = { it.id }) { item ->
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
    onSettleDebt: (com.example.data.DebtRecord) -> Unit,
    onDeleteDebt: (com.example.data.DebtRecord) -> Unit,
    onDismiss: () -> Unit
) {
    var nameInput by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }
    var selectedDirection by remember { mutableStateOf("PAYABLE") } // "PAYABLE" or "RECEIVABLE"

    val isDark = isAppDark()

    val titleText = if (lang == "bn") "দেন-পাওনা ট্র্যাকার" else "Debts & Loans Tracker"
    val namePlaceholder = if (lang == "bn") "নাম (যেমন: আবির)" else "Name (e.g., Abir)"
    val amountPlaceholder = if (lang == "bn") "টাকা (যেমন: ৫০০)" else "Amount (e.g., 500)"
    val notePlaceholder = if (lang == "bn") "নোট / কেন দেয়া হয়েছিল (ঐচ্ছিক)" else "Note (Optional)"
    val addButtonText = if (lang == "bn") "যোগ করুন" else "Add Record"
    val emptyText = if (lang == "bn") "কোনো দেন-পাওনার রেকর্ড নেই" else "No active debt records"

    Dialog(onDismissRequest = onDismiss) {
        AnimatedDialogContent {
            val dialogSurfaceBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White
            val dialogBorder = MaterialTheme.colorScheme.outlineVariant
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("debt_list_dialog_card"),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, dialogBorder),
            colors = CardDefaults.cardColors(containerColor = dialogSurfaceBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
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
                        modifier = Modifier.android16Clickable {
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
                            onAddDebt(name, amt, selectedDirection, noteInput.trim())
                            nameInput = ""
                            amountInput = ""
                            noteInput = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth().android16Clickable {
                        val name = nameInput.trim()
                        val amt = amountInput.toDoubleOrNull()
                        if (name.isNotEmpty() && amt != null && amt > 0.0) {
                            onAddDebt(name, amt, selectedDirection, noteInput.trim())
                            nameInput = ""
                            amountInput = ""
                            noteInput = ""
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(addButtonText, fontWeight = FontWeight.Bold)
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
                    val activeDebts = debtRecords.filter { !it.isSettled }
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
                            items(activeDebts, key = { it.id }) { item ->
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
                                                onClick = { onSettleDebt(item) },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (item.direction == "PAYABLE") Color(0xFFEF4444) else Color(0xFF10B981)
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier
                                                    .height(28.dp)
                                                    .android16Clickable { onSettleDebt(item) }
                                            ) {
                                                Text(
                                                    text = if (lang == "bn") "শোধ" else "Settle",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }

                                            // Delete icon button
                                            IconButton(
                                                onClick = { onDeleteDebt(item) },
                                                modifier = Modifier.size(28.dp).android16Clickable { onDeleteDebt(item) }
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
}
}

