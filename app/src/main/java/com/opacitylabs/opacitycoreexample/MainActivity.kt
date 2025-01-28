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
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.opacitylabs.opacitycore.OpacityCore
import com.opacitylabs.opacitycore.OpacityResponse
import com.opacitylabs.opacitycoreexample.ui.theme.OpacityCoreExampleTheme
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {
    private fun loadEnvFile(context: Context): Map<String, String> {
        val envMap = mutableMapOf<String, String>()
        val inputStream =
            context.assets.open("env")

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
        enableEdgeToEdge()
        setContent {
            OpacityCoreExampleTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = androidx.compose.ui.graphics.Color.Black
                ) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        Button(
                            onClick = {
                                lifecycleScope.launch {
                                    try {
                                        val res = OpacityCore.get("flow:uber_rider:profile", null)
                                        Log.d("MainActivity", res["json"].toString())
                                    } catch (e: Exception) {
                                        Log.e("MainActivity", e.toString())
                                    }
                                }
                            },
                        ) { Text(text = "Get uber driver profile") }
                        Button(
                            onClick = {
                                lifecycleScope.launch {
                                    try {
                                        val res = OpacityCore.get("generate_proof", null)
                                        Log.d("MainActivity", res["json"].toString())
                                        Log.d("MainActivity", res["signature"].toString())
                                    } catch (e: Exception) {
                                        Log.e("MainActivity", e.toString())
                                    }
                                }
                            },
                        ) { Text(text = "Generate Proof") }
                        Button(
                            onClick = {
                                lifecycleScope.launch {
                                    try {
                                        val res = OpacityCore.get("quack", null)
                                        Log.d("MainActivity", res["json"].toString())
                                    } catch (e: Exception) {
                                        Log.e("MainActivity", e.toString())
                                    }
                                }
                            }
                        ) { Text(text = "failing lua") }
                        Button(
                            onClick = {
                                lifecycleScope.launch {
                                    try {
                                        val res = OpacityCore.get("ip_address", null)
                                        Log.d("MainActivity", "🟦🟦🟦")
                                        Log.d("MainActivity", res.toString())
                                    } catch (e: Exception) {
                                        Log.e("MainActivity", e.toString())
                                    }
                                }
                            }
                        ) { Text(text = "Get ip") }
                    }
                }
            }
        }

        val dotenv = loadEnvFile(this)
        val opacityApiKey = dotenv["OPACITY_API_KEY"]
        requireNotNull(opacityApiKey) { "Opacity API key is null" }

        OpacityCore.initialize(opacityApiKey, false, OpacityCore.Environment.PRODUCTION)
        OpacityCore.setContext(this)
    }
}
