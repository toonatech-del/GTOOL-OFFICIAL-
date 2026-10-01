package com.example.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.example.ui.screens.DimensionUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class PhotoResizerState(
    val selectedPresetName: String = "Custom",
    val targetWidthPx: Int = 1080,
    val targetHeightPx: Int = 1080,
    val unit: DimensionUnit = DimensionUnit.PX,
    val targetDpi: Int = 200,
    val targetMaxKb: Int = 200,
    val isCustomMode: Boolean = true,
    val isAspectRatioLocked: Boolean = true,
    val originalBitmap: Bitmap? = null,
    val candidateName: String = "",
    val dateOfPhoto: String = "",
    val addNameDate: Boolean = false
)

class PhotoResizerViewModel : ViewModel() {
    private val _state = MutableStateFlow(PhotoResizerState())
    val state: StateFlow<PhotoResizerState> = _state

    fun setCustomMode(isCustom: Boolean) {
        _state.value = _state.value.copy(isCustomMode = isCustom)
    }

    fun updatePreset(name: String, widthCm: Float, heightCm: Float, dpi: Int, maxKb: Int) {
        val pxW = (widthCm / 2.54f * dpi).toInt()
        val pxH = (heightCm / 2.54f * dpi).toInt()
        
        _state.value = _state.value.copy(
            selectedPresetName = name,
            targetWidthPx = pxW,
            targetHeightPx = pxH,
            targetDpi = dpi,
            targetMaxKb = maxKb,
            isCustomMode = false
        )
    }
    
    // Additional methods to update dimensions, DPI, etc.
}
