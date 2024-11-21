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