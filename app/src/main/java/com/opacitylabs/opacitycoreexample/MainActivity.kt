package com.opacitylabs.opacitycoreexample

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.opacitylabs.opacitycore.JsonConverter
import com.opacitylabs.opacitycore.OpacityCore
import com.opacitylabs.opacitycore.OpacityError
import com.opacitylabs.opacitycoreexample.ui.theme.OpacityCoreExampleTheme
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader

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
                    Column(modifier = Modifier.padding(innerPadding)) {
                        val flowInput = remember { mutableStateOf("instagram:comments") }
                        val paramsInput =
                            remember { mutableStateOf("{\"previous_response\":\"\"}") }

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
                    }
                }
            }
        }

    }
}