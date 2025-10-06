package com.opacitylabs.opacitycoreexample

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.opacitylabs.opacitycore.JsonConverter
import com.opacitylabs.opacitycore.OpacityCore
import com.opacitylabs.opacitycore.OpacityError
import com.opacitylabs.opacitycoreexample.ui.theme.OpacityCoreExampleTheme
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState

class MainActivity : ComponentActivity() {

    companion object {
        init {
            System.loadLibrary("DemoCpp")
        }

        @JvmStatic
        external fun setEnv()
    }

    private fun loadEnvFile(context: Context): Map<String, String> {
        val envMap = mutableMapOf<String, String>()
        val inputStream = context.assets.open("env")

        BufferedReader(InputStreamReader(inputStream)).use { reader ->
            reader.lineSequence().forEach { line ->
                if (line.isNotBlank() && !line.startsWith("#")) {
                    val (key, value) = line.split("=", limit = 2)
                    envMap[key.trim()] = value.trim()
                }
            }
        }

        return envMap
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setEnv()
        enableEdgeToEdge()

        val dotenv = loadEnvFile(this)
        val opacityApiKey = dotenv["OPACITY_API_KEY"]
        requireNotNull(opacityApiKey) { "Opacity API key is null" }

        OpacityCore.setContext(this)
        OpacityCore.initialize(opacityApiKey, false, OpacityCore.Environment.PRODUCTION, false)

        Log.d("MainActivity", "Opacity SDK initialized and MainActivity loaded")

        setContent {
            OpacityCoreExampleTheme {
                var showSuccessDialog by remember { mutableStateOf(false) }

                // Success dialog
                if (showSuccessDialog) {
                    AlertDialog(
                        onDismissRequest = { showSuccessDialog = false },
                        title = { Text("Success") },
                        text = { Text("Test flow completed successfully!") },
                        confirmButton = {
                            TextButton(onClick = { showSuccessDialog = false }) {
                                Text("OK")
                            }
                        }
                    )
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = androidx.compose.ui.graphics.Color.Black
                ) { innerPadding ->
                    val flowInput = remember { mutableStateOf("github:profile") }
                    val logOutput = remember { mutableStateOf("") }
                    val isStressTestRunning = remember { mutableStateOf(false) }
                    val logLines = logOutput.value.lines()
                    val listState = rememberLazyListState()
                    Column(modifier = Modifier.padding(innerPadding)) {
                        val flowInput = remember { mutableStateOf("github:profile") }

                    // Auto-scroll to bottom when logs update
                    LaunchedEffect(logLines.size, isStressTestRunning.value) {
                        if (isStressTestRunning.value && logLines.isNotEmpty()) {
                            listState.animateScrollToItem(logLines.size - 1)
                        }
                    }

                    if (!isStressTestRunning.value) {
                        Column(modifier = Modifier.padding(innerPadding)) {
                            TextField(
                                value = flowInput.value,
                                onValueChange = { flowInput.value = it },
                                label = { Text("Enter flow") }
                            )
                            
                            TextField(
                                value = paramsInput.value,
                                onValueChange = { paramsInput.value = it },
                                label = { Text("Enter JSON parameters (optional)") }
                            )

                            Button(
                                onClick = {
                                    lifecycleScope.launch {
                                        var params: Map<String, Any>? = null

                                        if (!paramsInput.value.isBlank()) {
                                            params =
                                                JsonConverter.parseJsonElementToAny(
                                                    Json.parseToJsonElement(
                                                        paramsInput.value
                                                    )
                                                ) as Map<String, Any>?
                                        }

                                        val res = OpacityCore.get(flowInput.value, params)
                                        res.fold(
                                            onSuccess = { value ->
                                                Log.e(
                                                    "MainActivity",
                                                    "Res: ${value}value"
                                                )
                                            },
                                            onFailure = {
                                                when (it) {
                                                    is OpacityError -> Log.e(
                                                        "MainActivity",
                                                        "code: ${it.code}, message: ${it.message}"
                                                    )

                                                    else -> Log.e("MainActivity", it.toString())
                                                }
                                            })

                                    }
                                },
                            ) { Text(text = "Run Flow") }

                            Button(
                                onClick = {
                                    lifecycleScope.launch {
                                        try {
                                            val sdkVersions = OpacityCore.getSdkVersions()
                                            Log.d("MainActivity", "SDK Versions: $sdkVersions")
                                        } catch (e: Exception) {
                                            Log.e("MainActivity", e.toString())
                                        }
                                    }
                                },
                            ) { Text(text = "Get SDK Versions") }
                            
                            Button(
                                onClick = {
                                    lifecycleScope.launch {
                                        try {
                                            val dotenv = loadEnvFile(this@MainActivity)
                                            val opacityApiKey = dotenv["OPACITY_API_KEY"]
                                            requireNotNull(opacityApiKey) { "Opacity API key is null" }

                                            OpacityCore.initialize(
                                                opacityApiKey,
                                                false,
                                                OpacityCore.Environment.PRODUCTION,
                                                true
                                            )
                                            Log.d("MainActivity", "Opacity SDK re-initialized")
                                        } catch (e: Exception) {
                                            Log.e("MainActivity", e.toString())
                                        }
                                    }
                                },
                            ) { Text(text = "Re-initialize SDK") }
                            
                            Button(
                                onClick = {
                                    lifecycleScope.launch {
                                        val res = OpacityCore.get("uber_rider:profile", null)
                                        res.fold(
                                            onSuccess = { value ->
                                                Log.e(
                                                    "MainActivity",
                                                    "Res: ${value}value"
                                                )
                                            },
                                            onFailure = {
                                                when (it) {
                                                    is OpacityError -> Log.e(
                                                        "MainActivity",
                                                        "code: ${it.code}, message: ${it.message}"
                                                    )

                                                    else -> Log.e("MainActivity", it.toString())
                                                }
                                            })
                                    }
                                },
                            ) { Text(text = "Uber Rider Profile") }
                            
                            Button(
                                onClick = {
                                    lifecycleScope.launch {
                                        val res =
                                            OpacityCore.get("test:open_browser_must_succeed", null)
                                        res.fold(
                                            onSuccess = { value ->
                                                Log.e(
                                                    "MainActivity",
                                                    "Res: ${value}value"
                                                )
                                                showSuccessDialog = true
                                            },
                                            onFailure = {
                                                when (it) {
                                                    is OpacityError -> Log.e(
                                                        "MainActivity",
                                                        "code: ${it.code}, message: ${it.message}"
                                                    )

                                                    else -> Log.e("MainActivity", it.toString())
                                                }
                                            })
                                    }
                                },
                            ) { Text(text = "Test flow always succeeds") }
                            
                            Button(
                                onClick = {
                                    isStressTestRunning.value = true
                                    lifecycleScope.launch {
                                        try {
                                            logOutput.value = "Starting stress test...\n"
                                            val startTime = System.currentTimeMillis()
                                            var completed = 0
                                            var totalDuration = 0L
                                            val totalRequests = 10000
                                            val concurrentLimit = 40
                                            val lock = Any()
                                            var currentIndex = 0
                                            
                                            suspend fun launchNext(completion: () -> Unit) {
                                                val jobs = mutableListOf<kotlinx.coroutines.Job>()
                                                
                                                repeat(concurrentLimit) {
                                                    val job = lifecycleScope.launch {
                                                        while (true) {
                                                            val i = synchronized(lock) {
                                                                if (currentIndex >= totalRequests) return@launch
                                                                currentIndex++
                                                            }
                                                            
                                                            val reqStart = System.currentTimeMillis()
                                                            try {
                                                                val logMsg = "🟠 Starting request $i\n"
                                                                logOutput.value += logMsg
                                                                val res = OpacityCore.get("stress", null)
                                                                val reqDuration = System.currentTimeMillis() - reqStart
                                                                synchronized(lock) {
                                                                    totalDuration += reqDuration
                                                                    completed++
                                                                }
                                                                val successMsg = "🟩 Request $i completed - took ${String.format("%.3f", reqDuration / 1000.0)}s\n"
                                                                logOutput.value += successMsg
                                                            } catch (e: Exception) {
                                                                synchronized(lock) {
                                                                    completed++
                                                                }
                                                                val errorMsg = "🟥 Error on request $i: ${e.message}\n"
                                                                logOutput.value += errorMsg
                                                            }
                                                        }
                                                    }
                                                    jobs.add(job)
                                                }
                                                
                                                jobs.forEach { it.join() }
                                                completion()
                                            }
                                            
                                            launchNext {
                                                val avg = totalDuration.toDouble() / totalRequests / 1000.0
                                                val finalMsg = "🟩 Stress test completed!\nAverage request time: ${String.format("%.3f", avg)}s\nTotal completed: $completed"
                                                logOutput.value += finalMsg
                                            }
                                            logOutput.value += "🟩 Launched parallel requests (always 40 at a time)\n"
                                        } catch (e: Exception) {
                                            logOutput.value += "🟥 Error: ${e.toString()}\n"
                                        }
                                    }
                                },
                            ) { Text(text = "Stress Test") }
                        }
                    } else {
                        // Full-screen log view with auto-scroll
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(androidx.compose.ui.graphics.Color.Black)
                        ) {
                            items(logLines.size) { idx ->
                                Text(
                                    text = logLines[idx],
                                    fontFamily = FontFamily.Monospace,
                                    color = androidx.compose.ui.graphics.Color.White,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

    }
}