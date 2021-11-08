import java.io.File

plugins {
    kotlin("js") version "1.5.10"
}

group = "us.mastriel"
version = "1.1.7"

repositories {
    mavenCentral()

}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:1.5.2")
    implementation("commons-net:commons-net:3.3")
}


kotlin {
    js {
        binaries.executable()
        browser {
            commonWebpackConfig {
                cssSupport.enabled = true
            }
        }
    }
}
val buildNumFile = File(buildDir, "inc_counter")
val match = "https://*.youtube.com/*"
val author = "Mastriel#0085"
fun output(release: ReleaseType) = when (release) {
    ReleaseType.RELEASE -> File(buildDir, "userscript/ytildl.user.js")
    ReleaseType.CANARY -> File(buildDir, "userscript/ytildl.canary.user.js")
    ReleaseType.NIGHTLY -> File(buildDir, "userscript/ytildl.nightly.user.js")
}
fun updateURL(release: ReleaseType) = when (release) {
    ReleaseType.RELEASE -> "https://cdn.mastriel.xyz/ytildl.user.js"
    ReleaseType.CANARY -> "https://cdn.mastriel.xyz/ytildl.canary.user.js"
    ReleaseType.NIGHTLY -> "https://cdn.mastriel.xyz/ytildl.nightly.user.js"
}

val release : String? by project

tasks {
    register("generateUserscript") {
        group = "us.mastriel"
        description = "Adds the userscript preamble."
        dependsOn("browserWebpack")
        var releaseType : ReleaseType = ReleaseType.RELEASE
        if (project.hasProperty("release")) {
            releaseType = ReleaseType.of(release!!) ?: throw Exception("Invalid release type given.")
        }
        println("Release type is $releaseType.")

        doLast {
            println("Generating a userscript...")
            incrementVersion(releaseType)
            val lcName = project.name.toLowerCase()

            val bundledJavascript = File(buildDir, "distributions/${project.name}.js").readText()

            val nameAppend =
                if (releaseType != ReleaseType.RELEASE)
                    "-" + releaseType.name.toLowerCase()
                else ""

            val userscriptContents = userscript(lcName+nameAppend, releaseType, bundledJavascript, getVersion(releaseType))

            with(output(releaseType)) {
                parentFile.mkdirs()
                createNewFile()
                writeText(userscriptContents.replace("//# sourceMappingURL=YTILDL.js.map", ""))
            }

        }
    }
}

fun userscriptProperty(name: String, value: String) =
    "// @$name $value"


fun userscript(name: String, release: ReleaseType, js: String, version: String) =
    """
        |// ==UserScript==
        |${userscriptProperty("name", name)}
        |${userscriptProperty("namespace", project.group.toString())}
        |${userscriptProperty("author", author)}
        |${userscriptProperty("version", version)}
        |${userscriptProperty("updateURL", updateURL(release))}
        |${userscriptProperty("description", project.description ?: "A userscript designed to download YouTube videos directly from the YouTube window in the highest quality possible.")}
        |${userscriptProperty("match", match)}
        |// Why is this code obfuscated? It was written in Kotlin.JS, a library for the programming language Kotlin that allows it to transpile into JavaScript. Unfortunately, this mangles the code that it produces, making it nearly impossible for humans to read.
        |// ==/UserScript==
        |(function() {
        |    $js
        |})();    
    """.trimMargin()

enum class ReleaseType {
    RELEASE,
    CANARY,
    NIGHTLY;

    companion object {
        fun of(s: String) : ReleaseType? {
            values().forEach {
                if (s.equals(it.name, ignoreCase = true)) {
                    return it
                }
            }
            return null
        }
    }
}

fun getVersion(release: ReleaseType) : String {
    if (release == ReleaseType.NIGHTLY) {
        buildNumFile.createNewFile()
        // divide by 2 to adjust for the double incrementing, as commented below.
        val buildNum = try { buildNumFile.readText().toInt() } catch (_: NumberFormatException) { 0 } / 2
        return project.version.toString() + ".${buildNum}"
    } else {
        return project.version.toString()
    }
}

// for some reason, this double increments. no clue why.
fun incrementVersion(release: ReleaseType) {
    if (release == ReleaseType.NIGHTLY) {
        buildNumFile.createNewFile()
        val buildNum = try { buildNumFile.readText().toInt() } catch (_: NumberFormatException) { 0 }
        println(buildNum)

        println(buildNum+1)



        buildNumFile.writeText((buildNum+1).toString())
    }
}