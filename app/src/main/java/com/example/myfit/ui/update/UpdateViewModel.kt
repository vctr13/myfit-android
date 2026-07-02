package com.example.myfit.ui.update

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myfit.BuildConfig
import com.example.myfit.data.update.ReleaseInfo
import com.example.myfit.data.update.UpdateChecker
import kotlinx.coroutines.launch
import java.io.File

sealed class UpdateState {
    object Idle                                           : UpdateState()
    object Checking                                       : UpdateState()
    data class UpToDate(val version: String)              : UpdateState()
    data class UpdateAvailable(val release: ReleaseInfo)  : UpdateState()
    object Downloading                                    : UpdateState()
    object NeedsPermission                                : UpdateState()
    data class Error(val message: String)                 : UpdateState()
}

class UpdateViewModel(app: Application) : AndroidViewModel(app) {

    var state by mutableStateOf<UpdateState>(UpdateState.Idle)
        private set

    val currentVersion: String get() = BuildConfig.VERSION_NAME

    fun checkForUpdates() {
        if (state is UpdateState.Checking || state is UpdateState.Downloading) return
        state = UpdateState.Checking
        viewModelScope.launch {
            try {
                val release = UpdateChecker.fetchLatestRelease()
                state = when {
                    release == null ->
                        UpdateState.Error("Не удалось получить данные о релизе.\nПроверьте подключение к интернету.")
                    UpdateChecker.isNewer(release.versionName, BuildConfig.VERSION_NAME) ->
                        UpdateState.UpdateAvailable(release)
                    else ->
                        UpdateState.UpToDate(BuildConfig.VERSION_NAME)
                }
            } catch (e: Exception) {
                state = UpdateState.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    fun openInstallPermissionSettings() {
        val ctx = getApplication<Application>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${ctx.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(intent)
        }
    }

    fun downloadAndInstall(release: ReleaseInfo) {
        val ctx = getApplication<Application>()

        // Проверяем разрешение на установку (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !ctx.packageManager.canRequestPackageInstalls()
        ) {
            state = UpdateState.NeedsPermission
            return
        }

        state = UpdateState.Downloading
        viewModelScope.launch {
            try {
                val destFile = File(
                    ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    "vfit-update.apk"
                )
                UpdateChecker.downloadApk(release.apkUrl, destFile)

                val uri = FileProvider.getUriForFile(
                    ctx, "${ctx.packageName}.fileprovider", destFile
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(intent)
                state = UpdateState.Idle
            } catch (e: Exception) {
                state = UpdateState.Error("Ошибка: ${e.message}")
            }
        }
    }

    fun reset() { state = UpdateState.Idle }
}
