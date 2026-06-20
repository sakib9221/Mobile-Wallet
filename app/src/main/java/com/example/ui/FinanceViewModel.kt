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

    // Daily Expense Limit (default to 150)
    private val _dailyExpenseLimit = MutableStateFlow(prefs.getFloat("daily_expense_limit", 150f))
    val dailyExpenseLimit: StateFlow<Float> = _dailyExpenseLimit.asStateFlow()

    fun setDailyExpenseLimit(limit: Float) {
        _dailyExpenseLimit.value = limit
        prefs.edit().putFloat("daily_expense_limit", limit).apply()
    }

    private val _autoSyncOnChanges = MutableStateFlow(true)
    val autoSyncOnChanges: StateFlow<Boolean> = _autoSyncOnChanges.asStateFlow()

    // Personal user identification and secure PIN management
    private val _userSavedName = MutableStateFlow<String?>(prefs.getString("user_saved_name", null))
    val userSavedName: StateFlow<String?> = _userSavedName.asStateFlow()

    private val _userSavedPin = MutableStateFlow<String?>(prefs.getString("user_saved_pin", null))
    val userSavedPin: StateFlow<String?> = _userSavedPin.asStateFlow()

    private val _userDob = MutableStateFlow<String?>(prefs.getString("user_dob", ""))
    val userDob: StateFlow<String?> = _userDob.asStateFlow()

    private val _userGender = MutableStateFlow<String?>(prefs.getString("user_gender", ""))
    val userGender: StateFlow<String?> = _userGender.asStateFlow()

    private val _userAvatarIdx = MutableStateFlow<Int>(prefs.getInt("user_avatar_idx", 0))
    val userAvatarIdx: StateFlow<Int> = _userAvatarIdx.asStateFlow()

    private val _userAvatarCustomPath = MutableStateFlow<String?>(prefs.getString("user_avatar_custom_path", null))
    val userAvatarCustomPath: StateFlow<String?> = _userAvatarCustomPath.asStateFlow()

    private val _isAppLocked = MutableStateFlow<Boolean>(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    fun lockApp() {
        if (!prefs.getString("user_saved_pin", null).isNullOrBlank()) {
            _isAppLocked.value = true
        }
    }

    fun unlockApp(pin: String): Boolean {
        val savedPin = prefs.getString("user_saved_pin", null)
        return if (savedPin == pin) {
            _isAppLocked.value = false
            true
        } else {
            false
        }
    }

    fun unlockWithBiometric() {
        _isAppLocked.value = false
    }

    fun updateProfile(name: String, pin: String, dob: String, gender: String, avatarIdx: Int, customPath: String?) {
        _userSavedName.value = name
        _userSavedPin.value = pin
        _userDob.value = dob
        _userGender.value = gender
        _userAvatarIdx.value = avatarIdx
        _userAvatarCustomPath.value = customPath
        prefs.edit()
            .putString("user_saved_name", name)
            .putString("user_saved_pin", pin)
            .putString("user_dob", dob)
            .putString("user_gender", gender)
            .putInt("user_avatar_idx", avatarIdx)
            .putString("user_avatar_custom_path", customPath)
            .apply()
        // Automatically save local backup containing new credentials
        autoBackupData()
    }

    private val _pendingAutoRestoreBackup = MutableStateFlow<PendingRestoreInfo?>(null)
    val pendingAutoRestoreBackup: StateFlow<PendingRestoreInfo?> = _pendingAutoRestoreBackup.asStateFlow()

    fun saveUserNameAndPin(name: String, pin: String) {
        _userSavedName.value = name
        _userSavedPin.value = pin
        prefs.edit()
            .putString("user_saved_name", name)
            .putString("user_saved_pin", pin)
            .apply()
        // Create an automatic backup containing credentials
        autoBackupData()
    }

    fun confirmRestore(
        userNameInput: String,
        userPinInput: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val pending = _pendingAutoRestoreBackup.value ?: return
        
        // If backup has credentials, verify them strictly (case insensitive name, match PIN)
        if (pending.backupName.isNotEmpty()) {
            val nameMatch = pending.backupName.trim().equals(userNameInput.trim(), ignoreCase = true)
            val pinMatch = pending.backupPin.trim() == userPinInput.trim()
            if (!nameMatch || !pinMatch) {
                onError("Verification failed: Username or PIN is incorrect!")
                return
            }
        }

        // Proceed with database import
        importBackupContent(
            jsonStr = pending.jsonContent,
            onSuccess = {
                // Restore name/PIN credentials to preferences
                val restoredName = if (pending.backupName.isNotEmpty()) pending.backupName else userNameInput
                val restoredPin = if (pending.backupPin.isNotEmpty()) pending.backupPin else userPinInput
                saveUserNameAndPin(restoredName, restoredPin)
                
                _pendingAutoRestoreBackup.value = null
                prefs.edit().putBoolean("auto_restore_checked_v12", true).apply()
                onSuccess()
            },
            onError = { err ->
                onError(err)
            }
        )
    }

    fun cancelRestore() {
        _pendingAutoRestoreBackup.value = null
        prefs.edit().putBoolean("auto_restore_checked_v12", true).apply()
    }

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
                
                // Try to find if there is an existing backup in Download to restore from
                checkForAutoRestoreOnStartup(application)
            }
        } else {
            // Check for auto-restore
            checkForAutoRestoreOnStartup(application)
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

    // Calculate stats dynamically using high performance single-pass O(N) allocation-free loop
    val stats: StateFlow<Stats> = transactions
        .map { transactionList ->
            var income = 0.0
            var expense = 0.0
            for (t in transactionList) {
                if (t.type == "INCOME") {
                    income += t.amount
                } else if (t.type == "EXPENSE") {
                    expense += t.amount
                }
            }
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
        val list = listOf("en", "bn", "hi", "ar", "es")
        val current = _selectedLanguage.value
        val currentIndex = list.indexOf(current)
        val nextLang = if (currentIndex == -1 || currentIndex == list.lastIndex) list[0] else list[currentIndex + 1]
        _selectedLanguage.value = nextLang
        prefs.edit().putString("selected_lang", nextLang).apply()
    }

    fun setLanguage(lang: String) {
        val list = listOf("en", "bn", "hi", "ar", "es")
        if (lang in list) {
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

    fun updateDebtRecord(record: com.example.data.DebtRecord) {
        viewModelScope.launch {
            repository.updateDebt(record)
            autoBackupData()
        }
    }

    fun settleDebtRecord(record: com.example.data.DebtRecord, settleAmount: Double? = null) {
        viewModelScope.launch {
            val amtToSettle = settleAmount ?: record.amount
            if (amtToSettle >= record.amount) {
                val updated = record.copy(isSettled = true)
                repository.updateDebt(updated)
            } else {
                val updated = record.copy(amount = record.amount - amtToSettle)
                repository.updateDebt(updated)
            }

            val transType = if (record.direction == "PAYABLE") "EXPENSE" else "INCOME"
            val categoryStr = com.example.R.string.category_other.toString()
            val noteText = if (record.direction == "PAYABLE") {
                if (amtToSettle >= record.amount) "Paid debt to ${record.personName} (${record.note})" else "Paid partial debt to ${record.personName} (Paid: ৳$amtToSettle, Remaining: ৳${record.amount - amtToSettle})"
            } else {
                if (amtToSettle >= record.amount) "Collected debt from ${record.personName} (${record.note})" else "Collected partial debt from ${record.personName} (Collected: ৳$amtToSettle, Remaining: ৳${record.amount - amtToSettle})"
            }

            val transaction = Transaction(
                amount = amtToSettle,
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
        val rootObj = JSONObject().apply {
            put("saved_name", prefs.getString("user_saved_name", "") ?: "")
            put("saved_pin", prefs.getString("user_saved_pin", "") ?: "")
            put("user_dob", prefs.getString("user_dob", "") ?: "")
            put("user_gender", prefs.getString("user_gender", "") ?: "")
            put("user_avatar_idx", prefs.getInt("user_avatar_idx", 0))
            put("user_avatar_custom_path", prefs.getString("user_avatar_custom_path", "") ?: "")
        }

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
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
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

        // 2. Save/Overwrite file in public Downloads folder
        val fileName = "personal_finance_backup.json"
        var writeSuccessful = false

        // Try direct File API write first to prevent MediaStore name-conflict duplicate suffixes like (1).json
        try {
            val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsFolder.exists()) {
                downloadsFolder.mkdirs()
            }
            val directFile = File(downloadsFolder, fileName)
            directFile.writeText(jsonString, Charsets.UTF_8)
            writeSuccessful = true
            android.util.Log.i("FinanceViewModel", "Wrote/Overwrote backup file via Direct File API successfully.")
        } catch (e: Exception) {
            android.util.Log.e("FinanceViewModel", "Direct File API write failed: ${e.message}, falling back to MediaStore.")
        }

        // Also clean up any other duplicate/suffixed files like personal_finance_backup (1).json in Downloads folder to prevent clutter
        try {
            val resolver = context.contentResolver
            val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Files.getContentUri("external")
            }

            val cleanProjection = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME)
            val cleanSelection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
            } else {
                "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
            }
            val cleanSelectionArgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                arrayOf("personal_finance_backup%.json", "%Download%")
            } else {
                arrayOf("personal_finance_backup%.json")
            }

            resolver.query(collectionUri, cleanProjection, cleanSelection, cleanSelectionArgs, null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: ""
                    
                    if (name != fileName) {
                        val dupUri = ContentUris.withAppendedId(collectionUri, id)
                        try {
                            resolver.delete(dupUri, null, null)
                        } catch (e: Exception) {
                            // Ignore delete failures for non-owned files
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FinanceViewModel", "Failed cleaning duplicate backups: ${e.message}")
        }

        // Fallback to MediaStore if direct file write was not possible
        if (!writeSuccessful) {
            try {
                val resolver = context.contentResolver
                val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Files.getContentUri("external")
                }

                // Search for the main "personal_finance_backup.json" file.
                var existingUri: Uri? = null
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
                        if (cursor.moveToFirst()) {
                            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                            val id = cursor.getLong(idCol)
                            existingUri = ContentUris.withAppendedId(collectionUri, id)
                        }
                    }
                } catch (ee: Exception) {
                    android.util.Log.e("FinanceViewModel", "Error querying main backup: ${ee.message}")
                }

                if (existingUri != null) {
                    try {
                        // Try to open and overwrite in place. Using "rwt" opens/truncates existing file.
                        resolver.openOutputStream(existingUri!!, "rwt")?.use { output ->
                            output.write(jsonString.toByteArray(Charsets.UTF_8))
                            writeSuccessful = true
                        }
                        android.util.Log.i("FinanceViewModel", "Overwrote existing personal_finance_backup.json in place successfully via MediaStore.")
                    } catch (e: Exception) {
                        android.util.Log.e("FinanceViewModel", "Failed to overwrite main backup in place, attempting delete: ${e.message}")
                        try {
                            resolver.delete(existingUri!!, null, null)
                        } catch (delEx: Exception) {
                            android.util.Log.e("FinanceViewModel", "Failed to delete main backup: ${delEx.message}")
                        }
                    }
                }

                // If we couldn't overwrite, we insert a new record
                if (!writeSuccessful) {
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
                        android.util.Log.i("FinanceViewModel", "New personal_finance_backup.json written to Downloads successfully via MediaStore.")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("FinanceViewModel", "MediaStore public back up failed: ${e.message}")
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

                val restoredDob = rootObj.optString("user_dob", "")
                val restoredGender = rootObj.optString("user_gender", "")
                val restoredAvatarIdx = rootObj.optInt("user_avatar_idx", 0)
                val restoredCustomPath = rootObj.optString("user_avatar_custom_path", "")
                _userDob.value = restoredDob
                _userGender.value = restoredGender
                _userAvatarIdx.value = restoredAvatarIdx
                _userAvatarCustomPath.value = if (restoredCustomPath.isEmpty()) null else restoredCustomPath
                prefs.edit()
                    .putString("user_dob", restoredDob)
                    .putString("user_gender", restoredGender)
                    .putInt("user_avatar_idx", restoredAvatarIdx)
                    .putString("user_avatar_custom_path", if (restoredCustomPath.isEmpty()) null else restoredCustomPath)
                    .apply()

                onSuccess()
                
                // Re-trigger a fresh backup of the imported contents to sync state
                autoBackupData()
            } catch (e: Exception) {
                android.util.Log.e("FinanceViewModel", "Backup restoration fail: ${e.message}")
                onError(e.message ?: "Invalid file structure")
            }
        }
    }

    fun checkForAutoRestoreOnStartup(context: Context) {
        val alreadyChecked = prefs.getBoolean("auto_restore_checked_v12", false)
        if (alreadyChecked) return

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Confirm if database is empty to avoid overwriting active data
                val existingTxs = repository.getAllTransactions("local_guest").first()
                val existingBajar = repository.getAllBajarItems("local_guest").first()
                val existingDebts = repository.getAllDebts("local_guest").first()

                if (existingTxs.isNotEmpty() || existingBajar.isNotEmpty() || existingDebts.isNotEmpty()) {
                    prefs.edit().putBoolean("auto_restore_checked_v12", true).apply()
                    return@launch
                }

                android.util.Log.i("FinanceViewModel", "Start searching for existing backup to auto-restore...")
                
                // Track list of pairs: (displayName, fileContent)
                val candidateContents = mutableListOf<Pair<String, String>>()

                // 1. Check MediaStore Downloads
                val resolver = context.contentResolver
                val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Files.getContentUri("external")
                }

                val projection = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME)
                val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
                val selectionArgs = arrayOf("personal_finance_backup%.json")

                try {
                    resolver.query(collectionUri, projection, selection, selectionArgs, null)?.use { cursor ->
                        while (cursor.moveToNext()) {
                            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                            val id = cursor.getLong(idCol)
                            val name = cursor.getString(nameCol) ?: ""
                            val fileUri = ContentUris.withAppendedId(collectionUri, id)
                            
                            // Try to read the file
                            try {
                                val contentBuilder = java.lang.StringBuilder()
                                resolver.openInputStream(fileUri)?.use { inputStream ->
                                    java.io.BufferedReader(java.io.InputStreamReader(inputStream, java.nio.charset.StandardCharsets.UTF_8)).use { reader ->
                                        var line = reader.readLine()
                                        while (line != null) {
                                            contentBuilder.append(line)
                                            line = reader.readLine()
                                        }
                                    }
                                }
                                val fileStr = contentBuilder.toString()
                                if (fileStr.trim().isNotEmpty()) {
                                    candidateContents.add(name to fileStr)
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("FinanceViewModel", "Could not read media file $name: ${e.message}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("FinanceViewModel", "Error querying MediaStore for startup restore: ${e.message}")
                }

                // 2. Also check direct File path (just in case MediaStore misses some)
                try {
                    val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (downloadsFolder.exists()) {
                        downloadsFolder.listFiles()?.forEach { file ->
                            if (file.name.startsWith("personal_finance_backup") && file.name.endsWith(".json")) {
                                try {
                                    val fileStr = file.readText(Charsets.UTF_8)
                                    if (fileStr.trim().isNotEmpty()) {
                                        candidateContents.add(file.name to fileStr)
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("FinanceViewModel", "Could not read direct file ${file.name}: ${e.message}")
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("FinanceViewModel", "Error scanning direct files for startup restore: ${e.message}")
                }

                // 3. Evaluate the candidate contents and find the one with the MOST actual data items
                var bestFilename: String? = null
                var bestJson: String? = null
                var maxRecordCount = -1

                for ((name, jsonStr) in candidateContents) {
                    try {
                        val root = JSONObject(jsonStr)
                        val txCount = root.optJSONArray("transactions")?.length() ?: 0
                        val bajarCount = root.optJSONArray("bajar_items")?.length() ?: 0
                        val debtCount = root.optJSONArray("debt_records")?.length() ?: 0
                        val total = txCount + bajarCount + debtCount
                        if (total > maxRecordCount) {
                            maxRecordCount = total
                            bestFilename = name
                            bestJson = jsonStr
                        }
                    } catch (e: Exception) {
                        // Not a valid JSON or different structure, skip
                    }
                }

                if (bestJson != null && maxRecordCount >= 0) {
                    android.util.Log.i("FinanceViewModel", "Best backup file selected for restore: $bestFilename with $maxRecordCount records.")
                    val bRoot = JSONObject(bestJson)
                    val bName = bRoot.optString("saved_name", "")
                    val bPin = bRoot.optString("saved_pin", "")
                    _pendingAutoRestoreBackup.value = PendingRestoreInfo(
                        fileName = bestFilename ?: "personal_finance_backup.json",
                        jsonContent = bestJson,
                        backupName = bName,
                        backupPin = bPin
                    )
                } else {
                    android.util.Log.i("FinanceViewModel", "No local backup found with data for auto-restoring.")
                    prefs.edit().putBoolean("auto_restore_checked_v12", true).apply()
                }
            } catch (e: Exception) {
                android.util.Log.e("FinanceViewModel", "Critical error in auto-restore: ${e.message}")
                prefs.edit().putBoolean("auto_restore_checked_v12", true).apply()
            }
        }
    }
}

data class Stats(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val balance: Double = 0.0
)

data class PendingRestoreInfo(
    val fileName: String,
    val jsonContent: String,
    val backupName: String,
    val backupPin: String
)
