plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://jogamp.org/deployment/maven")
    }
    // oscP5 のためにこのリポジトリを追加
    maven { url = uri("https://repo.clojars.org/") }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("org.processing:core:4.3.4")
    // oscP5 (バージョン 0.9.8 は 2014年のもの)
    implementation("de.sojamo:oscp5:0.9.8")
}

tasks.test {
    useJUnitPlatform()
}