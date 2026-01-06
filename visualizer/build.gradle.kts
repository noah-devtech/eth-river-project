plugins {
    id("java")
    id("application")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://jogamp.org/deployment/maven") }
// oscP5 のためにこのリポジトリを追加
    maven { url = uri("https://repo.clojars.org/") }
    maven { url = uri("https://jitpack.io") }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

val processingVersion = "4.4.10"
val joglVersion = "2.5.0"

dependencies {
    implementation("de.sojamo:oscp5:0.9.8")
    implementation("com.github.seancfoley:ipaddress:5.5.1")
    // kotlin-stdlib
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.2.20")
    // Processing Core
    implementation("org.processing:core:$processingVersion")

    // JOGL & GlueGen
    val joglModules = listOf("jogl", "nativewindow", "newt")

    joglModules.forEach { module ->
        implementation("org.jogamp.jogl:$module:$joglVersion")
    }
    implementation("org.jogamp.gluegen:gluegen-rt:$joglVersion")

    // Native libraries for each platform
    val platforms = listOf("windows-amd64", "macosx-universal", "linux-amd64")
    platforms.forEach { platform ->
        joglModules.forEach { module ->
            runtimeOnly("org.jogamp.jogl:$module:$joglVersion:natives-$platform")
        }
        runtimeOnly("org.jogamp.gluegen:gluegen-rt:$joglVersion:natives-$platform")
    }

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

application {
    mainClass.set("Main")
    applicationDefaultJvmArgs =
        listOf(
            "--add-exports=java.desktop/sun.awt=ALL-UNNAMED",
            "--add-opens=java.desktop/sun.awt=ALL-UNNAMED",
            "--add-exports=java.desktop/sun.java2d=ALL-UNNAMED",
            "--add-opens=java.desktop/sun.java2d=ALL-UNNAMED"
        )
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}