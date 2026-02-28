package com.inputspark.ui

import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import java.awt.Point
import javax.swing.SwingUtilities
import com.inputspark.model.ContextType
import com.inputspark.model.InputMethodType
import com.inputspark.model.PluginConfig
import com.inputspark.model.SceneType
import com.inputspark.model.CaretState
import com.inputspark.services.ConfigurationManager
import com.inputspark.services.ContextAnalyzer
import com.inputspark.services.InputMethodSwitcher
import java.util.function.Consumer

/**
 * InputSpark 核心光标监听器
 * 负责处理光标移动事件，分析上下文并触发输入法切换
 * 基于状态机模式，统一管理光标状态和输入法切换
 *
 * @author 林龙祥
 * @since 2026-02-12
 */
class InputSparkCaretListener : CaretListener {
    // 当前光标状态
    private var currentCaretState: CaretState = CaretState.IN_CODE
    
    // 上次处理的行号
    private var lastLine = -1
    
    // 上次事件时间戳（防抖）
    private var lastEventTime = 0L
    
    // 防抖间隔（毫秒）
    private companion object {
        private const val DEBOUNCE_INTERVAL_MS = 50L
        private const val FOCUS_CHECK_DELAY_MS = 10L
    }

    override fun caretPositionChanged(e: CaretEvent) {
        val now = System.currentTimeMillis()
        // 防抖：避免极短时间内的微小移动触发多次计算
        if (now - lastEventTime < DEBOUNCE_INTERVAL_MS) return
        lastEventTime = now

        SwingUtilities.invokeLater {
            try {
                handleCaretChange(e.editor)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun handleCaretChange(editor: Editor) {
        // 1. 检查配置是否启用
        val configManager = service<ConfigurationManager>()
        val config = configManager.getConfig()
        if (!config.enabled) return

        // 2. 分析上下文
        val contextAnalyzer = service<ContextAnalyzer>()
        val offset = editor.caretModel.offset
        val contextType = contextAnalyzer.analyzeContext(editor, offset)
        
        val document = editor.document
        val currentLine = document.getLineNumber(offset)

        // 3. 同行防抖动逻辑（核心：防止在输入注释时被强制切回）
        if (currentLine == lastLine) {
            // 如果上下文没有改变，直接返回，不做任何操作
            if (contextType == getCurrentContextTypeFromState()) return
            
            // 特殊处理：如果之前是注释，现在判定为代码（可能是 PSI 滞后），
            // 但文本上看起来还是注释，则忽略这次变化，保持在注释状态
            if (currentCaretState == CaretState.IN_COMMENT_LINE && contextType == ContextType.CODE_DEFAULT) {
                if (isLineCommentByText(document, offset)) {
                    return 
                }
            }
        }
        
        lastLine = currentLine
        
        // 4. 计算新的光标状态
        val newCaretState = CaretState.fromContextType(contextType)
        
        // 5. 状态没有变化则不处理
        if (newCaretState == currentCaretState) return
        currentCaretState = newCaretState
        
        // 6. 执行输入法切换
        switchToTargetInputMethod(editor, newCaretState, config)
    }
    
    /**
     * 从当前状态获取对应的上下文类型
     */
    private fun getCurrentContextTypeFromState(): ContextType {
        return when (currentCaretState) {
            CaretState.IN_COMMENT_LINE -> ContextType.COMMENT_LINE
            CaretState.IN_COMMENT_BLOCK -> ContextType.COMMENT_BLOCK
            CaretState.IN_STRING_LITERAL -> ContextType.STRING_LITERAL
            CaretState.IN_GIT_COMMIT -> ContextType.GIT_COMMIT_MESSAGE
            CaretState.IN_TOOL_WINDOW -> ContextType.TOOL_WINDOW_TERMINAL
            CaretState.IN_CODE, CaretState.VIM_NORMAL_MODE, CaretState.VIM_INSERT_MODE -> ContextType.CODE_DEFAULT
            CaretState.OUTSIDE_IDE -> ContextType.CODE_DEFAULT
        }
    }
    
    /**
     * 切换到目标输入法
     */
    private fun switchToTargetInputMethod(editor: Editor, newState: CaretState, config: PluginConfig) {
        val switcher = service<InputMethodSwitcher>()
        var switched = false
        var tip = ""

        when (newState) {
            CaretState.IN_COMMENT_LINE, CaretState.IN_COMMENT_BLOCK, CaretState.IN_GIT_COMMIT -> {
                if (shouldSwitchToChinese(config, newState)) {
                    switched = switcher.switchToChinese()
                    tip = getTipText(newState)
                }
            }
            CaretState.IN_STRING_LITERAL -> {
                if (config.sceneConfig[SceneType.STRING_LITERAL.name] == true) {
                    switched = switcher.switchToEnglish()
                    tip = getTipText(newState)
                }
            }
            CaretState.IN_TOOL_WINDOW -> {
                // 工具窗口中不进行切换
                return
            }
            CaretState.IN_CODE, CaretState.VIM_NORMAL_MODE, CaretState.VIM_INSERT_MODE -> {
                if (config.sceneConfig[SceneType.DEFAULT.name] == true) {
                    switched = switcher.switchToEnglish()
                    tip = getTipText(newState)
                }
            }
            CaretState.OUTSIDE_IDE -> {
                // IDE 外部切换到中文
                switched = switcher.switchToChinese()
                tip = "离开 IDE - 中文"
            }
        }

        // 7. 只有在真正发生了状态改变时才显示提示和设置光标颜色
        if (switched) {
            setCursorColor(editor, newState.targetInputMethod)
            showBalloon(editor, tip)
        }
    }
    
    /**
     * 判断是否应该切换到中文输入法
     */
    private fun shouldSwitchToChinese(config: PluginConfig, state: CaretState): Boolean {
        if (state == CaretState.IN_GIT_COMMIT) {
             return config.sceneConfig[SceneType.GIT_COMMIT.name] == true
        }
        return config.sceneConfig[SceneType.COMMENT.name] == true
    }
    
    /**
     * 获取提示文本
     */
    private fun getTipText(state: CaretState): String {
        return when (state) {
            CaretState.IN_COMMENT_LINE, CaretState.IN_COMMENT_BLOCK -> "注释 - 中文"
            CaretState.IN_STRING_LITERAL -> "字符串 - 英文"
            CaretState.IN_GIT_COMMIT -> "提交信息 - 中文"
            CaretState.IN_CODE, CaretState.VIM_NORMAL_MODE, CaretState.VIM_INSERT_MODE -> "代码 - 英文"
            CaretState.OUTSIDE_IDE -> "离开 IDE - 中文"
            CaretState.IN_TOOL_WINDOW -> "工具窗口 - 英文"
        }
    }

    /**
     * 通过文本判断是否为注释行
     */
    private fun isLineCommentByText(document: com.intellij.openapi.editor.Document, offset: Int): Boolean {
         try {
             if (offset < 0 || offset > document.textLength) return false
             val line = document.getLineNumber(offset)
             val start = document.getLineStartOffset(line)
             val end = document.getLineEndOffset(line)
             val text = document.getText(com.intellij.openapi.util.TextRange(start, end))
             val idx = text.indexOf("//")
             return idx != -1 && (offset - start) >= idx
         } catch(e: Exception) { return false }
    }

    /**
     * 显示提示气泡
     */
    private fun showBalloon(editor: Editor, text: String) {
        try {
            val factory = JBPopupFactory.getInstance()
            // 根据主题自动切换背景色：亮色主题用白色，暗色主题用指定的深灰色
            val backgroundColor = JBColor(java.awt.Color(255, 255, 255), java.awt.Color(38, 40, 43))
            val builder = factory.createHtmlTextBalloonBuilder(text, null, backgroundColor, null)
            builder.setFadeoutTime(1500)
            val balloon = builder.createBalloon()
            val pos = editor.caretModel.visualPosition
            val p = editor.visualPositionToXY(pos)
            // 向上偏移 5 像素，使提示框更接近光标正上方
            val rp = RelativePoint(editor.contentComponent, Point(p.x, p.y - 5))
            balloon.show(rp, Balloon.Position.above)
        } catch (e: Exception) {
            // ignore UI exceptions
        }
    }
    
    /**
     * 根据输入法类型设置光标颜色
     */
    private fun setCursorColor(editor: Editor, inputMethodType: InputMethodType) {
        try {
            val configManager = service<ConfigurationManager>()
            val colorConfig = configManager.getCursorColorConfig()
            val color = colorConfig[inputMethodType] ?: java.awt.Color.BLACK
            
            // 尝试使用不同的 API 方法设置光标颜色
            try {
                // 方法 1: 直接设置光标颜色
                val settings = editor.settings
                val method = settings.javaClass.getMethod("setCursorColor", java.awt.Color::class.java)
                method.invoke(settings, color)
            } catch (e: Exception) {
                // 方法 1 失败，尝试方法 2
                try {
                    // 方法 2: 设置自定义光标颜色
                    val settings = editor.settings
                    val method = settings.javaClass.getMethod("setCustomCursorColor", java.awt.Color::class.java)
                    method.invoke(settings, color)
                } catch (e2: Exception) {
                    // 方法 2 失败，尝试方法 3
                    try {
                        // 方法 3: 通过 EditorColorsManager 设置
                        val colorsManager = com.intellij.openapi.editor.colors.EditorColorsManager.getInstance()
                        val scheme = colorsManager.globalScheme
                        val key = com.intellij.openapi.editor.colors.EditorColors.CARET_COLOR
                        scheme.setColor(key, color)
                    } catch (e3: Exception) {
                        // 所有方法都失败，忽略异常
                    }
                }
            }
        } catch (e: Exception) {
            // ignore UI exceptions
        }
    }
    
    /**
     * 获取当前光标状态（供外部使用）
     */
    fun getCurrentCaretState(): CaretState {
        return currentCaretState
    }
}
