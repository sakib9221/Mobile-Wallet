package com.example.ui

import android.widget.Toast
import android.app.Application
import android.content.Context
import android.net.Uri
import android.content.ContentUris
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Transaction
import com.example.data.TransactionRepository
import com.example.data.GoogleDriveManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import org.json.JSONObject
import org.json.JSONArray
import java.io.File
import android.content.ContentValues
import android.provider.MediaStore
import android.os.Build
import android.os.Environment

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TransactionRepository
    val googleDriveManager = GoogleDriveManager(application)
    
    val driveSyncState = googleDriveManager.syncState
    val driveLastSyncMessage = googleDriveManager.lastSyncMessage
    val driveOnlineCount = googleDriveManager.onlineCount

    // User Session: Always null (Guest Mode) as requested
    private val _currentUser = MutableStateFlow<String?>(null)
    val currentUser: StateFlow<String?> = _currentUser.asStateFlow()

    // Screen state: "dashboard" or other tabs
    private val _uiTab = MutableStateFlow("dashboard")
    val uiTab: StateFlow<String> = _uiTab.asStateFlow()

    private val prefs = application.getSharedPreferences("finance_prefs", Context.MODE_PRIVATE)

    // Multi-Language state: "en" for English, "bn" for Bengali
    private val _selectedLanguage = MutableStateFlow("en")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    // Theme selection: "system", "light", or "dark"
    private val _selectedTheme = MutableStateFlow("system")
    val selectedTheme: StateFlow<String> = _selectedTheme.asStateFlow()

    private val _autoSyncOnChanges = MutableStateFlow(true)
    val autoSyncOnChanges: StateFlow<Boolean> = _autoSyncOnChanges.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = TransactionRepository(db.transactionDao(), db.bajarItemDao(), db.debtRecordDao())
        _selectedLanguage.value = prefs.getString("selected_lang", "en") ?: "en"
        val savedTheme = prefs.getString("selected_theme", "system") ?: "system"
        _selectedTheme.value = if (savedTheme == "liquid_glass") "system" else savedTheme
        
        // Always enforce local guest user
        _currentUser.value = null

        // Reset database once if not done, to clear old seeds
        val dbResetZero = prefs.getBoolean("db_reset_zero_v5", false)
        if (!dbResetZero) {
            viewModelScope.launch {
                repository.deleteAll()
                repository.clearBajarItemsForUser("local_guest")
                repository.clearDebtsForUser("local_guest")
                prefs.edit().putBoolean("db_reset_zero_v5", true).apply()
                
                // Trigger an initial empty backup
                autoBackupData()
            }
        }
    }

    // Active userId is always guest
    val activeUserId: StateFlow<String> = flowOf("local_guest")
        .stateIn(viewModelScope, SharingStarted.Eagerly, "local_guest")

    // Retrieve transactions reactively
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

    // Retrieve Bajar items reactively
    @OptIn(ExperimentalCoroutinesApi::class)
    val bajarItems: StateFlow<List<com.example.data.BajarItem>> = activeUserId
        .flatMapLatest { userId ->
            repository.getAllBajarItems(userId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    // Retrieve Debt items reactively
    @OptIn(ExperimentalCoroutinesApi::class)
    val debtRecords: StateFlow<List<com.example.data.DebtRecord>> = activeUserId
        .flatMapLatest { userId ->
            repository.getAllDebts(userId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    // Calculate stats dynamically
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

    // Google Sign-In stubs for compatibility
    fun loginWithGoogle(email: String) {
        // Disabled
    }

    fun logout() {
        // Disabled
    }

    // Core write operations trigger an automatic backup
    fun addTransaction(amount: Double, category: String, type: String, note: String, dateLong: Long) {
        viewModelScope.launch {
            val transaction = Transaction(
                amount = amount,
                category = category,
                type = type,
                dateLong = dateLong,
                note = note,
                userId = "local_guest"
            )
            repository.insert(transaction)
            autoBackupData()
        }
    }

    fun updateTransaction(id: Long, amount: Double, category: String, type: String, note: String, dateLong: Long) {
        viewModelScope.launch {
            val transaction = Transaction(
                id = id,
                amount = amount,
                category = category,
                type = type,
                dateLong = dateLong,
                note = note,
                userId = "local_guest"
            )
            repository.insert(transaction)
            autoBackupData()
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.delete(transaction)
            autoBackupData()
        }
    }

    fun addBajarItem(name: String, quantity: String) {
        viewModelScope.launch {
            val item = com.example.data.BajarItem(
                name = name,
                quantity = quantity,
                isCompleted = false,
                userId = "local_guest"
            )
            repository.insertBajarItem(item)
            autoBackupData()
        }
    }

    fun toggleBajarItemCompletion(item: com.example.data.BajarItem, isCompleted: Boolean) {
        viewModelScope.launch {
            val updated = item.copy(isCompleted = isCompleted)
            repository.updateBajarItem(updated)
            autoBackupData()
        }
    }

    fun finishBajarShopping(totalCost: Double, completedItemsList: String) {
        viewModelScope.launch {
            val noteText = if (completedItemsList.isNotBlank()) {
                "Bajar: $completedItemsList"
            } else {
                "Bajar (Shopping) Expense"
            }

            val transaction = Transaction(
                amount = totalCost,
                category = com.example.R.string.category_groceries.toString(),
                type = "EXPENSE",
                dateLong = System.currentTimeMillis(),
                note = noteText,
                userId = "local_guest"
            )
            repository.insert(transaction)
            repository.deleteCompletedBajarItems("local_guest")
            autoBackupData()
        }
    }

    fun deleteBajarItem(item: com.example.data.BajarItem) {
        viewModelScope.launch {
            repository.deleteBajarItem(item)
            autoBackupData()
        }
    }

    fun addDebtRecord(personName: String, amount: Double, direction: String, note: String) {
        viewModelScope.launch {
            val record = com.example.data.DebtRecord(
                personName = personName,
                amount = amount,
                timestamp = System.currentTimeMillis(),
                direction = direction,
                isSettled = false,
                note = note,
                userId = "local_guest"
            )
            repository.insertDebt(record)
            autoBackupData()
        }
    }

    fun deleteDebtRecord(record: com.example.data.DebtRecord) {
        viewModelScope.launch {
            repository.deleteDebt(record)
            autoBackupData()
        }
    }

    fun settleDebtRecord(record: com.example.data.DebtRecord) {
        viewModelScope.launch {
            val updated = record.copy(isSettled = true)
            repository.updateDebt(updated)

            val transType = if (record.direction == "PAYABLE") "EXPENSE" else "INCOME"
            val categoryStr = com.example.R.string.category_other.toString()
            val noteText = if (record.direction == "PAYABLE") {
                "Paid debt to ${record.personName} (${record.note})"
            } else {
                "Collected debt from ${record.personName} (${record.note})"
            }

            val transaction = Transaction(
                amount = record.amount,
                category = categoryStr,
                type = transType,
                dateLong = System.currentTimeMillis(),
                note = noteText,
                userId = "local_guest"
            )
            repository.insert(transaction)
            autoBackupData()
        }
    }

    // Google sync compatibility stubs
    fun backupToDrive() {}
    fun restoreFromDrive() {}
    fun toggleAutoSync() {}
    fun getDriveLastSyncTime(): String = "Offline Active"
    fun getDriveOnlineCount(): Int = 0
    fun clearDriveSyncState() {}
    fun seedGuestDataIfNeeded() {}

    // Fully Local Serialization Helpers
    fun serializeFullBackup(
        transactions: List<Transaction>,
        bajarItems: List<com.example.data.BajarItem>,
        debtRecords: List<com.example.data.DebtRecord>
    ): String {
        val rootObj = JSONObject()

        // 1. Transactions
        val transArray = JSONArray()
        for (t in transactions) {
            val obj = JSONObject().apply {
                put("amount", t.amount)
                put("category", t.category)
                put("type", t.type)
                put("dateLong", t.dateLong)
                put("note", t.note)
                put("userId", t.userId)
            }
            transArray.put(obj)
        }
        rootObj.put("transactions", transArray)

        // 2. Bajar items
        val bajarArray = JSONArray()
        for (b in bajarItems) {
            val obj = JSONObject().apply {
                put("name", b.name)
                put("quantity", b.quantity)
                put("isCompleted", b.isCompleted)
                put("userId", b.userId)
            }
            bajarArray.put(obj)
        }
        rootObj.put("bajar_items", bajarArray)

        // 3. Debts
        val debtArray = JSONArray()
        for (d in debtRecords) {
            val obj = JSONObject().apply {
                put("personName", d.personName)
                put("amount", d.amount)
                put("timestamp", d.timestamp)
                put("direction", d.direction)
                put("isSettled", d.isSettled)
                put("note", d.note)
                put("userId", d.userId)
            }
            debtArray.put(obj)
        }
        rootObj.put("debt_records", debtArray)

        return rootObj.toString(2)
    }

    // Automatic Backup Trigger
    fun autoBackupData() {
        viewModelScope.launch {
            try {
                val transList = repository.getAllTransactions("local_guest").first()
                val bajarList = repository.getAllBajarItems("local_guest").first()
                val debtsList = repository.getAllDebts("local_guest").first()

                val jsonContent = serializeFullBackup(transList, bajarList, debtsList)
                saveBackupToDownloadsFolder(jsonContent)
            } catch (e: Exception) {
                android.util.Log.e("FinanceViewModel", "Error fetching DB for autoBackup: ${e.message}")
            }
        }
    }

    // Writes backup JSON to cache AND public Downloads folder via MediaStore
    private fun saveBackupToDownloadsFolder(jsonString: String) {
        val context = getApplication<Application>()

        // 1. Save to internal FilesDir as redundant fallback cache
        try {
            val file = File(context.filesDir, "personal_finance_backup.json")
            file.writeText(jsonString, Charsets.UTF_8)
        } catch (e: Exception) {
            android.util.Log.e("FinanceViewModel", "Cache backup failed: ${e.message}")
        }

        // 2. Save/Overwrite file in public Downloads folder using MediaStore API beautifully (no permissions needed on modern Q+)
        try {
            val resolver = context.contentResolver
            val fileName = "personal_finance_backup.json"
            val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Files.getContentUri("external")
            }

            // Explicitly query MediaStore to find any existing file with this name
            val projection = arrayOf(MediaStore.MediaColumns._ID)
            val querySelection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
            } else {
                "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
            }
            val selectionArgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                arrayOf(fileName, "%Download%")
            } else {
                arrayOf(fileName)
            }

            try {
                resolver.query(collectionUri, projection, querySelection, selectionArgs, null)?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                        val id = cursor.getLong(idCol)
                        val existingUri = ContentUris.withAppendedId(collectionUri, id)
                        resolver.delete(existingUri, null, null)
                    }
                }
            } catch (ee: Exception) {
                android.util.Log.e("FinanceViewModel", "Error deleting old instances prior to backup: ${ee.message}")
            }

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val fileUri = resolver.insert(collectionUri, values)
            if (fileUri != null) {
                resolver.openOutputStream(fileUri)?.use { output ->
                    output.write(jsonString.toByteArray(Charsets.UTF_8))
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(fileUri, values, null, null)
                }
                android.util.Log.i("FinanceViewModel", "Backup written to public Downloads folder successfully.")
            }
        } catch (e: Exception) {
            android.util.Log.e("FinanceViewModel", "MediaStore public back up failed, using direct File API: ${e.message}")
            try {
                val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsFolder.exists()) downloadsFolder.mkdirs()
                val file = File(downloadsFolder, "personal_finance_backup.json")
                file.writeText(jsonString, Charsets.UTF_8)
            } catch (ioEx: Exception) {
                android.util.Log.e("FinanceViewModel", "Direct File fallback back up also failed: ${ioEx.message}")
            }
        }
    }

    // Direct manual file triggers used by old helper dialogs (keeps everything backwards compatible and working!)
    fun exportBackupToFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val transList = repository.getAllTransactions("local_guest").first()
                val bajarList = repository.getAllBajarItems("local_guest").first()
                val debtsList = repository.getAllDebts("local_guest").first()
                val json = serializeFullBackup(transList, bajarList, debtsList)

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(json.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Export success!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.util.Log.e("FinanceViewModel", "Manual exportBackupToFile fail: ${e.message}")
            }
        }
    }

    fun importBackupFromFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val contentBuilder = java.lang.StringBuilder()
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    java.io.BufferedReader(java.io.InputStreamReader(inputStream, java.nio.charset.StandardCharsets.UTF_8)).use { reader ->
                        var line = reader.readLine()
                        while (line != null) {
                            contentBuilder.append(line)
                            line = reader.readLine()
                        }
                    }
                }
                val jsonStr = contentBuilder.toString()
                importBackupContent(
                    jsonStr = jsonStr,
                    onSuccess = {
                        Toast.makeText(context, "Backup restored!", Toast.LENGTH_SHORT).show()
                    },
                    onError = { err ->
                        Toast.makeText(context, "Import failed: $err", Toast.LENGTH_LONG).show()
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("FinanceViewModel", "Manual importBackupFromFile fail: ${e.message}")
            }
        }
    }

    // Fully Local RESTORE/IMPORT trigger (manual)
    fun importBackupContent(
        jsonStr: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val rootObj = JSONObject(jsonStr)

                val transactionsList = mutableListOf<Transaction>()
                if (rootObj.has("transactions")) {
                    val arr = rootObj.getJSONArray("transactions")
                    for (i in 0 until arr.length()) {
                        val json = arr.getJSONObject(i)
                        transactionsList.add(
                            Transaction(
                                id = 0,
                                amount = json.getDouble("amount"),
                                category = json.getString("category"),
                                type = json.getString("type"),
                                dateLong = json.getLong("dateLong"),
                                note = json.optString("note", ""),
                                userId = "local_guest"
                            )
                        )
                    }
                }

                val bajarList = mutableListOf<com.example.data.BajarItem>()
                if (rootObj.has("bajar_items")) {
                    val arr = rootObj.getJSONArray("bajar_items")
                    for (i in 0 until arr.length()) {
                        val json = arr.getJSONObject(i)
                        bajarList.add(
                            com.example.data.BajarItem(
                                id = 0,
                                name = json.getString("name"),
                                quantity = json.getString("quantity"),
                                isCompleted = json.optBoolean("isCompleted", false),
                                userId = "local_guest"
                            )
                        )
                    }
                }

                val debtList = mutableListOf<com.example.data.DebtRecord>()
                if (rootObj.has("debt_records")) {
                    val arr = rootObj.getJSONArray("debt_records")
                    for (i in 0 until arr.length()) {
                        val json = arr.getJSONObject(i)
                        debtList.add(
                            com.example.data.DebtRecord(
                                id = 0,
                                personName = json.getString("personName"),
                                amount = json.getDouble("amount"),
                                timestamp = json.getLong("timestamp"),
                                direction = json.getString("direction"),
                                isSettled = json.optBoolean("isSettled", false),
                                note = json.optString("note", ""),
                                userId = "local_guest"
                            )
                        )
                    }
                }

                // 1. Clear existing database for the local guest user to prevent double count/merging clashes
                repository.clearAllForUser("local_guest")
                repository.clearBajarItemsForUser("local_guest")
                repository.clearDebtsForUser("local_guest")

                // 2. Insert all deserialized items safely
                transactionsList.forEach { repository.insert(it) }
                bajarList.forEach { repository.insertBajarItem(it) }
                debtList.forEach { repository.insertDebt(it) }

                onSuccess()
                
                // Re-trigger a fresh backup of the imported contents to sync state
                autoBackupData()
            } catch (e: Exception) {
                android.util.Log.e("FinanceViewModel", "Backup restoration fail: ${e.message}")
                onError(e.message ?: "Invalid file structure")
            }
        }
    }
}

data class Stats(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val balance: Double = 0.0
)
