package com.example.llamatik.pure_local_llm

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.llamatik.library.platform.LlamaBridge
import com.llamatik.library.platform.GenStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val _chatHistory = MutableStateFlow(listOf("Assistant: I'm running locally on your device!"))
    val chatHistory = _chatHistory.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating = _isGenerating.asStateFlow()

    private var isModelLoaded = false

    init {
        prepareAndLoadModel()
    }

    private fun prepareAndLoadModel() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>().applicationContext
                val modelName = "Llama-3.2-1B-Instruct-Q4_K_M.gguf" // Ensure this file is in assets
                val modelFile = File(context.filesDir, modelName)

                // Copy from assets to internal storage if not already there
                if (!modelFile.exists()) {
                    Log.d("ChatViewModel", "Copying model from assets...")
                    try {
                        context.assets.open(modelName).use { input ->
                            modelFile.outputStream().use { output -> input.copyTo(output) }
                        }
                        Log.d("ChatViewModel", "Model copied successfully.")
                    } catch (e: Exception) {
                        Log.e("ChatViewModel", "Error copying model: ${e.message}")
                        _chatHistory.value = _chatHistory.value + "System: Error copying model file. Please ensure 'model.gguf' is in assets."
                        return@launch
                    }
                }

                // Initialize LlamaBridge
                Log.d("ChatViewModel", "Initializing LlamaBridge with path: ${modelFile.absolutePath}")
                val success = LlamaBridge.initGenerateModel(modelFile.absolutePath)
                if (success) {
                    isModelLoaded = true
                    Log.d("ChatViewModel", "Model loaded successfully.")
                } else {
                    Log.e("ChatViewModel", "Failed to load model.")
                    _chatHistory.value = _chatHistory.value + "System: Failed to load model."
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Exception loading model: ${e.message}")
            }
        }
    }

    fun sendPrompt(prompt: String) {
        if (!isModelLoaded) {
            _chatHistory.value = _chatHistory.value + "System: Model not loaded yet."
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isGenerating.value = true
            _chatHistory.value += "You: $prompt"
            
            var currentResponse = "Assistant: "
            _chatHistory.value += currentResponse

            val callback = object : GenStream {
                override fun onDelta(text: String) {
                    currentResponse += text
                    // Update the last message in history
                    // Note: This might be inefficient for long histories, but works for simple demo
                    val currentHistory = _chatHistory.value.toMutableList()
                    if (currentHistory.isNotEmpty()) {
                        currentHistory[currentHistory.lastIndex] = currentResponse
                        _chatHistory.value = currentHistory
                    }
                }

                override fun onComplete() {
                    _isGenerating.value = false
                    Log.d("ChatViewModel", "Generation complete")
                }

                override fun onError(message: String) {
                    _isGenerating.value = false
                    Log.e("ChatViewModel", "Error generating text: $message")
                    val currentHistory = _chatHistory.value.toMutableList()
                    currentHistory.add("System: Error - $message")
                    _chatHistory.value = currentHistory
                }
            }

            try {
                // Using generateStream for streaming response
                LlamaBridge.generateStream(prompt, callback)
            } catch (e: Exception) {
                _isGenerating.value = false
                Log.e("ChatViewModel", "Exception during generation: ${e.message}")
            }
        }
    }
}
