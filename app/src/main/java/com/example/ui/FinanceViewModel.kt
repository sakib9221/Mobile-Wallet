package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Transaction
import com.example.data.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TransactionRepository
    val googleDriveManager = com.example.data.GoogleDriveManager(application)
    
    // User Session: null means Guest (Local) Mode
    private val _currentUser = MutableStateFlow<String?>(null)
    val currentUser: StateFlow<String?> = _currentUser.asStateFlow()

    // Screen state: "all", or "summary" (for categorized summary view)
    private val _uiTab = MutableStateFlow("dashboard")
    val uiTab: StateFlow<String> = _uiTab.asStateFlow()

    private val prefs = application.getSharedPreferences("finance_prefs", android.content.Context.MODE_PRIVATE)

    // Multi-Language state: "en" for English, "bn" for Bengali
    private val _selectedLanguage = MutableStateFlow("en")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    // Theme selection: "system", "light", or "dark"
    private val _selectedTheme = MutableStateFlow("system")
    val selectedTheme: StateFlow<String> = _selectedTheme.asStateFlow()

    val driveSyncState = googleDriveManager.syncState
    val driveLastSyncMessage = googleDriveManager.lastSyncMessage
    val driveOnlineCount = googleDriveManager.onlineCount

    private val _autoSyncOnChanges = MutableStateFlow(true)
    val autoSyncOnChanges: StateFlow<Boolean> = _autoSyncOnChanges.asStateFlow()

    init {
        val transactionDao = AppDatabase.getDatabase(application).transactionDao()
        repository = TransactionRepository(transactionDao)
        _selectedLanguage.value = prefs.getString("selected_lang", "en") ?: "en"
        _selectedTheme.value = prefs.getString("selected_theme", "system") ?: "system"
        _currentUser.value = prefs.getString("selected_user", null)
        _autoSyncOnChanges.value = prefs.getBoolean("auto_sync_drive", true)

        // Reset database once to clear previously seeded default data
        val dbResetZero = prefs.getBoolean("db_reset_zero_v4", false)
        if (!dbResetZero) {
            viewModelScope.launch {
                repository.deleteAll()
                prefs.edit().putBoolean("db_reset_zero_v4", true).apply()
            }
        }
    }

    // Active userId depends on login state (Sharing Eagerly to prevent delays)
    val activeUserId: StateFlow<String> = _currentUser
        .map { it ?: "local_guest" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "local_guest")

    // Retrieve transactions reactively based on active user
    @OptIn(ExperimentalCoroutinesApi::class)
    val transactions: StateFlow<List<Transaction>> = activeUserId
        .flatMapLatest { userId ->
            repository.getAllTransactions(userId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    // Calculate income, expense and balance stats dynamically
    val stats: StateFlow<Stats> = transactions
        .map { transactionList ->
            val income = transactionList.filter { it.type == "INCOME" }.sumOf { it.amount }
            val expense = transactionList.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            Stats(
                totalIncome = income,
                totalExpense = expense,
                balance = income - expense
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = Stats()
        )

    fun toggleLanguage() {
        val nextLang = if (_selectedLanguage.value == "en") "bn" else "en"
        _selectedLanguage.value = nextLang
        prefs.edit().putString("selected_lang", nextLang).apply()
    }

    fun setLanguage(lang: String) {
        if (lang == "en" || lang == "bn") {
            _selectedLanguage.value = lang
            prefs.edit().putString("selected_lang", lang).apply()
        }
    }

    fun setTheme(theme: String) {
        if (theme == "system" || theme == "light" || theme == "dark") {
            _selectedTheme.value = theme
            prefs.edit().putString("selected_theme", theme).apply()
        }
    }

    fun changeTab(tab: String) {
        _uiTab.value = tab
    }

    // Google Sign-In action with user data migration and session persistence
    fun loginWithGoogle(email: String) {
        viewModelScope.launch {
            // 1. Migrate any guest data to this email first so user transactions don't disappear after logging in
            val guestTransactions = repository.getAllTransactions("local_guest").first()
            if (guestTransactions.isNotEmpty()) {
                guestTransactions.forEach { transaction ->
                    val migrated = transaction.copy(id = 0, userId = email) // id=0 to auto-generate a new ID in the database and avoid collision
                    repository.insert(migrated)
                }
                repository.clearAllForUser("local_guest")
            }

            // 2. Set user session state and persist session to SharedPreferences
            _currentUser.value = email
            prefs.edit().putString("selected_user", email).apply()

            // 3. Do not seed any sample data, keeping default data at 0
            prefs.edit().putBoolean("seeded_user_$email", true).apply()

            // 4. Automatically retrieve existing backup from Google Drive on Sign in!
            viewModelScope.launch {
                val restored = googleDriveManager.restoreData(email)
                if (restored.isNotEmpty()) {
                    repository.clearAllForUser(email)
                    restored.forEach { repository.insert(it) }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _currentUser.value = null
            prefs.edit().remove("selected_user").apply()
            googleDriveManager.clearState()
        }
    }

    fun addTransaction(amount: Double, categoryResId: Int, type: String, note: String, dateLong: Long) {
        viewModelScope.launch {
            val transaction = Transaction(
                amount = amount,
                category = categoryResId.toString(), // Store as string resource reference for proper i18n
                type = type,
                dateLong = dateLong,
                note = note,
                userId = activeUserId.value
            )
            repository.insert(transaction)

            // Auto-sync addition to Google Drive if active user is logged in
            if (_autoSyncOnChanges.value && activeUserId.value != "local_guest") {
                val currentList = repository.getAllTransactions(activeUserId.value).first()
                googleDriveManager.backupData(activeUserId.value, currentList)
            }
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.delete(transaction)

            // Auto-sync deletion to Google Drive if active user is logged in
            if (_autoSyncOnChanges.value && activeUserId.value != "local_guest") {
                val currentList = repository.getAllTransactions(activeUserId.value).first()
                googleDriveManager.backupData(activeUserId.value, currentList)
            }
        }
    }

    // Google Drive interactive methods
    fun backupToDrive() {
        viewModelScope.launch {
            val currentList = repository.getAllTransactions(activeUserId.value).first()
            googleDriveManager.backupData(activeUserId.value, currentList)
        }
    }

    fun restoreFromDrive() {
        viewModelScope.launch {
            val restored = googleDriveManager.restoreData(activeUserId.value)
            if (restored.isNotEmpty()) {
                repository.clearAllForUser(activeUserId.value)
                restored.forEach { repository.insert(it) }
            }
        }
    }

    fun toggleAutoSync() {
        val next = !_autoSyncOnChanges.value
        _autoSyncOnChanges.value = next
        prefs.edit().putBoolean("auto_sync_drive", next).apply()
    }

    fun getDriveLastSyncTime(): String {
        return googleDriveManager.getLastSyncTime(activeUserId.value)
    }

    fun getDriveOnlineCount(): Int {
        return googleDriveManager.getOnlineRecordCount(activeUserId.value)
    }

    fun clearDriveSyncState() {
        googleDriveManager.clearState()
    }

    private suspend fun seedSampleDataForUser(userId: String) {
        // Disabled to keep default data 0
    }

    fun seedGuestDataIfNeeded() {
        viewModelScope.launch {
            prefs.edit().putBoolean("seeded_guest", true).apply()
        }
    }

    // Export current transactions to selected URI (Local file)
    fun exportBackupToFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val currentList = repository.getAllTransactions(activeUserId.value).first()
                val jsonStr = googleDriveManager.serializeTransactions(currentList)
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(jsonStr)
                    }
                }
                // Save this json local copy also to the sandbox "google_drive_prefs" SharedPreferences
                // so if Google Drive sync restore is triggered, it has the backup payload!
                val drivePrefs = context.getSharedPreferences("google_drive_prefs", Context.MODE_PRIVATE)
                val nowFormatted = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
                drivePrefs.edit().apply {
                    putString("cloud_store_${activeUserId.value}", jsonStr)
                    putInt("count_${activeUserId.value}", currentList.size)
                    putString("time_${activeUserId.value}", nowFormatted)
                    apply()
                }
            } catch (e: Exception) {
                android.util.Log.e("FinanceViewModel", "Failed to export backup file: ${e.message}")
            }
        }
    }

    // Import transactions from selected URI (Local file)
    fun importBackupFromFile(context: Context, uri: Uri, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val contentBuilder = StringBuilder()
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        var line: String? = reader.readLine()
                        while (line != null) {
                            contentBuilder.append(line)
                            line = reader.readLine()
                        }
                    }
                }
                val jsonStr = contentBuilder.toString()
                val restoredList = googleDriveManager.deserializeTransactions(jsonStr, activeUserId.value)
                if (restoredList.isNotEmpty()) {
                    repository.clearAllForUser(activeUserId.value)
                    restoredList.forEach { repository.insert(it) }
                    
                    // Update our simulated "google_drive_prefs" SharedPreferences too to sync values
                    val drivePrefs = context.getSharedPreferences("google_drive_prefs", Context.MODE_PRIVATE)
                    val nowFormatted = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
                    drivePrefs.edit().apply {
                        putString("cloud_store_${activeUserId.value}", jsonStr)
                        putInt("count_${activeUserId.value}", restoredList.size)
                        putString("time_${activeUserId.value}", nowFormatted)
                        apply()
                    }
                    onSuccess()
                }
            } catch (e: Exception) {
                android.util.Log.e("FinanceViewModel", "Failed to import backup file: ${e.message}")
            }
        }
    }
}

data class Stats(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val balance: Double = 0.0
)
