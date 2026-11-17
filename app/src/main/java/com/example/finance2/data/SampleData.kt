package com.example.finance2.data

import com.example.finance2.ui.theme.ChartBlue
import com.example.finance2.ui.theme.ChartGray
import com.example.finance2.ui.theme.ChartPurple
import com.example.finance2.ui.theme.ChartRed
import com.example.finance2.ui.theme.ChartTeal
import com.example.finance2.ui.theme.ChartYellow

object SampleData {
    val initialTransactions = emptyList<Transaction>()

    val initialGoals = emptyList<Goal>()

    val initialContributions = emptyList<GoalContribution>()

    val initialStocks = StockList.allStocks

    val initialAnalyticsCategories = listOf(
        AnalyticsCategory("cat1", "Продукты", 0.0, 0, ChartRed),
        AnalyticsCategory("cat2", "Транспорт", 0.0, 0, ChartBlue),
        AnalyticsCategory("cat3", "Жилье", 0.0, 0, ChartPurple),
        AnalyticsCategory("cat4", "Развлечения", 0.0, 0, ChartYellow),
        AnalyticsCategory("cat5", "Одежда", 0.0, 0, ChartTeal),
        AnalyticsCategory("cat6", "Прочее", 0.0, 0, ChartGray)
    )
}
