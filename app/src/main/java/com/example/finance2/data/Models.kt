package com.example.finance2.data

import androidx.compose.ui.graphics.Color

enum class TransactionType {
    INCOME, EXPENSE
}

data class Transaction(
    val id: String,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: String,
    val categorySubtitle: String,
    val dateText: String,
    val iconName: String = "work"
)

data class Goal(
    val id: String,
    val title: String,
    val currentAmount: Double,
    val targetAmount: Double,
    val iconName: String, // "laptop", "palm", "shield"
    val statusText: String = "В процессе"
) {
    val progressPercentage: Int
        get() = if (targetAmount > 0) ((currentAmount / targetAmount) * 100).toInt() else 0

    val remainingAmount: Double
        get() = (targetAmount - currentAmount).coerceAtLeast(0.0)
}

data class GoalContribution(
    val id: String,
    val goalId: String,
    val goalTitle: String,
    val amount: Double,
    val dateText: String
)

data class Stock(
    val id: String,
    val name: String,
    val ticker: String,
    val priceText: String,
    val priceValue: Double,
    val changePercentText: String,
    val isPositive: Boolean,
    val logoText: String,
    val logoBgColor: Long,
    val holdingValueText: String? = null,
    val isHolding: Boolean = false
)

data class PortfolioPosition(
    val id: String,
    val stockId: String,
    val stockName: String,
    val ticker: String,
    val logoText: String,
    val logoBgColor: Long,
    val quantity: Int,
    val buyPricePerShare: Double,
    val currentPricePerShare: Double
) {
    val totalBuyCost: Double
        get() = quantity * buyPricePerShare

    val currentTotalValue: Double
        get() = quantity * currentPricePerShare

    val profitAmount: Double
        get() = currentTotalValue - totalBuyCost

    val profitPercent: Double
        get() = if (totalBuyCost > 0) (profitAmount / totalBuyCost) * 100.0 else 0.0
}

data class AnalyticsCategory(
    val id: String,
    val name: String,
    val amount: Double,
    val percentage: Int,
    val color: Color
)

sealed class Screen {
    data object Overview : Screen()
    data object Goals : Screen()
    data class GoalDetail(val goalId: String) : Screen()
    data object Investments : Screen()
    data object StockMarket : Screen()
    data object Analytics : Screen()
    data object Settings : Screen()
    data object NewTransaction : Screen()
}
