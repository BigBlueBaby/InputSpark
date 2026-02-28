package com.inputspark.vim

import com.intellij.openapi.editor.Editor
import com.inputspark.model.CaretState
import com.inputspark.model.ContextType

/**
 * VIM 模式检测器（占位实现）
 * 用于在 IdeaVIM 环境下根据模式切换输入法
 * 
 * 设计思路：
 * 1. NORMAL/VISUAL 模式：强制英文输入法
 * 2. INSERT 模式：根据光标位置自动切换（注释用中文，代码用英文）
 * 
 * TODO: 需要集成 IdeaVIM API 实现真正的模式监听
 *
 * @author 林龙祥
 * @since 2026-02-28
 */
class VimModeDetector : VimModeListener {
    
    // 当前是否为 INSERT 模式
    private var isInsertMode = false
    
    /**
     * 根据 VIM 模式计算光标状态
     */
    fun getCaretStateForVimMode(contextType: ContextType, vimMode: String): CaretState {
        return when (vimMode) {
            // NORMAL 模式：强制英文
            VimModeListener.MODE_NORMAL -> CaretState.VIM_NORMAL_MODE
            
            // VISUAL 模式：强制英文
            VimModeListener.MODE_VISUAL,
            VimModeListener.MODE_VISUAL_LINE,
            VimModeListener.MODE_VISUAL_BLOCK -> CaretState.VIM_NORMAL_MODE
            
            // REPLACE 模式：英文
            VimModeListener.MODE_REPLACE -> CaretState.VIM_NORMAL_MODE
            
            // INSERT 模式：根据内容判断
            VimModeListener.MODE_INSERT -> {
                isInsertMode = true
                CaretState.fromContextType(contextType, isVimInsertMode = true)
            }
            
            // 默认：英文
            else -> CaretState.VIM_NORMAL_MODE
        }
    }
    
    override fun onModeChanged(editor: Editor, mode: String, isInsertMode: Boolean) {
        this.isInsertMode = isInsertMode
        // TODO: 触发输入法切换逻辑
        // 这部分需要与 InputSparkCaretListener 集成
    }
}