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
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        Button(
                            onClick = {
                                lifecycleScope.launch {
                                    val res: OpacityResponse = OpacityCore.getUberRiderProfile()
                                    Log.d("MainActivity", res.proof ?: "No proof")
                                    Log.d("MainActivity", res.err ?: "No err")
                                }
                            },
                        ) { Text(text = "Get uber driver profile") }
                        Button (
                            onClick = {
                                lifecycleScope.launch {
                                    val res: OpacityResponse = OpacityCore.getGustoPayrollAdminId();
                                    Log.d("MainActivity", res.proof ?: "No proof")
                                    Log.d("MainActivity", res.err ?: "No err")
                                }
                            },
                        ) { Text(text = "Get Gusto Payroll Admin Id") }
                    }
                }
            }
        }

        val dotenv = loadEnvFile(this)
        val opacityApiKey = dotenv["OPACITY_API_KEY"]
        requireNotNull(opacityApiKey) { "Opacity API key is null" }

        OpacityCore.initialize(opacityApiKey, false, OpacityCore.Environment.STAGING)
        OpacityCore.setContext(this)
    }
}
