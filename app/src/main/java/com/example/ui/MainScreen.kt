package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.utils.SpeechRecognizerHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Note
import com.example.data.Transaction
import com.example.ui.components.FinanceTrendChart
import com.example.ui.theme.ExpenseColor
import com.example.ui.theme.IncomeColor
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()

    val totalIncome by viewModel.totalIncome.collectAsStateWithLifecycle()
    val totalExpense by viewModel.totalExpense.collectAsStateWithLifecycle()
    val netResult by viewModel.netResult.collectAsStateWithLifecycle()

    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    val aiFeedbackMessage by viewModel.aiFeedbackMessage.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

    var showAddTxDialog by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var selectedNoteForView by remember { mutableStateOf<Note?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "AI Økonomibok",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                },
                actions = {
                    val balanceColor = if (netResult >= 0) IncomeColor else ExpenseColor
                    val balanceBg = if (netResult >= 0) IncomeColor.copy(alpha = 0.12f) else ExpenseColor.copy(alpha = 0.12f)
                    
                    IconButton(
                        onClick = { viewModel.toggleDarkMode() },
                        modifier = Modifier.testTag("dark_mode_toggle")
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.WbSunny else Icons.Default.NightsStay,
                            contentDescription = if (isDarkMode) "Bytt til lyst modus" else "Bytt til mørkt modus",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Surface(
                        color = balanceBg,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .testTag("top_bar_total_balance")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Balanse:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatCurrency(netResult),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = balanceColor
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { viewModel.setActiveTab(0) },
                    icon = { Icon(Icons.Default.QueryStats, contentDescription = "Oversikt") },
                    label = { Text("Oversikt") },
                    modifier = Modifier.testTag("nav_tab_overview")
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { viewModel.setActiveTab(1) },
                    icon = { Icon(Icons.Default.Payments, contentDescription = "Transaksjoner") },
                    label = { Text("Penger") },
                    modifier = Modifier.testTag("nav_tab_transactions")
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { viewModel.setActiveTab(2) },
                    icon = { Icon(Icons.Default.Notes, contentDescription = "Notater") },
                    label = { Text("Notater") },
                    modifier = Modifier.testTag("nav_tab_notes")
                )
            }
        },
        floatingActionButton = {
            if (activeTab != 0) {
                ExtendedFloatingActionButton(
                    onClick = {
                        if (activeTab == 1) showAddTxDialog = true else showAddNoteDialog = true
                    },
                    icon = {
                        Icon(
                            imageVector = if (activeTab == 1) Icons.Default.AddBusiness else Icons.Default.NoteAdd,
                            contentDescription = "Legg til"
                        )
                    },
                    text = {
                        Text(text = if (activeTab == 1) "Ny Transaksjon" else "Nytt Notat")
                    },
                    modifier = Modifier.testTag(if (activeTab == 1) "fab_add_transaction" else "fab_add_note")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                0 -> OverviewTab(
                    viewModel = viewModel,
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    netResult = netResult,
                    recentTransactions = transactions.take(5),
                    allTransactions = transactions,
                    recentNotes = notes.take(3),
                    isAiLoading = isAiLoading,
                    aiFeedback = aiFeedbackMessage,
                    onViewTransactionClick = { viewModel.setActiveTab(1) },
                    onViewNotesClick = { viewModel.setActiveTab(2) },
                    onNoteClick = { selectedNoteForView = it },
                    onDeleteTransaction = { viewModel.deleteTransaction(it) }
                )
                1 -> TransactionsTab(
                    transactions = transactions,
                    onDelete = { viewModel.deleteTransaction(it) },
                    onAddClick = { showAddTxDialog = true }
                )
                2 -> NotesTab(
                    notes = notes,
                    onDelete = { viewModel.deleteNote(it) },
                    onAddClick = { showAddNoteDialog = true },
                    onNoteClick = { selectedNoteForView = it }
                )
            }
        }
    }

    // --- Dialogs ---

    if (showAddTxDialog) {
        AddTransactionDialog(
            onDismiss = { showAddTxDialog = false },
            onConfirm = { type, amount, cat, desc ->
                viewModel.addTransaction(type, amount, cat, desc)
                showAddTxDialog = false
            }
        )
    }

    if (showAddNoteDialog) {
        AddNoteDialog(
            onDismiss = { showAddNoteDialog = false },
            onConfirm = { title, content ->
                viewModel.addNote(title, content)
                showAddNoteDialog = false
            }
        )
    }

    selectedNoteForView?.let { note ->
        ViewNoteDialog(
            note = note,
            onDismiss = { selectedNoteForView = null },
            onDelete = {
                viewModel.deleteNote(note.id)
                selectedNoteForView = null
            }
        )
    }
}

// --- TAB CONTENT COMPOSABLES ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewTab(
    viewModel: FinanceViewModel,
    totalIncome: Double,
    totalExpense: Double,
    netResult: Double,
    recentTransactions: List<Transaction>,
    allTransactions: List<Transaction>,
    recentNotes: List<Note>,
    isAiLoading: Boolean,
    aiFeedback: String?,
    onViewTransactionClick: () -> Unit,
    onViewNotesClick: () -> Unit,
    onNoteClick: (Note) -> Unit,
    onDeleteTransaction: (Int) -> Unit
) {
    var aiInput by remember { mutableStateOf("") }
    var showSpeechDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasMicPermission = isGranted
            if (isGranted) {
                showSpeechDialog = true
            }
        }
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("overview_tab_scrollable"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // AI Quick Entry Section (Prominent Header)
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "AI-Inntasting (Skriv eller Snakk)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Text(
                        text = "Skriv f.eks. 'solgte varer til kaffe for 150 kr' eller 'notat: husk varetelling på mandag'. AI tolker og lagrer alt automatisk!",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = aiInput,
                            onValueChange = { aiInput = it },
                            placeholder = { Text("Skriv inn her...", fontSize = 14.sp) },
                            singleLine = false,
                            maxLines = 3,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_input_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )

                        // Voice Dictation Button
                        IconButton(
                            onClick = {
                                if (hasMicPermission) {
                                    showSpeechDialog = true
                                } else {
                                    requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .testTag("speech_mic_trigger_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Snakk inn transaksjon",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        Button(
                            onClick = {
                                if (aiInput.isNotBlank()) {
                                    viewModel.processAiInput(aiInput)
                                    aiInput = ""
                                }
                            },
                            enabled = !isAiLoading && aiInput.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(56.dp)
                                .testTag("ai_submit_button"),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            if (isAiLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Analyser"
                                )
                            }
                        }
                    }

                    // Display AI feedback message if any
                    AnimatedVisibility(
                        visible = aiFeedback != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        aiFeedback?.let { feedback ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = IncomeColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = feedback,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { viewModel.clearFeedback() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Lukk",
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

        // Financial Dashboard Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                                    MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Netto Resultat",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val netColor = if (netResult >= 0) IncomeColor else ExpenseColor
                        val prefix = if (netResult >= 0) "+" else ""

                        Text(
                            text = "$prefix${formatCurrency(netResult)}",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = netColor,
                            modifier = Modifier.testTag("net_result_text")
                        )

                        val statusLabel = if (netResult >= 0) "Overskudd" else "Underskudd"
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = netColor.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = statusLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = netColor,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        tint = IncomeColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Inntekter",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = formatCurrency(totalIncome),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeColor,
                                    modifier = Modifier.testTag("total_income_text")
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(40.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingDown,
                                        contentDescription = null,
                                        tint = ExpenseColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Utgifter",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = formatCurrency(totalExpense),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseColor,
                                    modifier = Modifier.testTag("total_expense_text")
                                )
                            }
                        }
                    }
                }
            }
        }

        // Financial Trend Chart Section
        item {
            FinanceTrendChart(
                transactions = allTransactions,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        // Recent Transactions Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Siste Transaksjoner",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                TextButton(onClick = onViewTransactionClick) {
                    Text("Se alle")
                }
            }
        }

        // Recent Transactions List
        if (recentTransactions.isEmpty()) {
            item {
                EmptyListCard(
                    text = "Ingen registrerte transaksjoner. Bruk AI eller legg til manuelt under Penger.",
                    icon = Icons.Default.ReceiptLong
                )
            }
        } else {
            items(recentTransactions) { tx ->
                TransactionRow(tx = tx, onDelete = { onDeleteTransaction(tx.id) })
            }
        }

        // Recent Notes Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Siste Notater",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                TextButton(onClick = onViewNotesClick) {
                    Text("Se alle")
                }
            }
        }

        // Recent Notes Grid/Cards
        if (recentNotes.isEmpty()) {
            item {
                EmptyListCard(
                    text = "Ingen notater funnet. Skriv dem inn med AI eller trykk Nytt Notat.",
                    icon = Icons.Default.Description
                )
            }
        } else {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    recentNotes.forEach { note ->
                        NoteItemRow(note = note, onClick = { onNoteClick(note) })
                    }
                }
            }
        }
    }

    if (showSpeechDialog) {
        SpeechDictationDialog(
            onDismiss = { showSpeechDialog = false },
            onResult = { speechText ->
                if (aiInput.isEmpty()) {
                    aiInput = speechText
                } else {
                    aiInput = "$aiInput $speechText"
                }
            }
        )
    }
}

@Composable
fun TransactionsTab(
    transactions: List<Transaction>,
    onDelete: (Int) -> Unit,
    onAddClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Alle Transaksjoner",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Button(
                onClick = onAddClick,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Legg til")
            }
        }

        TransactionListComponent(
            transactions = transactions,
            onDelete = onDelete,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun NotesTab(
    notes: List<Note>,
    onDelete: (Int) -> Unit,
    onAddClick: () -> Unit,
    onNoteClick: (Note) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mine Notater",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Button(
                onClick = onAddClick,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nytt notat")
            }
        }

        if (notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "Ingen notater lagret.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("notes_grid_scrollable")
            ) {
                items(notes, key = { it.id }) { note ->
                    NoteGridCard(
                        note = note,
                        onClick = { onNoteClick(note) },
                        onDelete = { onDelete(note.id) }
                    )
                }
            }
        }
    }
}

// --- SUB-COMPONENTS & ROWS ---

@Composable
fun TransactionTypeBadge(isIncome: Boolean, modifier: Modifier = Modifier) {
    val text = if (isIncome) "Inntekt" else "Utgift"
    val containerColor = if (isIncome) {
        Color(0xFFE8F5E9) // soft green
    } else {
        Color(0xFFFFEBEE) // soft red
    }
    val contentColor = if (isIncome) {
        Color(0xFF2E7D32) // dark green
    } else {
        Color(0xFFC62828) // dark red
    }

    Box(
        modifier = modifier
            .background(color = containerColor, shape = RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .testTag(if (isIncome) "label_income" else "label_expense")
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}

@Composable
fun TransactionCategoryBadge(category: String, modifier: Modifier = Modifier) {
    if (category.isNotBlank()) {
        Box(
            modifier = modifier
                .background(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 8.dp, vertical = 2.dp)
                .testTag("label_category_${category.lowercase()}")
        ) {
            Text(
                text = category,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun TransactionListComponent(
    transactions: List<Transaction>,
    onDelete: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (transactions.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    text = "Ingen transaksjoner registrert ennå.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.testTag("transactions_list_component"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(transactions, key = { it.id }) { tx ->
                TransactionRow(
                    tx = tx,
                    onDelete = onDelete?.let { { it(tx.id) } }
                )
            }
        }
    }
}

@Composable
fun TransactionRow(
    tx: Transaction,
    onDelete: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val isIncome = tx.type == "INNTEKT"
            val color = if (isIncome) IncomeColor else ExpenseColor
            val icon = if (isIncome) Icons.Default.TrendingUp else Icons.Default.TrendingDown

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.description.ifEmpty { tx.category },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TransactionTypeBadge(isIncome = isIncome)
                    TransactionCategoryBadge(category = tx.category)
                    Text(
                        text = "•",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = formatDate(tx.timestamp),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = (if (isIncome) "+" else "-") + formatCurrency(tx.amount),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = color
            )

            if (onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_tx_${tx.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Slett transaksjon",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun NoteItemRow(
    note: Note,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        MaterialTheme.colorScheme.secondaryContainer,
                        RoundedCornerShape(10.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notes,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = note.content,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun NoteGridCard(
    note: Note,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = note.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("delete_note_${note.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Slett notat",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = note.content,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = formatDate(note.timestamp),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
fun EmptyListCard(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = text,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// --- ADD / EDIT DIALOG COMPOSABLES ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onConfirm: (type: String, amount: Double, category: String, description: String) -> Unit
) {
    var type by remember { mutableStateOf("INNTEKT") }
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    var isSavingAndCategorizing by remember { mutableStateOf(false) }

    // AI Category Suggestion states
    var suggestedCategory by remember { mutableStateOf<String?>(null) }
    var isSuggestingCategory by remember { mutableStateOf(false) }

    LaunchedEffect(description, type) {
        val trimmedDesc = description.trim()
        if (trimmedDesc.length >= 3) {
            isSuggestingCategory = true
            delay(800) // Debounce typing
            val suggestion = com.example.data.AIEngine.suggestCategory(trimmedDesc, type)
            suggestedCategory = suggestion
            isSuggestingCategory = false
            // Auto-populate if category is currently empty
            if (suggestion != null && category.isBlank()) {
                category = suggestion
            }
        } else {
            suggestedCategory = null
            isSuggestingCategory = false
        }
    }

    val categories = listOf("Salg", "Varekjøp", "Tjenester", "Kontor", "Markedsføring", "Reise", "Diverse")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Ny Transaksjon",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                // Segmented button for Inntekt vs Utgift
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("INNTEKT" to "Inntekt", "UTGIFT" to "Utgift").forEach { (value, label) ->
                        val isSelected = type == value
                        val color = animateColorAsState(
                            targetValue = if (isSelected) {
                                if (value == "INNTEKT") IncomeColor else ExpenseColor
                            } else {
                                Color.Transparent
                            }, label = "tab_color"
                        )
                        val textColor = animateColorAsState(
                            targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                            label = "text_color"
                        )

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(color.value)
                                .clickable { type = value }
                                .testTag("select_type_$value")
                        ) {
                            Text(
                                text = label,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = textColor.value
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Beløp (kr)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_amount")
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Kategori (f.eks. Salg, Varekjøp)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        if (isSuggestingCategory) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else if (suggestedCategory != null) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI foreslått kategori",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_category")
                )

                if (isSuggestingCategory) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 1.5.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "AI tenker på kategori...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                } else if (suggestedCategory != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "AI-forslag:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable { category = suggestedCategory!! }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("ai_category_suggestion_chip")
                        ) {
                            Text(
                                text = suggestedCategory!!,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Beskrivelse (valgfri)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_description")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Avbryt")
                    }

                    Button(
                        onClick = {
                            val amount = amountText.toDoubleOrNull() ?: 0.0
                            if (amount > 0) {
                                val trimmedDesc = description.trim()
                                if (trimmedDesc.isNotEmpty() && category.isBlank()) {
                                    isSavingAndCategorizing = true
                                    scope.launch {
                                        try {
                                            val suggestion = com.example.data.AIEngine.suggestCategory(trimmedDesc, type)
                                            val finalCategory = if (!suggestion.isNullOrBlank()) suggestion else "Diverse"
                                            onConfirm(type, amount, finalCategory, trimmedDesc)
                                        } catch (e: Exception) {
                                            onConfirm(type, amount, "Diverse", trimmedDesc)
                                        } finally {
                                            isSavingAndCategorizing = false
                                        }
                                    }
                                } else {
                                    val finalCategory = if (category.isNotBlank()) category.trim() else "Diverse"
                                    onConfirm(type, amount, finalCategory, trimmedDesc)
                                }
                            }
                        },
                        enabled = !isSavingAndCategorizing && amountText.toDoubleOrNull() != null && amountText.toDouble() > 0,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("confirm_add_transaction"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSavingAndCategorizing) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Text("Tenker...", fontSize = 14.sp)
                            }
                        } else {
                            Text("Lagre")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNoteDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, content: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Nytt Notat",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tittel") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_note_title")
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Skriv notat...") },
                    minLines = 4,
                    maxLines = 8,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_note_content")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Avbryt")
                    }

                    Button(
                        onClick = {
                            if (content.isNotBlank()) {
                                onConfirm(title, content)
                            }
                        },
                        enabled = content.isNotBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("confirm_add_note"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Lagre")
                    }
                }
            }
        }
    }
}

@Composable
fun ViewNoteDialog(
    note: Note,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = note.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Slett",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Text(
                    text = note.content,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Opprettet: ${formatDate(note.timestamp)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Lukk")
                }
            }
        }
    }
}

// --- HELPERS ---

fun formatCurrency(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("no", "NO"))
    // Custom formatting to keep it clean in krone values if local format adds heavy formatting
    return try {
        val formatted = formatter.format(amount)
        // Clean up formatting trailing spaces or kr
        formatted.replace("kr", "kr ").replace(",00", ",-")
    } catch (e: Exception) {
        "${amount.toInt()} kr"
    }
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd. MMM yyyy, HH:mm", Locale("no", "NO"))
    return sdf.format(Date(timestamp))
}

@Composable
fun SpeechDictationDialog(
    onDismiss: () -> Unit,
    onResult: (String) -> Unit
) {
    val context = LocalContext.current
    val helper = remember { SpeechRecognizerHelper(context) }
    val isListening by helper.isListening.collectAsStateWithLifecycle()
    val partialText by helper.partialText.collectAsStateWithLifecycle()
    val finalText by helper.finalText.collectAsStateWithLifecycle()
    val errorState by helper.errorState.collectAsStateWithLifecycle()

    // Start listening on launch
    LaunchedEffect(Unit) {
        helper.startListening()
    }

    // Clean up on leave
    DisposableEffect(Unit) {
        onDispose {
            helper.cancel()
        }
    }

    Dialog(onDismissRequest = {
        helper.stopListening()
        onDismiss()
    }) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("speech_dialog_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "AI Stemme-inntasting",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Microphone icon with pulsing effect
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(100.dp)
                ) {
                    if (isListening) {
                        // Pulsing background rings
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 0.8f,
                            targetValue = 1.3f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1200, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "pulse_scale"
                        )
                        val alpha by infiniteTransition.animateFloat(
                            initialValue = 0.6f,
                            targetValue = 0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1200, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "pulse_alpha"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    alpha = alpha
                                )
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                        )
                    }

                    // Centered mic button
                    val buttonColor = if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                    val iconColor = if (isListening) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer

                    IconButton(
                        onClick = {
                            if (isListening) {
                                helper.stopListening()
                            } else {
                                helper.startListening()
                            }
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .background(buttonColor, androidx.compose.foundation.shape.CircleShape)
                            .testTag("speech_mic_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = if (isListening) "Lytter - trykk for å stoppe" else "Mute - trykk for å lytte",
                            tint = iconColor,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Text(
                    text = when {
                        errorState != null -> "Feil: $errorState"
                        isListening -> "Lytter... Snakk nå"
                        else -> "Tale satt på pause"
                    },
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = if (errorState != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("speech_status_text")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Partial / transcribed text field box
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        val textToDisplay = partialText.ifEmpty { finalText }
                        if (textToDisplay.isEmpty() && errorState == null) {
                            Text(
                                text = "Eksempel:\n\"Kjøpte kontorrekvisita for 450 kroner\"",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                style = androidx.compose.ui.text.TextStyle(
                                    lineHeight = 20.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            )
                        } else {
                            Text(
                                text = textToDisplay,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.testTag("speech_transcribed_text")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            helper.stopListening()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Avbryt")
                    }

                    if (errorState != null) {
                        Button(
                            onClick = { helper.startListening() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Prøv igjen")
                        }
                    } else {
                        Button(
                            onClick = {
                                helper.stopListening()
                                val finalResult = partialText.ifEmpty { finalText }
                                if (finalResult.isNotBlank()) {
                                    onResult(finalResult)
                                }
                                onDismiss()
                            },
                            enabled = partialText.isNotBlank() || finalText.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("speech_use_text_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Sett inn")
                        }
                    }
                }
            }
        }
    }
}
