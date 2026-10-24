package com.salvarez.miboveda.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.salvarez.miboveda.domain.model.PasswordEntry
import com.salvarez.miboveda.presentation.common.message
import com.salvarez.miboveda.presentation.detail.EntryDetailRoute
import com.salvarez.miboveda.presentation.edit.EntryEditRoute
import com.salvarez.miboveda.presentation.list.EntryListRoute
import com.salvarez.miboveda.presentation.onboarding.OnboardingRoute
import com.salvarez.miboveda.presentation.recovery.RecoveryRoute
import com.salvarez.miboveda.presentation.settings.SettingsRoute
import com.salvarez.miboveda.presentation.start.StartState
import com.salvarez.miboveda.presentation.start.StartViewModel
import com.salvarez.miboveda.presentation.unlock.UnlockRoute

private const val ROUTE_ONBOARDING = "onboarding"
private const val ROUTE_UNLOCK = "unlock"
private const val ROUTE_RECOVERY = "recovery"
private const val ROUTE_ENTRIES = "entries"
private const val ROUTE_SETTINGS = "settings"
private const val ARG_ID = "id"
private const val ROUTE_DETAIL = "entries/{$ARG_ID}"
private const val ROUTE_EDIT = "entries/{$ARG_ID}/edit"

private fun detailRoute(id: Long) = "entries/$id"
private fun editRoute(id: Long) = "entries/$id/edit"

@Composable
fun MiBovedaNavHost(startViewModel: StartViewModel = hiltViewModel()) {
    val startState by startViewModel.state.collectAsStateWithLifecycle()
    val isUnlocked by startViewModel.isUnlocked.collectAsStateWithLifecycle()
    when (val start = startState) {
        StartState.Loading -> CenteredBox { CircularProgressIndicator() }
        is StartState.Failed -> CenteredBox { Text(start.error.message(), color = MaterialTheme.colorScheme.error) }
        StartState.NoVault -> VaultNavHost(ROUTE_ONBOARDING, isUnlocked)
        StartState.VaultLocked -> VaultNavHost(ROUTE_UNLOCK, isUnlocked)
        StartState.VaultUnlocked -> VaultNavHost(ROUTE_ENTRIES, isUnlocked)
    }
}

@Composable
private fun VaultNavHost(startDestination: String, isUnlocked: Boolean) {
    val navController = rememberNavController()
    // Whenever the vault locks itself, leave the data screens and ask for the master password again.
    LaunchedEffect(isUnlocked) {
        val route = navController.currentDestination?.route
        if (!isUnlocked && route != null && route != ROUTE_ONBOARDING && route != ROUTE_UNLOCK) {
            navController.navigate(ROUTE_UNLOCK) { popUpTo(navController.graph.id) { inclusive = true } }
        }
    }
    val idArgument = listOf(navArgument(ARG_ID) { type = NavType.LongType })
    NavHost(navController = navController, startDestination = startDestination) {
        composable(ROUTE_ONBOARDING) {
            OnboardingRoute(onVaultCreated = { navController.openEntriesAfter(ROUTE_ONBOARDING) })
        }
        composable(ROUTE_UNLOCK) {
            UnlockRoute(
                onUnlocked = { navController.openEntriesAfter(ROUTE_UNLOCK) },
                onForgotPassword = { navController.navigate(ROUTE_RECOVERY) },
            )
        }
        composable(ROUTE_RECOVERY) {
            RecoveryRoute(
                onBack = { navController.popBackStack() },
                onRecovered = { navController.openEntriesAfter(ROUTE_UNLOCK) },
            )
        }
        composable(ROUTE_ENTRIES) {
            EntryListRoute(
                onAdd = { navController.navigate(editRoute(PasswordEntry.NEW_ID)) },
                onEntryClick = { navController.navigate(detailRoute(it)) },
                onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
            )
        }
        composable(ROUTE_SETTINGS) {
            SettingsRoute(onBack = { navController.popBackStack() })
        }
        composable(ROUTE_DETAIL, arguments = idArgument) {
            EntryDetailRoute(
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(editRoute(it)) },
            )
        }
        composable(ROUTE_EDIT, arguments = idArgument) {
            EntryEditRoute(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
    }
}

private fun NavController.openEntriesAfter(route: String) {
    navigate(ROUTE_ENTRIES) { popUpTo(route) { inclusive = true } }
}

@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.Center) { content() }
}
