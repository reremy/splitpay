package com.example.splitpay.ui.receipt

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.model.ReceiptData
import com.example.splitpay.domain.parser.ReceiptParser
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class ReceiptScannerUiState(
    val isProcessing: Boolean = false,
    val extractedText: String? = "", // Placeholder for actual receipt object
    val parsedReceipt: ReceiptData? = null,
    val error: String? = null
)

class ReceiptScannerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ReceiptScannerUiState())
    val uiState: StateFlow<ReceiptScannerUiState> = _uiState.asStateFlow()

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private val receiptParser = ReceiptParser()


    fun processReceiptImage(uri: Uri, context: Context){
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true, error = null)

            try {
                val image = InputImage.fromFilePath(context, uri)

                textRecognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val extractedText = visionText.text

                        val parsedData = receiptParser.parseReceipt(extractedText)

                        _uiState.value = uiState.value.copy(
                            isProcessing = false,
                            extractedText = extractedText,
                            parsedReceipt = parsedData
                        )

                        println("Parsed Receipt: $extractedText")
                    }
                    .addOnFailureListener { e ->
                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            error = "Failed to process image: ${e.message}"
                        )
                        e.printStackTrace()
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    error = "Failed to process image: ${e.message}"
                )
                e.printStackTrace()
            }
        }
    }
}