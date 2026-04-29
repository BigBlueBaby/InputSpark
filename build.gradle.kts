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
version = "1.5.1"

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
            <h2>Version 1.5.1</h2>
            <ul>
                <li>🛠 <strong>修复 ALT+TAB 误触发切换</strong>：避免在 IDEA 窗口来回切换时误按中英文切换热键</li>
                <li>🔔 <strong>修复错误失败提示</strong>：避免窗口激活阶段因焦点未稳定而弹出“切换到英文失败”提醒</li>
                <li>⚙ <strong>优化输入法恢复策略</strong>：IDE 重新激活后按真实编辑器上下文恢复输入法，不再一律强制切回英文</li>
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
