pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.mozilla.org/maven2/")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://maven.mozilla.org/maven2/")
        maven("https://jitpack.io")
        maven {
            url = uri(findRustlsPlatformVerifierMaven())
            metadataSources.artifact()
        }
    }
}

@Suppress("UNCHECKED_CAST")
fun findRustlsPlatformVerifierMaven(): String {
    val result = providers.exec {
        workingDir = File(rootDir, "../")
        commandLine(
            "cargo", "metadata", "--format-version", "1",
            "--filter-platform", "aarch64-linux-android",
            "--manifest-path", "sdk/Cargo.toml"
        )
    }.standardOutput.asText.get()

    val json = groovy.json.JsonSlurper().parseText(result) as Map<String, Any>
    val packages = json["packages"] as List<Map<String, Any>>
    val pkg = packages.first { it["name"] == "rustls-platform-verifier-android" }
    val manifestPath = File(pkg["manifest_path"] as String)
    return File(manifestPath.parentFile, "maven").path
}


rootProject.name = "OpacityAndroid"
include(":app")
include(":OpacityCore")
