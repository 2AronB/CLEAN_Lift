package dev.clean.lift.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.clean.lift.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BackupUiState(val busy: Boolean = false, val requestExportFile: Boolean = false,
                         val message: String? = null, val preview: BackupSummary? = null,
                         val current: Map<String, Int> = emptyMap(), val recoveryPending: Boolean = false,
                         val restart: Boolean = false)

class BackupViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = BackupRepository(application, WorkoutDatabase.get(application))
    private val resolver = application.contentResolver
    private val mutable = MutableStateFlow(BackupUiState())
    val state = mutable.asStateFlow()
    private var exportBytes: ByteArray? = null
    private var restoreBytes: ByteArray? = null

    fun prepareExport() {
        if (mutable.value.busy || mutable.value.recoveryPending) return
        mutable.value = mutable.value.copy(busy = true, message = null)
        viewModelScope.launch {
            try {
                exportBytes = repository.export()
                mutable.value = mutable.value.copy(requestExportFile = true)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutable.value = mutable.value.copy(busy = false, message = "Could not prepare a backup (maximum 8 MiB). No file was written; your data is unchanged.") }
        }
    }
    fun exportPickerOpened() { mutable.value = mutable.value.copy(requestExportFile = false) }
    fun exportPickerFailed() {
        exportBytes = null
        mutable.value = mutable.value.copy(busy = false, requestExportFile = false, message = "No file picker could be opened. Your data is unchanged.")
    }
    fun writeExport(uri: Uri?) {
        val bytes = exportBytes
        exportBytes = null
        if (uri == null) { mutable.value = mutable.value.copy(busy = false, message = "Backup cancelled. Your data is unchanged."); return }
        if (bytes == null) { mutable.value = mutable.value.copy(busy = false, message = "The backup draft expired. Delete the empty file and choose Save backup again."); return }
        mutable.value = mutable.value.copy(busy = true)
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { requireNotNull(resolver.openOutputStream(uri, "wt")).use { it.write(bytes); it.flush() } }
                mutable.value = mutable.value.copy(busy = false, message = "Backup saved. Keep the file in a private place.")
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutable.value = mutable.value.copy(busy = false, message = "Could not finish writing the backup. Delete the incomplete file and retry. Your app data is unchanged.") }
        }
    }
    fun load(uri: Uri?) {
        if (mutable.value.busy || mutable.value.recoveryPending) return
        if (uri == null) { mutable.value = mutable.value.copy(message = "File selection cancelled."); return }
        restoreBytes = null
        mutable.value = BackupUiState(busy = true)
        viewModelScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) { requireNotNull(resolver.openInputStream(uri)).use(BackupRepository::readLimited) }
                val preview = repository.preview(bytes)
                val counts = repository.currentCounts()
                restoreBytes = bytes
                mutable.value = BackupUiState(preview = preview, current = counts)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutable.value = BackupUiState(message = "This file could not be read or validated. Use a complete format 1 / database 8 backup, at most 8 MiB. Current data and settings are unchanged.") }
        }
    }
    fun pickerFailed() { mutable.value = mutable.value.copy(message = "No file picker could be opened.") }
    fun restore() {
        val bytes = restoreBytes ?: return
        if (mutable.value.busy || mutable.value.recoveryPending || mutable.value.preview == null) return
        mutable.value = mutable.value.copy(busy = true, message = null)
        viewModelScope.launch {
            try { repository.restore(bytes); restoreBytes = null; mutable.value = BackupUiState(restart = true) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                val pending = runCatching { repository.hasPending() }.getOrDefault(true)
                mutable.value = mutable.value.copy(busy = false, recoveryPending = pending,
                    message = if (pending) "Records were restored, but settings still need to finish. Tap Finish restore, or close and reopen the app."
                    else "Restore failed. Current data and settings are unchanged; the selected file is still available to retry.")
            }
        }
    }
    fun finishRestore() {
        if (mutable.value.busy || !mutable.value.recoveryPending) return
        mutable.value = mutable.value.copy(busy = true)
        viewModelScope.launch {
            try { repository.recoverPending(); mutable.value = BackupUiState(restart = true) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutable.value = mutable.value.copy(busy = false, message = "Could not finish settings. Retry, or close and reopen the app. Restored records remain on this device.") }
        }
    }
    fun dismiss() { if (!mutable.value.busy && !mutable.value.recoveryPending) { restoreBytes = null; mutable.value = BackupUiState() } }
    fun restartConsumed() { mutable.value = mutable.value.copy(restart = false) }
}
