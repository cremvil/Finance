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
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.InsertChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.example.finance2.data.PortfolioPosition
import com.example.finance2.data.Screen
import com.example.finance2.ui.FinanceUiState
import com.example.finance2.ui.FinanceViewModel
import com.example.finance2.ui.components.LineChartCurve
import com.example.finance2.ui.theme.ExpenseRed
import com.example.finance2.ui.theme.IncomeGreen
import com.example.finance2.ui.theme.PrimaryGreen

@Composable
fun InvestmentsScreen(
    state: FinanceUiState,
    onNavigate: (Screen) -> Unit,
    onSellStock: (positionId: String, quantityToSell: Int, sellPricePerShare: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPositionForSell by remember { mutableStateOf<PortfolioPosition?>(null) }

    val isPositive = state.portfolioProfitPercent >= 0
    val arrow = if (isPositive) "↑" else "↓"
    val sign = if (isPositive) "+" else ""
    val formattedPct = String.format(java.util.Locale.forLanguageTag("ru-RU"), "%.1f%%", state.portfolioProfitPercent)
    val mainBadgeText = if (state.portfolioValue > 0) "$arrow $sign$formattedPct за всё время" else "0,0% за всё время"

    val profitColor = if (isPositive) IncomeGreen else ExpenseRed
    val profitBgColor = if (isPositive) IncomeGreen.copy(alpha = 0.12f) else ExpenseRed.copy(alpha = 0.12f)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Инвестиции",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Растите капитал вместе с нами",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { onNavigate(Screen.StockMarket) },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.InsertChart,
                        contentDescription = "Рынок акций",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Portfolio Value Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Стоимость портфеля",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = FinanceViewModel.formatCurrency(state.portfolioValue),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(profitBgColor)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = mainBadgeText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = profitColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LineChartCurve(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        lineColor = profitColor,
                        showGradient = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2 Side-by-side stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEFF6FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AccountBalance,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "Инвестировано",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = FinanceViewModel.formatCurrency(state.portfolioInvested),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(profitBgColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.TrendingUp,
                                contentDescription = null,
                                tint = profitColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "Прибыль",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$sign${FinanceViewModel.formatCurrency(state.portfolioProfit)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = profitColor
                            )
                            Text(
                                text = "($sign$formattedPct)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = profitColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Portfolio Stocks Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Мой портфель",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Все на рынке",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = PrimaryGreen,
                    modifier = Modifier.clickable { onNavigate(Screen.StockMarket) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (state.positions.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "В портфеле пока нет акций. Выберите и купите акции на рынке MOEX.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                state.positions.forEach { position ->
                    PositionRow(
                        position = position,
                        onSellClick = { selectedPositionForSell = position }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }

    selectedPositionForSell?.let { position ->
        SellStockDialog(
            position = position,
            onDismiss = { selectedPositionForSell = null },
            onConfirm = { qty, price ->
                onSellStock(position.id, qty, price)
                selectedPositionForSell = null
            }
        )
    }
}

@Composable
fun PositionRow(
    position: PortfolioPosition,
    onSellClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPosProfitable = position.profitAmount >= 0
    val sign = if (isPosProfitable) "+" else ""
    val posProfitColor = if (isPosProfitable) IncomeGreen else ExpenseRed

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(position.logoBgColor)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = position.logoText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = position.stockName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${position.quantity} шт. по ${FinanceViewModel.formatCurrencyWithDecimals(position.buyPricePerShare)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = FinanceViewModel.formatCurrency(position.currentTotalValue),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$sign${FinanceViewModel.formatCurrency(position.profitAmount)} ($sign${String.format(java.util.Locale.forLanguageTag("ru-RU"), "%.1f%%", position.profitPercent)})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = posProfitColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onSellClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ExpenseRed.copy(alpha = 0.12f),
                    contentColor = ExpenseRed
                )
            ) {
                Text(
                    text = "Продать акции",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SellStockDialog(
    position: PortfolioPosition,
    onDismiss: () -> Unit,
    onConfirm: (quantity: Int, sellPricePerShare: Double) -> Unit
) {
    var quantityText by remember { mutableStateOf(position.quantity.toString()) }
    var priceText by remember { mutableStateOf(position.currentPricePerShare.toString()) }

    val qty = quantityText.toIntOrNull() ?: 0
    val price = priceText.toDoubleOrNull() ?: 0.0
    val proceeds = qty * price
    val costBasis = qty * position.buyPricePerShare
    val profitOrLoss = proceeds - costBasis
    val isProfit = profitOrLoss >= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Продажа акций ${position.ticker}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "${position.stockName} (В наличии: ${position.quantity} шт.)",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { if (it.all { char -> char.isDigit() }) quantityText = it },
                    label = { Text("Количество для продажи (шт.)") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Цена продажи за штуку (₽)") },
                    singleLine = true
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Выручка от продажи (в баланс):",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = FinanceViewModel.formatCurrency(proceeds),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Результат: ${if (isProfit) "+" else ""}${FinanceViewModel.formatCurrency(profitOrLoss)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isProfit) IncomeGreen else ExpenseRed
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (qty > 0 && qty <= position.quantity && price > 0.0) {
                        onConfirm(qty, price)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
            ) {
                Text("Продать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
