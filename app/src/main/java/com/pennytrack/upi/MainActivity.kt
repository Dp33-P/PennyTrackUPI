package com.pennytrack.upi

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.pennytrack.upi.data.model.Categories
import com.pennytrack.upi.data.model.TransactionEntity
import com.pennytrack.upi.data.model.TransactionType
import com.pennytrack.upi.ui.CategoryTotal
import com.pennytrack.upi.ui.DateFormatters
import com.pennytrack.upi.ui.MainUiState
import com.pennytrack.upi.ui.MainViewModel
import com.pennytrack.upi.ui.MainViewModelFactory
import com.pennytrack.upi.ui.MoneyFormat
import com.pennytrack.upi.ui.theme.AppAccent
import com.pennytrack.upi.ui.theme.AppAmber
import com.pennytrack.upi.ui.theme.AppBackground
import com.pennytrack.upi.ui.theme.AppMuted
import com.pennytrack.upi.ui.theme.AppRed
import com.pennytrack.upi.ui.theme.PennyTrackTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory((application as PennyTrackApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PennyTrackTheme {
                PennyTrackApp(viewModel)
            }
        }
    }
}

private data class AppScreen(
    val key: String,
    val label: String,
    val icon: ImageVector
)

private val screens = listOf(
    AppScreen("home", "Home", Icons.Rounded.Home),
    AppScreen("transactions", "Spend", Icons.Rounded.List),
    AppScreen("cash", "Cash", Icons.Rounded.Add),
    AppScreen("review", "Review", Icons.Rounded.Inbox),
    AppScreen("settings", "More", Icons.Rounded.Settings)
)

@Composable
private fun PennyTrackApp(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedScreen by rememberSaveable { mutableStateOf("home") }
    var hasSmsPermission by remember {
        mutableStateOf(context.hasPermission(Manifest.permission.READ_SMS))
    }
    val smsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasSmsPermission = granted
        if (granted) viewModel.importSms(context)
    }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatus()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                screens.forEach { screen ->
                    NavigationBarItem(
                        selected = selectedScreen == screen.key,
                        onClick = { selectedScreen = screen.key },
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label, maxLines = 1) }
                    )
                }
            }
        },
        containerColor = AppBackground
    ) { innerPadding ->
        val modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(AppBackground)

        when (selectedScreen) {
            "home" -> DashboardScreen(
                state = state,
                hasSmsPermission = hasSmsPermission,
                notificationEnabled = context.isNotificationListenerEnabled(),
                onRequestSms = { smsLauncher.launch(Manifest.permission.READ_SMS) },
                onScanSms = { viewModel.importSms(context) },
                onOpenNotificationSettings = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                },
                modifier = modifier
            )
            "transactions" -> TransactionsScreen(
                state = state,
                onCategorySelected = viewModel::updateCategory,
                modifier = modifier
            )
            "cash" -> CashScreen(
                onSave = viewModel::addCash,
                modifier = modifier
            )
            "review" -> ReviewScreen(
                state = state,
                onExclude = viewModel::excludeTransaction,
                onKeep = viewModel::keepTransaction,
                onCategorySelected = viewModel::updateCategory,
                modifier = modifier
            )
            "settings" -> SettingsScreen(
                state = state,
                hasSmsPermission = hasSmsPermission,
                notificationEnabled = context.isNotificationListenerEnabled(),
                onRequestSms = { smsLauncher.launch(Manifest.permission.READ_SMS) },
                onScanSms = { viewModel.importSms(context) },
                onOpenNotificationSettings = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                },
                onBudgetSave = viewModel::setMonthlyBudget,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun DashboardScreen(
    state: MainUiState,
    hasSmsPermission: Boolean,
    notificationEnabled: Boolean,
    onRequestSms: () -> Unit,
    onScanSms: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Header(title = "PennyTrack", subtitle = DateFormatters.monthLabel())
        SummaryGrid(state)
        AccessPanel(
            hasSmsPermission = hasSmsPermission,
            notificationEnabled = notificationEnabled,
            isImporting = state.isImporting,
            onRequestSms = onRequestSms,
            onScanSms = onScanSms,
            onOpenNotificationSettings = onOpenNotificationSettings
        )
        InsightPanel(state.insights)
        CategoryBreakdown(
            totals = state.categoryTotals,
            budgetPaise = state.monthlyBudgetPaise
        )
        RecentTransactions(state.transactions.take(5))
    }
}

@Composable
private fun Header(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = AppMuted
        )
    }
}

@Composable
private fun SummaryGrid(state: MainUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard("Today", MoneyFormat.format(state.todaySpendPaise), Modifier.weight(1f))
            MetricCard("Month", MoneyFormat.format(state.monthSpendPaise), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard("Safe/day", MoneyFormat.format(state.safePerDayPaise), Modifier.weight(1f))
            MetricCard("Fixed", MoneyFormat.format(state.fixedThisMonthPaise), Modifier.weight(1f))
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier.height(92.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = AppMuted)
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AccessPanel(
    hasSmsPermission: Boolean,
    notificationEnabled: Boolean,
    isImporting: Boolean,
    onRequestSms: () -> Unit,
    onScanSms: () -> Unit,
    onOpenNotificationSettings: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AccessRow(
                icon = Icons.Rounded.Sms,
                label = "SMS",
                active = hasSmsPermission,
                actionLabel = if (hasSmsPermission) "Scan" else "Allow",
                onAction = if (hasSmsPermission) onScanSms else onRequestSms,
                busy = isImporting
            )
            AccessRow(
                icon = Icons.Rounded.Notifications,
                label = "Notifications",
                active = notificationEnabled,
                actionLabel = "Open",
                onAction = onOpenNotificationSettings,
                busy = false
            )
        }
    }
}

@Composable
private fun AccessRow(
    icon: ImageVector,
    label: String,
    active: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
    busy: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = label, tint = if (active) AppAccent else AppMuted)
            Spacer(Modifier.width(10.dp))
            Text(label, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(8.dp))
            if (active) Icon(Icons.Rounded.Check, contentDescription = "Active", tint = AppAccent, modifier = Modifier.size(18.dp))
        }
        OutlinedButton(onClick = onAction, enabled = !busy) {
            Text(if (busy) "Scanning" else actionLabel)
        }
    }
}

@Composable
private fun InsightPanel(insights: List<String>) {
    if (insights.isEmpty()) return
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Money signals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            insights.forEach { insight ->
                Text(insight, style = MaterialTheme.typography.bodyMedium, color = AppMuted)
            }
        }
    }
}

@Composable
private fun CategoryBreakdown(totals: List<CategoryTotal>, budgetPaise: Long) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Categories", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(MoneyFormat.format(budgetPaise), color = AppMuted)
            }
            if (totals.isEmpty()) {
                EmptyText("No spends yet")
            } else {
                totals.take(8).forEach { total ->
                    CategoryBar(total = total, maxPaise = totals.maxOf { it.amountPaise })
                }
            }
        }
    }
}

@Composable
private fun CategoryBar(total: CategoryTotal, maxPaise: Long) {
    val progress = if (maxPaise == 0L) 0f else total.amountPaise.toFloat() / maxPaise.toFloat()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(total.category, style = MaterialTheme.typography.bodyMedium)
            Text(MoneyFormat.format(total.amountPaise), style = MaterialTheme.typography.bodyMedium, color = AppMuted)
        }
        LinearProgressIndicator(
            progress = progress.coerceIn(0f, 1f),
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = AppAccent,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun RecentTransactions(transactions: List<TransactionEntity>) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Recent", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (transactions.isEmpty()) {
                EmptyText("No transactions")
            } else {
                transactions.forEach { TransactionLine(it) }
            }
        }
    }
}

@Composable
private fun TransactionsScreen(
    state: MainUiState,
    onCategorySelected: (TransactionEntity, String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 20.dp)
    ) {
        item { Header("Transactions", "${state.transactions.size} entries") }
        if (state.transactions.isEmpty()) {
            item { EmptyPanel("No transactions yet") }
        } else {
            items(state.transactions, key = { it.id }) { transaction ->
                TransactionCard(transaction, onCategorySelected)
            }
        }
    }
}

@Composable
private fun TransactionCard(
    transaction: TransactionEntity,
    onCategorySelected: (TransactionEntity, String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = transaction.merchantName ?: transaction.category,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${transaction.category} · ${DateFormatters.dayTime(transaction.dateMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                CategoryMenu(transaction.category) { category -> onCategorySelected(transaction, category) }
            }
            Text(
                text = if (transaction.type == TransactionType.DEBIT) MoneyFormat.format(transaction.amountPaise) else "+${MoneyFormat.format(transaction.amountPaise)}",
                color = if (transaction.type == TransactionType.DEBIT) AppRed else AppAccent,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CashScreen(
    onSave: (String, String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var amount by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(Categories.FOOD) }
    var merchant by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    val quickAmounts = listOf("10", "20", "50", "100", "200", "500")
    val commonCategories = listOf(
        Categories.FOOD,
        Categories.GROCERY,
        Categories.TRAVEL,
        Categories.MEDICINE,
        Categories.FUEL,
        Categories.RENT,
        Categories.OTHER
    )

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Header("Cash Entry", "Fast offline expense")
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    quickAmounts.forEach { value ->
                        AssistChip(onClick = { amount = value }, label = { Text("₹$value") })
                    }
                }
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant or person") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    commonCategories.forEach { item ->
                        FilterChip(
                            selected = category == item,
                            onClick = { category = item },
                            label = { Text(item) }
                        )
                    }
                }
                Button(
                    onClick = {
                        if (MoneyFormat.parseInputToPaise(amount) != null) {
                            onSave(amount, category, merchant, note)
                            amount = ""
                            merchant = ""
                            note = ""
                        } else {
                            onSave(amount, category, merchant, note)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save cash expense")
                }
            }
        }
    }
}

@Composable
private fun ReviewScreen(
    state: MainUiState,
    onExclude: (Long) -> Unit,
    onKeep: (Long) -> Unit,
    onCategorySelected: (TransactionEntity, String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 20.dp)
    ) {
        item { Header("Review", "${state.reviewQueue.size} items") }
        if (state.reviewQueue.isEmpty()) {
            item { EmptyPanel("Nothing to review") }
        } else {
            items(state.reviewQueue, key = { it.id }) { transaction ->
                ReviewCard(
                    transaction = transaction,
                    onExclude = { onExclude(transaction.id) },
                    onKeep = { onKeep(transaction.id) },
                    onCategorySelected = { category -> onCategorySelected(transaction, category) }
                )
            }
        }
    }
}

@Composable
private fun ReviewCard(
    transaction: TransactionEntity,
    onExclude: () -> Unit,
    onKeep: () -> Unit,
    onCategorySelected: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TransactionLine(transaction)
            if (transaction.duplicateConfidence >= 55) {
                Text(
                    text = "Possible duplicate · ${transaction.duplicateConfidence}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppAmber,
                    fontWeight = FontWeight.Medium
                )
                transaction.duplicateReasons?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = AppMuted)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onExclude) { Text("Exclude") }
                    Button(onClick = onKeep) { Text("Keep") }
                }
            } else {
                CategoryMenu(transaction.category, onCategorySelected)
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    state: MainUiState,
    hasSmsPermission: Boolean,
    notificationEnabled: Boolean,
    onRequestSms: () -> Unit,
    onScanSms: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onBudgetSave: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var budget by remember(state.monthlyBudgetPaise) {
        mutableStateOf((state.monthlyBudgetPaise / 100).toString())
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Header("More", "Offline only")
        AccessPanel(
            hasSmsPermission = hasSmsPermission,
            notificationEnabled = notificationEnabled,
            isImporting = state.isImporting,
            onRequestSms = onRequestSms,
            onScanSms = onScanSms,
            onOpenNotificationSettings = onOpenNotificationSettings
        )
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Monthly budget", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = budget,
                    onValueChange = { budget = it },
                    label = { Text("Amount") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = { onBudgetSave(budget) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Save budget")
                }
            }
        }
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Local data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("No login · No cloud · No internet permission", color = AppMuted)
            }
        }
    }
}

@Composable
private fun TransactionLine(transaction: TransactionEntity) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                transaction.merchantName ?: transaction.category,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${transaction.category} · ${transaction.source.name.lowercase()}",
                style = MaterialTheme.typography.bodySmall,
                color = AppMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            if (transaction.type == TransactionType.CREDIT) "+${MoneyFormat.format(transaction.amountPaise)}" else MoneyFormat.format(transaction.amountPaise),
            color = if (transaction.type == TransactionType.CREDIT) AppAccent else AppRed,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CategoryMenu(
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        AssistChip(
            onClick = { expanded = true },
            label = { Text(selected, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Categories.all.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category) },
                    onClick = {
                        expanded = false
                        onSelected(category)
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyPanel(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        EmptyText(text, Modifier.padding(24.dp))
    }
}

@Composable
private fun EmptyText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        color = AppMuted,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyMedium
    )
}

private fun Context.hasPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

private fun Context.isNotificationListenerEnabled(): Boolean {
    val enabledListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
    return enabledListeners?.contains(packageName, ignoreCase = true) == true
}
