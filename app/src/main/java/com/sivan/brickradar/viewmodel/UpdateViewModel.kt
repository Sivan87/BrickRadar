package com.sivan.brickradar.viewmodel

import android.app.Application
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sivan.brickradar.BuildConfig
import com.sivan.brickradar.model.AppVersionResponse
import com.sivan.brickradar.repository.ApiResult
import com.sivan.brickradar.repository.ModelRepository
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val UPDATE_APK_FILENAME = "brickradar-update.apk"

sealed class UpdateDownloadState {
    data object Idle : UpdateDownloadState()
    data object Downloading : UpdateDownloadState()
    data object ReadyToInstall : UpdateDownloadState()
    data class Failed(val message: String) : UpdateDownloadState()
}

// AndroidViewModel (inte den vanliga ViewModel-basklassen appen annars
// använder) eftersom DownloadManager/FileProvider/PackageManager alla
// kräver en Context — application context räcker (ingen Activity-referens
// hålls kvar, så ingen läcka vid konfigurationsändring/rotation).
class UpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ModelRepository()

    // Null = ingen uppdatering känd/tillgänglig (antingen inte kollat än,
    // kollen misslyckades — fail silent, se checkForUpdate — eller servern
    // svarade med samma/lägre versionCode än den installerade appen).
    private val _updateInfo = MutableStateFlow<AppVersionResponse?>(null)
    val updateInfo: StateFlow<AppVersionResponse?> = _updateInfo.asStateFlow()

    private val _downloadState = MutableStateFlow<UpdateDownloadState>(UpdateDownloadState.Idle)
    val downloadState: StateFlow<UpdateDownloadState> = _downloadState.asStateFlow()

    private var downloadId: Long = -1L
    private var receiverRegistered = false

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (completedId == -1L || completedId != downloadId) return
            unregisterReceiver()

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val cursor = downloadManager.query(DownloadManager.Query().setFilterById(downloadId))
            val succeeded = cursor.use {
                it.moveToFirst() &&
                    it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL
            }

            _downloadState.value = if (succeeded) {
                UpdateDownloadState.ReadyToInstall
            } else {
                UpdateDownloadState.Failed("Nedladdningen misslyckades")
            }
        }
    }

    init {
        checkForUpdate()
    }

    private fun checkForUpdate() {
        viewModelScope.launch {
            when (val result = repository.getAppVersion()) {
                is ApiResult.Success -> {
                    if (result.data.versionCode > BuildConfig.VERSION_CODE) {
                        _updateInfo.value = result.data
                    }
                }
                // Fail silent (kickoff-krav): servern kan vara onåbar helt
                // normalt (telefonen utanför hemma-WiFi) — ingen felruta ska
                // störa vanlig appanvändning för detta.
                is ApiResult.Error -> Unit
            }
        }
    }

    fun dismissUpdate() {
        _updateInfo.value = null
    }

    fun canInstallPackages(): Boolean =
        getApplication<Application>().packageManager.canRequestPackageInstalls()

    fun startDownload() {
        val info = _updateInfo.value ?: return
        val context = getApplication<Application>()
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        // Rensar en ev. tidigare nedladdad fil på samma sökväg innan en ny
        // nedladdning startas, så ett omtryck på "Uppdatera" (t.ex. efter att
        // ha beviljat installationsbehörighet i Uppgift 2.4) aldrig råkar
        // installera en gammal/ofullständig fil som redan låg där.
        apkFile(context).delete()

        val request = DownloadManager.Request(Uri.parse(info.downloadUrl))
            .setTitle("BrickRadar-uppdatering")
            .setDescription("Laddar ner version ${info.versionName}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, UPDATE_APK_FILENAME)

        registerReceiver(context)
        downloadId = downloadManager.enqueue(request)
        _downloadState.value = UpdateDownloadState.Downloading
    }

    fun installApk() {
        val context = getApplication<Application>()
        val file = apkFile(context)
        if (!file.exists()) {
            _downloadState.value = UpdateDownloadState.Failed("Hittade inte den nedladdade filen")
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(installIntent)
        _downloadState.value = UpdateDownloadState.Idle
        // Rensas så "Ny version tillgänglig"-dialogen inte dyker upp igen bakom
        // installationsskärmen om användaren avbryter/går tillbaka — appen
        // frågar igen vid nästa vanliga appstart (checkForUpdate körs bara i init).
        _updateInfo.value = null
    }

    private fun apkFile(context: Context): File =
        File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), UPDATE_APK_FILENAME)

    private fun registerReceiver(context: Context) {
        if (receiverRegistered) return
        // DownloadManager skickar ACTION_DOWNLOAD_COMPLETE från
        // com.android.providers.downloads-processen — en annan UID än appens
        // egen — så mottagaren MÅSTE vara RECEIVER_EXPORTED. Med
        // RECEIVER_NOT_EXPORTED filtrerar Android tyst bort broadcasten helt
        // (ingen krasch, inget felmeddelande), vilket gjorde att nedladdningen
        // fastnade permanent på "Laddar ner uppdatering" trots att den i
        // praktiken redan var klar (bekräftat i logcat: DownloadManager
        // rapporterade STATUS_SUCCESSFUL, men onReceive kördes aldrig).
        // Riskfri trots exporten: onReceive jämför den mottagna download-id:n
        // mot vår egen — en spoofad broadcast med fel id ignoreras direkt.
        ContextCompat.registerReceiver(
            context,
            downloadReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED,
        )
        receiverRegistered = true
    }

    private fun unregisterReceiver() {
        if (!receiverRegistered) return
        try {
            getApplication<Application>().unregisterReceiver(downloadReceiver)
        } catch (e: IllegalArgumentException) {
            // Redan avregistrerad (t.ex. dubbel onCleared) — ofarligt, inget att göra.
        }
        receiverRegistered = false
    }

    override fun onCleared() {
        super.onCleared()
        unregisterReceiver()
    }
}
