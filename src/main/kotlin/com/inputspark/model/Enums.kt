package com.inputspark.model

/**
 * 输入法类型枚举
 * 定义支持的输入法类型
 *
 * @author 林龙祥
 * @since 2026-02-04
 */
enum class InputMethodType {
    /** 英文输入法 */
    ENGLISH,
    /** 中文输入法 */
    CHINESE,
    /** 日文输入法 */
    JAPANESE,
    /** 韩文输入法 */
    KOREAN
}

/**
 * 场景类型枚举
 * 定义插件支持的各种自动切换场景
 *
 * @author 林龙祥
 * @since 2026-02-04
 */
enum class SceneType {
    /** 默认场景（代码区域） */
    DEFAULT,
    /** 注释场景 */
    COMMENT,
    /** 字符串字面量场景 */
    STRING_LITERAL,
    /** Git提交场景 */
    GIT_COMMIT,
    /** 工具窗口场景 */
    TOOL_WINDOW,
    /** Vim Normal模式场景 */
    VIM_NORMAL,
    /** Vim Insert模式场景 */
    VIM_INSERT
}

/**
 * 上下文类型枚举
 * 定义代码分析后的具体上下文类型
 *
 * @author 林龙祥
 * @since 2026-02-04
 */
enum class ContextType {
    /** 默认代码区域 */
    CODE_DEFAULT,
    /** 块注释区域 */
    COMMENT_BLOCK,
    /** 行注释区域 */
    COMMENT_LINE,
    /** 字符串字面量 */
    STRING_LITERAL,
    /** Git提交信息 */
    GIT_COMMIT_MESSAGE,
    /** 终端工具窗口 */
    TOOL_WINDOW_TERMINAL,
    /** 项目工具窗口 */
    TOOL_WINDOW_PROJECT
}
