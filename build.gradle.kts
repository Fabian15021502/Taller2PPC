// Archivo de nivel de proyecto

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false

    // Registrar el plugin de Google Services aquí
    id("com.google.gms.google-services") version "4.4.2" apply false
}
