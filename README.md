# Opacity Android

Core library of Opacity for Android

# Install

Add the following repos to your settings (root `build.gradle`)

```
maven { url "https://maven.mozilla.org/maven2/" }
maven { url 'https://jitpack.io' }
```

Then the implementation

```
implementation 'com.github.OpacityLabs:opacity-android:3.5.5'
```

Then you can import the classes and call the SDK functions

```kotlin
import com.opacitylabs.opacitycore.OpacityCore
import com.opacitylabs.opacitycore.OpacityResponse

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SampleopacityandroidappTheme {
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
                    }
                }
            }
        }

        val result = OpacityCore.initialize(this, "[YOUR OPACITY API KEY]", false)
        println("Core init status: " + result)
    }
}

```
