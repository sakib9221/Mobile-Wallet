package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.Transaction
import java.text.SimpleDateFormat
import java.util.*

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

    // Helper functions for localized text
    fun getStringResource(id: Int): String {
        return try {
            localizedContext.getString(id)
        } catch (e: Exception) {
            "Other"
        }
    }

    // Modal dialog trigger states
    var showAddDialog by remember { mutableStateOf(false) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var showDeveloperCreditDialog by remember { mutableStateOf(false) }

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
                        val isDark = MaterialTheme.colorScheme.background == Color(0xFF0F172A)
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
                                .clickable { viewModel.toggleLanguage() }
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
                        val isDark = MaterialTheme.colorScheme.background == Color(0xFF0F172A)
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color(0x66FFFFFF))
                                .clickable { showDeveloperCreditDialog = true }
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
                                .clickable { showAuthDialog = true }
                                .testTag("auth_profile_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (currentUser != null) {
                                // Avatar circle with beautiful linear gradient and white border
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = listOf(Color(0xFF60A5FA), Color(0xFF6366F1))
                                            )
                                        )
                                )
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
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    icon = { Icon(Icons.Filled.Add, contentDescription = "Add transaction") },
                    text = { Text(getStringResource(R.string.add_transaction_title)) },
                    containerColor = Color(0xFF2563EB), // Rich Blue as in design HTML
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp), // 2xl rounded as in design HTML
                    modifier = Modifier
                        .navigationBarsPadding()
                        .testTag("floating_add_button")
                )
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
                getString = ::getStringResource,
                onSignInClick = { showAuthDialog = true }
            )

            // Interactive Google Drive Sync block
            GoogleDriveSyncBlock(
                viewModel = viewModel,
                currentUser = currentUser,
                getString = ::getStringResource
            )

            // Minimalist Monthly balance summary card
            DashboardSummaryCard(
                stats = stats,
                lang = currentLanguage,
                getString = ::getStringResource
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Navigation selector for transactions list or monthly category summary
            TabNavigationRow(
                currentTab = currentTab,
                onTabSelect = { viewModel.changeTab(it) },
                getString = ::getStringResource
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Content matching the active tab
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (currentTab == "dashboard") {
                    TransactionsTabContent(
                        transactions = transactions,
                        currentLanguage = currentLanguage,
                        getString = ::getStringResource,
                        onDeleteTransaction = {
                            viewModel.deleteTransaction(it)
                            Toast.makeText(context, getStringResource(R.string.delete_confirm), Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    SummaryTabContent(
                        transactions = transactions,
                        currentLanguage = currentLanguage,
                        getString = ::getStringResource
                    )
                }
            }
        }
    }
    }

    // Dialog 0: Developer Credit Dialog
    if (showDeveloperCreditDialog) {
        DeveloperCreditDialog(
            onDismiss = { showDeveloperCreditDialog = false }
        )
    }

    // Dialog 1: Add Transaction Dialog
    if (showAddDialog) {
        AddTransactionDialog(
            onDismiss = { showAddDialog = false },
            onSave = { amount, categoryId, type, note, dateLong ->
                viewModel.addTransaction(amount, categoryId, type, note, dateLong)
                showAddDialog = false
                Toast.makeText(context, getStringResource(R.string.transaction_saved), Toast.LENGTH_SHORT).show()
            },
            getString = ::getStringResource,
            lang = currentLanguage
        )
    }

    // Dialog 2: Interactive Google Auth Dialog (With Real Feedback Sync)
    if (showAuthDialog) {
        GoogleAuthDialog(
            currentUser = currentUser,
            selectedTheme = selectedTheme,
            onThemeChange = { theme -> viewModel.setTheme(theme) },
            onDismiss = { showAuthDialog = false },
            onSignInSuccess = { email ->
                viewModel.loginWithGoogle(email)
                showAuthDialog = false
                Toast.makeText(context, "Google Signed in as $email. Synchronizing...", Toast.LENGTH_LONG).show()
            },
            onSignOut = {
                viewModel.logout()
                showAuthDialog = false
                Toast.makeText(context, "Logged out. Switched to offline guest mode.", Toast.LENGTH_SHORT).show()
            },
            getString = ::getStringResource
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
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF0F172A)
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
            .clickable { onSignInClick() },
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
                Icon(
                    imageVector = if (currentUser != null) Icons.Default.CloudSync else Icons.Default.CloudOff,
                    contentDescription = "Sync state icon",
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )
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
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF0F172A)
    val cardBg = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.85f) else Color(0x66FFFFFF)
    val cardBorder = if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f) else Color(0x99FFFFFF)

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

            val formattedTotal = formatCurrency(stats.balance, lang)
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
                            text = formatCurrency(stats.totalIncome, lang),
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
                            text = formatCurrency(stats.totalExpense, lang),
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
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF0F172A)
    val tabRowBg = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.5f) else Color(0x33FFFFFF)
    val tabRowBorder = if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f) else Color(0x66FFFFFF)
    val activePaneBg = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xE6FFFFFF)
    val activeTextColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF2563EB)
    val inactiveTextColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF64748B)

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
            val activeModifier = Modifier
                .weight(1.5f)
                .clip(RoundedCornerShape(16.dp))
                .background(activePaneBg) // Crisp glass white active pane
                .padding(vertical = 12.dp)
            
            val inactiveModifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .clickable { onTabSelect("dashboard") }
                .padding(vertical = 12.dp)

            val activeModifierSummary = Modifier
                .weight(1.5f)
                .clip(RoundedCornerShape(16.dp))
                .background(activePaneBg)
                .padding(vertical = 12.dp)

            val inactiveModifierSummary = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .clickable { onTabSelect("summary") }
                .padding(vertical = 12.dp)

            // Tab 1: Dashboard & Transactions list
            Box(
                modifier = if (currentTab == "dashboard") activeModifier else inactiveModifier,
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getString(R.string.recent_transactions_header),
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.3.sp),
                    color = if (currentTab == "dashboard") activeTextColor else inactiveTextColor
                )
            }

            // Tab 2: Categorized Summary info
            Box(
                modifier = if (currentTab == "summary") activeModifierSummary else inactiveModifierSummary,
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getString(R.string.monthly_summary_header),
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.3.sp),
                    color = if (currentTab == "summary") activeTextColor else inactiveTextColor
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
    onDelete: () -> Unit
) {
    val isExpense = transaction.type == "EXPENSE"
    val categoryName = remember(transaction.category, lang) {
        val resId = transaction.category.toIntOrNull()
        if (resId != null) getString(resId) else transaction.category
    }

    val isDark = MaterialTheme.colorScheme.background == Color(0xFF0F172A)
    val itemBg = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.85f) else Color(0x99FFFFFF)
    val itemBorder = if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f) else Color(0x59FFFFFF)
    val titleColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF1E293B)
    val noteColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF64748B)
    val dateColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f) else Color(0xFF94A3B8)
    val amountColor = if (isExpense) Color(0xFFE11D48) else { if (isDark) Color(0xFF34D399) else Color(0xFF10B981) }
    val deleteIconTint = if (isDark) Color(0xFFF87171) else Color(0xFFFDA4AF)

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
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, itemBorder), // white/40 as in design HTML
        colors = CardDefaults.cardColors(containerColor = itemBg), // white/60 backdrop glass
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
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
                    text = formatDate(transaction.dateLong, lang),
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
                    text = "${if (isExpense) "-" else "+"}${formatCurrency(transaction.amount, lang)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = amountColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("delete_transaction_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = deleteIconTint,
                        modifier = Modifier.size(20.dp)
                    )
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
                items(activeList) { (categoryResId, totalAmount) ->
                    val percentage = if (totalSum > 0) (totalAmount / totalSum * 100) else 0.0
                    CategorySummaryItem(
                        categoryResId = categoryResId,
                        amount = totalAmount,
                        percentage = percentage,
                        lang = currentLanguage,
                        getString = getString,
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
    isExpense: Boolean
) {
    val categoryName = getString(categoryResId)
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

    val isDark = MaterialTheme.colorScheme.background == Color(0xFF0F172A)
    val itemBg = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.85f) else Color(0x99FFFFFF)
    val itemBorder = if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f) else Color(0x59FFFFFF)
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
                    text = formatCurrency(amount, lang),
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
                    text = String.format(Locale.US, "%.1f%%", percentage),
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

// Form logic to record new transactions
@OptIn(ExperimentalLayoutApi::class)
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

    var labelErrorText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .testTag("add_transaction_dialog_surface"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                    val isDark = MaterialTheme.colorScheme.background == Color(0xFF0F172A)
                    
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

                // Date Picker row info (Uses current date, can tap to trigger simple update)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .clickable {
                            // Update timestamp randomly to simulate selecting different days!
                            val offsetDays = (1..10).random()
                            dateMillis = System.currentTimeMillis() - (86400000L * offsetDays)
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
                        text = formatDate(dateMillis, lang),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
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
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .testTag("google_auth_dialog_surface"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                Icon(
                                    imageVector = Icons.Default.TaskAlt,
                                    contentDescription = "Success",
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                .clickable { onThemeChange(themeId) }
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

// Decimal and date formatters helper
fun formatDate(timestamp: Long, lang: String): String {
    val date = Date(timestamp)
    val pattern = "dd MMM, yyyy"
    val locale = if (lang == "bn") Locale("bn", "BD") else Locale.ENGLISH
    val sdf = SimpleDateFormat(pattern, locale)
    return sdf.format(date)
}

fun formatCurrency(amount: Double, lang: String): String {
    val formatted = String.format(Locale.US, "%,.2f", amount)
    if (lang == "bn") {
        val englishDigits = listOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
        val bengaliDigits = listOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        val sb = StringBuilder()
        sb.append("৳")
        for (char in formatted) {
            val idx = englishDigits.indexOf(char)
            if (idx != -1) {
                sb.append(bengaliDigits[idx])
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

// Soft fuzzy and performant Ambient Mesh Background
@Composable
fun MeshBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // Base canvas tint matching HTML
    ) {
        // Top-left fuzzy pastel blue bubble (blue-200/40)
        Box(
            modifier = Modifier
                .offset(x = (-60).dp, y = (-60).dp)
                .size(280.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x2E60A5FA), // light pastel blue a bit stronger than 40% for visual flavor
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Middle-right fuzzy pastel purple/pink bubble (purple-200/30)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 80.dp, y = (-40).dp)
                .size(340.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x24C084FC), // light pastel purple
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Bottom-left subtle soft aura
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-40).dp, y = 100.dp)
                .size(240.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x1B818CF8), // indigo aspect
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        content()
    }
}

// Beautiful Frosted Glass Developer Credit section
@Composable
fun DeveloperCreditDialog(
    onDismiss: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF0F172A)
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .testTag("developer_credit_dialog_surface"),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large elegant profile vector header with custom person icon
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Developer Symbol Icon",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Developer Credit",
                    style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 2.sp),
                    fontWeight = FontWeight.Black,
                    color = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF2563EB)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Detail display list cards
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
                        shape = RoundedCornerShape(14.dp),
                        border = blockBorder,
                        colors = blockBg
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
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

                    // Department Info block
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        border = blockBorder,
                        colors = blockBg
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
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

                    // Institute Info block
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        border = blockBorder,
                        colors = blockBg
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
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

@Composable
fun GoogleDriveSyncBlock(
    viewModel: FinanceViewModel,
    currentUser: String?,
    getString: (Int) -> String
) {
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF0F172A)
    
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
                    .clickable { isExpanded = !isExpanded },
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
                                    .clickable { viewModel.toggleAutoSync() }
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
                }
            }
        }
    }
}
