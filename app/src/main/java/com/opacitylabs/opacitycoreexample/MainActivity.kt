package com.opacitylabs.opacitycoreexample

import android.os.Bundle
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
import com.opacitylabs.opacitycore.OpacityCore
import com.opacitylabs.opacitycoreexample.ui.theme.OpacityCoreExampleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OpacityCoreExampleTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        Button(
                                onClick = { OpacityCore.getUberRiderProfile() },

                        ) { Text(text = "Get uber driver profile") }
                        Button(
                                onClick = { OpacityCore.sampleRedirection() },

                        ) { Text(text = "Sample redirection") }
                    }
                }
            }
        }

        val result = OpacityCore.initialize(this, "sample_key", false)
        println("Core init status: " + result)
    }
}
