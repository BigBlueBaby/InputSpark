package com.inputspark.vim

import com.intellij.openapi.editor.Editor

/**
 * VIM 模式监听器接口
 * 用于检测 IdeaVIM 的模式变化（NORMAL, INSERT, VISUAL 等）
 * 
 * 注意：这是一个占位接口，需要安装 IdeaVIM 插件后才能生效
 * 未来版本将实现完整的 VIM 模式监听和输入法切换逻辑
 *
 * @author 林龙祥
 * @since 2026-02-28
 */
interface VimModeListener {
    
    /**
     * VIM 模式变化回调
     * 
     * @param editor 当前编辑器
     * @param mode 新的 VIM 模式（NORMAL, INSERT, VISUAL 等）
     * @param isInsertMode 是否为插入模式
     */
    fun onModeChanged(editor: Editor, mode: String, isInsertMode: Boolean)
    
    companion object {
        /**
         * VIM 模式常量
         */
        const val MODE_NORMAL = "NORMAL"
        const val MODE_INSERT = "INSERT"
        const val MODE_VISUAL = "VISUAL"
        const val MODE_VISUAL_LINE = "VISUAL LINE"
        const val MODE_VISUAL_BLOCK = "VISUAL BLOCK"
        const val MODE_REPLACE = "REPLACE"
    }
}