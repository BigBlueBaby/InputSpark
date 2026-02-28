package com.inputspark.services.impl

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralValue
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.inputspark.model.ContextType
import com.inputspark.services.ContextAnalyzer

/**
 * 上下文分析实现类
 *
 * @author 林龙祥
 * @since 2026-02-04
 */
@Service(Service.Level.APP)
class ContextAnalyzerImpl : ContextAnalyzer {

    override fun analyzeContext(editor: Editor, offset: Int): ContextType {
        // 1. 优先检测 Git 提交
        if (isInGitCommit(editor)) {
            return ContextType.GIT_COMMIT_MESSAGE
        }
        
        val project = editor.project ?: return ContextType.CODE_DEFAULT
        val document = editor.document

        // 2. 使用 PSI 分析（完全依赖 PSI，避免在终端等非代码区域误判）
        return ApplicationManager.getApplication().runReadAction<ContextType> {
            val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document)
            if (psiFile == null) {
                // PSI 文件为 null 时，可能是终端、工具窗口等非代码编辑器
                // 统一返回 CODE_DEFAULT（英文），不触发切换
                return@runReadAction ContextType.CODE_DEFAULT
            }
            
            // 策略 1: 优先检查 offset 处的元素
            var element = psiFile.findElementAt(offset)
            
            // 策略 2: 如果是 null 或者是空白，且 offset > 0，检查前一个字符
            // (这是关键：在输入时，光标通常在内容之后，而那个位置可能是空白或 EOF)
            if (element == null || element is PsiWhiteSpace) {
                val prevElement = psiFile.findElementAt(offset - 1)
                if (prevElement != null) {
                    element = prevElement
                }
            }
            
            if (element != null) {
                // 3. 优先检查是否在注释中（最高优先级）
                if (isInComment(element)) {
                    return@runReadAction ContextType.COMMENT_LINE
                }
                // 4. 检查是否在字符串中
                if (isInStringLiteral(element)) {
                    return@runReadAction ContextType.STRING_LITERAL
                }
            }

            // 5. 其他所有情况（包括终端、工具窗口、普通代码）统一返回 CODE_DEFAULT
            // 这样确保在终端等非代码区域不会触发中文切换
            ContextType.CODE_DEFAULT
        }
    }

    override fun isInComment(element: PsiElement): Boolean {
        return ApplicationManager.getApplication().runReadAction<Boolean> {
            // 检查当前元素是否是注释
            if (element is PsiComment) {
                return@runReadAction true
            }
            
            // 检查前一个位置（处理光标在注释末尾的情况）
            val prevElement = element.prevSibling
            if (prevElement is PsiComment) {
                return@runReadAction true
            }
            
            // 检查父元素（处理嵌套情况）
            var parent = element.parent
            while (parent != null) {
                if (parent is PsiComment) {
                    return@runReadAction true
                }
                parent = parent.parent
            }
            
            // 使用 PSI 工具类检查
            PsiTreeUtil.getParentOfType(element, PsiComment::class.java) != null
        }
    }

    override fun isInStringLiteral(element: PsiElement): Boolean {
        return ApplicationManager.getApplication().runReadAction<Boolean> {
            // 简单判断，实际上可能需要针对不同语言进行处理
            PsiTreeUtil.getParentOfType(element, PsiLiteralValue::class.java) != null
        }
    }

    override fun isInGitCommit(editor: Editor): Boolean {
        // 通过 Document 的 UserData 或文件类型判断
        // 这里简单通过文件类型判断（需要更严谨的判断）
        val fileType = editor.virtualFile?.fileType?.name
        return fileType == "PLAIN_TEXT" && editor.virtualFile?.name?.contains("COMMIT_EDITMSG") == true
    }

    private fun isLineCommentByText(document: com.intellij.openapi.editor.Document, offset: Int): Boolean {
        if (offset < 0 || offset > document.textLength) return false
        val line = document.getLineNumber(offset)
        val start = document.getLineStartOffset(line)
        val end = document.getLineEndOffset(line)
        val text = document.getText(com.intellij.openapi.util.TextRange(start, end))
        
        // 检查双斜杠的位置
        val commentIndex = text.indexOf("//")
        if (commentIndex == -1) return false
        
        // 检查光标是否在双斜杠之后
        val offsetInLine = offset - start
        return offsetInLine >= commentIndex
    }
}
