package com.example.finance2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.finance2.data.Screen
import com.example.finance2.ui.FinanceViewModel
import com.example.finance2.ui.components.FinanceBottomNavBar
import com.example.finance2.ui.screens.AnalyticsScreen
import com.example.finance2.ui.screens.GoalDetailScreen
import com.example.finance2.ui.screens.GoalsScreen
import com.example.finance2.ui.screens.InvestmentsScreen
import com.example.finance2.ui.screens.NewTransactionScreen
import com.example.finance2.ui.screens.OverviewScreen
import com.example.finance2.ui.screens.SettingsScreen
import com.example.finance2.ui.screens.StockMarketScreen
import com.example.finance2.ui.theme.Finance2Theme
import com.example.finance2.ui.theme.PrimaryGreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinanceApp()
        }
    }
}

@Composable
fun FinanceApp(viewModel: FinanceViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    Finance2Theme(darkTheme = state.isDarkTheme) {
        val showBottomBar = state.currentScreen !is Screen.NewTransaction

        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    FinanceBottomNavBar(
                        currentScreen = state.currentScreen,
                        onTabSelected = { screen -> viewModel.navigateTo(screen) }
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Crossfade(
                targetState = state.currentScreen,
                label = "ScreenTransition",
                modifier = Modifier.padding(innerPadding)
            ) { screen ->
                when (screen) {
                    is Screen.Overview -> OverviewScreen(
                        state = state,
                        onNavigate = { viewModel.navigateTo(it) }
                    )

                    is Screen.Goals -> GoalsScreen(
                        state = state,
                        onGoalClick = { goalId -> viewModel.navigateTo(Screen.GoalDetail(goalId)) },
                        onAddGoal = { title, target -> viewModel.addGoal(title, target) }
                    )

                    is Screen.GoalDetail -> GoalDetailScreen(
                        goalId = screen.goalId,
                        state = state,
                        onBack = { viewModel.navigateBack() },
                        onContribute = { goalId, amount -> viewModel.contributeToGoal(goalId, amount) }
                    )

                    is Screen.Investments -> InvestmentsScreen(
                        state = state,
                        onNavigate = { viewModel.navigateTo(it) },
                        onSellStock = { posId, qty, price -> viewModel.sellStock(posId, qty, price) }
                    )

                    is Screen.StockMarket -> StockMarketScreen(
                        state = state,
                        onBack = { viewModel.navigateBack() },
                        onSearchQueryChange = { query -> viewModel.updateStockSearchQuery(query) },
                        onFilterChange = { filter -> viewModel.setStockFilter(filter) },
                        onBuyStock = { stock, qty, price -> viewModel.buyStock(stock, qty, price) },
                        onRefreshMoex = { viewModel.refreshMoexQuotes() }
                    )

                    is Screen.Analytics -> AnalyticsScreen(
                        state = state,
                        onPreviousMonth = { viewModel.previousAnalyticsMonth() },
                        onNextMonth = { viewModel.nextAnalyticsMonth() }
                    )

                    is Screen.Settings -> SettingsScreen(
                        state = state,
                        onBack = { viewModel.navigateBack() },
                        onToggleTheme = { viewModel.toggleTheme() }
                    )

                    is Screen.NewTransaction -> NewTransactionScreen(
                        onCancel = { viewModel.navigateBack() },
                        onSave = { title, amount, type, category, comment ->
                            viewModel.addTransaction(title, amount, type, category, comment)
                        }
                    )
                }
            }

            state.userErrorMessage?.let { errorMsg ->
                AlertDialog(
                    onDismissRequest = { viewModel.clearErrorMessage() },
                    title = { Text("Недостаточно средств") },
                    text = { Text(errorMsg) },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.clearErrorMessage() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                        ) {
                            Text("Понятно")
                        }
                    }
                )
            }
        }
    }
}
