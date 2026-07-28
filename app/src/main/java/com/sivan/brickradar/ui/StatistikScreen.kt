package com.sivan.brickradar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sivan.brickradar.model.StatsResponse
import com.sivan.brickradar.ui.theme.AccentGold
import com.sivan.brickradar.ui.theme.AppBackground
import com.sivan.brickradar.ui.theme.CardBackground
import com.sivan.brickradar.ui.theme.CardBorder
import com.sivan.brickradar.ui.theme.MonoFont
import com.sivan.brickradar.ui.theme.PanelBackground
import com.sivan.brickradar.ui.theme.TextMuted
import com.sivan.brickradar.ui.theme.TextMutedMore
import com.sivan.brickradar.ui.theme.TextPrimary
import com.sivan.brickradar.viewmodel.StatistikUiState
import com.sivan.brickradar.viewmodel.StatistikViewModel

// Version 1 — visar bara det övergripande kr/del-snittet för klon respektive
// LEGO (samma referenspunkter som detaljvyns VÄRDESKALA, se
// ValueScaleSection/avgKrPerPieceCloneAll/_lego_all i StatsResponse).
// Fler vyer (statusfördelning, kategori-snitt, totalt investerat värde) är
// medvetet inte med här — se CLAUDE.md.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatistikScreen(
    onBack: () -> Unit,
    viewModel: StatistikViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = { Text("Statistik", color = TextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PanelBackground),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Tillbaka", tint = TextMuted)
                    }
                },
            )
        },
    ) { padding ->
        when (val state = uiState) {
            is StatistikUiState.Loading -> LoadingBox(padding)
            is StatistikUiState.Error -> ErrorBox(padding, state.message, onRetry = viewModel::loadStats)
            is StatistikUiState.Loaded -> StatistikContent(padding, state.stats)
        }
    }
}

@Composable
private fun StatistikContent(padding: androidx.compose.foundation.layout.PaddingValues, stats: StatsResponse) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "KR/DEL I SNITT",
            style = MaterialTheme.typography.labelMedium,
            color = TextMutedMore,
        )
        AverageCard(label = "Klon-snitt", value = stats.avgKrPerPieceCloneAll)
        AverageCard(label = "LEGO-snitt", value = stats.avgKrPerPieceLegoAll)
    }
}

@Composable
private fun AverageCard(label: String, value: Double?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(width = 1.dp, color = CardBorder, shape = RoundedCornerShape(16.dp))
            .padding(20.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.titleSmall, color = TextMutedMore)
        Text(
            text = value?.let { "%.2f kr".format(it) } ?: "–",
            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = MonoFont),
            color = AccentGold,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun LoadingBox(padding: androidx.compose.foundation.layout.PaddingValues) {
    Box(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = AccentGold)
    }
}

@Composable
private fun ErrorBox(padding: androidx.compose.foundation.layout.PaddingValues, message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = message, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            Button(onClick = onRetry) {
                Text("Försök igen")
            }
        }
    }
}
