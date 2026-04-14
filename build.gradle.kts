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
version = "1.4.0"

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
            <h2>Version 1.4.0</h2>
            <ul>
                <li>✨ <strong>新增气泡提示开关</strong>：支持在设置页中一键关闭输入法切换气泡提示</li>
                <li>🐛 <strong>修复拖拽多选误触发</strong>：鼠标拖拽或 Shift 扩选代码时，不再误触发输入法切换导致 IDEA 搜索弹出</li>
                <li>🔧 <strong>优化启动期监听</strong>：构建与无头环境下跳过编辑器监听初始化，降低启动期副作用</li>
                <li>🚀 <strong>放开 IDE 版本上限</strong>：移除 until-build 限制，支持在更新分支的 IntelliJ IDEA 中安装使用</li>
                <li>🛠 <strong>优化构建流程</strong>：禁用易导致构建失败的 searchable options 生成步骤，提升打包稳定性</li>
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
