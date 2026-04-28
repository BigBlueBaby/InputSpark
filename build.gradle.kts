import java.util.Properties

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.21"
    id("org.jetbrains.intellij.platform") version "2.1.0"
}

// 读取本地配置文件
val localProperties = Properties()
val localPropertiesFile = file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

group = "com.inputspark"
version = "1.5.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2023.3.2")
        instrumentationTools()
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild.set("233")
            untilBuild.set(provider { null })
        }
        
        // 插件更新日志（用于 JetBrains Marketplace）
        changeNotes = """
            <h2>Version 1.5.0</h2>
            <ul>
                <li>✨ <strong>新增切换热键配置</strong>：支持在设置页中录入或一键选择中英文切换按键，兼容单键与组合键</li>
                <li>🛠 <strong>修复 Space 组合键录入</strong>：支持正确读取 Ctrl+Space、Shift+Space 等包含 Space 的快捷键</li>
                <li>🎛 <strong>新增常用快捷配置按钮</strong>：支持一键选择 Shift、Ctrl+Space、Ctrl+Shift、Ctrl 等常用方案</li>
                <li>🔔 <strong>新增失败引导提示</strong>：当输入法切换未生效时，在右下角提醒用户检查切换热键配置</li>
            </ul>
        """.trimIndent()
    }
    
    // 插件发布配置
    publishing {
        token.set(localProperties.getProperty("JETBRAINS_TOKEN"))
    }
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }
    named("buildSearchableOptions") {
        enabled = false
    }
}
