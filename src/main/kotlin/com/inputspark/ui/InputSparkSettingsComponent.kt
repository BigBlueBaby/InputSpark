package com.inputspark.ui

import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import java.awt.Color
import javax.swing.JColorChooser
import javax.swing.JPanel

/**
 * 设置页面 UI 组件
 *
 * @author 林龙祥
 * @since 2026-02-04
 */
class InputSparkSettingsComponent {
    
    val panel: JPanel
    val enabledCheckBox = JBCheckBox("启用 InputSpark 插件")
    val defaultSceneCheckBox = JBCheckBox("默认场景（代码区域）自动切换为英文")
    val commentSceneCheckBox = JBCheckBox("注释场景自动切换为中文")
    val stringLiteralSceneCheckBox = JBCheckBox("字符串字面量场景自动切换")
    val gitCommitSceneCheckBox = JBCheckBox("Git 提交场景自动切换为中文")
    
    // 测试按钮
    
    // 简化处理，实际应该使用 ColorPanel
    // 这里仅作为演示结构
    val englishColorLabel = JBLabel("英文输入法光标颜色 (RGB):")
    // 实际开发中需要更复杂的颜色选择器集成
    
    init {
        // 创建测试按钮面板
        
        panel = FormBuilder.createFormBuilder()
            .addComponent(enabledCheckBox)
            .addSeparator()
            .addComponent(JBLabel("场景开关配置:"))
            .addComponent(defaultSceneCheckBox)
            .addComponent(commentSceneCheckBox)
            .addComponent(stringLiteralSceneCheckBox)
            .addComponent(gitCommitSceneCheckBox)
            .addSeparator()
            .addComponent(JBLabel("光标颜色配置 (需重启 IDE 生效):"))
            // .addComponent(englishColorLabel) 
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }
    
    fun getPreferredFocusedComponent() = enabledCheckBox
    
    // Getters and Setters for binding
    var isEnabled: Boolean
        get() = enabledCheckBox.isSelected
        set(value) { enabledCheckBox.isSelected = value }
        
    var isDefaultSceneEnabled: Boolean
        get() = defaultSceneCheckBox.isSelected
        set(value) { defaultSceneCheckBox.isSelected = value }
        
    var isCommentSceneEnabled: Boolean
        get() = commentSceneCheckBox.isSelected
        set(value) { commentSceneCheckBox.isSelected = value }

    var isStringLiteralSceneEnabled: Boolean
        get() = stringLiteralSceneCheckBox.isSelected
        set(value) { stringLiteralSceneCheckBox.isSelected = value }
        
    var isGitCommitSceneEnabled: Boolean
        get() = gitCommitSceneCheckBox.isSelected
        set(value) { gitCommitSceneCheckBox.isSelected = value }
}
