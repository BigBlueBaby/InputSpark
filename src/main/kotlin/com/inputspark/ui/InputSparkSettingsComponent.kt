package com.inputspark.ui

import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import com.intellij.ui.components.JBTextField
import java.awt.Color
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JColorChooser
import javax.swing.JPanel
import javax.swing.border.LineBorder

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
    
    // 光标颜色配置
    val englishColorLabel = JBLabel("英文输入法光标颜色:")
    val englishColorField = JBTextField("#4CAF50", 8)
    val englishColorPreview = JPanel()
    
    val chineseColorLabel = JBLabel("中文输入法光标颜色:")
    val chineseColorField = JBTextField("#F44336", 8)
    val chineseColorPreview = JPanel()
    
    init {
        // 初始化颜色预览面板
        setupColorPreview(englishColorPreview, Color.decode("#4CAF50"))
        setupColorPreview(chineseColorPreview, Color.decode("#F44336"))
        
        // 设置颜色选择器点击事件
        setupColorPicker(englishColorPreview, englishColorField)
        setupColorPicker(chineseColorPreview, chineseColorField)
        
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
            .addLabeledComponent(englishColorLabel, FormBuilder.createFormBuilder()
                .addComponent(englishColorField)
                .addComponent(englishColorPreview)
                .panel)
            .addLabeledComponent(chineseColorLabel, FormBuilder.createFormBuilder()
                .addComponent(chineseColorField)
                .addComponent(chineseColorPreview)
                .panel)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }
    
    /**
     * 设置颜色预览面板
     */
    private fun setupColorPreview(previewPanel: JPanel, color: Color) {
        previewPanel.setSize(30, 30)
        previewPanel.setPreferredSize(java.awt.Dimension(30, 30))
        previewPanel.setBackground(color)
        previewPanel.border = LineBorder(Color.GRAY, 1)
    }
    
    /**
     * 设置颜色选择器
     */
    private fun setupColorPicker(previewPanel: JPanel, colorField: JBTextField) {
        previewPanel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                try {
                    val currentColor = Color.decode(colorField.text)
                    val selectedColor = JColorChooser.showDialog(
                        previewPanel,
                        "选择颜色",
                        currentColor
                    )
                    if (selectedColor != null) {
                        // 转换为十六进制格式
                        val hexColor = String.format("#%02X%02X%02X", 
                            selectedColor.red, 
                            selectedColor.green, 
                            selectedColor.blue)
                        colorField.text = hexColor
                        previewPanel.setBackground(selectedColor)
                    }
                } catch (ex: Exception) {
                    // 忽略颜色解析错误
                }
            }
        })
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
    
    // 颜色配置的 Getters and Setters
    var englishColor: String
        get() = englishColorField.text
        set(value) { englishColorField.text = value }
    
    var chineseColor: String
        get() = chineseColorField.text
        set(value) { chineseColorField.text = value }
    
    /**
     * 更新颜色预览
     */
    fun updateColorPreviews() {
        try {
            setupColorPreview(englishColorPreview, Color.decode(englishColorField.text))
            setupColorPreview(chineseColorPreview, Color.decode(chineseColorField.text))
        } catch (ex: Exception) {
            // 忽略颜色解析错误
        }
    }
}
