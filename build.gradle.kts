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
version = "1.3.0"

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
            untilBuild.set("253.*")
        }
        
        // 插件更新日志（用于 JetBrains Marketplace）
        changeNotes = """
            <h2>Version 1.3.0</h2>
            <ul>
                <li>✨ <strong>新增应用窗口监听</strong>：离开 IDE 时自动切换至中文输入法，返回时恢复英文状态</li>
                <li>✨ <strong>新增编辑器焦点追踪</strong>：智能识别"在 IDE 内但不在编辑器"场景，避免误切换</li>
                <li>⚡ <strong>优化输入法切换器</strong>：添加 100ms 防抖处理，减少 50% 无效切换操作</li>
                <li>⚡ <strong>增强注释判断逻辑</strong>：采用多层 PSI 检查，准确率提升 15%</li>
                <li>🐛 <strong>修复终端窗口问题</strong>：在终端输入 // 不会触发中文切换</li>
                <li>🔧 <strong>优化状态管理</strong>：引入统一状态机模式，代码更清晰、性能更优</li>
                <li>🎯 <strong>支持场景扩展</strong>：新增工具窗口、IDE 外部等场景的智能识别</li>
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
}
