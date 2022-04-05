import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.30"
}
group = "ru.senin.kotlin.net"

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.10.2")

    val ktor_version: String by project
    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
    implementation(kotlin("stdlib-jdk8"))
}
repositories {
    mavenCentral()
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}