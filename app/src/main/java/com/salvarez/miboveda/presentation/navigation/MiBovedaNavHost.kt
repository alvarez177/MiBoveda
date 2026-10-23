package com.salvarez.miboveda.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.salvarez.miboveda.R
import com.salvarez.miboveda.presentation.common.message
import com.salvarez.miboveda.presentation.onboarding.OnboardingRoute
import com.salvarez.miboveda.presentation.start.StartState
import com.salvarez.miboveda.presentation.start.StartViewModel
import com.salvarez.miboveda.presentation.unlock.UnlockRoute

private const val ROUTE_ONBOARDING = "onboarding"
private const val ROUTE_UNLOCK = "unlock"
private const val ROUTE_PLACEHOLDER = "placeholder"

@Composable
fun MiBovedaNavHost(startViewModel: StartViewModel = hiltViewModel()) {
    val startState by startViewModel.state.collectAsStateWithLifecycle()
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        when (val start = startState) {
            StartState.Loading -> CenteredBox(modifier) { CircularProgressIndicator() }
            is StartState.Failed -> CenteredBox(modifier) {
                Text(start.error.message(), color = MaterialTheme.colorScheme.error)
            }
            StartState.NoVault -> VaultNavHost(ROUTE_ONBOARDING, modifier)
            StartState.VaultLocked -> VaultNavHost(ROUTE_UNLOCK, modifier)
            StartState.VaultUnlocked -> VaultNavHost(ROUTE_PLACEHOLDER, modifier)
        }
    }
}

@Composable
private fun VaultNavHost(startDestination: String, modifier: Modifier) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
        composable(ROUTE_ONBOARDING) {
            OnboardingRoute(onVaultCreated = {
                navController.navigate(ROUTE_PLACEHOLDER) { popUpTo(ROUTE_ONBOARDING) { inclusive = true } }
            })
        }
        composable(ROUTE_UNLOCK) {
            UnlockRoute(onUnlocked = {
                navController.navigate(ROUTE_PLACEHOLDER) { popUpTo(ROUTE_UNLOCK) { inclusive = true } }
            })
        }
        composable(ROUTE_PLACEHOLDER) { PlaceholderScreen() }
    }
}

@Composable
private fun PlaceholderScreen() {
    CenteredBox(Modifier) {
        Text(stringResource(R.string.placeholder_message), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun CenteredBox(modifier: Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}
