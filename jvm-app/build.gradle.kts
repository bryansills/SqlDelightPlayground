plugins {
    kotlin("jvm")
    application
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

application {
    mainClass = "MainKt"
}

dependencies {
    implementation(projects.database)
    implementation(libs.coroutines)
    implementation(libs.kotlinx.datetime)
}