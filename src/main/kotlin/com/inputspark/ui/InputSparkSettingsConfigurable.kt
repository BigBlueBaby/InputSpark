package com.inputspark.ui

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.components.service
import com.inputspark.model.InputMethodType
import com.inputspark.model.SceneType
import com.inputspark.services.ConfigurationManager
import javax.swing.JComponent

/**
 * 设置页面配置器
 * 连接 UI 和配置管理服务
 *
 * @author 林龙祥
 * @since 2026-02-04
 */
class InputSparkSettingsConfigurable : Configurable {
    
    private var settingsComponent: InputSparkSettingsComponent? = null
    
    override fun getDisplayName(): String = "InputSpark"
    
    override fun getPreferredFocusedComponent(): JComponent? {
        return settingsComponent?.getPreferredFocusedComponent()
    }

    override fun createComponent(): JComponent? {
        settingsComponent = InputSparkSettingsComponent()
        return settingsComponent?.panel
    }

    override fun isModified(): Boolean {
        val configManager = service<ConfigurationManager>()
        val config = configManager.getConfig()
        val component = settingsComponent ?: return false
        
        return component.isEnabled != config.enabled ||
               component.isShowBalloonTipEnabled != config.showBalloonTip ||
               component.isDefaultSceneEnabled != (config.sceneConfig[SceneType.DEFAULT.name] ?: true) ||
               component.isCommentSceneEnabled != (config.sceneConfig[SceneType.COMMENT.name] ?: true) ||
               component.isStringLiteralSceneEnabled != (config.sceneConfig[SceneType.STRING_LITERAL.name] ?: false) ||
               component.isGitCommitSceneEnabled != (config.sceneConfig[SceneType.GIT_COMMIT.name] ?: true) ||
               component.englishColor != (config.cursorColors[InputMethodType.ENGLISH.name] ?: "#4CAF50") ||
               component.chineseColor != (config.cursorColors[InputMethodType.CHINESE.name] ?: "#F44336")
    }

    override fun apply() {
        val configManager = service<ConfigurationManager>()
        val config = configManager.getConfig()
        val component = settingsComponent ?: return
        
        config.enabled = component.isEnabled
        config.showBalloonTip = component.isShowBalloonTipEnabled
        
        // 更新场景配置
        config.sceneConfig[SceneType.DEFAULT.name] = component.isDefaultSceneEnabled
        config.sceneConfig[SceneType.COMMENT.name] = component.isCommentSceneEnabled
        config.sceneConfig[SceneType.STRING_LITERAL.name] = component.isStringLiteralSceneEnabled
        config.sceneConfig[SceneType.GIT_COMMIT.name] = component.isGitCommitSceneEnabled
        
        // 更新颜色配置
        config.cursorColors[InputMethodType.ENGLISH.name] = component.englishColor
        config.cursorColors[InputMethodType.CHINESE.name] = component.chineseColor
        
        // configManager.saveConfiguration(config) // 实际上直接修改对象引用即可，IntelliJ 会自动检测状态变化并保存
    }

    override fun reset() {
        val configManager = service<ConfigurationManager>()
        val config = configManager.getConfig()
        val component = settingsComponent ?: return
        
        component.isEnabled = config.enabled
        component.isShowBalloonTipEnabled = config.showBalloonTip
        component.isDefaultSceneEnabled = config.sceneConfig[SceneType.DEFAULT.name] ?: true
        component.isCommentSceneEnabled = config.sceneConfig[SceneType.COMMENT.name] ?: true
        component.isStringLiteralSceneEnabled = config.sceneConfig[SceneType.STRING_LITERAL.name] ?: false
        component.isGitCommitSceneEnabled = config.sceneConfig[SceneType.GIT_COMMIT.name] ?: true
        
        // 重置颜色配置
        component.englishColor = config.cursorColors[InputMethodType.ENGLISH.name] ?: "#4CAF50"
        component.chineseColor = config.cursorColors[InputMethodType.CHINESE.name] ?: "#F44336"
        
        // 更新颜色预览
        component.updateColorPreviews()
    }

    override fun disposeUIResources() {
        settingsComponent = null
    }
}
