package com.sivan.brickradar.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sivan.brickradar.ui.theme.AccentGold
import com.sivan.brickradar.viewmodel.UpdateDownloadState
import com.sivan.brickradar.viewmodel.UpdateViewModel

// Överlagras ovanpå resten av UI:t (kallas en gång från MainActivitys
// Compose-träd, utanför NavHost) — kollar en gång vid appstart (se
// UpdateViewModel.init) om servern har en nyare version, och driver hela
// dialogkedjan (uppdatering tillgänglig -> ev. installationsbehörighet ->
// nedladdningsförlopp -> installation).
@Composable
fun UpdateChecker(viewModel: UpdateViewModel = viewModel()) {
    val updateInfo by viewModel.updateInfo.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()
    val context = LocalContext.current

    var showPermissionExplainer by remember { mutableStateOf(false) }

    val permissionSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        // Ingen resultatkod att lita på för denna specifika inställningssida
        // (returnerar RESULT_CANCELED även vid ett beviljande på en del
        // enheter) — kollar den faktiska behörigheten direkt istället.
        if (viewModel.canInstallPackages()) {
            viewModel.startDownload()
        }
    }

    // Triggas automatiskt så fort nedladdningen är klar — ingen extra
    // knapptryckning krävs mellan nedladdning och installationsintent.
    LaunchedEffect(downloadState) {
        if (downloadState is UpdateDownloadState.ReadyToInstall) {
            viewModel.installApk()
        }
    }

    when (val state = downloadState) {
        is UpdateDownloadState.Downloading -> DownloadProgressDialog()
        is UpdateDownloadState.Failed -> {
            AlertDialog(
                onDismissRequest = viewModel::dismissUpdate,
                title = { Text("Uppdateringen misslyckades") },
                text = { Text(state.message) },
                confirmButton = {
                    TextButton(onClick = viewModel::dismissUpdate) { Text("OK") }
                },
            )
        }
        else -> {
            val info = updateInfo
            when {
                showPermissionExplainer -> UnknownSourcesExplainerDialog(
                    onConfirm = {
                        showPermissionExplainer = false
                        permissionSettingsLauncher.launch(
                            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                data = Uri.parse("package:${context.packageName}")
                            },
                        )
                    },
                    onDismiss = { showPermissionExplainer = false },
                )
                info != null -> AlertDialog(
                    onDismissRequest = viewModel::dismissUpdate,
                    title = { Text("Ny version tillgänglig — ${info.versionName}") },
                    text = {
                        Text(info.releaseNotes.ifBlank { "En ny version av BrickRadar finns tillgänglig." })
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            if (viewModel.canInstallPackages()) {
                                viewModel.startDownload()
                            } else {
                                showPermissionExplainer = true
                            }
                        }) { Text("Uppdatera") }
                    },
                    dismissButton = {
                        TextButton(onClick = viewModel::dismissUpdate) { Text("Senare") }
                    },
                )
            }
        }
    }
}

@Composable
private fun UnknownSourcesExplainerDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tillåt installation") },
        text = {
            Text(
                "BrickRadar behöver tillåtelse att installera appuppdateringar. " +
                    "Tryck \"Gå till inställningar\" och slå på \"Tillåt från denna källa\" för BrickRadar.",
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Gå till inställningar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Avbryt") }
        },
    )
}

@Composable
private fun DownloadProgressDialog() {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Laddar ner uppdatering") },
        text = {
            Row {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = AccentGold)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Detta tar bara ett ögonblick...")
            }
        },
        confirmButton = {},
    )
}
