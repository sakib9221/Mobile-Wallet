package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.BajarItem
import com.example.data.HouseholdBajarRecord
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseholdBajarDialog(
    records: List<HouseholdBajarRecord>,
    bajarItems: List<BajarItem>,
    userSavedName: String?,
    lang: String,
    onAddRecord: (itemName: String, quantity: String, buyerName: String, cost: Double, dateLong: Long, note: String, addToMainExpense: Boolean) -> Unit,
    onUpdateRecord: (HouseholdBajarRecord) -> Unit,
    onDeleteRecord: (HouseholdBajarRecord) -> Unit,
    onAddChecklistItem: (name: String, quantity: String) -> Unit,
    onToggleChecklistItem: (BajarItem, Boolean) -> Unit,
    onDeleteChecklistItem: (BajarItem) -> Unit,
    onFinishChecklistShopping: (totalCost: Double, completedList: String) -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = isAppDark()
    var selectedTab by remember { mutableStateOf(0) } // 0: Log / Entries, 1: Buyer Analytics, 2: Checklist
    var showAddDialog by remember { mutableStateOf(false) }
    var recordToEdit by remember { mutableStateOf<HouseholdBajarRecord?>(null) }
    var recordToDelete by remember { mutableStateOf<HouseholdBajarRecord?>(null) }

    // Month navigation / filter
    val currentCal = remember { Calendar.getInstance() }
    var selectedMonthYearOffset by remember { mutableStateOf(0) } // 0 = Current Month, -1 = Last Month, 999 = All Time

    val activeCal = remember(selectedMonthYearOffset) {
        if (selectedMonthYearOffset == 999) {
            null
        } else {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, selectedMonthYearOffset)
            cal
        }
    }

    val monthYearLabel = remember(activeCal, lang, selectedMonthYearOffset) {
        if (selectedMonthYearOffset == 999) {
            if (lang == "bn") "সকল রেকর্ড (All Time)" else "All Time Records"
        } else if (activeCal != null) {
            val format = if (lang == "bn") {
                SimpleDateFormat("MMMM yyyy", Locale("bn"))
            } else {
                SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)
            }
            format.format(activeCal.time)
        } else {
            ""
        }
    }

    // Filter records according to selected month
    val filteredRecords = remember(records, selectedMonthYearOffset, activeCal) {
        if (selectedMonthYearOffset == 999 || activeCal == null) {
            records
        } else {
            val targetYear = activeCal.get(Calendar.YEAR)
            val targetMonth = activeCal.get(Calendar.MONTH)
            val calEntry = Calendar.getInstance()
            records.filter { record ->
                calEntry.timeInMillis = record.dateLong
                calEntry.get(Calendar.YEAR) == targetYear && calEntry.get(Calendar.MONTH) == targetMonth
            }
        }
    }

    // Key stats
    val totalMonthCost = remember(filteredRecords) {
        filteredRecords.sumOf { it.cost }
    }

    val todayCost = remember(records) {
        val todayCal = Calendar.getInstance()
        val entryCal = Calendar.getInstance()
        records.filter { record ->
            entryCal.timeInMillis = record.dateLong
            entryCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                    entryCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
        }.sumOf { it.cost }
    }

    // Search and buyer filter within current month
    var searchQuery by remember { mutableStateOf("") }
    var selectedBuyerFilter by remember { mutableStateOf("ALL") }

    val displayedRecords = remember(filteredRecords, searchQuery, selectedBuyerFilter) {
        var list = filteredRecords
        if (selectedBuyerFilter != "ALL") {
            list = list.filter { it.buyerName.equals(selectedBuyerFilter, ignoreCase = true) }
        }
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            list = list.filter {
                it.itemName.lowercase().contains(q) ||
                        it.buyerName.lowercase().contains(q) ||
                        it.note.lowercase().contains(q) ||
                        it.quantity.lowercase().contains(q)
            }
        }
        list
    }

    // Available unique buyers
    val uniqueBuyers = remember(records, userSavedName) {
        val set = linkedSetOf<String>()
        if (!userSavedName.isNullOrBlank()) set.add(userSavedName)
        set.add("আমি")
        set.add("বাবা")
        set.add("মা")
        set.add("বউ")
        set.add("ভাই")
        records.forEach { if (it.buyerName.isNotBlank()) set.add(it.buyerName) }
        set.toList()
    }

    // Buyer analytics stats
    val buyerStats = remember(filteredRecords, totalMonthCost) {
        val map = mutableMapOf<String, Pair<Double, Int>>() // Buyer -> (Total Cost, Items Count)
        filteredRecords.forEach { record ->
            val cur = map[record.buyerName] ?: Pair(0.0, 0)
            map[record.buyerName] = Pair(cur.first + record.cost, cur.second + 1)
        }
        map.toList().sortedByDescending { it.second.first }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().testTag("household_bajar_dialog"),
            color = if (isDark) Color(0xFF0B132B) else Color(0xFFF8FAFC)
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .background(
                                if (isDark) Color(0xFF1E293B).copy(alpha = 0.95f)
                                else Color.White.copy(alpha = 0.95f)
                            )
                            .border(
                                BorderStroke(
                                    1.dp,
                                    if (isDark) Color(0xFF334155).copy(alpha = 0.5f)
                                    else Color(0xFFE2E8F0)
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF10B981), Color(0xFF059669))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingBag,
                                        contentDescription = "Household Bazar",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (lang == "bn") "ঘরের মাসিক বাজার" else "Household Monthly Bazar",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = if (lang == "bn") "খরচের খাতা ও কে কি কিনলো মনিটরিং" else "Grocery diary & buyer monitoring",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                    )
                                }
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.testTag("household_bajar_close_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Month selector bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = {
                                    if (selectedMonthYearOffset == 999) {
                                        selectedMonthYearOffset = 0
                                    } else {
                                        selectedMonthYearOffset -= 1
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.ChevronLeft,
                                    contentDescription = "Previous Month",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)),
                                modifier = Modifier.android16Clickable {
                                    selectedMonthYearOffset = if (selectedMonthYearOffset == 999) 0 else 999
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = monthYearLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color.White else Color(0xFF0F172A)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    if (selectedMonthYearOffset == 999) {
                                        selectedMonthYearOffset = 0
                                    } else {
                                        selectedMonthYearOffset += 1
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = "Next Month",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                },
                floatingActionButton = {
                    if (selectedTab == 0 || selectedTab == 1) {
                        ExtendedFloatingActionButton(
                            onClick = { showAddDialog = true },
                            containerColor = Color(0xFF10B981),
                            contentColor = Color.White,
                            shape = RoundedCornerShape(16.dp),
                            elevation = FloatingActionButtonDefaults.elevation(6.dp),
                            modifier = Modifier.testTag("fab_add_bajar_record")
                        ) {
                            Icon(Icons.Default.AddShoppingCart, contentDescription = "Add Grocery Entry")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (lang == "bn") "নতুন বাজার এন্ট্রি" else "Add Grocery Entry",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Summary Banner Cards
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Monthly Total Card
                        Card(
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) Color(0xFF1E293B) else Color.White
                            ),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color(0xFF10B981), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (lang == "bn") "মাসিক মোট বাজার" else "Monthly Total",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatCurrency(totalMonthCost, lang),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF10B981),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Today's Expense Card
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) Color(0xFF1E293B) else Color.White
                            ),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (lang == "bn") "আজকের বাজার" else "Today's Spent",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatCurrency(todayCost, lang),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Items count Card
                        Card(
                            modifier = Modifier.weight(0.9f),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) Color(0xFF1E293B) else Color.White
                            ),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (lang == "bn") "মোট এন্ট্রি" else "Total Items",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (lang == "bn") "${filteredRecords.size} টি" else "${filteredRecords.size} items",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFF59E0B) else Color(0xFFD97706)
                                )
                            }
                        }
                    }

                    // Modern Segmented Tabs
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = {},
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            modifier = Modifier.testTag("tab_bajar_entries"),
                            text = {
                                Text(
                                    text = if (lang == "bn") "বাজারের খাতা" else "Bazar Diary",
                                    fontWeight = if (selectedTab == 0) FontWeight.ExtraBold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            modifier = Modifier.testTag("tab_bajar_monitoring"),
                            text = {
                                Text(
                                    text = if (lang == "bn") "কে কত বাজার করলো" else "Buyer Breakdown",
                                    fontWeight = if (selectedTab == 1) FontWeight.ExtraBold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            modifier = Modifier.testTag("tab_bajar_checklist"),
                            text = {
                                Text(
                                    text = if (lang == "bn") "বাজারের ফর্দ (Checklist)" else "Shopping Checklist",
                                    fontWeight = if (selectedTab == 2) FontWeight.ExtraBold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tab Content
                    when (selectedTab) {
                        0 -> {
                            // Tab 0: Bazar Log / Entries
                            HouseholdBajarLogTab(
                                records = displayedRecords,
                                totalMonthCost = totalMonthCost,
                                searchQuery = searchQuery,
                                onSearchQueryChange = { searchQuery = it },
                                selectedBuyerFilter = selectedBuyerFilter,
                                onBuyerFilterChange = { selectedBuyerFilter = it },
                                uniqueBuyers = uniqueBuyers,
                                isDark = isDark,
                                lang = lang,
                                onEdit = { recordToEdit = it },
                                onDelete = { recordToDelete = it },
                                onAddNew = { showAddDialog = true }
                            )
                        }
                        1 -> {
                            // Tab 1: Buyer Analytics & Monitoring
                            HouseholdBajarBuyerAnalyticsTab(
                                buyerStats = buyerStats,
                                totalMonthCost = totalMonthCost,
                                records = filteredRecords,
                                isDark = isDark,
                                lang = lang
                            )
                        }
                        2 -> {
                            // Tab 2: Shopping Checklist
                            HouseholdBajarChecklistTab(
                                items = bajarItems,
                                isDark = isDark,
                                lang = lang,
                                onAdd = onAddChecklistItem,
                                onToggle = onToggleChecklistItem,
                                onDelete = onDeleteChecklistItem,
                                onFinishShopping = onFinishChecklistShopping
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Record Dialog
    if (showAddDialog) {
        AddHouseholdBajarDialog(
            userSavedName = userSavedName,
            uniqueBuyers = uniqueBuyers,
            lang = lang,
            onDismiss = { showAddDialog = false },
            onSave = { item, qty, buyer, cost, dateLong, note, addToMain ->
                onAddRecord(item, qty, buyer, cost, dateLong, note, addToMain)
                showAddDialog = false
            }
        )
    }

    // Edit Record Dialog
    if (recordToEdit != null) {
        EditHouseholdBajarDialog(
            record = recordToEdit!!,
            uniqueBuyers = uniqueBuyers,
            lang = lang,
            onDismiss = { recordToEdit = null },
            onSave = { updated ->
                onUpdateRecord(updated)
                recordToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    if (recordToDelete != null) {
        DeleteHouseholdBajarConfirmationDialog(
            record = recordToDelete!!,
            lang = lang,
            onConfirm = {
                onDeleteRecord(recordToDelete!!)
                recordToDelete = null
            },
            onDismiss = { recordToDelete = null }
        )
    }
}

// -------------------------------------------------------------
// TAB 0: BAZAR LOG / ENTRIES
// -------------------------------------------------------------
@Composable
private fun HouseholdBajarLogTab(
    records: List<HouseholdBajarRecord>,
    totalMonthCost: Double,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedBuyerFilter: String,
    onBuyerFilterChange: (String) -> Unit,
    uniqueBuyers: List<String>,
    isDark: Boolean,
    lang: String,
    onEdit: (HouseholdBajarRecord) -> Unit,
    onDelete: (HouseholdBajarRecord) -> Unit,
    onAddNew: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Search & Filter controls
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("household_bajar_search_input"),
            placeholder = {
                Text(if (lang == "bn") "পণ্য বা ক্রেতার নাম দিয়ে খুঁজুন..." else "Search by item or buyer...")
            },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = if (isDark) Color(0xFF1E293B) else Color.White,
                unfocusedContainerColor = if (isDark) Color(0xFF1E293B) else Color.White,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Buyer chips filter
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = selectedBuyerFilter == "ALL",
                    onClick = { onBuyerFilterChange("ALL") },
                    label = { Text(if (lang == "bn") "সকল ক্রেতা" else "All Buyers", fontSize = 12.sp) },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
            items(uniqueBuyers) { buyer ->
                FilterChip(
                    selected = selectedBuyerFilter.equals(buyer, ignoreCase = true),
                    onClick = { onBuyerFilterChange(buyer) },
                    label = { Text(buyer, fontSize = 12.sp) },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (records.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBasket,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Text(
                        text = if (lang == "bn") "কোন বাজার এন্ট্রি পাওয়া যায়নি!" else "No grocery purchase records found!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                    )
                    Text(
                        text = if (lang == "bn") "নিচের বাটনে চাপ দিয়ে আজকের বাজারের হিসাব লিখুন।" else "Tap the add button below to record your first purchase.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = onAddNew,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (lang == "bn") "বাজার এন্ট্রি করুন" else "Add Grocery Entry")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 84.dp)
            ) {
                items(
                    items = records,
                    key = { it.id },
                    contentType = { "household_bajar_record" }
                ) { record ->
                    HouseholdBajarItemCard(
                        record = record,
                        isDark = isDark,
                        lang = lang,
                        onEdit = { onEdit(record) },
                        onDelete = { onDelete(record) }
                    )
                }
            }
        }
    }
}

// Single Household Bajar Item Card
@Composable
private fun HouseholdBajarItemCard(
    record: HouseholdBajarRecord,
    isDark: Boolean,
    lang: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val formattedDate = remember(record.dateLong, lang) {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
        sdf.format(Date(record.dateLong))
    }

    val cardBg = if (isDark) Color(0xFF1E293B).copy(alpha = 0.65f) else Color.White
    val cardBorder = if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFE2E8F0)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder),
        modifier = Modifier.fillMaxWidth().testTag("bajar_record_card_${record.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Pill
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF10B981).copy(alpha = 0.25f), Color(0xFF10B981).copy(alpha = 0.08f))
                        )
                    )
                    .border(1.dp, Color(0xFF10B981).copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingBasket,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = record.itemName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Quantity badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0xFF0284C7).copy(alpha = 0.2f) else Color(0xFFE0F2FE),
                        border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = record.quantity,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0369A1),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Buyer & Date row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Buyer badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isDark) Color(0xFFF59E0B).copy(alpha = 0.15f) else Color(0xFFFEF3C7)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = record.buyerName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFFBBF24) else Color(0xFF92400E)
                            )
                        }
                    }

                    Text(
                        text = "• $formattedDate",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }

                if (record.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "নোট: ${record.note}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Cost & Action buttons
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f))
                ) {
                    Text(
                        text = formatCurrency(record.cost, lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF10B981),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp).testTag("edit_bajar_record_${record.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp).testTag("delete_bajar_record_${record.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 1: BUYER ANALYTICS & MONITORING
// -------------------------------------------------------------
@Composable
private fun HouseholdBajarBuyerAnalyticsTab(
    buyerStats: List<Pair<String, Pair<Double, Int>>>,
    totalMonthCost: Double,
    records: List<HouseholdBajarRecord>,
    isDark: Boolean,
    lang: String
) {
    if (buyerStats.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (lang == "bn") "কোন সদস্যের বাজার তথ্য পাওয়া যায়নি!" else "No buyer analytics recorded for this period!",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 84.dp)
        ) {
            item {
                Text(
                    text = if (lang == "bn") "সদস্যদের অবদান ও পর্যবেক্ষণ" else "Buyer Contributions & Monitoring",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (lang == "bn") "ঘরের বাজারের কোন সদস্য কত টাকা বাজার করেছেন তার বিবরণ" else "Overview of who spent how much on household groceries",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
            }

            items(buyerStats) { (buyerName, stat) ->
                val (spent, count) = stat
                val percentage = if (totalMonthCost > 0) (spent / totalMonthCost) * 100 else 0.0
                var expanded by remember { mutableStateOf(false) }

                val buyerItems = remember(records, buyerName) {
                    records.filter { it.buyerName.equals(buyerName, ignoreCase = true) }
                }

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF1E293B).copy(alpha = 0.7f) else Color.White
                    ),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth().android16Clickable { expanded = !expanded }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF3B82F6), Color(0xFF2563EB))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = buyerName.take(1).uppercase(),
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        fontSize = 16.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = buyerName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = if (lang == "bn") "$count টি পণ্য বাজার করেছেন" else "$count items purchased",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = formatCurrency(spent, lang),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF10B981)
                                )
                                Text(
                                    text = String.format(Locale.US, "%.1f%%", percentage),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Progress Bar
                        LinearProgressIndicator(
                            progress = { (percentage / 100f).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF10B981),
                            trackColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                        )

                        // Expandable item list
                        AnimatedVisibility(visible = expanded) {
                            Column(modifier = Modifier.padding(top = 14.dp)) {
                                HorizontalDivider(
                                    color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                                    modifier = Modifier.padding(bottom = 10.dp)
                                )
                                Text(
                                    text = if (lang == "bn") "${buyerName}-এর ক্রয়কৃত পণ্যসমূহ:" else "Items bought by $buyerName:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                buyerItems.forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "• ${item.itemName} (${item.quantity})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
                                        )
                                        Text(
                                            text = formatCurrency(item.cost, lang),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
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

// -------------------------------------------------------------
// TAB 2: SHOPPING CHECKLIST
// -------------------------------------------------------------
@Composable
private fun HouseholdBajarChecklistTab(
    items: List<BajarItem>,
    isDark: Boolean,
    lang: String,
    onAdd: (name: String, quantity: String) -> Unit,
    onToggle: (BajarItem, Boolean) -> Unit,
    onDelete: (BajarItem) -> Unit,
    onFinishShopping: (totalCost: Double, completedList: String) -> Unit
) {
    var nameInput by remember { mutableStateOf("") }
    var qtyInput by remember { mutableStateOf("") }
    var showFinishDialog by remember { mutableStateOf(false) }
    var finishTotalCostInput by remember { mutableStateOf("") }

    val completedItems = remember(items) { items.filter { it.isCompleted } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Quick add item input
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF1E293B) else Color.White
            ),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text(if (lang == "bn") "বাজারের ফর্দ (যেমন: আলু, পেঁয়াজ)" else "Item Name (e.g., Potato, Onion)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("checklist_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = qtyInput,
                        onValueChange = { qtyInput = it },
                        label = { Text(if (lang == "bn") "পরিমাণ (যেমন: ২ কেজি)" else "Qty (e.g., 2 kg)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("checklist_qty_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            val name = nameInput.trim()
                            val qty = qtyInput.trim()
                            if (name.isNotEmpty()) {
                                onAdd(name, if (qty.isEmpty()) "1" else qty)
                                nameInput = ""
                                qtyInput = ""
                            }
                        },
                        modifier = Modifier.height(52.dp).testTag("checklist_add_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (lang == "bn") "যুক্ত করুন" else "Add")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Bar for finishing checklist
        if (completedItems.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (lang == "bn") "${completedItems.size} টি কেনা হয়েছে" else "${completedItems.size} items purchased",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )

                Button(
                    onClick = { showFinishDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (lang == "bn") "বাজার শেষ করুন" else "Finish Shopping", fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Checklist,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = if (lang == "bn") "কোন ফর্দ নেই! নতুন পণ্য যুক্ত করুন।" else "Checklist is empty! Add items above.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 84.dp)
            ) {
                items(items = items, key = { it.id }) { item ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF1E293B) else Color.White
                        ),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = item.isCompleted,
                                onCheckedChange = { onToggle(item, it) },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.isCompleted) Color(0xFF94A3B8) else if (isDark) Color.White else Color(0xFF0F172A)
                                )
                                Text(
                                    text = "পরিমাণ: ${item.quantity}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                            }
                            IconButton(onClick = { onDelete(item) }) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text(if (lang == "bn") "বাজারের মোট খরচ" else "Total Shopping Cost") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (lang == "bn") "চেকলিস্টের ${completedItems.size} টি পণ্য কেনা সম্পন্ন হয়েছে। মোট কত টাকা খরচ হলো লিখুন:"
                        else "You completed ${completedItems.size} items. Enter total spent:"
                    )
                    OutlinedTextField(
                        value = finishTotalCostInput,
                        onValueChange = { finishTotalCostInput = it },
                        label = { Text(if (lang == "bn") "মোট খরচ (৳)" else "Total Cost (৳)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cost = finishTotalCostInput.toDoubleOrNull() ?: 0.0
                        val listStr = completedItems.joinToString(", ") { "${it.name} (${it.quantity})" }
                        onFinishShopping(cost, listStr)
                        showFinishDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text(if (lang == "bn") "সম্পন্ন করুন" else "Finish & Record")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) {
                    Text(if (lang == "bn") "বাতিল" else "Cancel")
                }
            }
        )
    }
}

// -------------------------------------------------------------
// ADD HOUSEHOLD BAJAR DIALOG
// -------------------------------------------------------------
@Composable
fun AddHouseholdBajarDialog(
    userSavedName: String?,
    uniqueBuyers: List<String>,
    lang: String,
    onDismiss: () -> Unit,
    onSave: (itemName: String, quantity: String, buyerName: String, cost: Double, dateLong: Long, note: String, addToMainExpense: Boolean) -> Unit
) {
    val isDark = isAppDark()
    var itemName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("১ কেজি") }
    var buyerName by remember { mutableStateOf(userSavedName?.takeIf { it.isNotBlank() } ?: "আমি") }
    var costText by remember { mutableStateOf("") }
    var dateLong by remember { mutableStateOf(System.currentTimeMillis()) }
    var note by remember { mutableStateOf("") }
    var addToMainExpense by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val formattedDate = remember(dateLong, lang) {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH)
        sdf.format(Date(dateLong))
    }

    val quickItems = remember {
        listOf("পেঁয়াজ", "আলু", "সয়াবিন তেল", "চাল", "ডাল", "মাছ", "মুরগি", "গরুর মাংস", "সবজি", "ডিম", "দুধ", "মসলা", "লবণ", "চিনি", "আদা-রসুন", "ফলমূল")
    }
    val quickQuantities = remember {
        listOf("১ কেজি", "২ কেজি", "৫ কেজি", "৫০০ গ্রাম", "২৫০ গ্রাম", "১ লিটার", "২ লিটার", "৫ লিটার", "১ ডজন", "১ হালি", "১ প্যাকেট", "১টি")
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .testTag("add_household_bajar_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = if (isDark) Color(0xFF1E293B) else Color.White,
            border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AddShoppingCart,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (lang == "bn") "নতুন বাজার এন্ট্রি" else "Add Grocery Purchase",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))

                // Item Name input & quick chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = itemName,
                        onValueChange = {
                            itemName = it
                            errorMessage = null
                        },
                        label = { Text(if (lang == "bn") "পণ্যের নাম (যেমন: পেঁয়াজ, চাল)" else "Item Name (e.g. Onion, Rice)") },
                        placeholder = { Text(if (lang == "bn") "পণ্যের নাম লিখুন" else "Enter item name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_bajar_item_name_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(quickItems) { item ->
                            SuggestionChip(
                                onClick = { itemName = item },
                                label = { Text(item, fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // Quantity & Price Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text(if (lang == "bn") "পরিমাণ" else "Quantity") },
                        placeholder = { Text("১ কেজি") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("add_bajar_quantity_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = costText,
                        onValueChange = {
                            costText = it
                            errorMessage = null
                        },
                        label = { Text(if (lang == "bn") "খরচ / দাম (৳)" else "Cost / Price (৳)") },
                        placeholder = { Text("১২০") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1.2f).testTag("add_bajar_cost_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Quick Quantity Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickQuantities) { q ->
                        SuggestionChip(
                            onClick = { quantity = q },
                            label = { Text(q, fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // Buyer Name input & suggestion chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = buyerName,
                        onValueChange = {
                            buyerName = it
                            errorMessage = null
                        },
                        label = { Text(if (lang == "bn") "কে বাজার করেছে / ক্রেতার নাম" else "Buyer Name / Who bought it") },
                        placeholder = { Text(if (lang == "bn") "যেমন: আমি, বাবা, মা" else "e.g., Me, Father, Mother") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_bajar_buyer_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(uniqueBuyers) { b ->
                            SuggestionChip(
                                onClick = { buyerName = b },
                                label = { Text(b, fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // Date Picker row
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF334155).copy(alpha = 0.4f) else Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth().android16Clickable { showDatePicker = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (lang == "bn") "বাজারের তারিখ" else "Purchase Date",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                            )
                        }

                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Note (Optional)
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(if (lang == "bn") "নোট / দোকানের নাম (ঐচ্ছিক)" else "Note / Shop Name (Optional)") },
                    placeholder = { Text(if (lang == "bn") "যেমন: কারওয়ান বাজার থেকে কেনা" else "e.g. Bought from local market") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_bajar_note_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Sync to main wallet expense toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0xFF334155).copy(alpha = 0.3f) else Color(0xFFF8FAFC))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (lang == "bn") "মেইন ওয়ালেট খরচ হিসেবে কাটুন" else "Deduct from Main Wallet Balance",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        Text(
                            text = if (lang == "bn") "এই টাকাটি প্রধান ব্যালেন্স থেকেও বিয়োগ হবে" else "Sync this expense to your main transactions",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }

                    Switch(
                        checked = addToMainExpense,
                        onCheckedChange = { addToMainExpense = it },
                        modifier = Modifier.testTag("add_bajar_sync_switch")
                    )
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (lang == "bn") "বাতিল" else "Cancel")
                    }

                    Button(
                        onClick = {
                            if (itemName.isBlank()) {
                                errorMessage = if (lang == "bn") "পণ্যের নাম লিখুন!" else "Please enter item name!"
                                return@Button
                            }
                            val cost = costText.toDoubleOrNull()
                            if (cost == null || cost < 0) {
                                errorMessage = if (lang == "bn") "সঠিক খরচের পরিমাণ লিখুন!" else "Please enter a valid cost!"
                                return@Button
                            }
                            if (buyerName.isBlank()) {
                                errorMessage = if (lang == "bn") "ক্রেতার নাম লিখুন!" else "Please enter buyer name!"
                                return@Button
                            }
                            onSave(itemName, quantity, buyerName, cost, dateLong, note, addToMainExpense)
                        },
                        modifier = Modifier.weight(1.5f).testTag("add_bajar_confirm_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text(if (lang == "bn") "সংরক্ষণ করুন" else "Save Entry", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        BeautifulInteractiveCalendarDialog(
            initialSelectedDateMillis = dateLong,
            lang = lang,
            onDismissRequest = { showDatePicker = false },
            onDateSelected = { selectedDate ->
                dateLong = selectedDate
            }
        )
    }
}

// -------------------------------------------------------------
// EDIT HOUSEHOLD BAJAR DIALOG
// -------------------------------------------------------------
@Composable
fun EditHouseholdBajarDialog(
    record: HouseholdBajarRecord,
    uniqueBuyers: List<String>,
    lang: String,
    onDismiss: () -> Unit,
    onSave: (HouseholdBajarRecord) -> Unit
) {
    val isDark = isAppDark()
    var itemName by remember { mutableStateOf(record.itemName) }
    var quantity by remember { mutableStateOf(record.quantity) }
    var buyerName by remember { mutableStateOf(record.buyerName) }
    var costText by remember { mutableStateOf(record.cost.toString()) }
    var dateLong by remember { mutableStateOf(record.dateLong) }
    var note by remember { mutableStateOf(record.note) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val formattedDate = remember(dateLong, lang) {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH)
        sdf.format(Date(dateLong))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .testTag("edit_household_bajar_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = if (isDark) Color(0xFF1E293B) else Color.White,
            border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3B82F6).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (lang == "bn") "বাজার তথ্য সম্পাদনা" else "Edit Grocery Record",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))

                OutlinedTextField(
                    value = itemName,
                    onValueChange = {
                        itemName = it
                        errorMessage = null
                    },
                    label = { Text(if (lang == "bn") "পণ্যের নাম" else "Item Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_bajar_item_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text(if (lang == "bn") "পরিমাণ" else "Quantity") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("edit_bajar_quantity_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = costText,
                        onValueChange = {
                            costText = it
                            errorMessage = null
                        },
                        label = { Text(if (lang == "bn") "খরচ / দাম (৳)" else "Cost / Price (৳)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1.2f).testTag("edit_bajar_cost_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = buyerName,
                        onValueChange = {
                            buyerName = it
                            errorMessage = null
                        },
                        label = { Text(if (lang == "bn") "ক্রেতার নাম" else "Buyer Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_bajar_buyer_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(uniqueBuyers) { b ->
                            SuggestionChip(
                                onClick = { buyerName = b },
                                label = { Text(b, fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // Date Picker row
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF334155).copy(alpha = 0.4f) else Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth().android16Clickable { showDatePicker = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (lang == "bn") "বাজারের তারিখ" else "Purchase Date",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                            )
                        }

                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(if (lang == "bn") "নোট / দোকানের নাম (ঐচ্ছিক)" else "Note / Shop Name (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_bajar_note_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (lang == "bn") "বাতিল" else "Cancel")
                    }

                    Button(
                        onClick = {
                            if (itemName.isBlank()) {
                                errorMessage = if (lang == "bn") "পণ্যের নাম লিখুন!" else "Please enter item name!"
                                return@Button
                            }
                            val cost = costText.toDoubleOrNull()
                            if (cost == null || cost < 0) {
                                errorMessage = if (lang == "bn") "সঠিক খরচের পরিমাণ লিখুন!" else "Please enter a valid cost!"
                                return@Button
                            }
                            if (buyerName.isBlank()) {
                                errorMessage = if (lang == "bn") "ক্রেতার নাম লিখুন!" else "Please enter buyer name!"
                                return@Button
                            }
                            onSave(
                                record.copy(
                                    itemName = itemName.trim(),
                                    quantity = if (quantity.isBlank()) "1" else quantity.trim(),
                                    buyerName = buyerName.trim(),
                                    cost = cost,
                                    dateLong = dateLong,
                                    note = note.trim()
                                )
                            )
                        },
                        modifier = Modifier.weight(1.5f).testTag("edit_bajar_save_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(if (lang == "bn") "আপডেট করুন" else "Save Changes", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        BeautifulInteractiveCalendarDialog(
            initialSelectedDateMillis = dateLong,
            lang = lang,
            onDismissRequest = { showDatePicker = false },
            onDateSelected = { selectedDate ->
                dateLong = selectedDate
            }
        )
    }
}

// -------------------------------------------------------------
// DELETE CONFIRMATION DIALOG
// -------------------------------------------------------------
@Composable
fun DeleteHouseholdBajarConfirmationDialog(
    record: HouseholdBajarRecord,
    lang: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (lang == "bn") "বাজার এন্ট্রি মুছে ফেলবেন?" else "Delete Grocery Record?",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = if (lang == "bn") "আপনি কি নিশ্চিত যে '${record.itemName}' (${record.quantity}) - ${formatCurrency(record.cost, lang)} এন্ট্রিটি মুছে ফেলতে চান?"
                else "Are you sure you want to delete '${record.itemName}' (${record.quantity}) - ${formatCurrency(record.cost, lang)}?"
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(if (lang == "bn") "মুছে ফেলুন" else "Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (lang == "bn") "বাতিল" else "Cancel")
            }
        }
    )
}
