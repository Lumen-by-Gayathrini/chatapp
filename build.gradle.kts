// Top-level build file where you can add configuration options common to all sub-projects/modules.
// Note: AGP 9+ provides built-in Kotlin support, so the standalone `org.jetbrains.kotlin.android`
// plugin is intentionally NOT applied.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
