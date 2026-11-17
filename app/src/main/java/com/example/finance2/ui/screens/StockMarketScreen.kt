package com.example.finance2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finance2.data.Stock
import com.example.finance2.ui.FinanceUiState
import com.example.finance2.ui.FinanceViewModel
import com.example.finance2.ui.StockFilter
import com.example.finance2.ui.theme.ExpenseRed
import com.example.finance2.ui.theme.IncomeGreen
import com.example.finance2.ui.theme.PrimaryGreen

@Composable
fun StockMarketScreen(
    state: FinanceUiState,
    onBack: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (StockFilter) -> Unit,
    onBuyStock: (stock: Stock, quantity: Int, pricePerShare: Double) -> Unit,
    onRefreshMoex: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedStockForBuy by remember { mutableStateOf<Stock?>(null) }

    val filteredStocks = state.stocks.filter { stock ->
        val matchesQuery = stock.name.contains(state.stockSearchQuery, ignoreCase = true) ||
                stock.ticker.contains(state.stockSearchQuery, ignoreCase = true)

        val matchesFilter = when (state.stockFilter) {
            StockFilter.ALL -> true
            StockFilter.STOCKS -> true
            StockFilter.PORTFOLIO -> stock.isHolding
        }

        matchesQuery && matchesFilter
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Назад",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(PrimaryGreen.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (state.isLoadingMoex) "Обновление MOEX..." else "MOEX Live",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(onClick = onRefreshMoex) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Обновить котировки MOEX",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 4.dp)
        ) {
            Text(
                text = "Рынок акций",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Нажмите на акцию для покупки",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Search Bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                OutlinedTextField(
                    value = state.stockSearchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Поиск акций, тикер, компания", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Поиск",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Filter Chips Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChipItem(
                    title = "Все",
                    isSelected = state.stockFilter == StockFilter.ALL,
                    onClick = { onFilterChange(StockFilter.ALL) }
                )
                FilterChipItem(
                    title = "Акции",
                    isSelected = state.stockFilter == StockFilter.STOCKS,
                    onClick = { onFilterChange(StockFilter.STOCKS) }
                )
                FilterChipItem(
                    title = "Портфель",
                    isSelected = state.stockFilter == StockFilter.PORTFOLIO,
                    onClick = { onFilterChange(StockFilter.PORTFOLIO) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Stock Quotes List
            filteredStocks.forEach { stock ->
                StockMarketRow(
                    stock = stock,
                    onClick = { selectedStockForBuy = stock }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    selectedStockForBuy?.let { stock ->
        BuyStockDialog(
            stock = stock,
            onDismiss = { selectedStockForBuy = null },
            onConfirm = { qty, price ->
                onBuyStock(stock, qty, price)
                selectedStockForBuy = null
            }
        )
    }
}

@Composable
private fun FilterChipItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.surface
            )
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun StockMarketRow(
    stock: Stock,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (stock.isHolding) PrimaryGreen.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(stock.logoBgColor)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stock.logoText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = stock.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (stock.isHolding) "${stock.ticker} · В портфеле" else stock.ticker,
                        fontSize = 12.sp,
                        color = if (stock.isHolding) PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stock.priceText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stock.changePercentText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (stock.isPositive) IncomeGreen else ExpenseRed
                )
            }
        }
    }
}

@Composable
private fun BuyStockDialog(
    stock: Stock,
    onDismiss: () -> Unit,
    onConfirm: (quantity: Int, pricePerShare: Double) -> Unit
) {
    var quantityText by remember { mutableStateOf("1") }
    var priceText by remember { mutableStateOf(stock.priceValue.toString()) }

    val qty = quantityText.toIntOrNull() ?: 0
    val price = priceText.toDoubleOrNull() ?: 0.0
    val totalCost = qty * price

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Покупка акций ${stock.ticker}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stock.name,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { if (it.all { char -> char.isDigit() }) quantityText = it },
                    label = { Text("Количество (шт.)") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Цена за штуку (₽)") },
                    singleLine = true
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Итоговая стоимость:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = FinanceViewModel.formatCurrency(totalCost),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (qty > 0 && price > 0.0) {
                        onConfirm(qty, price)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text("Купить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
