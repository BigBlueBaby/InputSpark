package com.inputspark.model

/**
 * 光标状态枚举
 * 定义光标在不同场景下的状态及对应的目标输入法
 *
 * @author 林龙祥
 * @since 2026-02-28
 */
enum class CaretState(
    val code: String,
    val targetInputMethod: InputMethodType
) {
    /** 在 IDE 外部（如浏览器、其他应用） */
    OUTSIDE_IDE("OUTSIDE_IDE", InputMethodType.CHINESE),
    
    /** 在注释中（行注释） */
    IN_COMMENT_LINE("IN_COMMENT_LINE", InputMethodType.CHINESE),
    
    /** 在注释中（块注释） */
    IN_COMMENT_BLOCK("IN_COMMENT_BLOCK", InputMethodType.CHINESE),
    
    /** 在字符串字面量中 */
    IN_STRING_LITERAL("IN_STRING_LITERAL", InputMethodType.ENGLISH),
    
    /** 在 Git 提交信息中 */
    IN_GIT_COMMIT("IN_GIT_COMMIT", InputMethodType.CHINESE),
    
    /** 在工具窗口（非编辑器）中 */
    IN_TOOL_WINDOW("IN_TOOL_WINDOW", InputMethodType.ENGLISH),
    
    /** 在编辑器中（代码区域） */
    IN_CODE("IN_CODE", InputMethodType.ENGLISH),
    
    /** VIM NORMAL 模式（强制英文） */
    VIM_NORMAL_MODE("VIM_NORMAL_MODE", InputMethodType.ENGLISH),
    
    /** VIM INSERT 模式（根据内容判断） */
    VIM_INSERT_MODE("VIM_INSERT_MODE", InputMethodType.ENGLISH);
    
    companion object {
        /**
         * 根据上下文类型获取对应的光标状态
         */
        fun fromContextType(contextType: ContextType, isVimInsertMode: Boolean = false): CaretState {
            return when (contextType) {
                ContextType.GIT_COMMIT_MESSAGE -> IN_GIT_COMMIT
                ContextType.TOOL_WINDOW_TERMINAL -> IN_TOOL_WINDOW
                ContextType.COMMENT_LINE -> IN_COMMENT_LINE
                ContextType.COMMENT_BLOCK -> IN_COMMENT_BLOCK
                ContextType.STRING_LITERAL -> IN_STRING_LITERAL
                ContextType.CODE_DEFAULT -> {
                    if (isVimInsertMode) VIM_INSERT_MODE
                    else IN_CODE
                }
                ContextType.TOOL_WINDOW_PROJECT -> IN_TOOL_WINDOW
            }
        }
    }
}