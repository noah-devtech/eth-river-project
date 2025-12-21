plugins {
    id("java")
    id("application")
}

group = "org.example"
version = "1.0-SNAPSHOT"
val os = "windows"
val arch = "amd64"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://jogamp.org/deployment/maven")
    }
    // oscP5 のためにこのリポジトリを追加
    maven { url = uri("https://repo.clojars.org/") }
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("org.processing:core:4.3.4")
    // oscP5 (バージョン 0.9.8 は 2014年のもの)
    implementation("de.sojamo:oscp5:0.9.8")
    implementation("com.github.seancfoley:ipaddress:5.5.1")

    // JOGLのネイティブライブラリ設定（Windows-amd64用）
    implementation("org.jogamp.gluegen:gluegen-rt:2.5.0:natives-$os-$arch")
    implementation("org.jogamp.jogl:jogl-all:2.5.0:natives-$os-$arch")
    implementation("org.processing:core:4.3.4")
}

// JOGLのネイティブライブラリをダウンロード設定（URLはOSやアーキテクチャによって変わる）
val joglNativeLibraryUrl =
    "https://jogamp.org/deployment/autobuilds/master/last/jogl-b1548/jogl-2.6-b1548-20250831-windows-amd64.7z"
val joglNativesDir = layout.buildDirectory.dir("jogl-natives")
val joglNativesArchive = layout.buildDirectory.file("jogl-natives.7z")
val joglExtractDir = layout.buildDirectory.dir("jogl-extract")

// JOGLのネイティブライブラリをダウンロードするタスク（curlコマンドに依存）
val downloadJoglArchive by tasks.registering(Exec::class) {
    description = "Download JOGL native library archive"
    group = "build setup"

    val archiveFile = joglNativesArchive.get().asFile

    inputs.property("url", joglNativeLibraryUrl)
    outputs.file(archiveFile)

    commandLine("curl", "-L", "-o", archiveFile.absolutePath, joglNativeLibraryUrl)

    onlyIf { !archiveFile.exists() }
}

// JOGLのネイティブライブラリを展開するタスク（7zコマンドに依存）
val extractJoglArchive by tasks.registering(Exec::class) {
    description = "Extract JOGL native library archive"
    group = "build setup"

    dependsOn(downloadJoglArchive)

    val archiveFile = joglNativesArchive.get().asFile
    val extractDir = joglExtractDir.get().asFile

    inputs.file(archiveFile)
    outputs.dir(extractDir)

    commandLine("7z", "x", archiveFile.absolutePath, "-o${extractDir.absolutePath}", "-y")

    doLast {
        archiveFile.delete()
    }

    onlyIf { !extractDir.exists() }
}

// JOGLのネイティブライブラリをコピーするタスク
val copyJoglNatives by tasks.registering(Copy::class) {
    description = "Copy JOGL native libraries to build directory"
    group = "build setup"

    dependsOn(extractJoglArchive)

    from(joglExtractDir.map { it.dir("jogl-2.6-b1548-20250831-$os-$arch/lib") })
    into(joglNativesDir)
}

tasks.named<JavaExec>("run") {
    dependsOn(copyJoglNatives) // copyJoglNativesタスクに依存させる
    systemProperty("java.library.path", joglNativesDir.get().asFile.absolutePath)
}

application {
    mainClass.set("Main")
    applicationDefaultJvmArgs =
        listOf("--add-exports=java.desktop/sun.awt=ALL-UNNAMED", "--add-opens=java.desktop/sun.awt=ALL-UNNAMED")
}

tasks.test {
    useJUnitPlatform()
}