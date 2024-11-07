# Opacity Android

Core library of Opacity for Android

# Install

Add the following repos to your settings (root `build.gradle`)

```
maven { url "https://maven.mozilla.org/maven2/" }
maven { url 'https://jitpack.io' }
```

Then add the dependency on the main core package:

```
implementation 'com.github.OpacityLabs:opacity-android:[GET THE LATEST RELEASE VERSION]'
```

## Add an activity

You need to add an activity to the "AndroidManifest.xml". It's used to launch the in-app web browser:

```xml
        <activity
            android:name="com.opacitylabs.opacitycore.InAppBrowserActivity"
            android:theme="@style/Theme.AppCompat.DayNight" />
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

        val result = OpacityCore.initialize(this, "[YOUR OPACITY API KEY]", false, OpacityCore.Environment.PRODUCTION)
        println("Core init status: " + result)
    }
}

```

## Running the sample app

You need to create an `env` (no leading dot) file inside `app/src/main/assets` (create the folders if necessary). Inside you need the OPACITY_API_KEY variable you will get from the backend:

```
OPACITY_API_KEY=[Your backend key]
```
