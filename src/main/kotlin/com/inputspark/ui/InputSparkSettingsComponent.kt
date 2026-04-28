package com.inputspark.ui

import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.FormBuilder
import com.intellij.ui.components.JBTextField
import com.inputspark.core.windows.InputMethodToggleHotkeyParser
import java.awt.Color
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton
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
    val showBalloonTipCheckBox = JBCheckBox("显示输入法切换气泡提示")
    val toggleHotkeyLabel = JBLabel("中英文切换按键:")
    val toggleHotkeyField = InputMethodHotkeyField(18)
    val toggleHotkeyHintLabel = JBLabel("点击输入框后直接按下快捷键，常用配置 Shift、Ctrl+Space、Ctrl+Shift、 Ctrl")
    val toggleHotkeyPresetPanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, 8, 0))
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
        setupHotkeyPresetButtons()
        
        panel = FormBuilder.createFormBuilder()
            .addComponent(enabledCheckBox)
            .addComponent(showBalloonTipCheckBox)
            .addLabeledComponent(toggleHotkeyLabel, toggleHotkeyField)
            .addComponent(toggleHotkeyHintLabel)
            .addComponent(toggleHotkeyPresetPanel)
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

    /**
     * 设置常用热键快捷配置按钮
     */
    private fun setupHotkeyPresetButtons() {
        toggleHotkeyPresetPanel.removeAll()
        toggleHotkeyPresetPanel.add(createHotkeyPresetButton("Shift", "Shift"))
        toggleHotkeyPresetPanel.add(createHotkeyPresetButton("Ctrl+Space", "Ctrl + Space"))
        toggleHotkeyPresetPanel.add(createHotkeyPresetButton("Ctrl+Shift", "Ctrl + Shift"))
        toggleHotkeyPresetPanel.add(createHotkeyPresetButton("Ctrl", "Ctrl"))
    }

    /**
     * 创建常用热键配置按钮
     */
    private fun createHotkeyPresetButton(buttonText: String, hotkeyText: String): JButton {
        val presetButton = JButton(buttonText)
        presetButton.addActionListener {
            toggleHotkeyField.text = InputMethodToggleHotkeyParser.parse(hotkeyText).displayText
            toggleHotkeyField.requestFocusInWindow()
            toggleHotkeyField.selectAll()
        }
        return presetButton
    }
    
    fun getPreferredFocusedComponent() = enabledCheckBox
    
    // Getters and Setters for binding
    var isEnabled: Boolean
        get() = enabledCheckBox.isSelected
        set(value) { enabledCheckBox.isSelected = value }
        
    var isDefaultSceneEnabled: Boolean
        get() = defaultSceneCheckBox.isSelected
        set(value) { defaultSceneCheckBox.isSelected = value }

    var isShowBalloonTipEnabled: Boolean
        get() = showBalloonTipCheckBox.isSelected
        set(value) { showBalloonTipCheckBox.isSelected = value }

    var toggleHotkey: String
        get() = toggleHotkeyField.text
        set(value) { toggleHotkeyField.text = value }
        
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
