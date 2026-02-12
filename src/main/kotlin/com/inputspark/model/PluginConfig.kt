package com.inputspark.model

import com.intellij.util.xmlb.annotations.OptionTag
import com.intellij.util.xmlb.annotations.XMap

/**
 * 插件配置实体类
 * 存储插件的所有配置信息
 *
 * @author 林龙祥
 * @since 2026-02-04
 */
class PluginConfig {
    /** 配置唯一标识 ID */
    var id: String = ""
    
    /** 场景配置映射（场景类型名称 -> 是否启用） */
    @get:OptionTag("sceneConfig")
    @get:XMap
    var sceneConfig: MutableMap<String, Boolean> = mutableMapOf(
        SceneType.DEFAULT.name to true,
        SceneType.COMMENT.name to true,
        SceneType.STRING_LITERAL.name to false,
        SceneType.GIT_COMMIT.name to true
    )
    
    /** 光标颜色配置映射（输入法类型名称 -> 颜色十六进制代码） */
    @get:OptionTag("cursorColors")
    @get:XMap
    var cursorColors: MutableMap<String, String> = mutableMapOf(
        InputMethodType.ENGLISH.name to "#4CAF50", // 绿色
        InputMethodType.CHINESE.name to "#F44336", // 红色
        InputMethodType.JAPANESE.name to "#2196F3"  // 蓝色
    )
    
    /** 自定义规则列表 */
    var customRules: MutableList<CustomRule> = mutableListOf()
    
    /** 插件是否全局启用 */
    var enabled: Boolean = true
    
    /** 创建时间 (时间戳) */
    var createdAt: Long = System.currentTimeMillis()
    
    /** 更新时间 (时间戳) */
    var updatedAt: Long = System.currentTimeMillis()
}
