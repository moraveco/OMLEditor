import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.net.URI

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvm("desktop")
    
    sourceSets {
        val desktopMain by getting
        
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(compose.materialIconsExtended)
            implementation("com.neoutils.highlight:highlight-view:2.2.0")
            implementation("com.fifesoft:rsyntaxtextarea:3.3.2")
            implementation("com.fifesoft:autocomplete:3.3.2")
            implementation("com.github.weisj:darklaf-extensions-kotlin:0.1.0")
            implementation("com.russhwolf:multiplatform-settings:1.1.1")

            // For highlighting in Compose
            implementation("com.neoutils.highlight:highlight-compose:2.2.0")
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)

        }
    }
}


compose.desktop {
    application {
        mainClass = "org.moraveco.omleditor.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "org.moraveco.omleditor"
            packageVersion = "1.0.0"
        }
    }
}
