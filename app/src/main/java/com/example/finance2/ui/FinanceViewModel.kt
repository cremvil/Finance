package com.example.finance2.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finance2.data.AnalyticsCategory
import com.example.finance2.data.Goal
import com.example.finance2.data.GoalContribution
import com.example.finance2.data.MoexRepository
import com.example.finance2.data.PortfolioPosition
import com.example.finance2.data.SampleData
import com.example.finance2.data.Screen
import com.example.finance2.data.Stock
import com.example.finance2.data.Transaction
import com.example.finance2.data.TransactionType
import com.example.finance2.ui.theme.ChartBlue
import com.example.finance2.ui.theme.ChartGray
import com.example.finance2.ui.theme.ChartPurple
import com.example.finance2.ui.theme.ChartTeal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class FinanceUiState(
    val currentScreen: Screen = Screen.Overview,
    val previousScreen: Screen? = null,
    val isDarkTheme: Boolean = false,
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val transactions: List<Transaction> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val contributions: List<GoalContribution> = emptyList(),
    val positions: List<PortfolioPosition> = emptyList(),
    val stocks: List<Stock> = SampleData.initialStocks.map { it.copy(isHolding = false, holdingValueText = null) },
    val stockSearchQuery: String = "",
    val stockFilter: StockFilter = StockFilter.ALL,
    val analyticsExpenseCategories: List<AnalyticsCategory> = SampleData.initialAnalyticsCategories.map { it.copy(amount = 0.0, percentage = 0) },
    val analyticsIncomeCategories: List<AnalyticsCategory> = listOf(
        AnalyticsCategory("inc1", "Зарплата", 0.0, 0, ChartTeal),
        AnalyticsCategory("inc2", "Инвестиции", 0.0, 0, ChartBlue),
        AnalyticsCategory("inc3", "Подработка", 0.0, 0, ChartPurple),
        AnalyticsCategory("inc4", "Прочее", 0.0, 0, ChartGray)
    ),
    val analyticsMonthOffset: Int = 0,
    val isLoadingMoex: Boolean = false,
    val userErrorMessage: String? = null
) {
    val portfolioValue: Double
        get() = positions.sumOf { it.currentTotalValue }

    val portfolioInvested: Double
        get() = positions.sumOf { it.totalBuyCost }

    val portfolioProfit: Double
        get() = portfolioValue - portfolioInvested

    val portfolioProfitPercent: Double
        get() = if (portfolioInvested > 0) ((portfolioValue - portfolioInvested) / portfolioInvested) * 100.0 else 0.0

    // Available cash balance on Overview = Income minus Expenses, minus Goal deposits, minus Stock purchases
    val freeCashBalance: Double
        get() = (totalIncome - totalExpenses - goals.sumOf { it.currentAmount } - portfolioInvested).coerceAtLeast(0.0)

    val displayTotalBalance: Double
        get() = freeCashBalance

    val analyticsMonthText: String
        get() {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MONTH, analyticsMonthOffset)
            val sdf = SimpleDateFormat("LLLL yyyy", Locale.forLanguageTag("ru-RU"))
            val formatted = sdf.format(calendar.time)
            return formatted.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.forLanguageTag("ru-RU")) else it.toString() }
        }
}

enum class StockFilter {
    ALL, STOCKS, PORTFOLIO
}

class FinanceViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FinanceUiState())
    val uiState: StateFlow<FinanceUiState> = _uiState.asStateFlow()

    init {
        refreshMoexQuotes()
    }

    private fun calculatePortfolioMetrics(stocks: List<Stock>): PortfolioMetrics {
        val holdings = stocks.filter { it.isHolding }
        val totalValue = holdings.sumOf { it.priceValue }

        if (totalValue <= 0.0) {
            return PortfolioMetrics(0.0, 0.0, 0.0, 0.0)
        }

        val weightedProfitPercent = holdings.sumOf { stock ->
            val pctStr = stock.changePercentText.replace("%", "").replace("+", "").replace(",", ".").trim()
            val pct = pctStr.toDoubleOrNull() ?: 0.0
            stock.priceValue * pct
        } / totalValue

        val profitAmount = totalValue * (weightedProfitPercent / 100.0)
        val investedAmount = (totalValue - profitAmount).coerceAtLeast(0.0)

        return PortfolioMetrics(
            totalValue = totalValue,
            investedAmount = investedAmount,
            profitAmount = profitAmount,
            profitPercent = weightedProfitPercent
        )
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(userErrorMessage = null) }
    }

    fun refreshMoexQuotes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoex = true) }
            val quotes = MoexRepository.fetchLiveQuotes()
            if (quotes.isNotEmpty()) {
                _uiState.update { currentState ->
                    val updatedStocks = MoexRepository.updateStockListWithMoexQuotes(currentState.stocks, quotes)

                    val updatedPositions = currentState.positions.map { pos ->
                        val liveQuote = quotes[pos.ticker]
                        if (liveQuote != null && liveQuote.price > 0.0) {
                            pos.copy(currentPricePerShare = liveQuote.price)
                        } else pos
                    }

                    currentState.copy(
                        stocks = updatedStocks,
                        positions = updatedPositions,
                        isLoadingMoex = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoadingMoex = false) }
            }
        }
    }

    fun buyStock(stock: Stock, quantity: Int, buyPricePerShare: Double) {
        if (quantity <= 0 || buyPricePerShare <= 0.0) return

        val totalCost = quantity * buyPricePerShare
        val currentCash = _uiState.value.freeCashBalance

        if (totalCost > currentCash) {
            _uiState.update {
                it.copy(userErrorMessage = "Недостаточно средств для покупки акций! У вас на балансе ${formatCurrency(currentCash)}, а требуется ${formatCurrency(totalCost)}. Пополните баланс во вкладке Доход.")
            }
            return
        }

        _uiState.update { currentState ->
            val existingPosition = currentState.positions.find { it.ticker == stock.ticker }

            val updatedPositions = if (existingPosition != null) {
                val newQty = existingPosition.quantity + quantity
                val newAvgPrice = ((existingPosition.quantity * existingPosition.buyPricePerShare) + (quantity * buyPricePerShare)) / newQty
                currentState.positions.map { pos ->
                    if (pos.id == existingPosition.id) {
                        pos.copy(
                            quantity = newQty,
                            buyPricePerShare = newAvgPrice,
                            currentPricePerShare = stock.priceValue
                        )
                    } else pos
                }
            } else {
                val newPos = PortfolioPosition(
                    id = "pos_${System.currentTimeMillis()}",
                    stockId = stock.id,
                    stockName = stock.name,
                    ticker = stock.ticker,
                    logoText = stock.logoText,
                    logoBgColor = stock.logoBgColor,
                    quantity = quantity,
                    buyPricePerShare = buyPricePerShare,
                    currentPricePerShare = stock.priceValue
                )
                currentState.positions + newPos
            }

            val updatedStocks = currentState.stocks.map { s ->
                if (s.ticker == stock.ticker) {
                    s.copy(isHolding = true, holdingValueText = formatCurrency(s.priceValue * (existingPosition?.quantity?.plus(quantity) ?: quantity)))
                } else s
            }

            currentState.copy(
                positions = updatedPositions,
                stocks = updatedStocks
            )
        }
    }

    fun sellStock(positionId: String, quantityToSell: Int, sellPricePerShare: Double) {
        if (quantityToSell <= 0 || sellPricePerShare <= 0.0) return

        _uiState.update { currentState ->
            val targetPosition = currentState.positions.find { it.id == positionId } ?: return@update currentState

            val actualQtyToSell = quantityToSell.coerceAtMost(targetPosition.quantity)
            val proceeds = actualQtyToSell * sellPricePerShare
            val costBasis = actualQtyToSell * targetPosition.buyPricePerShare
            val profitOrLoss = proceeds - costBasis

            val remainingQty = targetPosition.quantity - actualQtyToSell
            val updatedPositions = if (remainingQty > 0) {
                currentState.positions.map { pos ->
                    if (pos.id == positionId) {
                        pos.copy(quantity = remainingQty)
                    } else pos
                }
            } else {
                currentState.positions.filterNot { it.id == positionId }
            }

            val stillHasPosition = updatedPositions.any { it.ticker == targetPosition.ticker }
            val updatedStocks = currentState.stocks.map { s ->
                if (s.ticker == targetPosition.ticker) {
                    s.copy(
                        isHolding = stillHasPosition,
                        holdingValueText = if (stillHasPosition) {
                            val remPos = updatedPositions.find { it.ticker == targetPosition.ticker }
                            formatCurrency(s.priceValue * (remPos?.quantity ?: 0))
                        } else null
                    )
                } else s
            }

            val profitText = if (profitOrLoss >= 0) {
                "прибыль +${formatCurrency(profitOrLoss)}"
            } else {
                "убыток -${formatCurrency(-profitOrLoss)}"
            }

            val saleTransaction = Transaction(
                id = "t_sale_${System.currentTimeMillis()}",
                title = "Продажа ${targetPosition.ticker}",
                amount = proceeds,
                type = TransactionType.INCOME,
                category = "Инвестиции",
                categorySubtitle = "Продано $actualQtyToSell шт. · $profitText",
                dateText = "Сегодня",
                iconName = "work"
            )

            val newIncome = currentState.totalIncome + proceeds

            val updatedIncomeCategories = currentState.analyticsIncomeCategories.map { cat ->
                if (cat.name == "Инвестиции") {
                    val newAmt = cat.amount + proceeds
                    cat.copy(amount = newAmt)
                } else cat
            }

            currentState.copy(
                positions = updatedPositions,
                stocks = updatedStocks,
                transactions = listOf(saleTransaction) + currentState.transactions,
                totalIncome = newIncome,
                analyticsIncomeCategories = updatedIncomeCategories
            )
        }
    }

    fun navigateTo(screen: Screen) {
        _uiState.update { currentState ->
            currentState.copy(
                previousScreen = currentState.currentScreen,
                currentScreen = screen
            )
        }
    }

    fun navigateBack() {
        _uiState.update { currentState ->
            val targetScreen = currentState.previousScreen ?: Screen.Overview
            currentState.copy(
                currentScreen = targetScreen,
                previousScreen = null
            )
        }
    }

    fun toggleTheme() {
        _uiState.update { it.copy(isDarkTheme = !it.isDarkTheme) }
    }

    fun previousAnalyticsMonth() {
        _uiState.update { it.copy(analyticsMonthOffset = it.analyticsMonthOffset - 1) }
    }

    fun nextAnalyticsMonth() {
        _uiState.update { it.copy(analyticsMonthOffset = it.analyticsMonthOffset + 1) }
    }

    fun addTransaction(
        title: String,
        amount: Double,
        type: TransactionType,
        category: String,
        comment: String
    ) {
        val currentCash = _uiState.value.freeCashBalance

        if (type == TransactionType.EXPENSE && amount > currentCash) {
            _uiState.update {
                it.copy(userErrorMessage = "Недостаточно средств для списания расхода! У вас на балансе ${formatCurrency(currentCash)}, а вы пытаетесь списать ${formatCurrency(amount)}. Пополните баланс во вкладке Доход.")
            }
            return
        }

        val newTransaction = Transaction(
            id = "t_${System.currentTimeMillis()}",
            title = if (title.isBlank()) category else title,
            amount = amount,
            type = type,
            category = category,
            categorySubtitle = if (type == TransactionType.INCOME) "Доход · $category" else "Расход · $category",
            dateText = "Сегодня",
            iconName = if (type == TransactionType.INCOME) "work" else "cart"
        )

        _uiState.update { currentState ->
            val updatedTransactions = listOf(newTransaction) + currentState.transactions

            val newIncome = if (type == TransactionType.INCOME) {
                currentState.totalIncome + amount
            } else currentState.totalIncome

            val newExpenses = if (type == TransactionType.EXPENSE) {
                currentState.totalExpenses + amount
            } else currentState.totalExpenses

            // Update Expense Analytics
            val updatedExpenseCategories = if (type == TransactionType.EXPENSE) {
                val totalExp = newExpenses.coerceAtLeast(1.0)
                currentState.analyticsExpenseCategories.map { cat ->
                    if (cat.name.equals(category, ignoreCase = true) || (cat.name == "Продукты" && category == "Еда")) {
                        val newAmt = cat.amount + amount
                        cat.copy(amount = newAmt, percentage = ((newAmt / totalExp) * 100).toInt())
                    } else {
                        cat.copy(percentage = ((cat.amount / totalExp) * 100).toInt())
                    }
                }
            } else currentState.analyticsExpenseCategories

            // Update Income Analytics
            val updatedIncomeCategories = if (type == TransactionType.INCOME) {
                val totalInc = newIncome.coerceAtLeast(1.0)
                currentState.analyticsIncomeCategories.map { cat ->
                    if (cat.name.equals(category, ignoreCase = true) || (cat.name == "Зарплата" && category == "Работа")) {
                        val newAmt = cat.amount + amount
                        cat.copy(amount = newAmt, percentage = ((newAmt / totalInc) * 100).toInt())
                    } else {
                        cat.copy(percentage = ((cat.amount / totalInc) * 100).toInt())
                    }
                }
            } else currentState.analyticsIncomeCategories

            currentState.copy(
                transactions = updatedTransactions,
                totalIncome = newIncome,
                totalExpenses = newExpenses,
                analyticsExpenseCategories = updatedExpenseCategories,
                analyticsIncomeCategories = updatedIncomeCategories,
                currentScreen = Screen.Overview
            )
        }
    }

    fun addGoal(title: String, targetAmount: Double) {
        val newGoal = Goal(
            id = "g_${System.currentTimeMillis()}",
            title = title,
            currentAmount = 0.0,
            targetAmount = targetAmount,
            iconName = "star"
        )

        _uiState.update { currentState ->
            currentState.copy(
                goals = currentState.goals + newGoal,
                currentScreen = Screen.Goals
            )
        }
    }

    fun contributeToGoal(goalId: String, amount: Double) {
        val currentCash = _uiState.value.freeCashBalance

        if (amount > currentCash) {
            _uiState.update {
                it.copy(userErrorMessage = "Недостаточно свободных средств для пополнения цели! Доступно ${formatCurrency(currentCash)}, а требуется ${formatCurrency(amount)}. Пополните баланс во вкладке Доход.")
            }
            return
        }

        _uiState.update { currentState ->
            val updatedGoals = currentState.goals.map { goal ->
                if (goal.id == goalId) {
                    goal.copy(currentAmount = goal.currentAmount + amount)
                } else goal
            }

            val targetGoal = currentState.goals.find { it.id == goalId }
            val goalTitle = targetGoal?.title ?: "Цель"

            val newContribution = GoalContribution(
                id = "c_${System.currentTimeMillis()}",
                goalId = goalId,
                goalTitle = goalTitle,
                amount = amount,
                dateText = "Сегодня"
            )

            currentState.copy(
                goals = updatedGoals,
                contributions = listOf(newContribution) + currentState.contributions
            )
        }
    }

    fun updateStockSearchQuery(query: String) {
        _uiState.update { it.copy(stockSearchQuery = query) }
    }

    fun setStockFilter(filter: StockFilter) {
        _uiState.update { it.copy(stockFilter = filter) }
    }

    companion object {
        fun formatCurrency(amount: Double): String {
            val formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("ru-RU"))
            formatter.maximumFractionDigits = 0
            return "${formatter.format(amount)} ₽"
        }

        fun formatCurrencyWithDecimals(amount: Double): String {
            val formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("ru-RU"))
            formatter.minimumFractionDigits = 2
            formatter.maximumFractionDigits = 2
            return "${formatter.format(amount)} ₽"
        }
    }
}

private data class PortfolioMetrics(
    val totalValue: Double,
    val investedAmount: Double,
    val profitAmount: Double,
    val profitPercent: Double
)
