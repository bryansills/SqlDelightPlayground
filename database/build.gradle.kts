plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.sqldelight)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    jvm()
    androidTarget {
        publishLibraryVariants("release")
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.coroutines)
            implementation(libs.kotlinx.datetime)
            implementation(libs.paging.common)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
        }
        jvmMain.dependencies {
            implementation(libs.sqldelight.jvm.driver)
        }
    }
}

android {
    namespace = "ninja.bryansills.sqldelightplayground"

    compileSdk = 35
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugaring)
}

sqldelight {
    databases {
        create("Database") {
            packageName = "ninja.bryansills.sqldelightplayground"
            schemaOutputDirectory = file("src/commonMain/sqldelight/databases")
            verifyMigrations = true
            generateAsync = true
            deriveSchemaFromMigrations = true
            dialect(libs.sqldelight.sqlite.dialect)
        }
    }
}
