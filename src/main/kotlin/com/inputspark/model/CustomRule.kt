package com.inputspark.model

/**
 * 自定义规则实体类
 * 用于存储用户自定义的输入法切换规则
 *
 * @author 林龙祥
 * @since 2026-02-04
 */
class CustomRule {
    /** 规则唯一标识 ID */
    var id: String = ""
    
    /** 规则名称 */
    var name: String = ""
    
    /** 正则表达式模式 */
    var pattern: String = ""
    
    /** 目标输入法类型 */
    var targetInputMethod: InputMethodType = InputMethodType.ENGLISH
    
    /** 是否启用该规则 */
    var enabled: Boolean = true
    
    /** 规则优先级（数字越大优先级越高） */
    var priority: Int = 0
}
