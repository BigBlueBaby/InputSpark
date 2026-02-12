package com.inputspark.services

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.inputspark.model.ContextType

/**
 * 上下文分析接口
 * 用于分析当前光标位置的代码上下文环境
 *
 * @author 林龙祥
 * @since 2026-02-04
 */
interface ContextAnalyzer {
    
    /**
     * 分析-上下文
     * 根据编辑器和偏移量分析当前的上下文类型
     *
     * @param editor 编辑器实例
     * @param offset 光标偏移量
     * @return 上下文类型
     */
    fun analyzeContext(editor: Editor, offset: Int): ContextType
    
    /**
     * 判断-是否在注释中
     * 检查给定的 PSI 元素是否处于注释区域
     *
     * @param element PSI 元素
     * @return 是否在注释中
     */
    fun isInComment(element: PsiElement): Boolean
    
    /**
     * 判断-是否在字符串字面量中
     * 检查给定的 PSI 元素是否处于字符串字面量区域
     *
     * @param element PSI 元素
     * @return 是否在字符串中
     */
    fun isInStringLiteral(element: PsiElement): Boolean
    
    /**
     * 判断-是否在Git提交窗口
     * 检查当前编辑器是否属于 Git 提交消息窗口
     *
     * @param editor 编辑器实例
     * @return 是否在 Git 提交窗口
     */
    fun isInGitCommit(editor: Editor): Boolean
}
