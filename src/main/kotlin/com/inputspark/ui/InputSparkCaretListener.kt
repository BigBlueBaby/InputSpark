package com.inputspark.ui

import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import java.awt.Point
import javax.swing.SwingUtilities
import com.inputspark.model.ContextType
import com.inputspark.model.PluginConfig
import com.inputspark.model.SceneType
import com.inputspark.services.ConfigurationManager
import com.inputspark.services.ContextAnalyzer
import com.inputspark.services.InputMethodSwitcher

/**
 * InputSpark 核心光标监听器
 * 负责处理光标移动事件，分析上下文并触发输入法切换
 *
 * @author 林龙祥
 * @since 2026-02-12
 */
class InputSparkCaretListener : CaretListener {
    private var lastContextType: ContextType? = null
    private var lastLine = -1
    private var lastEventTime = 0L

    override fun caretPositionChanged(e: CaretEvent) {
        val now = System.currentTimeMillis()
        // 简单的防抖，避免极短时间内的微小移动触发多次计算
        if (now - lastEventTime < 50) return
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

        // 3. 同行防抖动逻辑 (核心：防止在输入注释时被强制切回)
        if (currentLine == lastLine) {
            // 如果上下文没有改变，直接返回，不做任何操作
            // 这意味着：如果用户在注释里手动切回了英文，只要上下文判定还是注释，我们就不会再次触发切换中文
            if (contextType == lastContextType) return
            
            // 特殊处理：如果之前是注释，现在判定为代码（可能是输入延迟导致的 PSI 滞后），
            // 但文本上看起来还是注释，则忽略这次变化，保持在注释状态
            if (lastContextType == ContextType.COMMENT_LINE && contextType == ContextType.CODE_DEFAULT) {
                if (isLineCommentByText(document, offset)) {
                    return 
                }
            }
        }
        
        lastLine = currentLine
        
        // 4. 更新状态
        if (contextType == lastContextType) return
        lastContextType = contextType
        
        // 5. 执行切换
        val switcher = service<InputMethodSwitcher>()
        var switched = false
        var tip = ""

        when (contextType) {
            ContextType.COMMENT_LINE, ContextType.COMMENT_BLOCK, ContextType.GIT_COMMIT_MESSAGE -> {
                if (shouldSwitchToChinese(config, contextType)) {
                    // 只有当真正发生了切换动作（返回 true）时，才认为需要提示
                    switched = switcher.switchToChinese()
                    tip = "注释 - 中文"
                }
            }
            ContextType.STRING_LITERAL -> {
                if (config.sceneConfig[SceneType.STRING_LITERAL.name] == true) {
                    switched = switcher.switchToEnglish()
                    tip = "字符串 - 英文"
                }
            }
            else -> { // CODE_DEFAULT
                if (config.sceneConfig[SceneType.DEFAULT.name] == true) {
                    switched = switcher.switchToEnglish()
                    tip = "代码 - 英文"
                }
            }
        }

        // 6. 只有在真正发生了状态改变时才显示提示
        if (switched) {
            showBalloon(editor, tip)
        }
    }
    
    private fun shouldSwitchToChinese(config: PluginConfig, type: ContextType): Boolean {
        if (type == ContextType.GIT_COMMIT_MESSAGE) {
             return config.sceneConfig[SceneType.GIT_COMMIT.name] == true
        }
        return config.sceneConfig[SceneType.COMMENT.name] == true
    }

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

    private fun showBalloon(editor: Editor, text: String) {
        try {
            val factory = JBPopupFactory.getInstance()
            val builder = factory.createHtmlTextBalloonBuilder(text, null, JBColor.GRAY, null)
            builder.setFadeoutTime(1500)
            val balloon = builder.createBalloon()
            val pos = editor.caretModel.visualPosition
            val p = editor.visualPositionToXY(pos)
            // 向上偏移 20 像素
            val rp = RelativePoint(editor.contentComponent, Point(p.x, p.y - 20))
            balloon.show(rp, Balloon.Position.above)
        } catch (e: Exception) {
            // ignore UI exceptions
        }
    }
}
