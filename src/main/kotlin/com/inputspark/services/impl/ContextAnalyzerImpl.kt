package com.inputspark.services.impl

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
        if (isInGitCommit(editor)) {
            return ContextType.GIT_COMMIT_MESSAGE
        }

        val project = editor.project ?: return ContextType.CODE_DEFAULT
        val document = editor.document
        
        // 策略0: 优先使用简单的文本正则判断 (处理 PSI 延迟问题)
        // 检查当前行是否以 // 开头 (针对 Java/Kotlin/C 等 C-style 语言)
        // 注意：这只是一个 heuristic，并不完美，但对于 //ces 这种场景非常有效
        if (isLineCommentByText(document, offset)) {
            return ContextType.COMMENT_LINE
        }

        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document) ?: return ContextType.CODE_DEFAULT
        
        // 策略1: 优先检查 offset 处的元素
        var element = psiFile.findElementAt(offset)
        
        // 策略2: 如果是 null 或者是空白，且 offset > 0，检查前一个字符
        // (这是关键：在输入时，光标通常在内容之后，而那个位置可能是空白或 EOF)
        if (element == null || element is PsiWhiteSpace) {
            val prevElement = psiFile.findElementAt(offset - 1)
            if (prevElement != null) {
                element = prevElement
            }
        }
        
        if (element != null) {
            if (isInComment(element)) {
                return ContextType.COMMENT_LINE
            }
            if (isInStringLiteral(element)) {
                return ContextType.STRING_LITERAL
            }
        }

        return ContextType.CODE_DEFAULT
    }

    override fun isInComment(element: PsiElement): Boolean {
        return PsiTreeUtil.getParentOfType(element, PsiComment::class.java) != null
    }

    override fun isInStringLiteral(element: PsiElement): Boolean {
        // 简单判断，实际上可能需要针对不同语言进行处理
        return PsiTreeUtil.getParentOfType(element, PsiLiteralValue::class.java) != null
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
