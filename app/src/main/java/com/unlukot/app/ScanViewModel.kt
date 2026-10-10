package com.unlukot.app

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.unlukot.app.ai.BitmapLoader
import com.unlukot.app.ai.ScanResult
import com.unlukot.app.ai.UnLukotEngine
import com.unlukot.app.data.AppSettings
import com.unlukot.app.data.ScanStore
import com.unlukot.app.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScanViewModel(app: Application) : AndroidViewModel(app) {

    sealed interface UiState {
        data object Idle : UiState
        data class Processing(val step: Int, val stage: String) : UiState
        data class Done(val result: ScanResult, val image: Bitmap) : UiState
        data class Failed(val message: String) : UiState
    }

    private val engine = UnLukotEngine()
    private val store = ScanStore(app)
    private val settingsStore = SettingsStore(app)

    private val _state = MutableStateFlow<UiState>(UiState.Idle)
    val state: StateFlow<UiState> = _state

    private val _history = MutableStateFlow<List<ScanResult>>(emptyList())
    val history: StateFlow<List<ScanResult>> = _history

    private val _settings = MutableStateFlow(settingsStore.load())
    val settings: StateFlow<AppSettings> = _settings

    private val _storageBytes = MutableStateFlow(0L)
    val storageBytes: StateFlow<Long> = _storageBytes

    init {
        refreshHistory()
    }

    fun processBitmap(bitmap: Bitmap) {
        viewModelScope.launch { runEngine(bitmap) }
    }

    fun processUri(uri: Uri) {
        viewModelScope.launch {
            _state.value = UiState.Processing(0, "Opening image…")
            val bmp = withContext(Dispatchers.IO) { BitmapLoader.load(getApplication(), uri) }
            if (bmp == null) _state.value = UiState.Failed("Couldn't open that image.")
            else runEngine(bmp)
        }
    }

    private suspend fun runEngine(bitmap: Bitmap) {
        _state.value = UiState.Processing(0, "Getting ready…")
        try {
            val out = engine.analyze(bitmap, _settings.value.flatten) { step, label ->
                _state.value = UiState.Processing(step, label)
            }
            if (_settings.value.saveHistory) {
                withContext(Dispatchers.IO) { store.save(out.result, out.image) }
                refreshHistory()
            }
            _state.value = UiState.Done(out.result, out.image)
        } catch (e: Exception) {
            _state.value = UiState.Failed(e.message ?: "Scan failed. Please try again.")
        }
    }

    fun open(result: ScanResult) {
        viewModelScope.launch {
            val img = withContext(Dispatchers.IO) { store.image(result.id) }
            if (img != null) _state.value = UiState.Done(result, img)
            else _state.value = UiState.Failed("The saved image is missing.")
        }
    }

    suspend fun thumbnail(id: Long): Bitmap? = withContext(Dispatchers.IO) { store.thumbnail(id) }

    fun delete(result: ScanResult) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { store.delete(result.id) }
            refreshHistory()
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { store.clear() }
            refreshHistory()
        }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        _settings.value = next
        settingsStore.save(next)
    }

    fun reset() {
        _state.value = UiState.Idle
    }

    private fun refreshHistory() {
        viewModelScope.launch {
            _history.value = withContext(Dispatchers.IO) { store.list() }
            _storageBytes.value = withContext(Dispatchers.IO) { store.sizeBytes() }
        }
    }

    override fun onCleared() {
        engine.close()
    }
}
