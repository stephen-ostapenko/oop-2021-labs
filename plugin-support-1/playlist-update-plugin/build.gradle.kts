plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":"))
    implementation("org.junit.jupiter:junit-jupiter:5.4.2")

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.7.0")
    testImplementation(project(":"))

    compileOnly(rootProject)
}

tasks.withType<Test>().all {
    useJUnitPlatform()
}