package com.example.llamatik.pure_local_llm

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LocalChatApp(vm: ChatViewModel = viewModel()) {
    val history by vm.chatHistory.collectAsState()
    val isGenerating by vm.isGenerating.collectAsState()
    var textInput by remember { mutableStateOf("") }

    Scaffold(
        bottomBar = {
            BottomAppBar {
                TextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    modifier = Modifier.weight(1f).padding(8.dp),
                    placeholder = { Text("Ask Llama...") },
                    enabled = !isGenerating
                )
                Button(
                    onClick = {
                        vm.sendPrompt(textInput)
                        textInput = ""
                    },
                    modifier = Modifier.padding(8.dp),
                    enabled = !isGenerating && textInput.isNotBlank()
                ) {
                    Text(if (isGenerating) "..." else "Send")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            reverseLayout = true // Optional: to show latest at bottom if list is reversed, but here history appends.
            // If history appends to end, scroll to bottom is needed.
            // For simplicity, I'll stick to user snippet which didn't have reverseLayout but did have LazyColumn.
        ) {
            // User snippet: items(history)
            // But usually we want latest at bottom. If history is [msg1, msg2], items renders msg1 then msg2.
            // So msg2 (latest) is at bottom. That's fine.
            items(history) { message ->
                Card(modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()) {
                    Text(text = message, modifier = Modifier.padding(12.dp))
                }
            }
        }
    }
}
