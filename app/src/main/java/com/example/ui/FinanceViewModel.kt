package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AIEngine
import com.example.data.AppDatabase
import com.example.data.FinanceRepository
import com.example.data.Note
import com.example.data.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FinanceRepository

    val transactions: StateFlow<List<Transaction>>
    val notes: StateFlow<List<Note>>

    val totalIncome: StateFlow<Double>
    val totalExpense: StateFlow<Double>
    val netResult: StateFlow<Double>

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _aiFeedbackMessage = MutableStateFlow<String?>(null)
    val aiFeedbackMessage: StateFlow<String?> = _aiFeedbackMessage.asStateFlow()

    private val _activeTab = MutableStateFlow(0) // 0: Oversikt, 1: Transaksjoner, 2: Notater
    val activeTab: StateFlow<Int> = _activeTab.asStateFlow()

    private val sharedPrefs = application.getSharedPreferences("ai_okonomibok_prefs", android.content.Context.MODE_PRIVATE)
    private val _isDarkMode = MutableStateFlow(
        sharedPrefs.getBoolean(
            "dark_mode",
            (application.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        )
    )
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode() {
        val newValue = !_isDarkMode.value
        _isDarkMode.value = newValue
        sharedPrefs.edit().putBoolean("dark_mode", newValue).apply()
    }

    init {
        val database = AppDatabase.getDatabase(application)
        repository = FinanceRepository(database.transactionDao(), database.noteDao())

        transactions = repository.allTransactions
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        notes = repository.allNotes
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        totalIncome = transactions.map { list ->
            list.filter { it.type == "INNTEKT" }.sumOf { it.amount }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

        totalExpense = transactions.map { list ->
            list.filter { it.type == "UTGIFT" }.sumOf { it.amount }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

        netResult = transactions.map { list ->
            calculateTotalBalance(list)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    }

    fun calculateTotalBalance(list: List<Transaction>): Double {
        val income = list.filter { it.type == "INNTEKT" }.sumOf { it.amount }
        val expense = list.filter { it.type == "UTGIFT" }.sumOf { it.amount }
        return income - expense
    }

    fun setActiveTab(tabIndex: Int) {
        _activeTab.value = tabIndex
    }

    fun clearFeedback() {
        _aiFeedbackMessage.value = null
    }

    fun addTransaction(type: String, amount: Double, category: String, description: String) {
        viewModelScope.launch {
            repository.insertTransaction(
                Transaction(
                    type = type,
                    amount = amount,
                    category = category.trim().ifEmpty { "Diverse" },
                    description = description.trim()
                )
            )
        }
    }

    fun deleteTransaction(id: Int) {
        viewModelScope.launch {
            repository.deleteTransactionById(id)
        }
    }

    fun addNote(title: String, content: String) {
        viewModelScope.launch {
            repository.insertNote(
                Note(
                    title = title.trim().ifEmpty { "Uten tittel" },
                    content = content.trim()
                )
            )
        }
    }

    fun deleteNote(id: Int) {
        viewModelScope.launch {
            repository.deleteNoteById(id)
        }
    }

    fun processAiInput(input: String) {
        if (input.trim().isEmpty()) return
        
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiFeedbackMessage.value = null
            
            try {
                val parsed = AIEngine.parseInput(input)
                if (parsed != null) {
                    when (parsed.result_type) {
                        "transaction" -> {
                            val tx = parsed.transaction
                            if (tx != null) {
                                addTransaction(
                                    type = tx.type,
                                    amount = tx.amount,
                                    category = tx.category,
                                    description = tx.description
                                )
                                _aiFeedbackMessage.value = parsed.feedback_message
                            } else {
                                _aiFeedbackMessage.value = "Klarte ikke å registrere transaksjonen."
                            }
                        }
                        "note" -> {
                            val nt = parsed.note
                            if (nt != null) {
                                addNote(
                                    title = nt.title,
                                    content = nt.content
                                )
                                _aiFeedbackMessage.value = parsed.feedback_message
                            } else {
                                _aiFeedbackMessage.value = "Klarte ikke å opprette notatet."
                            }
                        }
                        else -> {
                            _aiFeedbackMessage.value = parsed.feedback_message
                        }
                    }
                } else {
                    _aiFeedbackMessage.value = "Kunne ikke tolke inndata. Vennligst prøv igjen med mer detaljer."
                }
            } catch (e: Exception) {
                _aiFeedbackMessage.value = "Feil ved AI-tolking: ${e.localizedMessage}"
            } finally {
                _isAiLoading.value = false
            }
        }
    }
}
