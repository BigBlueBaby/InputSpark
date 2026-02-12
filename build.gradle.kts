plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.21"
    id("org.jetbrains.intellij.platform") version "2.1.0"
}

group = "com.inputspark"
version = "1.0.0"

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
    
    implementation(kotlin("stdlib")) // 显式声明，有时候能解决冲突警告
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild.set("233")
            untilBuild.set("253.*")
        }
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
